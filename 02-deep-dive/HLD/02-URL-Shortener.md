# 📚 HLD Deep Dive — URL Shortener (TinyURL / bit.ly)

---

## 🎯 Problem Statement
Design a URL shortening service that takes long URLs and generates short aliases. When users visit the short URL, they should be redirected to the original URL.

---

## Step 1: Clarify Requirements

### Functional
- Shorten a long URL → get a short URL (e.g., `tinyurl.com/abc123`)
- Redirect short URL → original URL
- Custom aliases (optional: `tinyurl.com/my-brand`)
- Expiry (optional: links expire after N days)
- Analytics (optional: click count, geo, referrer)

### Non-Functional
- **Availability**: 99.99% — redirect must always work
- **Latency**: Redirect < 10ms (user-facing)
- **Scale**: 100M URLs created/day, 10B redirects/day
- **Durability**: URLs should not be lost
- **Read:Write ratio**: ~10:1 (more reads than writes)

---

## Step 2: Estimation

```
Writes: 100M URLs/day = 100M/86400 ≈ 1,160 writes/sec ≈ 1.2K WPS
Reads:  10B redirects/day = 10B/86400 ≈ 115,000 reads/sec ≈ 115K RPS
Peak reads: ~350K RPS

Storage per URL: ~500 bytes (long URL + short code + metadata)
Storage/day: 100M × 500 bytes = 50 GB/day
5-year storage: 50GB × 365 × 5 ≈ 90 TB

Bandwidth (reads): 115K × 500 bytes ≈ 57 MB/s
```

---

## Step 3: API Design

```
POST /api/v1/urls
  Body: { "longUrl": "https://...", "customAlias": "optional", "expiryDays": 30 }
  Response: { "shortUrl": "https://tinyurl.com/abc123", "expiresAt": "..." }

GET /{shortCode}
  Response: 302 Redirect to longUrl
  (301 = permanent, cached by browser — no analytics; 302 = temporary, always hits server)

GET /api/v1/urls/{shortCode}/stats
  Response: { "clicks": 12345, "created": "...", "lastAccessed": "..." }

DELETE /api/v1/urls/{shortCode}
  Response: 204 No Content
```

**301 vs 302**: Use **302** (temporary redirect) to always hit server for analytics tracking.

---

## Step 4: Short Code Generation

### Option 1: Base62 Encoding of Auto-increment ID
```
Characters: [0-9][a-z][A-Z] = 62 chars
Length 6 → 62^6 ≈ 56 billion unique URLs
Length 7 → 62^7 ≈ 3.5 trillion unique URLs

Algorithm:
  1. DB auto-increment ID: 1, 2, 3, ...
  2. Convert ID to Base62: 1 → "0000001", 1000000 → "4c92"

Advantage: Simple, no collisions, sortable
Tradeoff: Predictable/sequential (enumerable), requires central ID generator
```

### Option 2: MD5/SHA256 Hash of Long URL
```
Algorithm:
  1. MD5(longUrl) → 128-bit hash → take first 43 bits → Base62 encode

Advantage: No central coordinator needed
Tradeoff: Collision possible (use collision retry), not unique per user (two users same URL → same short)
```

### Option 3: Random String Generation
```
Algorithm:
  1. Generate random 6-char Base62 string
  2. Check DB for collision → retry if collision

Advantage: Simple
Tradeoff: Collisions increase as DB fills; requires unique check
```

### ✅ Recommended: Counter-based with distributed ID generator (Snowflake)
```
Snowflake ID: 64-bit = timestamp(41) + datacenter(5) + machine(5) + sequence(12)
→ Globally unique, time-sortable, no DB roundtrip
→ Encode to Base62 → ~7 chars
```

---

## Step 5: High-Level Architecture

