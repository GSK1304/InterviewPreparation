# 📋 HLD Master Interview Q&A — 50 Most Common Questions

> These are the questions that appear in FAANG, unicorn startup, and senior engineer interviews repeatedly. Every answer here is a complete, interview-ready response — not just a definition.

---

## Category 1: Fundamentals & Estimation (Q1–Q10)

**Q1: How do you approach a system design problem you've never seen before?**

Use the CANES framework and never jump to solutions:
1. **Clarify** — Ask 3–4 questions: what scale? what latency target? strong or eventual consistency? read:write ratio? Don't assume.
2. **Estimate** — DAU → QPS, storage/day, bandwidth. Do rough math out loud. Even wrong numbers show systematic thinking.
3. **API** — Define 3–4 core endpoints before drawing any boxes. The API is the contract.
4. **Architecture** — Start with the simplest possible diagram: client → LB → service → DB. Then evolve it.
5. **Deep dive** — Pick the hardest component. Justify every decision. Mention the tradeoff you're not taking.
6. **Scale** — "If this needs to handle 10× traffic tomorrow, what breaks first and how do I fix it?"

The mistake most candidates make is drawing boxes immediately. Interviewers want to see you think, not just recall a pattern.

---

**Q2: How do you estimate QPS and storage for a system you're designing?**

QPS formula: `DAU × actions_per_day / 86,400`. Peak = average × 3.

Storage formula: `DAU × data_per_user_per_day`. Multiply by retention period.

Worked example — design Twitter:
- 500M users, 50M DAU, avg 5 tweets/day
- Write QPS: 50M × 5 / 86,400 ≈ 2,900 TPS. Peak ≈ 9,000 TPS
- Read QPS: assume 100:1 read:write → 290K reads/sec, peak 870K/sec
- Storage: 500M tweets/day × 300 bytes = 150 GB/day. 10-year = 550 TB

Don't be precise — round to nice numbers. The goal is order-of-magnitude accuracy. If your estimate is within 2–3× of reality, that's fine. Interviewers care that you can reason about scale, not that you memorised exact numbers.

---

**Q3: When do you use SQL vs NoSQL?**

Choose SQL when: ACID transactions required (payments, inventory), complex queries with JOINs, schema is stable and well-defined, < 10TB data and < 100K QPS.

Choose NoSQL when: horizontal scale is the primary constraint, schema flexibility needed (semi-structured data), very high write throughput (Cassandra for time-series), simple key-value or document lookups (DynamoDB), eventual consistency is acceptable.

The follow-up interviewers always ask: "But can't you just use SQL with read replicas?" Yes — and that's the right answer for most systems. You only reach for NoSQL when SQL genuinely can't handle the scale or access patterns. Don't use NoSQL to seem sophisticated. Justify it.

---

**Q4: What is the CAP theorem and how does it affect your designs?**

CAP says a distributed system can only guarantee two of: Consistency (all nodes see same data), Availability (always respond), Partition Tolerance (survive network splits). Since partition tolerance is non-negotiable in distributed systems (networks do fail), the real choice is CP vs AP.

CP (strong consistency): HBase, Zookeeper, MongoDB (primary reads). Correct response: return error during partition rather than potentially stale data. Use for: financial systems, inventory, auth.

AP (high availability): Cassandra, DynamoDB, CouchDB. Nodes respond with potentially stale data during partition. Use for: social feeds, analytics, DNS, shopping carts.

The nuance interviewers want: "Most systems use CP or AP for different parts." Cassandra with quorum reads is CP; with eventual reads is AP. You tune it per operation.

---

**Q5: How do you handle database bottlenecks at scale?**

In order of escalation — don't jump to sharding immediately:

1. **Index optimisation** — 90% of slow queries are fixed with the right index. Run EXPLAIN ANALYZE first.
2. **Query optimisation** — N+1 queries, missing covering indexes, SELECT * over the wire.
3. **Connection pooling** — PgBouncer between app and Postgres. Stops connection exhaustion.
4. **Read replicas** — for read-heavy systems (10:1+ read:write). Route reads to replicas.
5. **Caching layer** — Redis cache-aside for hot data. Reduces DB load by 90%+ for frequently read data.
6. **Vertical scaling** — Temporarily, before redesigning.
7. **Sharding** — Last resort. Adds massive operational complexity. Use consistent hashing for future-proofing.

The mistake: skipping straight to sharding. A properly cached and indexed Postgres instance handles 100K QPS easily. Only shard when you genuinely need > 1 write primary's capacity.

---

**Q6: What is consistent hashing and when do you need it?**

Standard hash: `node = hash(key) % N`. Problem: add or remove one node and ~all keys remap. Massive cache invalidation.

Consistent hashing: place nodes and keys on a ring (hash space 0–2³²). Each key maps to the nearest clockwise node. Add/remove one node: only K/N keys move (K=total keys, N=nodes). Virtual nodes (each physical node gets V positions on ring): even distribution even with few nodes. V=150–200 is typical.

Use when: distributed cache (Memcached/Redis cluster), database sharding where you need to add shards online, CDN edge server selection. Don't use for: small fixed clusters where resharding downtime is acceptable, systems with rarely changing node counts.

---

**Q7: How do you design for 99.99% availability?**

99.99% = 52 minutes downtime per year. To achieve it:

