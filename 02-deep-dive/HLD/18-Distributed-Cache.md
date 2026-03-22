# 📚 System Design — Distributed Cache (Redis-like)

---

## 🎯 Problem Statement
Design a distributed in-memory cache that supports key-value operations with sub-millisecond latency, horizontal scaling, high availability, and multiple eviction policies.

---

## Step 1: Clarify Requirements

### Functional
- GET(key) → value or null
- SET(key, value, ttl?)
- DELETE(key)
- TTL-based expiry
- Eviction policies (LRU, LFU, TTL)
- Support string, list, hash, set, sorted set data types
- Pub/Sub messaging
- Persistence (optional — RDB snapshots, AOF log)
- Atomic operations (INCR, SETNX)

### Non-Functional
- **Latency**: < 1ms p99 for GET/SET
- **Throughput**: 1M+ ops/sec per node
- **Availability**: 99.99%
- **Consistency**: Strong within a shard; eventual across replicas
- **Scale**: Horizontal sharding across N nodes

---

## Step 2: Estimation

```
Single cache node (Redis benchmark):
  100K–1M ops/sec on single thread (Redis is single-threaded for ops)
  Memory: up to 100GB per node (EC2 r6g.8xlarge = 256GB RAM)

Cluster:
  10M ops/sec → 10–100 nodes depending on op complexity
  1TB total cache → ~10 nodes × 100GB each

Key-value sizes:
  Key: avg 50 bytes
  Value: avg 500 bytes (session: ~1KB, product: ~2KB, token: ~200 bytes)
  1M cached items × 550 bytes = ~550MB (tiny — memory is cheap)
```

---

## Step 3: API Design

```
# Core operations
GET     key                         → value | null
SET     key value [EX seconds]      → OK
SETNX   key value                   → 1 (set) | 0 (already exists)
DEL     key [key ...]               → count deleted
EXPIRE  key seconds                 → 1 | 0
TTL     key                         → remaining TTL | -1 (no TTL) | -2 (gone)

# Atomic numeric
INCR    key                         → new value (atomic increment)
INCRBY  key delta

# Complex types
HSET    key field value             → hash set
HGET    key field                   → hash get
ZADD    key score member            → sorted set add
ZRANGE  key 0 -1 WITHSCORES        → sorted set range

# Distributed lock
SET lock:resource clientId NX EX 30 → OK | null (SETNX + EXPIRE atomic)

# Pub/Sub
SUBSCRIBE  channel
PUBLISH    channel message
```

---

## Step 4: High-Level Architecture

```
             Clients (Application Servers)
                    │
         ┌──────────▼──────────────┐
         │    Client Library        │
         │  (handles routing,       │
         │   consistent hashing,    │
         │   connection pooling)    │
         └──────────┬──────────────┘
                    │
    ┌───────────────┼───────────────────────┐
    │               │                       │
┌───▼──────┐  ┌─────▼─────┐  ┌────────────▼──┐
│  Shard 1  │  │  Shard 2  │  │   Shard 3     │
│ (Master)  │  │ (Master)  │  │   (Master)    │
│  Replica  │  │  Replica  │  │   Replica     │
└───────────┘  └───────────┘  └───────────────┘
    Keys 0-5460  Keys 5461-10922  Keys 10923-16383
    (Redis uses 16384 hash slots)

Routing:
  Client: slot = CRC16(key) % 16384
  Client library maps slot → shard
  Direct connection to correct shard (no proxy hop)
```

---

## Step 5: Core Internal Design

### Single-Threaded Event Loop
```
Why Redis is single-threaded (for commands):
  No lock contention → zero mutex overhead
  No context switching → CPU cache stays warm
  Simple mental model → easy to reason about consistency
  
  Performance: 1M simple ops/sec on a single core
  
  IO is multi-threaded in Redis 6+ (network I/O parallelized)
  Command execution still single-threaded
  
  For CPU-heavy ops: use Redis Cluster (parallelize across shards)
```

