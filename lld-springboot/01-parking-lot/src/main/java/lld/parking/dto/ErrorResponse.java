package lld.parking.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data @Builder
public class ErrorResponse {
    private int         status;
    private String      error;
    private String      message;
    private String      path;
    private Instant     timestamp;
    private List<String> validationErrors;
}
