package lld.elevator;
import java.util.*;

public class Building {
    private final String           name;
    private final int              minFloor;
    private final int              maxFloor;
    private final List<Elevator>   elevators;
    private final DispatchStrategy strategy;

    private Building(Builder builder) {
        this.name      = builder.name;
        this.minFloor  = builder.minFloor;
        this.maxFloor  = builder.maxFloor;
        this.elevators = Collections.unmodifiableList(builder.elevators);
        this.strategy  = builder.strategy;
    }

    public void requestElevator(int floor, Direction dir) {
        validateFloor(floor);
        Elevator chosen = strategy.selectElevator(elevators, floor, dir)
            .orElseThrow(() -> new ElevatorException("No elevator available"));
        chosen.addStop(floor);
        System.out.printf("Request: Floor %d %s -> assigned to %s%n", floor, dir, chosen.getId());
    }

    public void selectFloor(String elevatorId, int floor) {
        validateFloor(floor);
        findById(elevatorId).addStop(floor);
        System.out.printf("Elevator %s: destination %d added%n", elevatorId, floor);
    }

    public void simulate(int steps) {
        System.out.println("\n--- Simulation Start ---");
        for (int i = 0; i < steps; i++) {
            elevators.forEach(Elevator::step);
            if (i % 5 == 0) displayStatus();
        }
        System.out.println("--- Simulation End ---\n");
    }

    public void displayStatus() {
        System.out.println("\n=== " + name + " Status ===");
        elevators.forEach(e -> System.out.println("  " + e));
    }

    public void setMaintenance(String elevatorId, boolean maintenance) {
        findById(elevatorId).setMaintenance(maintenance);
    }

    private Elevator findById(String id) {
        return elevators.stream().filter(e -> e.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ElevatorException("Elevator not found: " + id));
    }

    private void validateFloor(int floor) {
        if (floor < minFloor || floor > maxFloor) throw new InvalidFloorException(floor, minFloor, maxFloor);
    }

    public static final class Builder {
        private final String         name;
        private int                  minFloor = 0;
        private int                  maxFloor;
        private final List<Elevator> elevators = new ArrayList<>();
        private DispatchStrategy     strategy  = new ScanStrategy();

        public Builder(String name, int maxFloor) {
            this.name = Objects.requireNonNull(name);
            this.maxFloor = maxFloor;
        }

        public Builder addElevator(String id, int initialFloor, int maxCapacity) {
            elevators.add(new Elevator(id, minFloor, maxFloor, initialFloor, maxCapacity));
            return this;
        }

        public Builder strategy(DispatchStrategy s) { this.strategy = s; return this; }

        public Building build() {
            if (elevators.isEmpty()) throw new IllegalStateException("Need at least one elevator");
            return new Building(this);
        }
    }
}
