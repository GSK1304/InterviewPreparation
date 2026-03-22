package lld.atm.repository;
import lld.atm.entity.ATMTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ATMTransactionRepository extends JpaRepository<ATMTransaction, Long> {
    List<ATMTransaction> findByAccountNumberOrderByTimestampDesc(String accountNumber);
}
