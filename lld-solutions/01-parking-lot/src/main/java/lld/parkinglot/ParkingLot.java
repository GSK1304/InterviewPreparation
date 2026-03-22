package lld.parkinglot;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingLot {
    private final String                     name;
    private final List<ParkingFloor>         floors;
    private final SpotSelectionStrategy      strategy;
    private final Map<String, ParkingTicket> activeTickets  = new ConcurrentHashMap<>();
    private final Map<String, String>        parkedVehicles = new ConcurrentHashMap<>();

    private ParkingLot(Builder builder) {
        this.name     = builder.name;
        this.floors   = Collections.unmodifiableList(builder.floors);
        this.strategy = builder.strategy;
    }

    public ParkingTicket park(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle required");
        if (parkedVehicles.containsKey(vehicle.licensePlate()))
            throw new VehicleAlreadyParkedException(vehicle.licensePlate());

        ParkingSpot spot = strategy.select(floors, vehicle.type())
            .filter(s -> s.tryOccupy(vehicle.type()))
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

    public void markMaintenance(String spotId, int floor, boolean maintenance) {
        findSpotById(spotId, floor)
            .orElseThrow(() -> new ParkingException("Spot not found: " + spotId))
            .setMaintenance(maintenance);
        System.out.printf("🔧 Spot %s on F%d marked %s%n",
            spotId, floor, maintenance ? "MAINTENANCE" : "AVAILABLE");
    }

    public void displayAvailability() {
        System.out.println("\n═══ " + name + " — Availability ═══");
        floors.forEach(f -> System.out.println("  " + f));
        long total = floors.stream().mapToLong(ParkingFloor::availableCount).sum();
        System.out.println("  Total available: " + total);
    }

    private Optional<ParkingSpot> findSpotById(String spotId, int floorNumber) {
        return floors.stream()
            .filter(f -> f.getNumber() == floorNumber)
            .findFirst()
            .flatMap(f -> f.findById(spotId));
    }

    public int getActiveTicketCount() { return activeTickets.size(); }

    public static final class Builder {
        private final String             name;
        private final List<ParkingFloor> floors   = new ArrayList<>();
        private SpotSelectionStrategy    strategy = new NearestFloorStrategy();

        public Builder(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Parking lot name required");
            this.name = name;
        }

        public Builder addFloor(ParkingFloor floor) {
            Objects.requireNonNull(floor, "Floor required");
            floors.add(floor);
            return this;
        }

        public Builder strategy(SpotSelectionStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        public ParkingLot build() {
            if (floors.isEmpty()) throw new IllegalStateException("Parking lot must have at least one floor");
            return new ParkingLot(this);
        }
    }
}
