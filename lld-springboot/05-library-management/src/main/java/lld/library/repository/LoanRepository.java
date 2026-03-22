package lld.library.repository;
import lld.library.entity.Loan;
import lld.library.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    long countByMemberMemberIdAndStatus(String memberId, LoanStatus status);
    Optional<Loan> findByMemberMemberIdAndCopyBarcodeAndStatus(String memberId, String barcode, LoanStatus status);
    List<Loan> findByMemberMemberIdOrderByBorrowDateDesc(String memberId);
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < CURRENT_DATE")
    List<Loan> findOverdueLoans();
}