### Memory Management
```
Internal data structures (Redis's actual implementation):

String:
  Small values (< 44 bytes): embstr (string embedded in robj)
  Large values: dynamic string (sds)

Hash:
  Small hash (< 128 fields, fields < 64 bytes): ziplist (compact array)
  Large hash: hashtable

Sorted Set:
  Small (< 128 elements): ziplist
  Large: skiplist + hashtable (O(log N) rank, O(1) score lookup)

List:
  Small: listpack
  Large: quicklist (linked list of ziplists)

Memory saving: these compact representations use 3-10x less memory
```

### Hash Table Implementation
```
Array of buckets, chaining for collisions.
Load factor threshold: 1.0 → start rehashing
Incremental rehash: don't block event loop → rehash 1 bucket per command
Two hash tables during rehash:
  Reads: check ht[0] first, then ht[1]
  Writes: always go to ht[1]
  Background: gradually move all keys from ht[0] to ht[1]
```

---

## Step 6: Eviction Policies

```
When memory is full, which keys to evict?

noeviction:      Return error when memory full. Use: critical data.
allkeys-lru:     Evict least recently used key from all keys.
volatile-lru:    Evict LRU key from keys WITH TTL set only.
allkeys-lfu:     Evict least frequently used (Redis 4+).
volatile-lfu:    Evict LFU from TTL keys only.
allkeys-random:  Random eviction. Use: cache-only data, all keys equal.
volatile-ttl:    Evict key with shortest remaining TTL first.

LRU approximation (Redis doesn't do exact LRU):
  Exact LRU needs doubly-linked list → too much memory overhead
  Redis: sample 5 random keys, evict LRU among those 5
  With 10 samples: nearly identical to exact LRU
  Memory overhead: near zero vs true LRU

LFU (Least Frequently Used):
  Better than LRU for popularity-skewed access (80/20 rule)
  Redis LFU: counter with decay (recent frequency weighted more)
  Morris counter: probabilistic, uses only 8 bits per key
```

---

## Step 7: TTL Expiry

```
Lazy expiry:
  Key is NOT deleted immediately when TTL expires
  On next access: check if expired → delete → return null
  Saves CPU (no active scanning)
  Problem: memory leak if expired keys never accessed

Active expiry (background):
  Every 100ms: sample 20 keys with TTL → delete expired ones
  Repeat until < 25% of sampled keys are expired
  Controlled CPU usage (< 25% of single core)

Combined: lazy + active = efficient and no significant memory leak

redis-cli DEBUG SLEEP: tests active expiry behavior
```

---

## Step 8: Replication & Persistence

### Replication
```
Master-replica async replication:
  1. Replica connects to master: REPLICAOF host port
  2. Master: RDB snapshot → sent to replica (initial sync)
  3. After sync: every command on master → replicated to replicas
  4. Replication is asynchronous (lag possible)

Replica uses:
  Read scaling (read from replicas)
  Failover (promote replica if master dies)
  
WAIT command: wait for N replicas to acknowledge (synchronous when needed)
```

### Persistence Options
```
RDB (Redis Database Snapshot):
  Periodic point-in-time snapshot to disk
  SAVE 900 1 → save if 1 key changed in 900 seconds
  Fast recovery (load binary file)
  Data loss: up to last snapshot interval (minutes)
  Use: acceptable RPO, fast restart

AOF (Append-Only File):
  Log every write command to disk
  appendfsync everysec: fsync every second (< 1 second data loss)
  appendfsync always: fsync every command (zero data loss, slower)
  Larger files than RDB; AOF rewrite to compact
  Use: near-zero RPO requirement

Both: use RDB for fast restart + AOF for durability (Redis recommendation)
```

---

## Step 9: High Availability — Redis Sentinel

```
Problem: Master fails → need automatic failover

Redis Sentinel (3+ instances):
  1. Sentinels monitor master + replicas
  2. If master unresponsive > down-after-milliseconds:
     Sentinel marks it SDOWN (Subjectively Down)
  3. If majority of sentinels agree: ODOWN (Objectively Down)
  4. Sentinels elect a sentinel leader (Raft-like voting)
  5. Leader promotes best replica to new master
  6. Updates config: other replicas point to new master
  7. Clients get new master address from Sentinel

Quorum: majority of sentinels must agree → prevents split-brain
Typical setup: 3 sentinels (odd number for majority vote)

Failover time: 10-30 seconds (configurable)
Client behavior: retry on connection error, re-ask Sentinel for master address
```

