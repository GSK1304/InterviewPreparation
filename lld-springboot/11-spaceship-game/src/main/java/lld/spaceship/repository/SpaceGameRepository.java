package lld.spaceship.repository;
import lld.spaceship.entity.SpaceGame;
import lld.spaceship.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SpaceGameRepository extends JpaRepository<SpaceGame, Long> {
    List<SpaceGame> findByPlayerNameOrderByScoreDesc(String playerName);
    List<SpaceGame> findByStatus(GameStatus status);
    @Query("SELECT g FROM SpaceGame g WHERE g.status = 'GAME_OVER' ORDER BY g.score DESC")
    List<SpaceGame> findLeaderboard(org.springframework.data.domain.Pageable pageable);
}
