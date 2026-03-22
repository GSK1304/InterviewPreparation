package lld.parkinglot;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public record ParkingTicket(String ticketId, Vehicle vehicle, String spotId,
                             int floorNumber, Instant entryTime) {
    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    public ParkingTicket {
        Objects.requireNonNull(ticketId);  Objects.requireNonNull(vehicle);
        Objects.requireNonNull(spotId);    Objects.requireNonNull(entryTime);
        if (floorNumber < 0) throw new IllegalArgumentException("Floor cannot be negative");
    }

    public static ParkingTicket issue(Vehicle vehicle, ParkingSpot spot) {
        return new ParkingTicket("TKT-" + COUNTER.getAndIncrement(),
                vehicle, spot.getId(), spot.getFloor(), Instant.now());
    }

    public ParkingFee calculateFee() {
        Duration parkedFor = Duration.between(entryTime, Instant.now());
        double hours = Math.max(1.0, Math.ceil(parkedFor.toMinutes() / 60.0));
        return new ParkingFee(Money.ofRupees(hours * vehicle.type().getHourlyRate()), parkedFor);
    }
}
