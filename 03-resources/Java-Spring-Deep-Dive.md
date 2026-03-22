# 📚 Java 21 & Spring Boot — Technical Deep-Dive

> For senior/staff interviews that go deep on Java internals, concurrency, and Spring Boot architecture.

---

## Part 1: Java 21 Key Features

### Records
```java
// Immutable value object — compiler generates equals, hashCode, toString, accessors
public record Money(long paise, String currency) {
    // Compact constructor for validation
    Money {
        if (paise < 0) throw new IllegalArgumentException("Negative money");
        Objects.requireNonNull(currency);
    }
    // Custom method
    public double toMajorUnit() { return paise / 100.0; }
}

// Records cannot be extended (implicitly final)
// Fields are implicitly private and final
// No setters — records are immutable by design
```

### Sealed Classes
```java
// Restricts which classes can implement/extend
public sealed interface Shape permits Circle, Rectangle, Triangle {}

public record Circle(double radius)               implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height)   implements Shape {}

// Compiler enforces exhaustiveness in switch
double area(Shape s) {
    return switch (s) {
        case Circle    c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle  t -> 0.5 * t.base() * t.height();
        // No default needed — sealed guarantees exhaustiveness
        // Adding a new permit type forces fixing all switches
    };
}
```

### Pattern Matching
```java
// instanceof with binding variable
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s.toUpperCase()); // s is String here, no cast needed
}

// Switch pattern matching (Java 21, standard)
Object result = switch (obj) {
    case Integer i when i > 0 -> "positive: " + i;
    case Integer i            -> "non-positive: " + i;
    case String  s            -> "string: " + s;
    case null                 -> "null";
    default                   -> "other";
};
```

### Virtual Threads (Java 21 — Project Loom)
```java
// Virtual threads: lightweight, JVM-managed (not OS threads)
// Can create MILLIONS — unlike platform threads (~10K practical limit)

// Old way: platform thread per request (expensive)
new Thread(() -> handleRequest(request)).start();

// New way: virtual thread per request (cheap)
Thread.ofVirtual().start(() -> handleRequest(request));

// With ExecutorService
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handleRequest(r1));
    executor.submit(() -> handleRequest(r2));
    // Each task gets its own virtual thread — no thread pool contention
}

// When to use virtual threads:
// - I/O-bound tasks (DB queries, HTTP calls) — the main benefit
// - When you're currently using thread pools with many threads
// When NOT to use:
// - CPU-bound tasks (virtual threads don't help, may hurt)
// - When holding locks for long periods (still blocks the carrier thread)
```

---

## Part 2: Java Concurrency

### The Memory Model — What Every Senior Must Know
```
Java Memory Model guarantees:
  1. Actions in a thread are visible to that thread in program order
  2. volatile write HAPPENS-BEFORE volatile read of same variable
  3. synchronized release HAPPENS-BEFORE subsequent acquire of same monitor
  4. Thread.start() HAPPENS-BEFORE any action in the started thread
  5. All actions in a thread HAPPEN-BEFORE Thread.join() returns

WITHOUT happens-before: compiler/CPU may reorder instructions
→ Thread B may see stale values written by Thread A
→ This is WHY you need volatile/synchronized
```

### synchronized vs volatile vs Atomic
```java
// volatile: visibility only, NO atomicity
private volatile boolean running = true;
// Safe: one writer, multiple readers
// NOT safe: running++ (read-modify-write is not atomic even with volatile)

// synchronized: visibility + atomicity + mutual exclusion
synchronized void increment() { count++; }
// Safe for compound actions
// Overhead: OS-level lock (blocking)

// AtomicInteger: visibility + atomicity, NO mutual exclusion
private final AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();   // atomic, non-blocking (CAS)
count.compareAndSet(expected, update); // compare-and-swap

// When to use what:
// volatile:     single-variable flag, one writer (running, initialized)
// AtomicXxx:    counter/reference with frequent updates, no compound logic
// synchronized: compound check-then-act, multiple variables updated together
// ReentrantLock: same as synchronized + tryLock(), timed lock, fairness
```

