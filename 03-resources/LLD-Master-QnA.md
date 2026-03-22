# 📋 LLD Master Interview Q&A — 30 Most Common Questions

> Complete model answers. Read before every interview.

---

## Category 1: OOP & SOLID (Q1–Q10)

**Q1: What is the difference between an abstract class and an interface? When do you choose each?**

Abstract class: can have state (fields), constructors, concrete methods, and abstract methods. Supports single inheritance only. Use when you want to share both code AND state between closely related classes — like `AbstractList` which holds `modCount` and provides default implementations for `iterator()` and `listIterator()`.

Interface: no instance state, no constructors. All methods implicitly public. Supports multiple implementation. Use when defining a contract for unrelated classes — `Comparable`, `Runnable`, `Serializable` are implemented by completely unrelated types. Java 8+ `default` methods blur the line, but the key distinction remains: abstract classes carry instance state; interfaces cannot.

Rule of thumb: prefer interfaces for type declarations. Use abstract classes only when partial implementation sharing with state is genuinely needed.

---

**Q2: Explain the Liskov Substitution Principle with a real example from your code.**

LSP says: if S is a subtype of T, you must be able to substitute S wherever T is expected, without breaking correctness. The classic violation is `Square extends Rectangle` — setting width independently breaks the area contract for callers who hold a `Rectangle` reference.

From our systems: `ParkingSpot` subclasses (if we had them for different spot types) must respect the `tryOccupy(VehicleType)` contract. A `ElectricChargingSpot extends ParkingSpot` is LSP-safe if it still accepts `ELECTRIC` vehicles correctly. But if it throws `UnsupportedOperationException` for non-electric vehicles instead of returning `false` — that's an LSP violation. The caller expects a boolean, not an exception.

Detection pattern: if a subclass has empty/no-op overrides, throws exceptions for base class operations, or requires callers to check the type before calling — LSP is violated.

---

**Q3: You see this code: `if (employee.getType().equals("MANAGER")) { ... } else if (...DEVELOPER...) { ... }`. What's wrong and how do you fix it?**

This is the "type code" smell — behaviour that belongs in subclasses is centrally dispatched by type. Every new employee type requires modifying this method. It violates OCP and SRP.

Fix: replace with polymorphism. `Employee` becomes an interface or abstract class with an abstract method `calculateBonus()`. `Manager` and `Developer` implement it with their own logic. The if/else disappears — callers just call `employee.calculateBonus()` and the right implementation runs.

In Java 21: if `Employee` is sealed — `sealed interface Employee permits Manager, Developer, Intern` — then a switch expression handles it exhaustively with compile-time safety. Adding a new type forces updating every switch, which is the right behaviour.

---

**Q4: What is the Dependency Inversion Principle and how does it relate to testability?**

DIP: high-level modules should not depend on low-level modules — both should depend on abstractions. In practice: `OrderService` should not `new MySQLOrderRepository()` inside itself. It should accept an `OrderRepository` interface via constructor injection.

Testability connection: when `OrderService` creates its own dependencies, you cannot test it without a real MySQL database. But when you inject an `OrderRepository` interface, your test passes `new InMemoryOrderRepository()` — no database needed. This is why constructor injection is the default approach in Spring: it makes dependencies explicit and the class testable in isolation.

The rule: if you find yourself saying "I need to spin up a database/server to test this class", DIP is being violated somewhere.

---

**Q5: When do you use composition over inheritance? Give an example from a machine coding problem.**

Composition when: (1) you want to reuse code but the IS-A test fails, (2) you need to change behaviour at runtime, (3) you want to combine multiple behaviours.

From our Parking Lot: `ParkingLot` HAS-A `SpotSelectionStrategy` (composition). Not `ParkingLot extends NearestFloorStrategy` (inheritance). The lot uses a strategy, doesn't IS-A strategy. This lets us swap from `NearestFloorStrategy` to `LoadBalancedStrategy` at runtime without changing `ParkingLot`.

