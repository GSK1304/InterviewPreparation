# 📚 System Design — Food Delivery Platform (Uber Eats / Swiggy / Zomato)

---

## 🎯 Problem Statement
Design a food delivery platform where customers can browse restaurants, place orders, track delivery in real-time, and delivery partners pick up and deliver orders.

---

## Step 1: Clarify Requirements

### Functional
- Browse nearby restaurants and menus
- Place orders (multi-item, special instructions)
- Real-time order tracking (restaurant → delivery partner → customer)
- Assign delivery partners to orders
- Dynamic delivery fee based on distance and demand
- Restaurant management (menu, hours, accepting orders)
- Ratings for restaurants and delivery partners
- Payments (prepaid, COD, wallet)
- Order history and reorder

### Non-Functional
- **Scale**: 50M DAU, 5M orders/day, 500K delivery partners
- **Latency**: Restaurant search < 100ms; order placement < 2s; location updates every 5s
- **Availability**: 99.99% (peak dinner times: 7-9pm)
- **Consistency**: Strong for orders and payments; eventual for location tracking

---

## Step 2: Estimation

```
Orders:          5M/day = 57 orders/sec avg; peak (7-9pm) = ~500 orders/sec
Location updates: 500K active delivery partners × 1 update/5s = 100K updates/sec
Restaurant search: 50M DAU × 3 searches/day = 150M/day = 1,736 QPS avg

Order size:        avg 3 items, avg ₹350 ($4.50)
Daily GMV:         5M × ₹350 = ₹1.75B/day ($21M/day)

Storage:
  Order: ~2KB; 5M/day × 2KB = 10GB/day
  Location: 100K updates/sec × 40 bytes = 4MB/sec (transient, not all stored)
  Menu items: 200K restaurants × avg 100 items × 500 bytes = 10GB
```

---

## Step 3: API Design

```
# Customer APIs
GET  /v1/restaurants?lat=...&lng=...&radius=5km&cuisine=indian
GET  /v1/restaurants/{id}/menu
POST /v1/orders                  → place order
GET  /v1/orders/{id}             → order status + ETA + partner location
POST /v1/orders/{id}/cancel
GET  /v1/orders/{id}/track       → SSE stream for real-time tracking
POST /v1/orders/{id}/rating

# Delivery partner APIs
PUT  /v1/partners/location       → update location + availability
GET  /v1/partners/assignment     → get assigned order
PUT  /v1/orders/{id}/status      → update (picked_up, delivered)

# Restaurant APIs
PUT  /v1/restaurants/{id}/status → open/closed/busy
GET  /v1/restaurants/{id}/orders → incoming orders queue
PUT  /v1/orders/{id}/status      → accepted / ready
PUT  /v1/restaurants/{id}/menu/{itemId} → update item availability
```

---

## Step 4: High-Level Architecture

```
Customers           Delivery Partners         Restaurants
     │                     │                      │
     └─────────────────────┴──────────────────────┘
                           │
               ┌───────────▼────────────┐
               │       API Gateway       │
               │  Auth | Rate Limit | Route │
               └──┬────────┬────────┬───┘
                  │        │        │
          ┌───────▼──┐ ┌───▼───┐ ┌──▼──────────┐
          │Restaurant│ │ Order │ │  Location   │
          │ Service  │ │Service│ │  Service    │
          └───────┬──┘ └───┬───┘ └──────┬──────┘
                  │        │            │
           ┌──────┼────────┼────────────┤
           │    Kafka (event bus)        │
           └──────┬────────┬────────────┘
                  │        │
        ┌─────────▼──┐ ┌───▼──────────────┐
        │  Dispatch  │ │  Notification    │
        │  Service   │ │  Service         │
        │ (matching) │ │ (push, SMS, WS)  │
        └────────────┘ └──────────────────┘
           │
     ┌─────▼─────────────────────────────┐
     │          Databases                 │
     │  MySQL (orders, users, menus)      │
     │  Redis (locations, sessions, cache)│
     │  Cassandra (order history, logs)   │
     └───────────────────────────────────┘
```

---

## Step 5: Order Lifecycle State Machine

