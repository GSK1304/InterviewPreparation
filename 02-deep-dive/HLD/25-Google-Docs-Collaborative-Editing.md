# 📚 System Design — Collaborative Document Editing (Google Docs)

---

## 🎯 Problem Statement
Design a real-time collaborative document editing system where multiple users can edit the same document simultaneously, see each other's changes in real-time, and the document converges to a consistent state.

---

## Step 1: Clarify Requirements

### Functional
- Create, read, update, delete documents
- Multiple users edit the same document simultaneously
- Real-time visibility of other users' changes (< 500ms)
- Show collaborator cursors and selections
- Conflict resolution: concurrent edits must not corrupt the document
- Version history (undo, restore to version)
- Comments and suggestions
- Offline editing with sync when reconnected
- Sharing and permissions (view/comment/edit)

### Non-Functional
- **Scale**: 1B documents, 100M DAU, up to 100 simultaneous editors
- **Latency**: Edits appear to collaborators < 500ms
- **Availability**: 99.99%
- **Consistency**: Strong eventual consistency — all clients converge to same state
- **Durability**: No data loss, even on server crash mid-edit

---

## Step 2: Estimation

```
Documents:       1B total; 10M active simultaneously (being edited)
Edits:           100M DAU × 100 edits/day = 10B edits/day = 115K ops/sec peak
Edit size:       avg 20 bytes per operation (character insert/delete + position)
Edit bandwidth:  115K × 20 bytes = 2.3MB/sec (low — text is tiny)
Storage:         10B edits × 20 bytes = 200GB/day (compressed ops log)
Document size:   avg 50KB; 1B docs = 50TB
```

---

## Step 3: The Core Problem — Concurrent Edit Conflict

This is the hardest part of collaborative editing. Two users editing simultaneously.

### Naive Approach (Lock-Based) — Why It Fails
```
User A acquires lock on document → edits → releases lock
User B must wait while A has lock

Problems:
  Only one person edits at a time → not collaborative
  User disconnects while holding lock → stuck indefinitely
  Unusable for real-time collaboration
```

### Operational Transformation (OT) — Google Docs' Approach
```
Core idea: transform operations to account for concurrent edits

Example:
  Document: "Hello World"
  
  User A: insert "Beautiful " at position 6 → "Hello Beautiful World"
  User B (concurrent): delete "World" at position 6 → "Hello "

  If applied naively:
    A's op: insert at 6 → OK
    B's op: delete at 6 → deletes "B" (wrong position!)

  OT transforms B's op based on A's op:
    B's delete: position 6 + len("Beautiful ") = position 16 → delete "World" at 16
    Result: "Hello Beautiful " ✅ (both changes applied correctly)

OT rules for text:
  insert(pos, char) vs insert(pos2, char2):
    if pos2 <= pos: shift pos right by 1
    else: pos unchanged
    
  insert(pos, char) vs delete(pos2):
    if pos2 < pos: shift pos left by 1
    if pos2 >= pos: pos unchanged
    
  (And many more rules for complex operations)
```

### Conflict-Free Replicated Data Types (CRDTs) — Alternative to OT

```
CRDTs are data structures designed to be merged without conflict.
No central server needed for conflict resolution.

For text: LSEQ or CRDT Rope

LSEQ: each character gets a unique fractional position between two neighbors
  Insert between position 1.0 and 2.0 → assign 1.5
  Insert between 1.0 and 1.5 → assign 1.25
  Never conflicts (positions are unique fractions)
  
  "H" at 1.0, "e" at 2.0, "l" at 3.0, "l" at 4.0, "o" at 5.0
  Insert "X" between "H" and "e": assign position 1.5
  Insert "Y" concurrently at same position: different user ID breaks tie
  
  Pros: No server coordination needed, works offline
  Cons: Position IDs grow without bound (garbage collection needed)

Google Docs uses OT (server coordinates transforms)
Figma uses CRDT (peer-to-peer friendly)
```

---

## Step 4: High-Level Architecture

