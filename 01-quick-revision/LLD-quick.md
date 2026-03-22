# ⚡ LLD Quick Revision — Night Before Review

> Read this the evening before your interview. 20 minutes covers everything.

---

## 1. The 5-Step LLD Approach (Memorise This)

```
1. CLARIFY     (5 min)  — actors, use cases, scale, multi-user?
2. ENTITIES    (5 min)  — nouns → classes; eliminate pure attributes
3. RELATIONS   (5 min)  — IS-A (inherit) vs HAS-A (compose) vs USES (param)
4. PATTERNS    (5 min)  — which pattern fits? name it, justify it
5. CODE        (25 min) — enums/records/exceptions → entities → service → main()
```

**Say this out loud**: "Before I code, let me identify the entities and their relationships."

---

## 2. Pattern Selection — 30-Second Decision

```
Multiple interchangeable algorithms?  → Strategy
Notify others when state changes?     → Observer
Build complex object step-by-step?    → Builder
Object behaviour changes with state?  → State
Add behaviour without subclassing?    → Decorator
Simplify complex subsystem?           → Facade
Control access / cross-cutting?       → Proxy
Ensure one instance?                  → Singleton (enum-based)
Create without knowing exact type?    → Factory Method
Incompatible interfaces work together?→ Adapter
Tree structure (uniform treatment)?   → Composite
Algorithm skeleton, vary steps?       → Template Method
Chain of handlers?                    → Chain of Responsibility
```

---

## 3. Java 21 — What to Use When

```java
// Immutable value objects  →  record
record Money(long paise) { Money { if (paise < 0) throw new IAE("Negative money"); } }

// Exhaustive type hierarchy  →  sealed interface
sealed interface Shape permits Circle, Rect {}
return switch (shape) {
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rect   r -> r.width() * r.height();
    // No default needed — sealed guarantees exhaustiveness
};

// Optional: avoid null  →  Optional.of / Optional.empty / Optional.ofNullable
return Optional.ofNullable(map.get(id));

// Functional strategy  →  @FunctionalInterface + lambda
@FunctionalInterface interface FareCalc { Money calculate(double km, Duration d); }
FareCalc surge = (km, d) -> base.calculate(km, d).multiply(1.5);
```

---

## 4. Thread Safety — The 3 Rules

```
Rule 1: Any shared MUTABLE state accessed by multiple threads = needs protection

Rule 2: Use the right tool
  synchronized   → critical section with multiple operations (tryLock, tryOccupy)
  ConcurrentHashMap → shared map
  AtomicInteger  → single counter (ticket IDs, sequence numbers)
  CopyOnWriteArrayList → event listener list (rare writes, frequent reads)
  volatile       → single flag visibility

Rule 3: Always check for rollback
  If you lock multiple resources and one fails → release all previously locked ones
```

```java
// The classic interview pattern — test-and-set atomically
synchronized boolean tryOccupy(VehicleType type) {
    if (status != AVAILABLE) return false;  // check
    status = OCCUPIED;                       // set
    return true;                             // both in one synchronized block
}
```

---

## 5. Money — Never Use Double

```java
// Always store in paise (₹) or cents ($)
record Money(long paise) {
    static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    Money add(Money o)              { return new Money(paise + o.paise); }
    @Override public String toString() { return String.format("₹%.2f", paise/100.0); }
}

// Split remainder correctly — last person absorbs rounding diff
long perPerson = total.paise() / n;
long remainder = total.paise() % n;
// Give extra 1 paise to first 'remainder' participants
```

---

## 6. SOLID — One Sentence Each

```
S — One class, one reason to change (split God classes)
O — Extend by adding, not by modifying (Strategy/Decorator instead of switch)
L — Subclass must work wherever base class works (avoid Square extends Rectangle)
I — Small focused interfaces (no Robot.eat() throwing UnsupportedOperationException)
D — Depend on abstractions; inject implementations (constructor injection)
```

---

## 7. Validation — Never Skip These

```java
// In EVERY constructor:
Objects.requireNonNull(param, "Param name required");  // null check
if (name.isBlank()) throw new IAE("Name cannot be blank");  // blank check
if (amount < 0)     throw new IAE("Amount cannot be negative");  // range check
if (!email.contains("@")) throw new IAE("Invalid email: " + email);  // format check

// State precondition:
if (status != CONFIRMED) throw new IllegalStateException("Expected CONFIRMED, got: " + status);

// Business rule:
if (guestCount > room.getCapacity())
    throw new BusinessRuleViolationException("Exceeds capacity of " + room.getCapacity());
```

