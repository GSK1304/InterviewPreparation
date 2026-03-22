# 📚 HLD Deep Dive — Fundamentals

---

## 🧠 What is System Design?

System design is the process of defining architecture, components, modules, interfaces, and data flow for a system to satisfy specified requirements. In interviews, it tests your ability to think at scale, make trade-offs, and communicate architectural decisions.

---

## 1. Scalability

### Vertical Scaling (Scale Up)
- Add more CPU, RAM, or storage to existing machine
- **Advantage**: No code changes; simpler; no distributed system problems
- **Tradeoff**: Hardware ceiling; expensive; single point of failure; downtime during upgrade
- **Use**: DB primary nodes, small-scale systems, stateful services

### Horizontal Scaling (Scale Out)
- Add more machines, distribute load across them
- **Advantage**: Near-infinite scale; fault tolerant; cost-effective at scale
- **Tradeoff**: Requires stateless services; distributed system complexity; data consistency harder
- **Use**: Web/app tier, microservices, stateless services

### Stateless vs Stateful Services
```
Stateless: No session data stored on server
  → Any request can go to any server
  → Easy to scale horizontally
  → Store state in shared cache (Redis) or DB

Stateful: Session data tied to specific server
  → Requires sticky sessions (IP hash) or session replication
  → Harder to scale; single server failure loses session
  → Use: WebSocket connections, real-time trading (kACE)
```

---

## 2. Availability & Reliability

### Availability Formula
```
Availability = Uptime / (Uptime + Downtime)

99%    = 3.65 days downtime/year  ("two nines")
99.9%  = 8.77 hours/year          ("three nines")
99.99% = 52.6 minutes/year        ("four nines")
99.999% = 5.26 minutes/year       ("five nines")
```

### High Availability Patterns
- **Active-Active**: Multiple instances serve traffic simultaneously. If one fails, others absorb load.
- **Active-Passive**: Standby takes over on failure. Simpler but wastes resources.
- **Multi-AZ**: Deploy across Availability Zones. Survives data center failure.
- **Multi-Region**: Deploy across regions. Survives regional outages. Complex for consistency.

### Redundancy at Every Layer
```
Client → [CDN (multi-PoP)] → [Load Balancer (active-active pair)]
       → [App Servers (N instances, auto-scaling)]
       → [Message Queue (Kafka, replication factor 3)]
       → [DB (Primary + Read Replicas + cross-region standby)]
       → [Cache (Redis Cluster with replicas)]
```

---

## 3. Latency vs Throughput

```
Latency  = Time for ONE request to complete (ms)
Throughput = Number of requests system handles per second (QPS/RPS/TPS)

Improving throughput often increases latency (batching)
Improving latency often reduces throughput (immediate processing)

p50 latency = median (50th percentile)
p95 latency = 95% of requests faster than this
p99 latency = 99% of requests faster than this (tail latency)
```

**Key latency numbers**:
```
L1 cache access:     ~0.5 ns
RAM access:          ~100 ns
SSD read:            ~100 µs  (1,000x slower than RAM)
Network (same DC):   ~0.5 ms
HDD read:            ~10 ms   (100x slower than SSD)
Network (cross DC):  ~50 ms
Network (cross globe): ~150 ms
```

---

## 4. Load Balancing

### Algorithms
| Algorithm | How | Use When |
|-----------|-----|---------|
| Round Robin | Rotate through servers in order | Homogeneous servers, stateless |
| Weighted RR | More traffic to more powerful servers | Heterogeneous servers |
| Least Connections | Route to server with fewest active connections | Long-lived connections |
| IP Hash | Hash client IP → sticky to same server | Stateful, session affinity |
| Consistent Hash | Minimal reshuffling when servers added/removed | Cache servers, sharded DBs |
| Random | Random server selection | Simple, good distribution |

### L4 vs L7 Load Balancer
```
L4 (Transport layer — TCP/UDP):
  - Routes by IP + port only
  - Faster (no packet inspection)
  - Cannot route by URL path, header, or cookie
  - Use: Simple TCP services, DNS load balancing

L7 (Application layer — HTTP/HTTPS):
  - Routes by URL, header, cookie, content
  - Can do SSL termination, content-based routing, A/B testing
  - Slightly slower (parses HTTP)
  - Use: Microservices routing, API gateways
```

### Health Checks
```
Active health check: LB sends periodic ping to servers
  → Removes unhealthy servers within seconds
  → Adds back when healthy again

Passive health check: LB monitors real traffic errors
  → Slower detection; no extra load
```

---

## 5. Caching

### Cache Hit Ratio
```
Cache Hit Ratio = hits / (hits + misses)
Target: > 80% (ideally 90%+)
Low hit ratio = cache is not helping much → review key design

Effective cache = hot data is small subset of total data (80/20 rule)
```

### Cache Patterns Deep Dive

**Cache-Aside (Lazy Loading)**:
```
Read:  App → check cache → HIT: return; MISS: read DB → write cache → return
Write: App → write DB → (optionally) invalidate or update cache

Advantage: Only caches what's actually read; fault tolerant (cache miss = read DB)
Tradeoff: Cache miss = 3 operations (read cache + read DB + write cache); stale data window
Use: Most common pattern; read-heavy workloads
```

