# 📚 System Design — Distributed Locking Service

---

## 🎯 Problem Statement
Design a distributed locking service that allows multiple processes across different machines to coordinate access to shared resources, preventing race conditions in a distributed environment.

---

## Step 1: Why Distributed Locks?

In a single-process application, mutexes and semaphores handle concurrency. In distributed systems with multiple servers, these don't work — each process has its own memory space.

### Classic Problems Requiring Distributed Locks
```
Problem 1: Cron job deduplication
  10 instances of a service, each with a cron scheduler
  All 10 try to send the "daily digest email" at 8am
  → 10 copies of the email sent to each user ❌
  Solution: Only the instance that acquires the lock sends the email

Problem 2: Inventory management
  User A and User B simultaneously buy the last item in stock
  Both see "1 in stock" → both purchase → stock goes to -1 ❌
  Solution: Lock the item during purchase transaction

Problem 3: Leader election
  Multiple service instances, only one should be "active" (primary)
  Others should be "standby" → lock = leadership claim

Problem 4: Rate limiting with fairness
  Distributed rate limiter must atomically check-and-increment counters
  Non-atomic check-then-increment = race condition

Problem 5: Cache stampede prevention
  Cache miss → 1000 requests all try to rebuild cache simultaneously
  → 1000 DB queries ❌
  Solution: One process acquires lock, rebuilds cache, others wait
```

---

## Step 2: Requirements

### Functional
- `acquire(resourceId, clientId, ttl)` → success or failure (non-blocking) or wait (blocking)
- `release(resourceId, clientId)` → release lock
- `renew(resourceId, clientId, ttl)` → extend TTL while still holding lock
- Auto-expiry: lock expires if holder crashes (prevents deadlock)
- Fencing token: monotonically increasing token to detect stale lock holders

### Non-Functional
- **Correctness**: Only ONE holder at a time — critical
- **Availability**: Lock service down = all dependent operations blocked
- **Latency**: acquire/release < 5ms
- **Deadlock-free**: Guaranteed expiry (TTL)
- **Fairness**: Optional (FIFO queue for waiters)

---

## Step 3: Solution 1 — Redis SETNX (Simple, Most Common)

```
Acquire lock:
  SET lock:{resourceId} {clientId} NX EX {ttlSeconds}
  
  NX = only set if NOT EXISTS (atomic check-and-set)
  EX = auto-expire in N seconds (deadlock prevention)
  
  Returns: OK → lock acquired ✅
           nil → lock already held ❌ → retry or fail

Release lock (MUST verify owner — critical!):
  Lua script (atomic check-then-delete):
  
  local current = redis.call('GET', KEYS[1])
  if current == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  else
    return 0
  end
  
  Why Lua? GET then DEL are two operations.
  Without atomicity: Client A checks → lock is mine → Client B's TTL
  expires, Client C acquires → Client A deletes Client C's lock! ❌
  Lua script runs atomically — no race condition.

Renew lock (while still working):
  SET lock:{resourceId} {clientId} XX EX {newTtl}
  XX = only set if EXISTS (don't create if expired already)
  Returns OK = renewed ✅; nil = lock already expired (someone else may have it)

Lock key naming:
  lock:inventory:{itemId}       → item-level lock
  lock:cron:daily-digest        → singleton job lock
  lock:user:{userId}:payment    → per-user payment lock
```

### Common Mistake: Using TTL Too Short or Too Long
```
Too short TTL (e.g., 1 second):
  Client acquires lock, starts work
  Work takes 2 seconds → lock expires after 1s
  Another client acquires lock → TWO holders ❌ (split-brain)
  
  Fix: Renew lock periodically (every TTL/3 seconds)
       Or set TTL = generous upper bound for the work + buffer

Too long TTL (e.g., 1 hour):
  Client acquires lock, then crashes
  Lock sits for 1 hour → resource blocked for 1 hour ❌
  
  Fix: Balance TTL with acceptable downtime
  Typical: 30s TTL, renew every 10s while working
```

---

## Step 4: Solution 2 — Redlock (Multi-Node for High Availability)

### Problem with Single Redis Node
```
Scenario:
  Client A acquires lock on Redis master
  Master crashes before replicating to replica
  Replica promoted to master (doesn't have the lock)
  Client B acquires same lock on new master
  → TWO clients hold the lock ❌
```

