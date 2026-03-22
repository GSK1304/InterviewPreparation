package lld.parkinglot;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface SpotSelectionStrategy {
    Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType);
}
