# 📚 System Design — Stock Exchange / Order Book

---

## 🎯 Problem Statement
Design a stock exchange matching engine that accepts buy and sell orders, matches them at the best available price, executes trades, and distributes market data to subscribers in real-time.

---

## Step 1: Clarify Requirements

### Functional
- Accept limit and market orders (buy/sell)
- Match orders using price-time priority (best price first, then earliest submitted)
- Execute trades when buy and sell prices match
- Maintain order book (sorted list of open bids and asks per symbol)
- Market data feed: real-time price, volume, order book depth
- Order management: cancel/modify open orders
- Account management: balance, positions, order history
- Risk checks: margin, position limits, daily loss limits

### Non-Functional
- **Latency**: Order acknowledgment < 1ms (HFT: microsecond range)
- **Throughput**: 1M orders/sec (NASDAQ handles ~10M orders/sec)
- **Availability**: 99.999% during market hours (9:15am-3:30pm IST)
- **Consistency**: Absolute — no duplicate trades, no lost orders
- **Determinism**: Same order of inputs must always produce same trades

---

## Step 2: Estimation

```
Orders:       1M orders/sec peak
Trades:       ~10-20% of orders result in trades = 100-200K trades/sec
Market data:  Every trade → price update → broadcast to subscribers
              1M updates/sec × 100 subscribers = 100M messages/sec

Order book:   Per symbol: ~1000 price levels × 2 (bid+ask) × ~50 bytes = 100KB
              10,000 symbols: 1GB order book (fits in RAM)

Latency:
  HFT target: 10-100 microseconds (co-location with exchange)
  Retail target: 1-10ms (via broker API)
  
Order size:   ~200 bytes
Storage/day:  1M orders/sec × 86400 × 200 bytes = ~17TB/day (compressed)
```

---

## Step 3: API Design

```
# Order placement
POST /v1/orders
Body: {
  symbol: "RELIANCE",
  side: "BUY" | "SELL",
  type: "LIMIT" | "MARKET" | "STOP",
  quantity: 100,
  price: 2500.00,        (for LIMIT orders)
  time_in_force: "GTC" | "IOC" | "FOK" | "DAY"
}
Response: { orderId, status: "NEW" | "REJECTED", timestamp }

# Cancel order
DELETE /v1/orders/{orderId}
Idempotency-Key: {uuid}

# Order status
GET /v1/orders/{orderId}
Response: { orderId, status, filled_qty, avg_price, remaining_qty }

# Market data (WebSocket)
WS /v1/market-data/subscribe?symbols=RELIANCE,TCS,INFY
Events:
  → TRADE: { symbol, price, quantity, timestamp, tradeId }
  → QUOTE: { symbol, bid, ask, bid_size, ask_size }
  → ORDER_BOOK_UPDATE: { symbol, side, price, quantity_delta }

# Account
GET /v1/accounts/{id}/positions
GET /v1/accounts/{id}/orders?status=OPEN
```

---

## Step 4: High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│               Order Entry Gateway                            │
│  TLS termination, auth, basic validation, rate limiting      │
│  UDP/TCP multicast for co-located HFT clients                │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│                    Risk Engine (pre-trade)                    │
│  - Account balance check (can they afford this order?)       │
│  - Position limit check (max shares per symbol)              │
│  - Daily loss limit check                                    │
│  - Margin requirement validation                             │
│  All checks: < 50 microseconds                               │
└──────────────────────────┬───────────────────────────────────┘
                           │ Order approved
┌──────────────────────────▼───────────────────────────────────┐
│              Matching Engine (per symbol, single-threaded)    │
│  - Receive order                                             │
│  - Find matching counterparty in order book                  │
│  - Generate trade(s)                                         │
│  - Update order book                                         │
│  - Publish trade events                                      │
│  Single-threaded per symbol → deterministic, no locking      │
└──────┬───────────────────────────────────────┬───────────────┘
       │                                        │
┌──────▼──────────┐                   ┌────────▼──────────────┐
│  Trade Engine   │                   │  Market Data Server   │
│  - Settlement   │                   │  - Broadcast prices   │
│  - Position     │                   │  - Order book depth   │
│    update       │                   │  - Real-time feed     │
│  - Reporting    │                   │  - UDP multicast      │
└──────┬──────────┘                   └───────────────────────┘
       │
