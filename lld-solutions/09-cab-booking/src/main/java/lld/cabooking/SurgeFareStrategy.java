package lld.cabooking;
import java.time.Duration;
import java.util.Objects;

public class SurgeFareStrategy implements FareStrategy {
    private final FareStrategy base;
    private final double       multiplier;
    public SurgeFareStrategy(FareStrategy base, double multiplier) {
        this.base = Objects.requireNonNull(base);
        this.multiplier = multiplier;
        if (multiplier < 1.0) throw new IllegalArgumentException("Surge must be >= 1.0");
    }
    @Override
    public Money calculate(double km, Duration dur, VehicleType type) {
        return base.calculate(km, dur, type).multiply(multiplier);
    }
}
