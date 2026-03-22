package lld.parking.dto;

import lld.parking.enums.SpotSize;
import lld.parking.enums.VehicleType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class ParkResponse {
    private String    ticketId;
    private String    licensePlate;
    private VehicleType vehicleType;
    private String    spotCode;
    private SpotSize  spotSize;
    private Integer   floorNumber;
    private Instant   entryTime;
    private String    hourlyRate;
    private String    message;
}
