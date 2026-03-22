# LLD — Hotel Reservation System — Complete Java 21

## Design Summary
| Aspect | Decision |
|--------|----------|
| Room availability | Date-range overlap check on confirmed reservations |
| Pricing | **Strategy** — WeekdayRate, WeekendRate, SeasonalRate, EarlyBirdRate |
| Reservation state | **State** — PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT / CANCELLED |
| Amenities | **Decorator** — base room + optional amenities (breakfast, parking, spa) |
| Search | `RoomSearchCriteria` record filters by type, capacity, date, price range |
| Thread safety | `synchronized` on reservation creation to prevent double booking |

## Complete Solution

```java
package lld.hotel;

import java.time.*;
import java.util.*;
import java.util.stream.*;

// ── Enums ─────────────────────────────────────────────────────────────────────

enum RoomType     { STANDARD, DELUXE, SUITE, PRESIDENTIAL }
enum BedType      { SINGLE, DOUBLE, QUEEN, KING, TWIN }
enum ReservationStatus { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW }

// ── Money ─────────────────────────────────────────────────────────────────────

record Money(long paise) {
    static final Money ZERO = new Money(0);
    static Money ofRupees(double r)   { return new Money(Math.round(r * 100)); }
    Money add(Money o)                { return new Money(paise + o.paise); }
    Money multiply(double factor)     { return new Money(Math.round(paise * factor)); }
    Money multiply(long nights)       { return new Money(paise * nights); }
    double toRupees()                 { return paise / 100.0; }
    boolean isGreaterThan(Money o)    { return paise > o.paise; }
    @Override public String toString(){ return String.format("₹%.2f", toRupees()); }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class HotelException              extends RuntimeException { HotelException(String m) { super(m); } }
class RoomNotAvailableException   extends HotelException   {
    RoomNotAvailableException(String roomId, LocalDate from, LocalDate to) {
        super("Room " + roomId + " not available from " + from + " to " + to); }
}
class ReservationNotFoundException extends HotelException {
    ReservationNotFoundException(String id) { super("Reservation not found: " + id); }
}
class InvalidCheckInException     extends HotelException {
    InvalidCheckInException(String msg) { super("Invalid check-in: " + msg); }
}

// ── Guest ─────────────────────────────────────────────────────────────────────

record Guest(String guestId, String name, String email, String phone, String idProof) {
    Guest {
        Objects.requireNonNull(guestId,  "Guest ID required");
        Objects.requireNonNull(name,     "Name required");
        Objects.requireNonNull(email,    "Email required");
        Objects.requireNonNull(phone,    "Phone required");
        Objects.requireNonNull(idProof,  "ID proof required");
        if (name.isBlank())    throw new IllegalArgumentException("Name cannot be blank");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
        if (phone.length() < 10)  throw new IllegalArgumentException("Invalid phone: " + phone);
    }
}

// ── Room Amenity (Decorator Pattern) ─────────────────────────────────────────

interface RoomService {
    String getDescription();
    Money getDailyRate();
}

class BaseRoom implements RoomService {
    private final String  roomId;
    private final RoomType type;
    private final BedType  bedType;
    private final int      capacity;
    private final int      floorNumber;
    private final Money    baseRate;

    BaseRoom(String roomId, RoomType type, BedType bedType,
             int capacity, int floorNumber, Money baseRate) {
        this.roomId      = Objects.requireNonNull(roomId);
        this.type        = Objects.requireNonNull(type);
        this.bedType     = Objects.requireNonNull(bedType);
        this.capacity    = capacity;
        this.floorNumber = floorNumber;
        this.baseRate    = Objects.requireNonNull(baseRate);
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be at least 1");
        if (baseRate.paise() <= 0) throw new IllegalArgumentException("Base rate must be positive");
    }

    public String   getRoomId()     { return roomId; }
    public RoomType getType()       { return type; }
    public BedType  getBedType()    { return bedType; }
    public int      getCapacity()   { return capacity; }
    public int      getFloorNumber(){ return floorNumber; }

    @Override public String getDescription() {
        return String.format("%s Room %s | %s bed | Floor %d | Cap: %d",
            type, roomId, bedType, floorNumber, capacity);
    }
    @Override public Money getDailyRate() { return baseRate; }
}

abstract class RoomAmenityDecorator implements RoomService {
    protected final RoomService wrapped;
    RoomAmenityDecorator(RoomService wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }
}

class BreakfastIncluded extends RoomAmenityDecorator {
    private static final Money DAILY_COST = Money.ofRupees(600);
    BreakfastIncluded(RoomService r) { super(r); }

    @Override public String getDescription() { return wrapped.getDescription() + " + Breakfast"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(DAILY_COST); }
}

class ParkingIncluded extends RoomAmenityDecorator {
    private static final Money DAILY_COST = Money.ofRupees(200);
    ParkingIncluded(RoomService r) { super(r); }

    @Override public String getDescription() { return wrapped.getDescription() + " + Parking"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(DAILY_COST); }
}

class SpaAccess extends RoomAmenityDecorator {
    private static final Money DAILY_COST = Money.ofRupees(1500);
    SpaAccess(RoomService r) { super(r); }

    @Override public String getDescription() { return wrapped.getDescription() + " + Spa"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(DAILY_COST); }
}

// ── Pricing Strategy ──────────────────────────────────────────────────────────

interface PricingStrategy {
    Money calculate(Money baseRate, LocalDate checkIn, LocalDate checkOut);
}

class StandardPricingStrategy implements PricingStrategy {
    @Override
    public Money calculate(Money baseRate, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) throw new IllegalArgumentException("Check-out must be after check-in");
        double total = 0;
        LocalDate date = checkIn;
        while (!date.equals(checkOut)) {
            DayOfWeek dow = date.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY);
            total += baseRate.toRupees() * (isWeekend ? 1.3 : 1.0);  // 30% weekend surcharge
            date = date.plusDays(1);
        }
        return Money.ofRupees(total);
    }
}

class EarlyBirdPricingStrategy implements PricingStrategy {
    private final PricingStrategy base;
    private final int             daysInAdvance;
    private final double          discount;

    EarlyBirdPricingStrategy(PricingStrategy base, int daysInAdvance, double discount) {
        this.base          = Objects.requireNonNull(base);
        this.daysInAdvance = daysInAdvance;
        this.discount      = discount;
        if (discount <= 0 || discount >= 1)
            throw new IllegalArgumentException("Discount must be between 0 and 1");
    }

    @Override
    public Money calculate(Money baseRate, LocalDate checkIn, LocalDate checkOut) {
        Money standard = base.calculate(baseRate, checkIn, checkOut);
        long  daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), checkIn);
        if (daysUntil >= daysInAdvance) {
            return standard.multiply(1.0 - discount);
        }
        return standard;
    }
}

// ── Date Range ────────────────────────────────────────────────────────────────

record DateRange(LocalDate checkIn, LocalDate checkOut) {
    DateRange {
        Objects.requireNonNull(checkIn,  "Check-in date required");
        Objects.requireNonNull(checkOut, "Check-out date required");
        if (!checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
    }

    boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && checkOut.isAfter(other.checkIn);
    }

    long nights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
}

// ── Reservation ───────────────────────────────────────────────────────────────

class Reservation {
    private static int counter = 20000;

    private final String           reservationId;
    private final Guest            guest;
    private final BaseRoom         room;
    private final RoomService      roomWithAmenities;  // decorated room
    private final DateRange        dates;
    private final int              guestCount;
    private final Money            totalAmount;
    private ReservationStatus      status;
    private final Instant          createdAt;
    private Instant                checkedInAt;
    private Instant                checkedOutAt;
    private String                 cancellationReason;

    Reservation(Guest guest, BaseRoom room, RoomService roomWithAmenities,
                DateRange dates, int guestCount, Money totalAmount) {
        this.reservationId    = "RES-" + counter++;
        this.guest            = Objects.requireNonNull(guest);
        this.room             = Objects.requireNonNull(room);
        this.roomWithAmenities= Objects.requireNonNull(roomWithAmenities);
        this.dates            = Objects.requireNonNull(dates);
        this.guestCount       = guestCount;
        this.totalAmount      = Objects.requireNonNull(totalAmount);
        this.status           = ReservationStatus.CONFIRMED;
        this.createdAt        = Instant.now();

        if (guestCount < 1) throw new IllegalArgumentException("Must have at least 1 guest");
        if (guestCount > room.getCapacity())
            throw new IllegalArgumentException("Guest count exceeds room capacity: " + room.getCapacity());
    }

    // ── State Transitions ─────────────────────────────────────────────────────

    void checkIn() {
        if (status != ReservationStatus.CONFIRMED)
            throw new InvalidCheckInException("Reservation not confirmed: " + status);
        if (LocalDate.now().isBefore(dates.checkIn()))
            throw new InvalidCheckInException("Check-in date is " + dates.checkIn());
        status      = ReservationStatus.CHECKED_IN;
        checkedInAt = Instant.now();
    }

    Money checkOut() {
        if (status != ReservationStatus.CHECKED_IN)
            throw new HotelException("Cannot check out from status: " + status);
        status       = ReservationStatus.CHECKED_OUT;
        checkedOutAt = Instant.now();
        return totalAmount;
    }

    void cancel(String reason) {
        if (status == ReservationStatus.CHECKED_IN)
            throw new HotelException("Cannot cancel a reservation where guest is already checked in");
        if (status == ReservationStatus.CHECKED_OUT || status == ReservationStatus.CANCELLED)
            throw new HotelException("Cannot cancel from status: " + status);
        status             = ReservationStatus.CANCELLED;
        cancellationReason = reason;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String            getReservationId()    { return reservationId; }
    public Guest             getGuest()            { return guest; }
    public BaseRoom          getRoom()             { return room; }
    public DateRange         getDates()            { return dates; }
    public int               getGuestCount()       { return guestCount; }
    public Money             getTotalAmount()      { return totalAmount; }
    public ReservationStatus getStatus()           { return status; }

    public boolean overlaps(DateRange range) {
        return status != ReservationStatus.CANCELLED
            && status != ReservationStatus.CHECKED_OUT
            && dates.overlaps(range);
    }

    @Override public String toString() {
        return String.format("Reservation[%s Guest=%s Room=%s %s→%s %s %s]",
            reservationId, guest.name(), room.getRoomId(),
            dates.checkIn(), dates.checkOut(), totalAmount, status);
    }
}

// ── Room Search Criteria ──────────────────────────────────────────────────────

record RoomSearchCriteria(
    LocalDate      checkIn,
    LocalDate      checkOut,
    int            guestCount,
    RoomType       roomType,       // null = any
    BedType        bedType,        // null = any
    Money          maxPricePerNight,// null = no limit
    boolean        needsParking
) {
    RoomSearchCriteria {
        Objects.requireNonNull(checkIn,  "Check-in required");
        Objects.requireNonNull(checkOut, "Check-out required");
        if (!checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
        if (guestCount < 1) throw new IllegalArgumentException("At least 1 guest required");
    }
}

// ── Hotel ─────────────────────────────────────────────────────────────────────

class Hotel {
    private final String               name;
    private final String               city;
    private final Map<String, BaseRoom>      rooms        = new LinkedHashMap<>();
    private final Map<String, Reservation>   reservations = new LinkedHashMap<>();
    private final PricingStrategy      pricingStrategy;

    Hotel(String name, String city, PricingStrategy pricingStrategy) {
        this.name            = Objects.requireNonNull(name);
        this.city            = Objects.requireNonNull(city);
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy);
        if (name.isBlank()) throw new IllegalArgumentException("Hotel name required");
    }

    // ── Room Management ───────────────────────────────────────────────────────

    public void addRoom(BaseRoom room) {
        Objects.requireNonNull(room, "Room required");
        if (rooms.containsKey(room.getRoomId()))
            throw new HotelException("Room already exists: " + room.getRoomId());
        rooms.put(room.getRoomId(), room);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<BaseRoom> searchAvailableRooms(RoomSearchCriteria criteria) {
        DateRange requestedRange = new DateRange(criteria.checkIn(), criteria.checkOut());
        return rooms.values().stream()
            .filter(r -> criteria.roomType() == null || r.getType() == criteria.roomType())
            .filter(r -> criteria.bedType()  == null || r.getBedType() == criteria.bedType())
            .filter(r -> r.getCapacity() >= criteria.guestCount())
            .filter(r -> criteria.maxPricePerNight() == null
                || !r.getDailyRate().isGreaterThan(criteria.maxPricePerNight()))
            .filter(r -> isRoomAvailable(r.getRoomId(), requestedRange))
            .collect(Collectors.toList());
    }

    private boolean isRoomAvailable(String roomId, DateRange range) {
        return reservations.values().stream()
            .filter(res -> res.getRoom().getRoomId().equals(roomId))
            .noneMatch(res -> res.overlaps(range));
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    public synchronized Reservation book(Guest guest, String roomId,
                                         LocalDate checkIn, LocalDate checkOut,
                                         int guestCount,
                                         List<Class<? extends RoomAmenityDecorator>> amenities) {
        BaseRoom room = getRoom(roomId);
        DateRange range = new DateRange(checkIn, checkOut);

        if (!isRoomAvailable(roomId, range))
            throw new RoomNotAvailableException(roomId, checkIn, checkOut);

        // Apply amenity decorators
        RoomService roomService = applyAmenities(room, amenities);

        // Calculate total price
        Money totalAmount = pricingStrategy.calculate(
            roomService.getDailyRate(), checkIn, checkOut);

        Reservation reservation = new Reservation(
            guest, room, roomService, range, guestCount, totalAmount);
        reservations.put(reservation.getReservationId(), reservation);

        System.out.printf("✅ Booked: %s%n   %s%n   Total: %s (%d nights)%n",
            reservation.getReservationId(),
            roomService.getDescription(),
            totalAmount, range.nights());
        return reservation;
    }

    /** Shorthand: book without amenities */
    public Reservation book(Guest guest, String roomId,
                            LocalDate checkIn, LocalDate checkOut, int guestCount) {
        return book(guest, roomId, checkIn, checkOut, guestCount, Collections.emptyList());
    }

    // ── Check-in / Check-out ──────────────────────────────────────────────────

    public void checkIn(String reservationId) {
        Reservation res = getReservation(reservationId);
        res.checkIn();
        System.out.printf("🏨 Checked in: %s | Room: %s | Guest: %s%n",
            reservationId, res.getRoom().getRoomId(), res.getGuest().name());
    }

    public Money checkOut(String reservationId) {
        Reservation res = getReservation(reservationId);
        Money bill = res.checkOut();
        System.out.printf("💰 Checked out: %s | Bill: %s%n", reservationId, bill);
        return bill;
    }

    public void cancelReservation(String reservationId, String reason) {
        getReservation(reservationId).cancel(reason);
        System.out.printf("❌ Cancelled: %s | Reason: %s%n", reservationId, reason);
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public void displayOccupancy(LocalDate date) {
        System.out.printf("%n═══ %s Occupancy on %s ═══%n", name, date);
        rooms.values().forEach(room -> {
            boolean occupied = reservations.values().stream()
                .anyMatch(r -> r.getRoom().getRoomId().equals(room.getRoomId())
                    && r.getStatus() == ReservationStatus.CHECKED_IN
                    && !r.getDates().checkIn().isAfter(date)
                    && r.getDates().checkOut().isAfter(date));
            System.out.printf("  Room %s %-8s %-8s: %s%n",
                room.getRoomId(), room.getType(), room.getBedType(),
                occupied ? "🔴 OCCUPIED" : "🟢 AVAILABLE");
        });
    }

    public void displayRevenue() {
        Money total = reservations.values().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT)
            .map(Reservation::getTotalAmount)
            .reduce(Money.ZERO, Money::add);
        System.out.printf("%n═══ Revenue Report ═══%n  Completed stays: %d%n  Total revenue: %s%n",
            reservations.values().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT).count(),
            total);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private RoomService applyAmenities(BaseRoom room,
                                       List<Class<? extends RoomAmenityDecorator>> amenities) {
        RoomService service = room;
        for (Class<? extends RoomAmenityDecorator> amenityClass : amenities) {
            try {
                service = amenityClass.getConstructor(RoomService.class).newInstance(service);
            } catch (Exception e) {
                throw new HotelException("Failed to apply amenity: " + amenityClass.getSimpleName());
            }
        }
        return service;
    }

    private BaseRoom    getRoom(String id)       {
        BaseRoom r = rooms.get(id);
        if (r == null) throw new HotelException("Room not found: " + id);
        return r;
    }

    private Reservation getReservation(String id) {
        Reservation r = reservations.get(id);
        if (r == null) throw new ReservationNotFoundException(id);
        return r;
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class HotelDemo {
    public static void main(String[] args) {
        Hotel hotel = new Hotel("The Grand Hyderabad",
            "Hyderabad",
            new EarlyBirdPricingStrategy(
                new StandardPricingStrategy(), 30, 0.15));  // 15% off if 30+ days ahead

        // Add rooms
        hotel.addRoom(new BaseRoom("101", RoomType.STANDARD,    BedType.DOUBLE, 2, 1, Money.ofRupees(3000)));
        hotel.addRoom(new BaseRoom("102", RoomType.STANDARD,    BedType.TWIN,   2, 1, Money.ofRupees(3000)));
        hotel.addRoom(new BaseRoom("201", RoomType.DELUXE,      BedType.QUEEN,  2, 2, Money.ofRupees(5000)));
        hotel.addRoom(new BaseRoom("202", RoomType.DELUXE,      BedType.KING,   2, 2, Money.ofRupees(5500)));
        hotel.addRoom(new BaseRoom("301", RoomType.SUITE,       BedType.KING,   4, 3, Money.ofRupees(10000)));
        hotel.addRoom(new BaseRoom("401", RoomType.PRESIDENTIAL,BedType.KING,   4, 4, Money.ofRupees(25000)));

        // Guests
        Guest alice = new Guest("G1", "Alice Kumar",  "alice@example.com",  "9876543210", "AADHAAR-1234");
        Guest bob   = new Guest("G2", "Bob Sharma",   "bob@example.com",    "9876543211", "PASSPORT-5678");
        Guest carol = new Guest("G3", "Carol Nair",   "carol@example.com",  "9876543212", "DL-9012");

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        // ── Scenario 1: Simple booking ────────────────────────────────────────
        System.out.println("=== Scenario 1: Standard Room ===");
        Reservation r1 = hotel.book(alice, "101", tomorrow, tomorrow.plusDays(3), 2);

        // ── Scenario 2: Deluxe with amenities ────────────────────────────────
        System.out.println("\n=== Scenario 2: Deluxe + Breakfast + Parking ===");
        Reservation r2 = hotel.book(bob, "201", tomorrow, tomorrow.plusDays(2), 2,
            List.of(BreakfastIncluded.class, ParkingIncluded.class));

        // ── Scenario 3: Suite with full amenities ────────────────────────────
        System.out.println("\n=== Scenario 3: Suite with Spa ===");
        Reservation r3 = hotel.book(carol, "301", nextWeek, nextWeek.plusDays(4), 3,
            List.of(BreakfastIncluded.class, SpaAccess.class, ParkingIncluded.class));

        // ── Scenario 4: Search available rooms ───────────────────────────────
        System.out.println("\n=== Scenario 4: Search Available Rooms ===");
        RoomSearchCriteria criteria = new RoomSearchCriteria(
            tomorrow, tomorrow.plusDays(3),
            2, RoomType.DELUXE, null,
            Money.ofRupees(6000),
            false
        );
        List<BaseRoom> available = hotel.searchAvailableRooms(criteria);
        System.out.println("Available DELUXE rooms (≤₹6000/night, 2 guests):");
        available.forEach(r -> System.out.printf("  %s%n", r.getDescription()));

        // ── Scenario 5: Double booking attempt ───────────────────────────────
        System.out.println("\n=== Scenario 5: Double Booking Attempt ===");
        try {
            hotel.book(alice, "101", tomorrow.plusDays(1), tomorrow.plusDays(2), 2);
        } catch (RoomNotAvailableException e) {
            System.out.println("❌ " + e.getMessage());
        }

        // ── Scenario 6: Check-in / Check-out ─────────────────────────────────
        System.out.println("\n=== Scenario 6: Check-in & Check-out ===");
        // Simulate same-day check-in by using today's reservation
        Reservation sameDay = hotel.book(alice, "102", today, today.plusDays(2), 1);
        hotel.checkIn(sameDay.getReservationId());
        hotel.checkOut(sameDay.getReservationId());

        // ── Scenario 7: Cancellation ──────────────────────────────────────────
        System.out.println("\n=== Scenario 7: Cancellation ===");
        hotel.cancelReservation(r2.getReservationId(), "Change of travel plans");

        // Now room 201 should be available again
        List<BaseRoom> afterCancel = hotel.searchAvailableRooms(criteria);
        System.out.println("Available after cancellation: " +
            afterCancel.stream().map(BaseRoom::getRoomId).collect(Collectors.joining(", ")));

        // ── Reports ───────────────────────────────────────────────────────────
        hotel.displayOccupancy(today);
        hotel.displayRevenue();
    }
}
```

## Extension Q&A

**Q: How do you add dynamic pricing based on occupancy?**
Inject an `OccupancyPricingStrategy` that checks current occupancy rate. `> 80%` occupancy → 1.3x multiplier. Implement as a decorator on top of `StandardPricingStrategy`. The occupancy rate is computed from active reservations on the requested date range. This is the same Decorator pattern as room amenities, applied to pricing.

**Q: How do you handle partial-night check-out (early departure)?**
Store `actualCheckOut: LocalDate` separately from `plannedCheckOut`. On early checkout, recalculate the bill using `actualCheckOut` and apply an early-departure fee if within 24 hours of check-in. The `checkOut()` method returns the recalculated `Money` rather than the originally stored `totalAmount`.

**Q: How do you manage room cleaning status between reservations?**
Add `CleaningStatus` enum (CLEAN, DIRTY, IN_PROGRESS) to `BaseRoom`. On `checkOut()`, set room to DIRTY. A `HousekeepingService` receives events (Observer) and assigns cleaning tasks. Room can only be checked into when `CleaningStatus == CLEAN`. Housekeeping team updates status via a separate API.
