# 📚 LLD Foundations — OOP & SOLID

> This is NOT a textbook recap. Every section shows **bad code → what breaks → good code → why it matters in interviews**.

---

## Part 1: The Four Pillars of OOP

### 1. Encapsulation

**What it is:** Bundle data (fields) and behaviour (methods) together. Hide internal state. Expose only what callers need to know.

**Violation:**
```java
// BAD — fields public, anyone can corrupt state
public class BankAccount {
    public double balance;  // anyone can set balance = -999999
    public String pin;      // anyone can read the PIN
}

BankAccount acc = new BankAccount();
acc.balance = -1000;  // no validation, no audit trail
acc.pin = "0000";     // security disaster
```

**Fix:**
```java
// GOOD — state protected, behaviour controlled
public class BankAccount {
    private double balance;
    private final String accountId;
    private final List<String> auditLog = new ArrayList<>();

    public BankAccount(String accountId, double initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative");
        this.accountId = accountId;
        this.balance = initialBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
        auditLog.add("DEPOSIT: " + amount);
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (amount > balance) throw new InsufficientFundsException("Balance: " + balance);
        balance -= amount;
        auditLog.add("WITHDRAWAL: " + amount);
    }

    public double getBalance() { return balance; }  // read-only access
}
```

**Why it matters in interviews:** Interviewers watch whether you make fields `private` instinctively. Public fields with no validation = immediate red flag. Ask yourself: "What invariant must this class always maintain?" Then protect it.

---

### 2. Abstraction

**What it is:** Hide implementation details behind a clean interface. Callers interact with the "what" not the "how".

**Violation:**
```java
// BAD — caller must know internal implementation details
public class EmailService {
    public void sendViaSMTP(String host, int port, String user,
                            String password, String to, String body) { ... }
    public void sendViaMailgun(String apiKey, String domain,
                               String to, String body) { ... }
}

// Caller is tightly coupled to every provider's internals
emailService.sendViaSMTP("smtp.gmail.com", 587, "user", "pass", "x@y.com", "Hi");
```

**Fix:**
```java
// GOOD — abstraction hides providers behind a clean contract
public interface EmailSender {
    void send(Email email);
}

public record Email(String to, String subject, String body) {
    public Email {
        Objects.requireNonNull(to, "Recipient required");
        Objects.requireNonNull(subject, "Subject required");
        if (to.isBlank()) throw new IllegalArgumentException("Recipient cannot be blank");
    }
}

public class SmtpEmailSender implements EmailSender {
    private final SmtpConfig config;
    public SmtpEmailSender(SmtpConfig config) { this.config = config; }

    @Override
    public void send(Email email) {
        // SMTP implementation hidden here
    }
}

public class MailgunEmailSender implements EmailSender {
    @Override
    public void send(Email email) {
        // Mailgun API hidden here
    }
}

// Caller only knows EmailSender — never changes when provider changes
EmailSender sender = new SmtpEmailSender(config);
sender.send(new Email("x@y.com", "Hi", "Body"));
```

**Why it matters:** Interviewers give points for "program to interfaces, not implementations." Every dependency in your design should be on an abstraction, not a concrete class. This enables testing (mock the interface) and extension (swap implementations).

---

### 3. Inheritance

**What it is:** A subclass inherits state and behaviour from a superclass. Enables code reuse and IS-A relationships.

**When to use:** ONLY when the subclass truly IS-A supertype in every sense. Use composition when in doubt.

**Violation — abuse of inheritance:**
```java
// BAD — Stack extends Vector just to reuse its storage
// Stack IS-A Vector? No — you can call stack.add(0, element) and break stack invariants
public class Stack<T> extends Vector<T> {
    public T push(T item) { addElement(item); return item; }
    public T pop() { ... }
    // But caller can also call: stack.add(0, item)  ← inserts at position 0, not a stack op!
    // This is the java.util.Stack mistake — it broke Liskov Substitution
}
```

