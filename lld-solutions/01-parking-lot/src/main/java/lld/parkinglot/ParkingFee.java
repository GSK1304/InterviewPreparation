package lld.parkinglot;
import java.time.Duration;
import java.util.Objects;

public record ParkingFee(Money amount, Duration parkedFor) {
    public ParkingFee { Objects.requireNonNull(amount); Objects.requireNonNull(parkedFor); }
    @Override public String toString() {
        long mins = parkedFor.toMinutes();
        return String.format("%s (parked %dh %dm)", amount, mins / 60, mins % 60);
    }
}
