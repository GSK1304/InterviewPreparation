# 📋 LLD Cheatsheet — Everything on One Page

---

## OOP Four Pillars — Interview-Ready Definitions

| Pillar | One-line | Violation Signal | Fix Signal |
|--------|----------|-----------------|------------|
| **Encapsulation** | Hide state, expose behaviour | `public` fields, no validation | `private` fields + validated setters/methods |
| **Abstraction** | Interface hides implementation | Caller knows HOW, not WHAT | `interface` / `abstract class` as the type |
| **Inheritance** | IS-A → reuse + extend | `extends` for code reuse only | Composition if IS-A test fails |
| **Polymorphism** | Same call, different behaviour | `instanceof` chains everywhere | Abstract method + override; sealed + switch |

---

## SOLID — One Violation + One Fix Each

```
S — Single Responsibility
  Violation: Order class saves to DB + sends email + formats HTML
  Fix:       Order, OrderRepository, OrderNotifier, OrderFormatter — 4 classes

O — Open/Closed
  Violation: switch(discountType) { case "STUDENT": ... case "SENIOR": ... }
  Fix:       DiscountStrategy interface + StudentDiscount, SeniorDiscount implementations

L — Liskov Substitution
  Violation: Square extends Rectangle → setWidth() breaks area contract
  Fix:       Sealed interface Shape | Square | Rectangle — no shared mutable state

I — Interface Segregation
  Violation: Robot implements Worker { void eat() { throw new UnsupportedOperationException(); } }
  Fix:       Workable, Eatable, Sleepable — separate interfaces; Robot implements Workable only

D — Dependency Inversion
  Violation: OrderService { MySQLOrderRepo repo = new MySQLOrderRepo(); }
  Fix:       OrderService(OrderRepository repo) — inject abstraction via constructor
```

---

## Design Pattern Quick-Select

### Creational
```
Need ONE instance globally           → Singleton (enum-based)
Create without knowing exact type    → Factory Method
Family of related objects            → Abstract Factory
Complex object step-by-step          → Builder (inner static class + validate in build())
Copy existing expensive object       → Prototype (deep clone)
```

### Structural
```
Incompatible interfaces to work together → Adapter (wraps the foreign class)
Add behaviour without subclassing        → Decorator (wraps same interface)
Simplify complex subsystem               → Facade (one clean entry point)
Control access / add cross-cutting       → Proxy (caching, logging, security)
Tree structures (file system, menu)      → Composite (leaf + composite same interface)
Many small objects sharing state         → Flyweight (intrinsic shared, extrinsic passed in)
Vary abstraction AND implementation      → Bridge (two separate hierarchies)
```

### Behavioural
```
Notify many objects on change      → Observer (EventBus<T>)
Swap algorithms at runtime         → Strategy (@FunctionalInterface)
Encapsulate request (undo/redo)    → Command (execute + undo)
Behaviour depends on internal state→ State (each state is a class)
Algorithm skeleton, fill steps in  → Template Method (final in base, abstract steps)
Chain of handlers, pass till handled→ Chain of Responsibility (middleware)
Sequential access to collection    → Iterator (hasNext + next)
```

---

## Java 21 Features — When to Use Each

```java
// Record — immutable value object, auto equals/hashCode/toString
record Money(long paise) {
    Money { if (paise < 0) throw new IllegalArgumentException("No negative money"); }
}

// Sealed interface — exhaustive type hierarchy, compiler enforces switch coverage
sealed interface BoardEntity permits Snake, Ladder, Empty {}
record Snake(int head, int tail) implements BoardEntity {}
record Ladder(int bottom, int top) implements BoardEntity {}
record Empty() implements BoardEntity {}

// Pattern matching switch — replaces instanceof chains
String describe(BoardEntity entity) {
    return switch (entity) {
        case Snake  s -> "Snake from " + s.head() + " to " + s.tail();
        case Ladder l -> "Ladder from " + l.bottom() + " to " + l.top();
        case Empty  e -> "Empty cell";
        // No default needed — sealed ensures exhaustiveness
    };
}

// Pattern matching instanceof — no explicit cast
if (shape instanceof Circle c && c.radius() > 0) {
    System.out.println("Circle radius: " + c.radius());
}

// Text block — multiline strings
String json = """
    {
        "name": "Alice",
        "amount": 1500
    }
    """;

// var — local type inference (use when type is obvious from context)
var activeLoans = new HashMap<String, Loan>();

// Switch expression (not just statement)
int tierMultiplier = switch (seatTier) {
    case RECLINER  -> 3;
    case PREMIUM   -> 2;
    case EXECUTIVE -> 1;
    case NORMAL    -> 1;
};
```

---

## Thread Safety Patterns — When and How