**Fix — prefer composition:**
```java
// GOOD — Stack HAS-A storage (composition), exposes only stack operations
public class Stack<T> {
    private final Deque<T> storage = new ArrayDeque<>();

    public void push(T item) {
        Objects.requireNonNull(item, "Cannot push null");
        storage.push(item);
    }

    public T pop() {
        if (isEmpty()) throw new EmptyStackException();
        return storage.pop();
    }

    public T peek() {
        if (isEmpty()) throw new EmptyStackException();
        return storage.peek();
    }

    public boolean isEmpty() { return storage.isEmpty(); }
    public int size() { return storage.size(); }
    // No way for caller to break stack semantics
}
```

**Composition vs Inheritance rule of thumb:**
```
IS-A test: "A Dog IS-A Animal" ✅ → inheritance OK
HAS-A test: "A Car HAS-A Engine" ✅ → composition
CAN-DO test: "A Duck CAN-DO FlyBehaviour" ✅ → interface

When to PREFER composition over inheritance:
  - You want to reuse behaviour but the IS-A test fails
  - You need to change behaviour at runtime
  - The superclass has methods you want to hide from callers
  - You're using a final class you can't extend (Decorator pattern)
```

---

### 4. Polymorphism

**What it is:** One interface, many implementations. Method dispatch decided at runtime (dynamic dispatch) based on actual object type.

**Two kinds:**
```java
// Compile-time polymorphism (method overloading)
public class Calculator {
    public int add(int a, int b) { return a + b; }
    public double add(double a, double b) { return a + b; }
    public String add(String a, String b) { return a + b; }
}

// Runtime polymorphism (method overriding) — the important one for LLD
public abstract class Shape {
    public abstract double area();
    public abstract double perimeter();

    // Template method — uses polymorphism internally
    public String describe() {
        return String.format("%s: area=%.2f, perimeter=%.2f",
            getClass().getSimpleName(), area(), perimeter());
    }
}

public class Circle extends Shape {
    private final double radius;
    public Circle(double radius) {
        if (radius <= 0) throw new IllegalArgumentException("Radius must be positive");
        this.radius = radius;
    }
    @Override public double area() { return Math.PI * radius * radius; }
    @Override public double perimeter() { return 2 * Math.PI * radius; }
}

public class Rectangle extends Shape {
    private final double width, height;
    // validation + constructor
    @Override public double area() { return width * height; }
    @Override public double perimeter() { return 2 * (width + height); }
}

// Polymorphic usage — caller doesn't need to know the type
List<Shape> shapes = List.of(new Circle(5), new Rectangle(4, 6));
shapes.forEach(s -> System.out.println(s.describe()));
// Correct area/perimeter called for each shape automatically
```

**Java 21 pattern matching — modern polymorphism without instanceof chains:**
```java
// OLD — verbose, error-prone instanceof chain
if (shape instanceof Circle) {
    Circle c = (Circle) shape;
    return c.getRadius() * 2;
} else if (shape instanceof Rectangle) {
    Rectangle r = (Rectangle) shape;
    return Math.max(r.getWidth(), r.getHeight());
}

// JAVA 21 — sealed classes + pattern matching switch
public sealed interface Shape permits Circle, Rectangle, Triangle {}
public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height) implements Shape {}

public double largestDimension(Shape shape) {
    return switch (shape) {
        case Circle c      -> c.radius() * 2;
        case Rectangle r   -> Math.max(r.width(), r.height());
        case Triangle t    -> t.base();
        // Compiler enforces exhaustiveness — no default needed!
        // If you add a new Shape type, compiler flags all switches that need updating
    };
}
```

**Why sealed classes + pattern matching are important for LLD:**
- Compile-time exhaustiveness check — you can't forget a case
- No casting, no ClassCastException
- Interviewers at senior level expect you to use Java 21 features naturally

---

## Part 2: SOLID Principles

### S — Single Responsibility Principle (SRP)

**Definition:** A class should have only one reason to change. Equivalently: a class should have only one job.

