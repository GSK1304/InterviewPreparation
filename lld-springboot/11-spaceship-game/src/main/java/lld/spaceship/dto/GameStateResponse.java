package lld.spaceship.dto;
import lld.spaceship.enums.GameStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder
public class GameStateResponse {
    private Long       gameId;
    private String     playerName;
    private GameStatus status;
    private int        score;
    private int        lives;
    private int        playerX;
    private int        playerY;
    private boolean    shieldActive;
    private boolean    rapidFire;
    private int        waveNumber;
    private int        enemiesKilled;
    private List<EnemyState> enemies;
    private String     message;

    @Data @Builder
    public static class EnemyState {
        private int    x;
        private int    y;
        private String type;
        private int    health;
    }
}
