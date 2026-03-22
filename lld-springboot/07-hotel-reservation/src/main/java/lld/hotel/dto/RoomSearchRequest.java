package lld.hotel.dto;
import jakarta.validation.constraints.*;
import lld.hotel.enums.BedType;
import lld.hotel.enums.RoomType;
import lombok.Data;
import java.time.LocalDate;
@Data
public class RoomSearchRequest {
    @NotNull @FutureOrPresent private LocalDate checkIn;
    @NotNull @Future private LocalDate checkOut;
    @Min(1) private int guestCount = 1;
    private RoomType roomType;
    private BedType  bedType;
    private Double   maxPricePerNight;
}