**Violation:**
```java
// BAD — this class has 4 responsibilities:
// 1. Represent an Order
// 2. Persist the order to DB
// 3. Format the order for display
// 4. Send email notification
public class Order {
    private List<OrderItem> items;
    private Customer customer;

    // Responsibility 1: business logic
    public double calculateTotal() { ... }

    // Responsibility 2: persistence — changes when DB changes
    public void saveToDatabase() {
        Connection conn = DriverManager.getConnection("jdbc:mysql://...");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO orders...");
        // ...
    }

    // Responsibility 3: formatting — changes when display format changes
    public String toHtmlReceipt() {
        return "<html><body>Order #" + id + "...</body></html>";
    }

    // Responsibility 4: notification — changes when email provider changes
    public void sendConfirmationEmail() {
        SmtpClient client = new SmtpClient("smtp.gmail.com");
        client.send(customer.getEmail(), "Order confirmed", toHtmlReceipt());
    }
}
```

**Fix:**
```java
// GOOD — each class has ONE reason to change
public class Order {
    private final String id;
    private final List<OrderItem> items;
    private final Customer customer;

    public double calculateTotal() {
        return items.stream().mapToDouble(OrderItem::subtotal).sum();
    }
    // Getters only — pure domain object
}

public class OrderRepository {
    // Reason to change: database schema or technology changes
    public void save(Order order) { /* DB logic */ }
    public Optional<Order> findById(String id) { /* DB query */ }
}

public class OrderReceiptFormatter {
    // Reason to change: receipt format changes
    public String toHtml(Order order) { /* HTML formatting */ }
    public String toPlainText(Order order) { /* text formatting */ }
}

public class OrderNotificationService {
    private final EmailSender emailSender;
    // Reason to change: notification channel or content changes
    public void sendConfirmation(Order order) {
        emailSender.send(new Email(
            order.getCustomer().getEmail(),
            "Order Confirmed",
            formatter.toPlainText(order)
        ));
    }
}
```

**Interview signal:** When you show a God Class, say: "I see this class has multiple responsibilities. Let me separate them." Then do it. That alone impresses interviewers.

---

### O — Open/Closed Principle (OCP)

**Definition:** Software entities should be open for extension, closed for modification. Add new behaviour by adding new code, not changing existing code.

**Violation:**
```java
// BAD — every new discount type requires modifying this method
public class DiscountCalculator {
    public double calculate(Order order, String discountType) {
        return switch (discountType) {
            case "STUDENT"   -> order.getTotal() * 0.10;
            case "SENIOR"    -> order.getTotal() * 0.15;
            case "EMPLOYEE"  -> order.getTotal() * 0.30;
            // Adding "MILITARY" requires changing this class
            // Every change risks breaking existing cases
            default -> 0.0;
        };
    }
}
```

**Fix:**
```java
// GOOD — new discount types added without touching existing code
@FunctionalInterface
public interface DiscountStrategy {
    double calculate(Order order);
}

public class StudentDiscount implements DiscountStrategy {
    @Override
    public double calculate(Order order) { return order.getTotal() * 0.10; }
}

public class SeniorDiscount implements DiscountStrategy {
    @Override
    public double calculate(Order order) { return order.getTotal() * 0.15; }
}

public class EmployeeDiscount implements DiscountStrategy {
    @Override
    public double calculate(Order order) { return order.getTotal() * 0.30; }
}

// NEW type added — zero changes to existing code:
public class MilitaryDiscount implements DiscountStrategy {
    @Override
    public double calculate(Order order) { return order.getTotal() * 0.20; }
}

public class DiscountCalculator {
    public double calculate(Order order, DiscountStrategy strategy) {
        return strategy.calculate(order);
    }
}
```

**Real-world application:** Spring's `Validator` interface, Java's `Comparator`, `Predicate` — all OCP. You extend behaviour by providing a new implementation, never by modifying the framework.

---

### L — Liskov Substitution Principle (LSP)

**Definition:** Subtypes must be substitutable for their base types without altering the correctness of the program. If S is a subtype of T, you should be able to use an S wherever a T is expected.

