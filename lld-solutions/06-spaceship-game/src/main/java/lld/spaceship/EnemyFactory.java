package lld.spaceship;
import java.util.Random;

public class EnemyFactory {
    private final Random rng = new Random();
    public Enemy create(int x, int y, int level) {
        int health     = 1 + level / 3;
        int scoreValue = GameConfig.SCORE_PER_KILL * (1 + level / 5);
        MovementStrategy strategy = (rng.nextInt(2) == 0)
            ? new StraightDownMovement(Math.min(level, 3))
            : new ZigZagMovement();
        PowerUpType drop = (rng.nextInt(10) == 0) ? PowerUpType.SHIELD : null;
        return new Enemy(x, y, health, scoreValue, strategy, drop);
    }
}