┌──────▼─────────────────────────────────────────────────────┐
│  Order DB (append-only log)  +  Clearing & Settlement DB    │
│  Trade DB (Cassandra — time-series, write-heavy)            │
└────────────────────────────────────────────────────────────┘
```

---

## Step 5: Order Book — Core Data Structure

```
Order book: sorted collection of buy and sell orders for a symbol

BID (Buy) side — sorted by PRICE DESCENDING (best buyer pays most):
  Price  | Quantity | Orders
  2502   | 500      | [order1: 200, order2: 300]
  2501   | 1200     | [order3: 500, order4: 400, order5: 300]
  2500   | 800      | [order6: 800]

ASK (Sell) side — sorted by PRICE ASCENDING (best seller wants least):
  Price  | Quantity | Orders
  2503   | 300      | [order7: 300]
  2504   | 600      | [order8: 400, order9: 200]
  2505   | 1000     | [order10: 1000]

Spread: ASK - BID = 2503 - 2502 = ₹1 (tighter spread = more liquid market)

Data structure for order book:
  Java TreeMap<Double, Queue<Order>> bids  (reversed comparator)
  Java TreeMap<Double, Queue<Order>> asks

  bids.firstKey() → best bid (2502) in O(log n)
  asks.firstKey() → best ask (2503) in O(log n)
  Adding order: O(log n) for price level + O(1) for queue append
  Cancelling order: O(1) with order ID → price level map + doubly-linked list removal
```

### Matching Algorithm
```java
void processLimitOrder(Order incomingOrder) {
    if (incomingOrder.side == BUY) {
        // Try to match against asks (sell orders)
        while (incomingOrder.remainingQty > 0 
               && !asks.isEmpty()
               && asks.firstKey() <= incomingOrder.price) {
            
            PriceLevel bestAsk = asks.firstEntry().getValue();
            Order matchedOrder = bestAsk.orders.peek();
            
            long tradedQty = Math.min(incomingOrder.remainingQty, matchedOrder.remainingQty);
            double tradePrice = matchedOrder.price; // price-time priority: resting order's price
            
            // Generate trade
            Trade trade = new Trade(incomingOrder.id, matchedOrder.id, 
                                    tradePrice, tradedQty, symbol, now());
            trades.add(trade);
            publishTrade(trade);
            
            incomingOrder.remainingQty -= tradedQty;
            matchedOrder.remainingQty -= tradedQty;
            
            if (matchedOrder.remainingQty == 0) {
                bestAsk.orders.poll(); // remove fully filled order
                if (bestAsk.orders.isEmpty()) asks.pollFirstEntry(); // remove empty level
            }
        }
        
        // If still has remaining qty → add to bid book (resting limit order)
        if (incomingOrder.remainingQty > 0) {
            bids.computeIfAbsent(incomingOrder.price, p -> new PriceLevel())
                .orders.add(incomingOrder);
        }
    }
    // ... SELL side is symmetric
}
```

---

## Step 6: Why Single-Threaded per Symbol?

```
Core design decision: one matching engine thread per symbol

Arguments for single-threaded:
  1. Determinism: same input sequence → same trades → regulatory requirement
  2. No locking overhead: no mutex contention → microsecond latency
  3. Simple reasoning: no race conditions, no deadlocks
  4. Sequential consistency: order book state is always consistent

But isn't single-threaded slow?
  One thread can process 1-2M orders/sec for a single symbol
  10,000 symbols → 10,000 threads (one per symbol) → parallelism!
  CPU core affinity: pin each symbol's thread to a specific core (no context switching)
  
  In practice: group less-active symbols on shared threads
  Top 100 symbols: dedicated threads
  Remaining: round-robin shared pool

LMAX Disruptor pattern:
  Lock-free ring buffer instead of queue
  100M+ operations/sec with sub-microsecond latency
  Used by: LMAX Exchange, many HFT firms
  
  Traditional: OrderQueue → Thread (lock + context switch)
  Disruptor: Pre-allocated ring buffer, consumers write/read via sequence numbers
             No GC pressure, no locks, cache-friendly sequential memory access
```

---

## Step 7: Market Data Distribution

```
Two audiences:
  1. General investors: delayed (15-20 min delayed data is free)
  2. Subscribers/HFT: real-time (paid data feed)

Distribution architecture:
  Matching Engine generates trade event
  → Market Data Server receives event
  → Multicast UDP to subscribers (for speed — fire and forget)
  → TCP/WebSocket for retail brokers (reliable, ordered)

