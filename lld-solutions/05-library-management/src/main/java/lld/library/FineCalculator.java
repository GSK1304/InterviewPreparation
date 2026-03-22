package lld.library;
import java.time.LocalDate;
@FunctionalInterface
public interface FineCalculator {
    Fine calculate(LocalDate dueDate, LocalDate returnDate);
}
