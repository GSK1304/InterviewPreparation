# 📚 System Design — Recommendation System (Netflix / Amazon / Spotify)

---

## 🎯 Problem Statement
Design a recommendation system that suggests relevant items (movies, products, songs) to users based on their history, preferences, and behavior of similar users.

---

## Step 1: Clarify Requirements

### Functional
- Personalized homepage recommendations ("Recommended for you")
- "Because you watched/bought X" — item-based recommendations
- "Users like you also liked" — collaborative filtering
- Real-time update: new interaction → recommendations update within minutes
- Cold start: handle new users with no history
- Diversity: avoid always recommending same type of content
- Explain recommendations ("Because you watched Inception")

### Non-Functional
- **Scale**: 100M users, 10M items (Netflix: 15K titles, Amazon: 400M products)
- **Latency**: Homepage recommendations < 100ms
- **Freshness**: Incorporate user's latest action within 5-10 minutes
- **Availability**: 99.99%
- **Offline accuracy**: Optimize for click-through rate and watch/purchase rate

---

## Step 2: Estimation

```
Users:         100M users × 10 interactions/day = 1B events/day = 11.5K EPS
Recommendations: 100M DAU × 2 page loads = 200M rec requests/day = 2,314 QPS peak ~7K

Pre-computation:
  100M users × 20 recommendations × 8 bytes = 16GB (fits in Redis!)
  Store top-20 recommendations per user → serve in O(1)

Model training:
  1B events/day → retrain model daily or incrementally
  Model size: 100M users × 128-dim embedding = ~51GB (GPU memory intensive)
```

---

## Step 3: System Architecture — Two Phases

Recommendation is a two-phase problem: **Candidate Generation** (find ~100 relevant items) then **Ranking** (score and sort those 100).

```
                        User Request
                             │
                    ┌────────▼────────┐
                    │  Serving Layer  │
                    │  (< 100ms SLA)  │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
   ┌────────▼──────┐ ┌───────▼──────┐ ┌──────▼────────┐
   │  Pre-computed │ │  Real-time   │ │  Popularity   │
   │  Recs Cache   │ │  User State  │ │  Fallback     │
   │  (Redis)      │ │  (Redis)     │ │  (trending)   │
   └───────────────┘ └──────────────┘ └───────────────┘
            │
   ┌────────▼────────────────────────────────────────┐
   │               Offline ML Pipeline               │
   │                                                 │
   │  Events → Feature Store → Model Training       │
   │                         → Candidate Generation │
   │                         → Ranking              │
   │                         → Write to Cache       │
   └─────────────────────────────────────────────────┘
```

---

## Step 4: Candidate Generation

Finding a small set (~100-500) of potentially relevant items from millions.

### Method 1: Collaborative Filtering (User-Based)
```
"Users who liked what you liked, also liked X"

Matrix Factorization:
  User-Item matrix: rows=users, cols=items, value=rating/interaction
  
  [100M users × 10M items] matrix — too large to compute directly
  
  Decompose: R ≈ U × Vᵀ
    U: 100M × 128 matrix (user embeddings)
    V: 10M × 128 matrix (item embeddings)
  
  For user u: recommended items = top-K by dot product u · vᵢ for all items i
  
  ALS (Alternating Least Squares): standard training algorithm
  Training time: hours/days on GPU cluster (distributed Spark)
  
  Embedding meaning: users close in embedding space have similar tastes
  Items close in embedding space are similar to each other
```

### Method 2: Item-Based Collaborative Filtering
```
"If you interacted with item A, you might like item B (similar users liked both)"

Similarity between items:
  item_similarity(A, B) = |users_who_liked_both| / sqrt(|users_who_liked_A| × |users_who_liked_B|)
  (Jaccard-like similarity)

Pre-compute similar items:
  For each item: top-20 most similar items → store in DB
  At serving time: user's recent items → look up their similar items → candidates

This is what Amazon's "customers who bought X also bought Y" uses.
Advantage: very fast (no matrix computation at serving time)
Disadvantage: doesn't personalize beyond what similar users did
```

