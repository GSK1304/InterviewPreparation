package lld.chess;
import java.util.List;
import java.util.Objects;

public abstract class Piece {
    protected final Color color;
    protected Position    position;
    protected boolean     hasMoved = false;

    public Piece(Color color, Position position) {
        this.color    = Objects.requireNonNull(color);
        this.position = Objects.requireNonNull(position);
    }

    public abstract List<Position> getLegalMoves(Board board);
    public abstract String getSymbol();

    public Color    getColor()    { return color; }
    public Position getPosition() { return position; }
    public boolean  hasMoved()    { return hasMoved; }

    public void moveTo(Position p) { this.position = p; this.hasMoved = true; }

    public boolean isEnemy(Piece other) { return other != null && other.color != this.color; }

    /** Add sliding moves (for Rook, Bishop, Queen) */
    protected void addSlidingMoves(Board board, List<Position> moves, int dr, int dc) {
        int r = position.row() + dr, c = position.col() + dc;
        while (r >= 0 && r <= 7 && c >= 0 && c <= 7) {
            Piece occ = board.getPieceAt(new Position(r, c));
            if (occ == null)         { moves.add(new Position(r, c)); }
            else { if (isEnemy(occ)) moves.add(new Position(r, c)); break; }
            r += dr; c += dc;
        }
    }
}
