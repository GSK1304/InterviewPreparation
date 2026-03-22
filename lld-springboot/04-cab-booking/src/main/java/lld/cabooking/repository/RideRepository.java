package lld.cabooking.repository;
import lld.cabooking.entity.Ride;
import lld.cabooking.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RideRepository extends JpaRepository<Ride, String> {
    List<Ride> findByRiderIdOrderByRequestedAtDesc(String riderId);
    List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status);
}
