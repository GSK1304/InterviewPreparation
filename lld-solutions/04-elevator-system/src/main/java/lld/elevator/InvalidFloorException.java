package lld.elevator;
public class InvalidFloorException extends ElevatorException {
    public InvalidFloorException(int floor, int min, int max) {
        super(String.format("Floor %d out of range [%d, %d]", floor, min, max));
    }
}
