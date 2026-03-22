package lld.parking.service;

import lld.parking.entity.ParkingSpot;
import lld.parking.enums.VehicleType;
import java.util.Optional;

public interface SpotSelectionStrategy {
    Optional<ParkingSpot> select(VehicleType vehicleType);
}
