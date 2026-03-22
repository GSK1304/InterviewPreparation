# 📚 HLD Deep Dive — Messaging System (WhatsApp / Kafka-based)

---

## 🎯 Problem Statement
Design a real-time messaging system like WhatsApp that supports 1-to-1 and group messaging, message persistence, delivery receipts, and offline message delivery.

---

## Step 1: Clarify Requirements

### Functional
- Send/receive messages 1-to-1 and in groups (up to 500 members)
- Message persistence — history retrievable
- Delivery receipts (sent ✓, delivered ✓✓, read ✓✓ blue)
- Offline message delivery (messages delivered when user comes back online)
- Media messages (images, videos, files)
- Push notifications for offline users

### Non-Functional
- **Availability**: 99.99% — messaging must always work
- **Latency**: Message delivery < 100ms for online users
- **Scale**: 2B users, 100B messages/day
- **Ordering**: Messages in a conversation must be ordered
- **Durability**: Messages must not be lost

---

## Step 2: Estimation

```
Messages/day: 100B = ~1.16M messages/sec = 1.2M MPS
Peak: ~3.5M MPS

Storage per message: ~100 bytes (text) + metadata
Storage/day: 100B × 100 bytes = 10 TB/day
5-year storage: 10TB × 365 × 5 ≈ 18 PB (use cold storage for old messages)

Active connections: 2B × 30% online = 600M concurrent WebSocket connections
  → Need ~600,000 servers @ 1,000 connections each
```

---

## Step 3: API Design

```
POST /api/v1/messages
  Body: { "to": "user_id", "content": "Hello", "type": "text", "conversationId": "..." }
  Response: { "messageId": "...", "timestamp": "...", "status": "sent" }

GET /api/v1/conversations/{conversationId}/messages?before=msgId&limit=50
  Response: { "messages": [...], "hasMore": true }

WebSocket /ws
  Connect with JWT: ws://api/ws?token=...
  Events:
    → message.new         (incoming message)
    → message.delivered   (delivery receipt)
    → message.read        (read receipt)
    → user.online         (presence)
    ← message.send        (send message)
    ← message.ack         (acknowledge receipt)
```

---

## Step 4: High-Level Architecture

```
                    ┌─────────────────────────────┐
                    │         Push Notification    │
                    │       (APNs / FCM)           │
                    └──────────────┬──────────────┘
                                   │ (offline users)
Client ──WebSocket──► Chat Server ─┤
                         │         │ (online users)
                         │         └──────────────► WebSocket to recipient
                         ▼
                    Kafka (message events)
                         │
              ┌──────────┼──────────────┐
              ▼          ▼              ▼
        Message DB  Notification   Analytics
        (Cassandra) Consumer       Consumer
              │
        ┌─────▼──────┐
        │ Redis Cache │
        │ (recent     │
        │  messages,  │
        │  presence)  │
        └────────────┘
```

---

## Step 5: Core Components

### Chat Server (WebSocket Service)
```
Each Chat Server:
  - Maintains WebSocket connections (100K per server)
  - Stateful: knows which users are connected to IT
  - On message receive: route to recipient's server via Kafka
  - On user connect: register in Service Discovery (Zookeeper/Redis)

Service Discovery:
  - user_id → chat_server_id mapping
  - Stored in Redis: SET user:{userId}:server {serverId}
  - TTL = heartbeat interval (30s)
```

### Message Flow (Online-to-Online)
```
1. User A sends "Hello" via WebSocket to Chat Server 1
2. Chat Server 1:
   a. Saves message to Cassandra (async, Kafka producer)
   b. Looks up User B's server: Redis → "User B is on Chat Server 3"
   c. Publishes message event to Kafka topic (partitioned by conversationId)
3. Kafka consumer on Chat Server 3 receives event
4. Chat Server 3 pushes message to User B's WebSocket connection
5. User B's client acks → Chat Server 3 publishes delivery receipt
6. Chat Server 1 receives delivery receipt → forwards to User A
```

### Message Flow (Offline Delivery)
```
1. User A sends message → Chat Server saves to Cassandra
2. Service discovery: User B is OFFLINE (no entry in Redis)
3. Publish event to Notification Kafka topic
4. Notification Service:
   - Checks User B's device tokens
   - Sends push notification via APNs (iOS) / FCM (Android)
5. User B comes online:
   - Connects WebSocket
   - Requests undelivered messages: GET /conversations?since=lastSeenAt
   - Server queries Cassandra for messages after lastSeenAt
```

---

## Step 6: Database Design

