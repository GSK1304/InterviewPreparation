package lld.hotel;
import java.time.LocalDate;
public class RoomNotAvailableException extends HotelException {
    public RoomNotAvailableException(String id, LocalDate from, LocalDate to) {
        super("Room " + id + " not available " + from + " to " + to);
    }
}