**Violation — the classic Square/Rectangle problem:**
```java
// BAD — Square extends Rectangle but breaks its contract
public class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int w)  { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w;  // Square forces equal dimensions
    }
    @Override
    public void setHeight(int h) {
        this.width = h;
        this.height = h;
    }
}

// This code works for Rectangle but BREAKS with Square:
void testRectangle(Rectangle r) {
    r.setWidth(5);
    r.setHeight(4);
    assert r.area() == 20;  // Passes for Rectangle, FAILS for Square (area = 16)
}
// Square is NOT a substitutable Rectangle — LSP violated
```

**Fix:**
```java
// GOOD — separate abstractions, no broken substitution
public sealed interface Shape permits Rectangle, Square, Circle {}

public record Rectangle(int width, int height) implements Shape {
    public Rectangle {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions must be positive");
    }
    public int area() { return width * height; }
}

public record Square(int side) implements Shape {
    public Square {
        if (side <= 0) throw new IllegalArgumentException("Side must be positive");
    }
    public int area() { return side * side; }
}

// Or share only what's truly common:
public interface HasArea { int area(); }
// Rectangle and Square both implement HasArea — only the area() contract is shared
```

**Violation pattern to watch for in interviews:**
- Subclass throws exceptions for operations the base class allows
- Subclass has empty/no-op override of a base class method
- Subclass narrows accepted input types or widens output types

---

### I — Interface Segregation Principle (ISP)

**Definition:** Clients should not be forced to depend on interfaces they don't use. Prefer many small, focused interfaces over one large "fat" interface.

**Violation:**
```java
// BAD — one fat interface forces every implementor to stub irrelevant methods
public interface Worker {
    void work();
    void eat();
    void sleep();
    void attendMeeting();
    void submitTimesheet();
}

// A Robot can work but can't eat, sleep, or attend meetings
public class Robot implements Worker {
    @Override public void work() { /* does real work */ }
    @Override public void eat() { throw new UnsupportedOperationException(); }  // forced!
    @Override public void sleep() { throw new UnsupportedOperationException(); }
    @Override public void attendMeeting() { throw new UnsupportedOperationException(); }
    @Override public void submitTimesheet() { throw new UnsupportedOperationException(); }
}
```

**Fix:**
```java
// GOOD — segregated interfaces, each client implements only what it needs
public interface Workable        { void work(); }
public interface Eatable         { void eat(); }
public interface Sleepable       { void sleep(); }
public interface MeetingAttendee { void attendMeeting(); }

// Human employee implements all relevant interfaces
public class HumanEmployee implements Workable, Eatable, Sleepable, MeetingAttendee {
    @Override public void work() { /* works */ }
    @Override public void eat() { /* eats */ }
    @Override public void sleep() { /* sleeps */ }
    @Override public void attendMeeting() { /* attends */ }
}

// Robot only implements what it actually does
public class Robot implements Workable {
    @Override public void work() { /* works 24/7 */ }
}
```

**Real Java example:** `java.util.List` vs `java.util.RandomAccess`. `RandomAccess` is a marker interface — classes that support fast random access implement it. Algorithms check `instanceof RandomAccess` to pick the optimal iteration strategy. Lists that don't support it (like `LinkedList`) simply don't implement it.

---

### D — Dependency Inversion Principle (DIP)

**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details; details should depend on abstractions.

**Violation:**
```java
// BAD — high-level OrderService directly depends on low-level MySQLOrderRepository
public class OrderService {
    // Hard dependency on concrete implementation
    private final MySQLOrderRepository orderRepository = new MySQLOrderRepository();
    private final GmailEmailSender emailSender = new GmailEmailSender("smtp.gmail.com");

    public void placeOrder(Order order) {
        orderRepository.save(order);    // tightly coupled to MySQL
        emailSender.sendConfirmation(order);  // tightly coupled to Gmail
        // To test this, you MUST have a MySQL database and Gmail account
        // To swap to PostgreSQL, you MUST change OrderService
    }
}
```

