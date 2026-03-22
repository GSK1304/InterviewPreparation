package lld.bookmyshow.repository;
import lld.bookmyshow.entity.Booking;
import lld.bookmyshow.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByStatus(BookingStatus status);
}
