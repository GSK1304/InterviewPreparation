package lld.elevator;
import java.util.*;
public class ScanStrategy implements DispatchStrategy {
    @Override
    public Optional<Elevator> selectElevator(List<Elevator> elevators, int floor, Direction dir) {
        // Priority 1: elevator moving same direction and will pass through floor
        Optional<Elevator> sameDir = elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .filter(e -> isOnTheWay(e, floor, dir))
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
        if (sameDir.isPresent()) return sameDir;
        // Priority 2: idle elevator nearest to floor
        Optional<Elevator> idle = elevators.stream()
            .filter(e -> e.getStatus() == ElevatorStatus.IDLE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
        if (idle.isPresent()) return idle;
        // Priority 3: any non-maintenance elevator
        return elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
    }

    private boolean isOnTheWay(Elevator e, int floor, Direction dir) {
        if (e.getDirection() == Direction.IDLE) return false;
        return (e.getDirection() == Direction.UP   && dir == Direction.UP   && e.getCurrentFloor() <= floor) ||
               (e.getDirection() == Direction.DOWN && dir == Direction.DOWN && e.getCurrentFloor() >= floor);
    }
}
