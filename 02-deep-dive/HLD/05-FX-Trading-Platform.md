# 📚 HLD Deep Dive — FX Options Trading Platform (kACE-style)

---

## 🎯 Problem Statement
Design a high-performance FX Options trading platform supporting real-time pricing, RFQ (Request for Quote) workflows, multi-leg option strategies, order management, and live market data distribution to traders.

---

## Step 1: Clarify Requirements

### Functional
- Real-time FX option pricing (European, TARF, Barrier, Digital, Touch products)
- RFQ workflow: trader requests quote → market maker prices → trader accepts/rejects
- Multi-leg strategy support (straddle, strangle, butterfly, condor — up to 52 strategies × 4 legs)
- Order management (create, amend, cancel orders)
- Live market data streaming (spot rates, vol surface updates)
- Dealing settings management (trading limits, currency pairs)
- User roles: Trader, Market Maker, Sales, Admin

### Non-Functional
- **Latency**: Price updates < 50ms; RFQ response < 200ms
- **Availability**: 99.99% during market hours (global FX markets = 24/5)
- **Consistency**: Prices must be consistent — no stale prices shown as live
- **Throughput**: 10K price updates/sec; 1K RFQs/sec
- **Auditability**: Every trade action logged with timestamp and user

---

## Step 2: Estimation

```
Users: 10K traders globally; 500 market makers
Price updates: 100 currency pairs × 100 vol surface points = 10K updates/sec
RFQs: 1K/sec peak
WebSocket connections: 10K concurrent (one per trader terminal)
Strategies: 52 strategies × 4 legs × 200+ fields = 41,600 cells per pricing grid

Storage/day:
  RFQ audit log: 1K RFQ/sec × 86,400 = 86M records × 1KB = 86 GB/day
  Price history: 10K updates/sec × 86,400 × 100 bytes = 86 GB/day
  Trade records: 10K trades/day × 2KB = 20 MB/day
```

---

## Step 3: High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    Trader Terminal (React 19)                     │
│   Pricing Grid (TanStack Form) | RFQ Panel | Dealing Settings    │
└─────────────────────────────────┬────────────────────────────────┘
                                  │ WebSocket (STOMP) + REST
┌─────────────────────────────────▼────────────────────────────────┐
│                    API Gateway (Spring Boot)                      │
│   JWT Validation | Rate Limiting | WebSocket Upgrade | Routing   │
└────┬────────────────────┬────────────────────┬───────────────────┘
     │                    │                    │
┌────▼────────┐  ┌────────▼──────┐  ┌─────────▼──────────┐
│  Pricing    │  │  RFQ Service  │  │  Market Data Svc   │
│  Service    │  │               │  │                    │
└────┬────────┘  └────────┬──────┘  └─────────┬──────────┘
     │                    │                    │
     └──────────┬──────────┘                  │
                │                             │
         ┌──────▼──────┐              ┌───────▼───────┐
         │    Kafka    │              │ Market Data   │
         │  (events)   │              │ Feed (FIX/    │
         └──────┬──────┘              │  Bloomberg)   │
                │                    └───────────────┘
     ┌──────────┼──────────┐
     ▼          ▼          ▼
 Trade DB   Audit Log   Analytics
 (Postgres) (Cassandra)  (ClickHouse)
     │
 Redis Cache
 (dropdown cache,
  session, pricing state)
```

---

## Step 4: Pricing Service — Deep Dive

### Pricing Grid Architecture
```
Challenge: 52 strategies × 4 legs × 200+ fields = 41,600 cells
  Each cell can be: user-input (blue) or calculated (black)
  Field clearing: changing one field cascades to dependent fields

Components:
  Frontend: TanStack Form v1 (manages 41,600 field subscriptions)
  Optimization: 
    - Batch setFieldValue calls: 200+ → 2 batch updates
    - useFormValues hook with shared cache (avoid redundant subscriptions)
    - @tanstack/react-virtual for viewport rendering
    - Frozen panes for row/column headers

