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

---

## 🤖 AI/ML Quick Reference

### RAG in One Paragraph
Embed documents into vectors → store in vector DB → on query: embed query → find K nearest chunks (ANN search) → optionally re-rank → inject top chunks into LLM prompt → generate grounded answer. Always use the same embedding model for indexing and querying.

### LLM Key Numbers
| Metric | Value |
|--------|-------|
| Token ≈ | 4 chars / ¾ of a word |
| 1K words ≈ | 1,300 tokens |
| GPT-4o input | $2.50/1M tokens |
| GPT-4o output | $10.00/1M tokens (4× input) |
| TTFT target | < 500ms |
| Generation speed target | > 20 tokens/sec |
| Temperature for code | 0.0–0.2 |
| Temperature for chat | 0.7 |

### Vector DB Selection
| Need | Choose |
|------|--------|
| Managed, quickest start | Pinecone |
| Self-hosted + hybrid search | Weaviate / Qdrant |
| Already on Postgres, small scale | pgvector |
| Billion-scale | Milvus |
| Prototyping | Chroma |

### LLM Cost Reduction Priority
1. Prompt caching (50% off cached tokens) — zero code change
2. Model routing (cheaper model for simple queries) — 40–70% saving
3. Semantic cache (same question → cached answer) — 20–40% hit rate
4. Batch API for non-real-time — 50% off
5. Self-hosted vLLM for high volume — 3–10× cheaper than API

---

## 🔒 SSE/WebSocket Multi-Instance Cheat Sheet

```
Problem: Client's persistent connection lives on Instance 1.
         Event fires on Instance 2. How does Instance 2 push to client?

Solution: Redis Pub/Sub
  Instance 2: PUBLISH channel:{resourceId}  {event_data}
  Instance 1: subscribed to channel:{resourceId} → receives → pushes to client's socket

Alternative: Sticky sessions (IP hash at LB) — simpler but fails on crash
Alternative: Dedicated gateway (Pusher, Socket.io, AWS API Gateway WS)
Alternative: Consistent hashing — resource routed to same instance always

Interview answer: "Redis Pub/Sub decouples event generation from connection
ownership — any instance publishes, the owning instance delivers."
```

---

## 🗺️ HLD Deep-Dive Coverage Map

| System Design | File | Key Pattern |
|---------------|------|-------------|
| URL Shortener | 02 | Base62 + Snowflake ID + Redis cache |
| Messaging (WhatsApp) | 03 | Kafka routing + Cassandra + fan-out |
| Auth/JWT | 04 | 3-token + RSA + JWKS + familyId |
| FX Trading (kACE) | 05 | WebSocket STOMP + Redis SubscriptionRegistry |
| Rate Limiter | 06 | Sliding window counter + Redis Lua |
| Notification | 07 | Multi-channel + Kafka priority + webhook signing |
| YouTube/Netflix | 13 | HLS/DASH + CDN segments + async view count |
| Twitter Feed | 14 | Fan-out hybrid (write small, read celebrity) |
| Uber | 15 | Redis GEORADIUS + matching + surge pricing |
| Google Drive | 16 | Content-addressed chunks + delta sync |
| Autocomplete | 17 | Trie top-K pre-stored + Redis sorted set |
| Distributed Cache | 18 | Redis internals + PagedAttention concept |
| Web Crawler | 19 | Two-queue frontier + Bloom filter dedup |
| Kafka | 20 | Log-based storage + ISR + consumer groups |
| Payment Gateway | 21 | Idempotency atomic with DB + double-entry |
| Proximity/Yelp | 22 | Redis GEORADIUS + two-step query |
| Recommendation | 23 | ALS + Wide&Deep + Feature Store |
| Food Delivery | 24 | Order state machine + dispatch + Redis Pub/Sub |
| Google Docs | 25 | OT algorithm + Redis stateless approach |
| Stock Exchange | 26 | TreeMap order book + single-threaded + WAL |
| Zoom | 27 | SFU vs MCU + WebRTC + Simulcast |
| CI/CD | 28 | Canary deployment + container isolation |
| Distributed Lock | 29 | Redis SETNX + Lua + Redlock + fencing token |
| Reddit/Quora | 30 | Hot score algorithm + vote aggregation |
| Google Maps | 31 | Contraction hierarchies + probe data traffic |
| How LLMs Work | 32 | Tokens + Transformer + hallucination mitigations |
| RAG | 33 | Chunk → embed → ANN → rerank → generate |
| LLM Inference | 34 | KV cache + PagedAttention + continuous batching |
| LLM Billing | 35 | Token metering + Redis budget + Clickhouse |
