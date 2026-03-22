package lld.spaceship;
public class PowerUp extends GameEntity {
    private final PowerUpType powerUpType;
    public PowerUp(int x, int y, PowerUpType type) {
        super(x, y, EntityType.POWERUP);
        this.powerUpType = type;
    }
    @Override public char getSymbol() { return '*'; }
    @Override public void update(GameState state) {
        if (state.getTick() % 5 == 0) y++;
        if (y >= GameConfig.GRID_HEIGHT) destroy();
    }
    public PowerUpType getPowerUpType() { return powerUpType; }
}
