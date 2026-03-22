package lld.parking.repository;

import lld.parking.entity.ParkingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, String> {

    Optional<ParkingTicket> findByTicketIdAndActiveTrue(String ticketId);

    boolean existsByLicensePlateAndActiveTrue(String licensePlate);

    @Query("SELECT t FROM ParkingTicket t JOIN FETCH t.spot s JOIN FETCH s.floor WHERE t.ticketId = :id")
    Optional<ParkingTicket> findWithSpotAndFloor(@Param("id") String ticketId);
}