Field Clearing System (PCLEAR.CPP equivalent in TypeScript):
  clearingLayoutUtils.ts:
    - FIELD_CLEAR_MAPPING: declarative rules per field
    - ClearingRule: {trigger, targets, conditions}
    - clearAllBehavior: clear all calculated fields in leg
    - statusFilterOtherLegs: only clear black-status (calculated) fields
      in other legs, preserve blue-status (user-entered) fields
    - Retain rules: keep certain fields even when clearing
    - Cascading: clearing A triggers clearing B triggers clearing C
```

### Pricing Calculation Flow
```
1. Trader enters spot rate in Leg 1
2. Field clearing triggers (clearingLayoutUtils):
   - Clear calculated fields in other legs
   - Preserve user-entered fields in other legs
3. Send pricing request to Pricing Service via WebSocket
4. Pricing Service:
   - Fetches vol surface from Market Data Service
   - Runs Black-Scholes / binomial model per leg
   - Returns delta, gamma, vega, theta, premium per leg
5. Update grid via batch setFieldValue (2 calls, not 200+)
6. Calculated fields shown in black; user fields in blue
```

---

## Step 5: RFQ System — Deep Dive

### RFQ Workflow
```
Trader → [Send RFQ] → API Gateway → RFQ Service
                                        │
                                    Kafka Topic: rfq.new
                                        │
                              ┌─────────▼──────────┐
                              │  Market Maker Pool  │
                              │  (receives via WS)  │
                              └─────────┬──────────┘
                                        │ [Price Quote]
                                    Kafka Topic: rfq.quoted
                                        │
                              ┌─────────▼──────────┐
                              │   RFQ Service       │
                              │   - Timer (30s)     │
                              │   - Best quote      │
                              └─────────┬──────────┘
                                        │
                              Trader receives quote via WebSocket
                                        │
                              [Accept / Reject / Counter]
                                        │
                              Kafka Topic: rfq.accepted
                                        │
                              Order Management Service → Trade DB
```

### RFQ State Machine
```
States: PENDING → QUOTED → ACCEPTED / REJECTED / EXPIRED / CANCELLED

Transitions:
  PENDING  →  QUOTED    (market maker prices)
  QUOTED   →  ACCEPTED  (trader accepts)
  QUOTED   →  REJECTED  (trader rejects)
  QUOTED   →  EXPIRED   (30s timer)
  PENDING  →  CANCELLED (trader cancels before quote)
  ACCEPTED →  FILLED    (order executed)
  ACCEPTED →  FAILED    (execution failure)
```

### RfqDeltaPollingService (kACE Pattern)
```java
// Pull-based RFQ delta instead of push-all
// Problem: 10K traders each subscribed to RFQ updates = 10K WebSocket pushes per update
// Solution: Each trader polls for their delta since last known state

@Service
public class RfqDeltaPollingService {
    // GET /api/rfq/delta?since={lastSequenceNumber}&userId={id}
    // Returns only changed RFQs since last poll
    // Client polls every 1 second when RFQ panel is open
    // Saves: 10K pushes → only affected traders receive updates
}
```

---

## Step 6: Market Data Distribution

### Vol Surface Update Flow
```
Bloomberg/Reuters Feed
        │
Market Data Service (FIX protocol parser)
        │
Kafka Topic: market-data.vol-surface (partitioned by currency pair)
        │
Vol Surface Processor (consumer)
        │
Redis (vol surface cache, TTL 5 seconds)
        │
Pricing Service reads on each pricing request
```

### WebSocket Distribution to Traders
```
Problem: 10K traders need spot rate updates; rates update 100 times/second
Total events: 100 updates/sec × 10K traders = 1M pushes/sec

Solutions:
  1. Topic-based subscription: trader subscribes to specific pairs
     → Only push EURUSD updates to traders with EURUSD in their grid
     → Reduces from 1M to 100K pushes/sec (10% of traders use each pair)

  2. Delta compression: only send changed fields
     → Full vol surface = 100 points × 4 bytes = 400 bytes
     → Delta update = only changed points = 10-50 bytes

  3. Batching: aggregate 100ms of updates, send as single WebSocket frame
     → Reduce overhead, acceptable for pricing (50ms latency target)