### Method 3: Content-Based Filtering
```
"Because you liked drama/thriller movies, here are more drama/thrillers"

Item features: genre, director, actors, release year, duration, tags
User profile: weighted average of features of items they've interacted with

user_vector = Σ(interaction_weight × item_feature_vector) / Σ(interaction_weights)

Recommend: items with feature vector most similar to user_vector (cosine similarity)

Advantage: works for new items (no interaction history needed)
Disadvantage: only recommends what user already knows they like (no serendipity)
```

### Method 4: Embedding + Approximate Nearest Neighbor
```
Train deep learning model to produce 128-dim embeddings for users and items
  Input: user interaction sequence (BERT-style attention over item history)
  Output: user embedding vector

At serving: find items closest to user embedding vector
  Exact: O(10M items × 128 dims) per query → too slow
  ANN (Approximate Nearest Neighbor):
    Faiss (Facebook AI): HNSW graph index → O(log N) queries
    Annoy (Spotify): tree-based → fast, memory efficient
    ScaNN (Google): quantized, extremely fast on large datasets
  
  Accuracy: ANN finds 95-99% of true nearest neighbors at 100x speedup
  Query time: < 10ms for 10M items
```

---

## Step 5: Ranking

Take ~100-500 candidates from generation → score and sort → return top 10-20.

```
Ranking model: heavier ML (can afford more compute on 100 candidates)

Features:
  User features: age, country, device, time of day, recent items
  Item features: genre, popularity, recency, quality score
  Interaction features: user's avg rating for this genre, past clicks on similar
  Context features: current session length, last action, day of week

Model: Wide & Deep (Google) or Two-Tower Neural Network
  Wide part: memorization (linear model with feature crosses)
  Deep part: generalization (deep neural net)
  
Output: P(click), P(watch_full), P(purchase) → weighted combination

Training:
  Positive labels: user watched > 70% of video, purchased, rated 4+
  Negative labels: shown but not clicked, watched < 10%
  Update: online learning or daily retraining

Diversity injection:
  After ranking: ensure not all top-10 are same genre/director/artist
  MMR (Maximal Marginal Relevance): balance relevance with diversity
```

---

## Step 6: Serving Layer

```
Pre-computed serving (90% of traffic):
  Offline job (daily): compute top-20 recommendations per user
  Store: Redis HSET recs:{userId} → JSON list of itemIds + scores
  Serve: GET recs:{userId} → return in < 1ms
  
  Cache refresh: triggered by significant user activity (5+ new interactions)
  
Real-time serving (10% of traffic, for freshness):
  User's last 5 interactions → ANN query → merge with pre-computed
  Boost items similar to very recent behavior (last 30 min)
  
Serving architecture:
  Client → Recommendation Service
    → Redis: get pre-computed recs (< 1ms)
    → Redis: get user's last 5 actions (real-time signal)
    → Blend: score = 0.7 × pre-computed + 0.3 × real-time recency boost
    → Fetch item details (Redis cache by itemId)
    → Return ranked list
    Total: < 20ms
```

---

## Step 7: Cold Start Problem

```
New user (no history):
  1. Onboarding: ask 3-5 preference questions ("What genres do you like?")
     → Map to item seeds → generate initial recs from item similarity
  2. Popular content: globally trending + popular in user's region/age
  3. Implicit signals: watch a few seconds of something → update profile
  4. After 10+ interactions: full personalization kicks in

New item (no interaction history):
  1. Content-based: use item features (genre, tags, creator) to place in space
  2. Inject new items into some users' recommendations randomly (exploration)
  3. Collect initial interactions → cold item becomes warm → collaborative filter works
  
Exploration vs Exploitation (Multi-armed Bandit):
  Exploit: show what model predicts user will like (high CTR)
  Explore: occasionally show uncertain items to learn about them/user
  ε-greedy: 90% exploit, 10% explore
  UCB (Upper Confidence Bound): explore items with high uncertainty
  Thompson Sampling: Bayesian approach, widely used at scale
```

