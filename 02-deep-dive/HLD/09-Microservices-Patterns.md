# 📚 HLD Core Concepts — Microservices Patterns

---

## 1. Monolith vs Microservices

### Monolith
Single deployable unit containing all business logic.
```
┌─────────────────────────────────┐
│           Monolith              │
│  ┌──────┐ ┌──────┐ ┌────────┐  │
│  │ Auth │ │Order │ │Payment │  │
│  └──────┘ └──────┘ └────────┘  │
│       Single DB                 │
└─────────────────────────────────┘
```
**Advantages**: Simple to develop, test, deploy initially. No network latency between components. Transactions are easy (single DB).
**Tradeoffs**: Hard to scale individual components. One bug can bring down everything. Long deploy cycles. Technology lock-in.
**Use when**: Early-stage product, small team (< 10 engineers), tight deadlines.

### Microservices
Each business domain as an independently deployable service with its own DB.
```
Client → API Gateway
              ├── Auth Service    → Auth DB
              ├── Order Service   → Order DB
              └── Payment Service → Payment DB
                      ↕ (event-driven via Kafka)
```
**Advantages**: Independent scaling and deployment. Technology flexibility per service. Fault isolation. Smaller, focused teams.
**Tradeoffs**: Network latency between services. Distributed transactions (no ACID). Operational complexity. Data consistency harder.
**Use when**: Team > 20 engineers, clear domain boundaries, different scaling needs per domain.

### Strangler Fig Pattern (Monolith → Microservices Migration)
```
Phase 1: New requests go to new microservice via feature flag
Phase 2: Migrate data incrementally
Phase 3: Remove old monolith code path
No big-bang rewrite — incremental, low-risk migration
```
**🏭 Used by**: Netflix (migrated from monolith over 7 years), Amazon (decoupled 2-pizza team services), Shopify (ongoing strangler migration).

---

## 2. Service Discovery

### Problem
In microservices, services scale up/down constantly. How does Service A know Service B's current IPs?

### Client-Side Discovery
```
Service A → Service Registry (Eureka/Consul) → gets list of Service B IPs
         → picks one (load balance client-side) → calls Service B directly

Pros: No extra hop, client controls load balancing
Cons: Client must implement discovery logic; language-specific
```

### Server-Side Discovery
```
Service A → Load Balancer/API Gateway → queries registry → routes to Service B

Pros: Client is dumb (just calls the LB)
Cons: Extra hop; LB is another component to manage
```

### Service Registry Tools
| Tool | Type | Use |
|------|------|-----|
| **Eureka** (Netflix) | Client-side | Spring Cloud microservices |
| **Consul** (HashiCorp) | Both | Multi-platform, health checks |
| **Zookeeper** | Coordination | Kafka broker discovery |
| **Kubernetes DNS** | Server-side | In-cluster service discovery |
| **AWS Cloud Map** | Server-side | AWS ECS/EKS services |

### 🏭 Industry Examples
- **Netflix**: Eureka registry handles discovery for 700+ microservices. Each service registers on startup, deregisters on shutdown.
- **Uber**: Consul-based service discovery with health checks for all microservices.
- **Kubernetes**: Built-in DNS-based discovery — `http://payment-service` resolves to ClusterIP automatically.

---

## 3. Circuit Breaker

### Problem
Service A calls Service B. Service B is slow/down. Service A's threads pile up waiting → Service A goes down too → **Cascading failure**.

### Circuit Breaker States
```
CLOSED (normal) ──────────────────────────────────────────────────────►
    │ failures exceed threshold                                         │
    ▼                                                                  │
OPEN (failing fast) ──► reject all calls immediately ──► timeout ──►   │
    │                   (don't wait for B)              half-open      │
    ▼                                                                  │
HALF-OPEN (testing) ──► allow 1 test call ──► SUCCESS → CLOSED        │
                                          └── FAIL   → OPEN again ────┘
```

### Implementation
```java
// Resilience4j (standard Java circuit breaker library)
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)          // open if 50% of calls fail
    .waitDurationInOpenState(Duration.ofSeconds(30)) // stay open 30s
    .permittedNumberOfCallsInHalfOpenState(5) // 5 test calls
    .slidingWindowSize(10)             // evaluate last 10 calls
    .build();

CircuitBreaker cb = CircuitBreaker.of("paymentService", config);

// Wrap your call
String result = cb.executeSupplier(() -> paymentService.charge(amount));
// If circuit is OPEN → throws CallNotPermittedException immediately
// → fallback: return cached result or error response
```

### Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Prevents cascading failures | False positives (brief spike trips breaker) |
| Fails fast (no thread piling) | State management complexity |
| Automatic recovery | Need good fallback strategy |
| Observability (metrics per state) | Tuning thresholds for each service |

