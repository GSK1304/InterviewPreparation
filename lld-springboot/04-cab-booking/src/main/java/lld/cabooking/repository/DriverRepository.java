package lld.cabooking.repository;
import lld.cabooking.entity.Driver;
import lld.cabooking.enums.DriverStatus;
import lld.cabooking.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    List<Driver> findByStatusAndVehicleType(DriverStatus status, VehicleType type);
    List<Driver> findByStatus(DriverStatus status);
}
