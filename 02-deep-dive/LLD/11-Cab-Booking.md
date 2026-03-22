# LLD — Cab Booking System (Uber-style) — Complete Java 21

## Design Summary
| Aspect | Decision |
|--------|----------|
| Driver matching | **Strategy** — NearestDriver, HighestRatedDriver, SurgeAwareStrategy |
| Ride state machine | **State** pattern — REQUESTED→ACCEPTED→ARRIVED→IN_PROGRESS→COMPLETED/CANCELLED |
| Fare calculation | **Strategy** — StandardFare, SurgeFare, SubscriptionFare |
| Location | Simple `(lat, lng)` with Haversine distance |
| Driver availability | In-memory map; production = geospatial index (Redis GEORADIUS) |
| Notifications | **Observer** — driver + rider notified on state transitions |

## Complete Solution

```java
package lld.cabooking;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

// ── Location & Distance ───────────────────────────────────────────────────────

record Location(double lat, double lng) {
    Location {
        if (lat < -90 || lat > 90)   throw new IllegalArgumentException("Invalid latitude: " + lat);
        if (lng < -180 || lng > 180) throw new IllegalArgumentException("Invalid longitude: " + lng);
    }

    /** Haversine formula — distance in kilometres */
    double distanceTo(Location other) {
        double R    = 6371.0;
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double a    = Math.sin(dLat/2) * Math.sin(dLat/2)
                    + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                    * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override public String toString() {
        return String.format("(%.4f, %.4f)", lat, lng);
    }
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum RideStatus {
    REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVED,
    IN_PROGRESS, COMPLETED, CANCELLED
}

enum VehicleType  { AUTO, MINI, SEDAN, SUV, PREMIUM }
enum DriverStatus { AVAILABLE, ON_TRIP, OFFLINE }
enum CancelReason { RIDER_CANCELLED, DRIVER_CANCELLED, NO_DRIVER_FOUND, PAYMENT_FAILED }

// ── Money ─────────────────────────────────────────────────────────────────────

record Money(long paise) {
    static Money ofRupees(double r)  { return new Money(Math.round(r * 100)); }
    Money add(Money o)               { return new Money(paise + o.paise); }
    Money multiply(double f)         { return new Money(Math.round(paise * f)); }
    double toRupees()                { return paise / 100.0; }
    @Override public String toString(){ return String.format("₹%.2f", toRupees()); }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class CabException           extends RuntimeException { CabException(String m) { super(m); } }
class NoDriverAvailableException extends CabException {
    NoDriverAvailableException(VehicleType t) { super("No driver available for: " + t); }
}
class InvalidRideStateException  extends CabException {
    InvalidRideStateException(String exp, RideStatus actual) {
        super("Expected state " + exp + " but got: " + actual); }
}

// ── Rating ────────────────────────────────────────────────────────────────────

record Rating(double value, String comment, Instant ratedAt) {
    Rating {
        if (value < 1.0 || value > 5.0)
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
    }
}

// ── Vehicle ───────────────────────────────────────────────────────────────────

record Vehicle(String vehicleId, String licensePlate,
               VehicleType type, String model, int year) {
    Vehicle {
        Objects.requireNonNull(vehicleId,      "Vehicle ID required");
        Objects.requireNonNull(licensePlate,   "License plate required");
        Objects.requireNonNull(type,           "Vehicle type required");
        Objects.requireNonNull(model,          "Model required");
        if (licensePlate.isBlank()) throw new IllegalArgumentException("License plate required");
        if (year < 2000 || year > Year.now().getValue())
            throw new IllegalArgumentException("Invalid year: " + year);
    }
}

// ── Driver ────────────────────────────────────────────────────────────────────

class Driver {
    private final String     driverId;
    private final String     name;
    private final String     phone;
    private final Vehicle    vehicle;
    private Location         currentLocation;
    private DriverStatus     status;
    private final List<Rating> ratings = new ArrayList<>();

    Driver(String driverId, String name, String phone, Vehicle vehicle, Location startLocation) {
        this.driverId        = Objects.requireNonNull(driverId);
        this.name            = Objects.requireNonNull(name);
        this.phone           = Objects.requireNonNull(phone);
        this.vehicle         = Objects.requireNonNull(vehicle);
        this.currentLocation = Objects.requireNonNull(startLocation);
        this.status          = DriverStatus.AVAILABLE;
        if (name.isBlank()) throw new IllegalArgumentException("Driver name required");
    }

    void updateLocation(Location loc) { this.currentLocation = Objects.requireNonNull(loc); }
    void setStatus(DriverStatus s)    { this.status = Objects.requireNonNull(s); }
    void addRating(Rating r)          { ratings.add(r); }

    double getAverageRating() {
        if (ratings.isEmpty()) return 5.0;
        return ratings.stream().mapToDouble(Rating::value).average().orElse(5.0);
    }

    double distanceTo(Location target) { return currentLocation.distanceTo(target); }

    public String       getDriverId()       { return driverId; }
    public String       getName()           { return name; }
    public String       getPhone()          { return phone; }
    public Vehicle      getVehicle()        { return vehicle; }
    public Location     getCurrentLocation(){ return currentLocation; }
    public DriverStatus getStatus()         { return status; }
    public boolean      isAvailable()       { return status == DriverStatus.AVAILABLE; }

    @Override public String toString() {
        return String.format("Driver[%s %s %s %.1f★ %s]",
            driverId, name, vehicle.type(), getAverageRating(), status);
    }
}

// ── Rider ─────────────────────────────────────────────────────────────────────

class Rider {
    private final String     riderId;
    private final String     name;
    private final String     phone;
    private final String     email;
    private final List<Rating> ratings = new ArrayList<>();

    Rider(String riderId, String name, String phone, String email) {
        this.riderId = Objects.requireNonNull(riderId);
        this.name    = Objects.requireNonNull(name);
        this.phone   = Objects.requireNonNull(phone);
        this.email   = Objects.requireNonNull(email);
        if (name.isBlank())  throw new IllegalArgumentException("Rider name required");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }

    void addRating(Rating r) { ratings.add(r); }

    double getAverageRating() {
        return ratings.stream().mapToDouble(Rating::value).average().orElse(5.0);
    }

    public String getRiderId() { return riderId; }
    public String getName()    { return name; }
    public String getPhone()   { return phone; }
    public String getEmail()   { return email; }

    @Override public String toString() {
        return String.format("Rider[%s %s %.1f★]", riderId, name, getAverageRating());
    }
}

// ── Fare Strategy ─────────────────────────────────────────────────────────────

interface FareStrategy {
    Money calculate(double distanceKm, Duration duration, VehicleType vehicleType);
}

class StandardFareStrategy implements FareStrategy {
    private static final Map<VehicleType, Double> BASE_FARE = Map.of(
        VehicleType.AUTO,    30.0,
        VehicleType.MINI,    40.0,
        VehicleType.SEDAN,   60.0,
        VehicleType.SUV,     80.0,
        VehicleType.PREMIUM, 120.0
    );
    private static final Map<VehicleType, Double> PER_KM = Map.of(
        VehicleType.AUTO,    8.0,
        VehicleType.MINI,    10.0,
        VehicleType.SEDAN,   14.0,
        VehicleType.SUV,     18.0,
        VehicleType.PREMIUM, 25.0
    );
    private static final double PER_MIN = 1.5;

    @Override
    public Money calculate(double distanceKm, Duration duration, VehicleType vehicleType) {
        double base     = BASE_FARE.getOrDefault(vehicleType, 40.0);
        double distance = PER_KM.getOrDefault(vehicleType, 10.0) * distanceKm;
        double time     = PER_MIN * duration.toMinutes();
        return Money.ofRupees(base + distance + time);
    }
}

class SurgeFareStrategy implements FareStrategy {
    private final FareStrategy base;
    private final double       surgeMultiplier;

    SurgeFareStrategy(FareStrategy base, double surgeMultiplier) {
        this.base            = Objects.requireNonNull(base);
        this.surgeMultiplier = surgeMultiplier;
        if (surgeMultiplier < 1.0)
            throw new IllegalArgumentException("Surge multiplier must be >= 1.0");
    }

    @Override
    public Money calculate(double distanceKm, Duration duration, VehicleType vehicleType) {
        return base.calculate(distanceKm, duration, vehicleType).multiply(surgeMultiplier);
    }
}

// ── Driver Matching Strategy ──────────────────────────────────────────────────

interface DriverMatchStrategy {
    Optional<Driver> match(List<Driver> available, Location pickup, VehicleType vehicleType);
}

class NearestDriverStrategy implements DriverMatchStrategy {
    private static final double MAX_RADIUS_KM = 5.0;

    @Override
    public Optional<Driver> match(List<Driver> available, Location pickup, VehicleType vehicleType) {
        return available.stream()
            .filter(d -> d.getVehicle().type() == vehicleType)
            .filter(d -> d.distanceTo(pickup) <= MAX_RADIUS_KM)
            .min(Comparator.comparingDouble(d -> d.distanceTo(pickup)));
    }
}

class HighestRatedDriverStrategy implements DriverMatchStrategy {
    @Override
    public Optional<Driver> match(List<Driver> available, Location pickup, VehicleType vehicleType) {
        return available.stream()
            .filter(d -> d.getVehicle().type() == vehicleType)
            .filter(d -> d.distanceTo(pickup) <= 8.0)  // wider radius
            .max(Comparator.comparingDouble(Driver::getAverageRating));
    }
}

// ── Ride ──────────────────────────────────────────────────────────────────────

class Ride {
    private static int counter = 9000;

    private final String     rideId;
    private final Rider      rider;
    private final VehicleType vehicleType;
    private final Location   pickup;
    private final Location   dropoff;
    private Driver           driver;
    private RideStatus       status;
    private Money            estimatedFare;
    private Money            actualFare;
    private Instant          requestedAt;
    private Instant          startedAt;
    private Instant          completedAt;
    private CancelReason     cancelReason;

    Ride(Rider rider, Location pickup, Location dropoff, VehicleType vehicleType) {
        this.rideId      = "RDE-" + counter++;
        this.rider       = Objects.requireNonNull(rider);
        this.pickup      = Objects.requireNonNull(pickup);
        this.dropoff     = Objects.requireNonNull(dropoff);
        this.vehicleType = Objects.requireNonNull(vehicleType);
        this.status      = RideStatus.REQUESTED;
        this.requestedAt = Instant.now();
    }

    // ── State Transitions ─────────────────────────────────────────────────────

    void assignDriver(Driver driver, Money estimatedFare) {
        requireStatus(RideStatus.REQUESTED);
        this.driver        = Objects.requireNonNull(driver);
        this.estimatedFare = Objects.requireNonNull(estimatedFare);
        this.status        = RideStatus.DRIVER_ASSIGNED;
        driver.setStatus(DriverStatus.ON_TRIP);
    }

    void driverArrived() {
        requireStatus(RideStatus.DRIVER_ASSIGNED);
        status = RideStatus.DRIVER_ARRIVED;
    }

    void startRide() {
        requireStatus(RideStatus.DRIVER_ARRIVED);
        status    = RideStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    void completeRide(Money actualFare) {
        requireStatus(RideStatus.IN_PROGRESS);
        this.actualFare  = Objects.requireNonNull(actualFare);
        this.status      = RideStatus.COMPLETED;
        this.completedAt = Instant.now();
        if (driver != null) driver.setStatus(DriverStatus.AVAILABLE);
    }

    void cancel(CancelReason reason) {
        if (status == RideStatus.COMPLETED || status == RideStatus.CANCELLED)
            throw new InvalidRideStateException("CANCELLABLE", status);
        status       = RideStatus.CANCELLED;
        cancelReason = reason;
        if (driver != null) driver.setStatus(DriverStatus.AVAILABLE);
    }

    private void requireStatus(RideStatus required) {
        if (status != required)
            throw new InvalidRideStateException(required.name(), status);
    }

    // ── Computed Properties ───────────────────────────────────────────────────

    double getDistanceKm()        { return pickup.distanceTo(dropoff); }

    Duration getActualDuration() {
        if (startedAt == null || completedAt == null) return Duration.ZERO;
        return Duration.between(startedAt, completedAt);
    }

    public String      getRideId()        { return rideId; }
    public Rider       getRider()         { return rider; }
    public Driver      getDriver()        { return driver; }
    public Location    getPickup()        { return pickup; }
    public Location    getDropoff()       { return dropoff; }
    public VehicleType getVehicleType()   { return vehicleType; }
    public RideStatus  getStatus()        { return status; }
    public Money       getEstimatedFare() { return estimatedFare; }
    public Money       getActualFare()    { return actualFare; }

    @Override public String toString() {
        return String.format("Ride[%s %s→%s %s %s fare=%s]",
            rideId, pickup, dropoff, vehicleType, status,
            actualFare != null ? actualFare : estimatedFare);
    }
}

// ── Notification Service (Observer) ──────────────────────────────────────────

interface RideNotificationListener {
    void onRideEvent(String event, Ride ride);
}

class ConsoleNotificationListener implements RideNotificationListener {
    @Override
    public void onRideEvent(String event, Ride ride) {
        System.out.printf("📱 [%s] Ride %s — %s%n", event, ride.getRideId(), ride.getStatus());
    }
}

// ── Cab Booking Service ───────────────────────────────────────────────────────

class CabBookingService {
    private final Map<String, Driver>  drivers  = new ConcurrentHashMap<>();
    private final Map<String, Rider>   riders   = new ConcurrentHashMap<>();
    private final Map<String, Ride>    rides    = new ConcurrentHashMap<>();
    private final FareStrategy         fareStrategy;
    private final DriverMatchStrategy  matchStrategy;
    private final List<RideNotificationListener> listeners = new CopyOnWriteArrayList<>();

    CabBookingService(FareStrategy fareStrategy, DriverMatchStrategy matchStrategy) {
        this.fareStrategy  = Objects.requireNonNull(fareStrategy);
        this.matchStrategy = Objects.requireNonNull(matchStrategy);
    }

    void addNotificationListener(RideNotificationListener l) { listeners.add(l); }

    // ── Registration ──────────────────────────────────────────────────────────

    public Driver registerDriver(String id, String name, String phone, Vehicle vehicle, Location loc) {
        Driver driver = new Driver(id, name, phone, vehicle, loc);
        drivers.put(id, driver);
        System.out.println("🚗 Driver registered: " + driver);
        return driver;
    }

    public Rider registerRider(String id, String name, String phone, String email) {
        Rider rider = new Rider(id, name, phone, email);
        riders.put(id, rider);
        System.out.println("👤 Rider registered: " + rider);
        return rider;
    }

    // ── Ride Lifecycle ────────────────────────────────────────────────────────

    public Ride requestRide(String riderId, Location pickup, Location dropoff,
                            VehicleType vehicleType) {
        Rider rider = getrider(riderId);

        // Find available driver
        List<Driver> available = drivers.values().stream()
            .filter(Driver::isAvailable)
            .collect(Collectors.toList());

        Driver driver = matchStrategy.match(available, pickup, vehicleType)
            .orElseThrow(() -> new NoDriverAvailableException(vehicleType));

        // Estimate fare
        double distKm  = pickup.distanceTo(dropoff);
        Money  estimate = fareStrategy.calculate(distKm, Duration.ofMinutes(
            (long)(distKm * 3)), vehicleType);  // rough estimate: 3 min/km

        Ride ride = new Ride(rider, pickup, dropoff, vehicleType);
        ride.assignDriver(driver, estimate);
        rides.put(ride.getRideId(), ride);

        notify("RIDE_REQUESTED", ride);
        System.out.printf("🚕 Ride requested: %s | Driver: %s | Est. fare: %s%n",
            ride.getRideId(), driver.getName(), estimate);
        return ride;
    }

    public void driverArrived(String rideId) {
        Ride ride = getRide(rideId);
        ride.driverArrived();
        notify("DRIVER_ARRIVED", ride);
        System.out.println("📍 Driver arrived for: " + rideId);
    }

    public void startRide(String rideId) {
        Ride ride = getRide(rideId);
        ride.startRide();
        notify("RIDE_STARTED", ride);
        System.out.println("▶ Ride started: " + rideId);
    }

    public Money completeRide(String rideId) {
        Ride ride = getRide(rideId);
        // Simulate actual duration
        Duration actualDuration = Duration.ofMinutes(
            (long)(ride.getDistanceKm() * 2.5 + 5));  // traffic simulation
        Money actualFare = fareStrategy.calculate(
            ride.getDistanceKm(), actualDuration, ride.getVehicleType());
        ride.completeRide(actualFare);
        notify("RIDE_COMPLETED", ride);
        System.out.printf("✅ Ride completed: %s | Fare: %s | Distance: %.2fkm%n",
            rideId, actualFare, ride.getDistanceKm());
        return actualFare;
    }

    public void cancelRide(String rideId, CancelReason reason) {
        Ride ride = getRide(rideId);
        ride.cancel(reason);
        notify("RIDE_CANCELLED", ride);
        System.out.println("❌ Ride cancelled: " + rideId + " | Reason: " + reason);
    }

    // ── Driver Operations ─────────────────────────────────────────────────────

    public void updateDriverLocation(String driverId, Location location) {
        getDriver(driverId).updateLocation(location);
    }

    public void setDriverOffline(String driverId) {
        Driver driver = getDriver(driverId);
        if (driver.getStatus() == DriverStatus.ON_TRIP)
            throw new CabException("Cannot go offline while on trip");
        driver.setStatus(DriverStatus.OFFLINE);
        System.out.println("🔴 Driver offline: " + driver.getName());
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    public void rateDriver(String rideId, double rating, String comment) {
        Ride ride = getRide(rideId);
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw new CabException("Can only rate completed rides");
        ride.getDriver().addRating(new Rating(rating, comment, Instant.now()));
        System.out.printf("⭐ Driver %s rated: %.1f%n", ride.getDriver().getName(), rating);
    }

    public void rateRider(String rideId, double rating, String comment) {
        Ride ride = getRide(rideId);
        if (ride.getStatus() != RideStatus.COMPLETED)
            throw new CabException("Can only rate completed rides");
        ride.getRider().addRating(new Rating(rating, comment, Instant.now()));
        System.out.printf("⭐ Rider %s rated: %.1f%n", ride.getRider().getName(), rating);
    }

    // ── Display ───────────────────────────────────────────────────────────────

    public void displayDriverStats() {
        System.out.println("\n═══ Driver Statistics ═══");
        drivers.values().forEach(d ->
            System.out.printf("  %s | Rating: %.2f★ | Status: %s%n",
                d.getName(), d.getAverageRating(), d.getStatus()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void notify(String event, Ride ride) {
        listeners.forEach(l -> l.onRideEvent(event, ride));
    }

    private Ride   getRide(String id)  { return Optional.ofNullable(rides.get(id))
        .orElseThrow(() -> new CabException("Ride not found: " + id)); }
    private Driver getDriver(String id){ return Optional.ofNullable(drivers.get(id))
        .orElseThrow(() -> new CabException("Driver not found: " + id)); }
    private Rider  getRider(String id) { return Optional.ofNullable(riders.get(id))
        .orElseThrow(() -> new CabException("Rider not found: " + id)); }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class CabBookingDemo {
    public static void main(String[] args) {
        CabBookingService service = new CabBookingService(
            new StandardFareStrategy(),
            new NearestDriverStrategy()
        );
        service.addNotificationListener(new ConsoleNotificationListener());

        // Hyderabad locations
        Location hitech  = new Location(17.4474, 78.3762);
        Location gachibowli = new Location(17.4401, 78.3489);
        Location madhapur = new Location(17.4485, 78.3908);

        // Register drivers
        Vehicle auto  = new Vehicle("V1", "TS09EA1234", VehicleType.AUTO,  "Bajaj RE",    2022);
        Vehicle sedan = new Vehicle("V2", "TS10CB5678", VehicleType.SEDAN, "Maruti Swift", 2023);
        Vehicle suv   = new Vehicle("V3", "TS11DC9012", VehicleType.SUV,   "Innova Crysta",2022);

        service.registerDriver("D1", "Ravi Kumar",   "9876543210", auto,  hitech);
        service.registerDriver("D2", "Suresh Reddy", "9876543211", sedan, gachibowli);
        service.registerDriver("D3", "Mahesh Rao",   "9876543212", suv,   madhapur);

        // Register riders
        service.registerRider("R1", "Priya Singh", "9111222333", "priya@example.com");
        service.registerRider("R2", "Arjun Nair",  "9444555666", "arjun@example.com");

        // ── Scenario 1: Normal ride ───────────────────────────────────────────
        System.out.println("\n=== Scenario 1: Complete Auto Ride ===");
        Ride ride1 = service.requestRide("R1", hitech, gachibowli, VehicleType.AUTO);
        service.driverArrived(ride1.getRideId());
        service.startRide(ride1.getRideId());
        Money fare1 = service.completeRide(ride1.getRideId());
        service.rateDriver(ride1.getRideId(), 4.5, "Good driver");
        service.rateRider(ride1.getRideId(), 5.0, "Polite rider");

        // ── Scenario 2: Surge pricing ────────────────────────────────────────
        System.out.println("\n=== Scenario 2: Surge Pricing (1.5x) ===");
        CabBookingService surgeService = new CabBookingService(
            new SurgeFareStrategy(new StandardFareStrategy(), 1.5),
            new NearestDriverStrategy()
        );
        surgeService.registerDriver("D4", "Vijay Sharma", "9777888999", sedan, hitech);
        surgeService.registerRider("R3", "Kavya Reddy", "9222333444", "kavya@example.com");
        Ride surgeRide = surgeService.requestRide("R3", hitech, madhapur, VehicleType.SEDAN);
        surgeService.startRide(surgeRide.getRideId());
        System.out.println("Surge fare: " + surgeService.completeRide(surgeRide.getRideId()));

        // ── Scenario 3: Cancellation ─────────────────────────────────────────
        System.out.println("\n=== Scenario 3: Rider Cancellation ===");
        Ride ride3 = service.requestRide("R2", hitech, madhapur, VehicleType.SEDAN);
        service.cancelRide(ride3.getRideId(), CancelReason.RIDER_CANCELLED);

        // ── Scenario 4: No driver available ──────────────────────────────────
        System.out.println("\n=== Scenario 4: No SUV Available at Pickup ===");
        // D3 is at madhapur, which is > 5km from gachibowli (NearestDriver radius)
        try {
            service.requestRide("R1", gachibowli, hitech, VehicleType.SUV);
        } catch (NoDriverAvailableException e) {
            System.out.println("❌ " + e.getMessage());
        }

        // ── Scenario 5: Wrong state transition ───────────────────────────────
        System.out.println("\n=== Scenario 5: Wrong State Transition ===");
        Ride ride5 = service.requestRide("R2", hitech, gachibowli, VehicleType.SEDAN);
        try {
            service.startRide(ride5.getRideId());  // skip driverArrived
        } catch (InvalidRideStateException e) {
            System.out.println("❌ " + e.getMessage());
        }

        service.displayDriverStats();
    }
}
```

## Extension Q&A

**Q: How do you implement pool/shared rides?**
Add `RideType` enum (SOLO, POOL). For POOL rides, a `PoolRideManager` groups requests going in the same direction within a time window. Each `Ride` gets a `poolId` — multiple `Rider`s share one `Driver`. Fare split equally. The driver gets turn-by-turn instructions for multiple pickups.

**Q: How do you calculate surge pricing dynamically?**
`SurgeCalculator` monitors: active ride requests / available drivers ratio. If ratio > 2.0 → 1.3x surge. Ratio > 3.0 → 1.5x. Ratio > 5.0 → 2.0x (capped). Recalculate every 30 seconds per geohash zone (8km² cell). Inject the dynamic `SurgeFareStrategy` into booking service.

**Q: How would you match drivers in a real geo-distributed system?**
Replace the in-memory `drivers.values()` scan with a geospatial query. Redis: `GEORADIUS drivers:available {lng} {lat} 5 km ASC COUNT 10`. Each driver's location is updated via `GEOADD` on every GPS ping. This gives O(log N) nearest-driver query instead of O(N) full scan.
