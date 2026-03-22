# 📋 HLD Cheatsheet — System Design Quick Reference

---

## 🧭 HLD Interview Framework (Use Every Time)

```
1. CLARIFY     → Functional requirements, non-functional (scale, latency, availability)
2. ESTIMATE    → DAU, QPS, storage, bandwidth
3. API DESIGN  → Core endpoints (REST or event-based)
4. DATA MODEL  → Tables/documents, key fields, relationships
5. HIGH-LEVEL  → Draw components: client → LB → service → DB → cache
6. DEEP DIVE   → Bottlenecks, scaling, consistency, failure modes
7. TRADEOFFS   → CAP, SQL vs NoSQL, sync vs async, push vs pull
```

---

## 📐 Back-of-Envelope Estimations

### Key Numbers to Memorise
| Metric | Value |
|--------|-------|
| 1 million users (DAU) | 10^6 |
| 1 billion users (DAU) | 10^9 |
| Seconds in a day | ~86,400 ≈ 10^5 |
| Seconds in a month | ~2.6 × 10^6 |
| 1 KB | 10^3 bytes |
| 1 MB | 10^6 bytes |
| 1 GB | 10^9 bytes |
| 1 TB | 10^12 bytes |
| RAM read latency | ~100 ns |
| SSD read latency | ~100 µs |
| HDD read latency | ~10 ms |
| Network round trip (same DC) | ~0.5 ms |
| Network round trip (cross-region) | ~150 ms |

### QPS Estimation Formula
```
QPS = DAU × avg_requests_per_day / 86,400
Peak QPS = QPS × 2–3 (traffic spike multiplier)

Example: 10M DAU, 10 requests/day
QPS = 10M × 10 / 86,400 ≈ 1,160 QPS
Peak QPS ≈ 3,000 QPS
```

### Storage Estimation Formula
```
Storage/day = DAU × avg_data_per_user_per_day
Total storage = Storage/day × 365 × years_to_retain

Example: 1M DAU, 1 tweet = 300 bytes, 5 tweets/day
Storage/day = 1M × 5 × 300 = 1.5 GB/day
5-year storage = 1.5 × 365 × 5 ≈ 2.7 TB
```

---

## 🏗️ Core Building Blocks

### Load Balancer
- **What**: Distributes incoming traffic across multiple servers
- **Algorithms**: Round Robin, Weighted RR, Least Connections, IP Hash, Consistent Hash
- **Layer 4 vs Layer 7**: L4 = TCP/UDP level (faster); L7 = HTTP level (smarter, can route by path/header)
- **Use**: Eliminate single point of failure; horizontal scaling; health checks
- **Tradeoff**: Adds latency hop; needs HA itself (active-active or active-passive)

### Horizontal vs Vertical Scaling
| | Vertical (Scale Up) | Horizontal (Scale Out) |
|--|--------------------|-----------------------|
| What | Bigger machine | More machines |
| Limit | Hardware ceiling | Near-infinite |
| Cost | Expensive | Cheaper at scale |
| Downtime | Yes (usually) | No (rolling deploy) |
| State | Easy (single machine) | Stateless required |
| **Use when** | DB primary, low complexity | Web/app tier, stateless services |

### Caching
| Layer | Tool | Use Case | TTL Strategy |
|-------|------|----------|-------------|
| CDN | CloudFront, Cloudflare | Static assets, geo-distributed | Long TTL (days) |
| Reverse proxy | Nginx, Varnish | Cacheable API responses | Medium TTL (minutes) |
| App cache | Redis, Memcached | Session, computed results, hot data | Short TTL (seconds–minutes) |
| DB cache | Query cache, materialized views | Expensive queries | Invalidate on write |

**Cache Patterns**:
- **Cache-Aside (Lazy)**: App checks cache → miss → read DB → write cache. Simple, risk of stale data.
- **Write-Through**: Write to cache + DB together. Consistent, but write latency doubles.
- **Write-Behind (Write-Back)**: Write to cache only, async flush to DB. Fast writes, risk of data loss.
- **Read-Through**: Cache handles DB read on miss. Transparent to app, cache vendor manages it.

**Cache Eviction**: LRU (most common), LFU, FIFO, TTL-based

**Cache Problems**:
- **Cache Stampede**: Many requests hit DB simultaneously on cache miss → use mutex/lock or probabilistic early expiry
- **Cache Penetration**: Query for non-existent keys → cache null values or use Bloom filter
- **Cache Avalanche**: Many keys expire at once → jitter TTLs, use consistent hashing

### Message Queues / Event Streaming
| | Message Queue (Kafka, RabbitMQ) | Sync REST |
|--|--------------------------------|-----------|
| Coupling | Loose | Tight |
| Reliability | High (durable) | Depends on retry |
| Latency | Higher | Lower |
| Throughput | Very high | Limited |
| Use | Async tasks, event sourcing, fan-out | Real-time request/response |

**Kafka key concepts**: Topics → Partitions → Consumer Groups → Offsets
- Partition key determines which partition → ordering guaranteed within partition
- Consumer group = parallel consumers, each partition consumed by one member

### Database Choices
| Type | Examples | Use When | Avoid When |
|------|---------|---------|-----------|
| Relational SQL | PostgreSQL, MySQL | ACID, complex queries, strong schema | Massive horizontal scale |
| Document | MongoDB | Flexible schema, nested data, rapid iteration | Complex joins needed |
| Key-Value | Redis, DynamoDB | Session, cache, simple lookups | Complex queries |
| Wide-Column | Cassandra, HBase | Time-series, write-heavy, massive scale | Complex joins, ACID |
| Graph | Neo4j | Relationships, social graphs, recommendations | Non-graph data |
| Search | Elasticsearch | Full-text search, log analytics | Primary data store |
| Time-Series | InfluxDB, TimescaleDB | Metrics, monitoring, IoT | General-purpose |

