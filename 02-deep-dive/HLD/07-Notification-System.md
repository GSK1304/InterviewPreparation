# 📚 HLD Deep Dive — Notification System

---

## 🎯 Problem Statement
Design a scalable notification system that can send push notifications, emails, and SMS to millions of users across multiple channels with priority handling, templating, user preferences, and delivery tracking.

---

## Step 1: Clarify Requirements

### Functional
- Send notifications via multiple channels: push (mobile), email, SMS, in-app
- Notification types: transactional (order confirmed) and marketing (promotions)
- User preferences: opt-out per channel, per category
- Priority: urgent (immediate) vs batch (digest)
- Templates with personalization (user name, order details)
- Delivery tracking: sent, delivered, opened, failed
- Retry on failure with exponential backoff

### Non-Functional
- **Throughput**: 10M notifications/sec (marketing blast to 100M users in minutes)
- **Latency**: Transactional < 1s; Marketing best-effort
- **Reliability**: Transactional: at-least-once; Marketing: best-effort
- **Scale**: 1B users, 10+ notification channels

---

## Step 2: Estimation

```
Notifications/day: 1B users × 5 notifications = 5B/day = ~58K/sec avg
Marketing blast: 100M users in 10 min = 100M/600 = 167K/sec peak

Storage:
  Notification record: 1KB each
  5B/day × 1KB = 5TB/day (prune old after 30 days = 150TB total)
  Inbox (in-app): keep 90 days

Email throughput: 1M emails/min requires dedicated SMTP clusters or SendGrid/SES
Push throughput: APNs processes ~1M push/sec; FCM ~500K/sec per project
```

---

## Step 3: High-Level Architecture

```
Triggering Systems              Notification Platform              Delivery Channels
─────────────────              ────────────────────               ─────────────────
Order Service ──────►
RFQ Service   ──────►  Event Bus   ►  Notification    ►  Push Service ► APNs/FCM
Auth Service  ──────►  (Kafka)        Service             Email Svc  ► SendGrid/SES
Marketing     ──────►                 │                   SMS Svc    ► Twilio
Scheduler     ──────►                 │                   In-App     ► WebSocket
                                      │
                             ┌────────┴────────┐
                             │                 │
                        User Prefs DB      Template DB
                        (PostgreSQL)       (Redis/DB)
                             │
                        Notification DB
                        (Cassandra — delivery tracking)
```

---

## Step 4: Core Components

### Event Producer (Any Service)
```java
// Any service publishes notification event to Kafka
// Decoupled — services don't know about notification channels

kafkaTemplate.send("notifications.transactional", NotificationEvent.builder()
    .userId("user123")
    .type("RFQ_ACCEPTED")
    .priority(Priority.HIGH)
    .data(Map.of("rfqId", "rfq456", "premium", "1.5M USD"))
    .channels(List.of(Channel.PUSH, Channel.EMAIL))
    .build());
```

### Notification Service (Orchestrator)
```
On receiving event from Kafka:
  1. Fetch user preferences (check opt-out per channel per category)
  2. Fetch notification template for event type
  3. Render template with event data (Mustache/Handlebars)
  4. For each allowed channel:
     a. Create notification record in DB (status=PENDING)
     b. Route to channel-specific queue (push.queue, email.queue, sms.queue)
  5. Channel workers pick up from queue → call external provider
  6. Update notification status on delivery/failure
```

### Template Engine
```
Template stored in DB:
  type: "RFQ_ACCEPTED"
  channel: "EMAIL"
  subject: "Your RFQ {{rfqId}} has been accepted"
  body: "Dear {{userName}}, your RFQ for {{pair}} has been accepted at {{premium}}..."
  
Rendering: Mustache template + user data → final notification content
Localization: Template per language → detect user's locale → select template
```

---

## Step 5: Channel Workers

### Push Notification Worker
```java
// Reads from push.queue Kafka topic
@KafkaListener(topics = "push.queue")
void processPushNotification(PushNotificationEvent event) {
    // Fetch device tokens for user
    List<String> deviceTokens = deviceRegistry.getTokens(event.getUserId());
    
    for (String token : deviceTokens) {
        try {
            // APNs for iOS, FCM for Android
            if (token.startsWith("ios:")) {
                apnsClient.send(token, event.getTitle(), event.getBody(), event.getData());
            } else {
                fcmClient.send(token, event.getTitle(), event.getBody(), event.getData());
            }
            updateStatus(event.getNotificationId(), token, Status.DELIVERED);
        } catch (InvalidTokenException e) {
            // Token expired → remove from registry
            deviceRegistry.removeToken(token);
        } catch (Exception e) {
            // Retry with exponential backoff
            retryQueue.publish(event, retryDelay(event.getAttempt()));
        }
    }
}
```

### Email Worker
```
Reads from email.queue → calls SendGrid/AWS SES API
Handles:
  - Bounces → mark email as invalid, suppress future sends
  - Unsubscribes → update user preferences
  - Open/click tracking → webhooks from SendGrid → update delivery record
  - Bulk sending → batching 1000 emails per API call
```

### SMS Worker
```
Reads from sms.queue → calls Twilio/SNS API
Considerations:
  - Phone number validation + formatting (+1 for US, etc.)
  - Character limit (160 chars for standard SMS, 1600 for Unicode)
  - Cost per SMS → only for transactional, never marketing
  - Delivery receipts via webhook
```

