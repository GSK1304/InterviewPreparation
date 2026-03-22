# 📚 System Design — Payment Gateway (Stripe / Razorpay)

---

## 🎯 Problem Statement
Design a payment gateway that enables merchants to accept payments from customers, process card transactions via payment networks, handle refunds, and provide a reliable, secure, and idempotent payment API.

---

## Step 1: Clarify Requirements

### Functional
- Accept payments (credit/debit card, UPI, wallets, net banking)
- Process authorization, capture, and settlement
- Refunds (full and partial)
- Webhooks — notify merchants of payment events
- Payment dashboard — transaction history, analytics
- Fraud detection
- Support multiple currencies and payment methods
- Idempotent payment API (no double charges)

### Non-Functional
- **Availability**: 99.999% — downtime = lost revenue for merchants
- **Latency**: Payment authorization < 2 seconds p99
- **Consistency**: Strong — financial data must never be lost or duplicated
- **Security**: PCI-DSS compliance (no raw card data stored)
- **Scale**: 1000 TPS peak (Stripe processes ~250M transactions/day)
- **Durability**: Zero data loss — every transaction must be durable

---

## Step 2: Estimation

```
Transactions:  250M/day = 2,893 TPS average, ~10,000 TPS peak
Avg amount:    $50 per transaction
Daily volume:  $12.5B processed/day

Storage per transaction: ~2KB (metadata, status, audit trail)
Storage/day:   250M × 2KB = 500GB/day
10-year total: ~1.8PB

Webhooks:      Each transaction → 3-5 webhook events = 1B webhook calls/day
Card data:     Never stored raw (tokenized via Vault)
```

---

## Step 3: API Design

```
# Create payment intent (Stripe style — two-step)
POST /v1/payment_intents
Body: { amount: 9999, currency: "usd", payment_method: "pm_xxx" }
Response: { id: "pi_xxx", status: "requires_confirmation", client_secret: "pi_xxx_secret_xxx" }

# Confirm payment
POST /v1/payment_intents/{id}/confirm
Idempotency-Key: {uuid}   ← REQUIRED for safe retry
Response: { id, status: "succeeded" | "requires_action" | "payment_failed" }

# Create refund
POST /v1/refunds
Body: { payment_intent: "pi_xxx", amount: 5000 }
Idempotency-Key: {uuid}
Response: { id: "re_xxx", status: "pending" | "succeeded" | "failed" }

# Retrieve payment
GET  /v1/payment_intents/{id}

# Webhook registration
POST /v1/webhooks
Body: { url: "https://merchant.com/webhook", events: ["payment_intent.succeeded"] }

# Dispute management
GET  /v1/disputes
POST /v1/disputes/{id}/close
```

---

## Step 4: High-Level Architecture

```
Customer Browser/App
        │ HTTPS (card data never leaves browser → Stripe.js tokenizes)
        │
┌───────▼──────────────────────────────────────────────────┐
│                    API Gateway                            │
│  TLS termination, API key auth, rate limiting, routing   │
└───────┬──────────────────────────────────────────────────┘
        │
┌───────▼──────────────────────────────────────────────────┐
│              Payment Service                              │
│  - Validate request                                       │
│  - Idempotency check (Redis)                             │
│  - Create PaymentIntent record (DB)                      │
│  - Route to appropriate payment processor                │
└───────┬──────────────────────────────────────────────────┘
        │
   ┌────┴──────────────────────────┐
   │                               │
┌──▼────────────┐        ┌────────▼───────────┐
│ Card Processor│        │  UPI / Wallet       │
│  Connector    │        │  Connector          │
│  (Visa/MC/Amex│        │  (NPCI, Paytm, GPay)│
│   via Acquirer│        └────────────────────┘
│   bank)       │
└──┬────────────┘
   │ Authorization request
   ▼
Card Networks (Visa/Mastercard)
   │ Authorization response
   ▼
Issuing Bank (customer's bank) → approve/decline
   │
┌──▼─────────────────────────────────────────────────────┐
│  Ledger Service          Webhook Service                │
│  Double-entry accounting  Event fan-out to merchants    │
│  (PostgreSQL)             (Kafka → delivery workers)    │
└────────────────────────────────────────────────────────┘
```

---

## Step 5: Payment Flow — Card Transaction

