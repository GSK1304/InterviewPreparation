package lld.elevator.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.elevator.dto.*;
import lld.elevator.service.ElevatorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/v1/elevator") @RequiredArgsConstructor
@Tag(name = "Elevator System", description = "SCAN algorithm dispatch with TreeSet stop queues. Simulates movement every 2 seconds.")
public class ElevatorController {
    private static final Logger log = LoggerFactory.getLogger(ElevatorController.class);
    private final ElevatorService service;

    @PostMapping("/call")
    @Operation(summary = "Call elevator to a floor", description = "Strategies: NEAREST_CAR or SCAN (default)")
    public Map<String,Object> callElevator(@Valid @RequestBody CallElevatorRequest req) {
        log.info("[ElevatorController] POST /call | floor={} dir={}", req.getFloorNumber(), req.getDirection());
        return service.callElevator(req);
    }

    @PostMapping("/select-floor")
    @Operation(summary = "Select destination floor from inside elevator")
    public Map<String,Object> selectFloor(@Valid @RequestBody SelectFloorRequest req) {
        return service.selectFloor(req);
    }

    @GetMapping("/status")
    @Operation(summary = "Get status of all elevators")
    public List<Map<String,Object>> getStatus() { return service.getStatus(); }

    @PatchMapping("/{elevatorId}/maintenance")
    @Operation(summary = "Toggle maintenance mode")
    public Map<String,Object> setMaintenance(@PathVariable String elevatorId, @RequestParam boolean enable) {
        return service.setMaintenance(elevatorId, enable);
    }
}
