package lld.bookmyshow.dto;
import lombok.Builder;
import lombok.Data;
import java.util.Map;
@Data @Builder
public class ShowAvailabilityResponse {
    private Long showId;
    private String movieName;
    private String showTime;
    private int totalSeats;
    private long availableSeats;
    private Map<String, Long>   availableByTier;
    private Map<String, String> priceByTier;
}
