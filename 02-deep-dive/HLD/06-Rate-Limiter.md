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
