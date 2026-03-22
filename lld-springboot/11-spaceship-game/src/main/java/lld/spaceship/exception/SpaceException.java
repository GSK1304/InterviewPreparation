package lld.spaceship.exception;
import org.springframework.http.HttpStatus;
public class SpaceException extends RuntimeException {
    private final HttpStatus status;
    public SpaceException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