```
                        ┌──────────────────────────────────┐
                        │           DNS / CDN               │
                        └──────────────────┬───────────────┘
                                           │
                        ┌──────────────────▼───────────────┐
                        │         Load Balancer             │
                        └──────┬─────────────────┬─────────┘
                               │                 │
                   ┌───────────▼──────┐  ┌───────▼──────────┐
                   │  Write Service    │  │  Redirect Service │
                   │  (URL creation)   │  │  (read-heavy)     │
                   └────────┬─────────┘  └───────┬──────────┘
                            │                    │
               ┌────────────▼──────────────────┐ │
               │       Redis Cache              │◄┘
               │  shortCode → longUrl (TTL 24h) │
               └────────────┬──────────────────┘
                            │ (cache miss)
               ┌────────────▼──────────────────┐
               │      Database (PostgreSQL)     │
               │  Primary (writes)              │
               │  Read Replicas (redirects)     │
               └───────────────────────────────┘
```

---

## Step 6: Database Schema

```sql
CREATE TABLE urls (
    id           BIGINT PRIMARY KEY,          -- Snowflake ID
    short_code   VARCHAR(10) UNIQUE NOT NULL, -- "abc123"
    long_url     TEXT NOT NULL,               -- original URL
    user_id      BIGINT,                      -- nullable for anonymous
    created_at   TIMESTAMP DEFAULT NOW(),
    expires_at   TIMESTAMP,                   -- nullable = never expires
    click_count  BIGINT DEFAULT 0
);

CREATE INDEX idx_short_code ON urls(short_code);  -- primary lookup
CREATE INDEX idx_user_id ON urls(user_id);         -- user's URLs
CREATE INDEX idx_expires_at ON urls(expires_at);   -- cleanup job
```

---

## Step 7: Redirect Flow (Hot Path)

```
User visits tinyurl.com/abc123:
  1. DNS resolves → CDN (if static, cached)
  2. CDN miss → Load Balancer → Redirect Service
  3. Redirect Service checks Redis: GET shortCode
     - HIT → return 302 to longUrl (< 5ms total)
     - MISS → query DB read replica → cache in Redis (TTL 24h) → return 302
  4. Async: increment click_count in DB (fire-and-forget Kafka event)
```

---

## Step 8: Scaling Deep Dives

### Cache Strategy
- Key: `shortCode`, Value: `longUrl`
- TTL: 24 hours (popular URLs stay warm; unpopular expire)
- Cache hit ratio: ~90%+ (Zipf distribution — 20% of URLs get 80% of traffic)
- Redis Cluster for HA + horizontal partitioning

### DB Scaling
- **Phase 1**: Single PostgreSQL + read replicas
- **Phase 2**: Shard by `short_code` first char (26 shards) when writes exceed 10K WPS
- **Consistent hashing** for future reshard

### Analytics at Scale
- Don't increment `click_count` synchronously (write amplification)
- Instead: publish click event to **Kafka** → Analytics consumer aggregates
- Use time-series DB (InfluxDB) for click metrics per interval

### URL Expiry
- Background cron job: `DELETE FROM urls WHERE expires_at < NOW()`
- Run every hour on off-peak
- Proactive: Check expiry on redirect, return 404 if expired

---

## Step 9: Trade-offs Discussed

| Decision | Choice | Reason |
|----------|--------|--------|
| Redirect type | 302 | Analytics tracking (301 cached by browser) |
| Short code generation | Snowflake + Base62 | No collisions, no central DB, sortable |
| Cache | Redis, TTL 24h | 90% hit ratio; Zipf distribution helps |
| DB | PostgreSQL + read replicas | ACID for URL creation; replicas for reads |
| Analytics | Async via Kafka | Decouple hot redirect path from analytics writes |
| Consistency | Eventual for analytics | Click count can lag; redirect must be immediate |

---

## Step 10: Failure Modes

| Failure | Impact | Mitigation |
|---------|--------|-----------|
| Redis down | All redirects hit DB | Read replicas absorb; Redis HA cluster |
| DB primary down | Can't create URLs | Failover to standby (< 30s with automatic promotion) |
| Redirect service down | Users can't redirect | Multiple instances + health checks |
| Snowflake ID generator down | Can't create short URLs | Multiple ID generator instances |
| Hash collision (MD5 approach) | Wrong redirect | Check uniqueness + retry with incremented suffix |
