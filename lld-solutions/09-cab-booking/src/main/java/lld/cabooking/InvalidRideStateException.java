package lld.cabooking;
public class InvalidRideStateException extends CabException {
    public InvalidRideStateException(String exp, RideStatus actual) {
        super("Expected " + exp + " but got: " + actual);
    }
}
