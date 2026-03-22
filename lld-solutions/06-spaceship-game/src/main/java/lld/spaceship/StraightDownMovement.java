package lld.spaceship;
import java.util.Random;
public class StraightDownMovement implements MovementStrategy {
    private final int speed;
    private final Random rng = new Random();
    public StraightDownMovement(int speed) { this.speed = Math.max(1, speed); }
    @Override public boolean move(Enemy enemy, int tick) {
        if (tick % Math.max(1, 3 / speed) == 0) enemy.moveDown();
        return rng.nextInt(100) < GameConfig.ENEMY_SHOOT_CHANCE;
    }
}