### Message Storage (Cassandra — Write-heavy, time-series)
```sql
-- Partition by conversationId for fast conversation fetch
-- Cluster by message_id (time-ordered) for pagination

CREATE TABLE messages (
    conversation_id  UUID,
    message_id       TIMEUUID,   -- time-ordered UUID (ordering built-in)
    sender_id        UUID,
    content          TEXT,
    message_type     TEXT,       -- text, image, video, file
    media_url        TEXT,       -- nullable
    status           TEXT,       -- sent, delivered, read
    created_at       TIMESTAMP,
    PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);
-- Fetch last N messages in conversation: O(1) Cassandra partition read
```

### User & Conversation (PostgreSQL)
```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY,
    username    VARCHAR(50) UNIQUE,
    phone       VARCHAR(20) UNIQUE,
    created_at  TIMESTAMP
);

CREATE TABLE conversations (
    id          UUID PRIMARY KEY,
    type        VARCHAR(10), -- 'direct' or 'group'
    name        VARCHAR(100), -- null for direct
    created_at  TIMESTAMP
);

CREATE TABLE conversation_members (
    conversation_id UUID REFERENCES conversations(id),
    user_id         UUID REFERENCES users(id),
    joined_at       TIMESTAMP,
    last_read_at    TIMESTAMP, -- for read receipts
    PRIMARY KEY (conversation_id, user_id)
);
```

---

## Step 7: Group Messaging at Scale

### Fan-out Strategy
```
Problem: Group with 500 members. 1 message → 499 deliveries.
At 1M messages/sec with avg 50 members = 50M fan-outs/sec

Option 1: Fan-out on Write
  - On send: create 500 delivery records, push to 500 WebSocket connections
  - Fast reads, slow writes
  - Problem: Celebrity/large group problem (500K members)

Option 2: Fan-out on Read
  - Store message once in Cassandra
  - Each member fetches on poll
  - Fast writes, slow reads, more DB reads

✅ Hybrid (WhatsApp approach):
  - Fan-out on Write for small groups (< 500 members) — store per-member delivery record
  - Fan-out on Read for very large groups — members poll on connect
```

---

## Step 8: Message Ordering & Exactly-Once Delivery

### Ordering Guarantee
```
Within a conversation → guaranteed by:
  1. TIMEUUID as message_id (Cassandra ordering)
  2. Kafka partition by conversationId (FIFO per partition)
  3. Client-side sequence numbers + gap detection

Cross-conversation → no global ordering needed
```

### At-Least-Once vs Exactly-Once
```
Kafka consumer:
  - At-least-once: commit offset after processing (possible duplicate on retry)
  - Exactly-once: idempotent producers + transactional consumers (Kafka 0.11+)

For messaging: use At-least-once + idempotency key
  - messageId is idempotency key
  - Duplicate delivery is handled by client (deduplicate by messageId)
```

---

## Step 9: Presence System (Online/Offline Status)

```
Architecture:
  - User connects WebSocket → publish "online" event to Kafka
  - Redis: SET user:{userId}:presence "online" EX 60 (TTL = heartbeat interval)
  - Client sends heartbeat every 30s → refresh TTL
  - User disconnects or TTL expires → presence marked offline

Scale challenge:
  - 600M concurrent users updating presence
  - Solution: Don't broadcast every presence change
  - Instead: Only update presence when a user OPENS a conversation
  - "Last seen" timestamp is good enough for most users
```

---

## Step 10: Trade-offs

| Decision | Choice | Reason |
|----------|--------|--------|
| DB for messages | Cassandra | Write-heavy, time-series, linear scale |
| DB for users/groups | PostgreSQL | ACID, relational, moderate scale |
| Real-time | WebSocket | Bi-directional, low latency |
| Routing | Kafka | Decouple chat servers; replay; fan-out |
| Offline delivery | Push notifications | Can't hold HTTP connection when offline |
| Ordering | TIMEUUID + Kafka partition | Conversation-level ordering sufficient |
| Group fan-out | Hybrid (write small, read large) | Balance write/read load |
| Presence | Redis TTL + heartbeat | Low DB load; eventually consistent |

---

## Kafka Architecture for kACE (Real-world Connection)

```
kACE uses similar patterns:

RFQ Topic (partitioned by rfqId):
  Producer: RfqMonitorWebSocketMessageHandler
  Consumer: RfqDeltaPollingService (pull-based)
  Pattern: Fan-out to all subscribers of an RFQ

WebSocket routing (similar to chat server routing):
  StompClientSingleton maintains connection
  SubscriptionRegistry maps topic → sessions
  Heartbeat-based expiry (same as presence system)
```
