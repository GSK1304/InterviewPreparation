package lld.spaceship;
public class GameEvent {
    public enum Type { ENEMY_KILLED, PLAYER_HIT, LEVEL_COMPLETE, GAME_OVER, POWERUP_COLLECTED }

    private final Type   type;
    private final int    intValue1;
    private final int    intValue2;
    private final int    intValue3;
    private final PowerUpType powerUpType;

    private GameEvent(Type type, int v1, int v2, int v3, PowerUpType p) {
        this.type = type; this.intValue1 = v1; this.intValue2 = v2; this.intValue3 = v3; this.powerUpType = p;
    }

    public static GameEvent enemyKilled(int x, int y, int score)  { return new GameEvent(Type.ENEMY_KILLED, x, y, score, null); }
    public static GameEvent playerHit(int lives)                  { return new GameEvent(Type.PLAYER_HIT, lives, 0, 0, null); }
    public static GameEvent levelComplete(int level, int score)   { return new GameEvent(Type.LEVEL_COMPLETE, level, score, 0, null); }
    public static GameEvent gameOver(int score)                   { return new GameEvent(Type.GAME_OVER, score, 0, 0, null); }
    public static GameEvent powerUpCollected(PowerUpType p)       { return new GameEvent(Type.POWERUP_COLLECTED, 0, 0, 0, p); }

    public Type       getType()       { return type; }
    public int        getX()          { return intValue1; }
    public int        getY()          { return intValue2; }
    public int        getScore()      { return intValue3; }
    public int        getLives()      { return intValue1; }
    public int        getLevel()      { return intValue1; }
    public int        getTotalScore() { return intValue2; }
    public PowerUpType getPowerUpType(){ return powerUpType; }
}
