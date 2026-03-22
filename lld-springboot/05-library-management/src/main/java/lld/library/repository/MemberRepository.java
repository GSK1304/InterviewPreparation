package lld.library.repository;
import lld.library.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    boolean existsByEmail(String email);
}
