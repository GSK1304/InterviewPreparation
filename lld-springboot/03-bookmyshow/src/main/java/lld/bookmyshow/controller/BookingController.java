package lld.bookmyshow.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.bookmyshow.dto.*;
import lld.bookmyshow.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/bookings") @RequiredArgsConstructor
@Tag(name = "BookMyShow", description = "Movie ticket booking with seat locking (5-min TTL)")
public class BookingController {
    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    private final BookingService service;

    @PostMapping("/lock")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lock seats", description = "Locks seats for 5 minutes. Must confirm within TTL.")
    public BookingResponse lockSeats(@Valid @RequestBody LockSeatsRequest req) {
        log.info("[BookingController] POST /lock | user={} show={} count={}", req.getUserId(), req.getShowId(), req.getSeatCount());
        return service.lockSeats(req);
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm booking", description = "Confirms booking after payment. Fails if TTL expired.")
    public BookingResponse confirm(@PathVariable String bookingId) {
        log.info("[BookingController] POST /{}/confirm", bookingId);
        return service.confirmBooking(bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking")
    public BookingResponse cancel(@PathVariable String bookingId) {
        log.info("[BookingController] POST /{}/cancel", bookingId);
        return service.cancelBooking(bookingId);
    }

    @GetMapping("/shows/{showId}/availability")
    @Operation(summary = "Get seat availability for a show")
    public ShowAvailabilityResponse getAvailability(@PathVariable Long showId) {
        log.debug("[BookingController] GET /shows/{}/availability", showId);
        return service.getAvailability(showId);
    }
}
