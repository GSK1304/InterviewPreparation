# 📚 HLD Core Concepts — Observability, Security & Disaster Recovery

---

## 1. Observability — The Three Pillars

Observability = ability to understand the internal state of a system from its external outputs. Three pillars: **Logs, Metrics, Traces**.

### Pillar 1: Logs
Timestamped, immutable records of discrete events.

```
Structured logging (JSON — searchable):
{
  "timestamp": "2025-03-22T10:15:30Z",
  "level": "ERROR",
  "service": "payment-service",
  "traceId": "abc-123",
  "userId": "user-456",
  "message": "Payment declined",
  "amount": 99.99,
  "reason": "insufficient_funds"
}

vs Unstructured (hard to parse):
"2025-03-22 ERROR: Payment failed for user 456"

Log Levels: TRACE → DEBUG → INFO → WARN → ERROR → FATAL
Rule: ERROR only for actionable issues; don't log noisy DEBUG in production
```

**Log Aggregation Stack**:
```
Services → Fluentd/Filebeat (collector)
         → Kafka (buffer)
         → Elasticsearch (index + search)
         → Kibana (dashboard)

OR: Datadog, Splunk, AWS CloudWatch Logs, GCP Cloud Logging
```

### Pillar 2: Metrics
Numeric measurements sampled over time.

```
Types:
  Counter:   monotonically increasing (total_requests = 10M, 10.1M, 10.2M...)
  Gauge:     value at a point in time (memory_used = 4.2GB now)
  Histogram: distribution of values (request_duration_ms: p50=12, p95=85, p99=340)
  Summary:   pre-computed quantiles (p95_latency=85ms over last 5 min)

Golden Signals (Google SRE):
  Latency   - p50, p95, p99 response times
  Traffic   - requests per second
  Errors    - error rate (5xx responses)
  Saturation - CPU%, memory%, queue depth

SLI/SLO/SLA:
  SLI (Indicator): actual measurement (e.g., 99.95% requests < 200ms)
  SLO (Objective): target (e.g., 99.9% of requests < 200ms)
  SLA (Agreement): contractual promise with penalty (99.5% availability)
```

**Metrics Stack**:
```
Services → Prometheus (scrape metrics) → Grafana (dashboards + alerts)
OR: Datadog, AWS CloudWatch, Dynatrace, New Relic
```

### Pillar 3: Distributed Tracing
Track a single request as it flows through multiple services.

```
User request → API Gateway → Order Service → Payment Service → DB

Trace ID: abc-123 (spans entire request)
  Span 1: API Gateway (0ms – 5ms)
  Span 2: Order Service (5ms – 45ms)
    Span 2a: DB query (10ms – 30ms)
  Span 3: Payment Service (45ms – 120ms)
    Span 3a: Stripe API call (50ms – 115ms) ← bottleneck found!
  
Total: 120ms. Without tracing, you'd never know Stripe was the bottleneck.
```

**Tracing Stack**:
```
Services → OpenTelemetry SDK (instrument code)
         → Jaeger / Zipkin / AWS X-Ray (collector + storage)
         → UI (flame graph visualization)

Sampling: Don't trace 100% of requests (too expensive)
  Head-based: decide at trace start (1% sample rate)
  Tail-based: decide at trace end (always sample errors, slow requests)
```

### Alerting Best Practices
```
Alert on SYMPTOMS, not causes:
  ✅ Alert: "Error rate > 1%" (user-visible symptom)
  ❌ Alert: "CPU > 80%" (may or may not cause user pain)

Alert fatigue prevention:
  - Every alert must be actionable (someone must do something)
  - Use severity levels: P1 (page), P2 (ticket), P3 (weekly review)
  - Error budget: if SLO allows 0.1% errors, alert only when budget burning fast

On-call: runbooks for every alert (what to check, how to fix)
```

### 🏭 Industry Examples
- **Netflix**: Custom Atlas (metrics), Mantis (real-time stream processing), hundreds of Grafana dashboards.
- **Uber**: Distributed tracing across 3000+ microservices using Jaeger (open-sourced by Uber).
- **Google**: Pioneered SLI/SLO/SLA framework documented in Google SRE Book (free online).
- **Datadog**: SaaS observability platform used by Airbnb, Stripe, Slack — all three pillars in one.

