package lld.elevator;
import java.util.*;

public class Elevator {
    private final String          id;
    private final int             minFloor;
    private final int             maxFloor;
    private final int             maxCapacity;
    private int                   currentFloor;
    private Direction             direction;
    private ElevatorStatus        status;
    private int                   currentPassengers = 0;
    private final TreeSet<Integer> upStops   = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>(Comparator.reverseOrder());

    public Elevator(String id, int minFloor, int maxFloor, int initialFloor, int maxCapacity) {
        this.id           = Objects.requireNonNull(id);
        this.minFloor     = minFloor;
        this.maxFloor     = maxFloor;
        this.currentFloor = initialFloor;
        this.maxCapacity  = maxCapacity;
        this.direction    = Direction.IDLE;
        this.status       = ElevatorStatus.IDLE;
    }

    public synchronized void addStop(int floor) {
        if (floor < minFloor || floor > maxFloor) throw new InvalidFloorException(floor, minFloor, maxFloor);
        if (floor > currentFloor) upStops.add(floor);
        else if (floor < currentFloor) downStops.add(floor);
        if (status == ElevatorStatus.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
            status    = direction == Direction.UP ? ElevatorStatus.MOVING_UP : ElevatorStatus.MOVING_DOWN;
        }
    }

    public synchronized void step() {
        if (status == ElevatorStatus.MAINTENANCE || status == ElevatorStatus.DOORS_OPEN) return;
        if (direction == Direction.UP) {
            if (!upStops.isEmpty()) {
                int next = upStops.first();
                if (currentFloor < next) { currentFloor++; }
                else { upStops.remove(next); openDoors(); }
            } else if (!downStops.isEmpty()) { direction = Direction.DOWN; status = ElevatorStatus.MOVING_DOWN; }
            else idle();
        } else if (direction == Direction.DOWN) {
            if (!downStops.isEmpty()) {
                int next = downStops.first();
                if (currentFloor > next) { currentFloor--; }
                else { downStops.remove(next); openDoors(); }
            } else if (!upStops.isEmpty()) { direction = Direction.UP; status = ElevatorStatus.MOVING_UP; }
            else idle();
        }
    }

    private void openDoors() {
        status = ElevatorStatus.DOORS_OPEN;
        System.out.printf("  [%s] DOORS OPEN at Floor %d%n", id, currentFloor);
        status = (direction == Direction.UP) ? ElevatorStatus.MOVING_UP :
                 (direction == Direction.DOWN) ? ElevatorStatus.MOVING_DOWN : ElevatorStatus.IDLE;
    }

    private void idle() { direction = Direction.IDLE; status = ElevatorStatus.IDLE; }

    public synchronized void setMaintenance(boolean m) {
        if (m && (status == ElevatorStatus.MOVING_UP || status == ElevatorStatus.MOVING_DOWN))
            throw new ElevatorException("Cannot put moving elevator under maintenance");
        status = m ? ElevatorStatus.MAINTENANCE : ElevatorStatus.IDLE;
    }

    public String          getId()               { return id; }
    public int             getCurrentFloor()     { return currentFloor; }
    public Direction       getDirection()        { return direction; }
    public ElevatorStatus  getStatus()           { return status; }
    public int             getCurrentPassengers(){ return currentPassengers; }

    @Override public String toString() {
        return String.format("Elevator[%s Floor=%d Dir=%s Status=%s]", id, currentFloor, direction, status);
    }
}
