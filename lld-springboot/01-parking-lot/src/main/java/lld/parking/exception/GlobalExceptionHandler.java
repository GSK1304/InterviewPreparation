package lld.parking.exception;

import jakarta.servlet.http.HttpServletRequest;
import lld.parking.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ParkingException.class)
    public ResponseEntity<ErrorResponse> handleParkingException(
            ParkingException ex, HttpServletRequest req) {
        log.warn("[PARKING-SERVICE] Business error: {} | path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.builder()
            .status(ex.getStatus().value())
            .error(ex.getStatus().getReasonPhrase())
            .message(ex.getMessage())
            .path(req.getRequestURI())
            .timestamp(Instant.now())
            .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        log.warn("[PARKING-SERVICE] Validation failed: {} | path={}", errors, req.getRequestURI());
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
            .status(400)
            .error("Validation Failed")
            .message("Request validation failed")
            .path(req.getRequestURI())
            .timestamp(Instant.now())
            .validationErrors(errors)
            .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest req) {
        log.error("[PARKING-SERVICE] Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.builder()
            .status(500)
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(req.getRequestURI())
            .timestamp(Instant.now())
            .build());
    }
}