### Redlock Algorithm (Distributed Lock across N Redis Instances)
```
Setup: 5 independent Redis instances (no replication between them)

Acquire:
  1. Record start time t1
  2. Try to acquire lock on ALL 5 Redis instances in sequence
     Each attempt: SET lock:{resource} {clientId} NX PX {ttlMs}
     Use short timeout per attempt (e.g., 5ms) — don't block on slow node
  3. Count successful acquisitions (SUCCESS count)
  
  Lock acquired IF:
    SUCCESS >= (N/2 + 1) = 3 out of 5  (quorum)
    AND elapsed time (t2 - t1) < TTL    (lock not already expired)
    
  Effective TTL = TTL - (t2 - t1)       (remaining validity time)
  
  If NOT acquired: release all N instances (cleanup)

Release:
  Release lock on ALL 5 Redis instances (even failed ones — belt and suspenders)

Why quorum?
  If 2 nodes fail: still 3 out of 5 respond → quorum met → works
  Clock drift: even with slight time differences, quorum prevents split-brain
  Two clients cannot both get quorum (would need 6 out of 5 locks)
```

### Redlock Controversy
```
Martin Kleppmann (2016): argued Redlock is unsafe because:
  - Process pauses (GC, OS scheduling) can cause lock expiry while still "running"
  - Clock drift can cause premature expiry
  - Fix: use FENCING TOKENS (see below)

Salvatore Sanfilippo (Redis creator): Redlock is "reasonably safe" for most use cases

Practical verdict:
  For most applications (cron job dedup, cache stampede prevention):
    Single Redis node + SETNX is sufficient
  For distributed coordination with strong safety:
    Use ZooKeeper or etcd (consensus-based, stronger guarantees)
  Redlock: for HA Redis clusters when ZooKeeper isn't available
```

---

## Step 5: Solution 3 — ZooKeeper / etcd (Strongest Guarantees)

### ZooKeeper Ephemeral Nodes
```
ZooKeeper data model: hierarchical znodes (like filesystem)

Lock acquisition using ephemeral sequential nodes:
  1. Client creates: /locks/mylock/lock-{sequenceNum} (ephemeral)
     Ephemeral = auto-deleted when client session disconnects
     Sequential = ZK appends monotonically increasing number
  
  2. Client lists all children of /locks/mylock
     If client's node has LOWEST sequence number → it holds the lock ✅
     If not → watch the node with the NEXT LOWER sequence number
  
  3. When next-lower node is deleted (previous holder releases/crashes):
     Watch fires → client re-checks if it now has lowest number
     If yes → acquires lock; if no → watches again

Example:
  Client A creates: /locks/mylock/lock-001 → lowest → holds lock
  Client B creates: /locks/mylock/lock-002 → watches lock-001
  Client C creates: /locks/mylock/lock-003 → watches lock-002
  
  Client A releases/crashes:
    lock-001 deleted
    Client B notified → lowest now (lock-002) → acquires lock ✅
    Client C still watches lock-002

Why this is safe:
  ZooKeeper uses ZAB consensus (Zookeeper Atomic Broadcast)
  All operations linearizable (total ordering)
  Ephemeral nodes → crash detection via heartbeat/session timeout
  No clock dependency → purely event-based
```

### etcd (Raft-based)
```
etcd uses Raft consensus (also used by Kubernetes for its own locking).

Lease-based lock:
  1. Create a lease with TTL: LeaseGrant(ttl=30s) → leaseId
  2. Atomic put with lease: PUT /locks/{resource} {clientId} withLease(leaseId)
     PrevKV: only succeed if key doesn't exist (atomic check-and-set)
  3. Keep lease alive: KeepAlive(leaseId) → sends heartbeat every 10s
  4. Release: Revoke(leaseId) OR DELETE /locks/{resource}
  5. If client crashes: lease TTL expires → key auto-deleted

etcd vs ZooKeeper:
  etcd: simpler API, native Kubernetes integration, HTTP/gRPC API
  ZooKeeper: more mature, richer features (watches, sequential nodes, ACLs)
  Both use consensus → strongest distributed lock guarantees
```

---

## Step 6: Fencing Tokens — The Correct Approach for Safety

```
Problem even with correct locking:
  Client A acquires lock (fencing token = 33)
  Client A's process pauses (GC pause, VM snapshot, etc.) for 60s
  Lock expires → Client B acquires lock (fencing token = 34)
  Client B writes to storage service with token 34 ✅
  Client A resumes (still thinks it holds lock) → writes with token 33 ❌

Fencing token solution:
  Lock service returns MONOTONICALLY INCREASING token on each acquire
  Client passes token with every storage operation
  Storage service REJECTS operations with token <= last seen token
  
  Client A: write(data, token=33) → rejected! (already saw token=34)
  Client B: write(data, token=34) → accepted ✅

Implementation:
  Redis: INCR lock:fencing_counter → returns next token
  ZooKeeper: sequential znode number IS the fencing token
  etcd: lease revision number acts as fencing token
  
  Storage service keeps: last_accepted_token per resource
  On write: if incoming_token <= last_accepted_token → reject (409 Conflict)
```

---

## Step 7: Building a Distributed Lock Service

