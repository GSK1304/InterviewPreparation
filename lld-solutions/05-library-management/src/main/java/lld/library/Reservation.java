package lld.library;
import java.time.LocalDate;
import java.util.Objects;

public class Reservation {
    private final String    reservationId;
    private final Member    member;
    private final ISBN      isbn;
    private final LocalDate reservedDate;
    private static int counter = 5000;

    public Reservation(Member member, ISBN isbn) {
        this.reservationId = "RES" + counter++;
        this.member        = Objects.requireNonNull(member);
        this.isbn          = Objects.requireNonNull(isbn);
        this.reservedDate  = LocalDate.now();
    }

    public void notifyAvailable(String bookTitle) {
        System.out.printf("Email to %s: '%s' is now available.%n", member.getEmail(), bookTitle);
    }

    public Member getMember()        { return member; }
    public ISBN   getIsbn()          { return isbn; }
    public String getReservationId() { return reservationId; }
}
