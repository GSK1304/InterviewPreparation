# 📚 DSA Deep Dive — Multi-threaded & Concurrent Data Structures

> Priority: 🟡 P2 — Asked at senior/lead level, especially for Java roles. Also covered in Java 21 section with more depth.

---

## 🧠 Core Problem

Standard data structures (HashMap, ArrayList, LinkedList) are **NOT thread-safe**. When multiple threads access them concurrently without synchronization, you get race conditions, data corruption, and undefined behaviour.

---

## 🔑 Java Concurrent Collections

### ConcurrentHashMap (Thread-safe HashMap)
```java
// Segment-based locking (Java 7) → CAS + synchronized per bucket (Java 8+)
// Multiple threads can read/write simultaneously — much faster than Hashtable
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Thread-safe atomic operations
map.put("key", 1);
map.putIfAbsent("key", 1);                        // atomic check + insert
map.computeIfAbsent("key", k -> expensiveOp(k));  // atomic compute if missing
map.merge("key", 1, Integer::sum);                // atomic increment

// Non-atomic (read then write — still needs external sync if needed)
// map.get() then map.put() is NOT atomic — use compute() instead
map.compute("key", (k, v) -> v == null ? 1 : v + 1); // atomic read-modify-write
```

### CopyOnWriteArrayList (Thread-safe List for read-heavy)
```java
// Write = copy entire array, modify copy, swap reference
// Read = no locking at all — always sees consistent snapshot
// Best for: read-heavy, rarely-written lists (event listeners, config)
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("item");          // creates a new copy internally
String val = list.get(0);  // lock-free read

// DO NOT use for write-heavy — O(n) per write
```

### BlockingQueue (Producer-Consumer Pattern)
```java
// Thread-safe queue with blocking put/take operations
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(capacity);

// Producer thread
queue.put(task);           // blocks if queue is full
queue.offer(task, 1, SECONDS); // try for 1 second

// Consumer thread
Task task = queue.take();  // blocks if queue is empty
Task task = queue.poll(1, SECONDS); // try for 1 second

// Implementations:
LinkedBlockingQueue<T>    // unbounded or bounded, linked nodes
ArrayBlockingQueue<T>     // bounded, array-backed, fair ordering option
PriorityBlockingQueue<T>  // unbounded, priority-ordered, no blocking on put
DelayQueue<T>             // elements become available after a delay
```

### ConcurrentLinkedQueue (Lock-free Queue)
```java
// Non-blocking, lock-free using CAS (Compare-And-Swap)
// Best for: high-throughput, non-blocking producer-consumer
ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
queue.offer(task);   // never blocks
queue.poll();        // returns null if empty (never blocks)
```

---

## 🔑 Thread-Safe Patterns

### Pattern 1: Producer-Consumer with BlockingQueue
```java
// Classic pattern — decouples producers and consumers
BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

// Producer
Thread producer = new Thread(() -> {
    while (true) {
        String msg = generateMessage();
        queue.put(msg);  // blocks if full
    }
});

// Consumer
Thread consumer = new Thread(() -> {
    while (true) {
        String msg = queue.take();  // blocks if empty
        process(msg);
    }
});
```

### Pattern 2: Read-Write Lock (Many readers, few writers)
```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();
Map<String, String> cache = new HashMap<>();

// Multiple threads can read simultaneously
String get(String key) {
    readLock.lock();
    try { return cache.get(key); }
    finally { readLock.unlock(); }
}

// Only one thread writes at a time, blocks all readers
void put(String key, String val) {
    writeLock.lock();
    try { cache.put(key, val); }
    finally { writeLock.unlock(); }
}
```

