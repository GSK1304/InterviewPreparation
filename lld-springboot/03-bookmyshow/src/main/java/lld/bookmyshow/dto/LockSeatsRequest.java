package lld.bookmyshow.dto;
import jakarta.validation.constraints.*;
import lld.bookmyshow.enums.SeatStrategy;
import lld.bookmyshow.enums.SeatTier;
import lombok.Data;
@Data
public class LockSeatsRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
    @NotNull(message = "Show ID is required")
    private Long showId;
    @Min(value = 1, message = "Minimum 1 seat") @Max(value = 10, message = "Maximum 10 seats")
    private int seatCount;
    private SeatStrategy strategy = SeatStrategy.BEST_AVAILABLE;
    private SeatTier preferredTier;
}