UDP Multicast (for HFT co-location):
  Single packet → delivered to ALL subscribers simultaneously
  No per-subscriber overhead
  Packet loss: handled by sequence numbers + retransmit request
  Latency: 10-50 microseconds from trade to market data packet

WebSocket fan-out for retail:
  Trade event → Kafka topic: market.data.{symbol}
  Market Data Workers: consume Kafka → push to WebSocket connections
  Scale by adding workers

Order Book depth feed:
  Every order add/cancel/trade → publish order book delta
  Level 2 market data: top 5 bid/ask levels with quantities
  Subscribers reconstruct full order book from deltas
```

---

## Step 8: Persistence and Recovery

```
Critical: Exchange cannot lose any order or trade

Write-ahead log (WAL) pattern:
  Before processing: append order to WAL (sequential disk write, fast)
  Process in memory
  Trade event → append to trade log
  
  On crash: replay WAL to reconstruct order book state
  Recovery time: O(unprocessed orders in WAL) — typically < 1 minute

Sequence numbers on everything:
  Every order: unique sequence number (monotonically increasing)
  Every trade: unique trade ID
  Every market data event: sequence number
  
  Clients detect gaps (seq 1001 received before 1000) → request retransmit

Database:
  Orders: PostgreSQL (ACID, state machine updates)
  Trades: Cassandra (append-only, time-series, high write throughput)
  Positions: PostgreSQL (real-time balance updates, strong consistency)

End-of-day reconciliation:
  Match orders processed by matching engine vs records in DB
  Match trades with clearing house records
  Any mismatch → alert, halt trading for that symbol until resolved
```

---

## Step 9: Risk Management

```
Pre-trade risk checks (before order enters matching engine):
  1. Sufficient balance: can user afford worst case fill?
     BUY 100 shares @ ₹2500 → need ₹250,000 in account
     
  2. Position limit: don't allow naked short positions beyond limit
     Short 10,000 shares of RELIANCE? Check if margin is sufficient
     
  3. Price bands: India's NSE uses circuit breakers:
     Stock can't move > 10-20% from reference price in a day
     Order rejected if price is outside allowed band
     
  4. Market manipulation detection:
     Wash trades (same entity buying and selling to self)
     Spoofing (place large order to move price, cancel before execution)
     Pattern detection runs async, suspicious activity flagged for review

Post-trade risk (real-time during trading session):
  Update position and P&L in real-time
  If account's portfolio loss exceeds daily limit → auto-cancel all open orders
  Margin call: if margin falls below maintenance level → liquidate positions
```

---

## Interview Q&A

**Q: Why can't the matching engine use a database directly?**
A: Database access (even on localhost) takes 1-5ms for a simple read. Matching engines need microsecond latency. The order book must be in memory (RAM). Persistence is async — write to WAL and DB after the trade executes in memory, not before. The matching engine is essentially a deterministic state machine in RAM, with WAL for crash recovery.

**Q: What is price-time priority and why is it fair?**
A: Price-time priority means: best price executes first (price priority), and among orders at the same price, the earlier order executes first (time priority). This rewards traders who offer better prices and who commit earlier. It's the most common matching algorithm on major exchanges (NYSE, NASDAQ, BSE, NSE). Alternative: pro-rata (allocation proportional to size) used in some derivatives markets.

**Q: How would you handle a "flash crash" (extreme price movement in seconds)?**
A: Circuit breakers: if a stock moves > 10% in 5 minutes, halt trading for that stock for 5-15 minutes (stabilization). Market-wide circuit breakers: if index falls > 7%, halt all trading. During halt: cancel day orders (GTC orders can remain). After resumption: call auction (collect all orders, find clearing price). These rules are regulatory requirements on most exchanges.

**Q: How do you ensure two brokers don't both believe they got the fill?**
A: The matching engine is single-threaded per symbol — there is no concurrency issue at the matching level. Each trade is assigned a unique trade ID. Trade confirmations are sent to both sides (broker who bought and broker who sold). If confirmation is lost in transit, broker queries by order ID — idempotent. Daily reconciliation with clearing house catches any discrepancy.

**Q: What is the difference between a market order and a limit order from a system design perspective?**
A: Limit order: enter into the order book at a specific price, wait there until matched or cancelled. Adds to order book — resting order. Market order: execute immediately at the best available price, no price specified. Matches against existing limit orders and executes. Never rests in the order book. From system design: market orders require no book placement, only immediate matching. They can cause significant price impact if the book is thin.
