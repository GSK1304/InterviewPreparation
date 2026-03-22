package lld.hotel.repository;
import lld.hotel.entity.HotelReservation;
import lld.hotel.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface ReservationRepository extends JpaRepository<HotelReservation, String> {
    List<HotelReservation> findByGuestIdOrderByCreatedAtDesc(Long guestId);
    @Query("SELECT r FROM HotelReservation r WHERE r.room.roomNumber = :room AND r.status NOT IN ('CANCELLED','CHECKED_OUT') AND r.checkIn < :out AND r.checkOut > :in")
    List<HotelReservation> findConflicting(@Param("room") String room, @Param("in") LocalDate checkIn, @Param("out") LocalDate checkOut);
    @Query("SELECT SUM(r.totalPaise) FROM HotelReservation r WHERE r.status = 'CHECKED_OUT'")
    Long totalRevenue();
}
