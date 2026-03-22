package lld.parking.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class UnparkResponse {
    private String  ticketId;
    private String  licensePlate;
    private String  spotCode;
    private Instant entryTime;
    private Instant exitTime;
    private long    durationMinutes;
    private String  fee;
    private String  message;
}
