# 📚 System Design — Social Network / Q&A Feed (Reddit / Quora)

---

## 🎯 Problem Statement
Design a community-driven discussion platform where users can post content in communities (subreddits/topics), vote on posts and answers, comment in threads, and see a ranked feed of popular and relevant content.

---

## Step 1: Clarify Requirements

### Functional
- Create posts (text, link, image) in communities/topics
- Upvote / downvote posts and comments
- Comment threads (nested replies, multiple levels deep)
- Feed types: Home (personalised), Popular, New, Rising, Community
- Search posts and comments
- User profiles (karma, post history)
- Community management (rules, moderators, flairs)
- Awards / badges
- Notifications (comment replies, post mentions)

### Non-Functional
- **Scale**: 500M users, 50M DAU, 1B votes/day (Reddit scale)
- **Read:Write**: 98:2 — extremely read-heavy
- **Latency**: Feed load < 200ms; vote action < 100ms
- **Consistency**: Vote counts: eventual OK (show ~real count, not exact); Post existence: strong
- **Availability**: 99.99%

---

## Step 2: Estimation

```
Posts:      500K posts/day = 5.8/sec
Comments:   5M comments/day = 57/sec
Votes:      1B votes/day = 11,574/sec

Vote storage:
  Each vote: 20 bytes (userId, postId, direction, timestamp)
  1B/day × 20 bytes = 20GB/day vote data
  10 years: 73TB (manageable)

Post storage:
  Text post: ~2KB; image post: ~500KB (image in S3, metadata in DB)
  500K/day × 2KB = 1GB/day text storage

Feed:
  50M DAU × 3 feed loads/day = 150M feed requests/day = 1,736 QPS avg
  Peak: ~10K QPS
```

---

## Step 3: API Design

```
# Posts
POST /api/v1/communities/{id}/posts   → create post
GET  /api/v1/posts/{id}              → get post + vote count
POST /api/v1/posts/{id}/vote         → { direction: "up"|"down"|"none" }
DELETE /api/v1/posts/{id}

# Comments
POST /api/v1/posts/{id}/comments
  Body: { content, parentCommentId? }
GET  /api/v1/posts/{id}/comments?sort=top|new|controversial

# Feed
GET  /api/v1/feed/home?sort=hot|new|top&after={cursor}
GET  /api/v1/feed/popular
GET  /api/v1/communities/{id}/feed?sort=hot|new|top

# Search
GET  /api/v1/search?q=...&type=post|comment|community

# Communities
POST /api/v1/communities
GET  /api/v1/communities/{id}
POST /api/v1/communities/{id}/subscribe
```

---

## Step 4: High-Level Architecture

```
┌────────────────────────────────────────────────┐
│                  Clients                        │
└──────────────────────┬─────────────────────────┘
                       │
               ┌───────▼────────┐
               │  API Gateway   │
               └──┬──┬──┬──┬───┘
                  │  │  │  │
     ┌────────────┘  │  │  └──────────────┐
     │               │  │                 │
┌────▼─────┐   ┌─────▼──▼──┐   ┌─────────▼──┐
│  Post    │   │   Feed    │   │  Vote      │
│  Service │   │  Service  │   │  Service   │
└────┬─────┘   └─────┬──────┘   └─────────┬──┘
     │               │                    │
     │         ┌─────▼──────┐            │
     │         │  Ranking   │            │
     │         │  Engine    │            │
     │         └────────────┘            │
     │                                   │
┌────▼──────────────────────────────────▼─┐
│              Kafka (event bus)           │
└─────────────────────────────────────────┘
          │           │           │
   ┌──────▼──┐  ┌─────▼─────┐  ┌─▼──────────┐
   │Post DB  │  │Vote/Score │  │Elasticsearch│
   │(Postgres│  │DB (Cassan)│  │(search)     │
   │+ Redis) │  │+ Redis    │  └─────────────┘
   └─────────┘  └───────────┘
```

---

## Step 5: Ranking Algorithm — The Heart of Reddit

Reddit's "Hot" algorithm is one of the most studied ranking systems.

