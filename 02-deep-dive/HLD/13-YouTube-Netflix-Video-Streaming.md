# 📚 System Design — Video Streaming Platform (YouTube / Netflix)

---

## 🎯 Problem Statement
Design a video streaming platform where users can upload videos, watch them with adaptive quality, search content, and receive recommendations.

---

## Step 1: Clarify Requirements

### Functional
- Upload videos (up to 10GB)
- Stream videos at adaptive bitrate (144p to 4K)
- Search videos by title/tags
- Recommendation feed (homepage, "up next")
- View counts, likes, comments
- User subscriptions / channels
- Video processing: transcode, thumbnail generation

### Non-Functional
- **Scale**: 2B users (YouTube scale), 500 hours of video uploaded per minute
- **Availability**: 99.99% for streaming
- **Latency**: Video starts within 2 seconds (TTFF — Time To First Frame)
- **Bandwidth**: Adaptive — reduce quality on slow connections
- **Storage**: ~1 exabyte of video data (YouTube scale)
- **Read:Write ratio**: 100:1 (far more views than uploads)

---

## Step 2: Estimation

```
Uploads:    500 hrs/min = 8.3 hrs/sec
            1 hr video raw = ~10 GB
            Transcoded (multiple resolutions) = ~5x = 50 GB/hr
            Storage/day = 500 × 60 × 24 × 50 GB = ~36 PB/day 🔥

Views:      2B users, avg 30 min/day = 60B min/day
            QPS (views) = 60B / 86,400 ≈ 700K video plays/sec
            Bandwidth = 700K × 1 Mbps (avg) = 700 Gbps streaming bandwidth

Search:     ~10% of users search/day = 200M searches/day = ~2,300 QPS
```

---

## Step 3: API Design

```
POST   /api/v1/videos/upload          → initiate upload, get upload URL
PUT    /api/v1/videos/{id}/chunk      → upload chunk (resumable upload)
POST   /api/v1/videos/{id}/publish    → trigger processing pipeline
GET    /api/v1/videos/{id}            → video metadata + streaming URLs
GET    /api/v1/videos/{id}/stream     → adaptive manifest (HLS/DASH)
GET    /api/v1/search?q=...&page=...  → search results
GET    /api/v1/feed/recommendations   → personalized home feed
POST   /api/v1/videos/{id}/views      → record view (async)
POST   /api/v1/videos/{id}/likes      → like/unlike
GET    /api/v1/channels/{id}/videos   → channel video list
```

---

## Step 4: High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Clients                           │
│            Web | Mobile | Smart TV                  │
└──────────────────────┬──────────────────────────────┘
                       │
               ┌───────▼────────┐
               │   CDN (Global)  │  ← serves 90%+ of video traffic
               │ Akamai/CloudFront│
               └───────┬────────┘
                       │ (cache miss or upload)
               ┌───────▼────────┐
               │  API Gateway   │  ← auth, rate limiting, routing
               └──┬──────────┬──┘
                  │          │
        ┌─────────▼──┐  ┌────▼─────────┐
        │  Upload     │  │  Streaming   │
        │  Service    │  │  Service     │
        └─────────┬───┘  └──────────────┘
                  │
        ┌─────────▼──────────┐
        │  Video Processing  │  ← transcoding pipeline
        │  Pipeline (async)  │
        └─────────┬──────────┘
                  │
   ┌──────────────┼──────────────────────┐
   ▼              ▼                      ▼
Object Store   Metadata DB          Search Engine
(S3/GCS)      (MySQL + replicas)   (Elasticsearch)
Raw + segments  Video, User, Channel  Full-text search
```

---

## Step 5: Video Upload & Processing Pipeline

```
Step 1: Resumable Upload
  Client → Upload Service: "I want to upload 5GB video"
  Upload Service → S3: create pre-signed multipart upload URL
  Client → S3: upload chunks (5MB each) in parallel
  Upload Service: tracks chunk receipt

