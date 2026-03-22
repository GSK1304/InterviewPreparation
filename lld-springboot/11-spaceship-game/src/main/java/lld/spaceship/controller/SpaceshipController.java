package lld.spaceship.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.spaceship.dto.*;
import lld.spaceship.entity.SpaceGame;
import lld.spaceship.enums.PowerUpType;
import lld.spaceship.service.SpaceshipService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/v1/spaceship") @RequiredArgsConstructor
@Tag(name = "Spaceship Game", description = "Space shooter with Observer events, wave progression, AABB collision, power-ups")
public class SpaceshipController {
    private static final Logger log = LoggerFactory.getLogger(SpaceshipController.class);
    private final SpaceshipService service;

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new game")
    public SpaceGame createGame(@Valid @RequestBody CreateGameRequest req) {
        log.info("[SpaceshipController] POST /games | player={}", req.getPlayerName());
        return service.createGame(req);
    }

    @PostMapping("/games/{gameId}/move")
    @Operation(summary = "Move ship and optionally fire", description = "deltaX: pixels to move left (negative) or right (positive). fire=true shoots a bullet.")
    public GameStateResponse move(@PathVariable Long gameId, @Valid @RequestBody MoveRequest req) {
        log.debug("[SpaceshipController] POST /games/{}/move | deltaX={} fire={}", gameId, req.getDeltaX(), req.isFire());
        return service.move(gameId, req);
    }

    @PostMapping("/games/{gameId}/power-up/{type}")
    @Operation(summary = "Activate a power-up", description = "Types: SHIELD, RAPID_FIRE, EXTRA_LIFE")
    public GameStateResponse activatePowerUp(@PathVariable Long gameId, @PathVariable PowerUpType type) {
        log.info("[SpaceshipController] POST /games/{}/power-up/{}", gameId, type);
        return service.activatePowerUp(gameId, type);
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Top 10 scores")
    public List<SpaceGame> getLeaderboard() { return service.getLeaderboard(); }
}
