# LLD — Space Shooter Game (Nokia-style, Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| Game loop | Fixed-timestep loop — update() then render() |
| Entity hierarchy | Abstract `GameEntity` → `PlayerShip`, `Enemy`, `Bullet`, `PowerUp` |
| Collision detection | AABB (Axis-Aligned Bounding Box) — fast for 2D grid games |
| Enemy spawning | **Factory** — `EnemyFactory` creates enemies with level-scaled health/speed |
| Enemy movement | **Strategy** — `MovementStrategy` (ZigZag, Straight, FormationDive) |
| Game state | **State** pattern — MENU / PLAYING / PAUSED / GAME_OVER / LEVEL_COMPLETE |
| Events | **Observer** — `GameEventBus` decouples collision from score/lives/sound |
| Power-ups | Sealed interface — exhaustive handling in switch |

## Complete Solution

```java
package lld.spaceshooter;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// ── Constants ─────────────────────────────────────────────────────────────────

class GameConfig {
    static final int  GRID_WIDTH       = 20;
    static final int  GRID_HEIGHT      = 15;
    static final int  PLAYER_LIVES     = 3;
    static final int  PLAYER_SPEED     = 1;
    static final int  BULLET_SPEED     = 2;
    static final int  ENEMY_BULLET_SPEED = 1;
    static final int  SCORE_PER_KILL   = 100;
    static final int  SCORE_PER_LEVEL  = 500;
    static final int  INITIAL_ENEMIES  = 5;
    static final int  MAX_BULLETS      = 3;   // max player bullets at once
    static final long TICK_MS          = 100;  // game ticks every 100ms
    static final int  ENEMY_SHOOT_CHANCE = 5;  // % chance enemy shoots per tick
}

// ── Enums ─────────────────────────────────────────────────────────────────────

enum Direction  { LEFT, RIGHT, UP, DOWN }
enum GameStatus { MENU, PLAYING, PAUSED, LEVEL_COMPLETE, GAME_OVER }
enum EntityType { PLAYER, ENEMY, BULLET, ENEMY_BULLET, POWERUP, EXPLOSION }

// ── Sealed Power-up Types ─────────────────────────────────────────────────────

sealed interface PowerUpType permits
    PowerUpType.ExtraLife,
    PowerUpType.RapidFire,
    PowerUpType.Shield,
    PowerUpType.DoubleBullet {

    record ExtraLife()     implements PowerUpType {}
    record RapidFire(int durationTicks) implements PowerUpType {}
    record Shield(int durationTicks)    implements PowerUpType {}
    record DoubleBullet(int durationTicks) implements PowerUpType {}
}

// ── Game Events (Observer) ────────────────────────────────────────────────────

sealed interface GameEvent permits
    GameEvent.EnemyKilled,
    GameEvent.PlayerHit,
    GameEvent.LevelComplete,
    GameEvent.GameOver,
    GameEvent.PowerUpCollected {

    record EnemyKilled(int x, int y, int scoreValue) implements GameEvent {}
    record PlayerHit(int livesRemaining)             implements GameEvent {}
    record LevelComplete(int level, int totalScore)  implements GameEvent {}
    record GameOver(int finalScore)                  implements GameEvent {}
    record PowerUpCollected(PowerUpType type)        implements GameEvent {}
}

@FunctionalInterface
interface GameEventListener {
    void onEvent(GameEvent event);
}

class GameEventBus {
    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();

    void subscribe(GameEventListener listener) { listeners.add(listener); }

    void publish(GameEvent event) {
        listeners.forEach(l -> {
            try { l.onEvent(event); }
            catch (Exception e) { /* don't let one listener crash the game */ }
        });
    }
}

// ── Position & Bounding Box ───────────────────────────────────────────────────

record Position(int x, int y) {
    boolean isInBounds() {
        return x >= 0 && x < GameConfig.GRID_WIDTH &&
               y >= 0 && y < GameConfig.GRID_HEIGHT;
    }
}

record BoundingBox(int x, int y, int width, int height) {
    boolean intersects(BoundingBox other) {
        return x < other.x + other.width  &&
               x + width  > other.x       &&
               y < other.y + other.height &&
               y + height > other.y;
    }
}

// ── Abstract Game Entity ──────────────────────────────────────────────────────

abstract class GameEntity {
    protected int        x, y;
    protected boolean    alive = true;
    protected final EntityType type;

    GameEntity(int x, int y, EntityType type) {
        this.x    = x;
        this.y    = y;
        this.type = type;
    }

    public abstract void update(GameState state);
    public abstract String getSymbol();

    BoundingBox getBounds() { return new BoundingBox(x, y, 1, 1); }
    boolean isAlive()       { return alive; }
    void destroy()          { alive = false; }
    EntityType getType()    { return type; }
    int getX()              { return x; }
    int getY()              { return y; }
}

// ── Player Ship ───────────────────────────────────────────────────────────────

class PlayerShip extends GameEntity {
    private int     lives;
    private boolean shieldActive   = false;
    private boolean rapidFire      = false;
    private int     shieldTicks    = 0;
    private int     rapidFireTicks = 0;
    private int     bulletsOnScreen = 0;

    PlayerShip(int x, int y, int lives) {
        super(x, y, EntityType.PLAYER);
        if (lives <= 0) throw new IllegalArgumentException("Lives must be positive");
        this.lives = lives;
    }

    @Override public String getSymbol() { return shieldActive ? "🛡" : "🚀"; }

    @Override
    public void update(GameState state) {
        // Tick down active power-ups
        if (shieldActive && --shieldTicks <= 0)  shieldActive = false;
        if (rapidFire    && --rapidFireTicks <= 0) rapidFire = false;
    }

    /** Move left/right; clamp to grid bounds */
    boolean move(Direction dir) {
        if (dir == Direction.LEFT  && x > 0) { x--; return true; }
        if (dir == Direction.RIGHT && x < GameConfig.GRID_WIDTH - 1) { x++; return true; }
        return false;
    }

    /** Returns new bullet if allowed, null if at max bullets */
    Optional<Bullet> shoot() {
        int maxBullets = rapidFire ? GameConfig.MAX_BULLETS * 2 : GameConfig.MAX_BULLETS;
        if (bulletsOnScreen >= maxBullets) return Optional.empty();
        bulletsOnScreen++;
        return Optional.of(new Bullet(x, y - 1, Direction.UP, EntityType.BULLET, this));
    }

    void bulletDespawned() {
        if (bulletsOnScreen > 0) bulletsOnScreen--;
    }

    boolean hit() {
        if (shieldActive) { shieldActive = false; return false; }  // shield absorbs hit
        lives--;
        return lives <= 0;
    }

    void applyPowerUp(PowerUpType type) {
        switch (type) {
            case PowerUpType.ExtraLife   e -> lives++;
            case PowerUpType.Shield      s -> { shieldActive = true;  shieldTicks = s.durationTicks(); }
            case PowerUpType.RapidFire   r -> { rapidFire    = true;  rapidFireTicks = r.durationTicks(); }
            case PowerUpType.DoubleBullet d -> rapidFire = true;  // simplified
        }
    }

    int getLives()             { return lives; }
    boolean hasShield()        { return shieldActive; }
    int getBulletsOnScreen()   { return bulletsOnScreen; }
}

// ── Movement Strategies ───────────────────────────────────────────────────────

interface MovementStrategy {
    /** Update enemy position. Return true if enemy should shoot */
    boolean move(Enemy enemy, int tick);
}

class StraightDownMovement implements MovementStrategy {
    private final int speed;
    StraightDownMovement(int speed) { this.speed = Math.max(1, speed); }

    @Override
    public boolean move(Enemy enemy, int tick) {
        if (tick % (3 / speed + 1) == 0) enemy.y++;
        return new Random().nextInt(100) < GameConfig.ENEMY_SHOOT_CHANCE;
    }
}

class ZigZagMovement implements MovementStrategy {
    private int directionChangeCounter = 0;
    private boolean movingRight = true;

    @Override
    public boolean move(Enemy enemy, int tick) {
        if (tick % 2 == 0) {
            if (movingRight) { if (enemy.x < GameConfig.GRID_WIDTH - 1) enemy.x++; else movingRight = false; }
            else             { if (enemy.x > 0) enemy.x--; else movingRight = true; }
            if (++directionChangeCounter % 6 == 0) enemy.y++;  // descend periodically
        }
        return new Random().nextInt(100) < GameConfig.ENEMY_SHOOT_CHANCE;
    }
}

class FormationDiveMovement implements MovementStrategy {
    private boolean diving = false;
    private int diveStartX;

    @Override
    public boolean move(Enemy enemy, int tick) {
        if (!diving && tick % 20 == 0) {
            diving    = true;
            diveStartX = enemy.x;
        }
        if (diving) {
            enemy.y++;
            if (enemy.y > GameConfig.GRID_HEIGHT - 3) {
                enemy.y = 0;  // reset to top after reaching bottom
                diving  = false;
            }
        } else {
            // Gentle horizontal drift
            if (tick % 3 == 0) enemy.x = (enemy.x + 1) % GameConfig.GRID_WIDTH;
        }
        return new Random().nextInt(100) < GameConfig.ENEMY_SHOOT_CHANCE * 2;
    }
}

// ── Enemy ─────────────────────────────────────────────────────────────────────

class Enemy extends GameEntity {
    private int              health;
    private final int        scoreValue;
    private final MovementStrategy movement;
    private final PowerUpType dropPowerUp;  // null if no drop

    Enemy(int x, int y, int health, int scoreValue,
          MovementStrategy movement, PowerUpType dropPowerUp) {
        super(x, y, EntityType.ENEMY);
        this.health      = health;
        this.scoreValue  = scoreValue;
        this.movement    = Objects.requireNonNull(movement);
        this.dropPowerUp = dropPowerUp;
    }

    @Override public String getSymbol() {
        return switch (health) {
            case 1      -> "👾";
            case 2      -> "🤖";
            default     -> "💀";
        };
    }

    @Override
    public void update(GameState state) {
        boolean shouldShoot = movement.move(this, state.getTick());

        // Enemy reached the bottom — game over condition
        if (y >= GameConfig.GRID_HEIGHT - 1) {
            state.setEnemyReachedBottom(true);
            return;
        }

        if (shouldShoot) {
            state.addEntity(new Bullet(x, y + 1, Direction.DOWN,
                EntityType.ENEMY_BULLET, null));
        }
    }

    boolean hit(int damage) {
        health -= damage;
        if (health <= 0) { destroy(); return true; }
        return false;
    }

    int getScoreValue()          { return scoreValue; }
    Optional<PowerUpType> getPowerUpDrop() { return Optional.ofNullable(dropPowerUp); }
}

// ── Enemy Factory ─────────────────────────────────────────────────────────────

class EnemyFactory {
    private final Random random = new Random();

    Enemy create(int x, int y, int level) {
        int health     = 1 + level / 3;         // health increases with level
        int scoreValue = GameConfig.SCORE_PER_KILL * (1 + level / 5);
        MovementStrategy strategy = selectStrategy(level);
        PowerUpType drop = random.nextInt(10) == 0 ? randomPowerUp() : null;  // 10% drop
        return new Enemy(x, y, health, scoreValue, strategy, drop);
    }

    private MovementStrategy selectStrategy(int level) {
        int choice = random.nextInt(3);
        return switch (choice) {
            case 0 -> new StraightDownMovement(Math.min(level, 3));
            case 1 -> new ZigZagMovement();
            default-> new FormationDiveMovement();
        };
    }

    private PowerUpType randomPowerUp() {
        return switch (new Random().nextInt(4)) {
            case 0  -> new PowerUpType.ExtraLife();
            case 1  -> new PowerUpType.Shield(50);
            case 2  -> new PowerUpType.RapidFire(30);
            default -> new PowerUpType.DoubleBullet(40);
        };
    }
}

// ── Bullet ────────────────────────────────────────────────────────────────────

class Bullet extends GameEntity {
    private final Direction  direction;
    private final PlayerShip owner;    // null for enemy bullets
    private int              tickDelay = 0;

    Bullet(int x, int y, Direction direction, EntityType type, PlayerShip owner) {
        super(x, y, type);
        this.direction = Objects.requireNonNull(direction);
        this.owner     = owner;
    }

    @Override public String getSymbol() {
        return type == EntityType.BULLET ? "↑" : "↓";
    }

    @Override
    public void update(GameState state) {
        // Move every N ticks for speed control
        if (tickDelay++ % (type == EntityType.BULLET ? 1 : 2) != 0) return;

        if (direction == Direction.UP)   y -= GameConfig.BULLET_SPEED;
        if (direction == Direction.DOWN) y += GameConfig.ENEMY_BULLET_SPEED;

        if (!new Position(x, y).isInBounds()) {
            destroy();
            if (owner != null) owner.bulletDespawned();
        }
    }

    boolean isPlayerBullet() { return type == EntityType.BULLET; }
    PlayerShip getOwner()    { return owner; }
}

// ── Power-up Drop ─────────────────────────────────────────────────────────────

class PowerUp extends GameEntity {
    private final PowerUpType powerUpType;

    PowerUp(int x, int y, PowerUpType type) {
        super(x, y, EntityType.POWERUP);
        this.powerUpType = Objects.requireNonNull(type);
    }

    @Override public String getSymbol() {
        return switch (powerUpType) {
            case PowerUpType.ExtraLife    e -> "❤";
            case PowerUpType.Shield       s -> "🛡";
            case PowerUpType.RapidFire    r -> "⚡";
            case PowerUpType.DoubleBullet d -> "✦";
        };
    }

    @Override
    public void update(GameState state) {
        // Power-up slowly descends
        if (state.getTick() % 5 == 0) y++;
        if (y >= GameConfig.GRID_HEIGHT) destroy();
    }

    PowerUpType getPowerUpType() { return powerUpType; }
}

// ── Game State ────────────────────────────────────────────────────────────────

class GameState {
    private int         tick        = 0;
    private boolean     enemyBottom = false;
    private final List<GameEntity> pendingAdditions = new ArrayList<>();

    void incrementTick()               { tick++; }
    int  getTick()                     { return tick; }
    void setEnemyReachedBottom(boolean v) { enemyBottom = v; }
    boolean hasEnemyReachedBottom()    { return enemyBottom; }
    void addEntity(GameEntity entity)  { pendingAdditions.add(entity); }
    List<GameEntity> drainPending()    {
        List<GameEntity> result = new ArrayList<>(pendingAdditions);
        pendingAdditions.clear();
        return result;
    }
}

// ── Game ──────────────────────────────────────────────────────────────────────

class SpaceShooterGame {
    private final PlayerShip       player;
    private final List<GameEntity> entities = new ArrayList<>();
    private final GameEventBus     eventBus;
    private final EnemyFactory     enemyFactory;
    private GameStatus             status;
    private int                    score  = 0;
    private int                    level  = 1;
    private final GameState        state  = new GameState();

    SpaceShooterGame() {
        this.player      = new PlayerShip(GameConfig.GRID_WIDTH / 2,
                                          GameConfig.GRID_HEIGHT - 1,
                                          GameConfig.PLAYER_LIVES);
        this.eventBus    = new GameEventBus();
        this.enemyFactory = new EnemyFactory();
        this.status      = GameStatus.MENU;
        entities.add(player);
        setupEventListeners();
    }

    private void setupEventListeners() {
        eventBus.subscribe(event -> switch (event) {
            case GameEvent.EnemyKilled e -> {
                score += e.scoreValue();
                System.out.printf("💥 Enemy destroyed at (%d,%d) +%d pts | Score: %d%n",
                    e.x(), e.y(), e.scoreValue(), score);
            }
            case GameEvent.PlayerHit h -> System.out.printf(
                "💔 Player hit! Lives remaining: %d%n", h.livesRemaining());
            case GameEvent.LevelComplete l -> System.out.printf(
                "🎯 Level %d complete! Score: %d%n", l.level(), l.totalScore());
            case GameEvent.GameOver g -> System.out.printf(
                "☠ GAME OVER! Final score: %d%n", g.finalScore());
            case GameEvent.PowerUpCollected p -> System.out.printf(
                "⭐ Power-up collected: %s%n", p.type().getClass().getSimpleName());
        });
    }

    // ── Public Controls ───────────────────────────────────────────────────────

    public void start() {
        status = GameStatus.PLAYING;
        spawnEnemies();
        System.out.println("🚀 Space Shooter started! Level " + level);
    }

    public void movePlayer(Direction dir) {
        if (status != GameStatus.PLAYING) return;
        player.move(dir);
    }

    public void playerShoot() {
        if (status != GameStatus.PLAYING) return;
        player.shoot().ifPresent(entities::add);
    }

    public void pause() {
        if (status == GameStatus.PLAYING) status = GameStatus.PAUSED;
        else if (status == GameStatus.PAUSED) status = GameStatus.PLAYING;
    }

    // ── Game Loop ─────────────────────────────────────────────────────────────

    /** Call this every TICK_MS milliseconds */
    public void tick() {
        if (status != GameStatus.PLAYING) return;

        state.incrementTick();

        // Update all entities
        entities.forEach(e -> { if (e.isAlive()) e.update(state); });

        // Add any entities spawned during update (enemy bullets, etc.)
        entities.addAll(state.drainPending());

        // Collision detection
        detectCollisions();

        // Remove dead entities
        entities.removeIf(e -> !e.isAlive());

        // Check win/loss conditions
        checkLevelComplete();
        checkGameOver();
    }

    // ── Collision Detection (AABB) ────────────────────────────────────────────

    private void detectCollisions() {
        List<Bullet>   playerBullets = entities.stream()
            .filter(e -> e instanceof Bullet b && b.isPlayerBullet() && e.isAlive())
            .map(e -> (Bullet) e).toList();

        List<Enemy>    enemies = entities.stream()
            .filter(e -> e instanceof Enemy && e.isAlive())
            .map(e -> (Enemy) e).toList();

        List<Bullet>   enemyBullets = entities.stream()
            .filter(e -> e instanceof Bullet b && !b.isPlayerBullet() && e.isAlive())
            .map(e -> (Bullet) e).toList();

        List<PowerUp>  powerUps = entities.stream()
            .filter(e -> e instanceof PowerUp && e.isAlive())
            .map(e -> (PowerUp) e).toList();

        // Player bullet hits enemy
        for (Bullet bullet : playerBullets) {
            for (Enemy enemy : enemies) {
                if (bullet.getBounds().intersects(enemy.getBounds())) {
                    bullet.destroy();
                    bullet.getOwner().bulletDespawned();
                    boolean killed = enemy.hit(1);
                    if (killed) {
                        score += enemy.getScoreValue();
                        eventBus.publish(new GameEvent.EnemyKilled(
                            enemy.getX(), enemy.getY(), enemy.getScoreValue()));
                        enemy.getPowerUpDrop().ifPresent(drop ->
                            entities.add(new PowerUp(enemy.getX(), enemy.getY(), drop)));
                    }
                    break;
                }
            }
        }

        // Enemy bullet hits player
        for (Bullet bullet : enemyBullets) {
            if (bullet.getBounds().intersects(player.getBounds())) {
                bullet.destroy();
                boolean dead = player.hit();
                eventBus.publish(new GameEvent.PlayerHit(player.getLives()));
                if (dead) {
                    player.destroy();
                    status = GameStatus.GAME_OVER;
                    eventBus.publish(new GameEvent.GameOver(score));
                }
                break;
            }
        }

        // Player collects power-up
        for (PowerUp powerUp : powerUps) {
            if (powerUp.getBounds().intersects(player.getBounds())) {
                player.applyPowerUp(powerUp.getPowerUpType());
                powerUp.destroy();
                eventBus.publish(new GameEvent.PowerUpCollected(powerUp.getPowerUpType()));
            }
        }
    }

    // ── Level Progression ─────────────────────────────────────────────────────

    private void checkLevelComplete() {
        boolean noEnemies = entities.stream()
            .noneMatch(e -> e instanceof Enemy && e.isAlive());
        if (noEnemies && status == GameStatus.PLAYING) {
            score += GameConfig.SCORE_PER_LEVEL;
            level++;
            status = GameStatus.LEVEL_COMPLETE;
            eventBus.publish(new GameEvent.LevelComplete(level - 1, score));
            // Auto-start next level
            status = GameStatus.PLAYING;
            spawnEnemies();
        }
    }

    private void checkGameOver() {
        if (state.hasEnemyReachedBottom() && status == GameStatus.PLAYING) {
            status = GameStatus.GAME_OVER;
            eventBus.publish(new GameEvent.GameOver(score));
        }
    }

    private void spawnEnemies() {
        int enemyCount = GameConfig.INITIAL_ENEMIES + (level - 1) * 2;
        for (int i = 0; i < enemyCount; i++) {
            int x = (i * (GameConfig.GRID_WIDTH / enemyCount)) % GameConfig.GRID_WIDTH;
            int y = 1 + (i % 3);  // stagger rows
            entities.add(enemyFactory.create(x, y, level));
        }
        System.out.printf("👾 Level %d: %d enemies spawned%n", level, enemyCount);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render() {
        char[][] grid = new char[GameConfig.GRID_HEIGHT][GameConfig.GRID_WIDTH];
        for (char[] row : grid) Arrays.fill(row, '·');

        entities.stream()
            .filter(GameEntity::isAlive)
            .forEach(e -> {
                if (e.getX() >= 0 && e.getX() < GameConfig.GRID_WIDTH &&
                    e.getY() >= 0 && e.getY() < GameConfig.GRID_HEIGHT) {
                    // Simple single-char rendering
                    char symbol = switch (e.getType()) {
                        case PLAYER       -> 'A';
                        case ENEMY        -> 'E';
                        case BULLET       -> '|';
                        case ENEMY_BULLET -> 'v';
                        case POWERUP      -> '*';
                        default           -> '?';
                    };
                    grid[e.getY()][e.getX()] = symbol;
                }
            });

        System.out.println("┌" + "─".repeat(GameConfig.GRID_WIDTH) + "┐");
        for (char[] row : grid) {
            System.out.print("│");
            for (char c : row) System.out.print(c);
            System.out.println("│");
        }
        System.out.println("└" + "─".repeat(GameConfig.GRID_WIDTH) + "┘");
        System.out.printf("Score: %d | Lives: %d | Level: %d | Status: %s%n%n",
            score, player.getLives(), level, status);
    }

    public GameStatus getStatus() { return status; }
    public int getScore()         { return score; }
    public int getLevel()         { return level; }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class SpaceShooterDemo {
    public static void main(String[] args) throws InterruptedException {
        SpaceShooterGame game = new SpaceShooterGame();
        game.start();
        game.render();

        // Simulate a few ticks with player actions
        for (int i = 0; i < 30; i++) {
            // Player shoots every tick
            game.playerShoot();

            // Player moves: left first, then right
            if (i < 10)      game.movePlayer(Direction.LEFT);
            else if (i < 20) game.movePlayer(Direction.RIGHT);

            game.tick();

            if (i % 5 == 0) game.render();  // render every 5 ticks

            if (game.getStatus() == GameStatus.GAME_OVER) {
                System.out.println("Game Over after " + (i + 1) + " ticks");
                break;
            }
            if (game.getStatus() == GameStatus.LEVEL_COMPLETE) {
                System.out.println("Level complete!");
            }

            Thread.sleep(GameConfig.TICK_MS);
        }

        System.out.println("Final score: " + game.getScore());
        System.out.println("Reached level: " + game.getLevel());
    }
}
```