### 🏭 Industry Examples
- **Netflix Hystrix**: First major circuit breaker library. Inspired the pattern for the industry.
- **Resilience4j**: Modern replacement for Hystrix, used by Spring Boot apps globally.
- **Amazon**: Every service-to-service call uses circuit breakers. Mandatory in their architecture review process.
- **Alibaba Sentinel**: Circuit breaker + rate limiter for Java microservices at Alibaba scale.

---

## 4. Bulkhead Pattern

### Problem
All requests share the same thread pool. One slow service consumes all threads → other services also fail.

### Solution: Isolate Resources Per Service
```
Without Bulkhead:                    With Bulkhead:
[Thread Pool: 100 threads]           [Payment Pool: 30] [Order Pool: 30] [User Pool: 40]
All services share → Payment         Payment slow → only payment pool      
slow → all 100 threads busy          exhausted → Order/User still work
→ everything fails                   → partial failure, not total
```

### Types of Bulkheads
```
Thread Pool Isolation: Each dependency gets its own thread pool
  + Strong isolation
  - More threads = more memory

Semaphore Isolation: Limit concurrent calls per service (no new threads)
  + Less memory overhead  
  - Threads still block if service is slow
```

### 🏭 Industry Examples
- **Netflix**: Separate thread pools for each external API call type (recommendation, billing, streaming).
- **Resilience4j Bulkhead**: Used alongside circuit breaker in Spring Boot for complete fault isolation.

---

## 5. Saga Pattern (Distributed Transactions)

### Problem
Multi-step transaction across microservices — each service has its own DB. ACID transactions don't work across services.

```
Example: Place Order
  1. Order Service: create order
  2. Payment Service: charge card
  3. Inventory Service: reserve stock
  4. Notification Service: send email

If step 3 fails → need to reverse steps 1 and 2 (compensating transactions)
```

### Choreography-Based Saga
```
Order Service     Payment Service    Inventory Service
     │ publishes OrderCreated
     │────────────────────────────►
                  │ publishes PaymentProcessed
                  │──────────────────────────────►
                                     │ publishes StockReserved
                                     │──► (success)
                  
If payment fails:
  Order Service listens to PaymentFailed → cancels order (compensating tx)
```
**Pros**: No central coordinator, loose coupling.
**Cons**: Hard to track overall state, complex debugging, event ordering issues.

### Orchestration-Based Saga
```
Saga Orchestrator
  │──► Order Service: "create order"
  │◄── OrderCreated
  │──► Payment Service: "charge card"
  │◄── PaymentFailed → trigger compensating tx
  │──► Order Service: "cancel order"
```
**Pros**: Central visibility, easier debugging, clear flow.
**Cons**: Orchestrator can become a bottleneck/SPOF.

### vs 2-Phase Commit (2PC)
| | 2PC | Saga |
|--|-----|------|
| Consistency | Strong (ACID) | Eventual |
| Blocking | Yes (locks held) | No |
| Performance | Slow | Fast |
| Failure handling | Coordinator failure = stuck | Compensating transactions |
| **Use when** | Financial critical single-DB | Cross-microservice transactions |

### 🏭 Industry Examples
- **Uber**: Orchestration saga for trip booking — driver assignment → payment → receipt generation.
- **Amazon**: Choreography saga for order fulfillment — payment → warehouse → shipping → notification.
- **Airbnb**: Saga for booking — hold dates → charge card → confirm reservation.

---

## 6. CQRS (Command Query Responsibility Segregation)

### What is CQRS?
Separate the **write model** (commands that change state) from the **read model** (queries that return data).

```
Traditional:                         CQRS:
                                    
POST /orders → OrderDB              POST /orders → Command Handler → Write DB
GET /orders/{id} ← OrderDB                                         → Event Bus
                                    GET /orders/{id} ← Query Handler ← Read DB
                                         (optimized for reads, denormalized)
```

### Why CQRS?
```
Write side: Normalized, ACID, validates business rules
Read side:  Denormalized, cached, optimized for query patterns
           (JOIN pre-computed, materialized views, search indexes)

Result: 10x faster reads (no joins), independent scaling of reads/writes
```

### CQRS + Event Sourcing
```
Instead of storing current state, store all EVENTS that led to the state:

Write: OrderPlaced, PaymentReceived, OrderShipped, OrderDelivered
       (append-only log)

Read:  Project events → current state (replay to rebuild any view)
       Different projections for different read models

Benefits: Full audit trail, time-travel queries, replay to fix bugs
```

### Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Read/write independently scaled | Eventual consistency between models |
| Optimized read models per use case | Higher complexity, more code |
| Full audit trail (with event sourcing) | Eventual consistency is hard to reason about |
| Supports time-travel / replay | Learning curve for teams |

