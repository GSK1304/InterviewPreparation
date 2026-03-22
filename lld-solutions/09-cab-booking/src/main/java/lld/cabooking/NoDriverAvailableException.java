package lld.cabooking;
public class NoDriverAvailableException extends CabException {
    public NoDriverAvailableException(VehicleType t) { super("No driver for: " + t); }
}
