package lld.parking.exception;

import lld.parking.enums.VehicleType;
import org.springframework.http.HttpStatus;

public class NoSpotAvailableException extends ParkingException {
    public NoSpotAvailableException(VehicleType type) {
        super("No available spot for vehicle type: " + type, HttpStatus.CONFLICT);
    }
}
