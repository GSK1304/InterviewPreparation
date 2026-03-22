# 📚 System Design — Pub/Sub System (Apache Kafka)

---

## 🎯 Problem Statement
Design a distributed publish-subscribe messaging system that allows producers to publish messages to topics and multiple consumers to read them independently, with high throughput, durability, and replay capability.

---

## Step 1: Clarify Requirements

### Functional
- Producers publish messages to named topics
- Multiple independent consumer groups read the same topic
- Each consumer group gets every message (fan-out)
- Within a consumer group, each message processed by exactly one consumer
- Message ordering guaranteed within a partition
- Messages retained for configurable duration (replay)
- Consumer can seek to any offset (replay from beginning or any point)
- At-least-once delivery (idempotent consumers handle dedup)

### Non-Functional
- **Throughput**: 1M+ messages/sec per cluster
- **Latency**: < 10ms end-to-end (producer → consumer)
- **Durability**: Messages survive broker failures (replication factor ≥ 3)
- **Scale**: Horizontally scalable — add brokers to increase capacity
- **Retention**: Messages kept for 7 days by default

---

## Step 2: Estimation

```
Throughput:    1M messages/sec
Avg message:   1KB
Write BW:      1GB/sec → need fast sequential disk writes (SSDs or RAID)

Retention:     7 days × 1GB/sec × 86400 sec = ~605TB storage/day × 7 = 4.2PB
               With 3x replication = 12.6PB total

Partitions:    1M msg/sec, 100K msg/sec/partition → 10 partitions minimum
               In practice: 10–100 partitions per topic for parallelism

Consumer lag:  Track offset per (consumer group, topic, partition)
               1M consumer groups × 100 partitions × 8 bytes = ~800MB metadata
```

---

## Step 3: API Design

```
# Producer API
producer.send(topic, key, value, headers?)
  → RecordMetadata { partition, offset, timestamp }

# Consumer API
consumer.subscribe([topics])
consumer.poll(timeout) → List<ConsumerRecord>
consumer.commitSync()   → commit current offsets
consumer.commitAsync()  → async offset commit
consumer.seek(partition, offset) → rewind/fast-forward

# Admin API
admin.createTopic(name, partitions, replicationFactor)
admin.listTopics() → [TopicInfo]
admin.describeConsumerGroup(groupId) → lag per partition
admin.deleteRecords(topic, partition, beforeOffset)

# REST Proxy (Confluent-style)
POST /topics/{name}  Body: { records: [{key, value}] }
GET  /consumers/{group}/instances/{id}/records
```

---

## Step 4: High-Level Architecture

```
Producers                  Kafka Cluster                  Consumers
─────────                  ─────────────                  ─────────
Service A ──►              ┌─────────────────┐
Service B ──► ──────────► │  Broker 1        │ ──────────► Consumer Group A
Service C ──►              │  Topic: orders   │             (order-processor)
                           │  Partition 0,3   │
                           ├─────────────────┤ ──────────► Consumer Group B
                           │  Broker 2        │             (analytics)
                           │  Topic: orders   │
                           │  Partition 1,4   │ ──────────► Consumer Group C
                           ├─────────────────┤             (audit-logger)
                           │  Broker 3        │
                           │  Topic: orders   │
                           │  Partition 2,5   │
                           └─────────────────┘
                                    │
                           ┌────────▼────────┐
                           │   ZooKeeper /   │
                           │   KRaft         │
                           │ (cluster coord) │
                           └─────────────────┘
```

---

## Step 5: Core Concepts Deep Dive

### Topics, Partitions, Offsets
```
Topic: logical channel (e.g., "orders", "payments", "user-events")

Partition: ordered, immutable log
  ┌──────────────────────────────────────────────────────┐
  │ offset: 0  | 1  | 2  | 3  | 4  | 5  | 6  | 7  | → │
  └──────────────────────────────────────────────────────┘
  Messages appended at the end only (sequential writes = fast)
  Consumers track their position via offset

Partition key:
  hash(key) % numPartitions → determines partition
  Same key → same partition → ordering guaranteed for that key
  e.g., orderId as key → all events for same order in same partition, in order
  No key → round-robin distribution

Multiple partitions:
  Parallelism = number of partitions
  Each partition can have ONE consumer per consumer group
  10 partitions → max 10 consumers in a group can process in parallel
```

### Replication
```
Replication factor = 3: each partition on 3 brokers

  Partition 0:
    Leader (Broker 1) ──► Follower (Broker 2) ──► Follower (Broker 3)

  Writes go to leader only
  Followers replicate asynchronously from leader
  
  ISR (In-Sync Replicas): followers that are caught up
  acks=all: producer waits for all ISR to confirm → strongest durability
  acks=1: producer waits for leader only → faster, some data loss risk
  acks=0: fire and forget → fastest, no durability guarantee

  If leader fails:
    Controller elects new leader from ISR
    Clients detect (via metadata refresh) and reconnect to new leader
    Failover time: typically < 30 seconds
```

### Log Storage Design
```
On disk, each partition = set of segment files:
  partition-0/
    00000000000000000000.log  (first segment, starts at offset 0)
    00000000000000000000.index
    00000000000001000000.log  (segment starting at offset 1,000,000)
    00000000000001000000.index

.log file: binary records, sequential append-only
.index file: sparse index offset → file position (for O(log n) seek by offset)

Why sequential writes are key:
  Sequential disk write: 500MB/s (HDD) or 3GB/s (SSD)
  Random disk write: 5MB/s (HDD) — 100x slower
  Kafka writes sequentially → disk throughput is the bottleneck, not seeks

Zero-copy transfer (sendfile syscall):
  Normal: Disk → OS buffer → Kernel → User space → Kernel → Network
  Zero-copy: Disk → OS buffer → Network (skip user space!)
  50%+ reduction in CPU usage for consumer reads
```

