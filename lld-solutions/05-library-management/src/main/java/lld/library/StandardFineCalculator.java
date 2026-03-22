package lld.library;
import java.time.*;
import java.time.temporal.ChronoUnit;
public class StandardFineCalculator implements FineCalculator {
    private static final double FINE_PER_DAY = 2.0;
    @Override
    public Fine calculate(LocalDate dueDate, LocalDate returnDate) {
        if (!returnDate.isAfter(dueDate)) return Fine.NONE;
        int days = (int) ChronoUnit.DAYS.between(dueDate, returnDate);
        return new Fine(days * FINE_PER_DAY, days);
    }
}
