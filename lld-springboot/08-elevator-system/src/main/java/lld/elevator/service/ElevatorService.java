package lld.elevator.service;
import jakarta.transaction.Transactional;
import lld.elevator.dto.*;
import lld.elevator.entity.Elevator;
import lld.elevator.entity.ElevatorRequest;
import lld.elevator.enums.*;
import lld.elevator.exception.ElevatorException;
import lld.elevator.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

@Service @RequiredArgsConstructor
public class ElevatorService {
    private static final Logger log = LoggerFactory.getLogger(ElevatorService.class);
    private final ElevatorRepository        elevatorRepo;
    private final ElevatorRequestRepository requestRepo;

    @Transactional
    public Map<String,Object> callElevator(CallElevatorRequest req) {
        log.info("[ElevatorService] Call elevator | floor={} direction={} strategy={}", req.getFloorNumber(), req.getDirection(), req.getStrategy());
        List<Elevator> available = elevatorRepo.findByStatusNot(ElevatorStatus.MAINTENANCE);
        if (available.isEmpty()) throw new ElevatorException("All elevators under maintenance", HttpStatus.SERVICE_UNAVAILABLE);
        DispatchStrategy strategy = req.getStrategy() != null ? req.getStrategy() : DispatchStrategy.SCAN;
        Elevator assigned = strategy == DispatchStrategy.NEAREST_CAR
            ? nearestCar(available, req.getFloorNumber())
            : scanStrategy(available, req.getFloorNumber(), req.getDirection());
        addStop(assigned, req.getFloorNumber());
        elevatorRepo.save(assigned);
        ElevatorRequest er = new ElevatorRequest();
        er.setFloorNumber(req.getFloorNumber()); er.setDirection(req.getDirection());
        er.setAssignedElevatorId(assigned.getElevatorId());
        requestRepo.save(er);
        log.info("[ElevatorService] Elevator {} assigned to floor {} (currently at floor {})", assigned.getElevatorId(), req.getFloorNumber(), assigned.getCurrentFloor());
        return Map.of("assignedElevator",assigned.getElevatorId(),"currentFloor",assigned.getCurrentFloor(),"targetFloor",req.getFloorNumber(),"etaFloors",Math.abs(assigned.getCurrentFloor()-req.getFloorNumber()));
    }

