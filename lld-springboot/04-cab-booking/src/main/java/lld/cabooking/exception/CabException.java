package lld.cabooking.exception;
import org.springframework.http.HttpStatus;
public class CabException extends RuntimeException {
    private final HttpStatus status;
    public CabException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
