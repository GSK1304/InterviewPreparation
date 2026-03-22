# 📚 AI/ML System Design — Real-Time LLM Billing & Cost Management

---

## 1. Why LLM Billing Is a Unique System Design Problem

LLM billing is harder than traditional API billing because:

```
Traditional API billing:          LLM billing:
─────────────────────────         ─────────────────────
Count API calls                   Count INPUT tokens + OUTPUT tokens separately
Fixed cost per call               Variable cost per request (depends on content length)
Response size predictable         Response size unknown upfront (generation stops unpredictably)
Billing post-request              Billing must account for streaming (tokens generated over time)
Simple metering                   Token counting needs tokeniser

Additional complexity:
  Different prices: input tokens vs output tokens vs cached tokens
  Different prices: model-to-model (GPT-4o vs GPT-4o-mini = 10× difference)
  Reasoning tokens: o1-style models generate hidden "reasoning" tokens billed separately
  Context caching: cached prefix charged at discount rate
  Multimodal: images billed by resolution/tile count, audio by seconds
  Fine-tuned models: training cost + higher per-token inference cost
  Volume discounts: enterprise rate negotiation
  Prepaid credits vs postpaid invoicing
```

---

## 2. Token Billing Model — How It Works

```
Pricing (approximate, as of 2025):
  Model                Input ($/1M tokens)   Output ($/1M tokens)
  ─────────────────    ──────────────────    ───────────────────
  GPT-4o               $2.50                 $10.00
  GPT-4o-mini          $0.15                 $0.60
  Claude Sonnet 4.5    $3.00                 $15.00
  Claude Haiku 4.5     $0.80                 $4.00
  Gemini 2.5 Pro       $1.25                 $10.00
  Llama 3.3 70B (self) ~$0.20                ~$0.20 (compute cost)

Key insight: OUTPUT tokens cost 4-10× more than INPUT tokens
  Why? Output is sequential (slow); input is parallelisable (fast)
  Design implication: long conversations with long responses are expensive
  
Cost per typical conversation:
  User message: 50 tokens input × $0.0000025 = $0.000125
  System prompt: 500 tokens × $0.0000025 = $0.00125 (cached = 50% off)
  Response: 200 tokens × $0.00001 = $0.002
  Total: ~$0.003 per turn

At scale:
  10M messages/day × $0.003 = $30,000/day = $900K/month
  Cost optimisation becomes critical
```

---

## 3. Real-Time Token Metering Architecture

### The Core Pipeline

```
LLM Call happens
     │
     ▼
┌────────────────────────────────────────────────────────────────┐
│              LLM Proxy / Gateway                                │
│  (LiteLLM, custom, or built-in to your inference service)      │
│                                                                │
│  For every request:                                            │
│    1. Record request start, model, prompt content             │
│    2. Count input tokens (tokeniser at proxy level)            │
│    3. Stream response tokens                                   │
│    4. Count output tokens as they stream                       │
│    5. Record: {userId, orgId, model, inputTokens,             │
│               outputTokens, cachedTokens, timestamp, cost}    │
└──────────────────────────────────┬─────────────────────────────┘
                                   │ fire-and-forget (async)
                          ┌────────▼────────┐
                          │   Kafka Topic    │
                          │  usage.events    │
                          └────────┬────────┘
                                   │
              ┌────────────────────┼───────────────────────────┐
              │                    │                            │
     ┌────────▼──────┐   ┌────────▼──────┐         ┌──────────▼──────┐
     │  Real-time     │   │  Billing      │         │  Analytics      │
     │  Budget Check  │   │  Aggregator   │         │  (Clickhouse)   │
     │  (Redis)       │   │  (Flink/Spark)│         │  Usage reports  │
     └────────────────┘   └────────┬──────┘         └─────────────────┘
                                   │
                          ┌────────▼────────┐
                          │   Billing DB     │
                          │  (PostgreSQL)    │
                          │  Token ledger    │
                          │  Invoice records │
                          └─────────────────┘
```

### Token Counting at the Proxy Layer