```
Full authorization → capture → settlement flow:

Step 1: Tokenization (browser/mobile)
  Stripe.js collects card details in iframe (merchant never sees raw card)
  Card details → Stripe vault → returns token (pm_xxx)
  Raw card data encrypted, stored in Stripe's PCI-DSS vault
  Merchant's systems never touch raw card data → simplified PCI compliance

Step 2: Authorization
  PaymentService → Acquirer bank → Card Network → Issuing Bank
  Issuing bank checks: sufficient funds? fraud signals? card valid?
  Response: authorized (hold funds) | declined (reason code)
  Latency: 500ms-2s (network hops to bank and back)

Step 3: Capture
  Authorize-only: hotel check-in holds $X, captures actual amount at checkout
  Immediate capture: most e-commerce (auth + capture in one call)
  Capture deadline: typically 7 days after auth (after that, auth expires)

Step 4: Settlement (end of day)
  Batch process: merchant's acquirer sends all captured transactions to card network
  Card network routes to each issuing bank
  Issuing bank debits customer, credits acquirer (T+1 or T+2 business days)
  Acquirer credits merchant account (minus fees)

Step 5: Payout
  Stripe holds merchant funds briefly → transfers to merchant bank account
  Standard: T+2 business days
  Instant payout (for fee): near real-time via Visa/MC push-to-card
```

---

## Step 6: Idempotency — Critical for Payments

```
Problem: Network timeout after payment is processed
  Client: "Did the payment go through? Let me retry..."
  Without idempotency: double charge!

Solution: Idempotency Key
  Client generates UUID for each unique payment intent
  Sends: Idempotency-Key: uuid-abc-123

Server processing:
  1. Check Redis: GET idempotency:{key}
     EXISTS → return cached response (same as original)
     NOT EXISTS → proceed
  2. Begin processing
  3. SET idempotency:{key} {response} EX 86400 (24hr)
     (Set AFTER processing, with TTL to prevent infinite growth)
  4. Return response

Edge case — crash between processing and storing key:
  Transaction committed but idempotency key not stored
  Retry arrives → key not found → process again → duplicate!
  Fix: Store idempotency key IN THE SAME DB TRANSACTION as the payment
    BEGIN;
    INSERT INTO idempotency_keys (key, response) VALUES (?, ?)
      ON CONFLICT DO NOTHING;
    INSERT INTO payments (...) VALUES (...);
    COMMIT;
  If key already exists: CONFLICT → return existing response (idempotent!)
```

---

## Step 7: Ledger — Financial Accuracy

```
Double-entry bookkeeping: every transaction has debit + credit entries

Example: Customer pays merchant $100
  DEBIT  customer_liability   $100  (we owe customer $100 less)
  CREDIT merchant_payable     $97   (we owe merchant $97 after 3% fee)
  CREDIT revenue              $3    (our fee)

Ledger table:
  CREATE TABLE ledger_entries (
    id           UUID PRIMARY KEY,
    account_id   BIGINT,
    amount       BIGINT,          -- in cents, never float!
    currency     CHAR(3),
    entry_type   ENUM('debit','credit'),
    transaction_id UUID,
    created_at   TIMESTAMP
  );

  -- Balance = SUM(credits) - SUM(debits) for account
  -- Never UPDATE balances — only INSERT new entries (immutable log)

Why BIGINT for money (never FLOAT):
  Float: 0.1 + 0.2 = 0.30000000000000004 (IEEE 754 floating point error)
  BIGINT cents: 10 + 20 = 30 (exact, always)
  Display: 30 cents → "$0.30" (format at display layer)

Ledger is append-only:
  No UPDATE or DELETE on ledger entries
  Corrections via reversal entries (new rows)
  Full audit trail always available
```

---

## Step 8: Fraud Detection

```
Real-time fraud scoring (< 100ms, runs during authorization):

Features used:
  - Velocity: how many transactions in last 1hr/24hr for this card?
  - Location: is billing address far from IP geolocation?
  - Device fingerprint: new device for this account?
  - Merchant risk: high-risk category (digital goods, gambling)?
  - Card BIN: issuing country vs transaction country mismatch?
  - Historical: has this card been flagged before?
  - Behavior: unusual amount for this customer's history?

Architecture:
  Transaction event → Fraud Service (sync, < 50ms)
    → Feature extraction (Redis lookups for velocity counts)
    → ML model scoring (XGBoost or neural net)
    → Rule engine (hard rules: blocked countries, max amount)
  → Score: 0-100 (low to high risk)
  → Decision: approve | review | decline

Post-authorization:
  Transaction → Kafka → async fraud analysis (deeper ML models)
  Retroactive fraud flag → trigger refund + card block

Velocity counters in Redis:
  INCR fraud:card:{cardHash}:1hr, EXPIRE 3600
  INCR fraud:card:{cardHash}:24hr, EXPIRE 86400
  Check before authorization: if count > threshold → flag
```