Step 2: Processing Pipeline Trigger
  S3 event → Kafka topic: video.uploaded
  Processing Orchestrator picks up event

Step 3: Transcoding (most expensive step)
  Input: raw video (MP4/MOV/AVI) at original resolution
  Output: multiple resolutions in HLS/DASH segments

  Transcoding Queue:
    Worker 1: 1080p H.264 (high quality)
    Worker 2: 720p H.264 (standard)
    Worker 3: 480p H.264 (mobile)
    Worker 4: 360p H.265 (very low bandwidth)
    Worker 5: thumbnail extraction (every 10 seconds)
    Worker 6: audio only track

  Segments: 2-10 second chunks for adaptive streaming
  Manifest: HLS (.m3u8) / DASH (.mpd) lists available bitrates

Step 4: CDN Distribution
  Transcoded segments uploaded to S3 (origin)
  CDN pulls from origin on first request
  Segments cached at PoPs worldwide

Step 5: Metadata Update
  Update video status: processing → available
  Generate video page, index in Elasticsearch
```

### Adaptive Bitrate Streaming (ABR)
```
Player monitors download speed and buffer level:
  Network fast → request higher quality chunk
  Network slow → request lower quality chunk (no buffering/rebuffering)

HLS (Apple HTTP Live Streaming) — dominant on mobile/Apple
DASH (Dynamic Adaptive Streaming over HTTP) — more open, used by YouTube

Manifest file lists available quality levels:
  #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
  360p/segment.m3u8
  #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720
  720p/segment.m3u8
  #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1920x1080
  1080p/segment.m3u8
```

---

## Step 6: Video Storage Architecture

```
Tiers:
  Hot storage (S3 Standard): recent videos, frequently accessed
    Cost: ~$23/TB/month
  Warm storage (S3 Infrequent Access): videos > 1 year old
    Cost: ~$12.5/TB/month
  Cold storage (S3 Glacier): videos > 3 years old, rarely watched
    Cost: ~$4/TB/month

Deduplication:
  Hash(video content) → detect re-uploads
  Facebook/YouTube: billions of duplicate uploads — dedup saves ~30% storage

Segment naming for CDN:
  /{videoId}/{quality}/{segmentNumber}.ts
  e.g., /abc123/1080p/0001.ts, /abc123/1080p/0002.ts, ...
```

---

## Step 7: Database Schema

```sql
CREATE TABLE videos (
    id           VARCHAR(11) PRIMARY KEY,  -- YouTube-style ID
    user_id      BIGINT,
    title        VARCHAR(500),
    description  TEXT,
    duration_sec INT,
    status       ENUM('processing','available','deleted'),
    view_count   BIGINT DEFAULT 0,
    like_count   BIGINT DEFAULT 0,
    created_at   TIMESTAMP,
    thumbnail_url VARCHAR(500)
);

-- Separate high-update counters (avoid hot row contention)
CREATE TABLE video_stats (
    video_id     VARCHAR(11),
    date         DATE,
    views        BIGINT DEFAULT 0,
    likes        BIGINT DEFAULT 0,
    PRIMARY KEY (video_id, date)
);
-- Daily rollup to avoid locking on every view

CREATE TABLE users (
    id           BIGINT PRIMARY KEY,
    username     VARCHAR(100) UNIQUE,
    subscriber_count BIGINT DEFAULT 0
);

CREATE TABLE subscriptions (
    subscriber_id  BIGINT,
    channel_id     BIGINT,
    created_at     TIMESTAMP,
    PRIMARY KEY (subscriber_id, channel_id)
);
```

---

## Step 8: View Count & Like System

```
Problem: 700K views/sec → can't UPDATE videos SET view_count=view_count+1 for each
         That's 700K write-locked row updates per second

Solution: Async counter aggregation
  1. View event → Kafka topic: video.viewed
  2. Counter Aggregator (Flink/Kafka Streams): batch count per video per minute
  3. Periodic UPDATE: UPDATE video_stats SET views = views + N WHERE ...
  4. For display: Redis cache of view count (refreshed every 30 sec from DB)

