# 📚 System Design — Maps & Navigation (Google Maps)

---

## 🎯 Problem Statement
Design a maps and navigation system that supports location search, turn-by-turn navigation, real-time traffic, ETA calculation, and map tile rendering for billions of users globally.

---

## Step 1: Clarify Requirements

### Functional
- Search for places (restaurants, addresses, businesses)
- Display map tiles (street maps, satellite imagery)
- Turn-by-turn navigation (routing from A to B)
- Real-time traffic (live congestion, incidents)
- ETA calculation (time accounting for traffic)
- Nearby places (points of interest around location)
- Street View imagery
- Offline maps (download for offline use)
- Transit routes (bus, metro, walking combinations)

### Non-Functional
- **Scale**: 1B+ users, 25M miles of roads mapped
- **Latency**: Map tiles < 100ms; route calculation < 2s for cross-country
- **Availability**: 99.999% — navigation during driving
- **Freshness**: Traffic updates every 1-2 minutes; road data within days of change
- **Storage**: Planet-scale geo data (maps = ~50TB compressed)

---

## Step 2: Estimation

```
Users:          1B users, 100M DAU navigating
Map tile requests: 100M DAU × 100 tiles/session = 10B tiles/day = 115K tiles/sec
Route requests: 100M DAU × 2 routes/day = 200M/day = 2,315 QPS
Traffic events: 500K vehicles reporting location/sec (Google probe data)

Map tile storage:
  World at zoom level 14: ~40M tiles × 50KB = ~2TB
  All zoom levels (0-20): ~50TB compressed
  Satellite imagery: much larger (~10PB)

Road network:
  25M miles of roads
  Road segment: ~500 bytes
  25M segments = ~12.5GB (tiny — fits in RAM of one server!)
```

---

## Step 3: Core Components

```
┌──────────────────────────────────────────────────────────┐
│                      Clients                              │
│         Mobile App | Web Browser | Car GPS               │
└──────────────────────┬───────────────────────────────────┘
                       │
               ┌───────▼────────┐
               │   API Gateway  │
               └──┬──┬──┬──┬───┘
                  │  │  │  │
    ┌─────────────┘  │  │  └─────────────────┐
    │                │  │                     │
┌───▼──────┐  ┌──────▼──┐  ┌────────────┐  ┌─▼──────────┐
│  Search  │  │ Routing  │  │ Tile       │  │  Traffic   │
│  Service │  │ Service  │  │ Service    │  │  Service   │
└───┬──────┘  └──────┬───┘  └─────┬──────┘  └─────┬──────┘
    │                │            │                │
    ▼                ▼            ▼                ▼
Elasticsearch   Graph DB      Tile Store       Traffic DB
(places index)  (road network) (S3 + CDN)    (time-series)
```

---

## Step 4: Map Tiles — Serving the Map

The map you see is composed of small image tiles (256×256 or 512×512 pixels).

### Tile Coordinate System
```
Map is divided into a grid at each zoom level:

Zoom 0: 1 tile covers the entire world
Zoom 1: 4 tiles (2×2 grid)
Zoom 10: 1,048,576 tiles (1024×1024 grid) — city level
Zoom 14: 268M tiles — street level (most detail for navigation)
Zoom 18: ~70B tiles — building level (satellite)

Tile URL: /tiles/{zoom}/{x}/{y}.png
  x, y = tile column and row at this zoom level

Tile request from client:
  Client calculates which tiles are needed for current viewport + zoom
  Requests each tile independently (cacheable by URL)
```

### Tile Generation Pipeline
```
Raw geographic data (OpenStreetMap, proprietary surveys, satellite imagery)
    │
    ▼
Tile Rendering Pipeline:
  1. Load geographic data (road shapes, building footprints, terrain, labels)
  2. Apply map style (colors, fonts, icon styles — Mapbox GL style spec)
  3. Render tile as PNG/WebP or vector tile (MVT format)
  4. Store in tile cache (S3)

Two types of tiles:
  Raster tiles (PNG): pre-rendered images — simple to serve, large files
  Vector tiles (MVT): raw geographic data — rendered on client, dynamic, smaller

Google Maps shifted to vector tiles:
  50KB raster tile → 5-10KB vector tile (5-10x smaller)
  Client renders with WebGL (smooth zoom, rotation, 3D buildings)
  Can apply any style without re-requesting tiles
  
Tile serving:
  S3 origin → CloudFront CDN → Client
  CDN cache hit rate: > 99% (most tiles are static)
  Only changing tiles (construction zones, traffic overlay) need frequent refresh
```