```
User A (editing)           User B (editing)
     │                          │
     │ WebSocket                │ WebSocket
     │                          │
┌────▼──────────────────────────▼────┐
│         Collaboration Server        │
│  (stateful — holds document state)  │
│                                    │
│  ┌─────────────────────────────┐   │
│  │  OT Engine                  │   │
│  │  - Receive op from User A   │   │
│  │  - Transform against pending│   │
│  │  - Broadcast to User B      │   │
│  │  - Persist to op log        │   │
│  └─────────────────────────────┘   │
└────────────────┬───────────────────┘
                 │
    ┌────────────┼────────────────────┐
    │            │                    │
┌───▼───────┐ ┌──▼──────────┐ ┌──────▼────┐
│  Document  │ │  Operation  │ │  Presence │
│  Storage   │ │  Log        │ │  Service  │
│ (GCS/S3)   │ │ (Cassandra) │ │  (Redis)  │
│ Full doc   │ │ All ops +   │ │ Who's     │
│ snapshots  │ │ timestamps  │ │ online    │
└────────────┘ └─────────────┘ └───────────┘
```

---

## Step 5: OT Server Implementation

```
Server maintains authoritative document state:
  - Receives operations from all clients
  - Assigns each operation a global sequence number
  - Transforms operations as needed
  - Broadcasts to all other clients

Data structures:
  Document state: current document text (in-memory on collaboration server)
  Operation log: (seqNum, clientId, operation, timestamp)
  Client cursors: {clientId → cursorPosition}

Processing an incoming operation:
  op arrives from Client A with client_revision = 42
  server_revision = 50 (server is ahead — other clients sent ops)
  
  Transform A's op against ops 43-50 (ops that happened since A's revision):
    for each server_op in ops[43..50]:
      A_op = transform(A_op, server_op)
  
  Apply transformed A_op to document
  Assign seqNum = 51
  Broadcast to all clients:
    to Client B (at revision 50): broadcast A_op as-is
    to Client C (at revision 48): broadcast A_op + transform info

Client applies operations:
  Client receives op from server
  If server_revision matches client_revision: apply directly
  Else: transform received op against client's pending (unacknowledged) ops
```

---

## Step 6: Persistence Strategy

```
Two-level persistence:

Level 1: Operation log (every change recorded)
  Cassandra: (doc_id, seq_num, user_id, op_type, op_data, timestamp)
  Ordered by seq_num → supports replay from any point
  Used for: undo history, version restore, debugging, analytics
  Retention: 30 days of full ops; older → snapshot only

Level 2: Document snapshots
  Full document state saved periodically (every 100 ops or every 5 min)
  Stored in GCS/S3 as compressed JSON/protobuf
  
  At document load:
    Find latest snapshot → load it
    Replay ops from snapshot seq_num to current
    This is faster than replaying from the beginning

Recovery after server crash:
  Collaboration server is stateful — what if it crashes?
  
  On crash: clients detect WebSocket disconnect
  On reconnect: client sends its revision number
  Server loads snapshot + replays ops since snapshot
  Client receives missed ops → catches up
  
  For large documents with many concurrent editors:
    Use Redis as real-time state buffer (fast, in-memory)
    Async flush to Cassandra (persistence)
    If Redis fails: replay from Cassandra (slightly slower reconnect)
```

---

## Step 7: Presence and Cursors

```
Show other editors' cursors and selections in real-time

Architecture:
  User connects → register presence: SET presence:{docId}:{userId} {name,color} EX 30
  Heartbeat every 15s → refresh TTL (liveness)
  User disconnects → TTL expires → presence removed
  
  SUBSCRIBE doc:{docId}:presence (Redis Pub/Sub)
  → All collaboration servers subscribed
  → Any client cursor move → PUBLISH doc:{docId}:presence {cursorUpdate}
  → All connected clients receive cursor updates

Cursor positions in operations:
  Every operation includes cursor position AFTER the operation
  Clients maintain cursor positions for all active editors
  Transform cursor positions the same way as content operations

Name and color assignment:
  First user: blue, "Alice"
  Second user: red, "Bob"
  Colors from a fixed palette (8 colors → if > 8 editors, reuse colors)
  Random animal names for anonymous editors (Google's "Mysterious Wombat")
```