From our Hotel: `BreakfastIncluded extends RoomAmenityDecorator` wraps a `RoomService` — Decorator pattern is composition. We stack `new SpaAccess(new BreakfastIncluded(room))`. With inheritance you'd need `RoomWithBreakfast`, `RoomWithSpa`, `RoomWithBreakfastAndSpa` — 2^N subclasses for N amenities.

---

**Q6: Explain encapsulation with an example showing what breaks without it.**

Without encapsulation: `public double balance;` on `BankAccount`. Any code anywhere can do `account.balance = -999999`. No validation, no audit trail, no invariant protection.

With encapsulation: `private double balance;` with `withdraw(double amount)` that validates amount > 0, amount ≤ balance, logs the transaction, and updates balance atomically. The `balance` field invariant (always ≥ 0) is guaranteed by the class itself — callers cannot violate it.

The rule: ask "what invariant must this class always maintain?" then write the class so callers cannot violate that invariant. Tell, Don't Ask — don't expose data for callers to make decisions; give them methods that make decisions.

---

**Q7: What is the "Tell, Don't Ask" principle?**

Instead of asking an object for its data to make a decision externally, tell the object to make the decision itself.

Asking (bad):
```java
if (order.getStatus() == PENDING) {
    order.setStatus(CONFIRMED);
    emailService.sendConfirmation(order.getEmail());
}
```

Telling (good):
```java
order.confirm();  // Order knows how to confirm itself, validate state, send email
```

The first scatters business logic across callers. The second keeps it in the domain object. Anemic domain models (objects with only getters/setters) are a sign of violating this principle. In LLD interviews, objects with rich behaviour score higher than DTOs.

---

**Q8: What is polymorphism and how do sealed classes improve it in Java 21?**

Polymorphism: one interface, multiple implementations — the right method is called based on the actual runtime type, not the declared type.

Before Java 21: `instanceof` chains were the only way to handle a type hierarchy exhaustively. They compile without error even if you forget a type. A new `Triangle` class compiles fine even if no `instanceof Triangle` check exists anywhere.

With sealed classes: `sealed interface Shape permits Circle, Rectangle, Triangle`. Every switch expression must handle all three. Add `Hexagon` to permits without adding it to the switch → compile error. This makes the type hierarchy self-documenting and refactoring-safe.

From our Snake & Ladder: `sealed interface BoardEntity permits Snake, Ladder, Empty` — the game loop switch is guaranteed to handle every cell type. If we add `Teleporter` to permits, every switch that handles `BoardEntity` becomes a compile error, forcing us to address it.

---

**Q9: You're reviewing a PR where a developer created 15 different subclasses of PaymentProcessor, one per country. What's the issue and how do you fix it?**

The issue: inheritance explosion. Each "variation" (country) is a subclass, but countries differ only in configuration (currency, tax rate, validation rules) — not in fundamental behaviour. Adding a new country requires a new class file, a new deployment, and a code review.

Fix: Strategy + configuration. One `PaymentProcessor` class accepts a `CountryConfig` record:

```java
record CountryConfig(Currency currency, double taxRate, PhoneValidator validator) {}
```

Create a `CountryConfigRegistry` that maps country codes to configs. New country = add a config entry, no new class. The Strategy handles validation and formatting per country. 15 classes → 1 class + 15 config objects.

---

**Q10: How do you apply SOLID when designing a notification system that needs to support email, SMS, and push?**

S: `NotificationService` handles sending. `TemplateEngine` handles formatting. `DeliveryTracker` handles tracking. Separate classes.

O: New channel (WhatsApp) = new `WhatsAppNotificationStrategy` class. Zero changes to `NotificationService`.

L: `EmailStrategy`, `SmsStrategy`, `PushStrategy` all implement `NotificationStrategy`. Each is fully substitutable — same contract.

I: `NotificationStrategy` has just `send(recipient, message)`. If SMS needs `sendBulk()` and email needs `sendWithAttachment()`, those go on separate interfaces — don't force email to implement `sendBulk()`.

D: `NotificationService` depends on `NotificationStrategy` interface, not `SmtpClient` or `TwilioClient`. Those concrete clients are injected.

---

## Category 2: Design Patterns (Q11–Q20)

**Q11: What's the difference between Strategy and State patterns?**