### Tile Caching Strategy
```
Static tiles (street map): TTL = 7 days (rarely change)
Traffic overlay tiles:     TTL = 2 minutes (real-time traffic)
Satellite imagery:         TTL = 30 days (expensive to update)

Cache hierarchy:
  Client-side: browser/app caches tiles in IndexedDB (offline use)
  CDN edge:    CloudFront at 300+ PoPs worldwide
  S3 origin:   source of truth

Cache key: /v3/tiles/{style}/{zoom}/{x}/{y}@2x.mvt
  Style versioning allows instant rollout of map style changes
```

---

## Step 5: Routing — Finding the Best Path

### Road Network as a Graph
```
Nodes: intersections (lat/lng coordinates)
Edges: road segments (distance, speed limit, direction, road type)

Edge weight = travel time = distance / speed
  But speed changes with traffic → dynamic weights

Road network size:
  25M road segments, each 500 bytes = 12.5GB
  Graph with bidirectional edges: ~50M edges
  This fits in RAM of a single high-memory server (r5.8xlarge = 256GB)
  
  For global routing: partition by region
    North America graph: ~5M segments
    Europe graph: ~5M segments
    Each region fits on dedicated routing server
```

### Dijkstra's Algorithm (Naive)
```
Standard Dijkstra on 50M edge graph:
  Worst case: O((V + E) log V) = O(50M × log 50M) ≈ 1.3B operations
  Single query: ~5-10 seconds ❌ (too slow)
```

### A* Search (Better)
```
A* uses a heuristic to guide search toward destination:
  f(n) = g(n) + h(n)
    g(n) = actual cost from source to n (like Dijkstra)
    h(n) = estimated cost from n to destination (Euclidean distance / max_speed)
  
  Since h(n) is admissible (never overestimates), A* finds optimal path
  
  For road networks: A* is 2-5x faster than Dijkstra
  Still too slow for cross-country routes
```

### Bidirectional A* (Production Approach)
```
Run A* simultaneously from source AND destination
Stop when both searches meet in the middle

Speedup: reduces search radius by sqrt(2)
For cross-continent route: 10s → 1-2s
```

### Contraction Hierarchies (Used by Google/Osrm)
```
Pre-processing step (done offline, takes hours):
  1. "Contract" unimportant nodes by adding shortcut edges
     If B is on the only path A→C, add shortcut edge A→C
     Remove B from the hierarchy level
  2. Build a hierarchy: local roads → arterial → highway → interstate
  3. Shortcuts encode the contracted paths
  
  Result: 100x smaller search space for long-distance routing
  
Query (milliseconds even for cross-country):
  1. Find highest-level hierarchy node near source (interstate junction)
  2. Find highest-level hierarchy node near destination
  3. Connect via highest-level shortcuts (few edges)
  4. Expand shortcuts back to actual road segments

Why it works:
  Cross-country trip: don't consider every neighborhood road
  Only care about: source neighborhood → interstate → destination neighborhood
  Hierarchy makes this explicit in the graph structure

Used by: OpenStreetMap's OSRM, Google Maps (proprietary variant), HERE Maps
Query time: < 10ms for any route in North America after preprocessing
```

---

## Step 6: Real-Time Traffic

```
Data sources:
  1. Probe data: GPS signals from phones running Google Maps
     - 500K location updates/sec globally
     - Every phone is a traffic sensor
  2. Road sensors: government traffic cameras, loop detectors
  3. Incident reports: user reports, traffic authority feeds
  4. Historical patterns: day-of-week + time-of-day baseline speeds

Traffic processing pipeline:
  Phone GPS events → Kafka
    → Flink: map-match GPS coords to road segments
    → Compute: actual speed per road segment (last 5 min)
    → Compare: actual vs speed limit / historical
    → Classify: free flow, slow, congested, standstill
    → Store: Redis (current traffic, TTL 5 min) + Cassandra (historical)

Dynamic edge weights:
  Edge weight = f(distance, road type, current_speed, traffic_multiplier)
  Routing engine re-reads current traffic from Redis before each calculation
  Traffic update → triggers re-calculation of affected routes

Traffic overlay on map:
  Green = free flow (actual speed > 80% of speed limit)
  Yellow = moderate (50-80% of speed limit)  
  Red = heavy (20-50% of speed limit)
  Dark red = severe (< 20% of speed limit)
  
  Implemented as a traffic tile overlay layer (separate from base map)
  TTL: 2 minutes on CDN
```

---

## Step 7: ETA Calculation

