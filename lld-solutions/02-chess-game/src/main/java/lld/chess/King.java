package lld.chess;
import java.util.*;
public class King extends Piece {
    public King(Color color, Position position) { super(color, position); }
    @Override public String getSymbol() { return color == Color.WHITE ? "♔" : "♚"; }
    @Override public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : dirs)
            Position.of(position.row()+d[0], position.col()+d[1]).ifPresent(p -> {
                Piece occ = board.getPieceAt(p);
                if (occ == null || isEnemy(occ)) moves.add(p);
            });
        return moves;
    }
}