Both encapsulate behaviour behind an interface. The difference is who changes the behaviour and why.

Strategy: behaviour is selected and injected externally. The object doesn't change its strategy on its own. The context doesn't know which strategy it has — it just delegates. Example: `FareCalculator` injected into `CabBookingService` — the service doesn't decide which fare strategy to use at runtime, it was configured at construction.

State: the object itself transitions between states based on what happens to it. States know about each other and trigger transitions. Example: `Ride` transitions from `REQUESTED` to `DRIVER_ASSIGNED` to `IN_PROGRESS` — these transitions are triggered by domain events, not external injection.

Test: if the "strategy" changes based on external configuration → Strategy. If the "strategy" changes because something happened to the object → State.

---

**Q12: When would you use Decorator instead of subclassing?**

Use Decorator when: (1) you need optional, combinable add-ons to an existing class, (2) you can't modify the class (final or from a library), (3) inheritance would cause a combinatorial explosion.

From our Hotel: `BreakfastIncluded`, `ParkingIncluded`, `SpaAccess` are all optional and stackable. With inheritance: `RoomWithBreakfast`, `RoomWithParking`, `RoomWithSpa`, `RoomWithBreakfastAndParking`... 7 subclasses for 3 amenities (2³ - 1). With Decorator: `new SpaAccess(new BreakfastIncluded(room))` — compose exactly what you need.

Use subclassing when the new behaviour is fundamental to ALL instances of that type, and you have one or two variants.

---

**Q13: You need to build a logger that supports multiple outputs (console, file, database). Which design pattern do you use and why?**

Chain of Responsibility — each logger in the chain decides whether to log and whether to pass to the next.

```java
abstract class Logger {
    protected Logger next;
    Logger setNext(Logger next) { this.next = next; return next; }
    void log(LogLevel level, String message) {
        if (shouldHandle(level)) handle(message);
        if (next != null) next.log(level, message);
    }
    abstract boolean shouldHandle(LogLevel level);
    abstract void handle(String message);
}
class ConsoleLogger extends Logger { ... }
class FileLogger extends Logger { ... }
class DatabaseLogger extends Logger { ... }
```

Chain: `console.setNext(file).setNext(database)`. FATAL logs hit all three. DEBUG logs hit only console (configured per handler). Adding a new handler = new class, zero changes to existing loggers (OCP). Also: Observer pattern works if all handlers always receive every log — fire-and-forget to all subscribers.

---

**Q14: What is the Observer pattern? How is it different from direct method calls?**

Observer: subject maintains a list of observers. When state changes, it notifies all observers without knowing who they are. Observers subscribe and unsubscribe independently.

Direct calls:
```java
game.score();
soundSystem.playScoreSound();  // game knows about soundSystem
leaderboard.update();          // game knows about leaderboard
analytics.track();             // game knows about analytics
```

With Observer:
```java
eventBus.publish(new ScoreEvent(points));  // game knows only about EventBus
// soundSystem, leaderboard, analytics subscribe independently
```

Adding a new reaction to score (e.g., achievement unlock) = add one subscriber, zero changes to the game. The game is closed for modification, open for extension.

---

**Q15: You're designing an ATM that dispenses ₹2000, ₹500, ₹200, and ₹100 notes. Which pattern fits?**

Chain of Responsibility. Each denomination is a handler. The request passes through the chain; each handler dispenses as many notes as it can, then passes the remainder.

```java
// ₹2000 handler:
int notes = (int)(amount / 2000);
amount -= notes * 2000;
if (next != null && amount > 0) next.dispense(amount);
```

Benefits: adding ₹50 notes = add one new handler class at the end of the chain (OCP). Each handler is independently testable. Order of the chain determines denomination preference (largest first = fewest notes).

Alternative considered: Strategy with pre-computing the combination. Chain of Responsibility is better because it naturally handles the sequential, remainder-passing nature of the problem.

---

**Q16: What is the Builder pattern and when does it solve a real problem?**

Builder solves the telescoping constructor anti-pattern: a class with many optional parameters leads to constructors like `new HttpRequest(url, method, null, null, null, timeout, 3)` — impossible to read.

