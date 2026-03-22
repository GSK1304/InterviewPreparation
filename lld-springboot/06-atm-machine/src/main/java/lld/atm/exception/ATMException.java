package lld.atm.exception;
import org.springframework.http.HttpStatus;
public class ATMException extends RuntimeException {
    private final HttpStatus status;
    public ATMException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }
}
