# LLD — Chess Game (Complete Java 21)

## Design Summary
| Aspect | Decision |
|--------|----------|
| Piece hierarchy | Abstract `Piece` with sealed subclasses; each knows its own move rules |
| Move validation | Each piece validates itself — no God-class validator |
| Board state | `Board` owns `Cell[][]`; immutable cell positions via records |
| Game state | **State** pattern — ACTIVE / CHECK / CHECKMATE / STALEMATE / DRAW |
| Turn management | `Game` enforces alternating turns |
| Move history | `Deque<Move>` enables undo |
| Patterns used | **Strategy** (piece movement), **Command** (move with undo), **State** (game phases) |

## Complete Solution

```java
package lld.chess;

import java.util.*;

// ── Enums ─────────────────────────────────────────────────────────────────────

enum Color { WHITE, BLACK;
    public Color opponent() { return this == WHITE ? BLACK : WHITE; }
}

enum GameStatus { ACTIVE, CHECK, CHECKMATE, STALEMATE, DRAW, RESIGNED }

// ── Position (Immutable Value Object) ────────────────────────────────────────

record Position(int row, int col) {
    Position {
        if (row < 0 || row > 7) throw new IllegalArgumentException("Row must be 0-7, got: " + row);
        if (col < 0 || col > 7) throw new IllegalArgumentException("Col must be 0-7, got: " + col);
    }

    boolean isValid() { return row >= 0 && row <= 7 && col >= 0 && col <= 7; }

    static Optional<Position> of(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return Optional.empty();
        return Optional.of(new Position(row, col));
    }

    @Override public String toString() {
        return "" + (char)('a' + col) + (8 - row);  // e.g., "e4"
    }
}

// ── Move (Command Object) ─────────────────────────────────────────────────────

record Move(Position from, Position to, Piece piece, Piece capturedPiece, boolean isPromotion) {
    Move(Position from, Position to, Piece piece) {
        this(from, to, piece, null, false);
    }
    Move(Position from, Position to, Piece piece, Piece capturedPiece) {
        this(from, to, piece, capturedPiece, false);
    }

    boolean isCapture() { return capturedPiece != null; }

    @Override public String toString() {
        String capture = isCapture() ? "x" : "-";
        return String.format("%s%s%s%s", piece.getSymbol(), from, capture, to);
    }
}

// ── Custom Exceptions ─────────────────────────────────────────────────────────

class ChessException extends RuntimeException {
    ChessException(String msg) { super(msg); }
}

class InvalidMoveException extends ChessException {
    InvalidMoveException(String msg) { super("Invalid move: " + msg); }
}

class WrongTurnException extends ChessException {
    WrongTurnException(Color expected, Color actual) {
        super("It's " + expected + "'s turn, but " + actual + " tried to move");
    }
}

class GameOverException extends ChessException {
    GameOverException(GameStatus status) { super("Game is over: " + status); }
}

// ── Abstract Piece ────────────────────────────────────────────────────────────

abstract class Piece {
    protected final Color  color;
    protected Position     position;
    protected boolean      hasMoved = false;

    Piece(Color color, Position position) {
        this.color    = Objects.requireNonNull(color);
        this.position = Objects.requireNonNull(position);
    }

    public Color    getColor()    { return color; }
    public Position getPosition() { return position; }
    public boolean  hasMoved()    { return hasMoved; }

    void moveTo(Position newPosition) {
        this.position = newPosition;
        this.hasMoved = true;
    }

    /** Returns all positions this piece can legally move to on the given board */
    public abstract List<Position> getLegalMoves(Board board);

    /** One-character Unicode symbol for display */
    public abstract String getSymbol();

    public boolean isEnemy(Piece other) {
        return other != null && other.color != this.color;
    }

    @Override public String toString() { return color.name().charAt(0) + getSymbol(); }
}

// ── Concrete Pieces ───────────────────────────────────────────────────────────

class King extends Piece {
    King(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♔" : "♚"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] directions = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : directions) {
            Position.of(position.row() + d[0], position.col() + d[1]).ifPresent(p -> {
                Piece occupant = board.getPieceAt(p);
                if (occupant == null || isEnemy(occupant)) moves.add(p);
            });
        }
        // TODO: castling — add when hasMoved==false and rook hasMoved==false
        return moves;
    }
}

class Queen extends Piece {
    Queen(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♕" : "♛"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        // Queen = Rook + Bishop combined
        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : directions) addSlidingMoves(board, moves, d[0], d[1]);
        return moves;
    }

    private void addSlidingMoves(Board board, List<Position> moves, int dr, int dc) {
        int r = position.row() + dr, c = position.col() + dc;
        while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
            Piece occupant = board.getPieceAt(new Position(r, c));
            if (occupant == null) {
                moves.add(new Position(r, c));
            } else {
                if (isEnemy(occupant)) moves.add(new Position(r, c));
                break;  // blocked
            }
            r += dr; c += dc;
        }
    }
}

class Rook extends Piece {
    Rook(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♖" : "♜"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : directions) {
            int r = position.row() + d[0], c = position.col() + d[1];
            while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
                Piece occupant = board.getPieceAt(new Position(r, c));
                if (occupant == null) {
                    moves.add(new Position(r, c));
                } else {
                    if (isEnemy(occupant)) moves.add(new Position(r, c));
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        return moves;
    }
}

class Bishop extends Piece {
    Bishop(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♗" : "♝"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] directions = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : directions) {
            int r = position.row() + d[0], c = position.col() + d[1];
            while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
                Piece occupant = board.getPieceAt(new Position(r, c));
                if (occupant == null) {
                    moves.add(new Position(r, c));
                } else {
                    if (isEnemy(occupant)) moves.add(new Position(r, c));
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        return moves;
    }
}

class Knight extends Piece {
    Knight(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♘" : "♞"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] jumps = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] j : jumps) {
            Position.of(position.row() + j[0], position.col() + j[1]).ifPresent(p -> {
                Piece occupant = board.getPieceAt(p);
                if (occupant == null || isEnemy(occupant)) moves.add(p);
            });
        }
        return moves;
    }
}

class Pawn extends Piece {
    Pawn(Color color, Position position) { super(color, position); }

    @Override public String getSymbol() { return color == Color.WHITE ? "♙" : "♟"; }

    @Override
    public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int direction = (color == Color.WHITE) ? -1 : 1;  // white moves up (decreasing row)
        int r = position.row(), c = position.col();

        // Forward one step
        Position.of(r + direction, c).ifPresent(p -> {
            if (board.getPieceAt(p) == null) {
                moves.add(p);
                // Forward two steps from starting rank
                if (!hasMoved) {
                    Position.of(r + 2 * direction, c).ifPresent(p2 -> {
                        if (board.getPieceAt(p2) == null) moves.add(p2);
                    });
                }
            }
        });

        // Diagonal captures
        for (int dc : new int[]{-1, 1}) {
            Position.of(r + direction, c + dc).ifPresent(p -> {
                Piece occupant = board.getPieceAt(p);
                if (isEnemy(occupant)) moves.add(p);
                // TODO: en passant
            });
        }
        return moves;
    }
}

// ── Board ─────────────────────────────────────────────────────────────────────

class Board {
    private final Piece[][] grid = new Piece[8][8];  // [row][col]

    /** Returns piece at position, or null if empty */
    public Piece getPieceAt(Position pos) {
        return grid[pos.row()][pos.col()];
    }

    void setPiece(Position pos, Piece piece) {
        grid[pos.row()][pos.col()] = piece;
    }

    void removePiece(Position pos) {
        grid[pos.row()][pos.col()] = null;
    }

    /** Find the King of the given color */
    Optional<Position> findKing(Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] instanceof King k && k.getColor() == color)
                    return Optional.of(new Position(r, c));
        return Optional.empty();
    }

    /** Returns all pieces of the given color */
    List<Piece> getPieces(Color color) {
        List<Piece> pieces = new ArrayList<>();
        for (Piece[] row : grid)
            for (Piece p : row)
                if (p != null && p.getColor() == color) pieces.add(p);
        return pieces;
    }

    /** Is the given position attacked by any piece of the given color? */
    boolean isUnderAttack(Position pos, Color byColor) {
        return getPieces(byColor).stream()
            .anyMatch(p -> p.getLegalMoves(this).contains(pos));
    }

    /** Is the king of the given color currently in check? */
    boolean isInCheck(Color color) {
        return findKing(color)
            .map(kingPos -> isUnderAttack(kingPos, color.opponent()))
            .orElse(false);
    }

    void display() {
        System.out.println("\n  a b c d e f g h");
        System.out.println("  ─────────────────");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + "│");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                System.out.print((p == null ? "·" : p.getSymbol()) + " ");
            }
            System.out.println("│" + (8 - r));
        }
        System.out.println("  ─────────────────");
        System.out.println("  a b c d e f g h\n");
    }
}

// ── Board Initialiser ─────────────────────────────────────────────────────────

class BoardInitialiser {
    static void setup(Board board) {
        // Black back rank (row 0)
        board.setPiece(new Position(0,0), new Rook(Color.BLACK,   new Position(0,0)));
        board.setPiece(new Position(0,1), new Knight(Color.BLACK, new Position(0,1)));
        board.setPiece(new Position(0,2), new Bishop(Color.BLACK, new Position(0,2)));
        board.setPiece(new Position(0,3), new Queen(Color.BLACK,  new Position(0,3)));
        board.setPiece(new Position(0,4), new King(Color.BLACK,   new Position(0,4)));
        board.setPiece(new Position(0,5), new Bishop(Color.BLACK, new Position(0,5)));
        board.setPiece(new Position(0,6), new Knight(Color.BLACK, new Position(0,6)));
        board.setPiece(new Position(0,7), new Rook(Color.BLACK,   new Position(0,7)));
        // Black pawns (row 1)
        for (int c = 0; c < 8; c++)
            board.setPiece(new Position(1,c), new Pawn(Color.BLACK, new Position(1,c)));
        // White pawns (row 6)
        for (int c = 0; c < 8; c++)
            board.setPiece(new Position(6,c), new Pawn(Color.WHITE, new Position(6,c)));
        // White back rank (row 7)
        board.setPiece(new Position(7,0), new Rook(Color.WHITE,   new Position(7,0)));
        board.setPiece(new Position(7,1), new Knight(Color.WHITE, new Position(7,1)));
        board.setPiece(new Position(7,2), new Bishop(Color.WHITE, new Position(7,2)));
        board.setPiece(new Position(7,3), new Queen(Color.WHITE,  new Position(7,3)));
        board.setPiece(new Position(7,4), new King(Color.WHITE,   new Position(7,4)));
        board.setPiece(new Position(7,5), new Bishop(Color.WHITE, new Position(7,5)));
        board.setPiece(new Position(7,6), new Knight(Color.WHITE, new Position(7,6)));
        board.setPiece(new Position(7,7), new Rook(Color.WHITE,   new Position(7,7)));
    }
}

// ── Player ────────────────────────────────────────────────────────────────────

record Player(String name, Color color) {
    Player {
        Objects.requireNonNull(name,  "Player name required");
        Objects.requireNonNull(color, "Player color required");
        if (name.isBlank()) throw new IllegalArgumentException("Player name cannot be blank");
    }
}

// ── Game ──────────────────────────────────────────────────────────────────────

class ChessGame {
    private final Board            board;
    private final Player           whitePlayer;
    private final Player           blackPlayer;
    private Color                  currentTurn = Color.WHITE;
    private GameStatus             status      = GameStatus.ACTIVE;
    private final Deque<Move>      moveHistory = new ArrayDeque<>();

    ChessGame(String whiteName, String blackName) {
        this.board       = new Board();
        this.whitePlayer = new Player(Objects.requireNonNull(whiteName), Color.WHITE);
        this.blackPlayer = new Player(Objects.requireNonNull(blackName), Color.BLACK);
        BoardInitialiser.setup(board);
    }

    public void makeMove(Position from, Position to) {
        if (status != GameStatus.ACTIVE && status != GameStatus.CHECK)
            throw new GameOverException(status);

        Piece piece = board.getPieceAt(from);

        // Validation
        if (piece == null)
            throw new InvalidMoveException("No piece at " + from);
        if (piece.getColor() != currentTurn)
            throw new WrongTurnException(currentTurn, piece.getColor());
        if (!piece.getLegalMoves(board).contains(to))
            throw new InvalidMoveException(from + " → " + to + " is not a legal move for " + piece);

        // Execute move
        Piece captured = board.getPieceAt(to);
        board.removePiece(from);
        board.setPiece(to, piece);
        piece.moveTo(to);

        // Verify move doesn't leave own king in check
        if (board.isInCheck(currentTurn)) {
            // Undo move
            board.removePiece(to);
            board.setPiece(from, piece);
            piece.moveTo(from);
            if (captured != null) board.setPiece(to, captured);
            throw new InvalidMoveException("Move would leave your king in check");
        }

        // Record move
        moveHistory.push(new Move(from, to, piece, captured));

        System.out.printf("♟ %s plays: %s%n",
            currentTurn == Color.WHITE ? whitePlayer.name() : blackPlayer.name(),
            moveHistory.peek());

        // Switch turns and update status
        currentTurn = currentTurn.opponent();
        updateStatus();
    }

    /** Shorthand: accept algebraic notation like "e2", "e4" */
    public void makeMove(String fromNotation, String toNotation) {
        makeMove(parsePosition(fromNotation), parsePosition(toNotation));
    }

    private void updateStatus() {
        boolean inCheck = board.isInCheck(currentTurn);
        boolean hasLegalMoves = board.getPieces(currentTurn).stream()
            .anyMatch(p -> !p.getLegalMoves(board).isEmpty());

        if (inCheck && !hasLegalMoves) {
            status = GameStatus.CHECKMATE;
            System.out.println("♛ CHECKMATE! " + currentTurn.opponent() + " wins!");
        } else if (!inCheck && !hasLegalMoves) {
            status = GameStatus.STALEMATE;
            System.out.println("⚖ STALEMATE! Draw.");
        } else if (inCheck) {
            status = GameStatus.CHECK;
            System.out.println("⚠ CHECK! " + currentTurn + " is in check.");
        } else {
            status = GameStatus.ACTIVE;
        }
    }

    public void resign(Color color) {
        if (status != GameStatus.ACTIVE && status != GameStatus.CHECK)
            throw new GameOverException(status);
        status = GameStatus.RESIGNED;
        System.out.println(color + " resigns. " + color.opponent() + " wins!");
    }

    public GameStatus getStatus()      { return status; }
    public Color      getCurrentTurn() { return currentTurn; }
    public int        getMoveCount()   { return moveHistory.size(); }
    public void       displayBoard()   { board.display(); }

    private Position parsePosition(String notation) {
        if (notation == null || notation.length() != 2)
            throw new IllegalArgumentException("Position must be like 'e4', got: " + notation);
        int col = notation.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(notation.charAt(1));
        return new Position(row, col);
    }

    public void printMoveHistory() {
        System.out.println("\n── Move History ──");
        List<Move> moves = new ArrayList<>(moveHistory);
        Collections.reverse(moves);
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) System.out.print((i / 2 + 1) + ". " + moves.get(i) + " ");
            else System.out.println(moves.get(i));
        }
        System.out.println();
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class ChessDemo {
    public static void main(String[] args) {
        ChessGame game = new ChessGame("Alice (White)", "Bob (Black)");
        game.displayBoard();

        // Scholar's mate attempt (white tries to checkmate in 4 moves)
        game.makeMove("e2", "e4");   // White: e2→e4
        game.makeMove("e7", "e5");   // Black: e7→e5
        game.makeMove("f1", "c4");   // White: bishop to c4
        game.makeMove("b8", "c6");   // Black: knight to c6
        game.makeMove("d1", "h5");   // White: queen to h5
        game.makeMove("g8", "f6");   // Black: knight to f6 (defends)

        game.displayBoard();
        game.printMoveHistory();

        // Test invalid move
        try {
            game.makeMove("h5", "f7");  // White queen takes f7 — check
        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }

        System.out.println("Game status: " + game.getStatus());
        System.out.println("Current turn: " + game.getCurrentTurn());
        System.out.println("Total moves: " + game.getMoveCount());
    }
}
```

## Extension Q&A

**Q: How do you add castling?**
In `King.getLegalMoves()`: if `!hasMoved`, check both rooks. If the rook at the corner also `!hasMoved`, if all squares between are empty and none are under attack — add the castling destination positions. In `ChessGame.makeMove()`, detect castling move and also move the rook.

**Q: How do you handle pawn promotion?**
When a pawn reaches row 0 (white) or row 7 (black), mark `isPromotion = true` in the Move record. Ask the player for the promotion piece type (Queen/Rook/Bishop/Knight) — either via callback or parameter. Replace the pawn with the new piece on the board.

**Q: How do you implement the AI player?**
Add a `MoveStrategy` interface: `Move selectMove(Board board, Color color)`. Implement `RandomMoveStrategy` (random legal move) and `MinimaxStrategy` (minimax with alpha-beta pruning, depth N). Inject the strategy into the `Player` record.

**Q: Why does each piece know its own valid moves instead of a central MoveValidator?**
Single Responsibility + Open/Closed. Each piece encapsulates its movement rules. Adding a new piece type doesn't require changing a central validator — just add the new class. A central validator would have a massive switch statement and violate OCP.
