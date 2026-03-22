package lld.parking.exception;

import org.springframework.http.HttpStatus;

public class VehicleAlreadyParkedException extends ParkingException {
    public VehicleAlreadyParkedException(String licensePlate) {
        super("Vehicle already parked: " + licensePlate, HttpStatus.CONFLICT);
    }
}
