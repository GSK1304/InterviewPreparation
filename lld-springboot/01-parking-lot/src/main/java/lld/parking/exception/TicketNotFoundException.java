package lld.parking.exception;

import org.springframework.http.HttpStatus;

public class TicketNotFoundException extends ParkingException {
    public TicketNotFoundException(String ticketId) {
        super("Ticket not found or already used: " + ticketId, HttpStatus.NOT_FOUND);
    }
}