Builder gives: named parameters, validation at build time, immutable result, readable construction.

Real problem it solves (from our code):
```java
HttpRequest request = HttpRequest.newBuilder("https://api.example.com/orders")
    .method("POST")
    .header("Content-Type", "application/json")
    .body(orderJson)
    .timeout(Duration.ofSeconds(10))
    .build();  // validates: POST requires body
```

Cross-field validation in `build()`: `POST` without a body → `IllegalStateException`. This validation cannot happen in individual setters because they don't know what the final combination will be. Only `build()` sees the complete picture.

---

**Q17: Singleton — when is it a good idea and when is it harmful?**

Good: when there truly must be exactly one instance and it's expensive to create or must be shared — thread pool, connection pool, configuration manager, logging system.

Harmful: when used as a lazy global variable bag. Singletons that hold mutable state become hidden dependencies — classes that use them can't be tested in isolation. `UserSession.getInstance().getUserId()` makes any class that calls it untestable without a real session.

Rule: Singleton is fine for infrastructure (connection pool, config). Avoid for domain objects or anything you want to mock in tests.

Java best practice: enum-based Singleton:
```java
public enum ConfigManager {
    INSTANCE;
    // Thread-safe by JVM, serialisation-safe, reflection-safe
    public String get(String key) { ... }
}
```

---

**Q18: You need to wrap a third-party payment SDK (Stripe) behind your own interface. Which pattern?**

Adapter. Your system has `PaymentProcessor.process(PaymentRequest)`. Stripe has `StripeClient.charge(currency, amountCents, token)`.

```java
public class StripeAdapter implements PaymentProcessor {
    private final StripeClient stripe;

    @Override
    public PaymentResult process(PaymentRequest request) {
        StripeCharge charge = stripe.charge(
            request.currency().code(),
            request.amount().paise(),
            request.token()
        );
        return PaymentResult.success(charge.getId());
    }
}
```

Your code only knows `PaymentProcessor`. Switching from Stripe to Razorpay = write `RazorpayAdapter`, change one injection line. Zero changes to business logic.

---

**Q19: What is the Template Method pattern? Give a real example from the codebase.**

Template Method defines the algorithm skeleton in a base class using a `final` method. Concrete steps are `abstract` or overridable.

From our Library Management: `DataImporter` has:
```java
public final ImportResult importData(InputStream source) {
    List<RawRecord> raw    = readRecords(source);   // abstract — must implement
    List<RawRecord> valid  = validateRecords(raw);  // hook — can override
    List<DomainObject> parsed = parseRecords(valid); // abstract — must implement
    persistRecords(parsed);                           // final — cannot override
}
```

`CsvDataImporter` implements `readRecords()` and `parseRecords()` for CSV. `JsonDataImporter` does the same for JSON. The import algorithm (read → validate → parse → persist) is fixed. Only the format-specific steps vary. Adding XML support = one new class implementing two methods.

---

**Q20: What is Proxy pattern and how does Spring use it?**

Proxy: a surrogate object that controls access to the real object — can add caching, logging, security checks, lazy initialization, or remote communication without changing the real object.

Spring's `@Transactional` is a proxy. Spring generates a proxy class around your `@Service`. When you call `orderService.placeOrder(order)`, you're actually calling the proxy's method. The proxy: begins a transaction, calls your real method, commits or rolls back based on outcome. Your code knows nothing about transactions — it just has `@Transactional`.

Spring's `@Cacheable` works the same way — the proxy checks the cache first, calls your method only on a miss, stores the result.

Java dynamic proxies: `java.lang.reflect.Proxy.newProxyInstance(...)` — creates a proxy at runtime for any interface. Used in Spring, Hibernate, JDK's own `Collections.unmodifiableList()`.

---

## Category 3: Machine Coding Problems (Q21–30)

**Q21: In the Parking Lot problem, how do you prevent two threads from assigning the same spot?**

The race condition: Thread A checks spot.status == AVAILABLE, Thread B checks spot.status == AVAILABLE, both proceed to assign the same spot.

