package lld.bookmyshow;
import java.time.*;
import java.util.*;

public class BookMyShowDemo {
    public static void main(String[] args) {
        BookingService service = new BookingService();
        Map<Integer, SeatTier> rowTiers = new HashMap<>();
        rowTiers.put(0, SeatTier.RECLINER); rowTiers.put(1, SeatTier.PREMIUM); rowTiers.put(2, SeatTier.PREMIUM); rowTiers.put(3, SeatTier.EXECUTIVE);
        Screen pvr = new Screen("SCR-1", "PVR Audi 1", 10, 15, rowTiers);
        LocalDateTime showTime = LocalDateTime.now().plusDays(1).withHour(18).withMinute(30).withSecond(0).withNano(0);
        Show show1 = new Show("Kalki 2898 AD", "Telugu", pvr, showTime);
        service.addShow(show1);

        System.out.println("\n=== Search ===");
        service.searchShows("Kalki 2898 AD", showTime.toLocalDate()).forEach(System.out::println);
        service.displayAvailability(show1.getShowId());

        System.out.println("\n=== Booking 1: Best Available ===");
        Booking b1 = service.lockSeats("USR-001", show1.getShowId(), 2, new BestAvailableStrategy(), Collections.emptyMap());
        service.confirmBooking(b1.getBookingId());

        System.out.println("\n=== Booking 2: Group Together ===");
        Booking b2 = service.lockSeats("USR-002", show1.getShowId(), 4, new GroupTogetherStrategy(), Collections.emptyMap());
        service.confirmBooking(b2.getBookingId());

        System.out.println("\n=== Booking 3: Recliner -> Cancel ===");
        Booking b3 = service.lockSeats("USR-003", show1.getShowId(), 2, new SpecificTierStrategy(), Map.of("tier", SeatTier.RECLINER));
        service.cancelBooking(b3.getBookingId());

        System.out.println("\n=== Booking 4: Recliner after cancellation ===");
        Booking b4 = service.lockSeats("USR-004", show1.getShowId(), 2, new SpecificTierStrategy(), Map.of("tier", SeatTier.RECLINER));
        service.confirmBooking(b4.getBookingId());

        System.out.println("\n=== Error: Invalid booking ===");
        try { service.confirmBooking("BKG-INVALID"); } catch (BookingException e) { System.out.println("Error: " + e.getMessage()); }

        service.displayAvailability(show1.getShowId());
    }
}
