# 📚 LLD — Design Patterns Reference (All 23 GoF + Modern Java)

> Format per pattern: Intent → When to use → When NOT to use → Java 21 code → Real example → Interview signal

---

## Part 1: Creational Patterns

Creational patterns deal with **object creation** — controlling how and when objects are instantiated.

---

### 1. Singleton

**Intent:** Ensure a class has only one instance and provide a global access point.

**When to use:**
- Shared resource that should have exactly one instance: configuration, thread pool, connection pool, registry
- Expensive to create, needs to be reused: DB connection pool, logger

**When NOT to use:**
- When it holds mutable global state → hidden dependencies, hard to test
- When you think "I'll only ever need one" — you're probably wrong
- Overused as a lazy way to avoid passing dependencies

**Java 21 — Thread-safe Singleton (best way):**
```java
// Enum Singleton — serialisation-safe, reflection-safe, thread-safe by JVM
public enum ConfigurationManager {
    INSTANCE;

    private final Properties properties = new Properties();
    private final Map<String, String> overrides = new ConcurrentHashMap<>();

    ConfigurationManager() {
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            if (is != null) properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public String get(String key) {
        return overrides.getOrDefault(key, properties.getProperty(key));
    }

    public String get(String key, String defaultValue) {
        return overrides.getOrDefault(key, properties.getProperty(key, defaultValue));
    }

    public void override(String key, String value) {
        overrides.put(key, value);
    }
}

// Usage
String dbUrl = ConfigurationManager.INSTANCE.get("db.url");

// Alternatively — Double-Checked Locking (when enum doesn't fit)
public final class ConnectionPool {
    private static volatile ConnectionPool instance;
    private final int maxSize;

    private ConnectionPool(int maxSize) { this.maxSize = maxSize; }

    public static ConnectionPool getInstance(int maxSize) {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {  // double-check after acquiring lock
                    instance = new ConnectionPool(maxSize);
                }
            }
        }
        return instance;
    }
}
```

**Real examples:** `Runtime.getRuntime()`, `System.out`, Spring `ApplicationContext` (effectively), Kafka `AdminClient`

**Interview signal:** Don't just recite. Say: "I'd use the enum-based Singleton because it's thread-safe without explicit synchronisation, serialisation-safe, and reflection-safe."

---

### 2. Factory Method

**Intent:** Define an interface for creating an object, but let subclasses decide which class to instantiate.

**When to use:**
- You know you need to create objects but don't know the exact type until subclass is chosen
- You want to give subclasses control over object creation

**Java 21:**
```java
// Abstract creator
public abstract class NotificationFactory {
    // Factory method — subclasses override this
    protected abstract Notification createNotification(String message);

    // Template method uses the factory method
    public void send(String recipient, String message) {
        Notification notification = createNotification(message);
        notification.validate();
        notification.deliver(recipient);
    }
}

public interface Notification {
    void validate();
    void deliver(String recipient);
}

// Concrete creators
public class EmailNotificationFactory extends NotificationFactory {
    private final SmtpConfig config;

    public EmailNotificationFactory(SmtpConfig config) { this.config = config; }

    @Override
    protected Notification createNotification(String message) {
        return new EmailNotification(message, config);
    }
}

public class SmsNotificationFactory extends NotificationFactory {
    @Override
    protected Notification createNotification(String message) {
        if (message.length() > 160) throw new IllegalArgumentException("SMS max 160 chars");
        return new SmsNotification(message);
    }
}

// Usage
NotificationFactory factory = new EmailNotificationFactory(smtpConfig);
factory.send("user@example.com", "Your order was shipped");
```

**Real examples:** `java.util.Iterator`, `javax.xml.parsers.DocumentBuilderFactory`, Spring's `BeanFactory`

---

### 3. Abstract Factory

**Intent:** Provide an interface for creating **families of related objects** without specifying their concrete classes.

**When to use:**
- System must be independent of how its products are created
- You need to create families of related objects that work together
- You want to enforce consistent use of product families

**Java 21:**
```java
// Abstract factory — creates families of UI components
public interface UIComponentFactory {
    Button createButton();
    TextField createTextField();
    Dialog createDialog();
}

// Product family 1: Light theme
public class LightThemeFactory implements UIComponentFactory {
    @Override public Button createButton()       { return new LightButton(); }
    @Override public TextField createTextField() { return new LightTextField(); }
    @Override public Dialog createDialog()       { return new LightDialog(); }
}

// Product family 2: Dark theme
public class DarkThemeFactory implements UIComponentFactory {
    @Override public Button createButton()       { return new DarkButton(); }
    @Override public TextField createTextField() { return new DarkTextField(); }
    @Override public Dialog createDialog()       { return new DarkDialog(); }
}

// Client code — only depends on abstract types
public class LoginScreen {
    private final Button loginButton;
    private final TextField usernameField;

    public LoginScreen(UIComponentFactory factory) {
        this.loginButton = factory.createButton();
        this.usernameField = factory.createTextField();
    }
    // Entire screen uses consistent theme without knowing which factory
}
```

