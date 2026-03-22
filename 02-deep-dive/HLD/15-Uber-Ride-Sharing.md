# 📚 System Design — Ride Sharing Platform (Uber / Lyft)

---

## 🎯 Problem Statement
Design a ride-sharing platform where riders can request rides, nearby drivers are matched and dispatched, real-time location is tracked, and pricing is computed dynamically.

---

## Step 1: Clarify Requirements

### Functional
- Rider requests a ride (pickup + destination)
- System finds nearby available drivers
- Match rider to best driver (proximity, rating, ETA)
- Real-time location tracking (driver and rider)
- Dynamic pricing (surge pricing)
- Trip tracking (start, end, route)
- Payment processing
- Ratings (post-trip)
- Driver/rider profiles

### Non-Functional
- **Scale**: 100M riders, 5M drivers, 20M trips/day
- **Latency**: Driver matching < 5 seconds
- **Location update frequency**: Every 4 seconds from active drivers
- **Availability**: 99.99% for ride requests
- **Consistency**: Strong for payments; eventual for location

---

## Step 2: Estimation

```
Active drivers: 5M total, ~500K active at peak
Location updates: 500K drivers × 1 update/4s = 125K location updates/sec
Ride requests: 20M trips/day = 231 TPS average, ~700 TPS peak
Matching latency: must find drivers within 5 seconds of request

Location data per update: 40 bytes (driverId, lat, lng, timestamp, status)
Location storage/day: 125K × 40 bytes × 86400 = ~432 GB/day
(Only need last known position + last 30 min for active trips)
```

---

## Step 3: API Design

```
# Rider APIs
POST   /api/v1/trips/estimate              → price estimate for route
POST   /api/v1/trips                       → request a ride
GET    /api/v1/trips/{id}                 → trip status + driver ETA
DELETE /api/v1/trips/{id}                 → cancel trip
POST   /api/v1/trips/{id}/rating          → rate driver post-trip

# Driver APIs
PUT    /api/v1/drivers/location           → update driver location + status
GET    /api/v1/drivers/assignments        → get assigned trip
PUT    /api/v1/trips/{id}/status          → update trip status (accepted/arrived/started/ended)

# WebSocket (real-time)
WS /ws/trips/{id}/location               → stream driver location to rider
WS /ws/drivers/{id}/dispatch             → push trip assignment to driver
```

---

## Step 4: High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Clients                           │
│              Rider App | Driver App                 │
└─────────────┬─────────────────────────┬────────────┘
              │ REST + WebSocket         │ Location updates
              │                          │ (every 4 sec)
       ┌──────▼────────┐        ┌───────▼──────────┐
       │  API Gateway   │        │ Location Service  │
       │  (auth, rate   │        │                   │
       │   limiting)    │        │ Redis Geo + Cache │
       └──┬──┬──┬──┬───┘        └───────┬───────────┘
          │  │  │  │                    │
   ┌──────┘  │  │  └──────────┐        │ Kafka: location.updates
   │         │  │             │         ▼
┌──▼──┐ ┌───▼──▼──┐ ┌────────▼──┐  ┌──────────────┐
│Trip │ │Matching │ │ Payment   │  │ Dispatch Svc │
│Svc  │ │Engine   │ │ Service   │  │              │
└──┬──┘ └────┬────┘ └───────────┘  └──────────────┘
   │         │
   │    ┌────▼──────┐
   │    │ Driver    │
   │    │ Location  │
   │    │ (Redis    │
   │    │  GEOADD)  │
   │    └───────────┘
   │
┌──▼───────────────────┐
│ Trip DB (PostgreSQL)  │
│ Driver DB (MySQL)     │
│ Surge Price (Redis)   │
└──────────────────────┘
```

---

## Step 5: Location Service — Core Component

### Real-time Location Storage
```
500K active drivers update location every 4 seconds.
Need: Given a rider's location, find nearest available drivers in O(log n).

Solution: Redis GEOADD (Geospatial index)

// Driver updates location
GEOADD drivers:active lng lat driverId
// "drivers:active" is a sorted set where score = Geohash of location

// Find drivers within 5km of rider
GEORADIUS drivers:active riderLng riderLat 5 km ASC COUNT 10
// Returns: [driverId1, driverId2, ...] sorted by distance

Properties:
  Redis GEORADIUS: O(N+log(M)) where N=results, M=total drivers
  Typical response: < 2ms for 500K drivers
  Precision: 0.6m error at Geohash level 8 (sufficient for dispatch)
```

### Location Update Flow
```
Driver app → Location Service (every 4 sec):
  1. Update Redis GEOADD (immediate, for matching queries)
  2. Publish to Kafka: location.updates (for persistence + analytics)
  3. If driver has active trip:
     a. Push location to rider via WebSocket (real-time tracking)
     b. Update trip route in DB (for post-trip replay)

Inactive drivers: don't update (save battery, don't pollute location index)
Status field: AVAILABLE, ON_TRIP, OFFLINE
  GEOADD drivers:available vs drivers:on_trip (separate sets)
```

---

## Step 6: Matching Engine

### Driver Selection Algorithm
```
Input: Rider location (lat, lng), pickup address, vehicle type
Output: Best matched driver

Step 1: Query nearby available drivers
  GEORADIUS drivers:available riderLng riderLat 5km ASC COUNT 50

Step 2: Score each candidate driver
  score = w1 × ETA_score + w2 × acceptance_rate + w3 × rating + w4 × trips_completed
  ETA_score: 1 - (distance / max_distance) — closer is better
  acceptance_rate: don't repeatedly offer to drivers who decline
  rating: prefer higher-rated drivers

