package lld.hotel;
import java.time.*;
import java.util.Objects;

public class Reservation {
    private static int counter = 20000;
    private final String           reservationId;
    private final Guest            guest;
    private final BaseRoom         room;
    private final RoomService      roomWithAmenities;
    private final DateRange        dates;
    private final int              guestCount;
    private final Money            totalAmount;
    private ReservationStatus      status;
    private Instant                checkedInAt, checkedOutAt;

    public Reservation(Guest guest, BaseRoom room, RoomService roomWithAmenities,
                       DateRange dates, int guestCount, Money totalAmount) {
        this.reservationId     = "RES-" + counter++;
        this.guest             = Objects.requireNonNull(guest);
        this.room              = Objects.requireNonNull(room);
        this.roomWithAmenities = Objects.requireNonNull(roomWithAmenities);
        this.dates             = Objects.requireNonNull(dates);
        this.guestCount        = guestCount;
        this.totalAmount       = Objects.requireNonNull(totalAmount);
        this.status            = ReservationStatus.CONFIRMED;
        if (guestCount < 1) throw new IllegalArgumentException("At least 1 guest required");
        if (guestCount > room.getCapacity()) throw new IllegalArgumentException("Exceeds capacity: " + room.getCapacity());
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED)
            throw new InvalidCheckInException("Not CONFIRMED: " + status);
        if (LocalDate.now().isBefore(dates.checkIn()))
            throw new InvalidCheckInException("Check-in date is " + dates.checkIn());
        status = ReservationStatus.CHECKED_IN; checkedInAt = Instant.now();
    }

    public Money checkOut() {
        if (status != ReservationStatus.CHECKED_IN) throw new HotelException("Not checked in: " + status);
        status = ReservationStatus.CHECKED_OUT; checkedOutAt = Instant.now();
        return totalAmount;
    }

    public void cancel(String reason) {
        if (status == ReservationStatus.CHECKED_IN)  throw new HotelException("Cannot cancel: guest checked in");
        if (status == ReservationStatus.CHECKED_OUT  || status == ReservationStatus.CANCELLED)
            throw new HotelException("Cannot cancel from: " + status);
        status = ReservationStatus.CANCELLED;
    }

    public boolean overlaps(DateRange range) {
        return status != ReservationStatus.CANCELLED && status != ReservationStatus.CHECKED_OUT && dates.overlaps(range);
    }

    public String            getReservationId()  { return reservationId; }
    public Guest             getGuest()          { return guest; }
    public BaseRoom          getRoom()           { return room; }
    public RoomService       getRoomService()    { return roomWithAmenities; }
    public DateRange         getDates()          { return dates; }
    public Money             getTotalAmount()    { return totalAmount; }
    public ReservationStatus getStatus()         { return status; }

    @Override public String toString() {
        return String.format("Reservation[%s Guest=%s Room=%s %s->%s %s %s]",
            reservationId, guest.name(), room.getRoomId(), dates.checkIn(), dates.checkOut(), totalAmount, status);
    }
}