---

## 2. Security in System Design

### Transport Security (TLS/HTTPS)
```
Every service-to-service call and client-to-server call must use TLS.

TLS 1.3 handshake (simplified):
  Client → Server: ClientHello (supported cipher suites)
  Server → Client: ServerHello + Certificate
  Client validates certificate chain → CA trusted?
  Client → Server: encrypted with server's public key
  Both derive session keys → encrypted communication

Certificate management:
  - Use Let's Encrypt for automatic free TLS certs
  - Internal: mTLS (mutual TLS) via service mesh (Istio)
  - Rotate certificates before expiry (automate with cert-manager)
```

### Authentication & Authorization
```
AuthN (who are you?):
  JWT tokens — stateless, verify with public key, no DB roundtrip
  OAuth2 — delegate auth to identity provider (Google, Okta)
  API Keys — for machine-to-machine, service accounts
  mTLS — certificate-based service identity

AuthZ (what can you do?):
  RBAC (Role-Based): roles → permissions; user assigned roles
    Simple, coarse-grained, easy to audit
  ABAC (Attribute-Based): policy engine evaluates attributes
    Flexible, fine-grained, complex
  ReBAC (Relationship-Based): permissions based on object relationships
    Used by Google Zanzibar (Google Docs sharing model)
```

### Data Security
```
Encryption at rest:
  DB: Transparent Data Encryption (TDE) at storage level
  Application-level: encrypt sensitive fields before storing
    (PII, credit card numbers — use envelope encryption with KMS)

Encryption in transit:
  TLS everywhere (internal + external)
  No plain HTTP, ever

Secrets management:
  Never store secrets in code or config files
  Use: AWS Secrets Manager, HashiCorp Vault, GCP Secret Manager
  Rotate secrets regularly; audit access

Data masking:
  Logs: never log PII, passwords, tokens, card numbers
  Dev/staging: use masked/anonymized production data
```

### Common Attack Vectors
```
SQL Injection:
  ❌ "SELECT * FROM users WHERE email='" + email + "'"
  ✅ PreparedStatement / parameterized queries always

XSS (Cross-Site Scripting):
  ❌ innerHTML = userInput
  ✅ Sanitize output; CSP headers; React's JSX auto-escapes

CSRF (Cross-Site Request Forgery):
  Fix: SameSite=Strict cookies; CSRF tokens for state-changing requests

Prompt Injection (AI systems):
  User input embedded in LLM prompts → manipulate AI behavior
  Fix: System prompt isolation; input sanitization; output filtering

Rate limiting:
  Brute force: 5 login attempts/min per IP
  API abuse: per-user request quotas
  
DDoS mitigation:
  CDN absorbs volumetric attacks at edge
  Rate limiting at API Gateway
  AWS Shield / Cloudflare DDoS protection
```

### Input Validation
```
Always validate at:
  1. API Gateway (basic schema validation)
  2. Service layer (business rules)
  3. Database layer (constraints, foreign keys)

Principle of Defense in Depth:
  Don't rely on a single layer; validate everywhere
```

### 🏭 Industry Examples
- **Netflix**: Zero-trust security model — every service-to-service call authenticated via mTLS + SPIFFE/SPIRE.
- **GitHub**: Advanced Security scans every push for secrets (API keys, passwords) accidentally committed.
- **Google Zanzibar**: Powers Google's authorization for Drive, YouTube, Maps. Open-sourced as SpiceDB.

---

## 3. Disaster Recovery (DR)

### Key Metrics
```
RTO (Recovery Time Objective):
  Maximum acceptable downtime after a failure
  "We can tolerate up to 4 hours of downtime"
  Drives: how fast must failover complete?

RPO (Recovery Point Objective):
  Maximum acceptable data loss
  "We can't lose more than 1 hour of data"
  Drives: how frequently must we backup/replicate?

Examples:
  Banking: RTO=15min, RPO=0 (zero data loss) → synchronous replication + hot standby
  Blog: RTO=24hr, RPO=24hr → daily backup is fine
  E-commerce: RTO=1hr, RPO=15min → async replication + automated failover
```

