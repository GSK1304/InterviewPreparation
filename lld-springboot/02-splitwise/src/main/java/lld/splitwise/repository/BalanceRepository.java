package lld.splitwise.repository;
import lld.splitwise.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Optional<Balance> findByDebtorIdAndCreditorId(String debtorId, String creditorId);
    List<Balance> findByDebtorIdOrCreditorId(String debtorId, String creditorId);
    @Modifying
    @Query("UPDATE Balance b SET b.amountPaise = b.amountPaise + :delta WHERE b.debtorId = :debtor AND b.creditorId = :creditor")
    int adjustBalance(@Param("debtor") String debtorId, @Param("creditor") String creditorId, @Param("delta") long delta);
}