### CompletableFuture — Modern Async
```java
// Chain async operations without blocking
CompletableFuture<User> future = CompletableFuture
    .supplyAsync(() -> userRepository.findById(userId))     // runs in ForkJoinPool
    .thenApply(user -> enrichWithProfile(user))             // transforms result
    .thenCompose(user -> fetchOrders(user.getId()))         // another async op
    .exceptionally(ex -> User.anonymous());                 // fallback on error

// Combine multiple futures
CompletableFuture<User>   userFuture   = fetchUser(id);
CompletableFuture<Orders> ordersFuture = fetchOrders(id);

CompletableFuture.allOf(userFuture, ordersFuture)
    .thenRun(() -> {
        User   user   = userFuture.join();   // guaranteed complete here
        Orders orders = ordersFuture.join();
        buildResponse(user, orders);
    });

// Running on specific executor (don't block ForkJoinPool with I/O!)
ExecutorService ioPool = Executors.newFixedThreadPool(50);
CompletableFuture.supplyAsync(() -> dbQuery(), ioPool);
```

### Common Concurrency Pitfalls
```java
// PITFALL 1: Check-then-act race condition
if (!map.containsKey(key)) {        // Thread A checks: false
    map.put(key, computeValue());   // Thread B also checks: false
}                                   // BOTH put — one overwrites
// Fix: map.computeIfAbsent(key, k -> computeValue())

// PITFALL 2: Publish before construction complete
class Singleton {
    private static Singleton instance;
    private int value;
    public static Singleton get() {
        if (instance == null) {
            instance = new Singleton();  // instance visible before constructor done?
        }
        return instance;
    }
}
// Fix: volatile + double-checked locking, or enum singleton

// PITFALL 3: Holding lock during I/O
synchronized void saveToDb(Data data) {
    repository.save(data);  // holds lock while DB call blocks — kills throughput
}
// Fix: do I/O outside lock, only lock state updates

// PITFALL 4: ConcurrentModificationException
for (String item : list) {
    if (shouldRemove(item)) list.remove(item);  // CME!
}
// Fix: use Iterator.remove(), or list.removeIf(this::shouldRemove)
```

---

## Part 3: JVM Internals

### Memory Areas
```
Heap:
  Young Generation (Eden + Survivor S0/S1):
    New objects allocated here
    Minor GC: fast, collects frequently
    Objects that survive N GCs promoted to Old Gen
  Old Generation (Tenured):
    Long-lived objects
    Major GC (Full GC): slower, less frequent
  Metaspace (Java 8+, replaced PermGen):
    Class metadata, method bytecode
    Not part of heap, grows dynamically

Stack (per thread):
  Local variables, method call frames
  Stack overflow = infinite recursion or very deep call stack

Off-Heap:
  Direct ByteBuffer (NIO), Netty uses this
  Not GC'd — manual management
```

### GC Algorithms
```
Serial GC:          Single thread. Use for small heaps, simple apps.
Parallel GC:        Multi-thread Young Gen collection. Default in Java 8.
G1 GC:              Default Java 9+. Predictable pause times. Heap split into regions.
ZGC (Java 15+):     Sub-millisecond pauses. Good for large heaps (TB scale).
Shenandoah:         Also sub-ms, OpenJDK alternative to ZGC.

For web services: G1GC is default and usually fine
For latency-sensitive (trading, gaming): ZGC or tune G1 pause targets
  -XX:MaxGCPauseMillis=50  (target pause time for G1)
  -Xms4g -Xmx4g            (set min=max to avoid GC on heap resize)
```

### Class Loading
```
Bootstrap ClassLoader:  loads rt.jar (java.lang, java.util, etc.)
Extension ClassLoader:  loads lib/ext jars
Application ClassLoader: loads classpath

Delegation model: child asks parent first → prevents class conflicts

Class lifecycle: Loading → Linking (Verify+Prepare+Resolve) → Initialisation → Use → Unloading

Static initialisation order matters:
  class A { static int x = B.y + 1; }  // circular dependency = 0 + 1 = 1 (not what you expect!)
```

