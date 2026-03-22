package lld.chess.service;

import jakarta.transaction.Transactional;
import lld.chess.dto.*;
import lld.chess.entity.*;
import lld.chess.enums.*;
import lld.chess.exception.ChessException;
import lld.chess.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class ChessService {
    private static final Logger log = LoggerFactory.getLogger(ChessService.class);
    private final ChessGameRepository gameRepo;
    private final ChessMoveRepository moveRepo;

    @Transactional
    public ChessGame createGame(CreateGameRequest req) {
        log.info("[ChessService] New game | white={} black={}", req.getWhitePlayer(), req.getBlackPlayer());
        ChessGame game = new ChessGame();
        game.setWhitePlayer(req.getWhitePlayer());
        game.setBlackPlayer(req.getBlackPlayer());
        game.setBoardState(BoardModel.initial().serialize());
        gameRepo.save(game);
        log.info("[ChessService] Game created | id={}", game.getId());
        return game;
    }

    @Transactional
    public MoveResponse makeMove(Long gameId, MakeMoveRequest req) {
        log.info("[ChessService] Move | game={} player={} from=({},{}) to=({},{})",
            gameId, req.getPlayerName(), req.getFromCol(), req.getFromRow(), req.getToCol(), req.getToRow());

        ChessGame game = getGame(gameId);
        if (game.getStatus() != GameStatus.ACTIVE)
            throw new ChessException("Game is not active: " + game.getStatus(), HttpStatus.CONFLICT);

        // Validate player turn
        boolean isWhite = req.getPlayerName().equals(game.getWhitePlayer());
        boolean isBlack = req.getPlayerName().equals(game.getBlackPlayer());
        if (!isWhite && !isBlack)
            throw new ChessException("Player not in this game: " + req.getPlayerName(), HttpStatus.FORBIDDEN);
        PieceColor playerColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;
        if (playerColor != game.getCurrentTurn())
            throw new ChessException("Not your turn. Current turn: " + game.getCurrentTurn(), HttpStatus.CONFLICT);

        BoardModel board = BoardModel.parse(game.getBoardState());

        if (!board.isValidMove(req.getFromCol(), req.getFromRow(), req.getToCol(), req.getToRow(), playerColor))
            throw new ChessException("Invalid move for " + playerColor + " piece at (" + req.getFromCol() + "," + req.getFromRow() + ")", HttpStatus.BAD_REQUEST);

        BoardModel.Piece moving  = board.get(req.getFromCol(), req.getFromRow());
        BoardModel.Piece captured = board.get(req.getToCol(), req.getToRow());

        // Execute move
        board.remove(req.getFromCol(), req.getFromRow());
        board.place(moving.color(), moving.type(), req.getToCol(), req.getToRow());

        // Check if move leaves own king in check
        if (board.isInCheck(playerColor)) {
            throw new ChessException("Move leaves your King in check", HttpStatus.BAD_REQUEST);
        }

        PieceColor opponent = playerColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
        boolean isCheck     = board.isInCheck(opponent);
        boolean isCheckmate = false;
        GameStatus newStatus = GameStatus.ACTIVE;

        if (isCheck) {
            // Simple checkmate detection: opponent has no valid escape
            isCheckmate = hasNoLegalMoves(board, opponent);
            if (isCheckmate) newStatus = playerColor == PieceColor.WHITE ? GameStatus.WHITE_WINS : GameStatus.BLACK_WINS;
        }

        // Persist
        game.setBoardState(board.serialize());
        game.setTotalMoves(game.getTotalMoves() + 1);
        game.setCurrentTurn(opponent);
        game.setStatus(newStatus);
        gameRepo.save(game);

        String notation = algebraic(moving.type(), req.getFromCol(), req.getFromRow(), req.getToCol(), req.getToRow(), captured != null, isCheck, isCheckmate);

        ChessMove move = new ChessMove();
        move.setGame(game); move.setMoveNumber(game.getTotalMoves());
        move.setColor(playerColor); move.setPieceType(moving.type());
        move.setFromCol(req.getFromCol()); move.setFromRow(req.getFromRow());
        move.setToCol(req.getToCol());   move.setToRow(req.getToRow());
        move.setCapturedPiece(captured != null ? captured.toString() : null);
        move.setIsCheck(isCheck); move.setIsCheckmate(isCheckmate);
        move.setAlgebraicNotation(notation);
        moveRepo.save(move);

        log.info("[ChessService] Move executed | {} {} {} check={} checkmate={}", playerColor, moving.type(), notation, isCheck, isCheckmate);

        return MoveResponse.builder()
            .playerName(req.getPlayerName()).piece(moving.type().name())
            .from(colName(req.getFromCol()) + (req.getFromRow()+1))
            .to(colName(req.getToCol()) + (req.getToRow()+1))
            .capturedPiece(captured != null ? captured.toString() : null)
            .isCheck(isCheck).isCheckmate(isCheckmate)
            .gameStatus(newStatus).nextTurn(isCheckmate ? null : opponent)
            .totalMoves(game.getTotalMoves()).algebraicNotation(notation)
            .message(isCheckmate ? req.getPlayerName() + " wins by checkmate!" : isCheck ? "Check!" : "Move accepted")
            .build();
    }

    public List<ChessMove> getMoveHistory(Long gameId) {
        getGame(gameId);
        return moveRepo.findByGameIdOrderByMoveNumberAsc(gameId);
    }

    public ChessGame getGame(Long id) {
        return gameRepo.findById(id).orElseThrow(() -> new ChessException("Game not found: " + id, HttpStatus.NOT_FOUND));
    }

    private boolean hasNoLegalMoves(BoardModel board, PieceColor color) {
        for (int fc=0;fc<8;fc++) for (int fr=0;fr<8;fr++) {
            BoardModel.Piece p = board.get(fc,fr);
            if (p==null||p.color()!=color) continue;
            for (int tc=0;tc<8;tc++) for (int tr=0;tr<8;tr++) {
                if (!board.isValidMove(fc,fr,tc,tr,color)) continue;
                BoardModel.Piece cap = board.get(tc,tr);
                board.remove(fc,fr); board.place(color,p.type(),tc,tr);
                boolean inCheck = board.isInCheck(color);
                board.remove(tc,tr); board.place(color,p.type(),fc,fr);
                if (cap!=null) board.place(cap.color(),cap.type(),tc,tr);
                if (!inCheck) return false;
            }
        }
        return true;
    }

    private String colName(int c) { return String.valueOf((char)('a'+c)); }
    private String algebraic(lld.chess.enums.PieceType type, int fc, int fr, int tc, int tr, boolean capture, boolean check, boolean mate) {
        String prefix = type == lld.chess.enums.PieceType.PAWN ? "" : type.name().substring(0,1);
        String cap    = capture ? "x" : "";
        String dest   = colName(tc) + (tr+1);
        String suffix = mate ? "#" : check ? "+" : "";
        return prefix + cap + dest + suffix;
    }
}
