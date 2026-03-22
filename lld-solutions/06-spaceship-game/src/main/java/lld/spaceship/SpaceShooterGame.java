package lld.spaceship;
import java.util.*;
import java.util.stream.Collectors;

public class SpaceShooterGame {
    private final PlayerShip       player;
    private final List<GameEntity> entities = new ArrayList<>();
    private final GameEventBus     eventBus;
    private final EnemyFactory     enemyFactory;
    private GameStatus             status;
    private int                    score = 0;
    private int                    level = 1;
    private final GameState        state = new GameState();

    public SpaceShooterGame() {
        this.player       = new PlayerShip(GameConfig.GRID_WIDTH / 2, GameConfig.GRID_HEIGHT - 1, GameConfig.PLAYER_LIVES);
        this.eventBus     = new GameEventBus();
        this.enemyFactory = new EnemyFactory();
        this.status       = GameStatus.MENU;
        entities.add(player);
        setupListeners();
    }

    private void setupListeners() {
        eventBus.subscribe(evt -> {
            switch (evt.getType()) {
                case ENEMY_KILLED    -> { score += evt.getScore(); System.out.printf("  Kill +%d | Score: %d%n", evt.getScore(), score); }
                case PLAYER_HIT      -> System.out.printf("  Player hit! Lives: %d%n", evt.getLives());
                case LEVEL_COMPLETE  -> System.out.printf("  Level %d complete! Score: %d%n", evt.getLevel(), evt.getTotalScore());
                case GAME_OVER       -> System.out.printf("  GAME OVER! Score: %d%n", evt.getScore());
                case POWERUP_COLLECTED -> System.out.printf("  Power-up: %s%n", evt.getPowerUpType());
            }
        });
    }

    public void start() {
        status = GameStatus.PLAYING;
        spawnEnemies();
        System.out.println("Space Shooter started! Level " + level);
    }

    public void movePlayer(Direction dir)  { if (status == GameStatus.PLAYING) player.move(dir); }
    public void playerShoot()              { if (status == GameStatus.PLAYING) player.shoot().ifPresent(entities::add); }

    public void tick() {
        if (status != GameStatus.PLAYING) return;
        state.incrementTick();
        entities.forEach(en -> { if (en.isAlive()) en.update(state); });
        entities.addAll(state.drainPending());
        detectCollisions();
        entities.removeIf(en -> !en.isAlive());
        checkLevelComplete();
        checkGameOver();
    }

    private void detectCollisions() {
        List<Bullet>  pBullets = entities.stream()
            .filter(en -> en instanceof Bullet b && b.isPlayerBullet() && en.isAlive())
            .map(en -> (Bullet) en).collect(Collectors.toList());
        List<Enemy>   enemies  = entities.stream()
            .filter(en -> en instanceof Enemy && en.isAlive())
            .map(en -> (Enemy) en).collect(Collectors.toList());
        List<Bullet>  eBullets = entities.stream()
            .filter(en -> en instanceof Bullet b && !b.isPlayerBullet() && en.isAlive())
            .map(en -> (Bullet) en).collect(Collectors.toList());
        List<PowerUp> powerUps = entities.stream()
            .filter(en -> en instanceof PowerUp && en.isAlive())
            .map(en -> (PowerUp) en).collect(Collectors.toList());

        for (Bullet b : pBullets) {
            for (Enemy en : enemies) {
                if (b.getBounds().intersects(en.getBounds())) {
                    b.destroy(); b.getOwner().bulletDespawned();
                    if (en.hit(1)) {
                        eventBus.publish(GameEvent.enemyKilled(en.getX(), en.getY(), en.getScoreValue()));
                        en.getDropPowerUp().ifPresent(pt -> entities.add(new PowerUp(en.getX(), en.getY(), pt)));
                    }
                    break;
                }
            }
        }
        for (Bullet b : eBullets) {
            if (b.getBounds().intersects(player.getBounds())) {
                b.destroy();
                boolean dead = player.hit();
                eventBus.publish(GameEvent.playerHit(player.getLives()));
                if (dead) { player.destroy(); status = GameStatus.GAME_OVER; eventBus.publish(GameEvent.gameOver(score)); }
                break;
            }
        }
        for (PowerUp pu : powerUps) {
            if (pu.getBounds().intersects(player.getBounds())) {
                player.applyPowerUp(pu.getPowerUpType()); pu.destroy();
                eventBus.publish(GameEvent.powerUpCollected(pu.getPowerUpType()));
            }
        }
    }

    private void checkLevelComplete() {
        if (entities.stream().noneMatch(en -> en instanceof Enemy && en.isAlive()) && status == GameStatus.PLAYING) {
            score += GameConfig.SCORE_PER_LEVEL; level++;
            eventBus.publish(GameEvent.levelComplete(level - 1, score));
            spawnEnemies();
        }
    }

    private void checkGameOver() {
        if (state.hasEnemyReachedBottom() && status == GameStatus.PLAYING) {
            status = GameStatus.GAME_OVER;
            eventBus.publish(GameEvent.gameOver(score));
        }
    }

    private void spawnEnemies() {
        int count = GameConfig.INITIAL_ENEMIES + (level - 1) * 2;
        for (int i = 0; i < count; i++) {
            int x = (i * Math.max(1, GameConfig.GRID_WIDTH / count)) % GameConfig.GRID_WIDTH;
            entities.add(enemyFactory.create(x, 1 + (i % 3), level));
        }
        System.out.printf("Level %d: %d enemies spawned%n", level, count);
    }

    public void render() {
        char[][] grid = new char[GameConfig.GRID_HEIGHT][GameConfig.GRID_WIDTH];
        for (char[] row : grid) Arrays.fill(row, '.');
        entities.stream().filter(GameEntity::isAlive).forEach(en -> {
            if (en.getX() >= 0 && en.getX() < GameConfig.GRID_WIDTH &&
                en.getY() >= 0 && en.getY() < GameConfig.GRID_HEIGHT)
                grid[en.getY()][en.getX()] = en.getSymbol();
        });
        System.out.println("+" + "-".repeat(GameConfig.GRID_WIDTH) + "+");
        for (char[] row : grid) { System.out.print("|"); for (char c : row) System.out.print(c); System.out.println("|"); }
        System.out.println("+" + "-".repeat(GameConfig.GRID_WIDTH) + "+");
        System.out.printf("Score: %d | Lives: %d | Level: %d | %s%n%n", score, player.getLives(), level, status);
    }

    public GameStatus getStatus() { return status; }
    public int        getScore()  { return score;  }
    public int        getLevel()  { return level;  }
}