---

## Part 4: Spring Boot Internals

### Bean Lifecycle
```
1. BeanDefinition registered (via @Component scan or @Bean)
2. BeanFactory post-processing (@PropertySource, @Conditional evaluated)
3. Bean instantiation (constructor called)
4. Dependency injection (@Autowired fields/setters)
5. BeanPostProcessor.postProcessBeforeInitialization()
6. @PostConstruct method called
7. InitializingBean.afterPropertiesSet() (if implemented)
8. BeanPostProcessor.postProcessAfterInitialization()  ← AOP proxies created here
9. Bean is ready

Destruction:
  @PreDestroy called
  DisposableBean.destroy() called
  Application shutdown
```

### Spring AOP
```java
// AOP: adds cross-cutting behaviour without modifying the target class
// Spring creates a proxy (JDK dynamic proxy or CGLIB subclass)

@Aspect
@Component
public class LoggingAspect {

    // Pointcut: which methods to intercept
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceLayer() {}

    // Around advice: wraps the method call
    @Around("serviceLayer()")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();  // call the actual method
            long duration = System.currentTimeMillis() - start;
            log.info("{} completed in {}ms", method, duration);
            return result;
        } catch (Exception e) {
            log.error("{} failed: {}", method, e.getMessage());
            throw e;
        }
    }
}

// GOTCHA: AOP only works on Spring-managed beans
// Calling a @Transactional method from WITHIN the same class bypasses the proxy!
// this.doSomething() ← doesn't go through proxy
// Fix: inject self, or move to a separate bean
```

### @Transactional — How It Really Works
```java
@Service
public class OrderService {

    @Transactional  // Spring wraps this in a transaction via AOP proxy
    public void placeOrder(Order order) {
        orderRepo.save(order);
        paymentService.charge(order);  // if this throws → rollback saves too
    }

    // GOTCHA 1: self-invocation bypass
    public void processOrders(List<Order> orders) {
        orders.forEach(this::placeOrder);  // won't be transactional! proxy bypassed
        // Fix: @Autowired OrderService self; self.placeOrder(o);
    }

    // GOTCHA 2: checked exceptions don't trigger rollback by default
    @Transactional  // only rolls back on RuntimeException
    public void doWork() throws IOException { ... }  // IOException = no rollback!
    // Fix: @Transactional(rollbackFor = Exception.class)

    // GOTCHA 3: propagation
    @Transactional(propagation = Propagation.REQUIRED)    // default: join existing or create
    @Transactional(propagation = Propagation.REQUIRES_NEW) // always new transaction
    @Transactional(propagation = Propagation.MANDATORY)    // must have existing, else error
}
```

### Auto-Configuration
```
How @SpringBootApplication works:
  @SpringBootConfiguration   → marks as configuration class
  @EnableAutoConfiguration   → enables auto-config magic
  @ComponentScan             → scans current package and sub-packages

Auto-configuration mechanism:
  spring.factories (META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
  Lists hundreds of XXXAutoConfiguration classes
  Each has @ConditionalOnClass, @ConditionalOnMissingBean, @ConditionalOnProperty
  Only activates when conditions are met (e.g., DataSourceAutoConfiguration only if Datasource.class on classpath)

To debug what's being configured:
  --debug flag → prints autoconfiguration report
  actuator /actuator/conditions endpoint
```

---

## Part 5: Kafka with Spring Boot

### Consumer Groups and Offset Management
```java
@KafkaListener(
    topics = "orders",
    groupId = "order-processor",
    containerFactory = "kafkaListenerContainerFactory"
)
public void handleOrder(@Payload OrderEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment ack) {
    try {
        processOrder(event);
        ack.acknowledge();  // manual commit after successful processing
    } catch (RetryableException e) {
        // Don't ack — message will be redelivered
        throw e;
    } catch (PermanentException e) {
        // Ack anyway to move past poison pill, send to DLT
        deadLetterPublisher.send(event, e);
        ack.acknowledge();
    }
}

// Configuration: manual ack mode for at-least-once semantics
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderEvent>();
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.setConcurrency(3);  // 3 consumer threads (match partition count)
    return factory;
}
```

