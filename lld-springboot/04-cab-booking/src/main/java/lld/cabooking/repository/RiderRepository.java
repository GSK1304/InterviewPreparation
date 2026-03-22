package lld.cabooking.repository;
import lld.cabooking.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RiderRepository extends JpaRepository<Rider, String> {}
