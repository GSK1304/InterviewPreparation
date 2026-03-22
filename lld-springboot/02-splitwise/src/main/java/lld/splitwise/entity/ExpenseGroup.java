package lld.splitwise.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity @Table(name = "expense_group") @Getter @Setter @NoArgsConstructor
public class ExpenseGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(name = "created_by", nullable = false) private String createdBy;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "user_id")
    private Set<String> memberIds = new LinkedHashSet<>();
}