---

## Step 8: Feature Store

```
Central repository for ML features — computed once, reused by all models

Features:
  User features:
    - user_avg_rating_comedy (last 30 days)
    - user_genre_distribution (watch time per genre)
    - user_activity_level (daily/weekly/monthly active)
    - user_embedding (128-dim, updated daily)
  
  Item features:
    - item_popularity_score (views in last 7 days)
    - item_completion_rate (% of viewers who finished)
    - item_embedding (128-dim)
    - item_genre_vector (multi-hot encoding)

Feature store architecture:
  Offline (batch): Spark computes daily features → Hive/Parquet
  Online (real-time): Flink computes streaming features → Redis
  
  Unified API: feature_store.get(userId, featureNames) → values
    Reads from Redis for real-time features
    Reads from offline store for daily features
  
  Benefits:
    No feature recomputation per model (compute once, share across 10+ models)
    Consistent features between training and serving (training-serving skew prevention)
    Feature versioning and lineage tracking
```

---

## Step 9: A/B Testing & Metrics

```
How to know if new recommendation algorithm is better?

Metrics:
  Click-through rate (CTR): did user click recommendation?
  Watch/purchase rate: did user complete the action?
  Session length: did session get longer with recommendations?
  Diversity: did user interact with new genres/categories?
  Long-term retention: are users coming back more often?

A/B test setup:
  Control group (10%): old algorithm
  Treatment group (10%): new algorithm
  Holdout (80%): production algorithm
  
  Run for minimum 2 weeks (capture weekly patterns)
  Minimum detectable effect: 0.5% CTR improvement
  Statistical significance: p < 0.05 (95% confidence)
  
  Avoid novelty effect: first week of new recs looks good just because they're different
  Measure at least two weeks
```

---

## Interview Q&A

**Q: How does Netflix recommend the right thumbnail for each user?**
A: Netflix serves different thumbnails for the same title to different users — a user who watches romantic movies sees the romantic couple in the Avengers thumbnail; an action fan sees an explosion. They A/B test thumbnails continuously and use contextual bandits to pick the thumbnail most likely to get a click for each individual user's profile. The recommendation *content* and *presentation* are both personalized.

**Q: How do you prevent the "filter bubble" (always recommending the same thing)?**
A: Diversity constraints: after ranking, ensure top-10 covers multiple genres/creators/styles (MMR algorithm). Serendipity injection: occasionally recommend items slightly outside comfort zone (exploration). Freshness: boost recently released items. Long-term satisfaction metrics: track whether users get bored and churn (declining engagement signal).

**Q: How would you recommend for a user who logs in monthly (sparse interaction)?**
A: Longer lookback window (3-6 months vs 30 days). Weight interactions more heavily (a monthly user's 10 interactions are more signal-dense than a daily user's 300). Rely more on content-based filtering (stable user taste expressed through limited interactions). Fall back to popularity-based recs blended with their limited profile.

**Q: What is training-serving skew and how do you prevent it?**
A: Training-serving skew is when the features used during model training differ from those used during serving (due to different data pipelines or staleness). Prevention: use the same Feature Store for both training and serving. Log the actual feature values used at serving time → train on those logged values. Continuously monitor feature distributions — alert if serving features drift from training distribution.

**Q: How does Spotify's Discover Weekly work?**
A: Weekly batch job: (1) Build user-song interaction matrix from 30 days of streams. (2) Matrix factorization → user and song embeddings. (3) For each user, find 30 songs from similar users that the user hasn't heard. (4) Rank by predicted fit using Deep Learning ranker. (5) Write to Redis at weekend for Monday delivery. Key insight: uses "taste profiles" of other users to create a curated playlist that feels like a knowledgeable friend made it.
