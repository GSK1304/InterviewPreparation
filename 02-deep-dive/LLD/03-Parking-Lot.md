# LLD — Parking Lot System (Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| Spot selection | **Strategy** pattern — swap algorithm without changing ParkingLot |
| Ticket generation | **Factory** method |
| Spot state | **Enum** (AVAILABLE / OCCUPIED / MAINTENANCE) |
| Value objects | **Records** (Vehicle, ParkingFee, ParkingTicket) |
| Thread safety | `synchronized` on spot assignment, `ConcurrentHashMap` for active tickets |
| Vehicle → Spot fit | Each VehicleType knows its minimum SpotSize (OCP) |

## Complete Solution

```java
package lld.parkinglot;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// ── Enums ────────────────────────────────────────────────────────────────────

enum SpotSize { MOTORCYCLE, COMPACT, LARGE }

enum SpotStatus { AVAILABLE, OCCUPIED, MAINTENANCE }

enum VehicleType {
    MOTORCYCLE(SpotSize.MOTORCYCLE, 10.0),
    CAR       (SpotSize.COMPACT,    20.0),
    TRUCK     (SpotSize.LARGE,      40.0);

    private final SpotSize minSpotSize;
    private final double   hourlyRate;   // INR per hour

    VehicleType(SpotSize minSpotSize, double hourlyRate) {
        this.minSpotSize = minSpotSize;
        this.hourlyRate  = hourlyRate;
    }

    public SpotSize getMinSpotSize() { return minSpotSize; }
    public double   getHourlyRate()  { return hourlyRate; }

    /** Returns true if this vehicle can park in a spot of the given size */
    public boolean fitsIn(SpotSize size) {
        return size.ordinal() >= minSpotSize.ordinal();
    }
}

// ── Value Objects (Records) ───────────────────────────────────────────────────

record Vehicle(String licensePlate, VehicleType type) {
    Vehicle {
        Objects.requireNonNull(licensePlate, "License plate required");
        Objects.requireNonNull(type,         "Vehicle type required");
        if (licensePlate.isBlank()) throw new IllegalArgumentException("License plate cannot be blank");
        if (!licensePlate.matches("[A-Z0-9-]+"))
            throw new IllegalArgumentException("License plate must be alphanumeric: " + licensePlate);
    }
}

record ParkingFee(double amount, Duration parkedFor) {
    ParkingFee {
        if (amount < 0)    throw new IllegalArgumentException("Fee cannot be negative");
        Objects.requireNonNull(parkedFor, "Duration required");
    }

    @Override public String toString() {
        long mins = parkedFor.toMinutes();
        return String.format("₹%.2f (parked %dh %dm)", amount, mins / 60, mins % 60);
    }
}

record ParkingTicket(
    String    ticketId,
    Vehicle   vehicle,
    String    spotId,
    int       floorNumber,
    Instant   entryTime
) {
    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    ParkingTicket {
        Objects.requireNonNull(ticketId,     "Ticket ID required");
        Objects.requireNonNull(vehicle,      "Vehicle required");
        Objects.requireNonNull(spotId,       "Spot ID required");
        Objects.requireNonNull(entryTime,    "Entry time required");
        if (floorNumber < 0) throw new IllegalArgumentException("Floor cannot be negative");
    }

    static ParkingTicket issue(Vehicle vehicle, ParkingSpot spot) {
        String id = "TKT-" + COUNTER.getAndIncrement();
        return new ParkingTicket(id, vehicle, spot.getId(), spot.getFloor(), Instant.now());
    }

    ParkingFee calculateFee() {
        Duration parkedFor = Duration.between(entryTime, Instant.now());
        // Minimum charge: 1 hour; then per-hour rate
        double hours = Math.max(1.0, Math.ceil(parkedFor.toMinutes() / 60.0));
        double amount = hours * vehicle.type().getHourlyRate();
        return new ParkingFee(amount, parkedFor);
    }
}

// ── Custom Exceptions ─────────────────────────────────────────────────────────

class ParkingException extends RuntimeException {
    ParkingException(String msg) { super(msg); }
}

class NoSpotAvailableException extends ParkingException {
    NoSpotAvailableException(VehicleType t) {
        super("No available spot for: " + t); }
}

class InvalidTicketException extends ParkingException {
    InvalidTicketException(String id) {
        super("Invalid or already used ticket: " + id); }
}

class VehicleAlreadyParkedException extends ParkingException {
    VehicleAlreadyParkedException(String plate) {
        super("Vehicle already parked: " + plate); }
}

// ── Spot Selection Strategy ───────────────────────────────────────────────────

@FunctionalInterface
interface SpotSelectionStrategy {
    Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType);
}

/** Selects the first available spot on the lowest floor (nearest to entry) */
class NearestFloorStrategy implements SpotSelectionStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType) {
        return floors.stream()
            .sorted(Comparator.comparingInt(ParkingFloor::getNumber))
            .flatMap(f -> f.availableSpotsFor(vehicleType).stream())
            .findFirst();
    }
}

/** Spreads vehicles evenly across floors to avoid single-floor congestion */
class LoadBalancedStrategy implements SpotSelectionStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType) {
        return floors.stream()
            .filter(f -> !f.availableSpotsFor(vehicleType).isEmpty())
            .max(Comparator.comparingLong(ParkingFloor::availableCount))
            .flatMap(f -> f.availableSpotsFor(vehicleType).stream().findFirst());
    }
}

// ── Parking Spot ──────────────────────────────────────────────────────────────

class ParkingSpot {
    private final String    id;
    private final int       floor;
    private final SpotSize  size;
    private SpotStatus      status;

    ParkingSpot(String id, int floor, SpotSize size) {
        this.id     = Objects.requireNonNull(id);
        this.floor  = floor;
        this.size   = Objects.requireNonNull(size);
        this.status = SpotStatus.AVAILABLE;
        if (floor < 0) throw new IllegalArgumentException("Floor cannot be negative");
    }

    /** Atomic: checks available + assigns in one synchronized block */
    synchronized boolean tryOccupy(VehicleType vehicleType) {
        if (status != SpotStatus.AVAILABLE) return false;
        if (!vehicleType.fitsIn(size))      return false;
        status = SpotStatus.OCCUPIED;
        return true;
    }

    synchronized void release() {
        if (status != SpotStatus.OCCUPIED)
            throw new ParkingException("Spot " + id + " is not occupied");
        status = SpotStatus.AVAILABLE;
    }

    synchronized void setMaintenance(boolean maintenance) {
        if (maintenance && status == SpotStatus.OCCUPIED)
            throw new ParkingException("Cannot mark occupied spot for maintenance: " + id);
        status = maintenance ? SpotStatus.MAINTENANCE : SpotStatus.AVAILABLE;
    }

    public String    getId()         { return id; }
    public int       getFloor()      { return floor; }
    public SpotSize  getSize()       { return size; }
    public SpotStatus getStatus()    { return status; }
    public boolean   isAvailable()   { return status == SpotStatus.AVAILABLE; }

    @Override public String toString() {
        return String.format("[%s F%d %s %s]", id, floor, size, status);
    }
}

// ── Parking Floor ─────────────────────────────────────────────────────────────

class ParkingFloor {
    private final int              number;
    private final List<ParkingSpot> spots;

    ParkingFloor(int number, List<ParkingSpot> spots) {
        if (number < 0) throw new IllegalArgumentException("Floor number cannot be negative");
        if (spots == null || spots.isEmpty())
            throw new IllegalArgumentException("Floor must have at least one spot");
        this.number = number;
        this.spots  = Collections.unmodifiableList(new ArrayList<>(spots));
    }

    public int getNumber() { return number; }

    public List<ParkingSpot> availableSpotsFor(VehicleType type) {
        return spots.stream()
            .filter(s -> s.isAvailable() && type.fitsIn(s.getSize()))
            .collect(Collectors.toList());
    }

    public long availableCount() {
        return spots.stream().filter(ParkingSpot::isAvailable).count();
    }

    public Map<SpotSize, Long> availabilityBreakdown() {
        return spots.stream()
            .filter(ParkingSpot::isAvailable)
            .collect(Collectors.groupingBy(ParkingSpot::getSize, Collectors.counting()));
    }

    public Optional<ParkingSpot> findById(String spotId) {
        return spots.stream().filter(s -> s.getId().equals(spotId)).findFirst();
    }

    @Override public String toString() {
        Map<SpotSize, Long> avail = availabilityBreakdown();
        return String.format("Floor %d | MC:%-3d CAR:%-3d TRUCK:%-3d | Total available: %d",
            number,
            avail.getOrDefault(SpotSize.MOTORCYCLE, 0L),
            avail.getOrDefault(SpotSize.COMPACT,    0L),
            avail.getOrDefault(SpotSize.LARGE,      0L),
            availableCount());
    }
}

// ── Parking Lot ───────────────────────────────────────────────────────────────

class ParkingLot {
    private final String               name;
    private final List<ParkingFloor>   floors;
    private final SpotSelectionStrategy strategy;
    private final Map<String, ParkingTicket> activeTickets  = new ConcurrentHashMap<>();
    private final Map<String, String>        parkedVehicles = new ConcurrentHashMap<>();
    // licensePlate → ticketId

    private ParkingLot(Builder builder) {
        this.name     = builder.name;
        this.floors   = Collections.unmodifiableList(builder.floors);
        this.strategy = builder.strategy;
    }

    // ── Core Operations ───────────────────────────────────────────────────────

    public ParkingTicket park(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle required");

        // Prevent duplicate parking
        if (parkedVehicles.containsKey(vehicle.licensePlate()))
            throw new VehicleAlreadyParkedException(vehicle.licensePlate());

        // Find a spot using the configured strategy
        ParkingSpot spot = strategy.select(floors, vehicle.type())
            .filter(s -> s.tryOccupy(vehicle.type()))  // atomic claim
            .orElseThrow(() -> new NoSpotAvailableException(vehicle.type()));

        ParkingTicket ticket = ParkingTicket.issue(vehicle, spot);
        activeTickets.put(ticket.ticketId(), ticket);
        parkedVehicles.put(vehicle.licensePlate(), ticket.ticketId());

        System.out.printf("✅ Parked: %s | Spot: %s (F%d) | Ticket: %s%n",
            vehicle.licensePlate(), spot.getId(), spot.getFloor(), ticket.ticketId());
        return ticket;
    }

    public ParkingFee unpark(String ticketId) {
        Objects.requireNonNull(ticketId, "Ticket ID required");

        ParkingTicket ticket = activeTickets.remove(ticketId);
        if (ticket == null) throw new InvalidTicketException(ticketId);

        ParkingSpot spot = findSpotById(ticket.spotId(), ticket.floorNumber())
            .orElseThrow(() -> new ParkingException("Spot not found: " + ticket.spotId()));

        spot.release();
        parkedVehicles.remove(ticket.vehicle().licensePlate());

        ParkingFee fee = ticket.calculateFee();
        System.out.printf("💰 Unparked: %s | Spot: %s | Fee: %s%n",
            ticket.vehicle().licensePlate(), ticket.spotId(), fee);
        return fee;
    }

    public void displayAvailability() {
        System.out.println("\n═══ " + name + " — Availability ═══");
        floors.forEach(f -> System.out.println("  " + f));
        long total = floors.stream().mapToLong(ParkingFloor::availableCount).sum();
        System.out.println("  Total available: " + total);
    }

    public boolean isSpotAvailable(VehicleType type) {
        return floors.stream().anyMatch(f -> !f.availableSpotsFor(type).isEmpty());
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    public void markMaintenance(String spotId, int floor, boolean maintenance) {
        findSpotById(spotId, floor)
            .orElseThrow(() -> new ParkingException("Spot not found: " + spotId))
            .setMaintenance(maintenance);
        System.out.printf("🔧 Spot %s on F%d marked %s%n",
            spotId, floor, maintenance ? "MAINTENANCE" : "AVAILABLE");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<ParkingSpot> findSpotById(String spotId, int floorNumber) {
        return floors.stream()
            .filter(f -> f.getNumber() == floorNumber)
            .findFirst()
            .flatMap(f -> f.findById(spotId));
    }

    public int getActiveTicketCount() { return activeTickets.size(); }

    // ── Builder ───────────────────────────────────────────────────────────────

    static final class Builder {
        private final String             name;
        private final List<ParkingFloor> floors   = new ArrayList<>();
        private SpotSelectionStrategy    strategy = new NearestFloorStrategy();

        Builder(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Parking lot name required");
            this.name = name;
        }

        Builder addFloor(ParkingFloor floor) {
            Objects.requireNonNull(floor, "Floor required");
            floors.add(floor);
            return this;
        }

        Builder strategy(SpotSelectionStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        ParkingLot build() {
            if (floors.isEmpty()) throw new IllegalStateException("Parking lot must have at least one floor");
            return new ParkingLot(this);
        }
    }
}

// ── Floor Builder Helper ──────────────────────────────────────────────────────

class FloorBuilder {
    private final int         floorNumber;
    private final List<ParkingSpot> spots = new ArrayList<>();
    private int spotCounter = 1;

    FloorBuilder(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    FloorBuilder addSpots(int count, SpotSize size) {
        if (count <= 0) throw new IllegalArgumentException("Count must be positive");
        for (int i = 0; i < count; i++) {
            String id = String.format("F%d-%s-%02d", floorNumber, size.name().charAt(0), spotCounter++);
            spots.add(new ParkingSpot(id, floorNumber, size));
        }
        return this;
    }

    ParkingFloor build() {
        return new ParkingFloor(floorNumber, spots);
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class ParkingLotDemo {
    public static void main(String[] args) {
        // Build a 3-floor parking lot
        ParkingLot lot = new ParkingLot.Builder("Phoenix Mall Parking")
            .addFloor(new FloorBuilder(0)
                .addSpots(5, SpotSize.MOTORCYCLE)
                .addSpots(10, SpotSize.COMPACT)
                .addSpots(3, SpotSize.LARGE)
                .build())
            .addFloor(new FloorBuilder(1)
                .addSpots(5, SpotSize.MOTORCYCLE)
                .addSpots(10, SpotSize.COMPACT)
                .addSpots(3, SpotSize.LARGE)
                .build())
            .addFloor(new FloorBuilder(2)
                .addSpots(2, SpotSize.MOTORCYCLE)
                .addSpots(8, SpotSize.COMPACT)
                .addSpots(5, SpotSize.LARGE)
                .build())
            .strategy(new NearestFloorStrategy())
            .build();

        lot.displayAvailability();

        // Park vehicles
        Vehicle bike  = new Vehicle("KA01HB1234", VehicleType.MOTORCYCLE);
        Vehicle car1  = new Vehicle("MH02CB5678", VehicleType.CAR);
        Vehicle car2  = new Vehicle("TN03DE9012", VehicleType.CAR);
        Vehicle truck = new Vehicle("DL04EF3456", VehicleType.TRUCK);

        ParkingTicket bikeTicket  = lot.park(bike);
        ParkingTicket carTicket1  = lot.park(car1);
        ParkingTicket carTicket2  = lot.park(car2);
        ParkingTicket truckTicket = lot.park(truck);

        lot.displayAvailability();

        // Unpark a car
        lot.unpark(carTicket1.ticketId());

        // Test: invalid ticket
        try {
            lot.unpark("TKT-INVALID");
        } catch (InvalidTicketException e) {
            System.out.println("❌ " + e.getMessage());
        }

        // Test: duplicate parking
        try {
            lot.park(car1);  // car1 is unparked so this should succeed now
        } catch (VehicleAlreadyParkedException e) {
            System.out.println("❌ " + e.getMessage());
        }

        // Maintenance
        lot.markMaintenance("F0-M-01", 0, true);

        lot.displayAvailability();

        // Unpark remaining
        lot.unpark(bikeTicket.ticketId());
        lot.unpark(carTicket2.ticketId());
        lot.unpark(truckTicket.ticketId());

        System.out.println("\nActive tickets remaining: " + lot.getActiveTicketCount());
    }
}
```

## Extension Q&A

**Q: How do you add a new vehicle type (e.g., BUS)?**
Add `BUS(SpotSize.LARGE, 60.0)` to `VehicleType` enum. The `fitsIn()` logic and fee calculation work automatically — zero changes to `ParkingLot` or `ParkingSpot` (OCP respected).

**Q: How do you add a monthly pass / pre-booked spot?**
Add `RESERVED` to `SpotStatus`. Add a `ReservationService` that pre-assigns spots and creates `Reservation` objects. The spot's `tryOccupy()` would also accept a valid `ReservationId`. Fee calculation branches on ticket type.

**Q: How would you support multiple entry gates routing to nearest spot?**
Add `Gate` entities with a floor preference. Update `SpotSelectionStrategy` to accept a `Gate` parameter and weight spots closer to that gate's floor. `NearestGateStrategy` replaces `NearestFloorStrategy`.

**Q: Where is thread safety applied and why?**
`tryOccupy()` and `release()` on `ParkingSpot` are `synchronized` — two threads assigning the same spot simultaneously is the classic race condition. `ConcurrentHashMap` for `activeTickets` and `parkedVehicles` handles concurrent park/unpark requests safely.
