package lld.parking.repository;

import lld.parking.entity.ParkingSpot;
import lld.parking.enums.SpotSize;
import lld.parking.enums.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    @Query("""
        SELECT s FROM ParkingSpot s
        JOIN FETCH s.floor f
        WHERE s.status = 'AVAILABLE'
        AND s.size IN :sizes
        ORDER BY f.floorNumber ASC, s.spotCode ASC
        """)
    List<ParkingSpot> findAvailableSpotsBySize(@Param("sizes") List<SpotSize> sizes);

    @Query("""
        SELECT s FROM ParkingSpot s
        JOIN FETCH s.floor f
        WHERE s.status = 'AVAILABLE'
        AND s.size IN :sizes
        ORDER BY (SELECT COUNT(s2) FROM ParkingSpot s2
                  WHERE s2.floor = f AND s2.status = 'AVAILABLE') DESC
        """)
    List<ParkingSpot> findAvailableSpotsLoadBalanced(@Param("sizes") List<SpotSize> sizes);

    @Modifying
    @Query("UPDATE ParkingSpot s SET s.status = :status WHERE s.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") SpotStatus status);

    Optional<ParkingSpot> findBySpotCode(String spotCode);

    @Query("SELECT COUNT(s) FROM ParkingSpot s WHERE s.status = 'AVAILABLE'")
    long countAvailableSpots();
}