```

---

## Step 7: Screen Layout Config System

### Layout Hierarchy
```
Priority resolution (SQL priority-based fallback):
  1. User-specific layout (highest priority)
  2. Class-specific layout (role-based: TRADER, SALES)
  3. Product-specific layout (per FX product type)
  4. Default layout (fallback)

useMergedLayout Hook:
  - Fetches all applicable layouts in parallel (React Query)
  - applyClassSpecificLayout: deep merge with priority
  - Changes cached in React Query; invalidated on config update

DB Schema (from kACE Confluence):
  screen_layouts(id, screen_id, class_id, user_id, priority, config_json)
  Priority 1 = highest; fallback: highest priority config wins per field
```

---

## Step 8: Scaling Strategies

### WebSocket Scaling

> **The problem:** Trader A's WebSocket lives on Gateway Instance 1. The Pricing Service (stateless, runs on Instance 3) computes a new price for rfq:{rfqId}. Instance 3 has no WebSocket connection to anyone — how does the price reach Trader A on Instance 1?

**Answer: Redis Pub/Sub via SubscriptionRegistry**

```
Problem: WebSocket = stateful; can't route to any server
Solution 1: Sticky Sessions (IP Hash at LB)
  - Same user always goes to same Gateway instance
  - Downside: server failure loses all sessions on that instance

Solution 2: Redis Pub/Sub + SubscriptionRegistry (kACE approach)
  - SubscriptionRegistry: Redis stores {topic → [sessionId:instanceId]}
  - Any instance can handle any event:
      Pricing Service: PUBLISH channel:rfq:{rfqId}  {priceUpdate}
      All Gateway instances subscribed to this channel
      Instance 1 holds Trader A's socket → receives msg → pushes ✅
      Instance 2/3 receive msg → no socket for this session → ignore
  - Heartbeat: TTL 60s; client reconnects on disconnect
  - Adding gateway instances: new instance subscribes to Redis → immediately works
```

> 📖 For full breakdown of all multi-instance SSE/WebSocket scaling patterns (Redis Pub/Sub, sticky sessions, dedicated gateway, consistent hashing — with diagrams and tradeoffs), see `12-Communication-Patterns.md` → Section 5.

### Pricing Service Scaling
```
Stateless: Each pricing request independent
→ Horizontal scaling: 50 pricing service instances
→ Load balancing: Round robin (all equivalent)
→ Caching: Vol surface in Redis (shared across instances)
→ Circuit breaker: If market data unavailable → return last known price + staleness flag
```

---

## Step 9: Audit & Compliance

```
Every action must be logged for regulatory compliance (MiFID II):
  - WHO made the request (userId, role)
  - WHAT action (RFQ sent, price accepted, order placed)
  - WHEN (timestamp to millisecond)
  - FROM WHERE (IP, terminal ID)
  - WHAT CHANGED (before/after state)

Architecture:
  - All write operations publish audit event to Kafka (audit-log topic)
  - Audit Consumer writes to Cassandra (append-only, immutable)
  - Cassandra: partition by user_id + date; cluster by timestamp
  - Retention: 7 years (regulatory requirement)
  - Query: GET /audit?userId=...&from=...&to=... (pagination)
```

---

## Step 10: Trade-offs

| Decision | Choice | Reason |
|----------|--------|--------|
| Real-time | WebSocket STOMP | Bi-directional, low latency, broker pattern |
| Pricing state | TanStack Form v1 | Fine-grained subscriptions, batching |
| Market data | Kafka fan-out | Decouple feed from consumers; replay |
| RFQ routing | Kafka + pull delta | Scale to 10K traders without push thundering herd |
| Layout config | DB + React Query | Lazy load; cache with React Query |
| Audit | Cassandra append-only | Write-heavy, immutable, time-series queries |
| Dropdown cache | Redis + Spring StaticCacheOrchestrator | 200 fields pre-loaded; O(1) lookup |
| Auth | 3-token JWT + RSA | Stateless verification; microservice-friendly |