- **Eliminate single points of failure**: Every component has N+1 redundancy minimum. LB pairs (active-active), DB primary + replica with auto-failover, multi-AZ deployment.
- **Health checks + auto-recovery**: LB health checks every 10s. Automatic unhealthy instance replacement (auto-scaling group). DB automatic failover (< 30s with Patroni or RDS Multi-AZ).
- **Graceful degradation**: When a dependency is down, degrade gracefully (show cached data, disable non-critical features) rather than returning 500.
- **Circuit breakers**: Stop calls to failing services immediately. Don't pile up threads waiting.
- **Chaos engineering**: Test failure modes in staging before they happen in production. Netflix Chaos Monkey.

The interviewer follow-up: "99.999% is 5 minutes/year. How do you get there?" Active-active multi-region with automated failover. Much harder: distributed transactions, data replication lag.

---

**Q8: What is the difference between latency and throughput? How do you optimise each?**

Latency = time for one request. Throughput = requests handled per second. They trade off: batching increases throughput but increases latency per item.

Optimise latency: caching (skip the slow DB call), async operations (don't wait for non-critical work), connection pooling (skip TCP handshake overhead), CDN (serve from edge near user), smaller data (compress, select only needed fields), faster hardware/network.

Optimise throughput: horizontal scaling (more servers), batching (process multiple items per operation), async processing (decouple producer from consumer via queue), efficient data structures (avoid unnecessary allocations), connection reuse (HTTP keep-alive, connection pools).

The tension: a request that batches 100 items has higher throughput but individual items wait longer (higher latency). Design for the metric your users actually care about. Users feel latency; operators see throughput.

---

**Q9: How do you design a system to handle traffic spikes (e.g., Black Friday)?**

Predictable spikes: pre-scale before the event. K8s HPA + scheduled scaling. Load test to find breaking point. Warm up caches before traffic hits.

Technical approaches:
- **Auto-scaling**: K8s HPA on CPU/memory/queue depth. But: cold start takes 2–5 minutes — too slow for sudden spikes.
- **Pre-scaling**: Scale to expected peak capacity 30 minutes before known event.
- **Rate limiting**: Protect backend from overwhelming traffic. Return 429 gracefully.
- **Queue buffering**: Accept all requests into a queue, process at controlled rate. Users see "estimated wait time" rather than errors.
- **Load shedding**: Shed lowest-priority requests first (analytics, recommendations) to protect core features (checkout, payment).
- **Static content to CDN**: Homepage, product images served from CDN — zero load on origin during spike.
- **Read replicas + caching**: Reads never hit primary DB during spike.

Amazon pre-provisions capacity months before Prime Day. Stripe stress-tests to 2× expected peak. Don't assume auto-scaling alone saves you.

---

**Q10: What are the four golden signals of service health?**

From Google SRE Book:

1. **Latency** — how long requests take. Track p50, p95, p99 (not just average). Alert on p99 exceeding SLO.
2. **Traffic** — requests per second. Establishes normal baseline; anomalies indicate problems.
3. **Errors** — rate of failed requests (5xx). Any non-zero error rate is worth investigating. Alert when > SLO allows.
4. **Saturation** — how "full" the service is. CPU%, memory%, disk I/O%, queue depth. High saturation predicts imminent failures.

Why these four: Latency catches slow degradation before it becomes an error. Traffic catches attack patterns or organic spikes. Errors catch bugs and dependencies failing. Saturation catches impending exhaustion. Together they cover almost all production issues.

---

## Category 2: Databases & Storage (Q11–Q20)

**Q11: How does a B-Tree index work and when does it not help?**

B-Tree organises indexed values in a balanced tree. Leaf nodes contain actual values (sorted), inner nodes contain routing keys. Queries traverse from root to leaf: O(log N). Leaf nodes linked in sorted order — enables efficient range scans.

Doesn't help for: `WHERE LOWER(email) = ?` (function on column destroys index — use a functional index), `WHERE name LIKE '%john%'` (wildcard prefix — use full-text search), columns with very low cardinality (boolean) in large tables (full scan is faster), queries where the result is > ~20% of the table (index scan becomes more expensive than sequential scan).

Index the most selective columns first in composite indexes. Leftmost prefix rule: index on (A, B, C) helps queries filtering on A, A+B, or A+B+C, but not B alone or C alone.

---

**Q12: What is the N+1 query problem and how do you fix it?**

N+1: fetch a list of N objects (1 query), then make 1 DB query per object to fetch a related field (N queries). Total: N+1 queries. For N=1000, this is 1001 queries instead of 2.

Classic ORM example:
```
users = User.find_all()  # 1 query
for user in users:
    print(user.orders)   # 1 query per user → N queries
```

Fixes: (1) **JOIN**: `SELECT users.*, orders.* FROM users LEFT JOIN orders ON...` — 1 query. (2) **Batch fetch**: `SELECT * FROM orders WHERE user_id IN (id1, id2, ..., idN)` — 2 queries total. (3) **ORM eager loading**: `User.include(:orders).find_all()` — ORM generates the batch query. (4) **DataLoader pattern** (GraphQL): batches all requests within a single tick of the event loop.

---

**Q13: When do you use a cache and what can go wrong?**

Cache when: data is read far more often than written, recomputing it is expensive, slight staleness is acceptable.

Cache-aside (most common): app checks cache → miss → read DB → write cache. Write-through: write cache + DB together. Write-behind: write cache, async flush to DB. Read-through: cache handles the miss itself.

What goes wrong:
- **Cache stampede** (thundering herd): cache expires, 1000 requests all miss simultaneously → all hit DB. Fix: mutex lock on cache rebuild, or probabilistic early expiry.
- **Cache penetration**: querying keys that will never exist (attack vector). Fix: cache null results, or Bloom filter check before DB.
- **Cache avalanche**: many keys expire simultaneously. Fix: jitter TTLs (randomise expiry within a range).
- **Stale data**: cached data diverges from DB. Fix: event-driven invalidation on write, or shorter TTL.
- **Inconsistency**: distributed cache nodes diverge. Fix: strong consistency mode (slower) or accept eventual consistency.

---

**Q14: What is the difference between sharding and partitioning?**

Partitioning: splitting data within a single database server across multiple tables or storage units. Still one server. Improves query performance by reducing scan scope. Types: range (by date, ID), list (by country), hash.

Sharding: distributing data across multiple separate database servers. Each shard is an independent DB instance. Required when a single server can't handle the load (CPU, memory, or write throughput). Each shard is responsible for a subset of data.

Relationship: sharding IS horizontal partitioning across servers. Partitioning can be a step within sharding (each shard also partitions its own data).

When to shard: when you've exhausted vertical scaling, read replicas, caching, and optimisation. Sharding adds enormous complexity: cross-shard queries, distributed transactions, resharding. Avoid if possible.

---

**Q15: What is MVCC and why does it matter?**

MVCC (Multi-Version Concurrency Control): instead of locking rows, the database keeps multiple versions of each row. Readers see the version that was committed at their transaction start. Writers create a new version.

Why it matters: readers don't block writers; writers don't block readers. This gives high concurrency without the performance penalty of locks. PostgreSQL uses MVCC — a long-running SELECT won't block an UPDATE on the same row.

Implication for design: in PostgreSQL, running a VACUUM is important because old row versions accumulate (table bloat). Long-running transactions can prevent old versions from being cleaned up (transaction ID wraparound). This is why you set `idle_in_transaction_session_timeout`.

---

**Q16: How do you design database schema migrations with zero downtime?**

The expand-and-contract pattern:

**Phase 1 — Expand**: Add the new column as nullable (no constraint). Deploy code that writes BOTH old and new columns, reads old column.

**Phase 2 — Backfill**: Background job populates the new column for all existing rows (in small batches to avoid locking).

**Phase 3 — Dual read**: Deploy code that reads from new column if populated, falls back to old column.

**Phase 4 — Verify**: Confirm all rows have been backfilled.

**Phase 5 — Contract**: Deploy code that reads new column only. Remove old column in a later migration.

Never: add NOT NULL without a default in a single migration (locks the table for a full table rewrite). Never: rename a column in one step (old code breaks immediately). The expand-and-contract pattern ensures old and new code can run simultaneously during deployment.

---

**Q17: When would you use Elasticsearch vs your primary database for search?**

Use Elasticsearch when: full-text search needed (inverted index, BM25 scoring), complex relevance ranking, faceted search with aggregations, log analytics (Kibana), fuzzy matching or typo tolerance, searches across multiple entity types.

Keep in primary DB when: exact match lookup (a B-Tree index is fine), simple range queries, volume is < 10M records and query patterns are known upfront, you can't afford the operational complexity of a separate search cluster.

Key constraint: Elasticsearch is NOT your primary data store. Data lives in your DB, gets synced to Elasticsearch via a change data capture pipeline (Debezium → Kafka → ES indexer) or a dual-write in application code. Eventual consistency: ES index may lag DB by seconds. Plan for this in your design.

---

**Q18: What is Write-Ahead Logging (WAL) and how does it enable replication?**

WAL: before modifying data pages, write a record of the change to a sequential log file first. The change is durable as soon as the WAL record is fsynced, even before the data page is updated on disk.

This enables: crash recovery (replay WAL from last checkpoint on restart), replication (PostgreSQL streaming replication ships WAL records to standbys — they replay the same WAL), point-in-time recovery (replay WAL up to any timestamp).

For system design: WAL is why PostgreSQL replication is so reliable. Replicas are guaranteed to be consistent with primary because they replay the exact same operations in order. Replication lag = how far behind the replica's WAL replay position is from the primary.

---

**Q19: How do you handle hot partitions in a database or Kafka?**

Hot partition: one partition/shard receives disproportionate traffic because of a hot partition key (e.g., one celebrity user in a users_by_id shard, or all orders in the same Kafka partition because all have the same customerId).

Solutions:
- **Salting**: append a random suffix to the partition key: `userId + "_" + random(0, N)`. Distributes writes across N sub-partitions. Reads must query all N sub-partitions and merge.
- **Compound key**: add a secondary dimension: `(userId, date)` instead of just `userId`.
- **Separate hot keys**: identify the top 1% of keys causing 90% of traffic, route them to dedicated partitions.
- **Application-level sharding**: handle the hot key in cache (Redis) rather than the DB. Don't write to DB for every access.

In Kafka: use a custom partitioner that detects hot keys and round-robins across multiple partitions. Or salt the key before publishing.

---

**Q20: What is the difference between optimistic and pessimistic locking?**

Pessimistic locking: lock the resource before modifying it. Other transactions wait. Use when: conflicts are frequent (many concurrent writers on same rows), the cost of retrying is high, correctness is critical (financial transactions). SQL: `SELECT ... FOR UPDATE`.

Optimistic locking: read the resource with a version number. Attempt the update with a condition: `UPDATE ... WHERE version = read_version`. If another writer changed it (version mismatch), retry. Use when: conflicts are rare (few concurrent writers), reads >> writes, throughput is more important than avoiding retries.

In distributed systems: compare-and-swap (CAS) operations in Redis (`SET key value XX GET` or Lua scripts) are optimistic locking. Pessimistic distributed locking requires a distributed lock service (Redis SETNX, ZooKeeper) with its own complexity.

---

## Category 3: Caching, Queues & Async (Q21–Q28)

**Q21: How do you choose between Redis and Memcached?**

Choose Redis almost always. Redis supports: rich data types (hashes, sets, sorted sets, streams), persistence (RDB snapshots, AOF logging), replication + Sentinel/Cluster, pub/sub messaging, Lua scripting, atomic operations, TTL with multiple eviction policies.

Memcached: only string key-value, no persistence, simpler to operate, slightly faster for pure string GET/SET (due to less overhead). Use Memcached only when: you need absolute maximum throughput for simple string caching and operational simplicity matters more than features.

In 2025, Redis is the default choice for any new system. Memcached is legacy.

---

**Q22: What is the outbox pattern and when do you use it?**

Problem: you need to update the DB and publish an event to Kafka atomically. Without coordination: update DB succeeds, then service crashes before publishing → event never sent. Or: publish event, then DB update fails → event sent for data that doesn't exist.

Outbox pattern: write the event to an `outbox` table in the SAME DB transaction as the data update. A separate "outbox poller" reads the outbox and publishes to Kafka, then deletes the row. Since DB transaction is atomic: either both happen or neither does. The poller provides at-least-once delivery (idempotency needed at consumer).

Use when: strict consistency between DB state and downstream events is required. Payment processed AND event published, never one without the other. Tools: Debezium (CDC-based outbox), custom outbox poller.

---

**Q23: Kafka vs RabbitMQ vs SQS — when do you use each?**

**Kafka**: use when you need replay/reprocessing (event sourcing, audit), multiple independent consumer groups reading the same events, very high throughput (1M+ msg/sec), stream processing integration (Flink, Kafka Streams), long retention (days/weeks). Operationally complex.

**RabbitMQ**: use when you need complex routing (exchanges, bindings), per-message TTL and dead-letter queues, push-based delivery, priority queues, or you want a simpler operational model than Kafka. Lower throughput ceiling.

**SQS**: use when you're on AWS and don't want to manage infrastructure, need a simple task queue with managed HA, can accept 14-day max retention, want pay-per-use pricing with zero ops overhead.

Gut check: if you need replay capability or multiple independent consumers — Kafka. If you need simple task distribution with no replay — SQS or RabbitMQ. If you're unsure — Kafka (more flexible, just more complex to run).

---

**Q24: How do you handle exactly-once processing in a distributed system?**

True end-to-end exactly-once is very hard. The practical approach: at-least-once delivery + idempotent consumers.

At the infrastructure layer: Kafka's idempotent producer (sequence numbers prevent duplicate messages in a session). Kafka transactions enable exactly-once in a Kafka-to-Kafka pipeline.

At the consumer layer: deduplicate using a unique event ID. Before processing: `INSERT INTO processed_events (eventId) ON CONFLICT DO NOTHING`. If 0 rows inserted: already processed, skip. If 1 row inserted: process, commit in same transaction. This guarantees exactly-once processing semantics even with retries.

The subtlety: "exactly-once" means different things at different layers. Kafka can guarantee it within the pipeline. Your downstream effect (charging a credit card) still needs idempotency at the application layer.

---

**Q25: What is back pressure and how do you handle it?**

Back pressure: when a consumer processes slower than a producer produces, the queue grows unboundedly → OOM crash or timeout cascade.

Handling strategies:
- **Bounded queue + rejection**: accept up to N items, reject (429) beyond that. Producer knows to retry.
- **Blocking producer**: producer's `send()` blocks until queue has space. Simple but ties producer throughput to consumer speed.
- **Drop with sampling**: in telemetry systems, drop a % of events under load. Statistical accuracy preserved even with drops.
- **Load shedding**: drop lowest-priority requests first (analytics, recommendations) to protect core operations.
- **Scale consumers**: Kafka + K8s HPA — consumers scale out to match partition throughput.
- **Reactive Streams**: Project Reactor, RxJava implement backpressure-aware async pipelines. Consumer signals capacity upstream.

In Kafka specifically: consumers pull at their own pace — back pressure is handled naturally. If consumers fall behind, messages wait in Kafka (up to retention period). Monitor consumer lag to detect persistent back pressure.

---

**Q26: How does a message queue help with service decoupling?**

Without a queue: Service A calls Service B synchronously. If B is slow → A waits. If B is down → A fails. A and B must be deployed together (tight temporal coupling). A must know B's API.

With a queue: A publishes an event. B consumes it when ready. A doesn't wait. If B is down → messages queue up, delivered when B recovers. A and B deploy independently. A doesn't know about B at all (multiple consumers can be added without changing A).

Additional benefits: natural rate limiting (B processes at its own pace), event replay (if B has a bug, fix it and replay from Kafka), fan-out (multiple consumers receive the same event), audit trail (all events logged in Kafka).

The tradeoff: eventual consistency — B processes A's event asynchronously, so state is not immediately consistent. For operations that require immediate consistency (inventory reservation before showing "in stock"), you need synchronous coordination.

---

**Q27: When should you use async processing instead of synchronous?**

Use async (message queue) when:
- The operation doesn't need an immediate result returned to the user (email sending, image processing, report generation)
- The operation is long-running (> 1 second is a candidate; > 5 seconds is a strong signal)
- The downstream service has high variability in response time
- You need to decouple write spikes from processing capacity
- Multiple services need to react to the same event

Keep synchronous when:
- The user needs the result immediately to proceed (form submission, payment confirmation)
- The operation must be atomic with the request (checking inventory before confirming order)
- Failure of the downstream operation should block the request
- Low volume where the queue adds more complexity than value

---

**Q28: How do you design a dead letter queue (DLQ) system?**

A DLQ receives messages that fail processing after N retries. Without it, failed messages block the queue or are silently dropped.

Design: after max retries (typically 3–5 with exponential backoff), move the message to the DLQ topic (Kafka) or SQS DLQ. Include in the DLQ message: original message, failure reason, attempt count, timestamps, error stack trace.

Monitoring: alert on DLQ depth > 0 (any DLQ message is a bug requiring attention). Dashboard: DLQ messages by service, by error type, over time.

Recovery options: (1) Fix the bug, replay messages from DLQ. (2) Manually inspect and decide: replay valid messages, discard invalid ones. (3) Automated replay with circuit breaker: if downstream recovers, replay automatically.

DLQ is not a place to ignore messages — it's a holding area for investigation. Every DLQ message is a failed user request.

---

## Category 4: Distributed Systems Concepts (Q29–Q38)

**Q29: What is idempotency and why is it critical for distributed systems?**

An operation is idempotent if performing it multiple times has the same effect as performing it once. `GET /users/123` is idempotent. `POST /payments` is NOT idempotent by default.

Why critical: networks fail. After a timeout, did the request succeed or not? Without idempotency: retry = double charge, duplicate order, duplicate email. With idempotency: retry is always safe.

Implementation: client generates a UUID per operation (`Idempotency-Key: uuid`). Server stores: `{idempotencyKey → response}` in DB or Redis. On receipt: check if key exists → return cached response. If not: process + store atomically. Key expiry: 24 hours (enough for any reasonable retry window).

Idempotency keys are mandatory for: payment APIs (Stripe, Razorpay), booking APIs, anything modifying state where the result matters.

---

**Q30: What is a distributed transaction and how do you handle it?**

Distributed transaction: a transaction that spans multiple services or databases. ACID guarantees don't extend across service boundaries.

Two-Phase Commit (2PC): coordinator asks all participants to "prepare" (vote commit/abort), then tells all to "commit" or "rollback". Problem: blocking protocol — if coordinator crashes after prepare, participants are stuck holding locks indefinitely. Rarely used in microservices.

Saga Pattern: break the transaction into local transactions per service. Each publishes an event. Next service reacts. On failure: compensating transactions (reverse previous steps). Two styles: choreography (services react to events, no central coordinator) and orchestration (central saga orchestrator calls each service). Use for: order placement spanning payment + inventory + shipping.

Practical rule: if you need distributed transactions, reconsider your service boundaries. Often, the need for cross-service transactions signals that two services should be one.

---

**Q31: What is the difference between a load balancer and a service mesh?**

Load balancer (API Gateway): handles north-south traffic (external clients → services). Manages: SSL termination, authentication, rate limiting, routing, DDoS protection. One instance at the edge.

Service mesh (Istio, Linkerd): handles east-west traffic (service → service). Every service gets a sidecar proxy (Envoy). Manages: mTLS between all services, circuit breaking, retries, timeout policies, distributed tracing, traffic splitting for canary deployments. Zero-trust security between services.

They complement each other: external traffic → load balancer → service → [mesh] → other services. Use a service mesh when: you have many microservices (10+), you need fine-grained observability between services, you want zero-trust networking without changing application code. The overhead (sidecar per pod, control plane) is worth it at scale.

---

**Q32: How do you detect and handle cascading failures?**

Cascading failure: Service A is slow → Service B's thread pool fills up waiting for A → B becomes slow → Service C fills up waiting for B → entire system down.

Prevention:
- **Circuit breaker**: after N failures, "open" the circuit — fail fast without calling the dependency. After a timeout, test with one request. If successful, close the circuit. Hystrix, Resilience4j.
- **Timeout on every call**: never wait indefinitely. If Service A doesn't respond in 200ms, return a fallback.
- **Bulkhead**: separate thread pools per dependency. Service A's slowness can't exhaust threads for Service B.
- **Fallback**: when a dependency fails, return a degraded response (cached data, default value, "service temporarily unavailable") rather than propagating the error.
- **Rate limiting on consumers**: don't send traffic to an already-struggling downstream.

Detection: Prometheus alerts on rising error rates and latency. Distributed tracing (Jaeger) shows where in the call chain failures originate.

---

**Q33: What is leader election and when do you need it?**

Leader election: in a cluster of N identical instances, elect exactly one as "leader" to perform certain tasks. Others are "followers" or standby.

Need it for: primary DB selection (only one node accepts writes), cron job deduplication (only one instance runs the scheduled task), partition leader assignment in Kafka (each partition has one leader broker).

Implementations:
- **ZooKeeper**: ephemeral sequential znodes — lowest sequence number = leader. Auto-released on disconnect.
- **etcd (Raft)**: lease-based — create a lease, PUT with prevExist=false. Kubernetes uses this for controller-manager HA.
- **Redis SETNX**: simple lock with TTL. Less robust than ZK/etcd (single point of failure, clock dependency) but often sufficient.

Fencing token: monotonically increasing number returned with each leader election. Pass it to storage operations. Storage rejects stale tokens (prevents split-brain writes even if old leader thinks it's still elected).

---

**Q34: How do you implement a distributed rate limiter?**

Centralised counter in Redis: `INCR ratelimit:{userId}:{window}; EXPIRE ratelimit:{userId}:{window} {windowSeconds}`. Atomic — all server instances share the same counter. Lua script: combine check + increment in one atomic operation.

Sliding window counter:
```
current = GET ratelimit:{userId}:{currentWindow}
previous = GET ratelimit:{userId}:{previousWindow}
elapsed = now % windowSeconds  
estimated = current + previous × (1 - elapsed/windowSeconds)
if estimated >= limit: reject
else: INCR ratelimit:{userId}:{currentWindow}
```

Local + global hybrid (for very high QPS): each server maintains a local counter. Every 100ms, sync with Redis central counter. Allows 100ms of burst per server but global limit holds over time. Reduces Redis calls by 100×.

Failure mode: if Redis is down — fail open (allow requests) for most APIs; fail closed for sensitive operations (payments, auth).

---

**Q35: What is eventual consistency and when is it acceptable?**

Eventual consistency: after a write, reads will eventually (not immediately) reflect it. All replicas converge to the same value given enough time with no new writes.

Acceptable when: social feed (seeing a tweet 2 seconds late is fine), view/like counts (approximate is fine), DNS (propagation delay is expected), product catalog (prices don't need instant global consistency), user presence (who's online can lag by seconds).

Not acceptable when: account balance after a transfer, inventory count after a reservation, auth token revocation, order confirmation after payment.

The implementation trick: read-your-writes consistency is often sufficient. A user always sees their own writes immediately (route their reads to the same replica they wrote to, or to primary for 30s after write). Others may see slight lag. This makes most eventual consistency invisible to the individual user.

---

**Q36: How do you handle network partitions in your system?**

A network partition = some nodes can communicate with each other but not others. Can't be prevented — design for it.

At the application layer:
- **Detect**: health checks between services. Track failed calls per endpoint.
- **Circuit breaker**: when a dependency becomes unreachable, fail fast.
- **Fallback**: serve cached or default data. Don't propagate the partition to users.
- **Async queue**: buffer writes during partition, replay when connectivity restores.
- **Idempotent retries**: once connectivity restores, retry with idempotency keys.

At the data layer:
- **CP systems** (ZooKeeper, etcd, PostgreSQL synchronous replication): reject writes during partition rather than serve potentially stale data. Service is unavailable but correct.
- **AP systems** (Cassandra, DynamoDB): continue to serve requests during partition. Resolve conflicts using last-write-wins, CRDTs, or application-level conflict resolution after reconnection.

The design decision: for financial data — CP (correctness > availability). For social data — AP (availability > perfect consistency).

---

**Q37: What is the two-generals problem and why does it matter?**

Two generals problem: two generals can only communicate via messengers that might be captured. They need to coordinate a simultaneous attack. No matter how many messages they exchange, they can never be 100% certain the other received the last confirmation. There's always a "what if my last acknowledgment was lost?"

Why it matters: it proves that in any system where communication can be lossy (i.e., all distributed systems), you cannot guarantee that two parties have achieved consensus with absolute certainty using finite messages.

Practical implication: this is why "exactly-once" delivery is so hard, why distributed consensus protocols (Raft, Paxos) are complex, and why idempotency is the pragmatic solution. Accept that messages may be lost or duplicated, and design your operations to be safe under those conditions.

---

**Q38: What is the difference between synchronous and asynchronous replication? When do you use each?**

Synchronous replication: the write is not acknowledged to the client until the replica has confirmed receipt and durability. Zero data loss on primary failure. Latency penalty = round-trip to replica (+ network latency to replica location). Use when: RPO=0 (zero data loss), financial systems, critical user data.

Asynchronous replication: write acknowledged to client after primary confirms. Replica catches up in the background. Possible data loss window = replication lag (typically milliseconds to seconds). No latency penalty. Use when: read scaling (replicas for analytics/reporting), cross-region standbys where RPO > 0 is acceptable.

Semi-synchronous (MySQL): at least one replica must acknowledge. Balance between durability and latency. Better than async for safety, less penalty than full synchronous.

---

## Category 5: Real-World System Design Judgment (Q39–50)

**Q39: How would you migrate a monolith to microservices without downtime?**

Strangler Fig Pattern: incrementally extract services while the monolith continues running. Never rewrite everything at once.

Steps: (1) Identify bounded contexts (payment, users, notifications — clear domains with minimal shared state). (2) Add an API gateway in front of the monolith. (3) Extract one service (start with least risky: notifications or email). (4) Route that service's traffic via gateway to the new service, monolith serves everything else. (5) Monitor for regressions. (6) Repeat, service by service.

Database migration: don't share the monolith's DB from microservices. Use the Strangler Fig at the DB level too: extract the relevant tables, run dual-write during transition (both old schema and new), cut over after validating consistency.

Timeline: this takes months to years for a large system. Amazon's service extraction took 7 years. Netflix took 7 years. Set expectations correctly.

---

**Q40: How do you design for data privacy (GDPR)?**

GDPR requires: user consent for data collection, right to access their data, right to deletion ("right to be forgotten"), data minimisation (collect only what you need), breach notification within 72 hours.

Design implications:
- **Data inventory**: tag every DB column with: is it PII? which user does it belong to? what's the purpose?
- **Right to deletion**: soft-delete records. Background job pseudonymises or hard-deletes after X days. Problem: data in Kafka (event log), CDN cache, backups. Need a deletion pipeline that cascades.
- **Data access API**: `GET /users/{id}/data` returns everything stored about the user.
- **Encryption**: PII fields encrypted at application level (with KMS). Even if someone gets a DB dump, they can't read names and emails.
- **Data residency**: EU user data stored only in EU region (AWS Frankfurt, not us-east-1). Achieved via geo-routing + regional DBs.
- **Retention policies**: don't keep data indefinitely. Define retention per data type, implement automated deletion.

---

**Q41: How do you design a system that must handle multiple tenants (multi-tenancy)?**

Three isolation models:

**Database per tenant**: completely separate DB per customer. Maximum isolation, no cross-tenant data leaks possible. High operational overhead at scale. Use for: enterprise customers with compliance requirements, huge customers needing performance isolation.

**Schema per tenant**: one DB, separate schema (namespace) per tenant. Medium isolation, easier to operate. Use for: mid-scale SaaS with moderate isolation needs.

**Shared schema (row-level tenancy)**: one DB, one schema, `tenant_id` column on every table. Every query includes `WHERE tenant_id = ?`. Lowest overhead, hardest to maintain isolation. Use for: many small tenants (1000s of customers).

Hybrid: small tenants → shared schema. Large/compliance customers → dedicated DB. Route based on tenant tier. This is what most SaaS companies do (Salesforce, Shopify).

Security: enforce `tenant_id` filter in a middleware layer — never rely on application code to always add it. A missed filter = data leak across tenants.

---

**Q42: How would you design the backend for a real-time multiplayer game?**

Core challenges: low latency (< 50ms for player actions), synchronisation (all players see same game state), cheat prevention.

Architecture:
- **Game server**: authoritative server holds canonical game state. Clients are "thin" — they send inputs, receive state updates, not compute the game.
- **WebSocket**: low-latency bidirectional protocol. UDP preferred for speed (WebRTC Data Channel for browser games).
- **State synchronisation**: delta compression (only send changed state). Client-side prediction (apply your own moves immediately, reconcile when server confirms). Lag compensation (server rewinds state to account for player latency).
- **Matchmaking**: ELO/MMR-based skill matching. Redis sorted set (ZRANGEBYSCORE to find players with similar rating + low wait time). Lock players during match to prevent double-matching.
- **Server allocation**: game servers allocated per match. Kubernetes pods per game instance. Geographic routing to server nearest to average player location.

Cheat prevention: never trust the client. Server-authoritative game state. Server validates all moves (can you actually move to that position? do you have ammo?).

---

**Q43: How do you design a search engine from scratch?**

Three components: crawler, indexer, query processor.

**Crawler**: BFS-based web crawler. URL frontier (priority queue by PageRank + freshness). Politeness (max N requests/sec per domain, respect robots.txt). Duplicate detection (SimHash for near-duplicate content, Bloom filter for seen URLs). Store raw HTML in S3.

**Indexer**: parse HTML → extract text, links, metadata. Build inverted index: for each term → list of documents (and position within document). TF-IDF for term importance. PageRank algorithm for link-based authority score. Store index in a distributed key-value store (sharded by term hash).

**Query processor**: parse query → tokenise → look up inverted index for each term → intersect document sets (AND queries) → score by TF-IDF × PageRank × freshness → return ranked results. Caching: top-K results for popular queries (90%+ cache hit). Spelling correction: edit distance on query terms against known vocabulary. Autocomplete: Trie of popular search queries.

---

**Q44: How do you handle a data breach in production?**

Immediate response (first 15 minutes): isolate the compromised system (revoke credentials, block network access), preserve evidence (don't wipe logs), notify security team and escalate.

Investigation: determine scope (what data was accessed? how many users?), find the attack vector, close the vulnerability.

User notification: GDPR requires notification within 72 hours to supervisory authority if breach risks users' rights. Notify affected users with: what happened, what data was affected, what you're doing about it, what they should do (change password, monitor accounts).

Technical remediation: rotate all credentials, API keys, certificates. Audit access logs for lateral movement. Patch the vulnerability. Add monitoring for the attack pattern. Penetration test the fixed system.

Post-mortem: blameless, public-facing post-mortem if significant. Timeline, root cause, customer impact, remediation steps, and preventive measures. Transparency builds trust.

---

**Q45: How do you design for observability in a microservices architecture?**

Three pillars: logs, metrics, traces. But the real answer is correlation between them.

**Logs**: structured (JSON), with request_id, user_id, service_name, trace_id on every log line. Centralised in Elasticsearch/Loki. Never log PII or secrets.

**Metrics**: Prometheus scrapes every service. Golden signals per service: latency (p50/p95/p99), error rate, throughput, saturation. SLO dashboards. Alert on symptoms (error rate > SLO) not causes (CPU > 80%).

**Traces**: OpenTelemetry SDK instruments every service. Trace ID propagated via HTTP header (`X-Trace-Id`). Jaeger or Tempo stores traces. Flame graph shows exactly which service/DB/call is slow for any request.

**Correlation**: when a user reports a bug, you search logs by request_id → find the trace_id → open the trace in Jaeger → see exactly where latency was. This cross-signal correlation is what makes debugging tractable.

Alert strategy: every alert must be actionable (someone must do something when it fires). No alerts that just get silenced. On-call rotation + runbooks for every alert.

---

**Q46: A senior engineer asks you to review their design. What do you look for?**

I review in this order:

1. **Correctness**: does the design actually solve the problem? Does it handle edge cases (empty state, max load, partial failures)?
2. **Single points of failure**: every critical path should have redundancy. Find the single node whose failure brings everything down.
3. **Data consistency**: where data is written in multiple places, is consistency handled? Are there race conditions?
4. **Scalability bottlenecks**: where does the design break at 10× load? Is the bottleneck load-balanced or sequential?
5. **Security**: authentication at every entry point? Are secrets handled correctly? Input validation?
6. **Operational complexity**: can a team of 5 operate this at 3am? Is there an on-call runbook?
7. **Missing components**: is there logging, metrics, alerting? Error handling and retry logic? What's the rollback plan?

I give specific, actionable feedback with alternatives, not just "this is wrong." I ask questions rather than assert assumptions: "What happens when service B is down here?" rather than "This breaks when B is down."

---

**Q47: How would you design a global leaderboard for a game with 100M players?**

Redis Sorted Set is the canonical answer: `ZADD leaderboard score userId`. ZREVRANK for a user's position, ZREVRANGE for top-100. Redis handles 100M members comfortably (~7.5GB memory). Sub-millisecond operations.

Challenges at scale:
- **Update frequency**: if scores update on every game action, 100M updates/sec is too many. Batch: accumulate score deltas in Redis per user, flush to the sorted set every 30 seconds. Users see their score update on next leaderboard refresh.
- **Near-real-time for the user's own rank**: special-case the requesting user's rank by querying ZREVRANK directly — always accurate.
- **Pagination**: `ZREVRANGE leaderboard 0 99` for top-100. For "show me users around rank 50,000": ZREVRANK to get rank, then ZREVRANGE (rank-50, rank+50). O(log N + M) where M = returned members.
- **Time-windowed leaderboards** (weekly, monthly): separate sorted sets per window. Background job creates new weekly set every Monday 00:00 UTC. Expire old sets via Redis TTL.

---

**Q48: How do you design an API that needs to support both mobile and web clients with different data needs?**

BFF (Backend for Frontend) pattern: separate API services optimised for each client type.

- **Web BFF**: can return rich, nested JSON (powerful browser, good bandwidth). Supports complex queries, pagination, sorting.
- **Mobile BFF**: returns minimal, flat JSON (battery, bandwidth, memory constrained). Pre-fetches and pre-aggregates data that the mobile app needs in one call. Supports offline sync APIs.

Alternative: GraphQL — single endpoint where each client queries exactly the fields it needs. Web fetches deeply nested data; mobile fetches minimal fields. Eliminates over-fetching and under-fetching. Operational complexity: N+1 problem at GraphQL layer (use DataLoader), caching is harder than REST.

For most teams: REST with a mobile-specific endpoint for the most bandwidth-critical screens + a shared general API. BFF is for large organisations (one team per client type). GraphQL when client diversity is high and backend team wants to expose a flexible schema.

---

**Q49: How do you decide between microservices and a monolith for a new product?**

Start with a monolith. Almost always.

Monolith advantages for early-stage: faster development (no network calls, no distributed transactions, simple deployment), easier debugging (full stack trace, one codebase), lower operational overhead (one service to monitor, deploy, and scale).

Move to microservices when: team > 20 engineers and coordination is slowing development, two domains genuinely need different scaling (recommendation system vs auth service), independent deployment cycles are needed (marketing can ship without waiting for auth team), or different tech stacks are required for different domains.

Warning signs that microservices are wrong: you have a 5-person team, you're building a synchronous request-response system with no real scale requirements, you're introducing distributed transactions everywhere, your "microservices" can only deploy together (they're a distributed monolith).

Jeff Bezos's "two-pizza team" rule: each service should be owned and operated by a team small enough to be fed by two pizzas. If your team can't independently own, operate, and deploy a service, it's not a real microservice.

---

**Q50: What separates a good system design from a great one?**

A good design solves the stated problem correctly and scales.

A great design also: anticipates failure modes (what breaks at 10×? what happens when the DB is down?), considers operational realities (can the on-call engineer understand and fix this at 3am?), makes the implicit explicit (why was this design choice made? what alternatives were considered and rejected?), is appropriately simple (not over-engineered for a problem that doesn't exist yet), and evolves gracefully (adding a new feature requires one service change, not five).

In an interview: what makes a candidate stand out is not knowing more patterns, it's demonstrating systems thinking — tracing a user request through the entire stack, spotting the race condition, noticing the single point of failure, asking "what happens when X fails?" Interviewers evaluate: can this person build something we can trust to run in production? The patterns are a vehicle; the judgment is what matters.