Step 3: Offer to top driver
  Push assignment to driver via WebSocket/push notification
  Driver has 15 seconds to accept

Step 4: If driver declines or doesn't respond:
  Remove from candidate list
  Offer to next best driver

Step 5: Lock state during matching (prevent double-booking)
  Redis SETNX driver:{driverId}:lock 1 EX 20
  If lock acquired → driver offered to this rider only
  Release lock if driver declines / times out
```

### Geohash Partitioning (City-Level Dispatch)
```
Each city = one or more Geohash prefixes
Dispatch servers partitioned by city (matching is local, not global)
  NYC drivers: dispatch-server-nyc
  London drivers: dispatch-server-london

Benefits:
  No cross-city matching queries
  Lower latency (local Redis per city cluster)
  Fault isolation (NYC outage doesn't affect London)
```

---

## Step 7: Surge Pricing

```
Real-time supply/demand imbalance → surge multiplier

Algorithm:
  Every 1 minute, compute per-Geohash cell:
    demand = pending_ride_requests (last 5 min)
    supply = available_drivers
    ratio = demand / supply
    
    if ratio < 0.5: multiplier = 1.0x (no surge)
    if ratio < 1.0: multiplier = 1.2x
    if ratio < 2.0: multiplier = 1.5x
    if ratio >= 2.0: multiplier = min(ratio, 4.0)x

Storage: Redis Hash
  HSET surge:geohash:{geohash8} multiplier 1.5 updated_at 1700000000
  
User sees estimated price × surge multiplier before requesting

Anti-abuse: Cap surge multiplier (legal requirements in some regions)
Transparency: Show rider the multiplier before they confirm
```

---

## Step 8: Trip Lifecycle

```
State Machine:
REQUESTED → MATCHED → DRIVER_ARRIVING → IN_PROGRESS → COMPLETED
                                                      └── CANCELLED

Transitions:
  REQUESTED → MATCHED:         Driver accepts
  MATCHED → DRIVER_ARRIVING:   Driver starts navigation to pickup
  DRIVER_ARRIVING → IN_PROGRESS: Driver arrives, rider boards, trip starts
  IN_PROGRESS → COMPLETED:     Driver ends trip at destination

Database:
  trips (
    id          UUID PRIMARY KEY,
    rider_id    BIGINT,
    driver_id   BIGINT,
    status      ENUM,
    pickup_lat  DECIMAL, pickup_lng DECIMAL,
    dest_lat    DECIMAL, dest_lng   DECIMAL,
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    distance_km DECIMAL,
    fare        DECIMAL,
    surge_mult  DECIMAL,
    route       JSONB     -- GPS path recorded during trip
  )
```

---

## Step 9: Payment Processing

```
Fare calculation:
  base_fare + per_minute_rate × duration + per_km_rate × distance + surge_multiplier
  
  Apply promotions/discounts
  
Payment flow:
  1. Trip ends → compute fare
  2. Charge rider's saved payment method (Stripe/Braintree)
  3. If charge succeeds → transfer to driver (next day batch)
  4. Idempotency key = tripId (prevent double charge on retry)

Driver payout:
  Batch processing: daily payout to driver bank accounts
  Deduct platform fee (25-30% of fare)
  
Fraud detection:
  GPS route validation (did driver actually take reasonable path?)
  Abnormal fare detection (trip too long, price too high)
```

---

## Step 10: ETA Calculation

```
Naive: straight-line distance / avg speed
Better: road network graph + Dijkstra

Production approach:
  Historical data: average speed per road segment by time-of-day + day-of-week
  Real-time: current congestion from GPS traces of all drivers
  Graph: road network modeled as weighted directed graph
  
  ETA = A* search on road graph with historical + real-time weights
  
  Challenges:
    Dynamic weights (traffic changes every few minutes)
    Road network updates (construction, closures)
    Multiple ETAs: pickup ETA + trip ETA to destination
    
Google/Apple Maps do this at global scale — Uber licenses map data
and builds routing on top using their real-time driver telemetry.
```

---

## Interview Q&A

**Q: How do you prevent two riders from being assigned to the same driver simultaneously?**
A: Optimistic locking via Redis: `SETNX driver:{id}:lock 1 EX 20`. Only one matcher can acquire the lock. After match confirmed, remove driver from available pool. The 20-second TTL releases the lock if the driver doesn't respond. This is a distributed lock pattern.

**Q: How would you design the location tracking to reduce battery drain on driver phones?**
A: Adaptive frequency: when driver is stationary, update every 30s. When moving slowly, every 8s. When moving fast, every 4s. Batched updates: collect 5-10 updates, send as one HTTP request. Background location (OS API differs between iOS/Android). Heading-based updates: only send if location changed significantly.

**Q: How does surge pricing prevent all drivers from rushing to the same surging area?**
A: Surge is calculated per-Geohash cell, not city-wide. When drivers move into a cell, supply increases → surge decreases → less incentive to move further. Natural equilibrium. Some companies use "heat maps" to show areas of high demand proactively to drivers.

**Q: How would you scale the matching engine to handle a city with 100K active drivers?**
A: Redis GEORADIUS on 100K drivers is still fast (< 2ms). However, for truly massive cities: partition by sub-region (Geohash prefix), each sub-region has its own matching server and Redis. Cross-region matching only if local supply is insufficient. Use consistent hashing for server assignment by Geohash.

**Q: How do you handle the case where a driver's GPS is inaccurate (GPS drift)?**
A: Map-snapping: snap GPS coordinates to nearest road using road network. Kalman filter: smooth location over time (reduce jitter). Confidence score: ignore updates with very high location accuracy error (from the GPS signal). Detect anomalies: flag teleportation events (location jumps > 1km in 4 seconds).
