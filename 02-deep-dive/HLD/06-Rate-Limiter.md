# 📚 HLD Deep Dive — Rate Limiter

---

## 🎯 Problem Statement
Design a rate limiter that restricts the number of requests a client can make to an API within a time window, protecting backend services from abuse, DDoS, and ensuring fair usage.

---

## Step 1: Clarify Requirements

### Functional
- Limit requests per user/IP/API key within a time window
- Support different limits per endpoint (e.g., login = 5/min, search = 100/sec)
- Return 429 Too Many Requests with Retry-After header when limit exceeded
- Multiple rate limiting strategies (token bucket, sliding window, etc.)
- Distributed — works across multiple API gateway instances

### Non-Functional
- **Latency**: < 5ms overhead per request (rate limit check must be fast)
- **Availability**: Rate limiter failure should fail-open (don't block legitimate traffic)
- **Accuracy**: Best-effort — small over-allowance acceptable
- **Scale**: 1M requests/sec across thousands of users

---

## Step 2: Where to Implement

```
Option 1: Client-side
  - Not reliable (clients can bypass)
  - Use only as courtesy to avoid accidental abuse

Option 2: API Gateway (recommended)
  - Centralized enforcement before any service sees request
  - Use Redis for distributed counting
  - kACE: Spring Cloud Gateway + Redis rate limiter

Option 3: Per-service middleware
  - Flexible, service-specific rules
  - Harder to coordinate global limits

✅ Recommended: API Gateway + Redis
```

---

## Step 3: Rate Limiting Algorithms

### Algorithm 1: Token Bucket
```
Concept:
  - Bucket has max capacity C tokens
  - Tokens refill at rate R per second
  - Each request consumes 1 token
  - Request rejected if bucket empty

Properties:
  + Allows bursts (up to bucket capacity)
  + Smooth average rate enforcement
  - State per user: {tokens_remaining, last_refill_time}

Implementation:
  tokens = min(capacity, stored_tokens + (now - last_refill) × rate)
  if tokens >= 1: tokens -= 1; allow; else: reject

Example: C=10, R=2/sec
  Burst of 10 requests → all allowed (consume from bucket)
  Next 10 requests in 1 second → 2 allowed (2 tokens refilled), 8 rejected
```

### Algorithm 2: Leaky Bucket
```
Concept:
  - Requests enter a FIFO queue
  - Queue drains at fixed rate R
  - Requests rejected if queue full

Properties:
  + Smooth, consistent output rate
  + Good for downstream rate control
  - Bursts queued/dropped, not served immediately
  - Higher latency during bursts

Use: Networks, traffic shaping, smoothing write load
```

### Algorithm 3: Fixed Window Counter
```
Concept:
  - Count requests in fixed time windows (e.g., per minute)
  - Reject if count > limit
  - Counter resets at window boundary

Implementation:
  key = "rl:{userId}:{window_start}"  // window_start = floor(now/60)*60
  INCR key
  EXPIRE key 60
  if count > limit: reject

Problem: Edge burst
  Limit: 100/min
  User sends 100 at 00:59 and 100 at 01:00 → 200 requests in 2 seconds!

Advantage: Very simple, O(1) Redis operations
```

### Algorithm 4: Sliding Window Log
```
Concept:
  - Store timestamp of each request in a sorted set
  - Count requests in [now - window, now]
  - Reject if count > limit

Implementation (Redis Sorted Set):
  ZREMRANGEBYSCORE key 0 (now - windowMs)  // remove old requests
  ZADD key now requestId                    // add current request
  count = ZCARD key
  EXPIRE key windowMs
  if count > limit: reject

Properties:
  + Very accurate (no edge burst problem)
  - High memory (stores all timestamps)
  - Higher Redis operations
  Use: When accuracy is critical (payment APIs, login)
```

### Algorithm 5: Sliding Window Counter ✅ Best for Most Cases
```
Concept:
  - Blend of fixed window and sliding log
  - Use two fixed windows + weighted interpolation

Formula:
  count = current_window_count +
          previous_window_count × (1 - elapsed_in_current_window / window_size)

Example: Limit 100/min, window=60s, current time = 00:45
  previous window (00:00-00:59): 80 requests
  current window (01:00-01:59): 30 requests
  elapsed = 45s (75% through current window)
  estimated count = 30 + 80 × (1 - 0.75) = 30 + 20 = 50 → ALLOW

Properties:
  + Accurate (no edge burst)
  + Low memory (only 2 counters)
  + Fast (2 Redis reads + 1 write)
  ± Approximate (not exact), but within 0.003% error rate
```

---

## Step 4: Distributed Rate Limiter Architecture

```
                ┌──────────────────────────────────┐
                │          API Gateway             │
                │  Instance 1 | Instance 2 | ...   │
                └──────────────────┬───────────────┘
                                   │ Redis calls (< 1ms)
                ┌──────────────────▼───────────────┐
                │         Redis Cluster             │
                │  (rate limit counters per user)   │
                │  Key: rl:{userId}:{endpoint}:{window}
                └───────────────────────────────────┘
```

### Lua Script for Atomic Counter (prevents race conditions)
```lua
-- Redis Lua script for sliding window counter (atomic)
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local prev_key = KEYS[2]

local curr_count = tonumber(redis.call('GET', key) or 0)
local prev_count = tonumber(redis.call('GET', prev_key) or 0)
local elapsed = now % window
local weight = 1 - (elapsed / window)
local estimated = curr_count + math.floor(prev_count * weight)

if estimated >= limit then
    return 0  -- reject
else
    redis.call('INCR', key)
    redis.call('EXPIRE', key, window * 2)
    return 1  -- allow
end
-- Lua script executes atomically — no race condition
```

---

## Step 5: Response Headers

```
Every rate-limited response should include:
  X-RateLimit-Limit: 100           // max requests per window
  X-RateLimit-Remaining: 45        // remaining requests
  X-RateLimit-Reset: 1700001000    // Unix timestamp when window resets

On 429 Too Many Requests:
  Retry-After: 30                  // seconds until retry allowed
  X-RateLimit-Remaining: 0
```

---

## Step 6: Multi-Tier Rate Limiting

```
Tier 1 — Per IP (prevent DDoS):
  100 requests/sec per IP
  Implemented at: CDN / API Gateway

Tier 2 — Per User (authenticated):
  1000 requests/min per user
  Implemented at: API Gateway (after JWT validation)

Tier 3 — Per Endpoint:
  POST /login: 5 requests/min per IP (brute force protection)
  GET /prices: 10,000 requests/min per user
  POST /rfq: 100 requests/min per user

Tier 4 — Per API Key (for external integrations):
  Free tier: 1,000 requests/day
  Pro tier: 100,000 requests/day
  Enterprise: Unlimited
```

---

## Step 7: Handling Edge Cases

### Rate Limiter is Down (Fail-Open vs Fail-Closed)
```
Fail-Open (recommended for most services):
  - If Redis is unavailable, allow requests
  - Reasoning: Downtime is rare; blocking all traffic costs more than abuse
  - Log the incident; alert on-call

Fail-Closed (for sensitive endpoints):
  - If Redis is unavailable, reject requests
  - Use: payment processing, authentication, privileged operations

Circuit Breaker for Rate Limiter:
  - If Redis latency > 10ms for 5 consecutive calls → switch to in-memory fallback
  - In-memory per-instance rate limiting (approximate, not distributed)
  - Alert and auto-recover when Redis returns to normal
```

### Distributed System Race Condition
```
Problem: Two API gateway instances both check counter simultaneously
  Instance 1: reads count=99 (limit=100) → allow, write 100
  Instance 2: reads count=99 (limit=100) → allow, write 100
  Result: 101 requests allowed → over limit

Solution: Lua script (atomic read-increment-check in Redis)
  OR: Redis INCR + check (INCR is atomic; check after)
```

---

## Step 8: Real-World Application (kACE)

```java
// Spring Cloud Gateway rate limiter for kACE API
@Bean
public RouteLocator routes(RouteLocatorBuilder builder, RedisRateLimiter rateLimiter) {
    return builder.routes()
        .route("rfq-service", r -> r.path("/api/rfq/**")
            .filters(f -> f.requestRateLimiter(c -> c
                .setRateLimiter(rateLimiter)
                .setKeyResolver(exchange ->
                    // Rate limit per user, not per IP
                    Mono.just(exchange.getRequest().getHeaders()
                        .getFirst("X-User-Id")))
            ))
            .uri("lb://rfq-service"))
        .build();
}

// Redis rate limiter config (token bucket)
@Bean
public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(
        100,   // replenishRate: tokens added per second
        200,   // burstCapacity: max burst size
        1      // requestedTokens: tokens per request
    );
}
```

---

## Step 9: Trade-offs

| Decision | Choice | Reason |
|----------|--------|--------|
| Algorithm | Sliding Window Counter | Accurate + memory efficient + fast |
| Storage | Redis | Sub-millisecond ops; TTL support; Lua scripts |
| Placement | API Gateway | Centralized; before business logic |
| Failure | Fail-open | Availability > strict enforcement |
| Granularity | Per-user + per-endpoint | Balance fairness and protection |
| Accuracy | Approximate (0.003% error) | Performance > perfect accuracy |

---

## Interview Q&A

**Q: How do you rate limit a distributed system where a user's requests can hit any of 100 servers?**
A: You need a shared counter store. The standard approach: Redis with atomic INCR and EXPIRE. Every server, regardless of which one the request hits, increments the same Redis key (`ratelimit:{userId}:{window}`). Redis's single-threaded command execution guarantees atomicity — no race conditions between servers. The Lua script approach (INCR + check in one atomic operation) eliminates the check-then-increment race. For very high throughput (> 100K requests/sec across all users), use Redis Cluster to shard the rate limit keys horizontally. Local rate limiting per server is faster but allows 100× overuse (one user saturates all 100 servers at the per-server limit).

**Q: What's wrong with using a database instead of Redis for rate limiting?**
A: Databases are too slow. Rate limiting must add < 2ms to request latency. A PostgreSQL query with an UPDATE and SELECT takes 5-20ms per request — that doubles your API latency. Additionally, DB connection pool exhaustion under high load would cause cascading failures. Redis: single-threaded, in-memory, < 0.5ms for INCR operations, no connection overhead with persistent connections, built-in TTL for key expiry. The only time a DB is acceptable: low-volume rate limiting (< 100 requests/sec total) where latency isn't critical, or for coarse daily/monthly quotas where a few ms doesn't matter.

**Q: How would you rate limit by both IP and user ID simultaneously?**
A: Apply multiple rate limiters in sequence, fail fast if any one is exceeded. Two Redis keys: `ratelimit:ip:{ip}:{window}` and `ratelimit:user:{userId}:{window}`. Check IP limit first (before authentication — prevents even reaching auth code). Then check user limit after authentication. Different thresholds: IP limit = 1000 req/min (prevents single-IP abuse), user limit = 500 req/min (per authenticated user). Unauthenticated requests: only IP-limited. Authenticated requests: both limits apply — a user behind a corporate NAT (many users, one IP) won't be unfairly blocked. Return the most specific 429 with the correct `Retry-After`.

**Q: Should rate limiting be at the API Gateway or inside each microservice?**
A: Both, for different purposes. API Gateway rate limiting: (1) protects the entire platform from external abuse before any service is reached, (2) enforces per-user/per-API-key global limits, (3) prevents DDoS. Per-service rate limiting: (1) protects a specific service from internal overload (Service A calls Service B 10K times/sec during a bug), (2) enforces per-endpoint or per-resource limits (e.g., max 5 payment attempts per card per hour — business rule, not infrastructure). Gateway handles external; services handle internal and business logic. They complement each other.

**Q: How would you handle rate limiting for a batch API where one request contains 1000 items?**
A: Count by "cost units" rather than request count. Define: a batch request of N items costs N units (or some function of N). Rate limit: 10,000 units/min, not 100 requests/min. Extract item count from request body before processing, increment Redis counter by count (not by 1). Return `X-RateLimit-Remaining: 7500` (units remaining). This prevents abuse via batching: `10,000 requests × 1 item` and `10 requests × 1000 items` are equivalent cost. OpenAI uses token-based rate limiting (exactly this concept — rate limit is per tokens consumed, not per API call).