Result: ~1000 DB writes/min instead of 700K/sec
```

---

## Step 9: Search & Recommendations

```
Search:
  Video metadata indexed in Elasticsearch
  Fields: title (boosted), description, tags, transcript (AI-generated captions)
  Query: multi-match with BM25 + popularity score boost
  
  GET /search?q=react hooks tutorial
  → Elasticsearch → ranked by relevance × popularity

Recommendations:
  Collaborative Filtering: "users who watched this also watched..."
  Content-Based: video metadata similarity (tags, description embeddings)
  
  Architecture:
    Offline: pre-compute recommendations daily (Spark ML)
    Online: refresh on user activity (Kafka event → update user vector)
    
  Storage: Redis Sorted Set per user
    ZADD recommendations:user123 score videoId
    ZRANGE recommendations:user123 0 20 → top 20 recommendations
```

---

## Step 10: Scaling Deep Dives

### CDN Strategy
```
Video segments: cache at CDN permanently (immutable content)
Thumbnails: cache 7 days
Manifests (.m3u8): cache 30 seconds (can update quality list)
Metadata API: cache 60 seconds

Cache hit ratio target: 95%+ for video segments
Miss handling: CDN fetches from S3 origin (not your servers)
```

### Transcoding Scaling
```
Problem: 500 hrs/min → need massive transcoding compute
Solution: Serverless/auto-scaling transcoding workers
  AWS Elastic Transcoder / AWS MediaConvert
  Each video = N parallel jobs (one per quality level)
  Priority queue: new uploads > re-encoding old content

At YouTube scale: dedicated hardware encoders + custom ASIC chips
```

### View Count at Scale
```
YouTube approach: approximate counts
  Show "12M views" not "12,345,678 views"
  Update every 24 hours from aggregated data
  Use probabilistic data structures (HyperLogLog) for unique viewers
```

---

## Interview Q&A

**Q: Why use HLS/DASH instead of just serving the full video file?**
A: Adaptive bitrate — player adjusts quality based on network. Resumability — user can seek without downloading full file. CDN efficiency — small segments cache better. DRM support — per-segment encryption. If you served the full file, buffering and quality would be poor on variable networks.

**Q: How do you prevent unauthorized video downloads?**
A: Signed URLs (expire after 10 minutes), stream from CDN only, DRM (Widevine for Android/Chrome, FairPlay for Apple). Watermarking for premium content. Note: determined users can always capture screen — focus on making it non-trivial.

**Q: How would you scale the video processing pipeline to handle 500 hrs/min?**
A: Auto-scaling transcoding workers (Kubernetes HPA on queue depth). Parallel transcoding of different quality levels simultaneously. GPU instances for faster encoding (H.265 is 40% smaller than H.264 at same quality). Prioritize new uploads over background re-encoding. Netflix uses a GPU cluster; YouTube uses custom ASICs.

**Q: How do you design the recommendation system for a new user (cold start)?**
A: For new users: show trending/popular content globally. After 5+ watched videos: begin collaborative filtering. Ask preferences during onboarding (genres, languages). Use content-based filtering (similar to what they've watched) until enough data for collaborative.

**Q: How would you handle a viral video getting 100M views in 1 hour?**
A: CDN absorbs streaming traffic (horizontal by design). View counter: Kafka buffer absorbs write spike; batch aggregate prevents DB overload. Redis cache for view count (refreshed periodically). Auto-scale transcoding workers if video needs re-encoding. Pre-warm CDN for anticipated viral events.

**Q: How does YouTube achieve 2 second start time globally?**
A: CDN edge caching (video segment at PoP nearest user), pre-buffering (client buffers a few segments ahead), progressive loading (start at 360p, upgrade quality as buffer fills), predictive pre-fetch (start loading "up next" video before current ends).