### Pattern 3: Thread-Safe Singleton (Double-Checked Locking)
```java
class StaticCacheOrchestrator {
    private static volatile StaticCacheOrchestrator instance;

    static StaticCacheOrchestrator getInstance() {
        if (instance == null) {                    // first check (no lock)
            synchronized (StaticCacheOrchestrator.class) {
                if (instance == null) {            // second check (with lock)
                    instance = new StaticCacheOrchestrator();
                }
            }
        }
        return instance;
    }
}
// volatile ensures visibility across threads
// Double-check avoids synchronization overhead after first init
```

### Pattern 4: Atomic Operations (Lock-free Counter)
```java
// AtomicInteger — lock-free thread-safe counter using CAS
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();          // atomic ++
counter.compareAndSet(expected, newVal); // CAS — only updates if current == expected
counter.getAndAdd(5);               // atomic += 5

// AtomicReference — lock-free reference swap
AtomicReference<Config> configRef = new AtomicReference<>(initialConfig);
configRef.compareAndSet(oldConfig, newConfig); // atomic swap
```

### Pattern 5: CountDownLatch & CyclicBarrier
```java
// CountDownLatch — wait for N threads to finish
CountDownLatch latch = new CountDownLatch(3); // wait for 3 threads
// Each worker thread:
latch.countDown();  // signal done
// Main thread:
latch.await();      // blocks until count reaches 0

// CyclicBarrier — all threads wait at a checkpoint
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("All ready!"));
// Each thread:
barrier.await();    // wait until all 3 threads reach this point
// Reusable — can be reset for next phase
```

### Pattern 6: Thread-Safe LRU Cache
```java
// Combine ConcurrentHashMap + synchronization for thread-safe LRU
class ThreadSafeLRUCache<K, V> {
    private final int capacity;
    private final Map<K, V> cache;

    ThreadSafeLRUCache(int capacity) {
        this.capacity = capacity;
        // LinkedHashMap with access-order + synchronized wrapper
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > capacity;
                }
            }
        );
    }

    V get(K key) { return cache.get(key); }
    void put(K key, V val) { cache.put(key, val); }
}
```

---

## 🔑 Common DSA Problems in Multi-threaded Context

### Thread-Safe Stack
```java
class ThreadSafeStack<T> {
    private final Deque<T> stack = new ArrayDeque<>();

    synchronized void push(T item) { stack.push(item); }
    synchronized T pop() {
        if (stack.isEmpty()) throw new EmptyStackException();
        return stack.pop();
    }
    synchronized T peek() { return stack.peek(); }
    synchronized boolean isEmpty() { return stack.isEmpty(); }
}
```

### Blocking Bounded Buffer (Classic Semaphore Pattern)
```java
class BoundedBuffer<T> {
    private final Queue<T> buffer = new LinkedList<>();
    private final int capacity;
    private final Semaphore items = new Semaphore(0);       // count of available items
    private final Semaphore spaces;                          // count of available spaces
    private final Semaphore mutex = new Semaphore(1);        // mutual exclusion

    BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.spaces = new Semaphore(capacity);
    }

    void produce(T item) throws InterruptedException {
        spaces.acquire();  // wait for space
        mutex.acquire();
        buffer.offer(item);
        mutex.release();
        items.release();   // signal item available
    }

    T consume() throws InterruptedException {
        items.acquire();   // wait for item
        mutex.acquire();
        T item = buffer.poll();
        mutex.release();
        spaces.release();  // signal space available
        return item;
    }
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Kafka Consumer Thread Pool (kACE)
```java
// Multiple threads consuming from Kafka partitions concurrently
BlockingQueue<KafkaRecord> processingQueue = new LinkedBlockingQueue<>(1000);

// Kafka poll thread (single) → puts into queue
kafkaConsumer.poll(Duration.ofMillis(100))
    .forEach(record -> processingQueue.put(record));

