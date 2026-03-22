# ⚡ HLD Quick Revision — Read the Night Before

> Target: 45 minutes. One read-through.

---

## 1. The Framework — Use This Every Interview

**CANES**:
- **C**larify → functional + non-functional requirements
- **A**rchitecture → high-level diagram (LB → Service → DB → Cache)
- **N**umbers → QPS, storage, bandwidth estimates
- **E**xpand → deep dive on hardest component
- **S**cale → how to handle 10x growth + failure modes

**Non-functional requirements to always ask about**:
- Scale (DAU, QPS)
- Latency (p99 acceptable?)
- Availability (99.9% vs 99.99%?)
- Consistency (eventual OK? or strong needed?)
- Durability (data loss acceptable?)

---

## 2. Estimation Quick Math

```
QPS = DAU × requests/day ÷ 86,400
Peak = QPS × 3
Storage/day = DAU × data_per_user
Bandwidth = QPS × avg_request_size

1M DAU, 10 req/day → ~115 QPS, ~350 peak QPS
10M DAU, 10 req/day → ~1,150 QPS, ~3,500 peak QPS
100M DAU, 10 req/day → ~11,500 QPS, ~35,000 peak QPS
```

---

## 3. Core Components — What, When, Tradeoff

### Load Balancer
- **What**: Distributes traffic, eliminates SPOF, enables horizontal scaling
- **L4 vs L7**: L4=TCP/IP faster; L7=HTTP smarter (route by path, header, cookie)
- **Algorithms**: Round Robin, Least Connections, IP Hash, Consistent Hash
- **Tradeoff**: Adds a hop; LB itself needs HA (active-active pair)

### Cache (Redis/Memcached)
- **Cache-Aside**: App reads cache → miss → reads DB → writes cache. Most common.
- **Write-Through**: Write cache + DB simultaneously. Consistent, slower writes.
- **Write-Behind**: Write cache only, async flush. Fast writes, risk data loss.
- **Eviction**: LRU most common. TTL for time-sensitive data.
- **Problems**: Stampede (use mutex), Penetration (cache null or Bloom filter), Avalanche (jitter TTLs)

### Message Queue (Kafka)
- **When**: Decouple producer/consumer; async processing; fan-out to multiple consumers; event sourcing
- **Kafka concepts**: Topic → Partitions → Consumer Groups → Offsets
- **Ordering**: Guaranteed within a partition only (use partition key)
- **Tradeoff vs REST**: Higher latency but higher throughput + reliability + loose coupling

### Database
- **SQL**: ACID, complex queries, strong schema. Scale with read replicas + sharding.
- **NoSQL**: Scale horizontally, flexible schema, eventual consistency. Cassandra/DynamoDB for write-heavy.
- **Redis**: Cache + session + pub-sub + sorted sets for leaderboards.
- **Elasticsearch**: Full-text search, log analytics — never primary store.

### CDN
- **What**: Edge servers cache static content close to users
- **Use**: Images, videos, JS/CSS files, API responses with long TTL
- **Tradeoff**: Eventual consistency, harder cache invalidation, cost

---

## 4. CAP Theorem — Always Mention

```
P (Partition Tolerance) is unavoidable → always choose CP or AP

CP (Consistency over Availability): HBase, MongoDB, Zookeeper
  → "We prefer returning an error over stale data"

AP (Availability over Consistency): Cassandra, DynamoDB, Couchbase
  → "We prefer returning possibly stale data over returning nothing"
```

---

## 5. Scaling Patterns

### Read-Heavy System
```
Client → CDN → Load Balancer → App Servers (horizontal)
                                    ↓
                              Redis Cache ← cache-aside
                                    ↓ (cache miss)
                              DB Primary + Read Replicas
```

### Write-Heavy System
```
Client → Load Balancer → App Servers
                              ↓
                         Kafka (buffer writes)
                              ↓
                    Write Workers → DB (Cassandra/DynamoDB)
```

### General Pattern
```
Add caching before scaling DB
Add read replicas before sharding
Shard only when replicas aren't enough
Use async (message queues) before adding more sync capacity
```

