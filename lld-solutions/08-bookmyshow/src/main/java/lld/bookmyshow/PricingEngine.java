package lld.bookmyshow;
import java.util.List;
import java.time.*;
import java.util.Map;

public class PricingEngine {
    private static final Map<SeatTier, Double> MULTIPLIER = Map.of(
        SeatTier.RECLINER, 2.5, SeatTier.PREMIUM, 1.8, SeatTier.EXECUTIVE, 1.3, SeatTier.NORMAL, 1.0);
    private static final double BASE = 150.0, PEAK = 1.2;

    public Money calculatePrice(Seat seat, LocalDateTime showTime) {
        double m    = MULTIPLIER.getOrDefault(seat.getTier(), 1.0);
        boolean pk  = isPeak(showTime);
        return Money.ofRupees(BASE * m * (pk ? PEAK : 1.0));
    }

    public Money calculateTotal(List<Seat> seats, LocalDateTime showTime) {
        return seats.stream().map(s -> calculatePrice(s, showTime)).reduce(new Money(0), Money::add);
    }

    private boolean isPeak(LocalDateTime dt) {
        DayOfWeek d = dt.getDayOfWeek(); int h = dt.getHour();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY || (h >= 18 && h <= 22);
    }
}
