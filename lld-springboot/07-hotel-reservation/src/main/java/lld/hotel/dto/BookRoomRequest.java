package lld.hotel.dto;
import jakarta.validation.constraints.*;
import lld.hotel.enums.AmenityType;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;
@Data
public class BookRoomRequest {
    @NotBlank private String guestName;
    @NotBlank @Email private String guestEmail;
    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$") private String guestPhone;
    @NotBlank private String idProof;
    @NotBlank private String roomNumber;
    @NotNull @FutureOrPresent(message = "Check-in must be today or future") private LocalDate checkIn;
    @NotNull @Future(message = "Check-out must be in future") private LocalDate checkOut;
    @Min(1) @Max(10) private int guestCount = 1;
    private Set<AmenityType> amenities;
}
