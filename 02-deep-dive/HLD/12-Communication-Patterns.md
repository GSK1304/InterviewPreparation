# 📚 HLD Core Concepts — Communication Patterns

---

## 1. Long Polling vs WebSocket vs SSE vs gRPC

### Long Polling
```
Client → Server: "Any new data?"
Server: (holds connection open until data available or timeout)
Server → Client: "Here's new data" (or timeout after 30s)
Client: immediately opens a new long poll request

Problems:
  - Each "push" requires a new HTTP request
  - HTTP overhead per request (headers ~0.5KB)
  - Server must manage many held connections
  - Not true real-time (latency = time between polls)

Use when: Simple push notifications, legacy system compatibility
```

### WebSocket
```
Client → Server: HTTP Upgrade request
Server → Client: 101 Switching Protocols
Client ←──────────── Server: bidirectional full-duplex over single TCP connection

Properties:
  + True bidirectional (server pushes WITHOUT client request)
  + Low overhead after handshake (2–10 bytes per frame vs ~500 bytes HTTP)
  + Low latency (no new connection per message)
  - Stateful connection (server holds open TCP socket per client)
  - Doesn't work well with HTTP load balancers (needs sticky sessions or Redis pub/sub)
  - No built-in reconnection (client must implement)

Use when: Chat, live trading UI, multiplayer games, collaborative editing, real-time dashboards
```

### SSE (Server-Sent Events)
```
Client → Server: HTTP GET /events (standard HTTP request with Accept: text/event-stream)
Server → Client: stream of events (never closes the response)

data: {"type": "price_update", "symbol": "EURUSD", "price": 1.0832}\n\n
data: {"type": "rfq_update", "rfqId": "123", "status": "quoted"}\n\n

Properties:
  + Simpler than WebSocket (standard HTTP, works through proxies)
  + Built-in reconnection (browser auto-reconnects with Last-Event-ID)
  + One direction only: server → client
  - No client → server streaming
  - Some browsers limit concurrent SSE connections (use HTTP/2 to fix)

Use when: Live feeds, notifications, dashboards where server pushes but client only reads
```

### gRPC
```
HTTP/2 + Protocol Buffers (binary serialization)

Proto definition:
  service PricingService {
    rpc GetPrice(PriceRequest) returns (PriceResponse);          // Unary
    rpc StreamPrices(PriceRequest) returns (stream PriceUpdate); // Server streaming
    rpc UploadLegs(stream LegData) returns (StrategyResult);     // Client streaming
    rpc BidirectionalTrade(stream TradeMsg) returns (stream TradeMsg); // Bidirectional
  }

Properties:
  + 5-10x faster than REST (binary + HTTP/2 multiplexing)
  + Strong typing (proto schema enforced at compile time)
  + Auto-generated clients in 10+ languages
  + Native streaming support (all 4 modes above)
  - Not browser-native (needs gRPC-Web proxy for browsers)
  - Binary format (harder to debug with curl/Postman)
  - Schema evolution requires care (field numbers, backward compat)

Use when: Internal microservice-to-microservice, high-throughput, polyglot environments
```

### Comparison Table
| | Long Polling | WebSocket | SSE | gRPC |
|--|-------------|-----------|-----|------|
| Direction | Client request-driven | Bidirectional | Server→Client only | All modes |
| Protocol | HTTP/1.1 | WS | HTTP | HTTP/2 |
| Browser support | ✅ | ✅ | ✅ | ⚠️ (needs gRPC-Web) |
| Load balancer friendly | ✅ | ❌ (sticky sessions) | ✅ | ✅ (HTTP/2 stream) |
| Overhead | High (HTTP per req) | Low | Low | Very low (binary) |
| Auto-reconnect | Client must implement | Client must implement | ✅ Built-in | Client must implement |
| **Best for** | Simple notifications | Real-time bidirectional | Server push feeds | Internal microservices |

---

## 2. Synchronous vs Asynchronous Communication

### Synchronous (Request-Response)
```
Service A ──── request ────► Service B
Service A ◄─── response ──── Service B (blocks waiting)

Pros: Simple, immediate feedback, easy error handling
Cons: Tight coupling, both services must be up, timeout management

Use when: User-facing operations needing immediate response, simple CRUD
```

### Asynchronous (Message-Based)
```
Service A ──► [Message Queue] ──► Service B
         (fire and forget)

Service A doesn't wait; continues processing
Service B processes when ready

Pros: Loose coupling, higher throughput, built-in buffering, retry on failure
Cons: Eventual consistency, harder debugging, complex error handling

Use when: Background processing, event fan-out, decoupling write spikes
```

### Message Queue vs Event Streaming