```java
// Pattern 1: synchronized method — for simple shared state
class ParkingSpot {
    synchronized boolean tryOccupy(VehicleType type) {
        if (status != AVAILABLE) return false;
        status = OCCUPIED;
        return true;
    }
}

// Pattern 2: ConcurrentHashMap — shared map across threads
Map<String, Booking> activeBookings = new ConcurrentHashMap<>();

// Pattern 3: AtomicInteger / AtomicLong — lock-free counter
AtomicInteger ticketCounter = new AtomicInteger(1000);
String id = "TKT-" + ticketCounter.getAndIncrement();

// Pattern 4: CopyOnWriteArrayList — for infrequent writes, frequent reads
List<EventListener> listeners = new CopyOnWriteArrayList<>();

// Pattern 5: volatile — visibility across threads without mutual exclusion
private volatile SpotStatus status; // reads always see latest written value

// When to use which:
// synchronized → protecting a critical section with multiple operations
// ConcurrentHashMap → concurrent map access (much faster than synchronized Map)
// AtomicInteger → single numeric counter, lock-free
// CopyOnWriteArrayList → event listeners (rare add, frequent iteration)
// volatile → single-flag visibility without need for atomicity
```

---

## Validation Checklist — Every Class You Write

```
Constructor:
  ✅ null check all required params (Objects.requireNonNull)
  ✅ blank check all String params
  ✅ range check all numeric params
  ✅ format check (email, phone, ISBN, license plate)
  ✅ business rule check (checkOut > checkIn, capacity > 0)
  ✅ cross-field validation (POST request requires body)

Methods:
  ✅ null check parameters
  ✅ state precondition check (e.g., must be AVAILABLE before OCCUPIED)
  ✅ idempotency: what happens if called twice?
  ✅ rollback on partial failure (release locked seats on error)

Exceptions:
  ✅ domain-specific exception (not raw RuntimeException)
  ✅ meaningful message with context (what was expected, what was found)
  ✅ hierarchy: checked for recoverable, unchecked for programmer errors
```

---

## Custom Exception Hierarchy Pattern

```java
// Base domain exception
class DomainException extends RuntimeException {
    DomainException(String msg) { super(msg); }
}

// Specific exceptions
class EntityNotFoundException extends DomainException {
    EntityNotFoundException(String entity, String id) {
        super(entity + " not found: " + id);
    }
}

class InvalidStateTransitionException extends DomainException {
    InvalidStateTransitionException(String expected, Object actual) {
        super("Expected state " + expected + " but was: " + actual);
    }
}

class BusinessRuleViolationException extends DomainException {
    BusinessRuleViolationException(String rule) {
        super("Business rule violated: " + rule);
    }
}

// Usage — specific, readable, catchable
throw new EntityNotFoundException("Booking", bookingId);
throw new InvalidStateTransitionException("CONFIRMED", booking.getStatus());
throw new BusinessRuleViolationException("Borrow limit of 3 books exceeded");
```

---

## Money Pattern — Never Use double/float for Currency

```java
// WRONG — floating point errors
double total = 0.1 + 0.2;  // = 0.30000000000000004 ❌

// RIGHT — store in smallest unit (paise for INR, cents for USD)
record Money(long paise) {
    static Money ofRupees(double rupees) {
        return new Money(Math.round(rupees * 100));
    }
    Money add(Money o)          { return new Money(paise + o.paise); }
    Money subtract(Money o)     {
        if (o.paise > paise) throw new IllegalArgumentException("Insufficient funds");
        return new Money(paise - o.paise);
    }
    Money multiply(double f)    { return new Money(Math.round(paise * f)); }
    double toRupees()           { return paise / 100.0; }
    @Override public String toString() { return String.format("₹%.2f", toRupees()); }
}

// Rounding: when splitting, last person gets the remainder
long perPerson = total.paise() / n;
long remainder = total.paise() % n;
// First 'remainder' participants get one extra paise
```

---

## Builder Pattern — Standard Template

```java
public final class Config {
    // All fields final — immutable after build()
    private final String  host;
    private final int     port;
    private final Duration timeout;

    private Config(Builder b) {
        this.host    = b.host;
        this.port    = b.port;
        this.timeout = b.timeout;
    }

    public static Builder newBuilder() { return new Builder(); }

    public static final class Builder {
        private String   host    = "localhost";  // defaults
        private int      port    = 8080;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder host(String host) {
            if (host == null || host.isBlank()) throw new IllegalArgumentException("host required");
            this.host = host; return this;
        }
        public Builder port(int port) {
            if (port < 1 || port > 65535) throw new IllegalArgumentException("invalid port");
            this.port = port; return this;
        }
        public Builder timeout(Duration t) {
            if (t.isNegative() || t.isZero()) throw new IllegalArgumentException("timeout must be positive");
            this.timeout = t; return this;
        }
        public Config build() {
            // Cross-field validation
            if ("prod".equals(host) && timeout.getSeconds() < 5)
                throw new IllegalStateException("Production needs timeout >= 5s");
            return new Config(this);
        }
    }
}
```

---