**Factory Method vs Abstract Factory:**
```
Factory Method:  one product, subclass decides which concrete type
Abstract Factory: family of products, entire factory swapped for consistent family
```

**Real examples:** `javax.xml.parsers.SAXParserFactory`, JDBC `ConnectionFactory`, Spring's `AbstractBeanFactory`

---

### 4. Builder

**Intent:** Separate the construction of a complex object from its representation. Build the object step by step.

**When to use:**
- Object has many optional parameters (constructor telescope problem)
- Object creation involves multiple steps that must be done in order
- You want immutable objects with readable construction

**Java 21 — with validation:**
```java
public final class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String body;
    private final Duration timeout;
    private final int maxRetries;

    private HttpRequest(Builder builder) {
        this.url        = builder.url;
        this.method     = builder.method;
        this.headers    = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body       = builder.body;
        this.timeout    = builder.timeout;
        this.maxRetries = builder.maxRetries;
    }

    // Getters only — immutable after construction
    public String url()       { return url; }
    public String method()    { return method; }
    public Map<String, String> headers() { return headers; }
    public Optional<String> body()       { return Optional.ofNullable(body); }
    public Duration timeout()            { return timeout; }

    public static Builder newBuilder(String url) {
        return new Builder(url);
    }

    public static final class Builder {
        private final String url;                            // required
        private String method = "GET";                       // optional with default
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;                                 // optional
        private Duration timeout = Duration.ofSeconds(30);  // default
        private int maxRetries = 3;                          // default

        private Builder(String url) {
            if (url == null || url.isBlank()) throw new IllegalArgumentException("URL required");
            if (!url.startsWith("http")) throw new IllegalArgumentException("URL must start with http");
            this.url = url;
        }

        public Builder method(String method) {
            this.method = Objects.requireNonNull(method).toUpperCase();
            return this;
        }

        public Builder header(String name, String value) {
            Objects.requireNonNull(name, "Header name required");
            Objects.requireNonNull(value, "Header value required");
            this.headers.put(name, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeout(Duration timeout) {
            if (timeout.isNegative() || timeout.isZero())
                throw new IllegalArgumentException("Timeout must be positive");
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) throw new IllegalArgumentException("Max retries cannot be negative");
            this.maxRetries = maxRetries;
            return this;
        }

        public HttpRequest build() {
            // Cross-field validation
            if ("POST".equals(method) || "PUT".equals(method)) {
                if (body == null) throw new IllegalStateException("POST/PUT requires a body");
            }
            return new HttpRequest(this);
        }
    }
}

// Usage — readable, validated, immutable
HttpRequest request = HttpRequest.newBuilder("https://api.example.com/orders")
    .method("POST")
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer " + token)
    .body(orderJson)
    .timeout(Duration.ofSeconds(10))
    .maxRetries(2)
    .build();
```

**Real examples:** `StringBuilder`, `Stream.Builder`, `ProcessBuilder`, `AlertDialog.Builder` (Android), Lombok `@Builder`

---

### 5. Prototype

**Intent:** Create new objects by copying (cloning) an existing object.

**When to use:**
- Object creation is expensive (DB lookup, complex init) and a copy is cheaper
- You need many similar objects with slight differences
- Classes to instantiate are specified at runtime

**Java 21:**
```java
public abstract class GameCharacter implements Cloneable {
    protected String name;
    protected int health;
    protected int level;
    protected List<String> abilities;
    protected Map<String, Integer> stats;

    // Deep clone — new collections, not shared references
    @Override
    public GameCharacter clone() {
        try {
            GameCharacter clone = (GameCharacter) super.clone();
            clone.abilities = new ArrayList<>(this.abilities);    // deep copy
            clone.stats = new HashMap<>(this.stats);              // deep copy
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone should always work", e);
        }
    }
}

// Registry of prototypes
public class CharacterTemplateRegistry {
    private final Map<String, GameCharacter> templates = new HashMap<>();

    public void register(String type, GameCharacter template) {
        templates.put(type, Objects.requireNonNull(template));
    }

    public GameCharacter create(String type) {
        GameCharacter template = templates.get(type);
        if (template == null) throw new IllegalArgumentException("Unknown character type: " + type);
        return template.clone();  // cheap copy instead of expensive construction
    }
}

// Usage — spawn 1000 enemy soldiers cheaply
CharacterTemplateRegistry registry = new CharacterTemplateRegistry();
registry.register("SOLDIER", new Soldier("Soldier", 100, 1));

// Each enemy is a clone — no expensive DB lookup or complex init per enemy
List<GameCharacter> enemies = IntStream.range(0, 1000)
    .mapToObj(i -> registry.create("SOLDIER"))
    .collect(Collectors.toList());
```

