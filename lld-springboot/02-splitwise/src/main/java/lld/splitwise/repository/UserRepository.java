package lld.splitwise.repository;
import lld.splitwise.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<AppUser, String> {
    boolean existsByEmail(String email);
    Optional<AppUser> findByEmail(String email);
}
