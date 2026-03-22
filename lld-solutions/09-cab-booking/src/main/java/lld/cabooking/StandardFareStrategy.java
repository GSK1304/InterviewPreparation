package lld.cabooking;
import java.time.Duration;
import java.util.Map;

public class StandardFareStrategy implements FareStrategy {
    private static final Map<VehicleType, Double> BASE = Map.of(
        VehicleType.AUTO,30.0, VehicleType.MINI,40.0, VehicleType.SEDAN,60.0, VehicleType.SUV,80.0, VehicleType.PREMIUM,120.0);
    private static final Map<VehicleType, Double> PER_KM = Map.of(
        VehicleType.AUTO,8.0,  VehicleType.MINI,10.0, VehicleType.SEDAN,14.0, VehicleType.SUV,18.0, VehicleType.PREMIUM,25.0);

    @Override
    public Money calculate(double km, Duration duration, VehicleType type) {
        return Money.ofRupees(BASE.getOrDefault(type,40.0) + PER_KM.getOrDefault(type,10.0)*km + 1.5*duration.toMinutes());
    }
}