---

## 8. Exception Hierarchy — Use This Every Time

```java
class DomainException extends RuntimeException {
    DomainException(String msg) { super(msg); }
}
class EntityNotFoundException extends DomainException {
    EntityNotFoundException(String entity, String id) {
        super(entity + " not found: " + id); }
}
class InvalidStateException extends DomainException {
    InvalidStateException(String expected, Object actual) {
        super("Expected " + expected + " but was: " + actual); }
}
```

---

## 9. The 11 Machine Coding Systems — Key Pattern Per System

| System | Key Pattern | Signature Line |
|--------|-------------|---------------|
| Parking Lot | Strategy (spot selection) | `synchronized boolean tryOccupy()` |
| Chess | Piece owns its moves | Each piece: `List<Position> getLegalMoves(Board)` |
| ATM | Chain of Responsibility | `₹2000 → ₹500 → ₹200 → ₹100` dispensers |
| Elevator | SCAN via TreeSet | `TreeSet<Integer> upStops` sorted queue |
| Library | Strategy (fine calc) | `FineCalculator.calculate(dueDate, returnDate)` |
| Spaceship | Observer + Sealed events | `sealed GameEvent` + EventBus<GameEvent> |
| Splitwise | Greedy debt simplification | O(N log N) min-transaction algorithm |
| BookMyShow | Seat lock with TTL | `seat.tryLock(bookingId, LOCK_TTL)` |
| Cab Booking | Haversine + State machine | State: REQUESTED→ASSIGNED→ARRIVED→IN_PROGRESS |
| Snake & Ladder | Sealed entities + Strategy | `sealed BoardEntity permits Snake, Ladder, Empty` |
| Hotel | Decorator (amenities) | `new SpaAccess(new Breakfast(new ParkingIncluded(room)))` |

---

## 10. What Interviewers Are Actually Checking

```
✅ Do you make fields private instinctively?
✅ Do you validate in constructors without being asked?
✅ Do you name design patterns when you use them?
✅ Do you separate concerns (SRP) naturally?
✅ Do you write domain-specific exceptions (not raw RuntimeException)?
✅ Can you identify which part needs thread safety?
✅ Do you use Money in paise/cents (not double)?
✅ Do you write a working main() that demonstrates the design?
✅ Can you explain WHY you chose composition over inheritance?
✅ When asked "how would you add X?", do you show it's extensible without changing existing code?
```

---

## 11. Phrases That Score Points in Interviews

```
"I'm using the Strategy pattern here because the algorithm needs to be swappable at runtime."
"I'm using composition instead of inheritance because the IS-A test fails."
"I'm storing money as paise to avoid floating-point precision errors."
"This method is synchronized because two threads could both check availability and both proceed."
"I'm using a sealed interface here so the compiler enforces exhaustive handling in the switch."
"The Builder validates cross-field constraints — you can't build an invalid object."
"I'm throwing a domain-specific exception so callers can catch it meaningfully."
"I'm using the Observer pattern to decouple the game loop from scoring and notification."
"Rolling back the partially-locked seats prevents them from being stuck in a locked state."
"The State pattern eliminates the if/else chains — each state only knows valid transitions."
```

---

## 12. LLD vs HLD — Know the Difference

```
LLD (Low-Level Design):                HLD (High-Level Design):
  Class diagram                          Component diagram
  Design patterns                        Architecture patterns
  OOP + SOLID                            CAP theorem, consistency
  Code quality                           Scalability, latency
  45 min: design + working code          45 min: draw + justify choices
  "What classes do you need?"            "What services do you need?"
  "How does Seat know it's occupied?"    "How do you prevent double-booking at scale?"
  Interviewer: reads your code           Interviewer: asks about tradeoffs
```

---

## Read Next (If Time)

1. `LLD-cheatsheet.md` — Full patterns reference
2. The specific system design files for problems you expect
3. `03-resources/LLD-Master-QnA.md` — 30 Q&As
4. `03-resources/LLD-Priority-Guide.md` — What to focus on for your role
