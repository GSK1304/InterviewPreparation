package lld.elevator;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface DispatchStrategy {
    Optional<Elevator> selectElevator(List<Elevator> elevators, int requestFloor, Direction dir);
}
