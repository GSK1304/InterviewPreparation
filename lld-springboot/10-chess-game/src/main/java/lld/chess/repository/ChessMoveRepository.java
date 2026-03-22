package lld.chess.repository;
import lld.chess.entity.ChessMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ChessMoveRepository extends JpaRepository<ChessMove, Long> {
    List<ChessMove> findByGameIdOrderByMoveNumberAsc(Long gameId);
    long countByGameId(Long gameId);
}
