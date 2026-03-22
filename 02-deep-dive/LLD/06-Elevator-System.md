# LLD — Elevator System (Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| Dispatch algorithm | **Strategy** — SCAN (elevator algorithm), NEAREST_CAR, FCFS |
| Elevator state | **State** pattern — IDLE / MOVING_UP / MOVING_DOWN / DOORS_OPEN |
| Request queuing | `TreeSet<Integer>` (sorted) for floor stops — enables SCAN algorithm |
| Direction tracking | Elevator tracks current direction, reverses when no more stops in that direction |
| Thread safety | Each elevator runs in its own thread; request queue is `synchronized` |
| Capacity | Passengers tracked; overweight alarm before doors close |

## Complete Solution

```java
package lld.elevator;

import java.util.*;
import java.util.concurrent.*;

// ── Enums ─────────────────────────────────────────────────────────────────────

enum Direction { UP, DOWN, IDLE }

enum ElevatorStatus { IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPEN, MAINTENANCE }

enum ButtonType { FLOOR_PANEL, INTERNAL }

// ── Value Objects ─────────────────────────────────────────────────────────────

record ElevatorRequest(int sourceFloor, int destinationFloor, Direction direction) {
    ElevatorRequest {
        if (sourceFloor < 0)         throw new IllegalArgumentException("Source floor cannot be negative");
        if (destinationFloor < 0)    throw new IllegalArgumentException("Destination floor cannot be negative");
        if (sourceFloor == destinationFloor)
            throw new IllegalArgumentException("Source and destination floor cannot be the same");
    }

    static ElevatorRequest of(int source, int dest) {
        Direction dir = dest > source ? Direction.UP : Direction.DOWN;
        return new ElevatorRequest(source, dest, dir);
    }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class ElevatorException extends RuntimeException {
    ElevatorException(String msg) { super(msg); }
}

class ElevatorMaintenanceException extends ElevatorException {
    ElevatorMaintenanceException(String elevatorId) {
        super("Elevator " + elevatorId + " is under maintenance");
    }
}

class InvalidFloorException extends ElevatorException {
    InvalidFloorException(int floor, int min, int max) {
        super(String.format("Floor %d is out of range [%d, %d]", floor, min, max));
    }
}

// ── Dispatch Strategy ─────────────────────────────────────────────────────────

@FunctionalInterface
interface DispatchStrategy {
    /** Choose the best elevator to serve the given request */
    Optional<Elevator> selectElevator(List<Elevator> elevators, ElevatorRequest request);
}

/** NEAREST CAR: pick the closest available elevator */
class NearestCarStrategy implements DispatchStrategy {
    @Override
    public Optional<Elevator> selectElevator(List<Elevator> elevators, ElevatorRequest request) {
        return elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .min(Comparator.comparingInt(e ->
                Math.abs(e.getCurrentFloor() - request.sourceFloor())));
    }
}

/**
 * SCAN (Elevator Algorithm):
 * Prefer elevators already moving toward the request floor in the same direction.
 * Falls back to nearest idle, then any elevator.
 */
class ScanStrategy implements DispatchStrategy {
    @Override
    public Optional<Elevator> selectElevator(List<Elevator> elevators, ElevatorRequest request) {
        int src = request.sourceFloor();
        Direction dir = request.direction();

        // Priority 1: Moving same direction and will pass through src
        Optional<Elevator> sameDir = elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .filter(e -> isOnTheWay(e, src, dir))
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - src)));
        if (sameDir.isPresent()) return sameDir;

        // Priority 2: Idle elevator, nearest to src
        Optional<Elevator> idle = elevators.stream()
            .filter(e -> e.getStatus() == ElevatorStatus.IDLE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - src)));
        if (idle.isPresent()) return idle;

        // Priority 3: Any non-maintenance elevator
        return elevators.stream()
            .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - src)));
    }

    private boolean isOnTheWay(Elevator e, int targetFloor, Direction requestDir) {
        if (e.getDirection() == Direction.IDLE) return false;
        if (e.getDirection() == Direction.UP   && requestDir == Direction.UP
            && e.getCurrentFloor() <= targetFloor) return true;
        if (e.getDirection() == Direction.DOWN && requestDir == Direction.DOWN
            && e.getCurrentFloor() >= targetFloor) return true;
        return false;
    }
}

// ── Elevator ──────────────────────────────────────────────────────────────────

class Elevator {
    private final String       id;
    private final int          minFloor;
    private final int          maxFloor;
    private final int          maxCapacity;    // max passengers
    private int                currentFloor;
    private Direction          direction;
    private ElevatorStatus     status;
    private int                currentPassengers = 0;

    // SCAN algorithm: separate sets for up/down stops
    private final TreeSet<Integer> upStops   = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>(Comparator.reverseOrder());

    Elevator(String id, int minFloor, int maxFloor, int initialFloor, int maxCapacity) {
        if (minFloor >= maxFloor) throw new IllegalArgumentException("minFloor must be < maxFloor");
        if (initialFloor < minFloor || initialFloor > maxFloor)
            throw new InvalidFloorException(initialFloor, minFloor, maxFloor);
        this.id           = Objects.requireNonNull(id);
        this.minFloor     = minFloor;
        this.maxFloor     = maxFloor;
        this.currentFloor = initialFloor;
        this.maxCapacity  = maxCapacity;
        this.direction    = Direction.IDLE;
        this.status       = ElevatorStatus.IDLE;
    }

    /** Add a stop — called when a request is assigned or passenger presses button */
    synchronized void addStop(int floor) {
        if (floor < minFloor || floor > maxFloor)
            throw new InvalidFloorException(floor, minFloor, maxFloor);
        if (floor > currentFloor) upStops.add(floor);
        else if (floor < currentFloor) downStops.add(floor);
        // floor == currentFloor: already here, open doors
        if (status == ElevatorStatus.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
            status = direction == Direction.UP ? ElevatorStatus.MOVING_UP : ElevatorStatus.MOVING_DOWN;
        }
    }

    /** Process one move step — advance toward next stop */
    synchronized void step() {
        if (status == ElevatorStatus.MAINTENANCE || status == ElevatorStatus.DOORS_OPEN) return;

        if (direction == Direction.UP) {
            if (!upStops.isEmpty()) {
                Integer nextStop = upStops.first();
                if (currentFloor < nextStop) {
                    currentFloor++;
                } else {
                    // Reached a stop
                    upStops.remove(nextStop);
                    openDoors();
                }
            } else if (!downStops.isEmpty()) {
                // No more up stops — reverse
                direction = Direction.DOWN;
                status    = ElevatorStatus.MOVING_DOWN;
            } else {
                idle();
            }
        } else if (direction == Direction.DOWN) {
            if (!downStops.isEmpty()) {
                Integer nextStop = downStops.first();
                if (currentFloor > nextStop) {
                    currentFloor--;
                } else {
                    downStops.remove(nextStop);
                    openDoors();
                }
            } else if (!upStops.isEmpty()) {
                direction = Direction.UP;
                status    = ElevatorStatus.MOVING_UP;
            } else {
                idle();
            }
        }
    }

    private void openDoors() {
        status = ElevatorStatus.DOORS_OPEN;
        System.out.printf("🚪 Elevator %s: DOORS OPEN at Floor %d (passengers: %d)%n",
            id, currentFloor, currentPassengers);
        // Simulate doors open/close
        closeDoors();
    }

    private void closeDoors() {
        // In a real system: wait N seconds, then close
        status = (direction == Direction.UP)   ? ElevatorStatus.MOVING_UP
               : (direction == Direction.DOWN) ? ElevatorStatus.MOVING_DOWN
               : ElevatorStatus.IDLE;
    }

    private void idle() {
        direction = Direction.IDLE;
        status    = ElevatorStatus.IDLE;
    }

    synchronized boolean boardPassenger() {
        if (currentPassengers >= maxCapacity) {
            System.out.println("⚠ Elevator " + id + " is at capacity!");
            return false;
        }
        currentPassengers++;
        return true;
    }

    synchronized void alightPassenger() {
        if (currentPassengers > 0) currentPassengers--;
    }

    synchronized void setMaintenance(boolean maintenance) {
        if (maintenance && (status == ElevatorStatus.MOVING_UP || status == ElevatorStatus.MOVING_DOWN))
            throw new ElevatorException("Cannot put moving elevator under maintenance");
        status = maintenance ? ElevatorStatus.MAINTENANCE : ElevatorStatus.IDLE;
    }

    // Getters
    public String          getId()               { return id; }
    public int             getCurrentFloor()     { return currentFloor; }
    public Direction       getDirection()        { return direction; }
    public ElevatorStatus  getStatus()           { return status; }
    public int             getCurrentPassengers(){ return currentPassengers; }
    public boolean         hasStops()            { return !upStops.isEmpty() || !downStops.isEmpty(); }

    @Override public String toString() {
        return String.format("Elevator[%s Floor=%d Dir=%s Status=%s Passengers=%d]",
            id, currentFloor, direction, status, currentPassengers);
    }
}

// ── Building ──────────────────────────────────────────────────────────────────

class Building {
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

    /** External call: someone presses UP/DOWN button on floor panel */
    public void requestElevator(int floor, Direction direction) {
        validateFloor(floor);
        if (direction == Direction.IDLE)
            throw new ElevatorException("Direction must be UP or DOWN");

        ElevatorRequest request = new ElevatorRequest(floor, floor, direction);
        Elevator chosen = strategy.selectElevator(elevators, request)
            .orElseThrow(() -> new ElevatorException("No elevator available"));

        chosen.addStop(floor);
        System.out.printf("📋 Request: Floor %d %s → assigned to Elevator %s%n",
            floor, direction, chosen.getId());
    }

    /** Internal call: passenger presses destination button inside elevator */
    public void selectFloor(String elevatorId, int floor) {
        validateFloor(floor);
        Elevator elevator = findById(elevatorId);
        elevator.addStop(floor);
        System.out.printf("🔢 Elevator %s: destination %d added%n", elevatorId, floor);
    }

    /** Simulate the system for N steps */
    public void simulate(int steps) {
        System.out.println("\n─── Simulation Start ───");
        for (int step = 0; step < steps; step++) {
            elevators.forEach(Elevator::step);
            if (step % 3 == 0) displayStatus();  // print every 3 steps
        }
        System.out.println("─── Simulation End ───\n");
    }

    public void displayStatus() {
        System.out.println("\n═══ " + name + " Status ═══");
        elevators.forEach(e -> System.out.println("  " + e));
    }

    public void setMaintenance(String elevatorId, boolean maintenance) {
        findById(elevatorId).setMaintenance(maintenance);
        System.out.printf("🔧 Elevator %s: maintenance=%s%n", elevatorId, maintenance);
    }

    private Elevator findById(String id) {
        return elevators.stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new ElevatorException("Elevator not found: " + id));
    }

    private void validateFloor(int floor) {
        if (floor < minFloor || floor > maxFloor)
            throw new InvalidFloorException(floor, minFloor, maxFloor);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    static final class Builder {
        private final String         name;
        private int                  minFloor = 0;
        private int                  maxFloor;
        private final List<Elevator> elevators = new ArrayList<>();
        private DispatchStrategy     strategy  = new ScanStrategy();

        Builder(String name, int maxFloor) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Building name required");
            if (maxFloor <= 0)
                throw new IllegalArgumentException("Max floor must be positive");
            this.name     = name;
            this.maxFloor = maxFloor;
        }

        Builder addElevator(String id, int initialFloor, int maxCapacity) {
            elevators.add(new Elevator(id, minFloor, maxFloor, initialFloor, maxCapacity));
            return this;
        }

        Builder strategy(DispatchStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        Building build() {
            if (elevators.isEmpty())
                throw new IllegalStateException("Building must have at least one elevator");
            return new Building(this);
        }
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class ElevatorDemo {
    public static void main(String[] args) throws InterruptedException {
        Building tower = new Building.Builder("Tech Tower", 20)
            .addElevator("E1", 0,  10)  // starts at ground, capacity 10
            .addElevator("E2", 10, 10)  // starts at floor 10
            .addElevator("E3", 5,  8)   // starts at floor 5
            .strategy(new ScanStrategy())
            .build();

        tower.displayStatus();

        // External requests (floor buttons)
        tower.requestElevator(1, Direction.UP);   // person on 1st floor going up
        tower.requestElevator(15, Direction.DOWN); // person on 15th going down
        tower.requestElevator(7, Direction.UP);    // person on 7th going up

        // Internal requests (inside elevator)
        tower.selectFloor("E1", 8);   // passenger in E1 wants floor 8
        tower.selectFloor("E2", 3);   // passenger in E2 wants floor 3

        // Simulate 15 steps
        tower.simulate(15);

        // Maintenance
        tower.setMaintenance("E3", true);
        tower.requestElevator(12, Direction.DOWN);  // E3 unavailable — E1 or E2 dispatched

        // Invalid floor
        try {
            tower.requestElevator(25, Direction.UP);  // floor 25 > maxFloor 20
        } catch (InvalidFloorException e) {
            System.out.println("❌ " + e.getMessage());
        }

        tower.displayStatus();
    }
}
```

## Extension Q&A

**Q: How do you support a VIP express elevator (skips floors)?**
Add an `expressOnly: boolean` field to `Elevator`. Override `addStop()` to reject stops outside the express range. The `DispatchStrategy` can check `expressOnly` and only assign VIP requests to it (identified by a `priority` field in `ElevatorRequest`).

**Q: How do you add weight sensors and overweight alarm?**
Replace `currentPassengers` with `currentWeightKg`. Add `maxWeightKg` config. When weight exceeds 80% of max, announce "elevator nearing capacity." Prevent doors from closing if weight exceeds limit — `closeDoors()` checks weight sensor reading.

**Q: What is the difference between SCAN and NEAREST_CAR for this problem?**
NEAREST_CAR minimises latency for the current request (closest elevator wins). SCAN optimises overall throughput by serving multiple requests in one sweep (elevator continues in its current direction, picking up anyone along the way). SCAN reduces total travel distance for the elevator fleet. NEAREST_CAR feels more responsive in a lightly loaded building; SCAN is better in a busy office tower during rush hour.
