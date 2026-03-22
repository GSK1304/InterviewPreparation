package lld.spaceship;
public abstract class GameEntity {
    protected int        x, y;
    protected boolean    alive = true;
    protected final EntityType type;

    public GameEntity(int x, int y, EntityType type) { this.x = x; this.y = y; this.type = type; }

    public abstract void update(GameState state);
    public abstract char getSymbol();

    public BoundingBox getBounds() { return new BoundingBox(x, y, 1, 1); }
    public boolean isAlive()       { return alive; }
    public void    destroy()       { alive = false; }
    public EntityType getType()    { return type; }
    public int getX()              { return x; }
    public int getY()              { return y; }
}
