# 📚 System Design — Proximity Service (Yelp / Google Places / Nearby Friends)

---

## 🎯 Problem Statement
Design a proximity service that allows users to find nearby points of interest (restaurants, hotels, stores) within a given radius, with fast geo-based search, reviews, ratings, and business information.

---

## Step 1: Clarify Requirements

### Functional
- Search businesses within a given radius (e.g., 5km) from user's location
- Filter by category (restaurants, hotels, gas stations)
- Sort by: distance, rating, review count
- View business details (name, address, hours, photos, phone)
- Add/view reviews and ratings
- Business owner: add/update business listing
- Real-time availability (is the business open right now?)

### Non-Functional
- **Scale**: 100M DAU, 200M businesses globally
- **Read:Write**: 99:1 (mostly searches and views, rarely writes)
- **Latency**: Search results < 100ms
- **Availability**: 99.99%
- **Consistency**: Eventual OK (new business may take minutes to appear)

---

## Step 2: Estimation

```
Businesses:    200M worldwide
Searches:      100M DAU × 5 searches/day = 500M/day = 5,787 QPS avg
               Peak = ~20K QPS

Business data: avg 1KB per business (name, address, category, rating)
               200M × 1KB = 200GB (fits in memory with Redis!)

Location data: lat/lng per business = 16 bytes
               200M × 16 bytes = 3.2GB (tiny — easily cached)

Reviews:       200M businesses × 10 reviews avg = 2B reviews × 500B = 1TB
```

---

## Step 3: API Design

```
# Search
GET /v1/businesses/search
  ?lat=12.9716&lng=77.5946
  &radius=5000           (meters, max 20km)
  &category=restaurant
  &sort=distance|rating
  &page=1&limit=20
Response: { businesses: [{id, name, address, distance_m, rating, is_open}], total }

# Business details
GET  /v1/businesses/{id}
Response: { id, name, address, lat, lng, phone, hours, photos, rating, review_count }

# Reviews
GET  /v1/businesses/{id}/reviews?page=1&limit=10
POST /v1/businesses/{id}/reviews
Body: { rating: 4, text: "Great place!" }

# Business management (owner API)
POST /v1/businesses               → add business
PUT  /v1/businesses/{id}          → update details
PUT  /v1/businesses/{id}/hours    → update operating hours
```

---

## Step 4: High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Clients                           │
│            Mobile App | Web Browser                 │
└──────────────────────┬──────────────────────────────┘
                       │
               ┌───────▼────────┐
               │  API Gateway   │
               │  (auth, rate   │
               │   limiting)    │
               └──┬──────────┬──┘
                  │          │
        ┌─────────▼──┐  ┌────▼──────────────┐
        │  Location  │  │  Business Info    │
        │  Service   │  │  Service          │
        │            │  │                   │
        │  Redis Geo │  │  MySQL + Replica  │
        │  PostGIS   │  │  Redis Cache      │
        └────────────┘  └───────────────────┘
                  │
        ┌─────────▼──────────────┐
        │   Review Service       │
        │   Cassandra (write-    │
        │   heavy, time-series)  │
        └────────────────────────┘
```

---

## Step 5: Geo-Indexing — The Core Problem

Finding "all businesses within 5km" efficiently is the central challenge.

### Option 1: Naive SQL (Don't Use)
```sql
SELECT * FROM businesses
WHERE (lat - 12.9716)^2 + (lng - 77.5946)^2 < (5/111.0)^2
-- Full table scan: O(N) = 200M rows → way too slow
```

### Option 2: Geohash

```
Geohash encodes (lat, lng) into a string where prefix similarity = geographic proximity

World → divide into 32 cells → pick one → divide into 32 → pick → ...

Precision levels:
  Length 1: ~5,000km × 5,000km (entire continent)
  Length 4: ~40km × 20km
  Length 6: ~1.2km × 0.6km (city block level)
  Length 9: ~5m × 5m (building level)

Example: Bengaluru, India = "tdnu3b..."