// Worker threads (multiple) → take from queue
ExecutorService workers = Executors.newFixedThreadPool(4);
workers.submit(() -> {
    while (true) {
        KafkaRecord record = processingQueue.take();
        processRfqEvent(record);
    }
});
```
**Where it applies**: kACE Kafka consumer pipeline — parallel RFQ event processing.
> 🏭 **Industry Example**: Netflix's Hystrix (circuit breaker library) uses `LinkedBlockingQueue` + thread pool for bounded concurrent execution. Twitter's Finagle uses similar bounded queues for request isolation. Apache Kafka's consumer itself uses `LinkedBlockingQueue` internally for its fetch pipeline.
> 🏦 **kACE Context**: kACE Kafka consumer pipeline — parallel RFQ event processing using `LinkedBlockingQueue` + `ExecutorService` worker pool.


---

### Use Case 2: Thread-Safe Dropdown Cache (kACE Spring Boot)
```java
// StaticCacheOrchestrator — shared cache across request threads
@Component
public class DropdownCache {
    // ConcurrentHashMap — multiple request threads read simultaneously
    private final ConcurrentHashMap<String, List<Option>> cache
        = new ConcurrentHashMap<>();

    public List<Option> getOptions(String fieldName) {
        // computeIfAbsent — atomic: only loads once even under concurrent requests
        return cache.computeIfAbsent(fieldName, this::loadFromDB);
    }
}
```
**Where it applies**: kACE `StaticCacheOrchestrator` — thread-safe pre-load across Spring request threads.
> 🏭 **Industry Example**: Spring Framework's `@Cacheable` uses `ConcurrentHashMap` internally for thread-safe method-level caching. Guava's `LoadingCache` uses `ConcurrentHashMap` with `computeIfAbsent` for concurrent cache loading. Netflix's EVCache uses ConcurrentHashMap for in-memory L1 cache.
> 🏦 **kACE Context**: kACE `StaticCacheOrchestrator` — `ConcurrentHashMap.computeIfAbsent()` for thread-safe dropdown pre-loading across Spring request threads.


---

### Use Case 3: WebSocket Session Registry (kACE)
```java
// Multiple WebSocket threads registering/deregistering concurrently
ConcurrentHashMap<String, Set<WebSocketSession>> topicSessions
    = new ConcurrentHashMap<>();

// Thread-safe add session to topic
void subscribe(String topic, WebSocketSession session) {
    topicSessions.computeIfAbsent(topic,
        k -> ConcurrentHashMap.newKeySet()).add(session);
}

// Thread-safe broadcast
void broadcast(String topic, String message) {
    topicSessions.getOrDefault(topic, Set.of())
        .forEach(s -> sendSafe(s, message));
}
```
**Where it applies**: kACE `SubscriptionRegistry` — concurrent WebSocket session management.
> 🏭 **Industry Example**: Slack's real-time messaging uses `ConcurrentHashMap<channelId, CopyOnWriteArraySet<session>>` for thread-safe session management. Discord's gateway uses concurrent maps for voice channel session tracking. Twitch's chat system uses concurrent maps for viewer session routing.
> 🏦 **kACE Context**: kACE `SubscriptionRegistry` — `ConcurrentHashMap.newKeySet()` for thread-safe WebSocket topic-to-session mapping.


---

### Use Case 4: Atomic RFQ Counter
```java
// Count active RFQs across multiple threads atomically
AtomicInteger activeRfqCount = new AtomicInteger(0);

// On RFQ start:
int count = activeRfqCount.incrementAndGet();
if (count > MAX_CONCURRENT_RFQS) {
    activeRfqCount.decrementAndGet();
    throw new RfqLimitExceededException();
}

// On RFQ complete:
activeRfqCount.decrementAndGet();
```
**Where it applies**: kACE RFQ rate limiting — concurrent RFQ count without locks.
> 🏭 **Industry Example**: AWS API Gateway uses `AtomicInteger` / `AtomicLong` for per-region request counters without locks. NGINX uses atomic counters for connection tracking. Netty (used by Kafka, Elasticsearch) uses atomic counters for active channel tracking.
> 🏦 **kACE Context**: kACE RFQ rate limiting — `AtomicInteger.incrementAndGet()` for lock-free concurrent RFQ count tracking.


---

### Use Case 5: Read-Heavy Config Cache (ReadWriteLock)
```java
// Layout configs read by hundreds of requests, updated rarely
ReadWriteLock lock = new ReentrantReadWriteLock();
Map<String, LayoutConfig> layoutCache = new HashMap<>();

