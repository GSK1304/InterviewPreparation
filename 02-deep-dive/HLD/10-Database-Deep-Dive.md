# 📚 HLD Core Concepts — Database Deep Dive

---

## 1. Database Indexing

### What is an Index?
A separate data structure that maintains a sorted copy of one or more columns, enabling O(log n) lookup instead of O(n) full table scan.

### B-Tree Index (Default)
```
Table: users(id, email, name, created_at)

B-Tree index on email:
                [m@...]
               /        \
         [a@...][h@...]  [s@...][z@...]
        /   |    |   \   /   |   |   \
    [a.][d.][f.][h.] [n.][p.][t.][x.]  ← Leaf nodes (sorted, linked)

SELECT * FROM users WHERE email = 'john@gmail.com'
→ B-Tree traversal: O(log n) ✅ instead of O(n) full scan
```

### Index Types
| Type | Best For | Not Good For |
|------|---------|-------------|
| **B-Tree** | Range queries, equality, ORDER BY | Full-text search |
| **Hash** | Exact equality only (O(1)) | Range queries |
| **GIN/GiST** | Full-text search, JSON, arrays | Simple lookups |
| **BRIN** | Very large tables with natural sort (time-series) | Random data |
| **Partial** | Filtered subset of rows | Full table queries |
| **Composite** | Multi-column WHERE clauses | Single column lookups |

### Composite Index Rules
```sql
-- Index on (user_id, status, created_at)
-- Leftmost prefix rule:

SELECT * WHERE user_id = 1                          -- ✅ uses index
SELECT * WHERE user_id = 1 AND status = 'active'    -- ✅ uses index  
SELECT * WHERE user_id = 1 AND status = 'active' AND created_at > '2024' -- ✅
SELECT * WHERE status = 'active'                    -- ❌ can't use (not leftmost)
SELECT * WHERE created_at > '2024'                  -- ❌ can't use (not leftmost)
```

### Covering Index
```sql
-- "Index-only scan" — answer query entirely from index, no table access
CREATE INDEX idx_cover ON orders(user_id, status, total);

SELECT status, total FROM orders WHERE user_id = 123;
-- All needed columns (status, total) are IN the index → no table heap access
-- 5-10x faster than non-covering index for this query
```

### Index Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| O(log n) reads instead of O(n) | Writes slower (index must be updated) |
| Enables sort without sort step | Storage overhead (can be 50%+ of table) |
| Foreign key constraint enforcement | Too many indexes = slow bulk inserts |
| Covering index = no table access | Index bloat over time (VACUUM needed) |

### 🏭 Industry Examples
- **Instagram**: B-Tree indexes on `(user_id, created_at)` for fetching user's photo feed.
- **Airbnb**: Partial index on `available_listings WHERE available = true` — much smaller than full index.
- **Uber**: Composite index on `(city_id, status, pickup_time)` for dispatch queries.

---

## 2. Write-Ahead Log (WAL)

### What is WAL?
Before writing data to disk pages, **write a record of the change to a sequential log first**.

```
Write request → [WAL (Sequential writes)] → return ACK to client
                        │
                        ▼ (async/background)
               [Data Pages (Random writes)]
```

### Why WAL Matters
```
Sequential writes to WAL: 500MB/s (fast, predictable)
Random writes to data pages: 50MB/s (slow, random seeks)

WAL gives you:
  1. Durability: If crash after WAL write but before page write
                 → replay WAL on restart → no data loss
  2. Speed: Acknowledge writes after WAL write (not page write)
  3. Replication: WAL shipped to replicas (PostgreSQL streaming replication)
  4. Point-in-time recovery: Replay WAL from any checkpoint
```

### WAL in Action: PostgreSQL
```
1. BEGIN TRANSACTION
2. Write WAL record: "INSERT INTO orders VALUES (...)"
3. WAL fsync'd to disk (durable!)
4. Return success to application
5. (Background) Dirty pages written to data files
6. Checkpoint: mark WAL position where all pages are flushed

Crash recovery:
  Start → find last checkpoint → replay WAL from that point → consistent state
```

---

## 3. Data Replication

### Synchronous vs Asynchronous Replication
```
Synchronous:
  Client → Primary → writes → Replica
                  ← ack ←── ack
  Confirm to client only after replica confirms
  + Zero data loss  
  - Higher write latency (wait for replica)
  Use: Financial transactions, user-visible writes

Asynchronous:
  Client → Primary → writes ──► Replica (eventually)
          ← ack (immediately)
  Confirm to client after primary writes; replica catches up
  + Lower write latency
  - Replication lag: replica may be behind (data loss possible on failover)
  Use: Read replicas for reporting, cross-region standby

Semi-synchronous:
  Wait for at least 1 replica to confirm (not all)
  Balance of durability and performance
  Used by: MySQL semi-sync replication
```

