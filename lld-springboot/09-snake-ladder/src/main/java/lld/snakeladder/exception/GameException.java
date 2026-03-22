package lld.snakeladder.exception;
import org.springframework.http.HttpStatus;
public class GameException extends RuntimeException {
    private final HttpStatus status;
    public GameException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