// Hundreds of concurrent reads — no blocking between readers
LayoutConfig getLayout(String screenId) {
    lock.readLock().lock();
    try { return layoutCache.get(screenId); }
    finally { lock.readLock().unlock(); }
}

// Rare config update — blocks all readers temporarily
void updateLayout(String screenId, LayoutConfig config) {
    lock.writeLock().lock();
    try { layoutCache.put(screenId, config); }
    finally { lock.writeLock().unlock(); }
}
```
**Where it applies**: kACE `useMergedLayout` — read-heavy layout config with occasional DB refresh.
> 🏭 **Industry Example**: Elasticsearch's cluster state management uses `ReentrantReadWriteLock` — many nodes read cluster state, only the master writes it. Spring's `@Cacheable` at cluster level uses read-write locks for cache population. ZooKeeper's data node access uses read-write locking semantics.
> 🏦 **kACE Context**: kACE `useMergedLayout` — read-write lock for layout config cache with hundreds of concurrent read requests and rare DB refresh writes.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Implement Thread-Safe Stack | Synchronized | Medium |
| 2 | Design Bounded Blocking Queue | Semaphore / BlockingQueue | Medium |
| 3 | LRU Cache (thread-safe) | ConcurrentHashMap + LinkedHashMap | Medium |
| 4 | Print in Order (3 threads) | CountDownLatch / Semaphore | Easy |
| 5 | Print FooBar Alternately | Semaphore / synchronized | Medium |
| 6 | Web Crawler (multi-threaded) | ConcurrentHashMap + ThreadPool | Hard |
| 7 | Design Rate Limiter (concurrent) | AtomicInteger + ConcurrentHashMap | Medium |
| 8 | Producer-Consumer | BlockingQueue | Medium |
| 9 | Read-Write Lock implementation | ReentrantReadWriteLock | Medium |
| 10 | Dining Philosophers | Deadlock avoidance | Hard |

---

## ⚠️ Common Mistakes & Pitfalls

- **HashMap in multi-threaded context** → Use `ConcurrentHashMap` not `Collections.synchronizedMap` (the latter locks entire map)
- **`i++` is NOT atomic** → Use `AtomicInteger.incrementAndGet()`
- **Double-checked locking without `volatile`** → broken visibility — always add `volatile`
- **`CopyOnWriteArrayList` for write-heavy workloads** → O(n) per write is too expensive
- **Holding locks across I/O** → causes thread starvation — keep critical sections short
- **Deadlock** → Always acquire locks in same order across all threads
- **`synchronized` on non-final fields** → if field reference changes, lock breaks
- **`BlockingQueue.poll()` vs `take()`** → `poll()` returns null if empty; `take()` blocks — use the right one for your use case

---

## 📐 Quick Reference — Which Collection When?

| Need | Use |
|------|-----|
| Thread-safe key-value store | `ConcurrentHashMap` |
| Read-heavy list, rare writes | `CopyOnWriteArrayList` |
| Producer-consumer with backpressure | `LinkedBlockingQueue` (bounded) |
| Lock-free non-blocking queue | `ConcurrentLinkedQueue` |
| Thread-safe counter | `AtomicInteger` / `AtomicLong` |
| Thread-safe reference swap | `AtomicReference` |
| Many readers, few writers | `ReentrantReadWriteLock` |
| Mutual exclusion | `ReentrantLock` / `synchronized` |
| Wait for N tasks to complete | `CountDownLatch` |
| All threads reach a point together | `CyclicBarrier` |
| Control concurrent access count | `Semaphore` |
