package lld.bookmyshow.repository;
import lld.bookmyshow.entity.Seat;
import lld.bookmyshow.enums.SeatStatus;
import lld.bookmyshow.enums.SeatTier;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByShowIdAndStatusOrderByRowNumberAscColNumberAsc(Long showId, SeatStatus status);
    List<Seat> findByShowIdAndTierAndStatusOrderByRowNumberAscColNumberAsc(Long showId, SeatTier tier, SeatStatus status);
    long countByShowIdAndStatus(Long showId, SeatStatus status);
    @Modifying @Query("UPDATE Seat s SET s.status = :status, s.lockedByBookingId = :bookingId, s.lockExpiry = :expiry WHERE s.id = :id AND s.status = 'AVAILABLE'")
    int tryLock(@Param("id") Long id, @Param("bookingId") String bookingId, @Param("expiry") Instant expiry, @Param("status") SeatStatus status);
    @Modifying @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedByBookingId = null, s.lockExpiry = null WHERE s.lockedByBookingId = :bookingId")
    int releaseByBookingId(@Param("bookingId") String bookingId);
    @Modifying @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedByBookingId = null, s.lockExpiry = null WHERE s.status = 'LOCKED' AND s.lockExpiry < :now")
    int releaseExpiredLocks(@Param("now") Instant now);
    @Modifying @Query("UPDATE Seat s SET s.status = 'BOOKED' WHERE s.lockedByBookingId = :bookingId AND s.status = 'LOCKED'")
    int confirmByBookingId(@Param("bookingId") String bookingId);
}
