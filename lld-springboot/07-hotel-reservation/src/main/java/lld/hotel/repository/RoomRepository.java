package lld.hotel.repository;
import lld.hotel.entity.Room;
import lld.hotel.enums.BedType;
import lld.hotel.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    @Query("""
        SELECT r FROM Room r WHERE r.capacity >= :guests
        AND (:type IS NULL OR r.type = :type)
        AND (:bed  IS NULL OR r.bedType = :bed)
        AND r.baseRatePaise <= :maxRate
        AND r.roomNumber NOT IN (
            SELECT h.room.roomNumber FROM HotelReservation h
            WHERE h.status NOT IN ('CANCELLED','CHECKED_OUT')
            AND h.checkIn < :checkOut AND h.checkOut > :checkIn)
        """)
    List<Room> findAvailable(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut,
        @Param("guests") int guests, @Param("type") RoomType type, @Param("bed") BedType bed, @Param("maxRate") long maxRate);
}
