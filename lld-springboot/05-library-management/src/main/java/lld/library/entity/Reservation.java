package lld.library.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "reservation") @Getter @Setter @NoArgsConstructor
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @Column(nullable = false) private String isbn;
    @Column(name = "reserved_date", nullable = false) private LocalDate reservedDate = LocalDate.now();
    @Column(nullable = false) private boolean active = true;
}