### 🏭 Industry Examples
- **Microsoft**: Azure Event Sourcing + CQRS patterns used in Azure DevOps.
- **LinkedIn**: CQRS for feed system — write side appends feed items, read side maintains per-user feed caches.
- **Axon Framework**: Popular Java CQRS/Event Sourcing framework used by many enterprises.
- **kACE**: Screen layout config follows CQRS — writes go to PostgreSQL (normalized), reads use React Query with denormalized merged layouts.

---

## 7. Event Sourcing

### Core Concept
Store the **sequence of events** that happened, not the current state. State is derived by replaying events.

```
Traditional:       Event Sourcing:
Account {          Events:
  balance: 500       AccountOpened(100)
}                    MoneyDeposited(300)
                     MoneyWithdrawn(200)
                     MoneyDeposited(300)
                     
                   Current state = replay all events = 500 ✅
```

### Key Properties
```
1. Append-only log — events never deleted or modified
2. Full audit trail — every state change is recorded
3. Time travel — replay events to see state at any point in time
4. Event replay — fix bugs by correcting projection logic and replaying
5. Multiple projections — same events → different read models
```

### Advantages & Tradeoffs
| Advantage | Tradeoff |
|-----------|---------|
| Complete audit trail | Event log grows forever (snapshots needed) |
| Temporal queries (state at time T) | Complex to implement correctly |
| Bug fixes via replay | Eventual consistency on projections |
| Multiple read model projections | Schema evolution of events is hard |

### 🏭 Industry Examples
- **Banking systems**: All financial transaction systems are effectively event-sourced (journal of debit/credit entries).
- **Git**: A version control system is event-sourced — commits are events, current state is derived by replaying.
- **Kafka**: Acts as the event store for event-sourced systems. Kafka's log retention enables replay.
- **Axon/Eventuate**: Java frameworks for event sourcing used in enterprise microservices.

---

## 8. Sidecar / Service Mesh

### Sidecar Pattern
Deploy a helper container alongside each service container to handle cross-cutting concerns.

```
Pod:
┌────────────────────────────────┐
│  App Container  │  Sidecar     │
│  (business      │  (Envoy      │
│   logic)        │   proxy)     │
│                 │  - TLS       │
│                 │  - metrics   │
│                 │  - tracing   │
│                 │  - retries   │
└────────────────────────────────┘
```

### Service Mesh (Istio/Linkerd)
```
All east-west traffic goes through sidecar proxies:
  Service A → [Envoy Sidecar A] ──TLS──► [Envoy Sidecar B] → Service B

Control Plane (Istio Pilot):
  - Distributes routing rules to all sidecars
  - Manages certificates (mTLS everywhere)
  - Collects telemetry

Benefits:
  - Zero-trust networking (mTLS between all services)
  - Traffic management (canary, A/B, weighted routing)
  - Observability (distributed tracing, metrics out of the box)
  - Retries/timeouts without code changes
```

### 🏭 Industry Examples
- **Lyft**: Invented Envoy proxy (now CNCF). Powers Istio's data plane.
- **Google**: Istio was co-developed by Google, IBM, Lyft. Used internally at Google via Traffic Director.
- **Airbnb**: Uses service mesh for mTLS and observability across 300+ microservices.

---

## Interview Q&A

**Q: Monolith vs microservices — which would you choose for a new startup?**
A: Start with a modular monolith. Premature microservices is a common mistake — you pay operational overhead before understanding domain boundaries. Extract services as teams and scaling needs justify it. Amazon, Netflix, and Uber all started as monoliths.

**Q: How do you handle data consistency in microservices without 2PC?**
A: Use the Saga pattern with compensating transactions. For strong consistency needs, consider keeping tightly-coupled operations in the same service/DB. Accept eventual consistency for cross-service data. Use idempotency keys for safe retries.

**Q: What's the difference between Saga and Event Sourcing?**
A: Saga is about managing distributed transactions (coordinate steps + rollback). Event Sourcing is about how you store state (as events rather than current state). They're complementary — event sourcing provides the natural event log that sagas can react to.

**Q: When would you NOT use a service mesh?**
A: Small teams with few services (overhead not worth it), teams without Kubernetes expertise, latency-critical paths where sidecar hop is unacceptable (< 1ms SLA), brownfield apps that can't be containerized.

**Q: How does the circuit breaker know when to transition to HALF-OPEN?**
A: After `waitDurationInOpenState` (typically 30-60 seconds), it automatically transitions to HALF-OPEN and allows a configurable number of probe requests. If they succeed → CLOSED. If they fail → OPEN again. This prevents constantly hammering a recovering service.