    @Transactional
    public Map<String,Object> selectFloor(SelectFloorRequest req) {
        log.info("[ElevatorService] Floor selected | elevator={} floor={}", req.getElevatorId(), req.getFloorNumber());
        Elevator e = getElevator(req.getElevatorId());
        if (e.getStatus() == ElevatorStatus.MAINTENANCE) throw new ElevatorException("Elevator under maintenance: " + req.getElevatorId(), HttpStatus.CONFLICT);
        if (req.getFloorNumber() < e.getMinFloor() || req.getFloorNumber() > e.getMaxFloor())
            throw new ElevatorException("Floor " + req.getFloorNumber() + " out of range [" + e.getMinFloor() + "," + e.getMaxFloor() + "]", HttpStatus.BAD_REQUEST);
        addStop(e, req.getFloorNumber()); elevatorRepo.save(e);
        return Map.of("elevatorId",e.getElevatorId(),"pendingStops",getStops(e));
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void simulateStep() {
        elevatorRepo.findByStatusNot(ElevatorStatus.MAINTENANCE).forEach(e -> {
            TreeSet<Integer> upStops   = new TreeSet<>();
            TreeSet<Integer> downStops = new TreeSet<>(Collections.reverseOrder());
            for (int s : getStops(e)) { if (s > e.getCurrentFloor()) upStops.add(s); else if (s < e.getCurrentFloor()) downStops.add(s); }
            if (upStops.isEmpty() && downStops.isEmpty()) { e.setStatus(ElevatorStatus.IDLE); e.setDirection(Direction.IDLE); elevatorRepo.save(e); return; }
            if (e.getDirection() == Direction.UP || (e.getDirection() == Direction.IDLE && !upStops.isEmpty())) {
                if (!upStops.isEmpty()) { e.setStatus(ElevatorStatus.MOVING_UP); e.setDirection(Direction.UP); int next = upStops.first();
                    if (e.getCurrentFloor() < next) { e.setCurrentFloor(e.getCurrentFloor() + 1); }
                    else { removeStop(e, next); log.info("[ElevatorService] DOORS OPEN | elevator={} floor={}", e.getElevatorId(), next); }
                } else { e.setDirection(Direction.DOWN); }
            } else {
                if (!downStops.isEmpty()) { e.setStatus(ElevatorStatus.MOVING_DOWN); e.setDirection(Direction.DOWN); int next = downStops.first();
                    if (e.getCurrentFloor() > next) { e.setCurrentFloor(e.getCurrentFloor() - 1); }
                    else { removeStop(e, next); log.info("[ElevatorService] DOORS OPEN | elevator={} floor={}", e.getElevatorId(), next); }
                } else { e.setDirection(Direction.UP); }
            }
            elevatorRepo.save(e);
        });
    }

    @Transactional
    public Map<String,Object> setMaintenance(String elevatorId, boolean maintenance) {
        Elevator e = getElevator(elevatorId);
        if (maintenance && (e.getStatus() == ElevatorStatus.MOVING_UP || e.getStatus() == ElevatorStatus.MOVING_DOWN))
            throw new ElevatorException("Cannot put moving elevator in maintenance", HttpStatus.CONFLICT);
        e.setStatus(maintenance ? ElevatorStatus.MAINTENANCE : ElevatorStatus.IDLE);
        elevatorRepo.save(e);
        return Map.of("elevatorId",elevatorId,"status",e.getStatus().name());
    }

    public List<Map<String,Object>> getStatus() {
        return elevatorRepo.findAll().stream().map(e -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("elevatorId",e.getElevatorId()); m.put("name",e.getName());
            m.put("currentFloor",e.getCurrentFloor()); m.put("direction",e.getDirection());
            m.put("status",e.getStatus()); m.put("pendingStops",getStops(e));
            return m;
        }).collect(Collectors.toList());
    }

    private Elevator nearestCar(List<Elevator> elevators, int floor) {
        return elevators.stream().min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor))).orElseThrow();
    }

    private Elevator scanStrategy(List<Elevator> elevators, int floor, Direction dir) {
        // Priority: same direction and will pass floor
        return elevators.stream().filter(e -> isOnTheWay(e, floor, dir))
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)))
            .or(() -> elevators.stream().filter(e -> e.getStatus() == ElevatorStatus.IDLE).min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor))))
            .orElseGet(() -> nearestCar(elevators, floor));
    }

    private boolean isOnTheWay(Elevator e, int floor, Direction dir) {
        return (e.getDirection() == Direction.UP && dir == Direction.UP && e.getCurrentFloor() <= floor) ||
               (e.getDirection() == Direction.DOWN && dir == Direction.DOWN && e.getCurrentFloor() >= floor);
    }

    private List<Integer> getStops(Elevator e) {
        if (e.getPendingStops() == null || e.getPendingStops().isBlank()) return Collections.emptyList();
        return Arrays.stream(e.getPendingStops().split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    private void addStop(Elevator e, int floor) {
        Set<Integer> stops = new TreeSet<>(getStops(e)); stops.add(floor);
        e.setPendingStops(stops.stream().map(String::valueOf).collect(Collectors.joining(",")));
        if (e.getStatus() == ElevatorStatus.IDLE) { e.setDirection(floor > e.getCurrentFloor() ? Direction.UP : Direction.DOWN); e.setStatus(floor > e.getCurrentFloor() ? ElevatorStatus.MOVING_UP : ElevatorStatus.MOVING_DOWN); }
    }

    private void removeStop(Elevator e, int floor) {
        List<Integer> stops = new ArrayList<>(getStops(e)); stops.remove((Integer) floor);
        e.setPendingStops(stops.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    private Elevator getElevator(String id) { return elevatorRepo.findById(id).orElseThrow(() -> new ElevatorException("Elevator not found: " + id, HttpStatus.NOT_FOUND)); }
}
