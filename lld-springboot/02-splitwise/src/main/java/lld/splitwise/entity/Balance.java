package lld.splitwise.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "balance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"debtor_id","creditor_id"}))
@Getter @Setter @NoArgsConstructor
public class Balance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "debtor_id",   nullable = false) private String debtorId;
    @Column(name = "creditor_id", nullable = false) private String creditorId;
    @Column(name = "amount_paise", nullable = false) private Long amountPaise = 0L;
}
