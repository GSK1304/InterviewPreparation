package lld.chess.repository;
import lld.chess.entity.ChessGame;
import lld.chess.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ChessGameRepository extends JpaRepository<ChessGame, Long> {
    List<ChessGame> findByWhitePlayerOrBlackPlayer(String white, String black);
    List<ChessGame> findByStatus(GameStatus status);
}
