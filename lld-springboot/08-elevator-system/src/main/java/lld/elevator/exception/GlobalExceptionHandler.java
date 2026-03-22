package lld.elevator.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ElevatorException.class)
    public ResponseEntity<Map<String,Object>> handle(ElevatorException ex, HttpServletRequest req) {
        log.warn("[ELEVATOR] {} | path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(Map.of("status",ex.getStatus().value(),"error",ex.getMessage(),"timestamp",Instant.now().toString()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();
        return ResponseEntity.badRequest().body(Map.of("status",400,"errors",errors));
    }
}