Search within 5km:
  1. Compute geohash of user location at precision level 5 (radius ~2.5km)
  2. Find 8 neighboring geohash cells (user's cell + 8 adjacent)
  3. Query: SELECT * FROM businesses WHERE geohash LIKE 'tdnu3%'
             OR geohash LIKE 'tdnu2%' (neighbor)...
  4. Filter exact distance with Haversine formula

Index: B-Tree index on geohash prefix → O(log N) per cell query

Edge cases:
  Cells near poles: Mercator projection makes them non-square
  Cells near antimeridian (180° longitude): neighbor computation wraps around
  User on cell boundary: their cell has few businesses → need larger radius
```

### Option 3: Redis GEOADD (Recommended for Most Cases)
```
Redis geospatial commands use Geohash internally:

GEOADD businesses lng lat businessId
  GEOADD businesses 77.5946 12.9716 "restaurant:123"
  GEOADD businesses 77.5999 12.9800 "restaurant:456"

GEORADIUS businesses 77.5946 12.9716 5 km ASC COUNT 20
→ Returns businessIds within 5km, sorted by distance

GEORADIUSBYMEMBER businesses myLocation 5 km

Capacity: 200M businesses → single Redis sorted set (score = Geohash integer)
Memory: 200M × ~50 bytes = 10GB → fine for single large Redis instance

Speed: O(N+log(M)) where N=results, M=total — typically < 2ms

Limitation: can't filter by category natively
Fix: Separate geo index per category:
  GEOADD businesses:restaurant lng lat id
  GEOADD businesses:hotel lng lat id
  GEOADD businesses:gas_station lng lat id
  → 10 category indexes × 200M / avg categories = manageable
```

### Option 4: PostGIS (PostgreSQL Extension)
```sql
-- Business table with geography column
CREATE TABLE businesses (
    id      BIGINT PRIMARY KEY,
    name    VARCHAR(255),
    location GEOGRAPHY(POINT, 4326)  -- WGS84 coordinates
);

-- Spatial index (R-Tree / GiST)
CREATE INDEX idx_location ON businesses USING GIST(location);

-- Search within 5km
SELECT id, name,
  ST_Distance(location, ST_MakePoint(77.5946, 12.9716)::geography) AS dist_m
FROM businesses
WHERE ST_DWithin(location, ST_MakePoint(77.5946, 12.9716)::geography, 5000)
ORDER BY dist_m
LIMIT 20;

-- Uses GiST index → O(log N + K) where K = results
```

### ✅ Recommended Architecture
```
Hot path (fast geo search): Redis GEOADD
  Stores only businessId + coordinates
  Returns matching IDs in < 2ms
  
Business details: MySQL (fetched by IDs after geo query)
  Cache with Redis: 100M DAU × top-100 businesses → hot businesses always cached

Two-step process:
  Step 1: Redis GEORADIUS → [id1, id2, id3, ..., id20]
  Step 2: MySQL/Redis: SELECT * FROM businesses WHERE id IN (id1, id2, ...) → details
```

---

## Step 6: Database Schema

```sql
CREATE TABLE businesses (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    city        VARCHAR(100),
    country     CHAR(2),
    lat         DECIMAL(10,8),
    lng         DECIMAL(11,8),
    phone       VARCHAR(30),
    website     VARCHAR(255),
    category_id INT,
    rating      DECIMAL(2,1),       -- cached avg, updated periodically
    review_count INT DEFAULT 0,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE business_hours (
    business_id BIGINT,
    day_of_week TINYINT,            -- 0=Sun, 1=Mon, ..., 6=Sat
    open_time   TIME,
    close_time  TIME,
    is_closed   BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (business_id, day_of_week)
);

CREATE TABLE reviews (
    id          BIGINT PRIMARY KEY,
    business_id BIGINT,
    user_id     BIGINT,
    rating      TINYINT,            -- 1-5
    text        TEXT,
    created_at  TIMESTAMP,
    helpful_count INT DEFAULT 0
);

CREATE INDEX idx_business_id ON reviews(business_id, created_at DESC);
```

---

## Step 7: Search Result Ranking

```
Distance alone isn't always the best sort — combine multiple signals:

relevance_score = 
    (1 - distance/max_radius) × 0.3    +  distance component
    rating/5 × 0.3                      +  quality component  
    log(review_count)/log(max_reviews) × 0.2 +  popularity
    is_open_now ? 0.1 : 0              +  availability
    paid_ranking_boost × 0.1              advertisement (clearly labeled)

Personalization (for logged-in users):
  Boost categories matching user's past searches/visits
  Boost price range matching user's history
  Penalize places user has already reviewed

A/B test different weights to optimize click-through rate and return visits
```

---

## Step 8: Handling "Is Open Now?"

```
Simple but important — users want to know if a place is currently open

Business hours in DB: per day of week + open/close time

Query time logic:
  now = current time in business's local timezone (use timezone stored in DB)
  day = day_of_week(now)
  time = time_of_day(now)
  
  SELECT * FROM business_hours
  WHERE business_id = ?
    AND day_of_week = ?
    AND open_time <= ? AND close_time > ?
    AND is_closed = FALSE

  Handle special cases:
    Crosses midnight: open_time=22:00, close_time=02:00
      → Split into two records or handle with day-spanning logic
    Holidays: separate holiday_hours table overrides regular schedule
    Timezone: store business's IANA timezone (e.g., "Asia/Kolkata")
      → Convert to local time before comparison

Cache is_open status:
  Redis: SETEX open:{businessId} 60 {true|false}
  Recompute every minute (not per request)
  Edge case: business closing in 5 minutes → serve cached "open" for max 60s
```

---

## Step 9: Scaling Deep Dives

```
Geo index sharding:
  Single Redis instance: 200M businesses × 50 bytes = 10GB → fits fine
  If needed: shard by region (US Redis, EU Redis, APAC Redis)
  Route by user's approximate location
  No cross-region queries needed (user won't search 5km radius across continents)

Business data read replicas:
  99:1 read:write ratio → many read replicas
  MySQL + 5 read replicas (geographic distribution)
  Redis cache hit rate > 95% for hot businesses

Review system:
  Write-heavy for popular businesses
  Cassandra: partition by business_id + cluster by created_at DESC
  Fast latest reviews query, horizontal scale for viral review spikes

CDN for photos:
  Business photos → S3 → CloudFront CDN
  Thumbnail generation: Lambda on S3 upload event
  Multiple sizes: 100×100 (list thumbnail), 400×300 (detail), original
```

---

## Interview Q&A

**Q: Why use Geohash instead of just a SQL range query on lat/lng?**
A: SQL range query `WHERE lat BETWEEN x1 AND x2 AND lng BETWEEN y1 AND y2` only makes a rectangle — needs post-filter for circle. More importantly, it requires two separate B-Tree indexes and DB does an intersection, which is much slower than a single spatial index. Geohash + prefix query uses ONE B-Tree index on the hash string and covers the spatial area efficiently.

**Q: How does Geohash handle the boundary problem (user on the edge of a cell)?**
A: Query the user's own Geohash cell PLUS all 8 neighbors (3×3 grid). This ensures all businesses within radius are found regardless of which cell the user falls in. The extra cells may return businesses slightly beyond the radius — filter these with exact Haversine distance calculation after the geo query.

**Q: How would you add real-time availability (e.g., table availability for a restaurant)?**
A: Separate real-time availability service (not part of geo search). Restaurant integrates via API to push available slot data. Cache in Redis with short TTL (30-60 seconds). Show "tables available" badge in search results — refresh on demand. For booking: reservation system with distributed locking (Redis SETNX) to prevent double booking same slot.

**Q: How do you handle businesses that operate in multiple locations (like McDonald's)?**
A: Each physical location is a separate business record with its own lat/lng. They share a brand/chain_id for filtering ("show me only McDonald's nearby"). One parent brand record → many location records. Search works on individual locations; chain filtering is an additional query predicate.

**Q: How would you design the search for "nearby friends" (like Snapchat/Facebook)?**
A: Similar architecture but with privacy controls. Users opt-in to location sharing. Store recent location (last 30min) in Redis GEOADD with user-friend privacy filter. On query: fetch user's friend list → intersect with geo results. Unlike businesses: user locations change constantly → TTL on Redis keys (expire after 30min of inactivity). Granularity: show approximate location ("2km away"), not exact coordinates.