**Real examples:** `Object.clone()`, Spring's `BeanDefinition` prototype scope, game engines (spawning many identical enemies)

---

## Part 2: Structural Patterns

Structural patterns deal with **object composition** — how classes and objects are combined to form larger structures.

---

### 6. Adapter

**Intent:** Convert the interface of a class into another interface that clients expect. Lets incompatible classes work together.

**When to use:**
- Integrating a third-party library with an incompatible interface
- Wrapping legacy code behind a modern interface
- Reusing existing classes when interfaces don't match

**Java 21:**
```java
// Your system's interface
public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}

// Third-party Stripe SDK (interface you can't change)
public class StripeClient {
    public StripeCharge charge(String currency, long amountCents, String sourceToken) { ... }
    public StripeRefund refund(String chargeId, long amountCents) { ... }
}

// Adapter — wraps Stripe, exposes your interface
public class StripePaymentAdapter implements PaymentProcessor {
    private final StripeClient stripeClient;

    public StripePaymentAdapter(StripeClient stripeClient) {
        this.stripeClient = Objects.requireNonNull(stripeClient);
    }

    @Override
    public PaymentResult process(PaymentRequest request) {
        try {
            StripeCharge charge = stripeClient.charge(
                request.currency().code(),
                (long)(request.amount() * 100),  // cents conversion
                request.paymentToken()
            );
            return PaymentResult.success(charge.getId());
        } catch (StripeException e) {
            return PaymentResult.failure(e.getMessage());
        }
    }
}

// Your code never knows it's talking to Stripe
PaymentProcessor processor = new StripePaymentAdapter(new StripeClient(apiKey));
PaymentResult result = processor.process(new PaymentRequest(...));
```

**Real examples:** `Arrays.asList()`, `Collections.list()`, Spring's `HandlerAdapter`

---

### 7. Decorator

**Intent:** Attach additional behaviour to an object dynamically without subclassing. Decorators wrap the original object.

**When to use:**
- Add responsibilities to objects without modifying their class
- When extension by subclassing would lead to an explosion of subclasses
- When you need to add/remove behaviour at runtime

**Java 21:**
```java
// Component interface
public interface DataSource {
    void write(byte[] data);
    byte[] read();
}

// Concrete component
public class FileDataSource implements DataSource {
    private final Path filePath;

    public FileDataSource(Path filePath) { this.filePath = filePath; }

    @Override public void write(byte[] data) { /* write to file */ }
    @Override public byte[] read() { /* read from file */ return new byte[0]; }
}

// Base decorator
public abstract class DataSourceDecorator implements DataSource {
    protected final DataSource wrapped;

    protected DataSourceDecorator(DataSource wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }
}

// Concrete decorators — each adds one behaviour
public class EncryptionDecorator extends DataSourceDecorator {
    private final EncryptionKey key;

    public EncryptionDecorator(DataSource wrapped, EncryptionKey key) {
        super(wrapped);
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public void write(byte[] data) {
        wrapped.write(encrypt(data, key));  // encrypt before delegating
    }

    @Override
    public byte[] read() {
        return decrypt(wrapped.read(), key);  // decrypt after delegating
    }
}

public class CompressionDecorator extends DataSourceDecorator {
    public CompressionDecorator(DataSource wrapped) { super(wrapped); }

    @Override
    public void write(byte[] data) {
        wrapped.write(compress(data));
    }

    @Override
    public byte[] read() {
        return decompress(wrapped.read());
    }
}

// Usage — stack decorators at runtime
DataSource source = new FileDataSource(Paths.get("/data/file.dat"));
source = new EncryptionDecorator(source, encryptionKey);  // add encryption
source = new CompressionDecorator(source);                  // add compression on top
// Writes: compress → encrypt → file
// Reads: file → decrypt → decompress
```

**Real examples:** `java.io.BufferedInputStream(new FileInputStream(...))`, `java.io.GZIPOutputStream`

---

### 8. Facade

**Intent:** Provide a simplified interface to a complex subsystem.

**When to use:**
- Simplify interaction with a complex library/framework/subsystem
- Provide a clean entry point for a layered architecture
- Reduce dependencies between client code and subsystem internals

