package lld.snakeladder.service;
import jakarta.transaction.Transactional;
import lld.snakeladder.dto.*;
import lld.snakeladder.entity.*;
import lld.snakeladder.enums.*;
import lld.snakeladder.exception.GameException;
import lld.snakeladder.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class SnakeLadderService {
    private static final Logger log = LoggerFactory.getLogger(SnakeLadderService.class);
    private final GameRepository gameRepo;
    private final Random rng = new Random();

    @Transactional
    public Game createGame(CreateGameRequest req) {
        log.info("[SnakeLadderService] Creating game | players={}", req.getPlayers().stream().map(CreateGameRequest.PlayerInfo::getName).collect(Collectors.joining(",")));
        Game game = new Game();
        List<GamePlayer> players = new ArrayList<>();
        for (int i = 0; i < req.getPlayers().size(); i++) {
            GamePlayer p = new GamePlayer(); p.setGame(game);
            p.setPlayerName(req.getPlayers().get(i).getName()); p.setPlayerToken(req.getPlayers().get(i).getToken());
            p.setPlayerIndex(i); players.add(p);
        }
        game.setPlayers(players);
        setupBoard(game);
        game.setStatus(GameStatus.ACTIVE);
        gameRepo.save(game);
        log.info("[SnakeLadderService] Game created | id={} players={}", game.getId(), players.size());
        return game;
    }

    @Transactional
    public TurnResponse playTurn(Long gameId) {
        Game game = getGame(gameId);
        if (game.getStatus() != GameStatus.ACTIVE) throw new GameException("Game is not active: " + game.getStatus(), HttpStatus.CONFLICT);
        GamePlayer current = game.getPlayers().get(game.getCurrentPlayerIndex());
        int dice = rng.nextInt(6) + 1;
        int from = current.getPosition();
        int to   = Math.min(from + dice, game.getBoardSize());
        current.setTurnsPlayed(current.getTurnsPlayed() + 1);
        game.setTotalTurns(game.getTotalTurns() + 1);
        log.info("[SnakeLadderService] Turn | player={} dice={} from={} to={}", current.getPlayerName(), dice, from, to);

        String event = "Moved to " + to;
        BoardCell cell = game.getCells().stream().filter(c -> c.getPosition() == to).findFirst().orElse(null);
        if (cell != null) {
            if (cell.getType() == CellType.SNAKE_HEAD) {
                to = cell.getTargetPosition(); current.setSnakeBites(current.getSnakeBites()+1);
                event = "SNAKE! Slid from " + cell.getPosition() + " to " + to;
                log.info("[SnakeLadderService] Snake bite | player={} from={} to={}", current.getPlayerName(), cell.getPosition(), to);
            } else if (cell.getType() == CellType.LADDER_BOTTOM) {
                to = cell.getTargetPosition(); current.setLaddersClimbed(current.getLaddersClimbed()+1);
                event = "LADDER! Climbed from " + cell.getPosition() + " to " + to;
                log.info("[SnakeLadderService] Ladder climb | player={} from={} to={}", current.getPlayerName(), cell.getPosition(), to);
            }
        }
        current.setPosition(to);

        boolean gameOver = to >= game.getBoardSize();
        String winner = null;
        if (gameOver) {
            game.setStatus(GameStatus.FINISHED); game.setWinnerName(current.getPlayerName());
            winner = current.getPlayerName();
            log.info("[SnakeLadderService] Game over! Winner={} totalTurns={}", winner, game.getTotalTurns());
        } else {
            boolean bonusTurn = dice == 6;
            if (!bonusTurn) game.setCurrentPlayerIndex((game.getCurrentPlayerIndex()+1) % game.getPlayers().size());
        }
        gameRepo.save(game);
        return TurnResponse.builder().playerName(current.getPlayerName()).token(current.getPlayerToken())
            .diceRoll(dice).fromPosition(from).toPosition(to).event(event).gameOver(gameOver).winner(winner)
            .message(gameOver ? current.getPlayerName() + " wins!" : "Next: " + game.getPlayers().get(game.getCurrentPlayerIndex()).getPlayerName()).build();
    }

    private void setupBoard(Game game) {
        // Classic snake and ladder positions
        Map<Integer,Integer> snakes  = Map.of(99,5, 70,55, 52,42, 36,6, 28,8);
        Map<Integer,Integer> ladders = Map.of(4,56, 12,50, 14,55, 22,58, 41,79, 54,88, 62,96);
        List<BoardCell> cells = new ArrayList<>();
        snakes.forEach((head,tail) -> { BoardCell c = new BoardCell(); c.setGame(game); c.setPosition(head); c.setType(CellType.SNAKE_HEAD); c.setTargetPosition(tail); cells.add(c); });
        ladders.forEach((bot,top) -> { BoardCell c = new BoardCell(); c.setGame(game); c.setPosition(bot); c.setType(CellType.LADDER_BOTTOM); c.setTargetPosition(top); cells.add(c); });
        game.setCells(cells);
    }

    private Game getGame(Long id) { return gameRepo.findById(id).orElseThrow(() -> new GameException("Game not found: " + id, HttpStatus.NOT_FOUND)); }
}
// expose getGame for controller
public Game getGame(Long id) { return gameRepo.findById(id).orElseThrow(() -> new GameException("Game not found: " + id, HttpStatus.NOT_FOUND)); }