---

## Step 6: Priority and Fan-out

### Priority Queues
```
Kafka topics by priority:
  notifications.critical  → processed immediately (auth, payment, security alerts)
  notifications.high      → processed within 1 second (RFQ updates, trade confirmations)
  notifications.normal    → processed within 10 seconds (order updates, promotions)
  notifications.batch     → digest processing (daily summary, weekly reports)

Workers consume in priority order:
  Critical workers: 50 instances, no batching
  High workers: 20 instances
  Normal workers: 10 instances
  Batch workers: 5 instances, process in bulk
```

### Marketing Blast Fan-out
```
Problem: Send to 100M users in 10 minutes
  Naive: Loop through 100M users in single process = too slow

Solution: Fan-out with sharded processing
  1. Campaign scheduler creates campaign record
  2. Segmentation service queries user DB: SELECT user_id WHERE segment=X
  3. Publish chunks of 10K user IDs to Kafka (fan-out.campaign partition)
  4. 1000 notification workers each pick up 1 chunk (10K users)
  5. Each worker: fetch preferences, render template, publish to push/email queues
  6. Channel workers deliver
  
Total time: 100M users / (1000 workers × 10K users/min) = 10 minutes ✅
```

---

## Step 7: User Preferences

```sql
CREATE TABLE notification_preferences (
    user_id        UUID,
    channel        VARCHAR(20),    -- PUSH, EMAIL, SMS, IN_APP
    category       VARCHAR(50),    -- TRANSACTIONAL, MARKETING, SECURITY
    enabled        BOOLEAN DEFAULT TRUE,
    updated_at     TIMESTAMP,
    PRIMARY KEY (user_id, channel, category)
);

-- User opts out of marketing emails:
INSERT INTO notification_preferences VALUES ('user123', 'EMAIL', 'MARKETING', false, NOW())
ON CONFLICT (user_id, channel, category) DO UPDATE SET enabled = false;

-- Check before sending:
SELECT enabled FROM notification_preferences
WHERE user_id=? AND channel=? AND category=?
-- Cache in Redis: rl:prefs:{userId}:{channel}:{category} → 0/1 (TTL 5 min)
```

---

## Step 8: Delivery Tracking

```sql
-- Cassandra (append-only, write-heavy, time-series)
CREATE TABLE notification_log (
    notification_id  UUID,
    user_id          UUID,
    channel          TEXT,
    status           TEXT,       -- PENDING, SENT, DELIVERED, FAILED, OPENED
    provider_id      TEXT,       -- APNs message-id, SES message-id
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    error_code       TEXT,
    PRIMARY KEY (user_id, created_at, notification_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
-- Fetch user's notification history: O(1) Cassandra partition read
```

### Retry Strategy
```
Attempt 1: Immediate
Attempt 2: 30 seconds delay
Attempt 3: 5 minutes delay
Attempt 4: 30 minutes delay
Attempt 5: 2 hours delay
After 5 failures: Mark as FAILED; alert on-call if critical

Exponential backoff with jitter:
  delay = min(baseDelay × 2^attempt + random(0, baseDelay), maxDelay)
  Jitter prevents thundering herd on retry
```

---

## Step 9: In-App Notification Inbox

```
Architecture:
  - Notifications stored in Cassandra (partition by userId)
  - WebSocket push for real-time delivery when user is online
  - REST API for inbox fetch: GET /notifications?before=timestamp&limit=20

Real-time delivery:
  1. Notification service publishes to Kafka topic: inapp.{userId}
  2. WebSocket service (subscribed to all inapp.* topics) pushes to connected user
  3. If user offline: stored in Cassandra, delivered on next connect

Badge count:
  - Redis counter: INCR unread:{userId}
  - Reset on inbox open: SET unread:{userId} 0
  - Pushed via WebSocket on each new notification
```

---

## Step 10: Trade-offs

| Decision | Choice | Reason |
|----------|--------|--------|
| Coupling | Event-based (Kafka) | Services don't know about channels; easy to add new channels |
| Priority | Multiple Kafka topics | Consumer priority via separate worker pools |
| Delivery | At-least-once + idempotency | Reliability > exactly-once complexity |
| Storage | Cassandra | Write-heavy, time-series delivery tracking |
| Preferences | PostgreSQL + Redis cache | ACID for updates; fast checks at scale |
| Template | DB + Mustache | Dynamic updates without deploy; localization |
| Marketing scale | Chunk fan-out | 100M users in 10 min via parallel workers |
| Failure | Exponential backoff + DLQ | Avoid thundering herd; preserve failed events |

---

## Real-World Application (kACE)

```
kACE notification patterns:
  1. RFQ state change → push to trader WebSocket + email if offline
  2. Trade confirmation → email + in-app notification
  3. Dealing limit breach → immediate push to Sales + Admin
  4. System alerts → PagerDuty integration via webhook

Implementation:
  - Kafka topics: rfq.events → Notification Consumer → WebSocket push
  - StompClientSingleton: in-app real-time delivery
  - Email: Spring Mail with Thymeleaf templates
  - Priority: RFQ events = HIGH; trade confirms = CRITICAL
```