---

## Step 8: Offline Editing

```
User closes laptop, edits while offline, reopens:
  
Client-side storage:
  IndexedDB: store document snapshot + pending operations queue
  Service Worker: intercept edits → queue locally
  
Reconnect flow:
  Client sends: { myRevision: 45, pendingOps: [op46, op47, op48] }
  Server:
    Load document at revision 45
    Get ops 46-latest from Cassandra
    Transform client's pending ops against server ops 46-latest
    Apply client ops
    Return: { newOps: [serverOp46-60], transformedClientOps: [op61, op62] }
  Client:
    Apply new server ops
    Replace pending ops with transformed versions
    Document is now in sync
    
Conflict example:
  User A (online): changed heading to "Chapter 2"
  User B (offline): changed same heading to "Section 2"
  
  On B's reconnect: OT transforms B's op
  Result depends on timing — last writer wins for same character position
  Both changes preserved in operation log for history
```

---

## Step 9: Horizontal Scaling

```
Challenge: Collaboration server is stateful (holds doc in memory)
           Must route all sessions for same document to SAME server

Solution: Document-affinity routing
  API Gateway knows document → server mapping
  GET /docs/{id}/ws → API Gateway routes to DocServer-3 (owns doc {id})
  
Document ownership:
  Hash-based: server = hash(docId) % numServers
  Problem: server restart → all docs on that server lost (reconnect surge)
  
  Better: Consistent hashing with virtual nodes
    Minimizes re-routing on server add/remove
    doc123 → Server A (primary), Server B (backup)
    
  Even better: Stateless via Redis
    Document state stored in Redis (fast enough for real-time)
    Any collaboration server can handle any document
    No affinity routing needed
    State: Redis Hash of {revision, pending_ops, collaborator_cursors}
    Ops: Redis Sorted Set by sequence number
    
At Netflix/Google scale: dedicated collaboration cluster per large document
"Rooms": each document is a room on one collaboration server
```

---

## Interview Q&A

**Q: Why does OT need a central server? Can't it be purely peer-to-peer?**
A: OT requires a central server to assign a global ordering to operations (sequence numbers). Without this, two clients can't agree on which operation to transform against which — the diamond dependency problem. CRDTs (like the ones used in Figma, and some offline-first apps) solve this by designing the data structure to be inherently merge-friendly, allowing peer-to-peer operation. The tradeoff is more complex data structures and ID size growth.

**Q: How does Google Docs handle undo when there are concurrent edits?**
A: Undo in collaborative editing is "selective undo" — undo YOUR change, not the most recent change. When you hit Ctrl+Z, it undoes your last operation. The server generates an inverse operation (delete "X" becomes insert "X") and transforms it against all operations that occurred since, then applies it. This preserves other users' changes while undoing yours.

**Q: What happens if the collaboration server crashes while users are editing?**
A: Clients detect WebSocket disconnect and switch to a reconnection loop. The operation log in Cassandra preserves all committed operations. When a new server instance takes over, it loads the latest snapshot + replays recent ops from Cassandra. Clients reconnect, send their revision numbers and pending ops, and the server catches them up. Users typically see a brief "reconnecting..." indicator and then resume seamlessly.

**Q: How would you design version history for Google Docs?**
A: The operation log in Cassandra IS the version history. To restore to any point: load the snapshot before that time, replay operations up to the desired timestamp. For user-visible versioning: periodic "named" snapshots (every hour of activity creates a version). Version list: timestamps + editor names. Restore: create a new operation that replaces document content with the historical snapshot.

**Q: How does Google Docs support 100 simultaneous editors without performance degradation?**
A: The key is that text operations are tiny (20-50 bytes) — 100 users typing simultaneously = only 2-5KB/sec per document. The collaboration server handles OT in-memory (microsecond transforms). The bottleneck is broadcast fan-out: one edit → broadcast to 99 other WebSocket connections. With WebSockets over HTTP/2 (multiplexing), this is handled efficiently. Presence updates are throttled (cursor position max once per 50ms) to avoid overwhelming the server.
