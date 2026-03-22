package lld.parkinglot;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LoadBalancedStrategy implements SpotSelectionStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType) {
        return floors.stream()
            .filter(f -> !f.availableSpotsFor(vehicleType).isEmpty())
            .max(Comparator.comparingLong(ParkingFloor::availableCount))
            .flatMap(f -> f.availableSpotsFor(vehicleType).stream().findFirst());
    }
}
