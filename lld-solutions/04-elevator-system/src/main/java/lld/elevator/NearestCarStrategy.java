package lld.elevator;
import java.util.*;
public class NearestCarStrategy implements DispatchStrategy {
    @Override
    public Optional<Elevator> selectElevator(List<Elevator> elevators, int floor, Direction dir) {
        return elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - floor)));
    }
}
