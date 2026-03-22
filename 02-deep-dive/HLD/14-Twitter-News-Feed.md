# 📚 System Design — Social Media News Feed (Twitter / Instagram)

---

## 🎯 Problem Statement
Design a social media news feed where users can post content, follow other users, and see a personalized feed of posts from people they follow, sorted by recency or relevance.

---

## Step 1: Clarify Requirements

### Functional
- Post tweets (text ≤ 280 chars, optional images/videos)
- Follow / unfollow users
- View home timeline (posts from followed users, newest first)
- User profile page (own tweets, followers/following count)
- Search tweets and users
- Trending topics / hashtags
- Notifications (likes, retweets, mentions)

### Non-Functional
- **Scale**: 500M users, 100M DAU, 500M tweets/day
- **Latency**: Timeline load < 200ms (p99)
- **Availability**: 99.99%
- **Consistency**: Eventual OK (seeing a tweet 1 second late is fine)
- **Read:Write**: ~90:10 (reads dominate)

---

## Step 2: Estimation

```
Tweets:   500M/day = 5,787/sec = ~6K TPS writes
Views:    Each tweet seen avg 200 times → 100B views/day = 1.16M reads/sec
Peak:     3x = ~3.5M reads/sec

Storage:
  Tweet: 280 chars + metadata = ~500 bytes
  500M tweets/day × 500 bytes = 250 GB/day
  10-year storage: ~900 TB (text only)
  Media (30% with images, avg 200KB) = 500M × 0.3 × 200KB = 30 TB/day

User follows:
  100M DAU × avg 500 follows = 50B follow relationships
  50B × 8 bytes = ~400 GB
```

---

## Step 3: API Design

```
POST   /api/v1/tweets                      → create tweet
DELETE /api/v1/tweets/{id}                → delete tweet
GET    /api/v1/tweets/{id}                → get tweet
POST   /api/v1/tweets/{id}/retweet        → retweet
POST   /api/v1/tweets/{id}/like           → like/unlike
GET    /api/v1/users/{id}/timeline        → user's own tweets
GET    /api/v1/home/timeline?cursor=...   → home feed (cursor-based pagination)
POST   /api/v1/follows                    → follow user {followerId, followeeId}
DELETE /api/v1/follows/{followeeId}       → unfollow
GET    /api/v1/search?q=...              → search tweets/users
GET    /api/v1/trends                    → trending topics
```

---

## Step 4: High-Level Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Clients                              │
└──────────────────────┬───────────────────────────────────┘
                       │
               ┌───────▼────────┐
               │  API Gateway   │
               │ (auth, routing)│
               └──┬──┬──┬──┬───┘
                  │  │  │  │
    ┌─────────────┘  │  │  └──────────────────┐
    │                │  │                      │
┌───▼────┐    ┌──────▼──┐  ┌────────────┐  ┌──▼──────────┐
│ Tweet  │    │Timeline │  │  User/     │  │  Search     │
│Service │    │Service  │  │  Follow    │  │  Service    │
└───┬────┘    └────┬────┘  │  Service   │  └──────┬──────┘
    │              │       └────────────┘          │
    │              │                               │
┌───▼────────────────────────────────────────┐    │
│              Kafka (event bus)              │    │
└────────────┬──────────┬────────────────────┘    │
             │          │                          │
    ┌────────▼──┐  ┌────▼──────────┐    ┌─────────▼──┐
    │ Tweet DB  │  │ Feed Cache    │    │Elasticsearch│
    │(Cassandra)│  │ (Redis)       │    │             │
    └───────────┘  └───────────────┘    └─────────────┘
                      Pre-computed
                      timeline feeds
```

---

## Step 5: The Core Problem — Feed Generation

The hardest part of news feed design. Two fundamental approaches:

### Approach 1: Fan-out on Write (Push Model)
```
When User A (100 followers) posts a tweet:
  1. Write tweet to Tweet DB
  2. For each follower: append tweetId to their timeline cache
     → 100 Redis writes

When User reads timeline:
  1. Read pre-computed timeline from Redis cache → fast!

PRO: O(1) read time, always fresh
CON: User with 10M followers → 10M Redis writes per tweet!
     "Hot user problem" (celebrities)
```

### Approach 2: Fan-out on Read (Pull Model)
```
When User A posts a tweet:
  1. Write tweet to Tweet DB
  → Done. No fanout.

When User reads timeline:
  1. Fetch all N users they follow
  2. Fetch last 50 tweets from each
  3. Merge + sort by time
  → Very slow! N × DB reads per timeline load

PRO: No write amplification, great for high-follower accounts
CON: O(N) DB reads per timeline load, very slow at scale
```

### ✅ Twitter's Hybrid Approach
```
Regular users (< 1M followers) → Fan-out on Write
  Post tweet → push to all followers' timeline caches immediately

Celebrity users (> 1M followers) → Fan-out on Read
  Post tweet → NOT pushed to timelines
  
Timeline read:
  1. Fetch pre-computed timeline from Redis (contains non-celebrity tweets)
  2. Separately fetch recent tweets from followed celebrities (< 20 typically)
  3. Merge at read time

Result:
  Regular reads: O(1) from Redis
  Celebrity tweets: O(celebrities_followed) ≈ O(20) — manageable
```

### Timeline Cache Design
```
Redis data structure per user:
  Key: timeline:{userId}
  Value: Sorted Set, score = timestamp, member = tweetId

  ZADD timeline:user123 1700000100 tweet456
  ZADD timeline:user123 1700000200 tweet789
  ZRANGE timeline:user123 0 49 WITHSCORES  → latest 50 tweets

