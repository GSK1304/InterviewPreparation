package lld.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lld.parking.dto.*;
import lld.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/parking")
@RequiredArgsConstructor
@Validated
@Tag(name = "Parking Lot", description = "Park, unpark vehicles and check availability")
public class ParkingController {

    private static final Logger log = LoggerFactory.getLogger(ParkingController.class);
    private final ParkingService parkingService;

    @PostMapping("/park")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Park a vehicle", description = "Parks vehicle and returns ticket. Supports NEAREST_FLOOR and LOAD_BALANCED strategies.")
    public ParkResponse park(@Valid @RequestBody ParkRequest request) {
        log.info("[ParkingController] POST /park | plate={} type={}", request.getLicensePlate(), request.getVehicleType());
        ParkResponse response = parkingService.park(request);
        log.info("[ParkingController] Park success | ticketId={}", response.getTicketId());
        return response;
    }

    @PostMapping("/unpark")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unpark a vehicle", description = "Releases the spot and calculates parking fee.")
    public UnparkResponse unpark(@Valid @RequestBody UnparkRequest request) {
        log.info("[ParkingController] POST /unpark | ticketId={}", request.getTicketId());
        UnparkResponse response = parkingService.unpark(request.getTicketId());
        log.info("[ParkingController] Unpark success | fee={} duration={}min",
            response.getFee(), response.getDurationMinutes());
        return response;
    }

    @GetMapping("/availability")
    @Operation(summary = "Get floor availability", description = "Returns available spots per floor, broken down by size.")
    public List<FloorStatusResponse> getAvailability() {
        log.debug("[ParkingController] GET /availability");
        return parkingService.getAvailability();
    }

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Parking Lot Service is UP");
    }
}
