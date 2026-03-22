package lld.spaceship.service;

import jakarta.transaction.Transactional;
import lld.spaceship.dto.*;
import lld.spaceship.entity.*;
import lld.spaceship.enums.*;
import lld.spaceship.exception.SpaceException;
import lld.spaceship.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class SpaceshipService {
    private static final Logger log = LoggerFactory.getLogger(SpaceshipService.class);

    // In-memory enemy state per game (enemies don't persist between HTTP calls)
    private final Map<Long, List<int[]>> enemyPositions = new ConcurrentHashMap<>(); // [x, y, health, type_ordinal]
    private final Map<Long, List<int[]>> bullets        = new ConcurrentHashMap<>(); // [x, y]

    private static final int BOARD_WIDTH = 800, BOARD_HEIGHT = 600;
    private static final Map<EnemyType, Integer> ENEMY_HEALTH = Map.of(EnemyType.BASIC,1,EnemyType.FAST,1,EnemyType.TANK,3);
    private static final Map<EnemyType, Integer> ENEMY_SCORE  = Map.of(EnemyType.BASIC,10,EnemyType.FAST,20,EnemyType.TANK,50);

    private final SpaceGameRepository  gameRepo;
    private final GameEventRepository  eventRepo;

    @Transactional
    public SpaceGame createGame(CreateGameRequest req) {
        log.info("[SpaceshipService] New game | player={}", req.getPlayerName());
        SpaceGame game = new SpaceGame();
        game.setPlayerName(req.getPlayerName());
        gameRepo.save(game);
        spawnWave(game.getId(), game.getWaveNumber());
        log.info("[SpaceshipService] Game created | id={} wave=1 enemies={}", game.getId(), enemyPositions.getOrDefault(game.getId(), List.of()).size());
        return game;
    }

    @Transactional
    public GameStateResponse move(Long gameId, MoveRequest req) {
        SpaceGame game = getGame(gameId);
        if (game.getStatus() != GameStatus.ACTIVE)
            throw new SpaceException("Game is not active: " + game.getStatus(), HttpStatus.CONFLICT);

        // Move player
        int newX = Math.max(0, Math.min(BOARD_WIDTH, game.getPlayerX() + req.getDeltaX()));
        game.setPlayerX(newX);
        log.debug("[SpaceshipService] Player moved | gameId={} x={}", gameId, newX);

        // Fire bullet
        if (req.isFire()) {
            List<int[]> bs = bullets.computeIfAbsent(gameId, k -> new ArrayList<>());
            bs.add(new int[]{game.getPlayerX(), game.getPlayerY() - 20});
            log.debug("[SpaceshipService] Bullet fired | gameId={} from=({},{})", gameId, game.getPlayerX(), game.getPlayerY());
        }

        // Advance bullets
        List<int[]> bs = bullets.computeIfAbsent(gameId, k -> new ArrayList<>());
        bs.replaceAll(b -> new int[]{b[0], b[1] - 15});
        bs.removeIf(b -> b[1] < 0);

        // Advance enemies
        List<int[]> enemies = enemyPositions.computeIfAbsent(gameId, k -> new ArrayList<>());
        int speed = 2 + game.getWaveNumber();
        enemies.replaceAll(e -> new int[]{e[0], e[1] + speed, e[2], e[3]});

        // Collision: bullet hits enemy (AABB)
        List<int[]> toRemoveBullets = new ArrayList<>();
        Iterator<int[]> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            int[] enemy = enemyIt.next();
            Iterator<int[]> bulletIt = bs.iterator();
            while (bulletIt.hasNext()) {
                int[] bullet = bulletIt.next();
                if (Math.abs(bullet[0]-enemy[0]) < 25 && Math.abs(bullet[1]-enemy[1]) < 25) {
                    enemy[2]--;
                    bulletIt.remove();
                    if (enemy[2] <= 0) {
                        EnemyType type = EnemyType.values()[enemy[3]];
                        int pts = ENEMY_SCORE.getOrDefault(type, 10);
                        game.setScore(game.getScore() + pts);
                        game.setEnemiesKilled(game.getEnemiesKilled() + 1);
                        enemyIt.remove();
                        log.info("[SpaceshipService] Enemy killed | type={} pts={} totalScore={}", type, pts, game.getScore());
                        recordEvent(gameId, "ENEMY_KILLED", type + " destroyed, +" + pts + " pts");
                        break;
                    }
                }
            }
        }

        // Enemy reaches bottom — lose a life
        Iterator<int[]> bottomIt = enemies.iterator();
        while (bottomIt.hasNext()) {
            int[] enemy = bottomIt.next();
            if (enemy[1] >= BOARD_HEIGHT) {
                game.setLives(game.getLives() - 1);
                bottomIt.remove();
                log.warn("[SpaceshipService] Enemy reached bottom | lives={}", game.getLives());
                recordEvent(gameId, "LIFE_LOST", "Enemy reached bottom, lives=" + game.getLives());
                if (game.getLives() <= 0) {
                    game.setStatus(GameStatus.GAME_OVER);
                    log.info("[SpaceshipService] GAME OVER | player={} score={}", game.getPlayerName(), game.getScore());
                }
            }
        }

        // Wave complete
        if (enemies.isEmpty() && game.getStatus() == GameStatus.ACTIVE) {
            game.setWaveNumber(game.getWaveNumber() + 1);
            spawnWave(gameId, game.getWaveNumber());
            log.info("[SpaceshipService] Wave {} started | gameId={}", game.getWaveNumber(), gameId);
            recordEvent(gameId, "WAVE_STARTED", "Wave " + game.getWaveNumber() + " begins");
        }

        game.setLastUpdated(Instant.now());
        gameRepo.save(game);

        return buildState(game, enemies);
    }

    @Transactional
    public GameStateResponse activatePowerUp(Long gameId, PowerUpType type) {
        SpaceGame game = getGame(gameId);
        if (game.getStatus() != GameStatus.ACTIVE) throw new SpaceException("Game not active", HttpStatus.CONFLICT);
        switch (type) {
            case SHIELD     -> { game.setShieldActive(true);  log.info("[SpaceshipService] Shield activated | gameId={}", gameId); }
            case RAPID_FIRE -> { game.setRapidFire(true);     log.info("[SpaceshipService] Rapid fire activated | gameId={}", gameId); }
            case EXTRA_LIFE -> { game.setLives(game.getLives() + 1); log.info("[SpaceshipService] Extra life | lives={}", game.getLives()); }
        }
        recordEvent(gameId, "POWER_UP", type + " activated");
        gameRepo.save(game);
        return buildState(game, enemyPositions.getOrDefault(gameId, List.of()));
    }

    public List<SpaceGame> getLeaderboard() {
        return gameRepo.findLeaderboard(PageRequest.of(0, 10));
    }

    private void spawnWave(Long gameId, int wave) {
        List<int[]> enemies = new ArrayList<>();
        int count = 5 + (wave - 1) * 2;
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            EnemyType type = wave >= 3 && i % 5 == 0 ? EnemyType.TANK : wave >= 2 && i % 3 == 0 ? EnemyType.FAST : EnemyType.BASIC;
            enemies.add(new int[]{rng.nextInt(BOARD_WIDTH), 20 + (i / 5) * 60, ENEMY_HEALTH.get(type), type.ordinal()});
        }
        enemyPositions.put(gameId, enemies);
        log.info("[SpaceshipService] Wave {} spawned | enemies={}", wave, enemies.size());
    }

    private GameStateResponse buildState(SpaceGame game, List<int[]> enemies) {
        List<GameStateResponse.EnemyState> enemyStates = enemies.stream().map(e ->
            GameStateResponse.EnemyState.builder().x(e[0]).y(e[1]).health(e[2]).type(EnemyType.values()[e[3]].name()).build()
        ).collect(Collectors.toList());
        return GameStateResponse.builder()
            .gameId(game.getId()).playerName(game.getPlayerName()).status(game.getStatus())
            .score(game.getScore()).lives(game.getLives()).playerX(game.getPlayerX()).playerY(game.getPlayerY())
            .shieldActive(game.getShieldActive()).rapidFire(game.getRapidFire())
            .waveNumber(game.getWaveNumber()).enemiesKilled(game.getEnemiesKilled())
            .enemies(enemyStates).message(game.getStatus() == GameStatus.GAME_OVER ? "Game Over! Score: " + game.getScore() : "Wave " + game.getWaveNumber())
            .build();
    }

    private void recordEvent(Long gameId, String type, String details) {
        GameEvent e = new GameEvent(); e.setGameId(gameId); e.setEventType(type); e.setDetails(details);
        eventRepo.save(e);
    }

    private SpaceGame getGame(Long id) {
        return gameRepo.findById(id).orElseThrow(() -> new SpaceException("Game not found: " + id, HttpStatus.NOT_FOUND));
    }
}
