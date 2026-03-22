package lld.chess;
import java.util.*;

public class ChessGame {
    private final Board         board;
    private final Player        whitePlayer;
    private final Player        blackPlayer;
    private Color               currentTurn = Color.WHITE;
    private GameStatus          status      = GameStatus.ACTIVE;
    private final Deque<Move>   moveHistory = new ArrayDeque<>();

    public ChessGame(String whiteName, String blackName) {
        this.board       = new Board();
        this.whitePlayer = new Player(Objects.requireNonNull(whiteName), Color.WHITE);
        this.blackPlayer = new Player(Objects.requireNonNull(blackName), Color.BLACK);
        BoardInitialiser.setup(board);
    }

    public void makeMove(String from, String to) { makeMove(parse(from), parse(to)); }

    public void makeMove(Position from, Position to) {
        if (status != GameStatus.ACTIVE && status != GameStatus.CHECK)
            throw new GameOverException(status);

        Piece piece = board.getPieceAt(from);
        if (piece == null)                        throw new InvalidMoveException("No piece at " + from);
        if (piece.getColor() != currentTurn)      throw new WrongTurnException(currentTurn, piece.getColor());
        if (!piece.getLegalMoves(board).contains(to))
            throw new InvalidMoveException(from + " to " + to + " not legal for " + piece.getSymbol());

        Piece captured = board.getPieceAt(to);
        board.removePiece(from);
        board.setPiece(to, piece);
        piece.moveTo(to);

        if (board.isInCheck(currentTurn)) {
            // Undo
            board.removePiece(to); board.setPiece(from, piece); piece.moveTo(from);
            if (captured != null) board.setPiece(to, captured);
            throw new InvalidMoveException("Move leaves own king in check");
        }

        moveHistory.push(new Move(from, to, piece, captured));
        String moverName = currentTurn == Color.WHITE ? whitePlayer.name() : blackPlayer.name();
        System.out.printf("  %s plays: %s%n", moverName, moveHistory.peek());

        currentTurn = currentTurn.opponent();
        updateStatus();
    }

    private void updateStatus() {
        boolean inCheck     = board.isInCheck(currentTurn);
        boolean hasLegal    = board.getPieces(currentTurn).stream()
            .anyMatch(p -> !p.getLegalMoves(board).isEmpty());
        if (inCheck && !hasLegal)  { status = GameStatus.CHECKMATE; System.out.println("CHECKMATE! " + currentTurn.opponent() + " wins!"); }
        else if (!inCheck && !hasLegal) { status = GameStatus.STALEMATE; System.out.println("STALEMATE! Draw."); }
        else if (inCheck)          { status = GameStatus.CHECK; System.out.println("CHECK! " + currentTurn + " is in check."); }
        else                       { status = GameStatus.ACTIVE; }
    }

    public void resign(Color color) {
        if (status != GameStatus.ACTIVE && status != GameStatus.CHECK) throw new GameOverException(status);
        status = GameStatus.RESIGNED;
        System.out.println(color + " resigns. " + color.opponent() + " wins!");
    }

    private Position parse(String n) {
        if (n == null || n.length() != 2) throw new IllegalArgumentException("Position must be like 'e4'");
        return new Position(8 - Character.getNumericValue(n.charAt(1)), n.charAt(0) - 'a');
    }

    public void displayBoard() { board.display(); }
    public GameStatus getStatus()      { return status; }
    public Color      getCurrentTurn() { return currentTurn; }
    public int        getMoveCount()   { return moveHistory.size(); }
}
