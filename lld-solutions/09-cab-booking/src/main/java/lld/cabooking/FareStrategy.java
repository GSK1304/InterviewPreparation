package lld.cabooking;
import java.time.Duration;
@FunctionalInterface
public interface FareStrategy {
    Money calculate(double distanceKm, Duration duration, VehicleType vehicleType);
}
