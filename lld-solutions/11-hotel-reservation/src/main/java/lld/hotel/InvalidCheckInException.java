package lld.hotel;
public class InvalidCheckInException extends HotelException {
    public InvalidCheckInException(String m) { super("Invalid check-in: " + m); }
}
