package lld.parkinglot;
public class NoSpotAvailableException extends ParkingException {
    public NoSpotAvailableException(VehicleType t) { super("No available spot for: " + t); }
}
