package lld.hotel;
import java.time.LocalDate;
import java.util.Objects;

public record RoomSearchCriteria(LocalDate checkIn, LocalDate checkOut, int guestCount,
                                  RoomType roomType, BedType bedType, Money maxPricePerNight) {
    public RoomSearchCriteria {
        Objects.requireNonNull(checkIn); Objects.requireNonNull(checkOut);
        if (!checkOut.isAfter(checkIn)) throw new IllegalArgumentException("Check-out must be after check-in");
        if (guestCount < 1)             throw new IllegalArgumentException("At least 1 guest");
    }
}
