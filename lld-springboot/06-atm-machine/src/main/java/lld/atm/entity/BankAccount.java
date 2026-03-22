package lld.atm.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "bank_account") @Getter @Setter @NoArgsConstructor
public class BankAccount {
    @Id @Column(name = "account_number") private String accountNumber;
    @Column(name = "card_number", nullable = false, unique = true) private String cardNumber;
    @Column(name = "hashed_pin", nullable = false) private String hashedPin;
    @Column(name = "balance_paise", nullable = false) private Long balancePaise;
    @Column(name = "failed_attempts", nullable = false) private Integer failedAttempts = 0;
    @Column(nullable = false) private Boolean blocked = false;
    @Column(name = "account_holder", nullable = false) private String accountHolder;
}
