package lld.spaceship;
import java.util.Optional;

public class Enemy extends GameEntity {
    private int                health;
    private final int          scoreValue;
    private final MovementStrategy movement;
    private final PowerUpType  drop;

    public Enemy(int x, int y, int health, int score, MovementStrategy movement, PowerUpType drop) {
        super(x, y, EntityType.ENEMY);
        this.health    = health;
        this.scoreValue= score;
        this.movement  = movement;
        this.drop      = drop;
    }

    @Override public char getSymbol() { return health > 1 ? 'W' : 'E'; }

    @Override public void update(GameState state) {
        boolean shoot = movement.move(this, state.getTick());
        if (y >= GameConfig.GRID_HEIGHT-1) { state.setEnemyReachedBottom(true); return; }
        if (shoot) state.addEntity(new Bullet(x, y+1, Direction.DOWN, EntityType.ENEMY_BULLET, null));
    }

    public void moveDown()  { y++; }
    public void moveLeft()  { x--; }
    public void moveRight() { x++; }

    public boolean hit(int dmg) { health -= dmg; if (health <= 0) { destroy(); return true; } return false; }
    public int getScoreValue()  { return scoreValue; }
    public Optional<PowerUpType> getDropPowerUp() { return Optional.ofNullable(drop); }
}