## State Pattern — Standard Template

```java
// Step 1: Define state interface
interface OrderState {
    void confirm(Order order);
    void ship(Order order);
    void deliver(Order order);
    void cancel(Order order);
}

// Step 2: Implement each state
class PendingState implements OrderState {
    @Override public void confirm(Order order) {
        order.setState(new ConfirmedState());
        System.out.println("Order confirmed");
    }
    @Override public void ship(Order o)    { throw new IllegalStateException("Must confirm first"); }
    @Override public void deliver(Order o) { throw new IllegalStateException("Must ship first"); }
    @Override public void cancel(Order o)  { o.setState(new CancelledState()); }
}

// Step 3: Context delegates to current state
class Order {
    private OrderState state = new PendingState();
    void setState(OrderState s) { this.state = s; }
    void confirm() { state.confirm(this); }
    void ship()    { state.ship(this); }
    void deliver() { state.deliver(this); }
    void cancel()  { state.cancel(this); }
}
```

---

## Strategy Pattern — Standard Template

```java
// Step 1: Strategy interface (often @FunctionalInterface)
@FunctionalInterface
interface NotificationStrategy {
    void send(String recipient, String message);
}

// Step 2: Concrete strategies
class EmailStrategy implements NotificationStrategy {
    @Override public void send(String to, String msg) { /* SMTP */ }
}
class SmsStrategy implements NotificationStrategy {
    @Override public void send(String to, String msg) { /* Twilio */ }
}
class PushStrategy implements NotificationStrategy {
    @Override public void send(String to, String msg) { /* FCM */ }
}

// Step 3: Context — injected, switchable
class NotificationService {
    private NotificationStrategy strategy;
    NotificationService(NotificationStrategy strategy) { this.strategy = strategy; }
    void setStrategy(NotificationStrategy s) { this.strategy = s; }
    void notify(String recipient, String message) { strategy.send(recipient, message); }
}

// Usage
NotificationService svc = new NotificationService(new EmailStrategy());
svc.notify("user@example.com", "Your order shipped");
svc.setStrategy(new SmsStrategy());  // switch at runtime
```

---

## Observer Pattern — Standard Template (EventBus)

```java
// Generic, decoupled event bus
class EventBus<T> {
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    void subscribe(Consumer<T> listener) { listeners.add(listener); }
    void unsubscribe(Consumer<T> listener) { listeners.remove(listener); }

    void publish(T event) {
        listeners.forEach(l -> {
            try { l.accept(event); }
            catch (Exception e) { /* isolate bad listeners */ }
        });
    }
}

// Domain events as records
record OrderPlaced(String orderId, double total) {}

// Usage
EventBus<OrderPlaced> bus = new EventBus<>();
bus.subscribe(e -> inventory.reserve(e.orderId()));    // lambda as listener
bus.subscribe(e -> email.sendConfirmation(e.orderId()));
bus.publish(new OrderPlaced("ORD-1", 599.0));
```

---

## LLD Interview Problem Approach (5 Steps, 45 Minutes)

```
Step 1 — Clarify (5 min)
  Ask: What are the core features? What scale? Multi-user? Persistence needed?
  Identify: actors (who uses the system), core use cases (what they do)

Step 2 — Identify Entities (5 min)
  Nouns from requirements → candidate classes
  Eliminate: things that are just attributes of other classes
  Keep: things with behaviour, own identity, or lifecycle

Step 3 — Class Relationships (5 min)
  IS-A → inheritance (only if truly substitutable)
  HAS-A (dies together) → composition
  HAS-A (lives independently) → aggregation
  USES (temporary) → dependency / method parameter

Step 4 — Apply Patterns (5 min)
  "Multiple algorithms that swap" → Strategy
  "Add behaviour without changing class" → Decorator
  "Object changes behaviour based on state" → State
  "One change notifies many" → Observer
  "Build complex object step by step" → Builder
  "Control access, add cross-cutting" → Proxy

Step 5 — Code Core Classes (25 min)
  Start with: enums, records, exceptions (5 min)
  Then: main entity classes with validation (10 min)
  Then: service class with core operations (10 min)
  Always write: main() demo showing the happy path + one error case
```

---

## Common Mistakes in LLD Interviews

| Mistake | Better Approach |
|---------|----------------|
| Using `double` for money | `long paise` / `long cents` record |
| Public fields with no validation | `private` + validated methods |
| God class with 20 methods | Split by SRP — one class, one reason to change |
| Using inheritance to reuse code | Composition — inject the dependency |
| Raw `RuntimeException` | Domain-specific exception with context message |
| No null checks on parameters | `Objects.requireNonNull()` in every constructor |
| Single if/else for states | State pattern — each state is a class |
| Switch on type string | Polymorphism / sealed classes |
| Forgetting thread safety | `synchronized` on shared mutable state; `ConcurrentHashMap` |
| No `main()` demo | Always show it works end-to-end |
