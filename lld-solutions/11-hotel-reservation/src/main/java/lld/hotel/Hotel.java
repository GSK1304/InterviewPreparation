package lld.hotel;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Hotel {
    private final String                 name, city;
    private final Map<String, BaseRoom>  rooms        = new LinkedHashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private final PricingStrategy        pricing;

    public Hotel(String name, String city, PricingStrategy pricing) {
        this.name    = Objects.requireNonNull(name); this.city   = Objects.requireNonNull(city);
        this.pricing = Objects.requireNonNull(pricing);
        if (name.isBlank()) throw new IllegalArgumentException("Hotel name required");
    }

    public void addRoom(BaseRoom room) {
        Objects.requireNonNull(room);
        if (rooms.containsKey(room.getRoomId())) throw new HotelException("Room already exists: " + room.getRoomId());
        rooms.put(room.getRoomId(), room);
    }

    public List<BaseRoom> searchAvailableRooms(RoomSearchCriteria c) {
        DateRange range = new DateRange(c.checkIn(), c.checkOut());
        return rooms.values().stream()
            .filter(r -> c.roomType() == null || r.getType() == c.roomType())
            .filter(r -> c.bedType()  == null || r.getBedType() == c.bedType())
            .filter(r -> r.getCapacity() >= c.guestCount())
            .filter(r -> c.maxPricePerNight() == null || !r.getDailyRate().isGreaterThan(c.maxPricePerNight()))
            .filter(r -> isAvailable(r.getRoomId(), range))
            .collect(Collectors.toList());
    }

    public synchronized Reservation book(Guest guest, String roomId,
                                          LocalDate checkIn, LocalDate checkOut,
                                          int guestCount, List<RoomService> amenityChain) {
        BaseRoom room  = getRoom(roomId);
        DateRange range = new DateRange(checkIn, checkOut);
        if (!isAvailable(roomId, range)) throw new RoomNotAvailableException(roomId, checkIn, checkOut);

        RoomService service = room;
        for (RoomService decorator : amenityChain) service = decorator;  // amenities pre-built by caller

        Money total = pricing.calculate(service.getDailyRate(), checkIn, checkOut);
        Reservation res = new Reservation(guest, room, service, range, guestCount, total);
        reservations.put(res.getReservationId(), res);
        System.out.printf("Booked: %s%n  %s%n  Total: %s (%d nights)%n",
            res.getReservationId(), service.getDescription(), total, range.nights());
        return res;
    }

    public void checkIn(String reservationId) {
        Reservation res = getReservation(reservationId);
        res.checkIn();
        System.out.printf("Checked in: %s | Room: %s | Guest: %s%n",
            reservationId, res.getRoom().getRoomId(), res.getGuest().name());
    }

    public Money checkOut(String reservationId) {
        Reservation res = getReservation(reservationId);
        Money bill = res.checkOut();
        System.out.printf("Checked out: %s | Bill: %s%n", reservationId, bill);
        return bill;
    }

    public void cancelReservation(String reservationId, String reason) {
        getReservation(reservationId).cancel(reason);
        System.out.printf("Cancelled: %s%n", reservationId);
    }

    public void displayOccupancy(LocalDate date) {
        System.out.printf("%n=== %s Occupancy on %s ===%n", name, date);
        rooms.values().forEach(r -> {
            boolean occ = reservations.values().stream()
                .anyMatch(res -> res.getRoom().getRoomId().equals(r.getRoomId())
                    && res.getStatus() == ReservationStatus.CHECKED_IN
                    && !res.getDates().checkIn().isAfter(date)
                    && res.getDates().checkOut().isAfter(date));
            System.out.printf("  Room %-4s %-12s %-8s: %s%n",
                r.getRoomId(), r.getType(), r.getBedType(), occ ? "OCCUPIED" : "AVAILABLE");
        });
    }

    public void displayRevenue() {
        Money total = reservations.values().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT)
            .map(Reservation::getTotalAmount).reduce(Money.ZERO, Money::add);
        System.out.printf("%n=== Revenue: %s ===%n", total);
    }

    private boolean isAvailable(String roomId, DateRange range) {
        return reservations.values().stream()
            .filter(r -> r.getRoom().getRoomId().equals(roomId))
            .noneMatch(r -> r.overlaps(range));
    }

    private BaseRoom     getRoom(String id)        { BaseRoom r = rooms.get(id); if (r==null) throw new HotelException("Room not found: " + id); return r; }
    private Reservation  getReservation(String id) { Reservation r = reservations.get(id); if (r==null) throw new ReservationNotFoundException(id); return r; }
}
