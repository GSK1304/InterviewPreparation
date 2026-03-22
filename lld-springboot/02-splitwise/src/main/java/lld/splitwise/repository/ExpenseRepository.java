package lld.splitwise.repository;
import lld.splitwise.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("SELECT e FROM Expense e JOIN FETCH e.splits WHERE :userId IN (SELECT s.userId FROM ExpenseSplit s WHERE s.expense = e)")
    List<Expense> findExpensesByParticipant(@Param("userId") String userId);
    List<Expense> findByGroupId(Long groupId);
}
