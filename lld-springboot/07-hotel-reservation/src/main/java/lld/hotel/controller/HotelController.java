package lld.hotel.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.hotel.dto.*;
import lld.hotel.entity.Room;
import lld.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/v1/hotel") @RequiredArgsConstructor
@Tag(name = "Hotel Reservation", description = "Room booking with Decorator-based amenities and weekend pricing")
public class HotelController {
    private static final Logger log = LoggerFactory.getLogger(HotelController.class);
    private final HotelService service;

    @PostMapping("/rooms/search")
    @Operation(summary = "Search available rooms")
    public List<Room> searchRooms(@Valid @RequestBody RoomSearchRequest req) {
        log.info("[HotelController] POST /rooms/search | checkIn={} checkOut={}", req.getCheckIn(), req.getCheckOut());
        return service.searchRooms(req);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a room", description = "Amenities: BREAKFAST (+Rs.600/night), PARKING (+Rs.200/night), SPA (+Rs.1500/night)")
    public Map<String,Object> bookRoom(@Valid @RequestBody BookRoomRequest req) {
        log.info("[HotelController] POST /reservations | room={} guest={}", req.getRoomNumber(), req.getGuestEmail());
        return service.bookRoom(req);
    }

    @PostMapping("/reservations/{id}/check-in")
    @Operation(summary = "Check in guest")
    public Map<String,Object> checkIn(@PathVariable String id) { return service.checkIn(id); }

    @PostMapping("/reservations/{id}/check-out")
    @Operation(summary = "Check out guest")
    public Map<String,Object> checkOut(@PathVariable String id) { return service.checkOut(id); }

    @DeleteMapping("/reservations/{id}")
    @Operation(summary = "Cancel reservation")
    public Map<String,Object> cancel(@PathVariable String id) { return service.cancelReservation(id); }
}
