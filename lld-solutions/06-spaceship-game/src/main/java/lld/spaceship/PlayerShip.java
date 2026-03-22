package lld.spaceship;
import java.util.Optional;

public class PlayerShip extends GameEntity {
    private int     lives;
    private boolean shieldActive   = false;
    private boolean rapidFire      = false;
    private int     shieldTicks    = 0;
    private int     rapidFireTicks = 0;
    private int     bulletsOnScreen= 0;
    private static final int POWER_UP_DURATION = 40;

    public PlayerShip(int x, int y, int lives) {
        super(x, y, EntityType.PLAYER);
        this.lives = lives;
    }

    @Override public char getSymbol() { return shieldActive ? 'O' : 'A'; }

    @Override public void update(GameState state) {
        if (shieldActive && --shieldTicks <= 0)   shieldActive = false;
        if (rapidFire    && --rapidFireTicks <= 0) rapidFire   = false;
    }

    public boolean move(Direction dir) {
        if (dir == Direction.LEFT  && x > 0)                          { x--; return true; }
        if (dir == Direction.RIGHT && x < GameConfig.GRID_WIDTH - 1)  { x++; return true; }
        return false;
    }

    public Optional<Bullet> shoot() {
        int max = rapidFire ? GameConfig.MAX_BULLETS * 2 : GameConfig.MAX_BULLETS;
        if (bulletsOnScreen >= max) return Optional.empty();
        bulletsOnScreen++;
        return Optional.of(new Bullet(x, y - 1, Direction.UP, EntityType.BULLET, this));
    }

    public void bulletDespawned() { if (bulletsOnScreen > 0) bulletsOnScreen--; }

    public boolean hit() {
        if (shieldActive) { shieldActive = false; return false; }
        lives--;
        return lives <= 0;
    }

    public void applyPowerUp(PowerUpType type) {
        switch (type) {
            case EXTRA_LIFE    -> lives++;
            case SHIELD        -> { shieldActive = true; shieldTicks    = POWER_UP_DURATION; }
            case RAPID_FIRE    -> { rapidFire    = true; rapidFireTicks = POWER_UP_DURATION; }
            case DOUBLE_BULLET -> rapidFire = true;
        }
    }

    public int getLives() { return lives; }
}
