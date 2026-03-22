package lld.chess;
import java.util.*;
public class Pawn extends Piece {
    public Pawn(Color color, Position position) { super(color, position); }
    @Override public String getSymbol() { return color == Color.WHITE ? "♙" : "♟"; }
    @Override public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int dir = (color == Color.WHITE) ? -1 : 1;
        int r = position.row(), c = position.col();
        Position.of(r + dir, c).ifPresent(p -> {
            if (board.getPieceAt(p) == null) {
                moves.add(p);
                if (!hasMoved) Position.of(r + 2*dir, c).ifPresent(p2 -> {
                    if (board.getPieceAt(p2) == null) moves.add(p2);
                });
            }
        });
        for (int dc : new int[]{-1, 1})
            Position.of(r + dir, c + dc).ifPresent(p -> {
                if (isEnemy(board.getPieceAt(p))) moves.add(p);
            });
        return moves;
    }
}