**Write-Through**:
```
Write: App → write cache + write DB (synchronously)
Read:  App → read cache (always fresh)

Advantage: Cache always consistent with DB; no stale data
Tradeoff: Write latency doubles; cache fills with infrequently-read data
Use: Write + read both frequent; consistency critical
```

**Write-Behind (Write-Back)**:
```
Write: App → write cache → return (async flush to DB later)
Read:  App → read cache

Advantage: Very fast writes; batch DB writes for efficiency
Tradeoff: Data loss if cache fails before flush; complex implementation
Use: High write throughput with acceptable durability risk
```

### Cache Invalidation Strategies
- **TTL (Time-To-Live)**: Expire after fixed time. Simple. Stale data possible.
- **Event-driven**: Invalidate on write event (publish to cache-invalidation topic). Precise. Complex.
- **Version key**: Append version to cache key (`user:123:v5`). Read new version on update. No invalidation needed.

---

## 6. Database Fundamentals

### ACID Properties
```
A — Atomicity: All or nothing (transaction commits fully or rolls back)
C — Consistency: DB always moves from valid state to valid state
I — Isolation: Concurrent transactions don't interfere
D — Durability: Committed data survives failures (WAL, replication)
```

### BASE (NoSQL philosophy)
```
BA — Basically Available: Always responds, may return stale data
S  — Soft state: State may change over time without new input
E  — Eventual consistency: Will become consistent given enough time
```

### Read Replicas
```
Architecture:
  Primary (writes) → replication log → Replica 1 (reads)
                                     → Replica 2 (reads)
                                     → Replica 3 (reads)

Replication lag: time delay between write on primary and visibility on replica
  Synchronous: Write confirmed only after replica acks. Strong consistency. Slower.
  Asynchronous: Write confirmed immediately. Faster. Replication lag possible.

Use case: Read:Write ratio > 3:1; reporting queries; geographic distribution
```

### Sharding Strategies
```
Range Sharding:
  user_id 1-1M → Shard 1
  user_id 1M-2M → Shard 2
  Problem: Hot shards if distribution is uneven

Hash Sharding:
  shard = hash(user_id) % num_shards
  Advantage: Even distribution
  Problem: Reshard requires remapping all keys

Consistent Hashing:
  Keys and nodes on circular ring
  Key maps to nearest clockwise node
  Add node: only K/N keys move (K=total keys, N=nodes)
  Virtual nodes: multiply ring positions per physical node → even balance

Directory-based Sharding:
  Lookup service maps key → shard
  Most flexible; single point of failure for lookup service
```

---

## 7. Microservices Communication

### Synchronous (Request-Response)
```
REST (HTTP):
  + Simple, widely understood, easy to debug
  + Good tooling, browser-native
  - Tight coupling, both services must be up, no retry-by-default

gRPC (HTTP/2 + Protocol Buffers):
  + 5-10x faster than REST (binary + multiplexing)
  + Strong typing, auto-generated clients
  + Streaming support
  - More complex setup, binary format hard to debug
  Use: Internal microservice communication (kACE pricing → gateway)
```

### Asynchronous (Event-Driven)
```
Message Queue (Kafka, RabbitMQ):
  + Loose coupling, services independent
  + High throughput, buffering, replay
  + Multiple consumers (fan-out)
  - Higher latency, eventual consistency, complex error handling

Kafka specifics:
  - Partitions enable parallelism (more partitions = more consumers)
  - Consumer group: each partition read by exactly one member
  - Offset commit: at-least-once vs exactly-once delivery
  - Retention: messages replayable within retention period
```

---

## 🌍 Real-World Use Cases (kACE Context)

### Use Case 1: kACE Pricing Service Architecture
```
Trader UI (React 19)
    ↓ WebSocket (STOMP)
API Gateway (Spring Boot + JWT validation)
    ↓
Pricing Service (stateless, horizontally scaled)
    ↓                    ↓
Kafka (RFQ events)    Redis (dropdown cache, session)
    ↓
Market Data Service → DB (PostgreSQL)
```

### Use Case 2: Dropdown Cache Strategy
- Cache-Aside pattern: Load ~200 dropdowns at startup into Redis
- TTL: 1 hour (data changes rarely)
- Warm-up: `StaticCacheOrchestrator` pre-loads on service start
- Fallback: Cache miss → DB read → re-cache

### Use Case 3: WebSocket Scaling
- Problem: WebSocket = stateful connection → can't route to any server
- Solution: Sticky sessions (IP hash at LB) OR shared subscription registry in Redis
- kACE approach: `SubscriptionRegistry` in shared Redis cluster

---

## 🏋️ Practice Questions

1. Design Twitter/X (news feed, followers, trending topics)
2. Design a URL shortener (tinyurl.com)
3. Design WhatsApp / Chat system
4. Design YouTube (video upload, streaming, recommendations)
5. Design a ride-sharing service (Uber/Ola)
6. Design a notification system (push, email, SMS)
7. Design an FX Options trading platform (kACE-style)
8. Design a rate limiter
9. Design a web crawler
10. Design a distributed cache (Redis-like)
