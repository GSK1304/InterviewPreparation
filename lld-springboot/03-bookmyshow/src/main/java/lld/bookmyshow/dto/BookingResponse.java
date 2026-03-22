package lld.bookmyshow.dto;
import lld.bookmyshow.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
@Data @Builder
public class BookingResponse {
    private String bookingId;
    private String userId;
    private String movieName;
    private String showTime;
    private List<String> seatCodes;
    private String totalAmount;
    private BookingStatus status;
    private Instant lockExpiry;
    private String message;
}