```python
# Every LLM request passes through the proxy
# Proxy counts tokens and emits usage events — non-blocking

import tiktoken  # OpenAI tokeniser
from anthropic import count_tokens  # Anthropic tokeniser

def count_tokens_for_model(model: str, text: str) -> int:
    if model.startswith("gpt"):
        enc = tiktoken.encoding_for_model(model)
        return len(enc.encode(text))
    elif model.startswith("claude"):
        return anthropic.count_tokens(text)  # API call or local estimate
    else:
        # Approximation: 1 token ≈ 4 characters
        return len(text) // 4

async def proxy_llm_call(request: LLMRequest) -> AsyncIterator[str]:
    start_time = time.time()
    input_tokens = count_tokens_for_model(request.model, build_prompt(request))
    
    output_tokens = 0
    output_buffer = ""
    
    async for chunk in call_llm_provider(request):
        token = chunk.delta.content
        output_buffer += token
        output_tokens += 1  # or use actual token count from provider
        yield token  # stream to client (fire-and-forget billing)
    
    # Emit usage event (async, does not block the response)
    asyncio.create_task(emit_usage_event(UsageEvent(
        userId=request.userId,
        orgId=request.orgId,
        model=request.model,
        inputTokens=input_tokens,
        outputTokens=output_tokens,
        cachedInputTokens=request.cached_tokens or 0,
        costUSD=calculate_cost(request.model, input_tokens, output_tokens),
        latencyMs=int((time.time() - start_time) * 1000),
        timestamp=datetime.utcnow()
    )))
```

---

## 4. Real-Time Budget Enforcement

```
Problem: A user's script goes into an infinite loop → 10,000 API calls → $1,000 bill

Solution: Real-time budget check before every request

Redis counters (atomic, sub-millisecond):
  INCRBYFLOAT budget:used:{userId}:{month}   costUSD
  GET budget:limit:{userId}                  → monthly limit

Per-request budget check:
  Before calling LLM:
    current_spend = GET budget:used:{userId}:{current_month}
    budget_limit  = GET budget:limit:{userId}
    
    if current_spend + estimated_cost > budget_limit:
        raise BudgetExceededException(
            f"Monthly budget ${budget_limit} exceeded. "
            f"Current spend: ${current_spend}. "
            f"Upgrade plan or wait until next month."
        )
    
  After LLM call:
    INCRBYFLOAT budget:used:{userId}:{current_month}  actual_cost

Estimated cost (before call):
  Hard to know output tokens upfront → use conservative estimate
  input_cost = input_tokens × model.input_price_per_token
  estimated_output = max_tokens × model.output_price_per_token  # worst case
  estimated_total = input_cost + estimated_output

Alert tiers:
  50% of budget consumed → warning email
  80% of budget consumed → warning + Slack
  95% of budget consumed → alert + optional auto-throttle
  100% → block requests + notification

Budget types:
  Per user monthly limit
  Per API key limit (useful for dev keys)
  Per feature/product (how much does the "summary" feature cost?)
  Per organisation with per-team sub-limits
```

---

## 5. Usage Attribution (Who Is Costing What?)

```
Problem: You're spending $50K/month on AI. Which team? Which feature? Which user?

Tag every LLM call with metadata:
  {
    "userId": "user-123",
    "orgId": "org-456",
    "feature": "document-summary",  ← which product feature triggered this
    "environment": "production",
    "model": "claude-sonnet-4-5",
    "tags": ["customer-support", "tier-enterprise"]
  }

Store in Clickhouse (columnar, fast analytics):
  usage_events (
    timestamp         DateTime,
    userId            String,
    orgId             String,
    feature           String,
    model             String,
    inputTokens       Int64,
    outputTokens      Int64,
    costUSD           Float64,
    latencyMs         Int32,
    environment       String
  )
  ORDER BY (orgId, timestamp)  -- fast queries by org over time

Dashboard queries:
  -- Cost by feature this month
  SELECT feature, SUM(costUSD) as cost
  FROM usage_events
  WHERE timestamp > subtractMonths(now(), 1)
  GROUP BY feature ORDER BY cost DESC
  
  -- Most expensive users
  SELECT userId, SUM(costUSD) as cost, SUM(outputTokens) as tokens
  FROM usage_events
  WHERE timestamp > subtractDays(now(), 7)
  GROUP BY userId ORDER BY cost DESC LIMIT 20
  
  -- Cost per model over time
  SELECT toDate(timestamp) as date, model, SUM(costUSD) as daily_cost
  FROM usage_events
  GROUP BY date, model ORDER BY date

Showback vs Chargeback:
  Showback:  Show teams how much they're spending (visibility without consequence)
  Chargeback: Teams have internal budgets; cross-charge for overruns
  Start with showback → adds visibility → teams self-regulate → switch to chargeback
```

