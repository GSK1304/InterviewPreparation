package lld.library.entity;
import jakarta.persistence.*;
import lld.library.enums.LoanStatus;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "loan") @Getter @Setter @NoArgsConstructor
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "barcode", nullable = false) private BookCopy copy;
    @Column(name = "borrow_date", nullable = false) private LocalDate borrowDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "return_date") private LocalDate returnDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LoanStatus status = LoanStatus.ACTIVE;
    @Column(name = "fine_paise") private Long finePaise;
}
