package lld.hotel;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record DateRange(LocalDate checkIn, LocalDate checkOut) {
    public DateRange {
        Objects.requireNonNull(checkIn); Objects.requireNonNull(checkOut);
        if (!checkOut.isAfter(checkIn)) throw new IllegalArgumentException("Check-out must be after check-in");
    }
    public boolean overlaps(DateRange o) { return checkIn.isBefore(o.checkOut()) && checkOut.isAfter(o.checkIn()); }
    public long    nights()              { return ChronoUnit.DAYS.between(checkIn, checkOut); }
}
