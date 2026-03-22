package lld.bookmyshow.entity;
import jakarta.persistence.*;
import lld.bookmyshow.enums.SeatStatus;
import lld.bookmyshow.enums.SeatTier;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "seat",
    indexes = {@Index(name = "idx_seat_show_status", columnList = "show_id,status")})
@Getter @Setter @NoArgsConstructor
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "show_id", nullable = false) private Show show;
    @Column(name = "row_number", nullable = false) private Integer rowNumber;
    @Column(name = "col_number", nullable = false) private Integer colNumber;
    @Column(name = "seat_code",  nullable = false) private String seatCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SeatTier tier;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SeatStatus status = SeatStatus.AVAILABLE;
    @Column(name = "locked_by_booking") private String lockedByBookingId;
    @Column(name = "lock_expiry") private Instant lockExpiry;
    @Column(name = "base_price_paise", nullable = false) private Long basePricePaise;
}
