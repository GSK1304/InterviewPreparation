package lld.cabooking;
import java.util.*;

public class NearestDriverStrategy implements DriverMatchStrategy {
    private static final double MAX_RADIUS_KM = 5.0;
    @Override
    public Optional<Driver> match(List<Driver> available, Location pickup, VehicleType type) {
        return available.stream()
            .filter(d -> d.getVehicle().type() == type)
            .filter(d -> d.distanceTo(pickup) <= MAX_RADIUS_KM)
            .min(Comparator.comparingDouble(d -> d.distanceTo(pickup)));
    }
}
