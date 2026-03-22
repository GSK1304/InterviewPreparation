package lld.chess;
import java.util.*;
public class Knight extends Piece {
    public Knight(Color color, Position position) { super(color, position); }
    @Override public String getSymbol() { return color == Color.WHITE ? "♘" : "♞"; }
    @Override public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        for (int[] j : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}})
            Position.of(position.row()+j[0], position.col()+j[1]).ifPresent(p -> {
                Piece occ = board.getPieceAt(p);
                if (occ == null || isEnemy(occ)) moves.add(p);
            });
        return moves;
    }
}