| | Message Queue (RabbitMQ, SQS) | Event Streaming (Kafka) |
|--|-------------------------------|------------------------|
| **Message lifetime** | Deleted after consumption | Retained for days/weeks |
| **Consumer model** | Competing consumers (one gets message) | Consumer groups (each group gets all) |
| **Replay** | ❌ No replay after ACK | ✅ Replay from any offset |
| **Ordering** | Per-queue FIFO | Per-partition ordering |
| **Throughput** | Moderate (10K-100K/s) | Very high (1M+/s) |
| **Use case** | Task queues, work distribution | Event sourcing, audit log, analytics |

### 🏭 Industry Examples
- **WhatsApp**: WebSocket for message delivery (bidirectional), Long polling fallback for restrictive networks.
- **GitHub Actions**: SSE for live build log streaming to browser.
- **Netflix gRPC**: All internal microservice communication, especially between data services.
- **Slack**: WebSocket for real-time message delivery + offline sync via REST.

---

## 3. Backpressure and Flow Control

### The Problem
```
Producer sends 10,000 msg/sec
Consumer processes 1,000 msg/sec
Queue grows by 9,000 msg/sec → OOM → crash
```

### Backpressure Solutions

**Drop**:
```
If queue full → drop new messages
Use: Metrics (losing a few data points is OK), UDP-based telemetry
Risk: Data loss
```

**Block**:
```
Producer blocks (waits) until consumer catches up
Use: Bounded blocking queues (Java's LinkedBlockingQueue)
Risk: Producer throughput tied to consumer speed
```

**Buffer with bounded queue + rejection**:
```
Accept up to N messages in queue
After N: return 429 (Too Many Requests) to producer
Use: API rate limiting, microservice overload protection
```

**Load shedding**:
```
System identifies low-priority requests and drops them under load
Priority: Auth > critical operations > best-effort analytics
Netflix: drops recommendation requests to protect core playback
```

**TCP Flow Control (built-in)**:
```
Receiver advertises "receive window" size (how much buffer is free)
Sender slows down if window fills up
This is the OS-level backpressure for all TCP connections
```

### Reactive Streams / Project Reactor
```java
// Java: backpressure-aware streaming
Flux.range(1, 1_000_000)
    .onBackpressureBuffer(1000)      // buffer up to 1000; error if exceeded
    .publishOn(Schedulers.parallel())
    .subscribe(item -> process(item));
```

### 🏭 Industry Examples
- **Kafka**: Consumer can pause/resume partitions; built-in backpressure via consumer.pause().
- **Reactive Spring / Project Reactor**: Backpressure-aware async processing used in kACE's WebFlux gateway.
- **Netflix**: Adaptive concurrency limiter — automatically adjusts max concurrent requests based on measured latency.

---

## 4. Push vs Pull Architecture

### Pull (Polling)
```
Client polls server periodically:
  GET /feed?since=last_poll_time

Pros:
  + Client controls timing
  + Simple server (stateless, no client tracking)
  + Works through any proxy/firewall

Cons:
  - Wasted requests when nothing new
  - Latency = poll interval
  - Not truly real-time

Use: News feed with acceptable latency, batch data sync, scheduled jobs
```

### Push
```
Server pushes to client when data available:
  WebSocket/SSE: server sends update immediately

Pros:
  + True real-time
  + No wasted requests
  + Lower latency

Cons:
  - Server must track all connected clients
  - Harder to scale (stateful connections)
  - Client must be connected (offline delivery problem)

Use: Chat, live trading prices, collaborative editing, notifications
```

### Hybrid: Pub/Sub with Fan-out
```
Publisher → [Message Broker (Kafka/Redis)] → Subscribers

Publisher doesn't know about subscribers
New subscribers just consume from topic
Works at massive scale (10K+ subscribers per topic)

Fan-out on write: publish → broker pushes to all subscribers' queues
Fan-out on read: subscribers pull from shared topic

Example: Twitter timeline
  Write: User tweets → pushed to followers' timeline feeds (fan-out on write)
  For users with 1M+ followers: fan-out on read (fetch on demand)
```

---

## 5. Scaling SSE and WebSocket Across Multiple Instances

This is one of the most commonly asked follow-up questions in system design interviews. Understanding it deeply separates candidates who understand distributed systems from those who only know the protocol.

### The Core Problem

SSE and WebSocket create **persistent, stateful TCP connections** — a client's socket lives on exactly one server instance. This breaks horizontal scaling.

```
Client A ──SSE──► Instance 1   ← socket lives HERE
Client B ──SSE──► Instance 2
Client C ──SSE──► Instance 3

Order event fires → hits Instance 2's REST handler
Client A needs to be notified → but Instance 2 has no socket to Client A

Instance 2 cannot push to Client A directly. ❌
```

