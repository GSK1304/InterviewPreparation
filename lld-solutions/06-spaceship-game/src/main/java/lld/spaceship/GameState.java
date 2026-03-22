package lld.spaceship;
import java.util.*;

public class GameState {
    private int  tick = 0;
    private boolean enemyBottom = false;
    private final List<GameEntity> pending = new ArrayList<>();

    public void incrementTick()                 { tick++; }
    public int  getTick()                       { return tick; }
    public void setEnemyReachedBottom(boolean v){ enemyBottom = v; }
    public boolean hasEnemyReachedBottom()      { return enemyBottom; }
    public void addEntity(GameEntity e)         { pending.add(e); }
    public List<GameEntity> drainPending()      {
        List<GameEntity> r = new ArrayList<>(pending); pending.clear(); return r;
    }
}