### Replication Topologies
```
Single-Leader (Most Common):
  Primary ──► Replica 1 (reads)
          ──► Replica 2 (reads)
          ──► Replica 3 (standby for failover)

  Writes → Primary only
  Reads → any replica (eventual consistency)

Multi-Leader (for multi-region writes):
  US Primary ←──►  EU Primary
       │                │
  US Replicas      EU Replicas
  Writes to nearest region; conflict resolution needed

Leaderless (Dynamo-style):
  Any node accepts writes; quorum (W + R > N) ensures consistency
  W = write quorum, R = read quorum, N = replication factor
  Cassandra: N=3, W=1, R=1 (fast, AP)
  Cassandra: N=3, W=2, R=2 (balanced)  
  Cassandra: N=3, W=3, R=3 (strong, like CP)
```

### Replication Lag Problems
```
Read-Your-Writes: User writes → reads from replica → doesn't see own write!
Fix: Route user's own reads to primary for 1 second after write

Monotonic Reads: User reads replica A (lag=1s) then replica B (lag=5s) → goes back in time!
Fix: Route same user's reads to same replica (session stickiness)

Causally Consistent Reads: User writes comment → friend can't see it yet
Fix: Pass timestamp with reads; replica waits until it has reached that timestamp
```

---

## 4. Database Partitioning

### Horizontal Partitioning (Sharding)
```
Range Sharding:
  Shard 1: user_id 1–1,000,000
  Shard 2: user_id 1,000,001–2,000,000
  + Easy range queries
  - Hot spots if IDs aren't uniformly accessed

Hash Sharding:
  Shard = hash(user_id) % 4
  + Even distribution
  - Can't do range queries across shards efficiently
  - Resharding requires remapping (consistent hashing helps)

Directory Sharding:
  Lookup table: user_id → shard_id
  + Most flexible
  - Lookup service is a SPOF/bottleneck

Geo Sharding:
  US users → US shard, EU users → EU shard
  + Compliance (GDPR data residency)
  + Latency for users
  - Uneven shard sizes if geo distribution uneven
```

### Vertical Partitioning
```
Split one wide table into multiple narrower tables:

users(id, name, email, avatar_blob, bio, preferences_json, last_login)
           ↓
users_core(id, name, email)         ← hot, frequently queried
users_profile(id, avatar_blob, bio) ← cold, large, rarely read
users_prefs(id, preferences_json)   ← moderate access

Benefits: Smaller hot table fits in memory; cache efficiency
```

### Cross-Shard Query Problems
```
Problem: SELECT COUNT(*) FROM users WHERE country='IN'
  → Must query ALL shards → fan-out → slow → aggregate

Solutions:
  1. Denormalize: maintain per-shard aggregates, merge at query time
  2. Dedicated analytics shard: replicate all shards to a single analytics DB
  3. Data warehouse: batch ETL to a separate analytics system (BigQuery, Redshift)
  4. Avoid: design queries to be shard-local where possible

Cross-shard JOIN:
  SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id
  → Requires users and orders on same shard (co-located sharding)
  → Or: fetch from both shards, join in application code
```

---

## 5. SQL vs NoSQL — When to Choose What

### Decision Framework
```
Choose SQL when:
  ✅ ACID transactions required (banking, e-commerce checkout)
  ✅ Complex queries with joins, aggregations
  ✅ Schema is well-defined and stable
  ✅ Strong consistency is required
  ✅ Small-to-medium scale (< 10TB, < 100K QPS)

Choose NoSQL when:
  ✅ Horizontal scale is the primary concern
  ✅ Schema flexibility needed (semi-structured data)
  ✅ Very high write throughput (time-series, events)
  ✅ Simple access patterns (key-value, document by ID)
  ✅ Eventual consistency acceptable
```

### NoSQL Types Deep Comparison
| Type | DB | Data Model | Use Case | Tradeoff |
|------|-----|-----------|---------|---------|
| **Key-Value** | Redis, DynamoDB | `key → value` | Session, cache, leaderboard | No complex queries |
| **Document** | MongoDB, Couchbase | JSON documents | User profiles, catalogs, CMS | Joins are hard |
| **Wide-Column** | Cassandra, HBase | Row key + column families | Time-series, IoT, messaging | Design for access patterns |
| **Graph** | Neo4j, Amazon Neptune | Nodes + edges | Social graphs, fraud detection | Not general purpose |
| **Time-Series** | InfluxDB, TimescaleDB | Timestamp + metrics | Monitoring, IoT sensor data | Only good for time-series |
| **Search** | Elasticsearch | Inverted index | Full-text search, log analytics | Not a primary store |