**Java 21:**
```java
// Complex subsystem — video encoding pipeline
public class VideoDecoder      { VideoStream decode(File f) {...} }
public class AudioMixer        { AudioStream mix(AudioStream... streams) {...} }
public class VideoEncoder      { File encode(VideoStream v, AudioStream a, Format f) {...} }
public class ThumbnailGenerator{ Image generate(VideoStream v, int atSecond) {...} }
public class MetadataExtractor { VideoMetadata extract(File f) {...} }

// Facade — simple interface hides the complexity
public class VideoConversionFacade {
    private final VideoDecoder decoder;
    private final AudioMixer audioMixer;
    private final VideoEncoder encoder;
    private final ThumbnailGenerator thumbnailGenerator;

    public VideoConversionFacade() {
        this.decoder            = new VideoDecoder();
        this.audioMixer         = new AudioMixer();
        this.encoder            = new VideoEncoder();
        this.thumbnailGenerator = new ThumbnailGenerator();
    }

    // Client calls this one method — not 5 different subsystem classes
    public ConversionResult convert(File inputFile, Format targetFormat) {
        VideoMetadata metadata = new MetadataExtractor().extract(inputFile);

        VideoStream videoStream = decoder.decode(inputFile);
        AudioStream audioStream = audioMixer.mix(videoStream.audioTracks());
        Image thumbnail = thumbnailGenerator.generate(videoStream, 5);
        File outputFile = encoder.encode(videoStream, audioStream, targetFormat);

        return new ConversionResult(outputFile, thumbnail, metadata);
    }
}

// Client code — no knowledge of the pipeline complexity
VideoConversionFacade facade = new VideoConversionFacade();
ConversionResult result = facade.convert(inputFile, Format.MP4);
```

**Real examples:** `javax.faces.context.FacesContext`, Spring's `JdbcTemplate`, SLF4J logging facade

---

### 9. Proxy

**Intent:** Provide a surrogate or placeholder for another object to control access to it.

**Three types:**
```
Virtual Proxy:    delay expensive object creation until actually needed
Protection Proxy: control access based on permissions
Remote Proxy:     represent remote object locally (RPC stub)
Caching Proxy:    cache results of expensive operations
```

**Java 21:**
```java
public interface UserService {
    User findById(String id);
    void createUser(User user);
}

// Caching + logging proxy
public class CachingUserServiceProxy implements UserService {
    private final UserService delegate;            // real service
    private final Cache<String, User> cache;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public CachingUserServiceProxy(UserService delegate, Cache<String, User> cache) {
        this.delegate = Objects.requireNonNull(delegate);
        this.cache    = Objects.requireNonNull(cache);
    }

    @Override
    public User findById(String id) {
        User cached = cache.get(id);
        if (cached != null) {
            log.debug("Cache hit for user: {}", id);
            return cached;
        }
        log.debug("Cache miss for user: {}", id);
        User user = delegate.findById(id);  // delegate to real service
        if (user != null) cache.put(id, user);
        return user;
    }

    @Override
    public void createUser(User user) {
        delegate.createUser(user);
        cache.put(user.getId(), user);  // populate cache on create
    }
}
```

**Real examples:** Spring AOP (transaction proxy, security proxy), JDK `java.lang.reflect.Proxy`, Hibernate lazy loading, RMI stubs

---

### 10. Composite

**Intent:** Compose objects into tree structures. Let clients treat individual objects and compositions uniformly.

**When to use:**
- Tree-like hierarchy: file systems, UI component trees, org charts, menus

**Java 21:**
```java
// Component — uniform interface for leaf and composite
public sealed interface FileSystemNode permits File, Directory {
    String name();
    long size();
    void print(String indent);
}

// Leaf
public record File(String name, long size) implements FileSystemNode {
    public File {
        Objects.requireNonNull(name, "File name required");
        if (size < 0) throw new IllegalArgumentException("File size cannot be negative");
    }
    @Override public void print(String indent) {
        System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
    }
}

// Composite
public final class Directory implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();

    public Directory(String name) {
        this.name = Objects.requireNonNull(name, "Directory name required");
    }

    public void add(FileSystemNode node) {
        Objects.requireNonNull(node);
        children.add(node);
    }

    public void remove(FileSystemNode node) { children.remove(node); }

    @Override public String name() { return name; }

    @Override
    public long size() {
        return children.stream().mapToLong(FileSystemNode::size).sum(); // recursive
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "📁 " + name + "/");
        children.forEach(child -> child.print(indent + "  "));
    }
}
```

**Real examples:** Java AWT/Swing component tree, XML DOM, JSON structure, org chart, arithmetic expression trees

---

### 11. Flyweight

**Intent:** Share common state between many fine-grained objects to reduce memory usage.

**When to use:**
- Large number of similar objects consuming too much memory
- Most object state can be made extrinsic (passed in, not stored)

