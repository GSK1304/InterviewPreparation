package lld.elevator.exception;
import org.springframework.http.HttpStatus;
public class ElevatorException extends RuntimeException {
    private final HttpStatus status;
    public ElevatorException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