### DR Strategies (Cost vs Recovery Speed)
```
Strategy 1: Backup and Restore (cheapest)
  - Backup to S3/GCS periodically
  - On disaster: restore from backup, reconfigure
  - RTO: hours-days, RPO: hours
  - Cost: minimal
  - Use: dev/staging, non-critical data

Strategy 2: Pilot Light
  - Core infrastructure running in DR region (minimal)
  - Data replicated continuously
  - On disaster: scale up DR region quickly
  - RTO: 1-4 hours, RPO: minutes
  - Cost: low (minimal running instances)

Strategy 3: Warm Standby
  - Fully functional but scaled down replica in DR region
  - Data replicated in real-time
  - On disaster: scale up, redirect traffic
  - RTO: minutes-1 hour, RPO: seconds-minutes
  - Cost: medium (~50% of production cost)

Strategy 4: Active-Active (most expensive)
  - Two or more regions serve production traffic simultaneously
  - Traffic split via GeoDNS/Global LB
  - Failure of one region: other absorbs traffic
  - RTO: seconds (auto-failover), RPO: near-zero
  - Cost: 2x production cost
  - Use: highest availability requirements
```

### Geo-Replication
```
Active-Passive (most common):
  Primary region: us-east-1 (all writes)
  Standby region: eu-west-1 (data replicated, no traffic)
  Failover: DNS TTL change → traffic moves to EU
  Lag: replication lag = potential data loss (RPO)

Active-Active:
  Traffic split: 50% US, 50% EU
  Writes accepted in both regions
  Challenge: conflict resolution for concurrent writes
  Solutions: 
    - Cassandra LWW (Last Write Wins) by timestamp
    - CRDTs (Conflict-free Replicated Data Types)
    - Single-master per entity (user writes always go to home region)
```

### Chaos Engineering
```
Practice: intentionally inject failures in production to test resilience

Netflix Chaos Monkey: randomly kills production instances
Netflix Chaos Kong: kills entire AWS availability zones
Facebook Failure Testing Service: tests service degradation gracefully

Principles:
  1. Define steady state (normal behavior metrics)
  2. Hypothesize that steady state holds under failure
  3. Introduce real-world events (instance kills, network delays)
  4. Disprove hypothesis (find the weakness)

Start in staging, then production with careful blast radius control
```

### 🏭 Industry Examples
- **Netflix**: Active-active across 3 AWS regions. Chaos Monkey runs continuously in production.
- **Amazon**: Each AWS service is built for regional isolation. S3 has 11 nines durability via geo-replication.
- **Facebook**: Multi-region active-active for user data. Custom Akkio system for geo-distributed data.

---

## 4. Idempotency in Distributed Systems

### What is Idempotency?
An operation is idempotent if performing it multiple times has the same effect as performing it once.

### Why It Matters
```
Distributed systems have partial failures:
  - Request sent, response lost (was it processed?)
  - Network timeout (did the payment go through?)
  - Message queue consumer crashes mid-processing

Without idempotency: retry → double charge, duplicate order, duplicate email
With idempotency: retry safely → only one effect
```

### Implementation Patterns
```
Pattern 1: Idempotency Key (client-generated)
  Client generates UUID for each unique request
  Server stores processed keys: SET idempotency:{key} "processed" EX 86400
  
  POST /payments
  Idempotency-Key: uuid-abc-123
  { "amount": 99.99, "card": "..." }
  
  First call: process payment, store key
  Retry call: detect existing key → return same response (no reprocessing)
  
  Used by: Stripe, Braintree, PayPal for payment APIs

Pattern 2: Natural idempotency (PUT over POST)
  PUT /users/123 { "name": "John" }
  → Second PUT sets same value → idempotent by definition
  
  POST /users { "name": "John" }
  → Second POST creates another user → NOT idempotent

Pattern 3: Conditional writes
  UPDATE orders SET status='shipped' WHERE id=123 AND status='processing'
  → Second update: WHERE clause fails → no double-update

Pattern 4: Sequence numbers / Offset tracking
  Kafka consumer: process message at offset 1001 → commit offset 1002
  Restart: resume from last committed offset
  Deduplication: check if offset already processed before acting
```