```
                        ┌─────────────────┐
                        │    PLACED        │ ← Customer places order
                        └────────┬─────────┘
                                 │ Restaurant accepts (within 3 min)
                        ┌────────▼─────────┐
                        │    ACCEPTED       │ ← Restaurant starts preparing
                        └────────┬─────────┘
                                 │ Delivery partner assigned
                        ┌────────▼─────────┐
                        │  PARTNER_ASSIGNED │ ← Partner heading to restaurant
                        └────────┬─────────┘
                                 │ Partner arrives at restaurant
                        ┌────────▼─────────┐
                        │   PARTNER_AT_     │
                        │   RESTAURANT      │
                        └────────┬─────────┘
                                 │ Food ready, partner picks up
                        ┌────────▼─────────┐
                        │   PICKED_UP       │ ← Partner heading to customer
                        └────────┬─────────┘
                                 │ Partner arrives at customer
                        ┌────────▼─────────┐
                        │   DELIVERED       │ ← Order complete
                        └──────────────────┘
                        
Parallel cancellation path:
  PLACED → CANCELLED (restaurant rejects, customer cancels within 1 min)
  ACCEPTED → CANCELLED (restaurant can't fulfill, partner unavailable)
  Each transition fires a Kafka event → notifications + partner app + customer app update
```

---

## Step 6: Delivery Partner Dispatch

```
Problem: New order arrives → find best available delivery partner nearby

Similar to Uber ride dispatch (Chapter 15) but with nuances:
  - Partner must go to restaurant FIRST (not direct to customer)
  - Consider restaurant prep time (no point sending partner if food takes 20 min)
  - Partner may have an existing order to deliver (stacking)

Algorithm:
  Step 1: Find available partners within 3km of restaurant
    Redis GEORADIUS partners:available restaurantLng restaurantLat 3 km ASC
    
  Step 2: Score each candidate partner
    score = w1 × eta_to_restaurant +    ← time to reach restaurant
            w2 × (partner_rating) +     ← quality
            w3 × acceptance_rate +      ← reliability
            w4 × stacking_penalty       ← penalty if already has order

  Step 3: Account for restaurant prep time
    If food ready in 15 min → don't send partner immediately
    Schedule dispatch: ZADD dispatch_queue (now + 10min) orderId
    Background worker polls queue, dispatches when time is right
    
  Step 4: Offer to top partner (15s to accept)
    If declined → next best partner
    Redis lock: SETNX partner:{id}:lock orderId EX 20
    
  Step 5: Partner accepts → update order status → notify customer ETA
```

---

## Step 7: Real-Time Order Tracking

```
Customer wants live map showing delivery partner's location

Architecture:
  Delivery partner app → Location Service every 5s:
    PUT /v1/partners/location { lat, lng, heading, speed }
  
  Location Service:
    Update Redis: GEOADD partners:active lng lat partnerId
    If partner has active order:
      Publish to Kafka: location.partner.{orderId}
  
  Customer tracking:
    GET /v1/orders/{id}/track (SSE or WebSocket)
    Customer's browser subscribes to partner's location stream
    
  Location fan-out:
    Kafka consumer per active order subscription
    Push location update to customer's WebSocket/SSE connection

  ⚡ Multi-instance SSE problem:
    Customer's SSE connection lives on Gateway Instance 1.
    Kafka consumer runs on Worker Instance 2.
    Worker 2 can't push to customer directly — no socket.
    
    Solution: Redis Pub/Sub
      Worker 2: PUBLISH channel:order:{orderId}:location {lat, lng, eta}
      All Gateway instances subscribed to this channel
      Instance 1 receives → has customer's SSE socket → pushes ✅
    
    (See 12-Communication-Patterns.md → Section 5 for full patterns)
    
  ETA recalculation:
    Every location update → recalculate ETA (route + traffic)
    Push updated ETA to customer
    Google Maps Distance Matrix API for route calculation

Optimisation:
  Don't push every 5s update to customer — smooth movement every 2s
  Dead reckoning: interpolate position between GPS updates on client
  Only push to customer if partner moved > 50m (reduces unnecessary updates)
```

---

## Step 8: Restaurant Search & Menu

