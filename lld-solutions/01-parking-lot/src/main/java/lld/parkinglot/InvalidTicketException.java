package lld.parkinglot;
public class InvalidTicketException extends ParkingException {
    public InvalidTicketException(String id) { super("Invalid or already used ticket: " + id); }
}
