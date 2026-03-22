package lld.chess.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.chess.dto.*;
import lld.chess.entity.*;
import lld.chess.service.ChessService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/v1/chess") @RequiredArgsConstructor
@Tag(name = "Chess Game", description = "Two-player chess with move validation, check/checkmate detection and algebraic notation")
public class ChessController {
    private static final Logger log = LoggerFactory.getLogger(ChessController.class);
    private final ChessService service;

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new chess game")
    public ChessGame createGame(@Valid @RequestBody CreateGameRequest req) {
        log.info("[ChessController] POST /games | white={} black={}", req.getWhitePlayer(), req.getBlackPlayer());
        return service.createGame(req);
    }

    @PostMapping("/games/{gameId}/moves")
    @Operation(summary = "Make a move", description = "Validates piece movement, checks for check/checkmate, records in algebraic notation")
    public MoveResponse makeMove(@PathVariable Long gameId, @Valid @RequestBody MakeMoveRequest req) {
        log.info("[ChessController] POST /games/{}/moves | player={} from=({},{}) to=({},{})",
            gameId, req.getPlayerName(), req.getFromCol(), req.getFromRow(), req.getToCol(), req.getToRow());
        return service.makeMove(gameId, req);
    }

    @GetMapping("/games/{gameId}/moves")
    @Operation(summary = "Get move history with algebraic notation")
    public List<ChessMove> getMoveHistory(@PathVariable Long gameId) {
        return service.getMoveHistory(gameId);
    }

    @GetMapping("/games/{gameId}")
    @Operation(summary = "Get game state")
    public ChessGame getGame(@PathVariable Long gameId) {
        return service.getGame(gameId);
    }
}
