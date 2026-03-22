package lld.cabooking.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.cabooking.dto.*;
import lld.cabooking.entity.Driver;
import lld.cabooking.service.CabBookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/v1/cab") @RequiredArgsConstructor
@Tag(name = "Cab Booking", description = "Ride lifecycle with Haversine distance-based driver matching")
public class CabBookingController {
    private static final Logger log = LoggerFactory.getLogger(CabBookingController.class);
    private final CabBookingService service;

    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a driver")
    public Driver registerDriver(@Valid @RequestBody RegisterDriverRequest req) { return service.registerDriver(req); }

    @PostMapping("/drivers/{driverId}/location")
    @Operation(summary = "Update driver location")
    public ResponseEntity<String> updateLocation(@PathVariable String driverId, @Valid @RequestBody LocationUpdateRequest req) {
        service.updateLocation(driverId, req); return ResponseEntity.ok("Location updated"); }

    @PostMapping("/rides")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a ride", description = "Finds nearest available driver using Haversine distance (max 5km radius)")
    public Map<String,Object> requestRide(@Valid @RequestBody RequestRideRequest req) {
        log.info("[CabController] POST /rides | riderId={} type={}", req.getRiderId(), req.getVehicleType());
        return service.requestRide(req);
    }

    @PatchMapping("/rides/{rideId}/status/{action}")
    @Operation(summary = "Update ride status", description = "Actions: ARRIVE, START, COMPLETE, CANCEL")
    public Map<String,Object> updateStatus(@PathVariable String rideId, @PathVariable String action) {
        log.info("[CabController] PATCH /rides/{}/status/{}", rideId, action);
        return service.updateRideStatus(rideId, action);
    }

    @PostMapping("/rides/{rideId}/rate")
    @Operation(summary = "Rate a driver after completed ride")
    public Map<String,Object> rateDriver(@PathVariable String rideId, @Valid @RequestBody RateRequest req) {
        return service.rateDriver(rideId, req);
    }
}