### Redis Cluster (Horizontal Scale)
```
Automatic sharding across N masters:
  16384 hash slots → distributed across masters
  
  3 masters (typical):
    Master 1: slots 0–5460
    Master 2: slots 5461–10922
    Master 3: slots 10923–16383

Each master has replicas for HA:
  Master 1 fails → its replica promoted to master
  Cluster self-heals (no Sentinel needed in cluster mode)

Client routing:
  MOVED redirect: "This key is in slot 5000 → go to node X"
  Smart clients cache slot map → direct routing (no redirect in steady state)

Cluster limitations:
  Cross-slot multi-key ops restricted (keys must be in same slot)
  Hash tags: {user}.session and {user}.cart → same slot
  Use: large datasets, write-heavy workloads needing horizontal scale
```

---

## Step 10: Distributed Lock (SETNX Pattern)

```
Problem: Distributed systems need mutually exclusive access to a resource

Redis distributed lock:
  Acquire: SET lock:{resource} {clientId} NX EX 30
    NX: only set if key doesn't exist
    EX 30: auto-expire in 30s (prevent deadlock if client crashes)
    Returns OK if acquired, nil if already locked

  Release: Lua script (atomic check-then-delete)
    if redis.call("GET", key) == clientId then
      return redis.call("DEL", key)
    else
      return 0
    end
    -- Must verify clientId to avoid releasing someone else's lock

  Renewal: Before expiry, SET lock:{resource} {clientId} XX EX 30
    (XX: only set if exists)

Problems with single-node lock:
  If Redis master fails before replication → new master doesn't have lock
  → Two clients think they hold the lock

Redlock (multi-node distributed lock):
  Acquire lock on majority (N/2 + 1) of N independent Redis instances
  All instances must acknowledge within TTL/2
  If < majority acquired → release all, retry with backoff
  More complex; debate on correctness (Martin Kleppmann vs Salvatore Sanfilippo)
  For most applications: single-master Redis lock is sufficient
```

---

## Interview Q&A

**Q: How is Redis different from Memcached?**
A: Redis supports rich data types (strings, hashes, lists, sets, sorted sets, streams), persistence (RDB/AOF), pub/sub, Lua scripting, and cluster mode. Memcached is simpler — only strings, no persistence, no replication. Redis is nearly always preferred for new systems. Memcached is slightly faster for simple string GET/SET but the difference is negligible for most apps.

**Q: How do you handle cache stampede (thundering herd on cache miss)?**
A: (1) Mutex/lock: first request to miss acquires a Redis lock, fetches from DB, populates cache. Others wait and retry. (2) Probabilistic early expiry: before key expires, randomly re-fetch it (probability increases as TTL approaches 0). (3) Background refresh: cache hit triggers async background refresh before key expires. (4) Stale-while-revalidate: serve stale value while refreshing asynchronously.

**Q: What is the difference between SET key value EX 30 and SETNX + EXPIRE?**
A: `SET key value EX 30` is atomic — the value is set AND the TTL is applied in one operation. `SETNX` + `EXPIRE` is two separate commands — if the process crashes between them, the key has no TTL (memory leak and potential deadlock for locks). Always use `SET NX EX` for atomic set-with-expiry.

**Q: How does Redis handle memory fragmentation?**
A: Redis uses jemalloc (better than glibc for fragmentation). `activedefrag yes` enables online defragmentation — Redis gradually moves keys to contiguous memory. Monitor `mem_fragmentation_ratio` — ideally 1.0–1.5. > 2.0 = high fragmentation. Can trigger `MEMORY PURGE` to force defragmentation or restart instance to compact memory.

**Q: How would you design cache key naming conventions?**
A: Use hierarchical colon-separated keys: `{service}:{entity}:{id}:{field}`. Example: `user:profile:123`, `user:session:abc456`, `product:price:789`, `rate_limit:user:123:hour`. Advantages: easy to scan by pattern (`SCAN 0 MATCH user:*`), namespacing prevents collisions, easy to understand purpose from key name. Document key schemas like you document DB schemas.
