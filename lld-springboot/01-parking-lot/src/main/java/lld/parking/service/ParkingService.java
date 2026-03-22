package lld.parking.service;

import jakarta.transaction.Transactional;
import lld.parking.dto.*;
import lld.parking.entity.*;
import lld.parking.enums.*;
import lld.parking.exception.*;
import lld.parking.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

    private final ParkingFloorRepository  floorRepository;
    private final ParkingSpotRepository   spotRepository;
    private final ParkingTicketRepository ticketRepository;
    private final Map<String, SpotSelectionStrategy> strategies;

    // ── Park Vehicle ──────────────────────────────────────────────────────────

    @Transactional
    public ParkResponse park(ParkRequest request) {
        log.info("[ParkingService] Park request | plate={} type={} strategy={}",
            request.getLicensePlate(), request.getVehicleType(), request.getStrategy());

        if (ticketRepository.existsByLicensePlateAndActiveTrue(request.getLicensePlate())) {
            log.warn("[ParkingService] Vehicle already parked: {}", request.getLicensePlate());
            throw new VehicleAlreadyParkedException(request.getLicensePlate());
        }

        SpotSelectionStrategy strategy = strategies.get(request.getStrategy().name());
        ParkingSpot spot = strategy.select(request.getVehicleType())
            .orElseThrow(() -> new NoSpotAvailableException(request.getVehicleType()));

        log.debug("[ParkingService] Selected spot={} floor={}", spot.getSpotCode(), spot.getFloor().getFloorNumber());

        // Atomically mark spot as OCCUPIED
        int updated = spotRepository.updateStatus(spot.getId(), SpotStatus.OCCUPIED);
        if (updated == 0) {
            log.warn("[ParkingService] Spot {} was taken concurrently, retrying...", spot.getSpotCode());
            throw new NoSpotAvailableException(request.getVehicleType());
        }

        // Issue ticket
        ParkingTicket ticket = new ParkingTicket();
        ticket.setTicketId("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setLicensePlate(request.getLicensePlate().toUpperCase());
        ticket.setVehicleType(request.getVehicleType());
        ticket.setSpot(spot);
        ticket.setEntryTime(Instant.now());
        ticket.setActive(true);
        ticketRepository.save(ticket);

        log.info("[ParkingService] Ticket issued | ticketId={} plate={} spot={} floor={}",
            ticket.getTicketId(), ticket.getLicensePlate(),
            spot.getSpotCode(), spot.getFloor().getFloorNumber());

        return ParkResponse.builder()
            .ticketId(ticket.getTicketId())
            .licensePlate(ticket.getLicensePlate())
            .vehicleType(request.getVehicleType())
            .spotCode(spot.getSpotCode())
            .spotSize(spot.getSize())
            .floorNumber(spot.getFloor().getFloorNumber())
            .entryTime(ticket.getEntryTime())
            .hourlyRate(formatAmount(request.getVehicleType().getHourlyRatePaise()))
            .message("Vehicle parked successfully")
            .build();
    }

    // ── Unpark Vehicle ────────────────────────────────────────────────────────

    @Transactional
    public UnparkResponse unpark(String ticketId) {
        log.info("[ParkingService] Unpark request | ticketId={}", ticketId);

        ParkingTicket ticket = ticketRepository.findByTicketIdAndActiveTrue(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Instant exitTime = Instant.now();
        Duration duration = Duration.between(ticket.getEntryTime(), exitTime);
        long minutesParked = Math.max(1, duration.toMinutes());
        long hoursBilled   = (long) Math.ceil(minutesParked / 60.0);
        long feePaise      = hoursBilled * ticket.getVehicleType().getHourlyRatePaise();

        // Update ticket
        ticket.setExitTime(exitTime);
        ticket.setFeePaise(feePaise);
        ticket.setActive(false);
        ticketRepository.save(ticket);

        // Release spot
        spotRepository.updateStatus(ticket.getSpot().getId(), SpotStatus.AVAILABLE);

        log.info("[ParkingService] Vehicle unparked | ticketId={} plate={} duration={}min fee={}",
            ticketId, ticket.getLicensePlate(), minutesParked, formatAmount(feePaise));

        return UnparkResponse.builder()
            .ticketId(ticketId)
            .licensePlate(ticket.getLicensePlate())
            .spotCode(ticket.getSpot().getSpotCode())
            .entryTime(ticket.getEntryTime())
            .exitTime(exitTime)
            .durationMinutes(minutesParked)
            .fee(formatAmount(feePaise))
            .message("Vehicle unparked successfully")
            .build();
    }

    // ── Availability ──────────────────────────────────────────────────────────

    public List<FloorStatusResponse> getAvailability() {
        log.debug("[ParkingService] Fetching floor availability");
        return floorRepository.findAllWithSpotsOrderByFloor().stream()
            .map(floor -> {
                Map<String, Long> bySize = floor.getSpots().stream()
                    .filter(s -> s.getStatus() == SpotStatus.AVAILABLE)
                    .collect(Collectors.groupingBy(s -> s.getSize().name(), Collectors.counting()));
                return FloorStatusResponse.builder()
                    .floorNumber(floor.getFloorNumber())
                    .name(floor.getName())
                    .totalSpots((long) floor.getSpots().size())
                    .availableSpots(floor.availableCount())
                    .occupiedSpots(floor.getSpots().size() - floor.availableCount())
                    .availableBySize(bySize)
                    .build();
            }).toList();
    }

    private String formatAmount(long paise) {
        return String.format("Rs.%.2f", paise / 100.0);
    }
}