```
Naive ETA: sum of (edge_distance / edge_speed) for all route edges
  → Changes as traffic changes while driving

Production ETA:
  Not just current traffic — predicts future traffic along route
  
  If route takes 45 min, don't use current traffic at destination
  Use: predicted traffic at destination 45 min from now
  
  Historical model:
    speed[road_segment][day_of_week][hour_of_day] = avg speed (last 4 weeks)
  
  Blend real-time + historical:
    speed = 0.7 × real_time_speed + 0.3 × historical_average
    
  When will you reach segment S?
    Estimate time to reach S from current position
    Look up predicted speed for S at that future time
    More accurate ETA for long routes

ETA confidence:
  Show range: "32–40 min" when high uncertainty (construction, events)
  Show point: "35 min" when high confidence (clear highway)
  
  Sources of uncertainty: incidents, school zones, weather, live events
```

---

## Step 8: Place Search

```
User searches "coffee near me" → find relevant nearby places

Architecture: Two-phase
  Phase 1: Geo-filter (fast) → get ~100 candidates within radius
    Redis GEORADIUS places:coffee userLng userLat 2 km ASC COUNT 100
    
  Phase 2: Rank candidates by relevance
    Score = (0.4 × review_score) + (0.3 × distance_decay) + (0.2 × popularity) + (0.1 × is_open_now)
    Return top 10

Text search:
  Elasticsearch index on place name, address, category, tags
  BM25 relevance + geographic proximity boost
  
Autocomplete:
  Trie of popular place names + addresses (see Autocomplete chapter)
  Suggest "coffee shops near downtown" as you type

Data freshness:
  Google's data comes from: Google Maps uploads, business listings, scraping
  User contributions: add missing place, correct address, add hours
  Changes go through moderation queue before appearing
```

---

## Step 9: Offline Maps

```
User downloads map region for offline use:

Download flow:
  1. User selects region (city, country) on app
  2. App requests offline package: POST /offline/packages {boundingBox, zoom_levels}
  3. Server generates package:
     - All vector tiles within bounding box up to zoom 16
     - Road network for offline routing
     - Place data (name, address, category)
     - Compress: typically 100-500MB per city
  4. App downloads to local storage
  5. On-device routing engine runs Dijkstra/CH on downloaded graph

Technical challenges:
  Freshness: offline map may be days/weeks old
    Show "last updated" date; prompt update periodically
  
  Storage: city = 200MB, country = 2GB, continent = 20GB
    Progressive download: download route corridor first (navigation priority)
  
  On-device routing:
    Road network for a city: 50-500MB
    Run Contraction Hierarchies on device (precomputed during download)
    ETA = offline historical data only (no live traffic offline)
```

---

## Interview Q&A

**Q: How does Google Maps know about traffic in real time?**
A: Google uses "probe data" — anonymous speed signals from all Android phones running Google Maps with location sharing enabled. 500M+ Android phones each reporting location every few seconds creates a massive distributed sensor network. These GPS signals are map-matched to road segments, aggregated to compute actual vehicle speeds, and compared against the speed limit to classify congestion. Combined with incident reports from users and municipal traffic sensors, Google has near-real-time visibility into global road conditions.

**Q: Why does navigation recalculate the route sometimes?**
A: The routing engine re-evaluates the route periodically (every 30-60 seconds) using updated traffic data. If current traffic changes significantly — a new accident, congestion emerging on the planned route, or a faster alternative opening up — the system finds a new optimal path. The threshold for recalculation is tuned: don't recalculate for a 2-minute savings (disruptive), but do recalculate for 10+ minutes.

**Q: How do you handle map updates (new roads, building changes)?**
A: Data pipeline: field surveyors, satellite imagery analysis, user contributions, government road agency feeds. Changes go through: automated validation (does this new road connect properly to the graph?) → quality review → staging environment (A/B test routing with new data) → gradual rollout (10% of users, monitor for routing errors) → full rollout. Map edits appear within hours on Google Maps; full re-rendering of tiles takes 24-48 hours.

**Q: How does Street View work at scale?**
A: Google's Street View cars capture 360° panoramic photos every ~10 meters along roads. Each panorama = ~24MB raw → 1MB compressed. Total: billions of panoramas × 1MB = PB-scale storage. Panoramas stored in GCS with geo-indexing. Client requests: "what panorama is at this location?" → nearest neighbor lookup → serve from CDN. Navigation between panoramas = "walk to the next captured position." Image stitching and blurring (faces, license plates) done by ML pipeline before publication.

**Q: How would you design the ETA prediction for an entire route without real-time traffic?**
A: Historical speed model: for each road segment, store average speed broken down by hour of day × day of week × month (168 buckets per segment). When traffic data is unavailable (rural areas, new roads), use this historical model. Build the model from: years of aggregated probe data, speed limits, road type (highway vs residential). For a route query at 8am Tuesday, use 8am-Tuesday historical speeds for each segment. Accuracy within 10-15% of actual ETA for typical routes.
