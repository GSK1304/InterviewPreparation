package lld.bookmyshow;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BookingService {
    private final Map<String, Show>    shows    = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final PricingEngine        pricing  = new PricingEngine();
    private static final java.time.Duration LOCK_TTL = java.time.Duration.ofMinutes(5);

    public void addShow(Show show) { shows.put(show.getShowId(), show); System.out.println("Added: " + show); }

    public List<Show> searchShows(String movie, LocalDate date) {
        return shows.values().stream()
            .filter(s -> s.getMovieName().equalsIgnoreCase(movie))
            .filter(s -> s.getShowTime().toLocalDate().equals(date))
            .sorted(Comparator.comparing(Show::getShowTime))
            .collect(Collectors.toList());
    }

    public Booking lockSeats(String userId, String showId, int count,
                             SeatSelectionStrategy strategy, Map<String, Object> params) {
        Show show = getShow(showId);
        List<Seat> selected = strategy.select(show.getScreen(), count, params);
        String tmpId = "TMP-" + UUID.randomUUID();
        List<Seat> locked = new ArrayList<>();
        try {
            for (Seat s : selected) {
                if (!s.tryLock(tmpId, LOCK_TTL)) throw new SeatNotAvailableException(s.getSeatId());
                locked.add(s);
            }
        } catch (SeatNotAvailableException e) {
            locked.forEach(s -> s.release(tmpId)); throw e;
        }
        Money total   = pricing.calculateTotal(locked, show.getShowTime());
        Booking booking = new Booking(userId, show, locked, total);
        locked.forEach(s -> { s.release(tmpId); s.tryLock(booking.getBookingId(), LOCK_TTL); });
        bookings.put(booking.getBookingId(), booking);
        System.out.printf("Locked: %s | %s | Pay within 5 min%n", booking.getBookingId(), total);
        return booking;
    }

    public Booking confirmBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        if (booking.isExpired()) { releaseSeats(booking); booking.cancel(); throw new BookingExpiredException(bookingId); }
        booking.getSeats().forEach(s -> s.confirm(bookingId));
        booking.confirm();
        System.out.println("Confirmed: " + booking);
        return booking;
    }

    public void cancelBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        releaseSeats(booking);
        booking.cancel();
        System.out.println("Cancelled: " + bookingId);
    }

    public void displayAvailability(String showId) {
        Show show = getShow(showId);
        System.out.printf("%n=== %s === Available: %d/%d%n",
            show.getMovieName(), show.getScreen().availableCount(), show.getScreen().getTotalSeats());
        for (SeatTier tier : SeatTier.values()) {
            long cnt = show.getScreen().getAvailableByTier(tier).size();
            if (cnt > 0) System.out.printf("  %-12s: %2d | %s%n", tier, cnt,
                pricing.calculatePrice(show.getScreen().getAvailableByTier(tier).get(0), show.getShowTime()));
        }
    }

    private void releaseSeats(Booking b) { b.getSeats().forEach(s -> s.release(b.getBookingId())); }
    private Show    getShow(String id)   { Show s    = shows.get(id);    if (s==null) throw new ShowNotFoundException(id);                    return s; }
    private Booking getBooking(String id){ Booking b = bookings.get(id); if (b==null) throw new BookingException("Not found: " + id);         return b; }
}
