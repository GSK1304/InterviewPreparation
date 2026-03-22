package lld.bookmyshow;
public class SeatNotAvailableException extends BookingException {
    public SeatNotAvailableException(String id) { super("Seat not available: " + id); }
}
