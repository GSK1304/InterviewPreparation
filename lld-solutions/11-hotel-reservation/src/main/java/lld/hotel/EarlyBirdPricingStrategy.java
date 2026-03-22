package lld.hotel;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class EarlyBirdPricingStrategy implements PricingStrategy {
    private final PricingStrategy base;
    private final int             daysInAdvance;
    private final double          discount;

    public EarlyBirdPricingStrategy(PricingStrategy base, int daysInAdvance, double discount) {
        this.base = Objects.requireNonNull(base); this.daysInAdvance = daysInAdvance; this.discount = discount;
        if (discount <= 0 || discount >= 1) throw new IllegalArgumentException("Discount must be between 0 and 1");
    }

    @Override
    public Money calculate(Money rate, LocalDate checkIn, LocalDate checkOut) {
        Money standard = base.calculate(rate, checkIn, checkOut);
        return ChronoUnit.DAYS.between(LocalDate.now(), checkIn) >= daysInAdvance
            ? standard.multiply(1.0 - discount) : standard;
    }
}
