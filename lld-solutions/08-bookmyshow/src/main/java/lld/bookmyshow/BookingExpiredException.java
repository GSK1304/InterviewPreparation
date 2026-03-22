package lld.bookmyshow;
public class BookingExpiredException extends BookingException {
    public BookingExpiredException(String id) { super("Booking expired: " + id); }
}
