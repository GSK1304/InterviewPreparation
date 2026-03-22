package lld.atm.repository;
import lld.atm.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    Optional<BankAccount> findByCardNumber(String cardNumber);
    @Modifying @Query("UPDATE BankAccount a SET a.balancePaise = a.balancePaise - :amount WHERE a.accountNumber = :acc AND a.balancePaise >= :amount")
    int debit(@Param("acc") String accountNumber, @Param("amount") long amountPaise);
    @Modifying @Query("UPDATE BankAccount a SET a.balancePaise = a.balancePaise + :amount WHERE a.accountNumber = :acc")
    int credit(@Param("acc") String accountNumber, @Param("amount") long amountPaise);
}