### 🏭 Industry Examples
- **Stripe**: Every API has idempotency key support. Standard practice for payment SDKs.
- **AWS SQS**: At-least-once delivery + consumer must handle duplicates via deduplication ID.
- **Kafka**: Idempotent producer (exactly-once within a partition) using sequence numbers.

---

## 5. Leader Election

### Why Needed?
In distributed systems, certain tasks must be done by exactly ONE node (avoid conflicts):
- Primary database node for writes
- Scheduler that triggers cron jobs
- Kafka partition leader
- Service that sends hourly digest emails

### Approaches

**Zookeeper/etcd Lease**:
```
All nodes try to create an ephemeral node: /election/leader
First to succeed → leader (Zookeeper guarantees atomicity)
If leader crashes → ephemeral node deleted → others re-elect
Used by: Kafka broker leader election, HDFS NameNode
```

**Raft Consensus Algorithm**:
```
Nodes elect a leader via voting:
  1. Node starts as Follower, sets random election timeout
  2. If no heartbeat from leader → becomes Candidate, votes for self
  3. Broadcasts RequestVote to all peers
  4. Peers vote for first candidate they hear from (in current term)
  5. Candidate with majority votes → becomes Leader
  6. Leader sends heartbeats to prevent new elections

Properties:
  - Leader handles all writes; replicates to followers
  - Guarantees consistency (majority quorum)
  - Used by: etcd (Kubernetes store), CockroachDB, TiKV
```

**Database-based Lock**:
```sql
-- Simple leader election via DB
INSERT INTO leader_lock(service, leader_id, expires_at)
VALUES ('scheduler', 'node-A', NOW() + INTERVAL '30 seconds')
ON CONFLICT (service) DO UPDATE
  SET leader_id = 'node-A', expires_at = NOW() + INTERVAL '30 seconds'
  WHERE leader_lock.expires_at < NOW(); -- only steal if expired

-- Node-A becomes leader if insert succeeds or it already owns the lock
-- Renew every 10s; if node dies, lock expires, another node wins
```

### 🏭 Industry Examples
- **Apache Kafka**: Each partition has exactly one leader broker (elected via Zookeeper/KRaft).
- **Kubernetes**: etcd (Raft) stores cluster state; controller-manager uses leader election for HA.
- **Elasticsearch**: Master node elected via Zen Discovery (Raft-based in v7+).

---

## Interview Q&A

**Q: What is the difference between RPO and RTO?**
A: RPO is about data — how much data loss can we tolerate? (drives replication frequency). RTO is about time — how long can we be down? (drives failover speed and infrastructure standby). A bank might have RPO=0 (zero data loss, synchronous replication) and RTO=15 minutes (hot standby that takes 15 minutes to promote).

**Q: How do you design for zero downtime deployments?**
A: Blue-green deployments (spin up new version, shift traffic, keep old version ready for rollback) or rolling deployments (replace instances one-by-one). Database migrations must be backward-compatible during the transition window (expand then contract pattern). Feature flags enable deployment independent of feature release.

**Q: How would you detect a compromised JWT token?**
A: JWT is stateless — can't be revoked without a denylist. Options: (1) Short TTL (15 min) — token expires quickly. (2) Denylist in Redis — add JTI to denylist on logout (TTL = remaining token lifetime). (3) Token versioning — store version in DB; increment on compromise; reject tokens with old version. (4) Refresh token rotation + familyId binding — theft detection via family conflict.

**Q: What is the CAP theorem and how does it apply to your designs?**
A: CAP states a distributed system can only guarantee 2 of: Consistency (all nodes see same data), Availability (always respond), Partition Tolerance (survive network splits). Since partitions are unavoidable, choose CP (strong consistency, may reject writes during partition — banks) or AP (always available, eventual consistency — social feeds, DNS). In practice, PACELC extends this to also consider latency vs consistency tradeoffs when no partition.

**Q: How do you handle distributed tracing without impacting performance?**
A: Sampling — trace only a percentage of requests (1-10%). Always trace errors and slow requests (tail-based sampling). Use async exporters so trace export doesn't block request path. OpenTelemetry has < 1ms overhead with 1% sampling. Adjust sampling rate per service based on traffic volume.