---

## 6. Billing Tiers and Credit Systems

```
Pay-as-you-go (postpaid):
  Bill at end of month based on actual usage
  User gets invoice: "Used 5M tokens (Claude Sonnet) = $15.00"
  Risk: user may dispute bill; fraud possible
  Advantage: no barrier to entry, no upfront commitment

Prepaid credits:
  User buys $100 in credits upfront
  Each API call deducts from balance
  If balance = 0: requests blocked until top-up
  
  DB tables:
    credit_balances (userId, balanceUSD, updatedAt)
    credit_transactions (userId, amount, type, createdAt)
      type: 'purchase', 'usage', 'refund', 'bonus'
  
  Benefits: no bad debt, Stripe charges upfront, user controls spend
  ASC 606 accounting: credits are "deferred revenue" until consumed
  Expiry: credits may expire (motivates usage), must disclose clearly

Subscription plans:
  Starter: $20/month → 2M tokens included
  Pro: $100/month → 15M tokens included (better rate)
  Enterprise: custom pricing, committed use discount
  
  Overage: charge at standard rate when included tokens exhausted
  OR: soft limit (degrade to slower/cheaper model when quota exceeded)

Volume discounts (enterprise):
  Commit to $10K+/month → 20% discount
  Auto-apply based on rolling 30-day spend
  Manual negotiation for $50K+/month contracts
```

---

## 7. Fraud Detection and Abuse Prevention

```
Common abuse patterns:
  1. API key leaked → bots run thousands of requests
  2. Customer creates hundreds of accounts to exploit free tier
  3. Scripted benchmark attacks consuming quota
  4. Prompt injection causing unexpectedly long outputs

Detection rules (Redis-based velocity checks):
  - More than 100 requests in last 60 seconds → rate limit
  - More than 10,000 output tokens in last 5 minutes → alert
  - Cost spike: current hour cost > 5× hourly average → block + alert
  - New account with > $10 spend in first hour → manual review

Account-level anomaly detection:
  Baseline: rolling 7-day average spend per user
  Alert: if today's spend > 3× daily average before noon
  
  Slack alert:
    "🚨 Anomaly: user-789 spent $45 in last hour (7-day avg: $8/day)"

Key leak response:
  User reports leaked key → immediately revoke key + issue new one
  Retroactive: waive charges from suspected leaked key period
  Prevention: never log API keys, use short-lived tokens when possible

Free tier abuse:
  Email verification required
  Phone number for free tier (harder to create many accounts)
  Free tier limited to: 1M tokens/month, no GPT-4/Claude Opus
  Credit card required to upgrade (identity verification)
```

---

## 8. Invoice Generation and Revenue Recognition

```
Invoice generation (monthly):
  Batch job runs on 1st of each month:
    1. Query Clickhouse: aggregate usage by org/user for prior month
    2. Apply pricing tiers and discounts
    3. Subtract prepaid credits used
    4. Generate invoice PDF
    5. Charge credit card via Stripe
    6. Send invoice email

Revenue recognition (ASC 606):
  For usage-based: recognise revenue AS usage occurs
    Each token used = $X of revenue recognised immediately
    NOT when cash is received (if prepaid)
  
  For subscriptions: recognise ratably over subscription period
    $100/month plan: $100/30 = $3.33/day recognised
    
  For prepaid credits:
    Buying credits → Deferred Revenue (liability on balance sheet)
    Using credits  → Revenue (realised)
    Unused expiring credits → Breakage Revenue (estimate upfront)

Dunning (failed payment recovery):
  Day 0:  Charge fails → retry in 3 days
  Day 3:  Retry → if fails, send warning email
  Day 7:  Retry → if fails, downgrade to free tier (service degraded)
  Day 14: Final notice → account suspension
  Day 30: Account closed, data deletion scheduled
```

