package lld.library.exception;
import org.springframework.http.HttpStatus;
public class LibraryException extends RuntimeException {
    private final HttpStatus status;
    public LibraryException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