---

## 6. Database Sharding

- **Range-based**: shard by ID range. Simple but hot spots.
- **Hash-based**: `hash(id) % N`. Even distribution but hard to reshard.
- **Consistent hashing**: keys + nodes on a ring. Minimal resharding (K/N keys moved).
- **Virtual nodes**: each physical node owns multiple ring positions. Balances heterogeneous hardware.

**Problems with sharding**: Cross-shard joins, distributed transactions, rebalancing complexity.

---

## 7. Rate Limiting

| Algorithm | Key trait | Best for |
|-----------|-----------|---------|
| Token Bucket | Allows bursts up to bucket size | APIs with burst tolerance |
| Leaky Bucket | Smooth fixed-rate output | Systems needing predictable throughput |
| Sliding Window Log | Most accurate | Low-volume, accuracy critical |
| Sliding Window Counter | Low memory + accurate | General purpose |

**Where to implement**: API Gateway (globally), Nginx (per-instance), Redis (distributed, token bucket)

---

## 8. Consistency Models

- **Strong**: Every read sees latest write. Expensive, slower. Use for: banking, inventory.
- **Eventual**: Reads converge eventually. Fast, available. Use for: social feeds, analytics.
- **Read-Your-Writes**: User always sees their own changes. Session consistency. Use for: profile updates.
- **Causal**: Causally related ops in order. Use for: chat messages, comments.

---

## 9. Communication Patterns

- **REST**: Simple CRUD. Synchronous. Easy to debug. Use for: public APIs.
- **gRPC**: Binary + HTTP/2 + streaming. Very fast. Use for: microservice-to-microservice.
- **WebSocket**: Bi-directional persistent connection. Use for: trading UI, chat, live scores.
- **Kafka/MQ**: Async, decoupled, durable. Use for: event processing, notifications, fan-out.
- **SSE**: Server push over HTTP. Use for: live feeds, notifications (simpler than WebSocket).

---

## 10. Common System Design Patterns

### Fan-out on Write vs Fan-out on Read
- **Write**: When a user posts, pre-compute and push to all followers' feeds. Fast reads, slow writes, bad for celebrities.
- **Read**: Merge feeds at read time. Slow reads, fast writes. Used for users with many followers (hybrid approach).

### CQRS (Command Query Responsibility Segregation)
- Separate write model (commands) from read model (queries)
- Write path: normalised DB for integrity; Read path: denormalised views for speed
- Use when: read/write patterns are very different; reporting needs different shape than operational data

### Event Sourcing
- Store all events (facts) rather than current state
- State = replay of all events
- Use when: audit trail required; temporal queries needed; undo/redo needed

### Saga Pattern (Distributed Transactions)
- Long-running transactions across microservices
- **Choreography**: each service publishes events, others react
- **Orchestration**: central saga orchestrator calls each service
- Use instead of: 2-phase commit (too slow/brittle for microservices)

---

## 11. Failure Modes to Always Address

- **Single Point of Failure (SPOF)**: Everything needs redundancy (LB pairs, DB replicas, multi-AZ)
- **Cascading failures**: Circuit breaker pattern — fail fast, stop propagation
- **Hot spots**: Consistent hashing, virtual nodes, request routing by affinity
- **Data loss**: Write-ahead logging, replication factor ≥ 3, cross-region backups
- **Network partition**: Design for it — idempotent operations, retry with exponential backoff

---

## 12. Security Basics (Mention in Every Design)

- **Authentication**: JWT (stateless) vs Sessions (stateful). JWT for microservices.
- **Authorization**: RBAC (role-based) or ABAC (attribute-based)
- **Data in transit**: TLS everywhere
- **Data at rest**: Encryption at storage level
- **Rate limiting**: Prevent abuse and DDoS
- **Input validation**: At API gateway and service level

---

## 🎯 HLD Interview Anti-patterns (Don't Do These)

