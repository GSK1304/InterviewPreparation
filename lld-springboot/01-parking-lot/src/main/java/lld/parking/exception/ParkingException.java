package lld.parking.exception;

import org.springframework.http.HttpStatus;

public class ParkingException extends RuntimeException {
    private final HttpStatus status;

    public ParkingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