**Fix:**
```java
// GOOD — depend on abstractions, inject concrete implementations
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
}

public interface NotificationService {
    void sendConfirmation(Order order);
}

public class OrderService {
    private final OrderRepository orderRepository;     // abstraction
    private final NotificationService notificationService; // abstraction

    // Constructor injection — dependencies provided from outside
    public OrderService(OrderRepository orderRepository,
                        NotificationService notificationService) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    public void placeOrder(Order order) {
        validateOrder(order);
        orderRepository.save(order);
        notificationService.sendConfirmation(order);
    }

    private void validateOrder(Order order) {
        if (order.getItems().isEmpty()) throw new InvalidOrderException("Order has no items");
        if (order.getTotal() <= 0) throw new InvalidOrderException("Order total must be positive");
    }
}

// Now you can:
// 1. Test with MockOrderRepository (no DB needed)
// 2. Swap MySQL → PostgreSQL without touching OrderService
// 3. Swap Gmail → Twilio without touching OrderService
OrderService service = new OrderService(
    new MySQLOrderRepository(dataSource),
    new EmailNotificationService(emailSender)
);

// In tests:
OrderService service = new OrderService(
    new InMemoryOrderRepository(),  // no DB
    new NoOpNotificationService()   // no email
);
```

---

## Part 3: UML Class Diagram — How to Draw in Interviews

You don't need a tool. Text-based diagrams are fine in interviews. Know the symbols.

### Relationships

```
Inheritance (IS-A):          Animal ◄────────── Dog
                             (hollow triangle arrowhead)

Interface Implementation:    Flyable ◄- - - - - Bird
                             (dashed + hollow triangle)

Composition (owns, dies together):
                             House ◆────────── Room
                             (filled diamond on owner side)
                             If House destroyed → Rooms destroyed

Aggregation (has, survives):
                             Team ◇────────── Player
                             (hollow diamond on container side)
                             If Team disbanded → Players still exist

Association (uses):          Order ─────────── Customer
                             (plain line, both survive independently)

Dependency (uses temporarily):
                             OrderService - - - → PaymentGateway
                             (dashed arrow — one method call, no field)
```

### Class Box Format
```
┌─────────────────────────────┐
│       ClassName              │   ← Class name (bold/centered)
├─────────────────────────────┤
│ - privateField: Type         │   ← Fields (- private, + public, # protected)
│ + publicField: Type          │
├─────────────────────────────┤
│ + publicMethod(): ReturnType │   ← Methods
│ - privateMethod(): void      │
│ + staticMethod(): Type       │   ← underline for static
└─────────────────────────────┘
```

### Interview Drawing Strategy
```
Step 1: List all entities (nouns from requirements) → these become classes
Step 2: Identify relationships:
          - IS-A hierarchy? → inheritance
          - Owns/part-of (dies together)? → composition
          - Has but independent? → aggregation
          - Just uses? → dependency/association
Step 3: Add key fields and methods (don't add every getter/setter)
Step 4: Identify which design pattern this structure resembles
Step 5: Explain out loud — "I'm using composition here because..."
```

### Example — Parking Lot quick sketch
```
ParkingLot (1) ◆─────── (1..*) ParkingFloor
ParkingFloor (1) ◆─────── (1..*) ParkingSpot
ParkingSpot ────── SpotType <<enum>>: SMALL, MEDIUM, LARGE
ParkingSpot ────── SpotStatus <<enum>>: AVAILABLE, OCCUPIED

Vehicle ◄─────── Car
        ◄─────── Bike
        ◄─────── Truck

ParkingTicket ──── ParkingSpot (assigned spot)
ParkingTicket ──── Vehicle (which vehicle)
ParkingLot uses──→ ParkingStrategy <<interface>>
                         ◄─────── NearestEntryStrategy
                         ◄─────── LargestAvailableStrategy
```

---

## Part 4: Composition Over Inheritance — When to Use Each