1. **Jump to solution without clarifying** — always clarify first
2. **Skip estimations** — always do rough QPS and storage math
3. **One-size-fits-all DB** — justify your DB choice
4. **Ignore failure modes** — always address SPOF and cascading failures
5. **Over-engineer upfront** — start simple, scale on demand
6. **Forget CAP** — always mention consistency vs availability tradeoff
7. **No API design** — define at least 3–4 key endpoints
8. **Ignore caching** — mention cache at every layer (CDN, app, DB)

---

## 13. AI/ML Quick Revision

### RAG Pipeline (memorise this)
```
Offline: Document → Chunk (256-512 tokens, 10% overlap) → Embed (same model always!)
         → Vector DB (HNSW index, cosine similarity)

Online:  Query → Embed → ANN search (top-20) → Re-rank (top-5)
         → Prompt: "Answer using ONLY context below:" + chunks + query
         → LLM → grounded response
```

### When RAG vs Fine-tuning
- **RAG**: knowledge changes, need citations, large knowledge base → use RAG
- **Fine-tuning**: need different behaviour/style, domain-specific reasoning → fine-tune
- **Both**: fine-tune for behaviour + RAG for knowledge (most production systems)

### LLM Inference Key Terms
- **TTFT** (Time To First Token): optimise this for streaming UX — target < 500ms
- **KV Cache**: stored K/V matrices for past tokens → O(n) decode not O(n²)
- **PagedAttention** (vLLM): virtual memory for KV cache → 2-4× throughput
- **Continuous batching**: fill GPU batch as requests complete → 5-10× throughput
- **Speculative decoding**: draft model generates N tokens, large model verifies → 2-3× speedup

### LLM Billing Formula
```
cost = (input_tokens × input_price) + (output_tokens × output_price)
     + (cached_tokens × cached_price)    ← 50% of input price

output tokens cost 4-10× more than input tokens
At scale → proxy → Kafka → Redis budget check → Clickhouse attribution
```

---

## 14. SSE/WebSocket Multi-Instance (Read Before Every Interview)

> "How does the backend know where to push data when there are multiple instances?"

This is asked in EVERY interview involving real-time features. Answer confidently:

```
WRONG assumption: any instance can push to any client
RIGHT answer: each client's connection lives on ONE instance

Solution: Redis Pub/Sub
  When event fires on any instance:
    PUBLISH channel:{userId_or_resourceId}  {event}
  
  All instances subscribe to relevant channels:
    Instance with the client's socket receives message → pushes to client
    Other instances receive message → no socket for this client → ignore

Channel patterns:
  Per user:      channel:user:{userId}
  Per order:     channel:order:{orderId}
  Per resource:  channel:trip:{tripId}

Used by: Discord, Slack, Socket.io Redis adapter, kACE SubscriptionRegistry
```

Alternatives: sticky sessions (simpler but fails on crash), dedicated gateway (Pusher/Ably), consistent hashing routing.

---

## 🎯 Top 10 Most Common Interview Mistakes

1. **Jump to architecture without clarifying** — always ask scale, consistency, latency first
2. **Skip estimation** — always do QPS and storage math, even rough
3. **Don't justify DB choice** — always explain WHY SQL vs NoSQL
4. **Ignore failure modes** — for every component, say what happens when it goes down
5. **Over-engineer** — start simple, evolve; don't design for 100× on day one
6. **Forget caching** — mention cache at every layer (CDN, app cache, DB cache)
7. **Treat WebSocket as stateless** — always address multi-instance push problem
8. **Ignore idempotency** — for any write operation, say how you handle retries safely
9. **Miss the hot spot** — identify which component becomes the bottleneck first
10. **No API design** — define at least 3 endpoints before drawing any boxes

---

## 📚 What to Read the Night Before

In order of priority:
1. This file (HLD-quick.md) — all sections
2. `12-Communication-Patterns.md` → Section 5 (SSE/WebSocket multi-instance)
3. The deep-dive for the specific system you expect to be asked
4. `00-cheatsheets/HLD-cheatsheet.md` → numbers table and tradeoffs
5. `03-resources/HLD-Master-QnA.md` → Q39–Q50 (judgment questions)
