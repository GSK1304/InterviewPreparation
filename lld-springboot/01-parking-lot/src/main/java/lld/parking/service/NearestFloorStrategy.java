package lld.parking.service;

import lld.parking.entity.ParkingSpot;
import lld.parking.enums.SpotSize;
import lld.parking.enums.VehicleType;
import lld.parking.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component("NEAREST_FLOOR")
@RequiredArgsConstructor
public class NearestFloorStrategy implements SpotSelectionStrategy {

    private static final Logger log = LoggerFactory.getLogger(NearestFloorStrategy.class);
    private final ParkingSpotRepository spotRepository;

    @Override
    public Optional<ParkingSpot> select(VehicleType vehicleType) {
        List<SpotSize> eligibleSizes = eligibleSizes(vehicleType);
        log.debug("[NearestFloor] Looking for {} spot among sizes: {}", vehicleType, eligibleSizes);
        Optional<ParkingSpot> spot = spotRepository.findAvailableSpotsBySize(eligibleSizes)
            .stream().findFirst();
        spot.ifPresentOrElse(
            s -> log.debug("[NearestFloor] Found spot {} on floor {}", s.getSpotCode(), s.getFloor().getFloorNumber()),
            ()  -> log.warn("[NearestFloor] No available spot for {}", vehicleType)
        );
        return spot;
    }

    private List<SpotSize> eligibleSizes(VehicleType type) {
        return List.of(SpotSize.values()).stream()
            .filter(size -> type.fitsIn(size))
            .toList();
    }
}
