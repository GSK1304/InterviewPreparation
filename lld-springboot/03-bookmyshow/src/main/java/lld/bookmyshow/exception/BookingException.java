package lld.bookmyshow.exception;
import org.springframework.http.HttpStatus;
public class BookingException extends RuntimeException {
    private final HttpStatus status;
    public BookingException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