```
Use INHERITANCE when:
  ✅ True IS-A relationship exists (a Dog truly IS-A Animal)
  ✅ Subclass enriches the parent without breaking its contract
  ✅ You want to reuse AND extend base class behaviour
  ✅ LSP holds — subclass is fully substitutable

Use COMPOSITION when:
  ✅ HAS-A relationship (Car HAS-A Engine)
  ✅ You want to reuse behaviour from a class you can't extend (final)
  ✅ Behaviour needs to change at runtime (inject different strategy)
  ✅ You want to combine behaviours from multiple sources
  ✅ When in doubt — composition is almost always safer

Pitfall — shallow IS-A:
  Stack extends Vector (Java's mistake) — Stack IS-A Vector? No.
  HTTP connection extends Socket? No — HAS-A socket.
  Square extends Rectangle? Breaks LSP.
  
Real examples of correct inheritance:
  ArrayList extends AbstractList ✅
  FileInputStream extends InputStream ✅
  IllegalArgumentException extends RuntimeException ✅
```

---

## Interview Q&A

**Q: What's the difference between an abstract class and an interface? When do you use each?**
A: Abstract class: can have state (fields), constructors, concrete methods, and abstract methods. Single inheritance only. Use when sharing code AND state between closely related classes (e.g., `AbstractList` holds `modCount`). Interface: no state (only constants), no constructors, all methods implicitly public. Multiple implementation. Use when defining a contract for unrelated classes (e.g., `Comparable`, `Serializable`, `Runnable`). Java 8+ default/static methods blur the line — but the key distinction remains: abstract classes can hold instance state, interfaces cannot. In practice: prefer interfaces for type declarations, abstract classes for partial implementations.

**Q: What is the difference between composition and aggregation?**
A: Both are HAS-A relationships. Composition: the contained object cannot exist without the container. A `House` has `Rooms` — if the House is destroyed, the Rooms cease to exist (they're part of the same lifecycle). Aggregation: the contained object has an independent lifecycle. A `Team` has `Players` — if the team is disbanded, the Players still exist. Implementation: composition = create the child inside the parent's constructor. Aggregation = receive the child via constructor/setter (it was created elsewhere). In UML: filled diamond (composition) vs hollow diamond (aggregation).

**Q: You're designing a payment system and a new intern says "let's make PaymentProcessor extend DatabaseConnection to reuse the connection handling code." What do you say?**
A: That's a classic inheritance misuse. PaymentProcessor IS-NOT-A DatabaseConnection — it just USES one. The intern wants to reuse code, which is a valid goal, but inheritance is the wrong tool. Instead: give PaymentProcessor a `DatabaseConnection` field (composition) — inject it via constructor. This also makes the PaymentProcessor testable (inject a mock connection) and respects the Single Responsibility Principle (database connection management is separate from payment processing logic). The rule: never inherit just to reuse code. Inherit only when there's a genuine IS-A relationship.

**Q: How do you apply SOLID to a class you're designing in an interview?**
A: I run through it as a checklist: (S) Does this class have more than one reason to change? If so, split it. (O) If I add a new type of X, do I need to modify this class? If so, introduce an interface. (L) If I subclass this, can the subclass always substitute the parent without breaking callers? (I) Is my interface forcing implementors to stub methods they don't need? (D) Are my high-level classes depending on concrete implementations? If so, inject abstractions. In practice, S and D come up most in interviews — God classes and hard-wired dependencies are the most common problems I see.

**Q: What is the "Tell, Don't Ask" principle?**
A: Tell objects what to do — don't ask them for data and make decisions externally. Bad: `if (order.getStatus() == Status.PENDING) { order.setStatus(Status.CONFIRMED); }` — you're asking for data and making the decision outside. Good: `order.confirm()` — the Order object knows how to confirm itself, validates its own state, and encapsulates the transition logic. Violating this leads to anemic domain models (objects with only getters/setters) and business logic scattered across service classes. In LLD interviews, showing domain objects with rich behaviour (not just DTOs) is a strong positive signal.
