package lld.hotel.exception;
import org.springframework.http.HttpStatus;
public class HotelException extends RuntimeException {
    private final HttpStatus status;
    public HotelException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