### 🏭 Industry Examples
- **Facebook**: MySQL (social graph facts) + Cassandra (inbox search) + RocksDB (storage engine) + MySQL (user data).
- **Twitter**: MySQL + Cassandra (tweets) + Manhattan (key-value). Different DB for different access patterns.
- **Airbnb**: MySQL for listings/bookings, Elasticsearch for search, Redis for sessions.

---

## 6. Database Connection Pooling

### Why Connection Pooling?
```
Without pool: Each request opens a DB connection → 10ms setup time → close
With pool: Pre-created connections reused → < 1ms acquisition time

Connection setup is expensive:
  - TCP handshake
  - TLS negotiation  
  - PostgreSQL authentication
  - ~10–50ms total
```

### Pool Configuration
```
min_connections: Keep X connections always open (warm pool)
max_connections: Never exceed this (protect DB from overload)
acquire_timeout: How long to wait if pool exhausted
idle_timeout: Close connections idle for N minutes

PostgreSQL max_connections: ~100–500 (each connection = ~5MB RAM)
→ Use PgBouncer (connection pooler) to multiplex thousands of app connections
   into a small pool of DB connections
```

### 🏭 Industry Examples
- **Instagram**: PgBouncer pools thousands of app connections to 100 PostgreSQL connections.
- **Uber**: Uses connection pooling middleware between microservices and MySQL/PostgreSQL.

---

## 7. Database Query Optimization

### EXPLAIN / EXPLAIN ANALYZE
```sql
EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id = 123 AND status = 'active';

Output:
  Seq Scan on orders (cost=0..50000 rows=500000)   ← BAD: full table scan!
  Filter: (user_id = 123 AND status = 'active')

After adding index (user_id, status):
  Index Scan on idx_user_status (cost=0..8 rows=3)  ← GOOD: index used
```

### Common Query Anti-patterns
```sql
-- ❌ Function on indexed column kills index
SELECT * FROM users WHERE LOWER(email) = 'john@gmail.com';
-- Fix: Store email pre-lowercased, or use functional index

-- ❌ Wildcard prefix kills B-Tree index
SELECT * FROM users WHERE name LIKE '%john%';
-- Fix: Use full-text search (GIN index) or Elasticsearch

-- ❌ N+1 query problem
for user in users:                    -- 1 query
    print(user.orders)                -- N queries (one per user!)
-- Fix: JOIN or use ORM eager loading (SELECT * FROM orders WHERE user_id IN (...))

-- ❌ SELECT * over the wire
SELECT * FROM products;               -- sends all columns including large blobs
-- Fix: SELECT only needed columns
```

---

## Interview Q&A

**Q: What's the difference between clustered and non-clustered index?**
A: Clustered index determines the physical order of rows on disk (the table IS the index). Non-clustered index is a separate structure with pointers to heap rows. PostgreSQL has heap tables with separate indexes (non-clustered). MySQL InnoDB uses clustered B-Tree on primary key — secondary indexes store the primary key value to look up the row.

**Q: When would you use a read replica vs a cache?**
A: Read replica for: complex queries that need fresh data, reporting/analytics, reducing load on primary. Cache (Redis) for: sub-millisecond response requirement, very hot/repeated reads, pre-computed results. Use both together — cache hot queries; read replicas for cold analytical queries.

**Q: How do you handle a database migration with zero downtime?**
A: (1) Expand: add new column as nullable; (2) Backfill: populate new column in background batches; (3) Deploy: new code reads new column, old code reads old column; (4) Contract: remove old column after all services migrated. Never make breaking schema changes in a single deploy.

**Q: What is the N+1 query problem and how do you fix it?**
A: Fetching a list of N objects then making one DB query per object = N+1 queries total. Fix with: JOIN (fetch together), batch fetch (`WHERE id IN (...)`), or DataLoader pattern (batches requests across a request cycle). ORMs often cause this with lazy loading.

**Q: How does PostgreSQL handle transactions that read data modified by concurrent transactions?**
A: Using MVCC (Multi-Version Concurrency Control) — each transaction sees a snapshot of data as it was at transaction start. Writers don't block readers. Read committed = see committed changes at each statement. Repeatable read = consistent snapshot for entire transaction. Serializable = full isolation (SSI algorithm in PostgreSQL).