**Java 21:**
```java
// Intrinsic state (shared) — font, size, bold, italic
public record CharacterStyle(String fontName, int fontSize, boolean bold, boolean italic) {}

// Flyweight factory — caches and reuses styles
public class CharacterStyleFactory {
    private final Map<CharacterStyle, CharacterStyle> pool = new HashMap<>();

    public CharacterStyle getStyle(String fontName, int fontSize, boolean bold, boolean italic) {
        CharacterStyle key = new CharacterStyle(fontName, fontSize, bold, italic);
        return pool.computeIfAbsent(key, k -> k);  // reuse if exists
    }
}

// Character — stores extrinsic state (position) + reference to shared style
public record DocumentCharacter(char character, int row, int col, CharacterStyle style) {}

// Document with 1M characters
// Without Flyweight: 1M CharacterStyle objects (each ~100 bytes = 100MB)
// With Flyweight: maybe 50 unique styles × 100 bytes = 5KB for styles
// 99.995% memory reduction on the style data
```

**Real examples:** Java `String.intern()`, `Integer.valueOf(-128..127)`, `Boolean.TRUE`/`FALSE`, character rendering in text editors

---

### 12. Bridge

**Intent:** Decouple an abstraction from its implementation so both can vary independently.

**When to use:**
- Both abstraction and implementation should be extensible via subclassing
- Implementation should be hidden from client completely

**Java 21:**
```java
// Implementation hierarchy
public interface Renderer {
    void renderCircle(double x, double y, double radius);
    void renderRectangle(double x, double y, double w, double h);
}

public class VectorRenderer implements Renderer { /* SVG/PDF rendering */ }
public class RasterRenderer implements Renderer { /* pixel/bitmap rendering */ }

// Abstraction hierarchy
public abstract class Shape {
    protected final Renderer renderer;  // bridge to implementation

    protected Shape(Renderer renderer) { this.renderer = renderer; }

    public abstract void draw();
    public abstract void resize(double factor);
}

public class Circle extends Shape {
    private double x, y, radius;

    public Circle(double x, double y, double radius, Renderer renderer) {
        super(renderer);
        this.x = x; this.y = y; this.radius = radius;
    }

    @Override public void draw() { renderer.renderCircle(x, y, radius); }
    @Override public void resize(double factor) { radius *= factor; }
}
```

---

## Part 3: Behavioural Patterns

Behavioural patterns deal with **communication between objects** — how responsibilities are distributed.

---

### 13. Observer

**Intent:** Define a one-to-many dependency. When one object changes state, all dependents are notified automatically.

**When to use:**
- Event systems, pub/sub, reactive programming
- A change to one object requires updating others, without knowing how many
- GUI event listeners, domain event publishing

**Java 21 — with generics:**
```java
// Generic observer interface
@FunctionalInterface
public interface EventListener<T> {
    void onEvent(T event);
}

// Event bus / subject
public class EventBus<T> {
    private final List<EventListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(EventListener<T> listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    public void unsubscribe(EventListener<T> listener) {
        listeners.remove(listener);
    }

    public void publish(T event) {
        Objects.requireNonNull(event);
        listeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Don't let one bad listener affect others
                log.error("Listener threw exception for event: {}", event, e);
            }
        });
    }
}

// Domain events
public record OrderPlacedEvent(String orderId, String customerId, double total) {}
public record OrderShippedEvent(String orderId, String trackingNumber) {}

// Usage
EventBus<OrderPlacedEvent> orderBus = new EventBus<>();

// Inventory service listens
orderBus.subscribe(event -> inventoryService.reserve(event.orderId()));
// Email service listens
orderBus.subscribe(event -> emailService.sendConfirmation(event.customerId()));
// Analytics listens
orderBus.subscribe(event -> analyticsService.track("order_placed", event.orderId()));

// Publisher doesn't know who's listening
orderBus.publish(new OrderPlacedEvent("ORD-123", "CUST-456", 99.99));
```

**Real examples:** `java.util.EventListener`, Spring's `ApplicationEventPublisher`, React's state updates, Node.js EventEmitter

---

### 14. Strategy

**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

**When to use:**
- Multiple variants of an algorithm exist
- Need to switch algorithm at runtime
- Related classes differ only in behaviour

**Java 21:**
```java
// Strategy interface
@FunctionalInterface
public interface SortStrategy<T> {
    void sort(List<T> list, Comparator<T> comparator);
}

// Concrete strategies
public class QuickSortStrategy<T> implements SortStrategy<T> {
    @Override
    public void sort(List<T> list, Comparator<T> comparator) {
        // QuickSort implementation
    }
}

public class MergeSortStrategy<T> implements SortStrategy<T> {
    @Override
    public void sort(List<T> list, Comparator<T> comparator) {
        // MergeSort — stable, good for linked lists
    }
}

// Context uses the strategy
public class DataProcessor<T> {
    private SortStrategy<T> sortStrategy;

    public DataProcessor(SortStrategy<T> sortStrategy) {
        this.sortStrategy = Objects.requireNonNull(sortStrategy);
    }

    // Switch strategy at runtime
    public void setSortStrategy(SortStrategy<T> strategy) {
        this.sortStrategy = Objects.requireNonNull(strategy);
    }

    public List<T> process(List<T> data, Comparator<T> comparator) {
        List<T> copy = new ArrayList<>(data);
        sortStrategy.sort(copy, comparator);
        return copy;
    }
}

// Lambda as strategy — Java 8+ makes this natural
DataProcessor<Order> processor = new DataProcessor<>(List::sort);  // built-in sort
processor.setSortStrategy((list, cmp) -> Collections.sort(list, cmp)); // switch at runtime
```

