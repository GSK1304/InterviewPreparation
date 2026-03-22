package lld.hotel.entity;
import jakarta.persistence.*;
import lld.hotel.enums.ReservationStatus;
import lombok.*;
import java.time.*;
import java.util.*;
@Entity @Table(name = "hotel_reservation") @Getter @Setter @NoArgsConstructor
public class HotelReservation {
    @Id @Column(name = "reservation_id") private String reservationId;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "guest_id", nullable = false) private Guest guest;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "room_number", nullable = false) private Room room;
    @Column(name = "check_in", nullable = false) private LocalDate checkIn;
    @Column(name = "check_out", nullable = false) private LocalDate checkOut;
    @Column(name = "guest_count", nullable = false) private Integer guestCount;
    @Column(name = "total_paise", nullable = false) private Long totalPaise;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status = ReservationStatus.CONFIRMED;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_amenities", joinColumns = @JoinColumn(name = "reservation_id"))
    @Enumerated(EnumType.STRING) @Column(name = "amenity")
    private Set<lld.hotel.enums.AmenityType> amenities = new HashSet<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
