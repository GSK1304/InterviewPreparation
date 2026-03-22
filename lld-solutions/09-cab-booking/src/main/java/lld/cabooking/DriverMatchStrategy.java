package lld.cabooking;
import java.util.List;
import java.util.Optional;
@FunctionalInterface
public interface DriverMatchStrategy {
    Optional<Driver> match(List<Driver> available, Location pickup, VehicleType vehicleType);
}
