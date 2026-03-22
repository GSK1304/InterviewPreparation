package lld.splitwise.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "expense_split") @Getter @Setter @NoArgsConstructor
public class ExpenseSplit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "expense_id") private Expense expense;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "amount_paise", nullable = false) private Long amountPaise;
}