Fix: make the check-and-set atomic with `synchronized`:
```java
synchronized boolean tryOccupy(VehicleType type) {
    if (status != AVAILABLE) return false;   // check
    if (!type.fitsIn(size)) return false;
    status = OCCUPIED;                        // set
    return true;                             // both in ONE synchronized block
}
```

Thread A acquires the monitor, checks AVAILABLE, sets OCCUPIED, returns true. Thread B waits for the monitor. When it enters, status is OCCUPIED — returns false. Spot is assigned to exactly one thread.

---

**Q22: In the Chess problem, why does each piece validate its own moves instead of a central validator?**

Single Responsibility + Open/Closed. Each piece has one reason to change: its movement rules. A `Knight` changes only when knight movement rules change. A central `MoveValidator` with a switch on piece type changes every time any piece changes — it accumulates all reasons to change.

OCP: adding a new piece type (e.g., Chancellor — moves as Rook or Knight) = add a new class implementing `getLegalMoves(Board)`. Zero changes to `Board`, `Game`, or any existing piece. With a central validator, you add another `case CHANCELLOR:` to the switch — modifying existing code.

The Board's `isInCheck()` and `isUnderAttack()` use polymorphism: they call `piece.getLegalMoves(board)` on every piece without knowing its type.

---

**Q23: How does Splitwise calculate the minimum number of transactions to settle all debts?**

Greedy algorithm on net positions:

1. Compute net balance per person: positive = net creditor (owed money), negative = net debtor (owes money).
2. Put creditors in a max-heap (by amount owed), debtors in a min-heap (by amount owed, most negative first).
3. Greedily match: take the largest creditor and largest debtor. The smaller amount settles. If they're equal — both resolved. If not — one is partially settled and goes back in the heap.
4. Continue until both heaps are empty.

Time complexity: O(N log N). This gives a near-optimal solution. The truly optimal (fewest transactions) is NP-hard in the general case — the greedy approach is the practical answer and what interviewers expect.

---

**Q24: In BookMyShow, what happens if a user locks seats but never completes payment?**

Without TTL: locked seats are stuck forever — no one else can book them. The show appears sold out when it isn't.

Solution: lock with a TTL (Time To Live). In our implementation:
```java
// Seat.tryLock() stores lockExpiry = Instant.now() + Duration.ofMinutes(5)
// seat.isAvailable() calls releaseLockIfExpired() before checking
void releaseLockIfExpired() {
    if (status == LOCKED && Instant.now().isAfter(lockExpiry)) {
        status = AVAILABLE; lockBookingId = null;
    }
}
```

Every availability check self-heals expired locks. No background thread needed for small scale. Production: Redis `SET seat:{id} {bookingId} NX EX 300` — automatic TTL expiry without any application-level cleanup.

---

**Q25: How does the Cab Booking state machine prevent invalid transitions?**

Each transition method validates the current state before proceeding:
```java
void driverArrived() {
    requireStatus(RideStatus.DRIVER_ASSIGNED);  // throws if wrong
    status = RideStatus.DRIVER_ARRIVED;
}
void startRide() {
    requireStatus(RideStatus.DRIVER_ARRIVED);   // can't start without driver arriving
    status = RideStatus.IN_PROGRESS;
}
private void requireStatus(RideStatus required) {
    if (status != required)
        throw new InvalidRideStateException(required.name(), status);
}
```

This prevents: starting a ride before the driver arrives, completing a ride before starting, cancelling an already-completed ride. Each method is self-documenting about its precondition. The full state machine is readable from the transitions alone.

---

**Q26: In the Hotel system, how does the Decorator pattern work for amenities, and how does pricing know the total?**

Each `RoomAmenityDecorator` wraps a `RoomService` and adds to its daily rate:
```java
class BreakfastIncluded extends RoomAmenityDecorator {
    private static final Money DAILY_COST = Money.ofRupees(600);
    @Override public Money getDailyRate() { return wrapped.getDailyRate().add(DAILY_COST); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Breakfast"; }
}
```

