package lld.splitwise.entity;
import jakarta.persistence.*;
import lld.splitwise.enums.ExpenseCategory;
import lld.splitwise.enums.SplitType;
import lombok.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name = "expense") @Getter @Setter @NoArgsConstructor
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String description;
    @Column(name = "total_paise", nullable = false) private Long totalPaise;
    @Column(name = "paid_by_user_id", nullable = false) private String paidByUserId;
    @Enumerated(EnumType.STRING) @Column(name = "split_type", nullable = false) private SplitType splitType;
    @Enumerated(EnumType.STRING) private ExpenseCategory category = ExpenseCategory.OTHER;
    @Column(name = "group_id") private Long groupId;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ExpenseSplit> splits = new ArrayList<>();
}
