package lld.bookmyshow.entity;
import jakarta.persistence.*;
import lld.bookmyshow.enums.BookingStatus;
import lombok.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name = "booking") @Getter @Setter @NoArgsConstructor
public class Booking {
    @Id @Column(name = "booking_id") private String bookingId;
    @Column(name = "user_id", nullable = false) private String userId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "show_id", nullable = false) private Show show;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    private List<Long> seatIds = new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BookingStatus status = BookingStatus.LOCKED;
    @Column(name = "total_paise", nullable = false) private Long totalPaise;
    @Column(name = "lock_expiry", nullable = false) private Instant lockExpiry;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