---

## Step 9: Webhook Delivery

```
Merchants register webhook URLs for events:
  payment_intent.succeeded, payment_intent.failed, refund.created, dispute.created

Delivery architecture:
  Payment event → Kafka topic: payment.events
  Webhook Worker polls Kafka:
    For each merchant subscribed to this event type:
      POST merchant_webhook_url { event data }
      
Reliability requirements:
  At-least-once delivery (retry on failure)
  Idempotent webhooks (merchants handle duplicates via event_id)

Retry policy:
  Immediate → 5s → 30s → 5min → 30min → 2hr → 8hr → 24hr → give up
  Max 3 days of retries
  
Failure handling:
  4xx (merchant bug) → don't retry (configuration error)
  5xx / timeout → retry with backoff
  If endpoint consistently fails: disable webhook, alert merchant

Webhook signing (HMAC-SHA256):
  Stripe-Signature: t=timestamp,v1=hmac_signature
  Merchant verifies: HMAC(secret, timestamp + "." + payload) == signature
  Prevents replay attacks (timestamp within 5 min), forged webhooks
```

---

## Step 10: Database Design

```sql
CREATE TABLE payment_intents (
    id              VARCHAR(30) PRIMARY KEY,  -- pi_xxx
    merchant_id     BIGINT,
    amount          BIGINT,          -- in cents
    currency        CHAR(3),
    status          ENUM('created','processing','succeeded','failed','cancelled'),
    payment_method_id VARCHAR(30),
    idempotency_key VARCHAR(255) UNIQUE,
    metadata        JSONB,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE transactions (
    id              VARCHAR(30) PRIMARY KEY,
    payment_intent_id VARCHAR(30),
    type            ENUM('authorization','capture','refund','dispute'),
    amount          BIGINT,
    currency        CHAR(3),
    status          ENUM('pending','succeeded','failed'),
    processor_ref   VARCHAR(100),  -- acquirer transaction ID
    failure_code    VARCHAR(50),
    failure_message TEXT,
    created_at      TIMESTAMP
);

CREATE TABLE idempotency_keys (
    key             VARCHAR(255) PRIMARY KEY,
    merchant_id     BIGINT,
    response_status INT,
    response_body   JSONB,
    created_at      TIMESTAMP
);
```

---

## Interview Q&A

**Q: How do you prevent double charges on network timeout?**
A: Idempotency keys — client generates a UUID per payment attempt and sends it as a header. Server checks if this key was already processed (in DB atomically with the payment). If yes, return the original response without processing again. This means any number of retries produce exactly one charge.

**Q: How does Stripe tokenize card data to achieve PCI compliance?**
A: Stripe.js runs in an iframe on the merchant's page — the browser sends card data directly to Stripe's servers, never touching the merchant's server. Stripe returns a single-use token. The merchant passes only the token to their server. This way, the merchant never handles raw card data and only needs to comply with the simplest PCI-DSS level (SAQ-A).

**Q: How would you design the refund flow for a partially captured payment?**
A: Refund can only be for ≤ captured amount. Create a refund transaction record, call acquirer API with capture reference. Acquirer reverses the charge to the issuing bank (T+3-5 days to reach customer). Create ledger entries for the reversal. Fire refund.created webhook. If acquirer refund fails (e.g., card expired), fall back to bank transfer. Track refund state machine: pending → succeeded | failed.

**Q: How do you handle currency conversion?**
A: Store all amounts in the settlement currency (USD for Stripe). Convert at time of transaction using mid-market rate + spread. Store both: original customer amount (EUR) + settled amount (USD) + exchange rate used. Rate source: ECB rates updated daily, or real-time FX feed. Round half-up for consumer; round half-down for merchant — standard financial rounding.

**Q: How would you design the dispute/chargeback flow?**
A: Customer disputes charge with their bank → issuing bank reverses funds from acquirer → acquirer notifies gateway via dispute notification → gateway notifies merchant via webhook → merchant has 7-21 days to submit evidence → card network rules on outcome → if merchant wins: reversal reversed; if customer wins: merchant keeps chargeback. Store dispute as separate entity with state machine and deadline tracking.
