package lld.splitwise.repository;
import lld.splitwise.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface GroupRepository extends JpaRepository<ExpenseGroup, Long> {}
