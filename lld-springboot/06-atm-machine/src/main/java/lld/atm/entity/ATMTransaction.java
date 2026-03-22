package lld.atm.entity;
import jakarta.persistence.*;
import lld.atm.enums.TransactionStatus;
import lld.atm.enums.TransactionType;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "atm_transaction") @Getter @Setter @NoArgsConstructor
public class ATMTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "account_number", nullable = false) private String accountNumber;
    @Column(name = "masked_card",    nullable = false) private String maskedCard;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionType type;
    @Column(name = "amount_paise") private Long amountPaise;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransactionStatus status;
    @Column(nullable = false) private Instant timestamp = Instant.now();
    @Column(name = "dispensed_notes") private String dispensedNotes;
    @Column private String remarks;
}