Compare this with REST, which is stateless — any instance can handle any request. SSE/WebSocket don't have this property, and this is the design problem you must solve.

---

### Solution 1: Redis Pub/Sub (Standard Production Answer)

Every instance subscribes to Redis channels. When an event fires on any instance, it publishes to Redis. Redis fans it out to all instances. Only the instance that owns the socket actually delivers it.

```
                     Redis Pub/Sub
                ┌──────────────────────────┐
                │  Channel: order:{orderId} │
                └──────────────────────────┘
                     ▲              │
                     │ PUBLISH      │ broadcasts to ALL subscribers
                     │              ▼
 ┌──────────────┐   ┌─────────────────────────────────────────────┐
 │ Order Service│──►│  Instance 1    Instance 2    Instance 3      │
 │ (any node)   │   │  (has ClientA) (has ClientB) (has ClientC)  │
 └──────────────┘   └─────────────────────────────────────────────┘
                          │
                     Client A receives update ✅
                     Instance 2 & 3: receive Redis msg but have no
                     socket for Client A → silently ignore
```

**Step-by-step flow:**
```
1. Client A opens SSE to Instance 1
   → Instance 1 registers: SUBSCRIBE channel:user:{userId}

2. Order is placed → Order Service (on Instance 2) processes it
   → PUBLISH channel:user:{userId}  {"event":"order.delivered","data":{...}}

3. Redis delivers to ALL instances subscribed to that channel

4. Instance 1 receives message → has Client A's open socket → pushes SSE event ✅
   Instance 2 receives message → no socket for Client A → ignores
   Instance 3 receives message → no socket for Client A → ignores
```

**Channel naming patterns:**
```
Per user:      channel:user:{userId}          → all events for a user
Per resource:  channel:order:{orderId}         → order status updates
Per topic:     channel:rfq:{rfqId}             → trading RFQ updates
Per room:      channel:doc:{docId}             → collaborative editing
```

**Why this works at scale:**
- Redis Pub/Sub delivers in < 1ms
- Each instance only holds connections for its own clients — memory efficient
- Adding instances: new instance subscribes to Redis channels → immediately participates
- Removing instances: clients reconnect to other instances → Redis subscriptions migrate

**🏭 Industry Examples:**
- **Socket.io**: built-in Redis adapter does exactly this — `const io = new Server({ adapter: createAdapter(pubClient, subClient) })`
- **Discord**: Redis Pub/Sub for real-time message fan-out across gateway servers
- **Slack**: Redis Pub/Sub for cross-instance WebSocket delivery
- **kACE**: `SubscriptionRegistry` in Redis maps topics → session IDs; STOMP gateway uses pub/sub for cross-instance RFQ delivery

---

### Solution 2: Sticky Sessions (Simpler, Limited Scale)

Configure the load balancer to always route a given client to the same instance using IP hash or a session cookie.

```
Client A → Load Balancer (IP hash) → always Instance 1
Client B → Load Balancer            → always Instance 2
Client C → Load Balancer            → always Instance 3

Event fires on Instance 2 → needs to reach Client A (Instance 1)
→ Still broken unless combined with Redis Pub/Sub or direct inter-instance call
```

**Sticky sessions alone don't solve the cross-instance push problem** — they just ensure the client always reconnects to the same instance. If an event fires on a *different* instance, you still need a messaging layer.

**Where sticky sessions help:**
- Reduce reconnection overhead (client reconnects to same instance after brief disconnect)
- Useful for stateful protocols that need session affinity (legacy WebSocket without Redis)
- Combined with Redis Pub/Sub: sticky sessions reduce unnecessary Redis messages (same-instance events go direct)

**Tradeoffs:**
```
✅ Simple LB config (ip_hash in Nginx)
✅ Reduces Redis overhead for same-instance events
❌ Instance crash → all that instance's clients must reconnect (possibly to different instance)
❌ Uneven load if some clients are more active
❌ Complicates auto-scaling (new instances don't get old clients)
❌ Doesn't solve cross-instance event delivery alone
```

---

### Solution 3: Dedicated Connection Gateway

A separate service owns ALL persistent connections. Application servers publish events to it; it handles delivery.

```
Client A ──SSE──►  ┌──────────────────────────────┐
Client B ──SSE──►  │   SSE / WebSocket Gateway     │◄── any app server publishes here
Client C ──SSE──►  │   (owns all sockets)          │    POST /push {userId, event, data}
                   └──────────────────────────────┘
                   Internally uses Redis Pub/Sub
                   across its own instances
```

**Managed services that are this pattern:**
- **Pusher** / **Ably**: SaaS real-time message delivery
- **AWS API Gateway WebSocket**: managed WebSocket with Lambda backends
- **Firebase Realtime Database**: managed SSE-like delivery

