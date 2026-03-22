# LLD — BookMyShow (Movie Ticket Booking) — Complete Java 21

## Design Summary
| Aspect | Decision |
|--------|----------|
| Seat locking | Temporary lock (5 min TTL) before payment — prevents double booking |
| Seat selection | **Strategy** — BestAvailable, SpecificSeat, GroupTogether |
| Booking state | **State** — LOCKED / CONFIRMED / CANCELLED / EXPIRED |
| Pricing | Base price + seat tier multiplier + peak hour surcharge |
| Thread safety | `synchronized` on seat state transitions |
| Patterns | Strategy (seat selection), State (booking), Factory (show creation) |

## Complete Solution

```java
package lld.bookmyshow;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

// ── Enums ─────────────────────────────────────────────────────────────────────

enum SeatTier       { RECLINER, PREMIUM, EXECUTIVE, NORMAL }
enum SeatStatus     { AVAILABLE, LOCKED, BOOKED, MAINTENANCE }
enum BookingStatus  { LOCKED, CONFIRMED, CANCELLED, EXPIRED }
enum PaymentStatus  { PENDING, SUCCESS, FAILED, REFUNDED }

// ── Money ─────────────────────────────────────────────────────────────────────

record Money(long paise) {
    static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    Money multiply(double factor)   { return new Money(Math.round(paise * factor)); }
    Money add(Money o)              { return new Money(paise + o.paise); }
    double toRupees()               { return paise / 100.0; }
    @Override public String toString() { return String.format("₹%.2f", toRupees()); }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class BookingException       extends RuntimeException { BookingException(String m)       { super(m); } }
class SeatNotAvailableException extends BookingException {
    SeatNotAvailableException(String seatId) { super("Seat not available: " + seatId); }
}
class BookingExpiredException extends BookingException {
    BookingExpiredException(String id) { super("Booking expired: " + id); }
}
class ShowNotFoundException   extends BookingException {
    ShowNotFoundException(String id) { super("Show not found: " + id); }
}

// ── Seat ──────────────────────────────────────────────────────────────────────

class Seat {
    private final String     seatId;
    private final int        row;
    private final int        col;
    private final SeatTier   tier;
    private SeatStatus       status = SeatStatus.AVAILABLE;
    private String           lockedByBookingId;
    private Instant          lockExpiry;

    Seat(String seatId, int row, int col, SeatTier tier) {
        this.seatId = Objects.requireNonNull(seatId);
        this.row    = row;
        this.col    = col;
        this.tier   = Objects.requireNonNull(tier);
    }

    synchronized boolean tryLock(String bookingId, Duration lockDuration) {
        releaseLockIfExpired();
        if (status != SeatStatus.AVAILABLE) return false;
        status             = SeatStatus.LOCKED;
        lockedByBookingId  = bookingId;
        lockExpiry         = Instant.now().plus(lockDuration);
        return true;
    }

    synchronized boolean confirm(String bookingId) {
        if (status == SeatStatus.LOCKED && bookingId.equals(lockedByBookingId)) {
            status = SeatStatus.BOOKED;
            return true;
        }
        return false;
    }

    synchronized void release(String bookingId) {
        if (bookingId.equals(lockedByBookingId)) {
            status            = SeatStatus.AVAILABLE;
            lockedByBookingId = null;
            lockExpiry        = null;
        }
    }

    synchronized void releaseLockIfExpired() {
        if (status == SeatStatus.LOCKED && lockExpiry != null
                && Instant.now().isAfter(lockExpiry)) {
            status            = SeatStatus.AVAILABLE;
            lockedByBookingId = null;
            lockExpiry        = null;
        }
    }

    synchronized boolean isAvailable() {
        releaseLockIfExpired();
        return status == SeatStatus.AVAILABLE;
    }

    public String    getSeatId() { return seatId; }
    public int       getRow()    { return row; }
    public int       getCol()    { return col; }
    public SeatTier  getTier()   { return tier; }
    public SeatStatus getStatus(){ return status; }

    @Override public String toString() {
        return String.format("Seat[%s R%dC%d %s %s]", seatId, row, col, tier, status);
    }
}

// ── Screen / Auditorium ───────────────────────────────────────────────────────

class Screen {
    private final String       screenId;
    private final String       name;
    private final int          totalRows;
    private final int          totalCols;
    private final List<Seat>   seats;

    Screen(String screenId, String name, int rows, int cols,
           Map<Integer, SeatTier> rowTierMap) {
        this.screenId  = screenId;
        this.name      = name;
        this.totalRows = rows;
        this.totalCols = cols;
        List<Seat> s   = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            SeatTier tier = rowTierMap.getOrDefault(r, SeatTier.NORMAL);
            for (int c = 0; c < cols; c++) {
                s.add(new Seat(screenId + "-R" + r + "C" + c, r, c, tier));
            }
        }
        this.seats = Collections.unmodifiableList(s);
    }

    Optional<Seat> findById(String seatId) {
        return seats.stream().filter(s -> s.getSeatId().equals(seatId)).findFirst();
    }

    List<Seat> getAvailableSeats() {
        return seats.stream().filter(Seat::isAvailable).collect(Collectors.toList());
    }

    List<Seat> getAvailableByTier(SeatTier tier) {
        return seats.stream()
            .filter(Seat::isAvailable)
            .filter(s -> s.getTier() == tier)
            .collect(Collectors.toList());
    }

    public String      getScreenId()   { return screenId; }
    public String      getName()       { return name; }
    public List<Seat>  getSeats()      { return seats; }
    public int         getTotalSeats() { return seats.size(); }

    long availableCount() { return seats.stream().filter(Seat::isAvailable).count(); }
}

// ── Pricing Engine ────────────────────────────────────────────────────────────

class PricingEngine {
    private static final Map<SeatTier, Double> TIER_MULTIPLIER = Map.of(
        SeatTier.RECLINER,  2.5,
        SeatTier.PREMIUM,   1.8,
        SeatTier.EXECUTIVE, 1.3,
        SeatTier.NORMAL,    1.0
    );
    private static final double PEAK_SURCHARGE = 1.2;   // 20% extra on weekends / prime time
    private static final double BASE_PRICE     = 150.0; // ₹150 base

    Money calculatePrice(Seat seat, LocalDateTime showTime) {
        double multiplier = TIER_MULTIPLIER.getOrDefault(seat.getTier(), 1.0);
        boolean isPeak    = isPeakTime(showTime);
        double amount     = BASE_PRICE * multiplier * (isPeak ? PEAK_SURCHARGE : 1.0);
        return Money.ofRupees(amount);
    }

    Money calculateTotal(List<Seat> seats, LocalDateTime showTime) {
        return seats.stream()
            .map(s -> calculatePrice(s, showTime))
            .reduce(new Money(0), Money::add);
    }

    private boolean isPeakTime(LocalDateTime dt) {
        DayOfWeek day  = dt.getDayOfWeek();
        int       hour = dt.getHour();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
            || (hour >= 18 && hour <= 22);
    }
}

// ── Show ──────────────────────────────────────────────────────────────────────

class Show {
    private static int counter = 5000;

    private final String        showId;
    private final String        movieName;
    private final String        language;
    private final Screen        screen;
    private final LocalDateTime showTime;
    private final Duration      duration;

    Show(String movieName, String language, Screen screen,
         LocalDateTime showTime, Duration duration) {
        this.showId    = "SHW-" + counter++;
        this.movieName = Objects.requireNonNull(movieName);
        this.language  = Objects.requireNonNull(language);
        this.screen    = Objects.requireNonNull(screen);
        this.showTime  = Objects.requireNonNull(showTime);
        this.duration  = Objects.requireNonNull(duration);
        if (movieName.isBlank()) throw new IllegalArgumentException("Movie name required");
        if (showTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Show time cannot be in the past");
    }

    public String        getShowId()   { return showId; }
    public String        getMovieName(){ return movieName; }
    public String        getLanguage() { return language; }
    public Screen        getScreen()   { return screen; }
    public LocalDateTime getShowTime() { return showTime; }
    public long          availableSeats(){ return screen.availableCount(); }

    @Override public String toString() {
        return String.format("Show[%s '%s' %s %s | %d seats available]",
            showId, movieName, language,
            showTime.toString().replace("T", " "), availableSeats());
    }
}

// ── Seat Selection Strategy ───────────────────────────────────────────────────

interface SeatSelectionStrategy {
    List<Seat> select(Screen screen, int count, Map<String, Object> params);
}

class BestAvailableStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        // Prefer mid-rows, then by tier (PREMIUM first if available)
        List<Seat> available = screen.getAvailableSeats();
        int        midRow    = screen.getSeats().stream()
            .mapToInt(Seat::getRow).max().orElse(0) / 2;
        return available.stream()
            .sorted(Comparator.comparingInt(s -> Math.abs(s.getRow() - midRow)))
            .limit(count)
            .collect(Collectors.toList());
    }
}

class SpecificTierStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        SeatTier tier = (SeatTier) params.getOrDefault("tier", SeatTier.NORMAL);
        List<Seat> available = screen.getAvailableByTier(tier);
        if (available.size() < count)
            throw new BookingException("Not enough " + tier + " seats. Available: " + available.size());
        return available.stream().limit(count).collect(Collectors.toList());
    }
}

class GroupTogetherStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        // Find 'count' consecutive seats in the same row
        Map<Integer, List<Seat>> byRow = screen.getAvailableSeats().stream()
            .collect(Collectors.groupingBy(Seat::getRow));

        for (var entry : byRow.entrySet()) {
            List<Seat> rowSeats = entry.getValue().stream()
                .sorted(Comparator.comparingInt(Seat::getCol))
                .collect(Collectors.toList());
            List<Seat> consecutive = findConsecutive(rowSeats, count);
            if (consecutive != null) return consecutive;
        }
        throw new BookingException("No " + count + " consecutive seats available in any row");
    }

    private List<Seat> findConsecutive(List<Seat> rowSeats, int count) {
        for (int i = 0; i <= rowSeats.size() - count; i++) {
            List<Seat> candidate = rowSeats.subList(i, i + count);
            boolean consecutive  = true;
            for (int j = 1; j < candidate.size(); j++) {
                if (candidate.get(j).getCol() != candidate.get(j-1).getCol() + 1) {
                    consecutive = false; break;
                }
            }
            if (consecutive) return new ArrayList<>(candidate);
        }
        return null;
    }
}

// ── Booking ───────────────────────────────────────────────────────────────────

class Booking {
    private static int counter = 10000;

    private final String        bookingId;
    private final String        userId;
    private final Show          show;
    private final List<Seat>    seats;
    private final Money         totalAmount;
    private BookingStatus       status;
    private PaymentStatus       paymentStatus;
    private final Instant       lockExpiry;
    private Instant             confirmedAt;

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    Booking(String userId, Show show, List<Seat> seats, Money totalAmount) {
        this.bookingId     = "BKG-" + counter++;
        this.userId        = Objects.requireNonNull(userId);
        this.show          = Objects.requireNonNull(show);
        this.seats         = Collections.unmodifiableList(new ArrayList<>(seats));
        this.totalAmount   = Objects.requireNonNull(totalAmount);
        this.status        = BookingStatus.LOCKED;
        this.paymentStatus = PaymentStatus.PENDING;
        this.lockExpiry    = Instant.now().plus(LOCK_TTL);
    }

    boolean isExpired() { return Instant.now().isAfter(lockExpiry); }

    void confirm() {
        if (isExpired()) throw new BookingExpiredException(bookingId);
        if (status != BookingStatus.LOCKED)
            throw new BookingException("Booking not in LOCKED state: " + status);
        status        = BookingStatus.CONFIRMED;
        paymentStatus = PaymentStatus.SUCCESS;
        confirmedAt   = Instant.now();
    }

    void cancel() {
        if (status == BookingStatus.CANCELLED)
            throw new BookingException("Already cancelled: " + bookingId);
        status        = BookingStatus.CANCELLED;
        paymentStatus = status == BookingStatus.CONFIRMED
            ? PaymentStatus.REFUNDED : PaymentStatus.FAILED;
    }

    public String        getBookingId()     { return bookingId; }
    public String        getUserId()        { return userId; }
    public Show          getShow()          { return show; }
    public List<Seat>    getSeats()         { return seats; }
    public Money         getTotalAmount()   { return totalAmount; }
    public BookingStatus getStatus()        { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant       getLockExpiry()    { return lockExpiry; }

    @Override public String toString() {
        String seatIds = seats.stream().map(Seat::getSeatId).collect(Collectors.joining(", "));
        return String.format("Booking[%s User=%s Show=%s Seats=[%s] %s %s %s]",
            bookingId, userId, show.getShowId(), seatIds,
            totalAmount, status, paymentStatus);
    }
}

// ── Booking Service ───────────────────────────────────────────────────────────

class BookingService {
    private final Map<String, Show>    shows    = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final PricingEngine        pricing  = new PricingEngine();

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    // ── Show Management ───────────────────────────────────────────────────────

    public void addShow(Show show) {
        shows.put(show.getShowId(), show);
        System.out.println("🎬 Show added: " + show);
    }

    public List<Show> searchShows(String movieName, LocalDate date) {
        return shows.values().stream()
            .filter(s -> s.getMovieName().equalsIgnoreCase(movieName))
            .filter(s -> s.getShowTime().toLocalDate().equals(date))
            .sorted(Comparator.comparing(Show::getShowTime))
            .collect(Collectors.toList());
    }

    // ── Booking Flow ──────────────────────────────────────────────────────────

    /**
     * Step 1: Lock seats and create a pending booking.
     * User has LOCK_TTL minutes to complete payment.
     */
    public Booking lockSeats(String userId, String showId,
                             int seatCount, SeatSelectionStrategy strategy,
                             Map<String, Object> params) {
        Show show = getShow(showId);

        // Select seats using strategy
        List<Seat> selected = strategy.select(show.getScreen(), seatCount, params);
        if (selected.size() < seatCount)
            throw new BookingException("Not enough seats available");

        // Atomically lock all selected seats
        String tentativeId = "TMP-" + UUID.randomUUID();
        List<Seat> locked  = new ArrayList<>();
        try {
            for (Seat seat : selected) {
                if (!seat.tryLock(tentativeId, LOCK_TTL))
                    throw new SeatNotAvailableException(seat.getSeatId());
                locked.add(seat);
            }
        } catch (SeatNotAvailableException e) {
            // Rollback: release already-locked seats
            locked.forEach(s -> s.release(tentativeId));
            throw e;
        }

        Money total   = pricing.calculateTotal(locked, show.getShowTime());
        Booking booking = new Booking(userId, show, locked, total);

        // Re-lock with actual booking ID
        locked.forEach(s -> {
            s.release(tentativeId);
            s.tryLock(booking.getBookingId(), LOCK_TTL);
        });

        bookings.put(booking.getBookingId(), booking);
        System.out.printf("🔒 Locked: %s | %s | Pay within 5 min%n",
            booking.getBookingId(), total);
        return booking;
    }

    /**
     * Step 2: Confirm booking after payment.
     */
    public Booking confirmBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        if (booking.isExpired()) {
            releaseSeats(booking);
            booking.cancel();
            throw new BookingExpiredException(bookingId);
        }
        booking.getSeats().forEach(s -> s.confirm(bookingId));
        booking.confirm();
        System.out.println("✅ Confirmed: " + booking);
        return booking;
    }

    /**
     * Cancel a booking — releases seats and initiates refund if already paid.
     */
    public void cancelBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        releaseSeats(booking);
        booking.cancel();
        System.out.println("❌ Cancelled: " + bookingId
            + (booking.getPaymentStatus() == PaymentStatus.REFUNDED ? " | Refund initiated" : ""));
    }

    public void displayShowAvailability(String showId) {
        Show show = getShow(showId);
        System.out.printf("%n═══ %s — %s ═══%n", show.getMovieName(), show.getShowTime());
        System.out.printf("Available seats: %d / %d%n",
            show.getScreen().availableCount(), show.getScreen().getTotalSeats());
        for (SeatTier tier : SeatTier.values()) {
            long count = show.getScreen().getAvailableByTier(tier).size();
            if (count > 0)
                System.out.printf("  %-12s: %2d seats | Price: %s%n",
                    tier, count, pricing.calculatePrice(
                        show.getScreen().getAvailableByTier(tier).get(0),
                        show.getShowTime()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void releaseSeats(Booking booking) {
        booking.getSeats().forEach(s -> s.release(booking.getBookingId()));
    }

    private Show getShow(String showId) {
        Show show = shows.get(showId);
        if (show == null) throw new ShowNotFoundException(showId);
        return show;
    }

    private Booking getBooking(String id) {
        Booking b = bookings.get(id);
        if (b == null) throw new BookingException("Booking not found: " + id);
        return b;
    }

    public Map<String, Booking> getAllBookings() { return Collections.unmodifiableMap(bookings); }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class BookMyShowDemo {
    public static void main(String[] args) {
        BookingService service = new BookingService();

        // Set up screen with tier mapping
        Map<Integer, SeatTier> rowTiers = new HashMap<>();
        rowTiers.put(0, SeatTier.RECLINER);    // row 0: recliners
        rowTiers.put(1, SeatTier.PREMIUM);     // row 1: premium
        rowTiers.put(2, SeatTier.PREMIUM);     // row 2: premium
        rowTiers.put(3, SeatTier.EXECUTIVE);   // row 3: executive
        // rows 4-9: NORMAL (default)

        Screen pvr = new Screen("SCR-1", "PVR Audi 1", 10, 15, rowTiers);

        // Add shows
        LocalDateTime showTime = LocalDateTime.now()
            .plusDays(1).withHour(18).withMinute(30).withSecond(0).withNano(0);
        Show show1 = new Show("Kalki 2898 AD", "Telugu", pvr, showTime, Duration.ofMinutes(180));
        service.addShow(show1);

        // Search shows
        System.out.println("\n=== Search Shows ===");
        service.searchShows("Kalki 2898 AD", showTime.toLocalDate())
               .forEach(System.out::println);

        // Display availability
        service.displayShowAvailability(show1.getShowId());

        // Booking 1: Best available — 2 seats
        System.out.println("\n=== Booking 1: Best Available (2 seats) ===");
        Booking b1 = service.lockSeats("USR-001", show1.getShowId(), 2,
            new BestAvailableStrategy(), Collections.emptyMap());
        service.confirmBooking(b1.getBookingId());

        // Booking 2: Group together — 4 consecutive seats
        System.out.println("\n=== Booking 2: Group Together (4 consecutive seats) ===");
        Booking b2 = service.lockSeats("USR-002", show1.getShowId(), 4,
            new GroupTogetherStrategy(), Collections.emptyMap());
        service.confirmBooking(b2.getBookingId());

        // Booking 3: Specific tier — Recliner
        System.out.println("\n=== Booking 3: Recliner Seats ===");
        Booking b3 = service.lockSeats("USR-003", show1.getShowId(), 2,
            new SpecificTierStrategy(), Map.of("tier", SeatTier.RECLINER));
        // Cancel before payment
        service.cancelBooking(b3.getBookingId());

        // Try to book already-taken seats
        System.out.println("\n=== Booking 4: Recliner again after cancellation ===");
        Booking b4 = service.lockSeats("USR-004", show1.getShowId(), 2,
            new SpecificTierStrategy(), Map.of("tier", SeatTier.RECLINER));
        service.confirmBooking(b4.getBookingId());

        // Cancel confirmed booking (refund)
        System.out.println("\n=== Cancel Confirmed Booking ===");
        service.cancelBooking(b2.getBookingId());

        service.displayShowAvailability(show1.getShowId());

        // Error: expired booking
        System.out.println("\n=== Error Cases ===");
        try {
            service.confirmBooking("BKG-INVALID");
        } catch (BookingException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }
}
```

## Extension Q&A

**Q: How do you handle two users trying to book the same seat simultaneously?**
`seat.tryLock()` is `synchronized` — only one thread can acquire the lock. The second thread finds status `LOCKED` and gets `false` → throws `SeatNotAvailableException`. The rollback in `lockSeats()` releases all seats already locked by that request. This is the classic test-and-set pattern.

**Q: How do you implement waitlist when show is sold out?**
Add a `WaitlistEntry(userId, showId, seatCount, requestTime)` queue per show. When a booking is cancelled, `releaseSeats()` checks the waitlist, notifies the first user via `NotificationService`, and auto-locks the seats for them with a shorter TTL (2 min to act fast).

**Q: How do you scale seat locking across multiple servers?**
Replace in-memory `synchronized` with a distributed lock (Redis `SET seat:{seatId} {bookingId} NX EX 300`). The 300s TTL handles auto-expiry without a background thread. All servers share the same Redis lock. On confirmation, `DEL seat:{seatId}` and mark booked in DB atomically using Lua script.
