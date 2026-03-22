# 📚 System Design — Search Autocomplete / Typeahead

---

## 🎯 Problem Statement
Design a search autocomplete system that suggests relevant search queries as the user types, with sub-100ms response time, personalization, and support for billions of queries per day.

---

## Step 1: Clarify Requirements

### Functional
- Return top 5–10 suggestions as user types each character
- Suggestions based on: query popularity, recency, user's search history
- Support prefix matching ("rea" → "react tutorial", "react hooks", "react native")
- Trending suggestions for popular current topics
- Personalized suggestions (user's past searches ranked higher)
- Handle typos / fuzzy matching (optional)
- Multiple languages

### Non-Functional
- **Latency**: Response < 100ms p99 (users notice anything > 100ms)
- **Scale**: Google scale = 5B queries/day, peak = 100K QPS for autocomplete
- **Availability**: 99.99%
- **Data freshness**: New trending queries appear in suggestions within 30 minutes

---

## Step 2: Estimation

```
Autocomplete requests:
  Average query = 5 characters typed
  Each character typed = 1 autocomplete request
  5B queries/day × 5 chars = 25B autocomplete requests/day
  25B / 86,400 = ~290K RPS average; peak ~900K RPS

Storage for top queries:
  Top 10M phrases × avg 30 chars + frequency = ~400MB (fits in memory!)
  Full query index with all prefixes: trie compressed ~5-10GB

Query log:
  5B queries/day × 100 bytes = 500GB/day (raw logs for analytics)
```

---

## Step 3: API Design

```
GET /api/v1/suggest?q=rea&limit=5&lang=en&userId=optional
Response:
{
  "suggestions": [
    { "text": "react tutorial", "score": 9842, "type": "trending" },
    { "text": "react hooks", "score": 8761, "type": "popular" },
    { "text": "react native", "score": 7234, "type": "popular" },
    { "text": "react router", "score": 5123, "type": "popular" },
    { "text": "react vs vue", "score": 4567, "type": "popular" }
  ],
  "query": "rea",
  "latency_ms": 8
}

No authentication required (public endpoint)
Rate limit per IP: 100 req/sec
```

---

## Step 4: High-Level Architecture

```
Typing...        ┌─────────────────────────────────────────┐
"r" "re" "rea"   │     Autocomplete Service Layer           │
───────────────► │                                         │
                 │  ┌───────────────┐  ┌────────────────┐  │
                 │  │ Trie Service   │  │  Redis Cache   │  │
                 │  │ (in-memory)    │  │  (hot queries) │  │
                 │  └───────────────┘  └────────────────┘  │
                 └──────────────────────────┬──────────────┘
                                            │ (cache miss)
                                     ┌──────▼──────────┐
                                     │  Suggestion DB   │
                                     │  (prefix table)  │
                                     └──────────────────┘

                 ┌──────────────────────────────────────────┐
                 │         Data Pipeline (Offline)           │
                 │                                          │
  Query Logs ──► │ Kafka → Spark/Flink aggregation          │
                 │       → Frequency computation (hourly)   │
                 │       → Trie rebuild                      │
                 │       → Push to Suggestion DB + Trie Svc │
                 └──────────────────────────────────────────┘
```

---

## Step 5: Trie Data Structure — Core Component

### Basic Trie for Autocomplete
```
Queries in trie:
  "react tutorial" (score: 9842)
  "react hooks" (score: 8761)
  "react native" (score: 7234)

Trie structure:
  root
    r
      e
        a
          c
            t (→ store top-5 completions here)
              ' '
                t ... (tutorial)
                h ... (hooks)
                n ... (native)

For prefix "rea": traverse r→e→a, return top-5 stored at "rea" node
```

### Optimized Trie: Pre-store Top-K at Each Node
```
Key insight: At each trie node, pre-compute and store top-K completions.

Node "rea":
  topK = [
    ("react tutorial", 9842),
    ("react hooks", 8761),
    ("react native", 7234),
    ("react router", 5123),
    ("react vs vue", 4567)
  ]

Query for "rea" → directly return pre-stored topK → O(1) after traversal
No need to traverse all children!

Memory: 10M unique prefix nodes × 5 completions × 30 bytes = ~1.5GB (fits in RAM)

Build: offline Spark job, rebuild every 30 min, atomic swap
```

### Compressed Trie (Patricia Trie / Radix Tree)
```
Compress single-child chains:
  r → e → a → c → t  becomes  "react" → node

Reduces memory by 10-50× for typical word sets
Used by: Linux kernel routing tables, HTTP routing frameworks

In practice: Use sorted prefix table in Redis rather than full trie implementation
```

---

## Step 6: Redis-Based Implementation (Production Approach)

### Sorted Set Prefix Trick
```
Instead of coding a trie from scratch, use Redis Sorted Sets:

Key: suggestion:prefix:{prefix}
Members: completion strings
Scores: query frequency

ZADD suggestion:prefix:rea 9842 "react tutorial"
ZADD suggestion:prefix:rea 8761 "react hooks"
ZADD suggestion:prefix:rea 7234 "react native"

Query: ZREVRANGE suggestion:prefix:rea 0 4 WITHSCORES
Returns top 5 by score in O(log N) 

For all N-character prefixes of a query, create separate sorted set
Storage: 10M queries × avg 10 prefixes × 30 bytes = ~3GB in Redis
Query latency: < 2ms with Redis pipeline

Limitation: fixed prefixes — no fuzzy matching
```

### Caching Strategy
```
Two-tier cache:
  L1: In-process LRU cache in each Autocomplete node (< 1ms)
      Cache top 1M most common prefixes (covers ~95% of traffic)
  L2: Redis shared across all nodes (< 2ms)
      Cache all active prefixes with recent queries

CDN caching: Can cache common queries at edge!
  "re", "rea", "reac", "react" — highly popular, same response for all users
  TTL: 30 seconds (balance freshness vs cache hit rate)
  Vary: don't cache personalized responses at CDN
```

---

## Step 7: Data Pipeline — Keeping Suggestions Fresh

```
Query Log Collection:
  User submits search → publish to Kafka: search.queries
    {userId, query, timestamp, sessionId, clickedResult}

Aggregation (Flink streaming):
  Sliding window: count queries per phrase per hour
  Trending detection: compare current hour vs previous week same hour
  Filter: remove PII, adult content, spam queries

Frequency Scoring:
  score = frequency_last_24h × 0.5 + frequency_last_7d × 0.3 + frequency_total × 0.2
  Boost: trending queries get +50% score boost
  Boost: seasonal (e.g., "christmas" in December)

Trie/Index Update:
  Every 30 minutes: compute new top-K per prefix
  Blue-green swap: build new trie, atomically swap with old
  Zero downtime: old trie serves while new one is built

Frequency calculation in Spark:
  GROUP BY query, WINDOW(last_24h)
  COUNT(*) as frequency
  JOIN with previous period to detect trending
  Filter frequency < 10 (too rare to suggest)
  SELECT TOP 10M by frequency
```

---

## Step 8: Personalization

```
For logged-in users: blend personal history + global popularity
  personal_score = user_query_frequency × 2 (boost personal history)
  global_score = query_popularity
  final_score = personal_score + global_score

Storage: per-user query history
  Redis Hash: HSET user:queries:{userId} "react hooks" timestamp
  Keep last 100 queries per user
  On autocomplete: fetch user queries matching prefix + merge with global

A/B testing:
  Test different blending weights
  Measure: did user select a suggestion? did they modify the suggestion?
  Optimize for suggestion acceptance rate

Cold start (new user):
  No history → pure global popularity ranking
  After 5+ queries: start personalizing
```

---

## Step 9: Scaling to Google Scale

```
100K QPS for autocomplete:
  Each server handles 10K QPS → need 10 servers
  Each server holds full trie in memory (~3GB) → fine with 16GB RAM instances

Geographic distribution:
  Deploy trie servers in each region (US, EU, APAC, India, etc.)
  Region-specific trending (Indian users see Indian trends)
  CDN at edge for ultra-common prefixes

Multi-language:
  Separate trie per language (en, es, zh, hi, etc.)
  Language detection from Accept-Language header / user profile
  
Handling typos:
  Simple: suggest despite 1 character difference
  Use: SymSpell algorithm (very fast fuzzy matching)
  OR: n-gram indexing (index all 2-3 char substrings)
  Trade-off: higher memory vs better recall
```

---

## Interview Q&A

**Q: How do you update the trie when a new trending topic emerges?**
A: Query events flow to Kafka in real-time. Flink processes a 30-minute sliding window. When frequency of a new phrase exceeds threshold and shows rapid growth, it gets promoted to the suggestion trie. Trie rebuilt every 30 minutes with atomic swap. For breaking news (e.g., "earthquake"), a faster 5-minute pipeline with lower threshold can accelerate promotion.

**Q: Why not just use Elasticsearch for autocomplete instead of a custom trie?**
A: Elasticsearch has a completion suggester (Trie-based) built in — absolutely valid approach. For Google-scale, a custom in-memory trie is faster (< 1ms vs 5-10ms for ES). For most companies, Elasticsearch completion suggester is the pragmatic choice. Google uses custom systems because they have 100K QPS and need personalization + multi-language + typo correction all integrated.

**Q: How do you prevent offensive or inappropriate suggestions?**
A: Block list of banned words/phrases. Crowd-reporting: users flag inappropriate suggestions. ML content classifier as post-filter before serving suggestions. Human review queue for borderline cases. Temporary suppression of specific phrases during sensitive events (e.g., "plane crash" suggestions during a real crash).

**Q: How does the system handle "react" vs "React" (case sensitivity)?**
A: Store all queries lowercase in the trie. Normalize input to lowercase before lookup. For display, use the most common capitalization from the query log (could be "React" or "react" based on what users actually typed). Language-specific: some languages are case-sensitive for meaning.

**Q: How would you add "search-as-you-type" for product search on Amazon?**
A: Same trie approach but scope to product names + categories. Product-specific scoring: inventory status, price competitiveness, conversion rate, rating. Personalization: user's browsing/purchase history. Category filtering: "laptop cha" → "laptop charger" (not "laptop charge cable" from different category). Edge CDN for common product searches.
