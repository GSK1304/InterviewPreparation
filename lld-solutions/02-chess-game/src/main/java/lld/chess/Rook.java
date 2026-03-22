package lld.chess;
import java.util.*;
public class Rook extends Piece {
    public Rook(Color color, Position position) { super(color, position); }
    @Override public String getSymbol() { return color == Color.WHITE ? "♖" : "♜"; }
    @Override public List<Position> getLegalMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}})
            addSlidingMoves(board, moves, d[0], d[1]);
        return moves;
    }
}