```
Search flow:
  Customer location → find nearby open restaurants (within 10km)
  Filter: cuisine, rating, price range, delivery time, offers
  Sort: relevance (distance × rating × popularity) or ETA or price

Geo-search: Redis GEORADIUS (same pattern as Yelp chapter)
  Separate index per cuisine type

Menu management:
  Stored in MySQL (structured, ACID for price updates)
  Cached in Redis per restaurant (TTL 5min)
  
  Menu item availability:
    SETEX menu:{restaurantId}:item:{itemId}:available 300 {true|false}
    Restaurant pushes availability updates in real-time
    
Dynamic ETA:
  Delivery time = preparation time + pickup time + delivery time
  Preparation time: restaurant's historical avg per cuisine/time-of-day
  Pickup time: ETA of nearest available partner to restaurant
  Delivery time: Google Maps ETA from restaurant to customer
  Buffer: add 5-10 min for reliability (under-promise, over-deliver)

"Busy" restaurant:
  If order acceptance rate drops or prep times increase → flag as busy
  Show customer "Currently busy - ETA may be longer"
  Reduce restaurant weighting in search results
```

---

## Step 9: Payments & Reconciliation

```
Payment flow:
  Customer pays at order placement (prepaid) via Stripe/Razorpay
  or Cash on Delivery (COD)
  
  Order placed → payment authorized (hold)
  Order delivered → payment captured
  Order cancelled → payment released
  
  Payout flow:
    Platform holds delivery fee
    Restaurant payout: daily batch (order total - commission - taxes)
    Partner payout: daily or weekly (delivery fee - commission)

Reconciliation:
  End of day: match orders in order DB with payment processor records
  Any mismatches → alert finance team
  Failed payouts: retry pipeline with exponential backoff
  
Refund policy:
  Wrong item / quality issue → auto-refund (no delivery partner fault)
  Late delivery → partial credit to wallet
  Cancelled after pickup → full charge (partner + restaurant compensated)
```

---

## Step 10: Surge Pricing & Partner Incentives

```
Surge pricing (similar to Uber):
  Demand/supply ratio per 1km² cell → surge multiplier
  High demand + low supply → delivery fee increases → attracts more partners
  
  Customer sees: "Surge pricing in effect - delivery fee ₹80 (normally ₹40)"
  Legal requirement in some regions: cap surge at 2x
  
Partner incentives:
  Bonus for completing N orders in rush hour
  Streak bonus: 5 consecutive orders completed on time = ₹100 bonus
  Incentives published in advance → partners plan their availability
  
Restaurant commission:
  Standard: 25-30% of order value
  Higher visibility (promoted placement): higher commission
  Exclusive restaurants: lower commission for partnership lock-in
```

---

## Interview Q&A

**Q: How do you handle the case where a restaurant takes too long to accept an order?**
A: Auto-cancel after 3 minutes if restaurant doesn't accept. Try to reassign to another nearby restaurant offering the same cuisine (if available). Notify customer of alternative. If no alternative, cancel and refund. Track restaurant's acceptance rate — restaurants with < 80% acceptance rate flagged, coached, eventually delisted.

**Q: How does "order batching" work (partner picks up from multiple restaurants)?**
A: Batching assignment: algorithm checks if partner can pick up a second order from a restaurant within 0.5km before delivering the first. Maximum 2 orders batched to prevent very late deliveries. Customer is notified if their order is batched and shown updated ETA. Partner sees both pickups on their app with optimal routing.

**Q: How do you ensure the delivery partner actually delivered the order?**
A: Geofencing: mark order as delivered only when partner's GPS is within 100m of customer's address. OTP-based delivery (high-value orders): customer shows OTP to partner — delivery only completed when OTP entered. Photo proof: partner takes photo of delivered order at door. Fallback: customer can mark "not received" → triggers investigation.

**Q: How would you scale to handle peak dinner traffic (5-10x normal)?**
A: Pre-scale before peak: predictive auto-scaling (Kubernetes HPA + scheduled scaling). Redis cluster handles location spike (100K updates/sec). Order DB: read replicas + connection pooling. Notification service: pre-warmed Kafka consumer instances. CDN for static assets (restaurant images, menus). Circuit breakers: if Google Maps ETA API is slow, use estimated ETA from internal model.

**Q: How do you handle "ghost kitchens" (restaurant only exists for delivery)?**
A: Same architecture as regular restaurants — ghost kitchens are just restaurants without a dine-in component. They often run multiple virtual brands from one kitchen. System: one kitchen address, multiple "restaurant" entities (Brand A: burgers, Brand B: pizza) — each with own menu, ratings. Location service deduplicates — multiple virtual restaurants at same physical address is fine.
