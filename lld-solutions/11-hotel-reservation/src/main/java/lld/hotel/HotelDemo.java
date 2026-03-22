package lld.hotel;
import java.time.LocalDate;
import java.util.*;

public class HotelDemo {
    public static void main(String[] args) {
        Hotel hotel = new Hotel("The Grand Hyderabad", "Hyderabad",
            new EarlyBirdPricingStrategy(new StandardPricingStrategy(), 30, 0.15));

        hotel.addRoom(new BaseRoom("101", RoomType.STANDARD,     BedType.DOUBLE, 2, 1, Money.ofRupees(3000)));
        hotel.addRoom(new BaseRoom("102", RoomType.STANDARD,     BedType.TWIN,   2, 1, Money.ofRupees(3000)));
        hotel.addRoom(new BaseRoom("201", RoomType.DELUXE,       BedType.QUEEN,  2, 2, Money.ofRupees(5000)));
        hotel.addRoom(new BaseRoom("202", RoomType.DELUXE,       BedType.KING,   2, 2, Money.ofRupees(5500)));
        hotel.addRoom(new BaseRoom("301", RoomType.SUITE,        BedType.KING,   4, 3, Money.ofRupees(10000)));

        Guest alice = new Guest("G1","Alice Kumar","alice@example.com","9876543210","AADHAAR-1234");
        Guest bob   = new Guest("G2","Bob Sharma",  "bob@example.com",  "9876543211","PASSPORT-5678");
        Guest carol = new Guest("G3","Carol Nair",  "carol@example.com","9876543212","DL-9012");

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        System.out.println("=== Standard Room ===");
        Reservation r1 = hotel.book(alice, "101", tomorrow, tomorrow.plusDays(3), 2, Collections.emptyList());

        System.out.println("\n=== Deluxe + Breakfast + Parking ===");
        BaseRoom room201 = new BaseRoom("201", RoomType.DELUXE, BedType.QUEEN, 2, 2, Money.ofRupees(5000));
        RoomService withAmenities = new ParkingIncluded(new BreakfastIncluded(room201));
        // Book with amenities pre-applied in service
        Reservation r2 = hotel.book(bob, "201", tomorrow, tomorrow.plusDays(2), 2,
            List.of(new BreakfastIncluded(new BaseRoom("201",RoomType.DELUXE,BedType.QUEEN,2,2,Money.ofRupees(5000))),
                    new ParkingIncluded(new BaseRoom("201",RoomType.DELUXE,BedType.QUEEN,2,2,Money.ofRupees(5000)))));

        System.out.println("\n=== Search Available ===");
        RoomSearchCriteria criteria = new RoomSearchCriteria(
            tomorrow, tomorrow.plusDays(3), 2, RoomType.DELUXE, null, Money.ofRupees(6000));
        hotel.searchAvailableRooms(criteria).forEach(r -> System.out.println("  " + r.getDescription()));

        System.out.println("\n=== Double Booking Attempt ===");
        try { hotel.book(carol, "101", tomorrow.plusDays(1), tomorrow.plusDays(2), 2, Collections.emptyList()); }
        catch (RoomNotAvailableException e) { System.out.println("Error: " + e.getMessage()); }

        System.out.println("\n=== Check-in / Check-out ===");
        Reservation sameDay = hotel.book(alice, "102", today, today.plusDays(2), 1, Collections.emptyList());
        hotel.checkIn(sameDay.getReservationId());
        hotel.checkOut(sameDay.getReservationId());

        System.out.println("\n=== Cancellation ===");
        hotel.cancelReservation(r2.getReservationId(), "Change of plans");

        hotel.displayOccupancy(today);
        hotel.displayRevenue();
    }
}