Cache capacity:
  100M DAU × 50 tweet IDs × 8 bytes = 40 GB (manageable in Redis cluster)
  
Cache eviction:
  LRU — inactive users' timelines evicted
  On next login: rebuild timeline from DB (cold start for inactive users)
```

---

## Step 6: Tweet Storage

```
Why Cassandra (not SQL)?
  - Write-heavy (6K TPS), read-heavy (1.16M RPS)
  - Time-series access pattern (newest tweets first)
  - Linear horizontal scale
  - No complex joins needed
  
Schema:
  tweets (
    user_id     UUID,
    tweet_id    TIMEUUID,  -- time-ordered UUID (sorting built-in)
    content     TEXT,
    media_urls  LIST<TEXT>,
    like_count  COUNTER,
    retweet_count COUNTER,
    PRIMARY KEY (user_id, tweet_id)
  ) WITH CLUSTERING ORDER BY (tweet_id DESC)
  -- Partition by user_id → O(1) to fetch user's tweets
  -- Cluster by tweet_id (time) → sorted by newest first
```

---

## Step 7: Follow Graph Storage

```
Options:
  1. RDBMS: follows(follower_id, followee_id)
     Fine for < 1B rows; hard to scale graph queries
     
  2. Graph DB (Neo4j): natural for relationship queries
     "Friends of friends", "Who to follow" recommendations
     Expensive at Twitter scale
     
  3. ✅ Wide-column (Cassandra) + Graph cache (Redis):
     followers_by_user (user_id, follower_ids) → who follows me
     following_by_user (user_id, following_ids) → who I follow
     
     Redis SET: following:{userId} → set of followee IDs
     Fast lookup for fan-out: SMEMBERS following:user123 → [id1, id2, ...]
```

---

## Step 8: Trending Topics

```
Algorithm:
  Count hashtag occurrences in a sliding time window (last 1 hour)
  Compare to baseline (expected frequency from historical data)
  "Trending" = significantly above expected frequency

Implementation:
  Tweets → Kafka → Flink/Kafka Streams window aggregation
  Every 5 min: TOP-K hashtags by count → Redis ZADD trending score hashtag
  API: ZREVRANGE trending 0 9 → top 10 trends

Geo-specific trends:
  Partition counts by country/city
  Separate trending sorted set per region
```

---

## Step 9: Notifications

```
Types: like, retweet, mention, new follower, DM

Architecture:
  Action (like/RT/follow) → Kafka topic: notifications.events
  Notification Service consumes → checks user preferences → delivers
  
Delivery:
  Push notification: APNs (iOS) / FCM (Android) for mobile
  WebSocket: for web clients (real-time counter update)
  In-app inbox: Cassandra storage (partition by user_id)
  Email: async, throttled (don't spam)

Rate limiting notifications:
  Max 1 "X liked your tweet" push per hour (batched: "X and 5 others liked...")
  Don't send "1 new follower" 100x if someone gets viral
```

---

## Step 10: Search

```
Tweet indexing:
  Tweet created → Kafka → Elasticsearch indexing consumer
  Indexed fields: content (full-text, BM25), user_id, created_at, hashtags
  
  Query: "react hooks" → BM25 relevance score × recency score
  
  Elasticsearch index settings:
    Shards: 20 (horizontal scale)
    Replicas: 2 (read scale)
    TTL: tweets > 30 days moved to cold storage / archived index

User search:
  Separate index on username, display name
  Auto-suggest: Trie in memory for prefix matching on popular accounts
```

---

## Interview Q&A

**Q: Why use cursor-based pagination instead of offset for the timeline?**
A: Offset pagination (`?page=2&limit=20`) breaks when new tweets are inserted — items shift, causing duplicates or gaps. Cursor-based (`?cursor=tweetId`) uses the last seen ID — always consistent regardless of new inserts. Also more efficient — no need to count/skip N rows in DB.

**Q: How do you handle the case where a user follows 5000 accounts?**
A: Fan-out on write would produce 5000 DB reads to rebuild the timeline on cache miss. Solution: (1) Cap timeline rebuild to most recent 500 tweets from top 200 most interacted-with accounts. (2) Lazy load older tweets on scroll. (3) Apply relevance scoring to reduce which accounts to fetch from.

**Q: How would you implement "show tweets from people you follow, sorted by relevance, not just time"?**
A: Two-phase ranking: (1) Fetch last 200 recent tweets from followed accounts (fast, from Redis/Cassandra). (2) Score each tweet with ML model: engagement rate of author, your past interaction with them, content relevance, recency decay. Return top 50 by score. Twitter/Instagram both shifted from chronological to algorithmic feed.

**Q: How does Twitter handle a user with 50M followers posting a tweet?**
A: This is the "celebrity problem." Twitter skips fan-out for accounts > 1M followers. The tweet is stored in Cassandra. When users load their timeline, celebrity tweets are fetched separately and merged. They typically follow < 20 celebrities, so it's only 20 extra reads.

**Q: How would you design "like count" to not cause hot row contention?**
A: Counters in Cassandra use the COUNTER type (CAS-free atomic increment, no locking). For Redis: use atomic INCR. For SQL: use a separate counters table with Redis caching, batched DB updates via Kafka. Never UPDATE tweet SET likes=likes+1 under high load in SQL — it causes row locking.