**Build-it-yourself:** Dedicated Node.js cluster with Redis adapter (Socket.io pattern)

**Tradeoffs:**
```
✅ Application servers stay stateless (just POST to push API)
✅ Clean separation of concerns
✅ Gateway scales independently
❌ Extra network hop for every push
❌ Additional operational complexity / cost
```

---

### Solution 4: Consistent Hashing for Connection Affinity

For systems with many topics/resources (like trading), assign each resource to a specific instance using consistent hashing.

```
hash(docId) % numInstances → Instance 2 owns this document's connections
hash(rfqId) % numInstances → Instance 1 owns all connections for this RFQ

API Gateway routes:
  WebSocket for rfq:{rfqId} → always to Instance 1
  WebSocket for rfq:{rfqId2} → always to Instance 3

When event fires for rfq:{rfqId} on any service:
  → Route to Instance 1 directly (no Redis needed for this event)
```

**Tradeoffs:**
```
✅ No Redis overhead for events (direct routing)
✅ Predictable — you know which instance owns a resource
❌ Instance failure → must rehash and migrate connections
❌ Needs smart API Gateway that understands resource routing
❌ More complex than Redis Pub/Sub
```
Used by: kACE's document-affinity routing concept, real-time trading systems

---

### Comparison and When to Use Each

| Solution | Complexity | Scale | Failure Tolerance | Best For |
|----------|-----------|-------|------------------|---------|
| **Redis Pub/Sub** | Medium | Very high | High (Redis HA) | Standard production choice |
| **Sticky sessions alone** | Low | Medium | Low (instance crash = reconnect) | Simple apps, internal tools |
| **Sticky + Redis Pub/Sub** | Medium | High | High | Most real-world systems |
| **Dedicated gateway** | High | Very high | High | Large scale, clean architecture |
| **Consistent hashing** | High | High | Medium | Resource-partitioned systems |

**The answer interviewers expect:**

> "Each SSE/WebSocket connection is tied to one server instance. When an event fires on a different instance, it can't push directly. The standard solution is Redis Pub/Sub — the instance handling the event publishes to a channel keyed by userId or resourceId. All instances subscribe to that channel. The instance holding the socket receives the Redis message and pushes to the client. This fully decouples event generation from connection ownership and works across any number of instances."

---

### How This Applies to Each System in This Repo

| System | How It Solves Multi-Instance Push |
|--------|----------------------------------|
| **Chat (WhatsApp)** | Service discovery (user → server mapping in Redis) + inter-server Kafka routing |
| **Food Delivery** | Redis Pub/Sub on order channel; driver location SSE fan-out |
| **Uber** | Redis GEOADD + Pub/Sub for trip location stream |
| **Google Docs** | Redis Pub/Sub for presence updates; document-affinity for OT server |
| **Stock Exchange** | Consistent hashing (symbol → matching engine instance); UDP multicast for market data |
| **kACE Trading** | Redis SubscriptionRegistry + Pub/Sub across STOMP gateway instances |
| **Notification System** | Kafka consumer → WebSocket gateway (dedicated connection layer) |

---

## Interview Q&A

**Q: When would you use WebSocket over SSE?**
A: WebSocket when client also needs to send data to server (chat, gaming, collaborative editing). SSE when server only pushes to client (price feeds, notifications, live logs). SSE is simpler and works better with standard HTTP load balancers.

**Q: How do you scale WebSocket connections to millions?**
A: Each server holds N connections (typically 10K-100K per instance). For routing: use sticky sessions (IP hash) OR decouple connection from message routing using Redis Pub/Sub (any server can receive a message and forward to the connected client via Redis subscription). Scale horizontally — add more WebSocket servers.

**Q: gRPC vs REST — which would you use for a new microservice?**
A: gRPC for internal service-to-service communication (faster, type-safe, streaming support). REST for external/public APIs (better tooling, easier consumption, browser-native). Common pattern: public API = REST, internal = gRPC.

**Q: How does Kafka handle backpressure?**
A: Producers: if broker is slow, producer blocks (default) or throws exception (if max.block.ms exceeded). Consumers: consumer pulls at its own pace — naturally handles backpressure. Consumer can call `consumer.pause()` to stop fetching and `consumer.resume()` when ready. Kafka's retention means data isn't lost even if consumer is slow.

**Q: What's the difference between message queue and event streaming?**
A: Message queue (RabbitMQ, SQS): messages deleted after consumption, competing consumers, work distribution pattern. Event streaming (Kafka): messages retained, all consumer groups see all events, replay from any point. Choose queue for task distribution; choose streaming for event-driven architecture, analytics, and audit trails.
