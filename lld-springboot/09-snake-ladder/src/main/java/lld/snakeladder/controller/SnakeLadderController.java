package lld.snakeladder.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.snakeladder.dto.*;
import lld.snakeladder.entity.Game;
import lld.snakeladder.service.SnakeLadderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/snake-ladder") @RequiredArgsConstructor
@Tag(name = "Snake & Ladder", description = "Classic board game with configurable players (2-6). Roll dice via API.")
public class SnakeLadderController {
    private static final Logger log = LoggerFactory.getLogger(SnakeLadderController.class);
    private final SnakeLadderService service;

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new game with 2-6 players")
    public Game createGame(@Valid @RequestBody CreateGameRequest req) {
        log.info("[SnakeLadderController] POST /games | players={}", req.getPlayers().size());
        return service.createGame(req);
    }

    @PostMapping("/games/{gameId}/turn")
    @Operation(summary = "Play one turn (rolls dice for current player)")
    public TurnResponse playTurn(@PathVariable Long gameId) {
        log.info("[SnakeLadderController] POST /games/{}/turn", gameId);
        return service.playTurn(gameId);
    }

    @GetMapping("/games/{gameId}")
    @Operation(summary = "Get current game state")
    public Game getGame(@PathVariable Long gameId) {
        return service.getGame(gameId);
    }
}
