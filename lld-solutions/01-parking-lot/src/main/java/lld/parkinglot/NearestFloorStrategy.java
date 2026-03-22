package lld.parkinglot;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestFloorStrategy implements SpotSelectionStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, VehicleType vehicleType) {
        return floors.stream()
            .sorted(Comparator.comparingInt(ParkingFloor::getNumber))
            .flatMap(f -> f.availableSpotsFor(vehicleType).stream())
            .findFirst();
    }
}
