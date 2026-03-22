package lld.elevator.repository;
import lld.elevator.entity.ElevatorRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ElevatorRequestRepository extends JpaRepository<ElevatorRequest, Long> {
    List<ElevatorRequest> findByFulfilledFalseOrderByRequestedAtAsc();
}
