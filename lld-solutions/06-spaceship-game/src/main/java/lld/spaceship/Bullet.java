package lld.spaceship;
public class Bullet extends GameEntity {
    private final Direction    direction;
    private final PlayerShip   owner;
    private int                tickDelay = 0;

    public Bullet(int x, int y, Direction dir, EntityType type, PlayerShip owner) {
        super(x, y, type);
        this.direction = dir;
        this.owner     = owner;
    }

    @Override public char getSymbol() { return type == EntityType.BULLET ? '|' : 'v'; }

    @Override public void update(GameState state) {
        if (tickDelay++ % (type == EntityType.BULLET ? 1 : 2) != 0) return;
        if (direction == Direction.UP)   y -= GameConfig.BULLET_SPEED;
        if (direction == Direction.DOWN) y++;
        if (!new Position(x, y).isInBounds()) {
            destroy();
            if (owner != null) owner.bulletDespawned();
        }
    }

    public boolean isPlayerBullet() { return type == EntityType.BULLET; }
    public PlayerShip getOwner()    { return owner; }
}
