package lld.library.repository;
import lld.library.entity.BookCopy;
import lld.library.enums.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, String> {
    Optional<BookCopy> findFirstByBookIsbnAndStatus(String isbn, CopyStatus status);
    long countByBookIsbnAndStatus(String isbn, CopyStatus status);
    @Modifying @Query("UPDATE BookCopy c SET c.status = :status WHERE c.barcode = :barcode")
    int updateStatus(@Param("barcode") String barcode, @Param("status") CopyStatus status);
}