**Strategy vs State:** Strategy = inject different algorithms. State = object changes its own behaviour based on internal state.

**Real examples:** `java.util.Comparator`, `java.util.concurrent.ThreadPoolExecutor` (rejection policies), Spring's `ResourceLoader`

---

### 15. Command

**Intent:** Encapsulate a request as an object. Enables undo/redo, queuing, logging, and parameterising operations.

**When to use:**
- Undo/redo functionality
- Queue or schedule operations
- Logging operations for audit trail
- Transactional behaviour (execute + rollback)

**Java 21:**
```java
// Command interface with undo
public interface Command {
    void execute();
    void undo();
    String description();
}

// Concrete commands
public class MoveCommand implements Command {
    private final Shape shape;
    private final int deltaX, deltaY;

    public MoveCommand(Shape shape, int deltaX, int deltaY) {
        this.shape = Objects.requireNonNull(shape);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override public void execute() { shape.move(deltaX, deltaY); }
    @Override public void undo()    { shape.move(-deltaX, -deltaY); }
    @Override public String description() {
        return String.format("Move %s by (%d, %d)", shape.getId(), deltaX, deltaY);
    }
}

// Command history — enables undo/redo
public class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final int maxSize;

    public CommandHistory(int maxSize) { this.maxSize = maxSize; }

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();  // new action clears redo history
        if (undoStack.size() > maxSize) {
            // Remove oldest command from bottom of stack
            ((ArrayDeque<Command>) undoStack).removeLast();
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo");
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo");
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}
```

**Real examples:** Java `Runnable`/`Callable` (command objects), Spring `@Transactional` (wraps operations), database transactions, text editor history

---

### 16. State

**Intent:** Allow an object to alter its behaviour when its internal state changes. The object will appear to change its class.

**When to use:**
- Object's behaviour depends heavily on its state
- Operations have complex conditional behaviour based on state
- State transitions are explicit and need to be managed

**Java 21 — Vending Machine states:**
```java
public enum VendingMachineState { IDLE, ITEM_SELECTED, PAYMENT_PENDING, DISPENSING }

public interface VendingState {
    void selectItem(VendingMachine machine, String itemCode);
    void insertCoin(VendingMachine machine, double amount);
    void dispense(VendingMachine machine);
    void cancel(VendingMachine machine);
}

public class IdleState implements VendingState {
    @Override
    public void selectItem(VendingMachine machine, String itemCode) {
        if (!machine.hasItem(itemCode)) {
            System.out.println("Item not available: " + itemCode);
            return;
        }
        machine.setSelectedItem(itemCode);
        machine.setState(new ItemSelectedState());
        System.out.println("Item selected: " + itemCode + ". Please insert coins.");
    }

    @Override
    public void insertCoin(VendingMachine machine, double amount) {
        System.out.println("Please select an item first");
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("Please select an item and pay first");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Nothing to cancel");
    }
}

public class PaymentPendingState implements VendingState {
    @Override
    public void insertCoin(VendingMachine machine, double amount) {
        machine.addBalance(amount);
        double required = machine.getSelectedItemPrice();
        if (machine.getBalance() >= required) {
            machine.setState(new DispensingState());
            machine.dispenseCurrentItem();
        } else {
            System.out.printf("Insert %.2f more%n", required - machine.getBalance());
        }
    }

    @Override
    public void selectItem(VendingMachine machine, String itemCode) {
        System.out.println("Item already selected. Please complete payment or cancel.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("Please complete payment first");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.printf("Returning %.2f%n", machine.getBalance());
        machine.resetBalance();
        machine.setSelectedItem(null);
        machine.setState(new IdleState());
    }
}
```

**Real examples:** TCP connection states (LISTEN/SYN_SENT/ESTABLISHED/CLOSE_WAIT), order lifecycle, media player (playing/paused/stopped)

---

### 17. Template Method

**Intent:** Define the skeleton of an algorithm in a base class. Let subclasses fill in specific steps without changing the algorithm's structure.

**When to use:**
- Multiple classes share the same algorithm structure but differ in specific steps
- You want to control which parts subclasses can override

