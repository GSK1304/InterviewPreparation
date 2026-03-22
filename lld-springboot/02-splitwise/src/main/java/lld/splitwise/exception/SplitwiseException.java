package lld.splitwise.exception;
import org.springframework.http.HttpStatus;
public class SplitwiseException extends RuntimeException {
    private final HttpStatus status;
    public SplitwiseException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
