package lld.parking.repository;

import lld.parking.entity.ParkingFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParkingFloorRepository extends JpaRepository<ParkingFloor, Long> {

    @Query("SELECT f FROM ParkingFloor f LEFT JOIN FETCH f.spots ORDER BY f.floorNumber ASC")
    List<ParkingFloor> findAllWithSpotsOrderByFloor();
}
