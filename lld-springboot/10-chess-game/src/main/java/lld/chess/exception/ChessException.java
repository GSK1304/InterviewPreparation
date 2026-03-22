package lld.chess.exception;
import org.springframework.http.HttpStatus;
public class ChessException extends RuntimeException {
    private final HttpStatus status;
    public ChessException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
