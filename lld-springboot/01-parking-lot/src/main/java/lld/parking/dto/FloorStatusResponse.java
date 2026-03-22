package lld.parking.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data @Builder
public class FloorStatusResponse {
    private Integer floorNumber;
    private String  name;
    private long    totalSpots;
    private long    availableSpots;
    private long    occupiedSpots;
    private Map<String, Long> availableBySize;
}