**Java 21:**
```java
// Abstract template
public abstract class DataImporter {

    // Template method — defines the algorithm skeleton
    public final ImportResult importData(InputStream source) {
        Objects.requireNonNull(source, "Source stream required");

        List<RawRecord> rawRecords = readRecords(source);          // step 1 — abstract
        List<RawRecord> valid = validateRecords(rawRecords);       // step 2 — can override
        List<DomainObject> parsed = parseRecords(valid);           // step 3 — abstract
        persistRecords(parsed);                                     // step 4 — final
        return new ImportResult(rawRecords.size(), parsed.size());
    }

    // Abstract — subclasses MUST implement
    protected abstract List<RawRecord> readRecords(InputStream source);
    protected abstract List<DomainObject> parseRecords(List<RawRecord> records);

    // Hook — subclasses CAN override but don't have to
    protected List<RawRecord> validateRecords(List<RawRecord> records) {
        return records.stream()
            .filter(r -> r != null && !r.isEmpty())
            .collect(Collectors.toList());
    }

    // Final — subclasses CANNOT override
    private void persistRecords(List<DomainObject> records) {
        repository.saveAll(records);
    }
}

public class CsvDataImporter extends DataImporter {
    @Override
    protected List<RawRecord> readRecords(InputStream source) {
        // CSV-specific parsing
        return CsvParser.parse(source);
    }

    @Override
    protected List<DomainObject> parseRecords(List<RawRecord> records) {
        return records.stream().map(CsvMapper::toDomain).collect(Collectors.toList());
    }
}

public class JsonDataImporter extends DataImporter {
    @Override
    protected List<RawRecord> readRecords(InputStream source) {
        return JsonParser.parseArray(source);
    }

    @Override
    protected List<DomainObject> parseRecords(List<RawRecord> records) {
        return records.stream().map(JsonMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    protected List<RawRecord> validateRecords(List<RawRecord> records) {
        // JSON-specific validation on top of base validation
        List<RawRecord> base = super.validateRecords(records);
        return base.stream().filter(JsonValidator::isValidSchema).collect(Collectors.toList());
    }
}
```

**Real examples:** `java.io.InputStream.read()`, `HttpServlet.service()` (calls doGet/doPost), JUnit `@BeforeEach`/`@AfterEach`, Spring's `AbstractController`

---

### 18. Chain of Responsibility

**Intent:** Pass a request along a chain of handlers. Each handler decides to process or pass to the next.

**When to use:**
- Multiple handlers may process a request
- Handler set should be determined dynamically
- Middleware/filter pipelines: HTTP request processing, authentication chains

**Java 21:**
```java
public abstract class RequestHandler {
    private RequestHandler next;

    public RequestHandler setNext(RequestHandler next) {
        this.next = next;
        return next;  // enables chaining: h1.setNext(h2).setNext(h3)
    }

    public final void handle(HttpRequest request, HttpResponse response) {
        if (shouldHandle(request)) {
            process(request, response);
        }
        if (next != null && !response.isCompleted()) {
            next.handle(request, response);
        }
    }

    protected abstract boolean shouldHandle(HttpRequest request);
    protected abstract void process(HttpRequest request, HttpResponse response);
}

public class AuthenticationHandler extends RequestHandler {
    private final TokenValidator tokenValidator;

    @Override
    protected boolean shouldHandle(HttpRequest request) { return true; }

    @Override
    protected void process(HttpRequest request, HttpResponse response) {
        String token = request.getHeader("Authorization");
        if (token == null || !tokenValidator.isValid(token)) {
            response.setStatus(401);
            response.setBody("Unauthorized");
            response.complete();
        }
    }
}

public class RateLimitHandler extends RequestHandler {
    private final RateLimiter rateLimiter;

    @Override
    protected boolean shouldHandle(HttpRequest request) { return true; }

    @Override
    protected void process(HttpRequest request, HttpResponse response) {
        if (!rateLimiter.tryAcquire(request.getClientId())) {
            response.setStatus(429);
            response.setBody("Too Many Requests");
            response.complete();
        }
    }
}

// Build the chain
RequestHandler chain = new AuthenticationHandler(tokenValidator);
chain.setNext(new RateLimitHandler(rateLimiter))
     .setNext(new LoggingHandler())
     .setNext(new BusinessLogicHandler());

chain.handle(request, response);
```

**Real examples:** Java `logging` API (Logger → Handler hierarchy), Servlet `FilterChain`, Spring Security filter chain, Express.js middleware

---

### 19. Iterator

**Intent:** Provide a way to sequentially access elements of a collection without exposing its underlying representation.