## Extension Q&A

**Q: How do you add multi-directional shooting (spread shot)?**
In `PlayerShip.shoot()`, when spread shot is active (a `PowerUpType`), return 3 bullets with directions LEFT-UP, UP, RIGHT-UP. The `Bullet` class moves based on its `Direction` enum — add diagonal directions (UP_LEFT, UP_RIGHT) and update the `update()` movement accordingly.

**Q: How do you add a boss enemy every 5 levels?**
In `EnemyFactory.create()`, add a `BossEnemy extends Enemy` class. At level % 5 == 0, `spawnEnemies()` adds one boss instead of regular enemies. Boss has health=20, width=3 (multi-cell bounding box), multi-phase movement, and a special attack pattern. The `BoundingBox` already handles variable sizes.

**Q: How do you persist high scores?**
Add a `HighScoreManager` with `load(Path file)` and `save(Path file)` methods. On game over, publish a `GameEvent.GameOver` event — the `HighScoreManager` listener persists the score. Scores stored as `List<ScoreEntry(name, score, level, date)>` serialized to JSON.

**Q: Why use Observer (EventBus) instead of direct method calls for scoring?**
The game loop doesn't need to know about scoring, sound effects, or high score persistence. Each concern subscribes to the event bus independently. Adding a sound effect on enemy kill = add one subscriber, zero changes to game loop. This is the Open/Closed principle applied to event handling — the game is closed for modification but open for extension via subscribers.