```
If you want a dedicated service (rather than embedding in Redis/ZK):

API:
  POST /v1/locks/{resourceId}/acquire
    Body: { clientId, ttlMs, waitMs (0 = non-blocking) }
    Response: { acquired: true, token: 1234, expiresAt }
    OR: { acquired: false, currentHolder, expiresAt }

  DELETE /v1/locks/{resourceId}
    Header: X-Client-Id, X-Fencing-Token

  PUT /v1/locks/{resourceId}/renew
    Header: X-Client-Id, X-Fencing-Token
    Body: { ttlMs }

Internally:
  Backed by Redis SETNX for simplicity (or ZooKeeper for correctness)
  HTTP wrapper adds: observability, rate limiting, audit logging
  Monitoring: lock acquisition rate, contention rate, avg hold time
  Alert: locks held > 2× expected duration (possible deadlock)
```

---

## Step 8: Design Patterns Using Distributed Locks

### Pattern 1: Singleton Job (Cron Deduplication)
```java
// Only ONE instance runs the daily digest
boolean acquired = lock.acquire("cron:daily-digest", instanceId, ttl=120s);
if (acquired) {
    try {
        sendDailyDigest();
    } finally {
        lock.release("cron:daily-digest", instanceId);
    }
} else {
    log.info("Another instance is handling daily digest, skipping");
}
```

### Pattern 2: Optimistic Lock with Retry
```java
// Retry with exponential backoff + jitter
int maxRetries = 5;
for (int i = 0; i < maxRetries; i++) {
    LockResult result = lock.acquire("inventory:" + itemId, clientId, ttl=10s);
    if (result.acquired()) {
        try {
            decrementInventory(itemId);
            return SUCCESS;
        } finally {
            lock.release("inventory:" + itemId, clientId);
        }
    }
    Thread.sleep(100 * (1 << i) + random.nextInt(100)); // exponential + jitter
}
return RETRY_EXCEEDED;
```

### Pattern 3: Cache Stampede Prevention
```java
String cachedValue = cache.get(key);
if (cachedValue != null) return cachedValue;  // fast path

// Slow path: acquire lock to rebuild cache
boolean locked = lock.acquire("rebuild:" + key, instanceId, ttl=30s);
if (locked) {
    try {
        // Re-check cache (another instance may have rebuilt while we waited)
        cachedValue = cache.get(key);
        if (cachedValue == null) {
            cachedValue = db.query(key);  // ONE DB query
            cache.set(key, cachedValue, ttl=3600s);
        }
    } finally {
        lock.release("rebuild:" + key, instanceId);
    }
} else {
    // Lock held by another instance → wait briefly and retry cache
    Thread.sleep(50);
    cachedValue = cache.get(key);  // should be populated by now
}
return cachedValue;
```

---

## Interview Q&A

**Q: What is the difference between a distributed lock and a database transaction?**
A: A database transaction provides atomicity within a single database — operations either all commit or all roll back. A distributed lock coordinates access across multiple services or processes that may use different databases or no database at all (e.g., calling an external API). Database transactions can't span multiple services; distributed locks can. Use a transaction when all operations are in one DB; use a distributed lock for cross-service coordination.

**Q: If Redis goes down, all distributed locks fail. How do you handle this?**
A: Use Redis Sentinel or Redis Cluster for HA (automatic failover). For the single-node failure window: design operations to be idempotent — if the lock is unavailable, either queue the operation or return an error and let the client retry (no silent duplication). For critical operations: use Redlock (5 independent nodes) or etcd/ZooKeeper for stronger guarantees. In practice, Redis HA has 99.99% uptime — brief outages are acceptable for most lock use cases.

**Q: Can you use a database (PostgreSQL) as a distributed lock?**
A: Yes, with advisory locks or SELECT FOR UPDATE. PostgreSQL advisory locks: `SELECT pg_try_advisory_lock(hashtext('my-resource'))` — returns true/false atomically. Session-level (auto-release on disconnect) or transaction-level. Downside: ties lock lifetime to DB connection, DB is a bottleneck, doesn't scale as well as Redis. Fine for low-contention locks but Redis is preferred for high-throughput locking.

**Q: What is the "herd effect" and how do distributed locks cause it?**
A: When a popular lock is released, all waiting clients simultaneously try to reacquire it — causing a spike of lock acquisition attempts. Solution: (1) Random backoff — each waiter sleeps a random duration before retrying. (2) Queue-based waiting (ZooKeeper sequential nodes) — waiters are notified one at a time in order, no herd. (3) For caches specifically: use a queue where only the first waiter rebuilds, others wait for the result.

**Q: How does Kubernetes use distributed locking for leader election?**
A: Kubernetes controller-manager, scheduler, and other controllers use leader election via Kubernetes Lease objects stored in etcd. Each instance tries to CREATE or UPDATE a Lease object (atomic operation via etcd's CAS). The instance that successfully writes the lease is the leader. The leader renews the lease every few seconds. If renewal stops (crash), the lease expires and other instances compete to become the new leader. This is built on etcd's Raft consensus for correctness.
