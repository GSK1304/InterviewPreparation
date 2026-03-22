package lld.atm.repository;
import lld.atm.entity.CashCassette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CashCassetteRepository extends JpaRepository<CashCassette, Integer> {
    List<CashCassette> findAllByOrderByDenominationDesc();
    @Modifying @Query("UPDATE CashCassette c SET c.noteCount = c.noteCount - :count WHERE c.denomination = :denom AND c.noteCount >= :count")
    int dispense(@Param("denom") int denomination, @Param("count") int count);
}
