package lld.parkinglot;
public class VehicleAlreadyParkedException extends ParkingException {
    public VehicleAlreadyParkedException(String plate) { super("Vehicle already parked: " + plate); }
}
