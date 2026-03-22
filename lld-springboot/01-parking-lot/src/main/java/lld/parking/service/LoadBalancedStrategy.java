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

@Component("LOAD_BALANCED")
@RequiredArgsConstructor
public class LoadBalancedStrategy implements SpotSelectionStrategy {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancedStrategy.class);
    private final ParkingSpotRepository spotRepository;

    @Override
    public Optional<ParkingSpot> select(VehicleType vehicleType) {
        List<SpotSize> eligibleSizes = List.of(SpotSize.values()).stream()
            .filter(size -> vehicleType.fitsIn(size)).toList();
        log.debug("[LoadBalanced] Selecting most-available floor for {}", vehicleType);
        return spotRepository.findAvailableSpotsLoadBalanced(eligibleSizes).stream().findFirst();
    }
}
