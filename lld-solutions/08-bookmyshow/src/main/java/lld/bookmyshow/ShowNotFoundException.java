package lld.bookmyshow;
public class ShowNotFoundException extends BookingException {
    public ShowNotFoundException(String id) { super("Show not found: " + id); }
}
