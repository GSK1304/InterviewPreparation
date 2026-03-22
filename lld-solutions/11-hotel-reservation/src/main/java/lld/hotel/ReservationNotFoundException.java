package lld.hotel;
public class ReservationNotFoundException extends HotelException {
    public ReservationNotFoundException(String id) { super("Reservation not found: " + id); }
}
