package lld.spaceship;
import java.util.Random;
public class ZigZagMovement implements MovementStrategy {
    private boolean right = true;
    private int     counter = 0;
    private final Random rng = new Random();
    @Override public boolean move(Enemy enemy, int tick) {
        if (tick % 2 == 0) {
            if (right) { if (enemy.getX() < GameConfig.GRID_WIDTH-1) enemy.moveRight(); else right = false; }
            else        { if (enemy.getX() > 0) enemy.moveLeft(); else right = true; }
            if (++counter % 6 == 0) enemy.moveDown();
        }
        return rng.nextInt(100) < GameConfig.ENEMY_SHOOT_CHANCE;
    }
}