### Reddit Hot Score Formula
```python
import math
from datetime import datetime

def hot_score(ups, downs, post_time):
    """
    Score that balances votes with recency.
    Popular AND recent = high score.
    Popular BUT old = medium score.
    Recent BUT no votes = medium score.
    """
    score = ups - downs  # net votes
    
    order = math.log(max(abs(score), 1), 10)  # log scale (1000 votes ≈ 3 points)
    
    sign = 1 if score > 0 else (-1 if score < 0 else 0)
    
    # Seconds since Reddit epoch (Dec 8, 2005)
    epoch = datetime(2005, 12, 8, 7, 46, 43).timestamp()
    seconds = post_time.timestamp() - epoch
    
    # Every 12.5 hours ≈ 1 "point" of recency
    return round(sign * order + seconds / 45000, 7)

# Result: recent post with 0 votes scores HIGHER than week-old post with 100 votes
# Because: recency dominates over vote count on short timescales
# Balances "new" content surfacing vs perpetually popular old posts
```

### Other Sorting Modes
```
New:      ORDER BY created_at DESC (trivial, no ranking)

Top (all time / this week / today):
  ORDER BY (upvotes - downvotes) DESC
  Filter: WHERE created_at > now - interval
  Simple but doesn't decay

Rising:
  Posts gaining votes faster than their age predicts
  velocity = votes_last_hour / age_hours
  ORDER BY velocity DESC

Controversial:
  Posts with many votes BUT close to 50/50 split
  score = (ups + downs) / (abs(ups - downs) + 1)
  High score = many votes AND tied = controversial!
  ORDER BY score DESC

Best (comment ranking — Wilson score):
  Uses statistical confidence interval for true upvote rate
  Accounts for low vote count uncertainty
  New comment with 1 upvote is NOT better than old with 100 upvotes
  Wilson lower bound: score = (p + z²/(2n) - z*sqrt((p(1-p) + z²/(4n))/n)) / (1 + z²/n)
  where p = upvote fraction, n = total votes, z = 1.96 (95% confidence)
```

---

## Step 6: Vote System at Scale

```
1B votes/day = 11,574 votes/sec. Cannot UPDATE post SET score=score+1 at this rate.

Solution: Async vote aggregation (same pattern as YouTube view counts)

Step 1: Client votes
  POST /api/v1/posts/{id}/vote {direction: "up"}
  → Immediate response: 200 OK (optimistic)
  → Publish to Kafka: votes.events {userId, postId, direction, timestamp}

Step 2: Vote deduplication (each user votes once per post)
  Kafka consumer checks Redis Set: SADD voted:{postId} userId
  If SADD returns 0 (already exists) → user already voted → deduplicate
  Handle vote CHANGE: remove old vote, add new

Step 3: Score aggregation (Flink streaming)
  5-minute rolling window: COUNT votes by postId, direction
  NET_SCORE = upvotes - downvotes
  Update Cassandra: votes table per post
  Update Redis: score:{postId} → new score (for fast feed ranking)

Step 4: Feed read uses cached score
  GET /api/v1/feed → uses Redis sorted set with score
  Score visible to user: may lag actual by ~30 seconds (acceptable)

User's own vote (must be consistent):
  User's own vote is written to their vote history immediately
  Redis: HSET user:{userId}:votes {postId} "up"
  → Immediately shows correct state for the voter
  → Others see eventual count (< 1 min lag)
```

### Vote Integrity
```
Double-vote prevention:
  Redis: SETNX vote:{postId}:{userId} "up" EX 86400
  Returns 0 → already voted → reject or record vote change

Vote count display:
  Reddit famously "fuzzes" vote counts (shows approximate counts)
  Prevents gaming (users trying to hit exact thresholds)
  Also helps with: showing cached counts without exact sync overhead
```

---

## Step 7: Comment Thread Design

```
Threaded comments = tree structure (each comment has a parentId)

Storage: Adjacency list (simple, works fine for most cases)
  comments (
    id          BIGINT PRIMARY KEY,
    post_id     BIGINT,
    parent_id   BIGINT,        -- null for top-level comments
    author_id   BIGINT,
    content     TEXT,
    score       INT,
    depth       INT,           -- precomputed nesting depth
    path        LTREE,         -- e.g., "1.45.234" for efficient subtree queries
    created_at  TIMESTAMP,
    deleted     BOOLEAN        -- soft delete (Reddit's [deleted] behaviour)
  )

PostgreSQL LTREE extension:
  Efficient subtree queries: WHERE path <@ '1.45'  (all children of comment 45)
  Efficient ancestor queries: WHERE '1.45.234' <@ path

Comment loading strategy:
  Load top-level comments (parent_id IS NULL), sorted by score
  Lazy load replies (fetch when "show replies" clicked)
  Server sends first 3 levels on initial load, rest on demand

Score computation: Wilson score (discussed above) → best reply ranks highest
Collapse: client-side collapse of threads below score threshold
```