Stacking: `new SpaAccess(new BreakfastIncluded(new ParkingIncluded(room)))`.
- `room.getDailyRate()` = ₹5000 (base)
- `ParkingIncluded.getDailyRate()` = ₹5000 + ₹200 = ₹5200
- `BreakfastIncluded.getDailyRate()` = ₹5200 + ₹600 = ₹5800
- `SpaAccess.getDailyRate()` = ₹5800 + ₹1500 = ₹7300

The `PricingStrategy` calls `roomService.getDailyRate()` — it doesn't know or care about decorators. The total price emerges from the chain naturally.

---

**Q27: Why is the Snake & Ladder board built with a Builder pattern?**

Three reasons:

1. Validation at build time: snake head must be above tail, ladder bottom below top, no position can be both a snake head and a ladder bottom. The `addSnake()` and `addLadder()` methods validate immediately. `build()` returns only a valid board.

2. Fluent, readable construction:
```java
new Board.Builder(100)
    .addSnake(99, 5).addSnake(70, 55)
    .addLadder(4, 56).addLadder(12, 50)
    .build();
```
Far more readable than passing two `Map<Integer, Integer>` parameters to a constructor.

3. Immutable result: the `Board` is constructed from the Builder and then never changes. Players move on the board — the board itself is immutable after `build()`.

---

**Q28: How does the Library system handle two members trying to borrow the last copy simultaneously?**

The `findAvailableCopy()` + `setStatus(BORROWED)` must be atomic. In our implementation, `Book.findAvailableCopy()` returns an `Optional<BookCopy>` and the `Library.borrowBook()` calls these under no lock — a race condition.

Production fix: wrap the find-and-reserve in a `synchronized` block on the book, or use database-level optimistic locking:
```java
synchronized (book) {
    BookCopy copy = book.findAvailableCopy()
        .orElseThrow(() -> new BookNotAvailableException(isbn));
    copy.setStatus(BookStatus.BORROWED);
}
```

In the database version: `UPDATE book_copies SET status='BORROWED' WHERE barcode=? AND status='AVAILABLE'` — if 0 rows updated, another thread won. This is optimistic locking — no Java-level lock, the DB handles it atomically.

---

**Q29: How would you extend the Spaceship game to support multiplayer?**

Current: single `PlayerShip`, single game loop.

Multiplayer changes:
1. `List<PlayerShip> players` instead of one player
2. Each player has a `playerId` and separate keyboard controls (or network socket for remote)
3. `GameState` tracks which player's bullets hit which enemy — score attributed per player
4. `EventBus` publishes `PlayerScoredEvent(playerId, points)` instead of global score
5. For network multiplayer: `GameState` becomes the authoritative server state; clients send input events; server applies them and broadcasts the updated state back to all clients (authoritative server pattern)

The Observer pattern (`GameEventBus`) and Strategy pattern (`MovementStrategy`) require zero changes — they're already decoupled from single-player assumptions.

---

**Q30: You have 45 minutes for a machine coding problem you haven't seen before. What is your exact approach?**

Minutes 0–5 (Clarify):
Ask: What are the three core features? Is it multi-user? Does state need to persist? Any special rules I should know about? Then repeat back your understanding: "So I need to design X with Y and Z. Let me start with the entities."

Minutes 5–10 (Entities + Relations):
Write out the key classes on the whiteboard/editor as comments. Identify IS-A vs HAS-A. Pick the two or three patterns that fit. Name them out loud.

Minutes 10–15 (Enums + Records + Exceptions):
Code the enums (states, types), records (immutable value objects like Money, Position), and domain exceptions (specific names, context in messages). These are fast and show discipline.

Minutes 15–35 (Core Classes):
Code the main entities with full validation in constructors. Code the service class with 3–4 core operations. Use private helpers. Apply thread safety where clearly needed.

Minutes 35–45 (Main Demo + Extension):
Write a `main()` that shows the happy path and one error case. Then say: "Here's how I'd extend this for [common follow-up]." Showing extensibility scores as much as the core design.

Throughout: narrate design decisions. "I'm using synchronized here because two threads could race on this check-and-set." "I chose composition over inheritance because the IS-A test fails." Interviewers evaluate your thinking, not just your output.