**Java 21:**
```java
// Custom iterator for a binary tree (in-order traversal)
public class BinaryTree<T extends Comparable<T>> implements Iterable<T> {
    private Node<T> root;

    private record Node<T>(T value, Node<T> left, Node<T> right) {}

    public void insert(T value) { /* BST insert */ }

    @Override
    public Iterator<T> iterator() {
        return new InOrderIterator<>(root);
    }

    private static class InOrderIterator<T> implements Iterator<T> {
        private final Deque<Node<T>> stack = new ArrayDeque<>();

        InOrderIterator(Node<T> root) { pushLeft(root); }

        private void pushLeft(Node<T> node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public boolean hasNext() { return !stack.isEmpty(); }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            Node<T> node = stack.pop();
            pushLeft(node.right);
            return node.value;
        }
    }
}

// For-each works because BinaryTree implements Iterable
BinaryTree<Integer> tree = new BinaryTree<>();
tree.insert(5); tree.insert(3); tree.insert(7); tree.insert(1);
for (int value : tree) {  // 1, 3, 5, 7
    System.out.println(value);
}
```

**Real examples:** All Java Collections (`ArrayList`, `LinkedList`), `Scanner`, `ResultSet`

---

### 20. Other Important Patterns

**Mediator:** Objects communicate through a mediator rather than directly. Reduces coupling between many classes.
```
Chat room: User → ChatRoom → User (not User → User directly)
Air traffic control: Plane → Tower → Plane
```

**Memento:** Capture an object's internal state so it can be restored later. Used in undo systems.
```
Editor's undo history stores Mementos of document state
Game save points store Mementos of game state
```

**Visitor:** Add new operations to a class hierarchy without modifying the classes.
```
AST (Abstract Syntax Tree) visitors: add pretty-printer, type-checker, optimizer
each as a new Visitor without touching the AST node classes
```

**Interpreter:** Define a grammar and an interpreter for a simple language.
```
SQL parser, regex engine, expression evaluator
```

---

## Pattern Selection Quick Reference

```
Object creation:
  One instance → Singleton
  Create without knowing type → Factory Method
  Family of related objects → Abstract Factory
  Complex object step-by-step → Builder
  Copy existing object → Prototype

Structure:
  Incompatible interfaces → Adapter
  Add behaviour dynamically → Decorator
  Simplify complex subsystem → Facade
  Control access → Proxy
  Tree structures → Composite
  Memory-efficient shared objects → Flyweight
  Vary abstraction and implementation independently → Bridge

Behaviour:
  Notify dependents → Observer
  Swap algorithms → Strategy
  Encapsulate requests → Command
  State-dependent behaviour → State
  Algorithm skeleton with variable steps → Template Method
  Chain of handlers → Chain of Responsibility
  Sequential access → Iterator
  Reduce class coupling → Mediator
```

---

## Interview Q&A

**Q: What's the difference between Strategy and State patterns?**
A: Both encapsulate behaviour behind an interface, but for different purposes. Strategy: the algorithm/behaviour is chosen externally and injected. The object doesn't decide which strategy to use — the caller does. The strategy doesn't change itself. State: the object transitions between states based on its own logic. States know about each other and trigger transitions. The object appears to change its class as its internal state changes. Practical test: if the "algorithm" is selected by the client and the object doesn't change it → Strategy. If the object itself transitions between behaviours based on what happened to it → State.

**Q: When would you use Decorator instead of Inheritance to add behaviour?**
A: Decorator when: (1) you need to add behaviour to an object you don't own or can't subclass (final class), (2) you need to combine multiple behaviours dynamically at runtime (`new Compression(new Encryption(new FileStore()))`), (3) extension by inheritance would create a combinatorial explosion (compression + encryption + logging = 8 subclasses). Inheritance when: the new behaviour is fundamental to the type (every Dog barks — it's not an optional add-on), and you only have one or two variants. If you catch yourself saying "sometimes I need encryption, sometimes not, sometimes both" → Decorator wins.

**Q: You're seeing a switch statement on a type field scattered across multiple methods. What design problem is this and how do you fix it?**
A: This is the "type code" smell — behaviour that should be in subclasses is centrally dispatched by type. Replace with polymorphism. Each `case` becomes a subclass (or separate implementation of an interface), and the method becomes a virtual dispatch. If the type can't be changed after creation → use sealed interfaces with pattern matching. If the behaviour needs to vary per type → Strategy pattern. If you need to add new operations (not new types) without modifying the hierarchy → Visitor pattern. In Java 21: sealed classes + switch expressions give you compile-time exhaustiveness checking as a safety net.

**Q: How do you apply the Facade pattern in a microservices context?**
A: The API Gateway IS a Facade — it provides a unified interface to multiple downstream microservices. Clients call `GET /user-dashboard` and the gateway (facade) calls User Service, Order Service, Recommendation Service, and assembles the response. Each microservice is a subsystem. The gateway hides the complexity of N service calls behind one clean endpoint. Within a single service, a Facade can aggregate a complex internal pipeline (messaging, persistence, notification) behind a single `OrderService.placeOrder()` call.