---

## Step 8: Feed Generation

```
Two types of feed to handle:

1. Community feed (r/programming, r/india):
   All posts in one community, sorted by hot/new/top
   
   Pre-computed: Offline job re-ranks every 5 minutes
   Store in Redis Sorted Set:
     ZADD feed:community:{communityId}:hot score postId
   
   Serve: ZREVRANGE feed:community:{communityId}:hot 0 24 → top 25 posts
   Pagination: ZREVRANGEBYSCORE with score cursor

2. Home feed (personalised, posts from subscribed communities):
   User subscribes to 50 communities
   Must merge feeds from all 50
   
   Fan-out on read (pull model — simpler for Quora/Reddit):
     On each feed request:
       GET subscribed communities for user
       For each community: get top 10 posts from Redis
       Merge and re-rank by hot score
       Return top 25
     
   Problem: User subscribes to 500 communities → 500 Redis reads per feed
   Solution: 
     Cache the merged home feed per user: SETEX homefeed:{userId} 300 {postIds}
     Invalidate when any subscribed community gets a new high-ranking post
     Background refresh: pre-generate for active users

3. r/all / Popular (global trending):
   Aggregate across all communities
   Cannot compute from all 100K communities in real time
   Solution: Maintain a global sorted set of currently hot posts
     Any post that enters top 100 in its community → added to global set
     Kafka event → global feed updater → ZADD global:hot score postId
```

---

## Step 9: Search

```
Full-text search on posts and comments:
  Elasticsearch index:
    posts: title (boosted 3x), content, community, author, score, created_at
    comments: content, post_id, author, score

  Indexing pipeline:
    Post/comment created → Kafka → Elasticsearch indexing consumer
    Vote score updated hourly → re-index score field
    
  Search query:
    q="react hooks tutorial" →
      Multi-match: title^3 + content
      Filter: community (optional), date range
      Sort: relevance × log(score) (popular results preferred)
    
  Autocomplete: Elasticsearch completion suggester on post titles
  
  Faceted search: filter by community, time range, content type
```

---

## Interview Q&A

**Q: How do you design the "Rising" feed to surface new content gaining momentum?**
A: Track vote velocity per post — votes per hour / post age in hours. A 1-hour-old post with 50 votes is "rising" faster than a 24-hour-old post with 200 votes. Implement with a Flink streaming job: sliding 1-hour window, count votes per post, compute velocity = votes/hour × (1/sqrt(age_hours)). Store top-K rising posts per community in a Redis sorted set, refreshed every 5 minutes.

**Q: How does Reddit handle content moderation at scale?**
A: Three-layer system: (1) AutoModerator — community rules evaluated as YAML conditions per post/comment, runs synchronously on submission. (2) ML models — trained on human-moderated content, classify spam/hate speech, run async via Kafka pipeline. (3) Human moderators — community volunteers review flagged content queues. For site-wide violations: Reddit admins have override capability. Removed content soft-deleted (content hidden, record preserved for appeals).

**Q: How would you implement the "save post" feature that must work cross-device?**
A: Store in PostgreSQL: `saved_posts(userId, postId, savedAt)`. Indexed on userId for O(log n) fetch of user's saved posts. Cache in Redis: sorted set ZADD saved:{userId} timestamp postId (for fast recent lookups). On save: write to DB + update Redis. Cross-device: both read from DB (consistent). Rate limit: max 1000 saved posts (cleanup old ones or show paginated).

**Q: How do you handle a community ban while ensuring existing posts are hidden?**
A: Soft-ban flag in community table. Feed queries include `AND community.status != 'banned'`. Redis feed caches invalidated on ban. Search index: add `is_banned` boolean field, filter on search. CDN-cached pages: send cache purge to CDN for all posts from that community. For legal reasons: content may need physical deletion after 30 days (scheduled cleanup job).

**Q: How does Quora's "answer ranking" differ from Reddit's vote-based ranking?**
A: Quora uses a proprietary ranking model that weighs: author credentials (is this person an expert in this topic?), answer quality signals (length, citations, reading time), helpfulness votes, and recency. Unlike Reddit's democratic upvote model, Quora weights expert answers higher even with fewer votes — a doctor's answer to a medical question ranks above a highly-upvoted anecdote. ML model trained on long-term engagement signals (users marking answers helpful months later).