---

## 🔑 CAP Theorem

```
A distributed system can only guarantee 2 of 3:
  C — Consistency  (all nodes see same data at same time)
  A — Availability  (every request gets a response)
  P — Partition Tolerance  (system works despite network failures)

Since P (network partition) is unavoidable in distributed systems:
  Choose CA → traditional RDBMS (single node, no partition)
  Choose CP → Zookeeper, HBase, MongoDB (strict consistency)
  Choose AP → Cassandra, DynamoDB, CouchDB (eventual consistency)
```

**PACELC Extension**: Even without partition, tradeoff between Latency and Consistency.

---

## 🔄 Consistency Models

| Model | Guarantee | Example |
|-------|-----------|---------|
| Strong Consistency | Read always sees latest write | RDBMS, Zookeeper |
| Eventual Consistency | Reads converge to latest eventually | DynamoDB, Cassandra |
| Read-Your-Writes | User sees own writes immediately | User profile updates |
| Monotonic Read | No going back to older value | Social feed |
| Causal Consistency | Causally related ops seen in order | Chat messages |

---

## 🌐 API Design Patterns

### REST Best Practices
```
GET    /users/{id}           → get user
POST   /users                → create user
PUT    /users/{id}           → full update
PATCH  /users/{id}           → partial update
DELETE /users/{id}           → delete user
GET    /users/{id}/orders    → nested resource

Status codes: 200 OK, 201 Created, 400 Bad Request,
              401 Unauthorized, 403 Forbidden, 404 Not Found,
              429 Too Many Requests, 500 Internal Server Error
```

### Pagination Patterns
| Type | How | Use When |
|------|-----|---------|
| Offset | `?page=2&limit=20` | Small datasets, random access |
| Cursor | `?cursor=abc&limit=20` | Large/realtime datasets, no gaps |
| Keyset | `?after_id=100&limit=20` | Sorted by unique key, fast |

---

## 📊 Database Scaling Patterns

### Read Replicas
- Primary handles writes; replicas handle reads
- Replication lag = eventual consistency on reads
- Read:Write ratio justifies replicas (typically 10:1+)

### Database Sharding
```
Strategies:
  Range-based  → shard by user_id range (risk: hot shards)
  Hash-based   → hash(user_id) % num_shards (even distribution, hard to reshard)
  Directory    → lookup table maps key → shard (flexible, lookup overhead)
  Geo-based    → shard by region (compliance, latency)

Problems: Cross-shard queries, joins, transactions, rebalancing
Solution: Consistent hashing minimises data movement on reshard
```

### Consistent Hashing
- Nodes and keys placed on a ring
- Key maps to nearest clockwise node
- Adding/removing node moves only `K/N` keys (K = total keys, N = nodes)
- Virtual nodes: each physical node has multiple ring positions for even distribution

---

## 🔐 Rate Limiting Algorithms

| Algorithm | How | Advantage | Tradeoff |
|-----------|-----|-----------|---------|
| Token Bucket | Tokens refill at fixed rate; consume per request | Allows bursts | Complex to distribute |
| Leaky Bucket | Requests drain at fixed rate | Smooth output | Drops bursts |
| Fixed Window Counter | Count per time window | Simple | Edge burst at window boundary |
| Sliding Window Log | Store timestamps of requests | Accurate | High memory |
| Sliding Window Counter | Blend of fixed windows | Low memory + accurate | Approximate |

---

## 📡 Communication Patterns

| Pattern | Protocol | Use | Latency | Scalability |
|---------|---------|-----|---------|------------|
| Request-Response | REST/gRPC | CRUD, queries | Low | Good |
| Long Polling | HTTP | Notifications, chat | Medium | Poor (held connections) |
| WebSocket | WS | Real-time, trading, games | Very low | Needs sticky sessions |
| SSE | HTTP | Server push, feeds | Low | Good |
| Message Queue | Kafka/RMQ | Async processing | Higher | Excellent |
| gRPC | HTTP/2 | Microservice-to-microservice | Very low | Excellent |

---

## ⚖️ Key Trade-offs to Always Mention

| Decision | Option A | Option B | Key Factor |
|----------|---------|---------|-----------|
| Storage | SQL | NoSQL | Schema flexibility vs ACID |
| Consistency | Strong | Eventual | User experience vs availability |
| Processing | Sync | Async | Latency vs throughput |
| Caching | Write-through | Write-behind | Consistency vs write speed |
| Scaling | Vertical | Horizontal | Cost vs ceiling |
| Communication | REST | Message Queue | Coupling vs reliability |
| Data fetch | Push | Pull | Freshness vs load |
| Search | DB query | Elasticsearch | Simplicity vs capability |

---

## 🔢 Numbers Every Interviewer Expects

| System Component | Typical Numbers |
|-----------------|----------------|
| Single DB (PostgreSQL) | 10K–50K QPS reads; 5K–10K QPS writes |
| Redis | 100K–1M QPS |
| Kafka | 1M+ messages/sec per cluster |
| CDN cache hit ratio | 90–95% |
| Single server (8 core) | ~10K concurrent connections |
| DB connection pool | 100–200 connections per instance |
| JWT token expiry | Access: 15 min; Refresh: 7 days |
| Image size (compressed) | ~300 KB avg |
| Video size (1 min, 720p) | ~50 MB |
