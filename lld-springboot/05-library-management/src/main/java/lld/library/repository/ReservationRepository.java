package lld.library.repository;
import lld.library.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findFirstByIsbnAndActiveTrueOrderByReservedDateAsc(String isbn);
    List<Reservation> findByMemberMemberIdAndActiveTrue(String memberId);
}