### Consumer Groups & Offset Management
```
Consumer Group: set of consumers that collectively consume a topic
  - Each partition assigned to exactly ONE consumer in the group
  - If consumers < partitions: some consumers handle multiple partitions
  - If consumers > partitions: extra consumers are idle (no partition assigned)

Offset storage (Kafka 0.9+):
  Stored in __consumer_offsets internal topic (not ZooKeeper)
  consumer.commitSync() → writes (groupId, topic, partition, offset) to this topic

Rebalancing:
  Triggered when consumer joins/leaves/crashes
  All consumers stop, coordinator reassigns partitions
  New protocol (cooperative rebalancing): only affected partitions reassigned

Offset reset policy (auto.offset.reset):
  earliest: start from beginning of topic (replay all)
  latest: start from now (skip historical messages)
  none: throw exception if no offset found
```

---

## Step 6: Producer Deep Dive

```
Producer batching (for throughput):
  batch.size=16KB: accumulate messages up to 16KB before sending
  linger.ms=5: wait up to 5ms for more messages before sending
  
  Result: Instead of 1 network call per message:
    1 network call per batch (100s of messages)
    5-10x throughput improvement

Compression:
  compression.type=snappy/lz4/zstd
  Applied per batch → 4-5x size reduction for JSON payloads

Idempotent producer (enable.idempotence=true):
  Producer gets a unique producerId from broker
  Each message gets sequence number (producerId, partition, sequenceNum)
  Broker deduplicates on retry → exactly-once within a session

Transactions (cross-partition atomic writes):
  producer.beginTransaction()
  producer.send(topic1, msg1)
  producer.send(topic2, msg2)
  producer.commitTransaction()
  → Either both committed or neither (atomic across partitions)
```

---

## Step 7: When to Use Kafka vs Alternatives

```
Use Kafka when:
  ✅ High throughput (> 100K msg/sec)
  ✅ Message replay needed (event sourcing, audit)
  ✅ Multiple independent consumer groups
  ✅ Long retention (days/weeks)
  ✅ Stream processing (Kafka Streams, Flink)
  ✅ Event-driven microservices

Use RabbitMQ when:
  ✅ Complex routing (topic exchanges, headers exchanges)
  ✅ Per-message TTL and dead-letter queues needed
  ✅ Push-based delivery (consumer doesn't poll)
  ✅ Small teams, simpler operational model
  ✅ Priority queues

Use AWS SQS when:
  ✅ Managed service, no ops overhead
  ✅ Simple task queue
  ✅ 14-day max retention is acceptable
  ✅ Already on AWS

Use Redis Pub/Sub when:
  ✅ Fire-and-forget (no durability needed)
  ✅ Real-time notifications (WebSocket fan-out)
  ✅ Sub-millisecond latency critical
  ❌ Not when durability/replay needed
```

---

## Step 8: Kafka Streams / Stream Processing

```
Kafka Streams: Java library for stream processing, runs inside your app

Example: real-time order aggregation
KStream<String, Order> orders = builder.stream("orders");
KTable<String, Long> orderCounts = orders
    .groupBy((key, order) -> order.getUserId())
    .count();
orderCounts.toStream().to("user-order-counts");

Windowed aggregation:
  orders
    .groupBy((k,v) -> v.getRegion())
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
    .count()
    → count of orders per region per 5-minute window

Stateful processing:
  State stored locally in RocksDB
  Backed by Kafka changelog topic (fault tolerant)
  On restart: restore state from changelog
```

---

## Interview Q&A

**Q: How does Kafka guarantee ordering?**
A: Ordering is guaranteed **within a partition**. Use the same partition key for messages that must be ordered relative to each other (e.g., all events for `orderId=123` go to the same partition via `hash(orderId) % partitions`). Across partitions, there is no global ordering guarantee. If you need global ordering, use a single partition — but that limits throughput to one consumer.

**Q: What happens when a consumer crashes mid-processing?**
A: With `commitSync()` after processing: the uncommitted offset is re-processed after restart — at-least-once delivery. Consumer must be idempotent (handle duplicates). With `commitSync()` before processing (at-most-once): risk of data loss if crash happens after commit but before processing. For exactly-once: use Kafka transactions + idempotent producers together.

**Q: How do you choose the number of partitions?**
A: Rule of thumb: partitions = max(target throughput / throughput per partition, target consumer parallelism). If you need 1M msg/sec and each partition handles 100K msg/sec → 10 partitions minimum. Match to consumer count for even distribution. Partitions can only be increased, never decreased — plan with some headroom. Too many partitions increases ZooKeeper/controller load and rebalancing time.

**Q: What is the difference between Kafka and a traditional message queue?**
A: Traditional MQ (RabbitMQ, SQS): messages deleted after consumption, competing consumers (one consumer gets each message), work distribution pattern. Kafka: messages retained for days/weeks, all consumer groups see all messages independently, replay from any offset, much higher throughput, built for event streaming and event sourcing patterns.

**Q: How does Kafka handle backpressure?**
A: Consumers pull at their own pace — natural backpressure. Producer-side: if brokers are slow, `producer.send()` blocks until `buffer.memory` has space (configurable). `max.block.ms` controls how long producer blocks before throwing exception. Kafka's retention means messages aren't lost even if consumer is far behind — lag is visible via consumer group monitoring.