### Producer Patterns
```java
@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(Order order) {
        OrderEvent event = OrderEvent.from(order);
        // Key = orderId → ensures same order always goes to same partition (ordering)
        ProducerRecord<String, OrderEvent> record =
            new ProducerRecord<>("orders", order.getId(), event);

        // Async with callback
        kafkaTemplate.send(record)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish order {}: {}", order.getId(), ex.getMessage());
                    // retry or alert
                } else {
                    log.debug("Published order {} to partition {} offset {}",
                        order.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}

// Producer config for reliability
spring.kafka.producer.acks=all             # wait for all ISR replicas
spring.kafka.producer.retries=3
spring.kafka.producer.enable.idempotence=true  # exactly-once per session
```

---

## Part 6: Common Interview Questions

**Q: What is the difference between `HashMap` and `ConcurrentHashMap`?**
A: `HashMap`: not thread-safe, O(1) average get/put, allows one null key. `ConcurrentHashMap`: thread-safe via segmented locking (Java 8: CAS + synchronized on individual buckets), O(1) average, no null keys/values. `ConcurrentHashMap` does NOT lock the entire map — only the bucket being modified. `size()` is approximate (sums segment counts). `computeIfAbsent()` is atomic. `HashMap` is faster in single-threaded code; use `ConcurrentHashMap` in any shared-state scenario.

**Q: Explain `String` immutability and the String pool.**
A: `String` objects are immutable — once created, their content cannot change. This enables: thread safety (no synchronization needed), caching (String pool), security (class names, passwords can't be modified after validation). The String pool (interning): `String s = "hello"` → checks pool first, returns existing reference if found. `new String("hello")` → always creates a new object (bypasses pool). `s.intern()` → adds to pool manually. Implication: `==` compares references (use `.equals()` for content), but interned strings can use `==`.

**Q: What is a memory leak in Java and how do you find one?**
A: Java has GC so explicit deallocation isn't needed, but memory leaks still happen when objects are held in memory longer than needed. Common causes: (1) Static collections that grow without bound. (2) Listeners/callbacks not deregistered. (3) ThreadLocal variables not removed. (4) Caches without eviction policy. (5) Long-lived references to large objects in method chains. Detection: heap dump analysis (VisualVM, Eclipse MAT), monitoring heap growth with JVM flags `-verbose:gc`, Micrometer JVM metrics in production.

**Q: How does Spring's dependency injection work internally?**
A: On startup, Spring scans for `@Component`, `@Service`, `@Repository`, `@Controller`. For each, it creates a `BeanDefinition` (metadata: class, scope, dependencies). At wiring time: for each dependency (`@Autowired`), Spring searches `BeanFactory` by type (then name if ambiguous). If the dependency is an interface with multiple implementations, it needs `@Qualifier` or `@Primary`. For circular dependencies: Spring can resolve field injection or setter injection (creates a partially-initialised proxy), but NOT constructor injection (throws `BeanCurrentlyInCreationException`). This is why constructor injection is preferred — it makes circular dependencies fail fast.

**Q: What is N+1 query problem in Spring Data JPA, and three ways to fix it?**
A: When fetching a collection of N entities and each entity triggers an additional query for a related entity. Example: fetching 100 Users, then for each User fetching their Orders = 101 queries. Fix 1: JPQL JOIN FETCH: `@Query("SELECT u FROM User u JOIN FETCH u.orders")` — single query with JOIN. Fix 2: `@EntityGraph(attributePaths = "orders")` on repository method — tells Hibernate to eagerly load in same query. Fix 3: `@BatchSize(size = 25)` on the collection — instead of N queries, does N/25 queries (batches). Fix 4: use a DTO projection (`@Query` returning a record/interface) — only fetches fields you need, no lazy loading.
