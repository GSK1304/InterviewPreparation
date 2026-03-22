package lld.bookmyshow;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class Booking {
    private static int counter = 10000;
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final String         bookingId, userId;
    private final Show           show;
    private final List<Seat>     seats;
    private final Money          totalAmount;
    private BookingStatus        status;
    private final Instant        lockExpiry;

    public Booking(String userId, Show show, List<Seat> seats, Money totalAmount) {
        this.bookingId   = "BKG-" + counter++;
        this.userId      = Objects.requireNonNull(userId);
        this.show        = Objects.requireNonNull(show);
        this.seats       = Collections.unmodifiableList(new ArrayList<>(seats));
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status      = BookingStatus.LOCKED;
        this.lockExpiry  = Instant.now().plus(LOCK_TTL);
    }

    public boolean isExpired() { return Instant.now().isAfter(lockExpiry); }

    public void confirm() {
        if (isExpired()) throw new BookingExpiredException(bookingId);
        if (status != BookingStatus.LOCKED) throw new BookingException("Not LOCKED: " + status);
        status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == BookingStatus.CANCELLED) throw new BookingException("Already cancelled");
        status = BookingStatus.CANCELLED;
    }

    public String        getBookingId()   { return bookingId; }
    public String        getUserId()      { return userId; }
    public Show          getShow()        { return show; }
    public List<Seat>    getSeats()       { return seats; }
    public Money         getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus()      { return status; }

    @Override public String toString() {
        return String.format("Booking[%s User=%s Seats=%s %s %s]", bookingId, userId,
            seats.stream().map(Seat::getSeatId).collect(Collectors.joining(",")), totalAmount, status);
    }
}
