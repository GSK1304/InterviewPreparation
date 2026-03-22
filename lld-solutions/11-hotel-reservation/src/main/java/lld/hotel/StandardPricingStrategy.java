package lld.hotel;
import java.time.*;

public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public Money calculate(Money baseRate, LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) throw new IllegalArgumentException("Check-out must be after check-in");
        double total = 0;
        LocalDate d  = checkIn;
        while (!d.equals(checkOut)) {
            boolean weekend = (d.getDayOfWeek() == DayOfWeek.FRIDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY);
            total += baseRate.toRupees() * (weekend ? 1.3 : 1.0);
            d = d.plusDays(1);
        }
        return Money.ofRupees(total);
    }
}
