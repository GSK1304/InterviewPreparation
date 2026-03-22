package lld.elevator.repository;
import lld.elevator.entity.Elevator;
import lld.elevator.enums.ElevatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ElevatorRepository extends JpaRepository<Elevator, String> {
    List<Elevator> findByStatusNot(ElevatorStatus status);
}
