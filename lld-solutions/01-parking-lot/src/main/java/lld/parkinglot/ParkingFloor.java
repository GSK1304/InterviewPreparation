package lld.parkinglot;
import java.util.*;
import java.util.stream.Collectors;

public class ParkingFloor {
    private final int               number;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int number, List<ParkingSpot> spots) {
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

    public Optional<ParkingSpot> findById(String spotId) {
        return spots.stream().filter(s -> s.getId().equals(spotId)).findFirst();
    }

    @Override public String toString() {
        Map<SpotSize, Long> avail = spots.stream()
            .filter(ParkingSpot::isAvailable)
            .collect(Collectors.groupingBy(ParkingSpot::getSize, Collectors.counting()));
        return String.format("Floor %d | MC:%-3d CAR:%-3d TRUCK:%-3d | Available: %d",
            number,
            avail.getOrDefault(SpotSize.MOTORCYCLE, 0L),
            avail.getOrDefault(SpotSize.COMPACT,    0L),
            avail.getOrDefault(SpotSize.LARGE,      0L),
            availableCount());
    }
}