---

## 9. Cost Optimisation Strategies (Summary)

```
Strategy                   Savings Potential    Complexity
────────────────────────── ──────────────────── ──────────
Model routing              40-70%               Medium
Prompt caching             20-50%               Low
Semantic response cache    20-40% (for chatbots) Medium
Batch API (non-real-time)  50%                  Low
Reduce max_tokens          10-30%               Low
Smaller model fine-tuning  60-80%               High
Quantised self-hosted      60-80% vs API        High (ops)
Compress/trim context      10-20%               Medium
Output format optimisation 5-15%                Low

Practical roadmap:
  Week 1: Enable prompt caching (low effort, immediate saving)
  Week 2: Add model routing (route 70% of queries to cheaper model)
  Month 1: Implement semantic cache (high ROI for repetitive queries)
  Month 2: Evaluate self-hosted for high-volume workloads
  Quarter 1: Fine-tune smaller model for specific high-volume tasks
```

---

## Interview Q&A

**Q: How do you design a billing system that handles streaming responses (tokens arrive over time)?**
A: Don't try to bill during streaming — it adds latency to every token. Instead: (1) Estimate cost before the request for budget enforcement (use max_tokens as worst case). (2) After streaming completes, count actual output tokens from the completed response (or use the usage field from the API response). (3) Emit a single usage event asynchronously after stream completion. This means billing is slightly delayed (by the duration of the response), which is acceptable — we use the pre-call estimate for real-time budget checks and the actual count for billing records.

**Q: What happens when a provider API call fails mid-stream — do you charge the user?**
A: Only charge for tokens actually received. Track output_tokens_received during streaming. If stream fails at token 50 out of expected 200, emit a usage event for 50 output tokens only. The user got partial output — don't charge them for tokens they never received. This requires careful stream accounting at the proxy layer and idempotent usage events (use requestId as idempotency key to prevent double-billing on retry).

**Q: How would you implement per-feature cost attribution in a product with 20 AI features?**
A: Require a `feature` tag on every LLM call — enforce this at the LLM client library level (throw exception if tag missing). Store in Clickhouse with feature as a dimension. Build a cost allocation dashboard showing spend by feature over time. Set per-feature budgets in Redis — if the "document summary" feature exceeds its daily budget, it falls back to a cheaper model or returns an error. This makes AI costs visible to product teams who own each feature, creating accountability.

**Q: How do you handle a customer who disputes their $5,000 bill?**
A: First: have complete audit trail — every request logged with timestamp, userId, model, prompt hash (not content for privacy), input/output token count. Show the customer: request log grouped by hour, top 10 most expensive requests, daily spend chart. Second: investigate anomalies — was there an API key leak? Did their application enter a loop? Third: policy — if breach was due to our billing bug or ambiguous documentation, credit appropriately. If clear customer error: partial goodwill credit (20-50%) as relationship investment. Prevention: always show real-time spend dashboard + proactive spend alerts.

**Q: What's the difference between showing approximate vs exact token counts in your billing dashboard?**
A: Exact: count using the actual tokeniser for each model (tiktoken for GPT, Anthropic's counter for Claude). Required for accurate billing reconciliation with provider invoices. Approximate (chars ÷ 4): fine for real-time budget checks at the proxy layer (fast, no tokeniser overhead) but should not be used for final billing. The delta is typically < 5% which matters at scale — for $1M/month spend that's $50K/month discrepancy. Always reconcile against provider invoice monthly and adjust if systematic drift.
