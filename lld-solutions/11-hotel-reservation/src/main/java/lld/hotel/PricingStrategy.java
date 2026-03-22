package lld.hotel;
import java.time.LocalDate;
public interface PricingStrategy {
    Money calculate(Money dailyRate, LocalDate checkIn, LocalDate checkOut);
}
