package lld.chess;
import java.util.*;
import java.util.stream.Collectors;

public class Board {
    private final Piece[][] grid = new Piece[8][8];

    public Piece getPieceAt(Position pos)          { return grid[pos.row()][pos.col()]; }
    public void  setPiece(Position pos, Piece p)   { grid[pos.row()][pos.col()] = p; }
    public void  removePiece(Position pos)         { grid[pos.row()][pos.col()] = null; }

    public List<Piece> getPieces(Color color) {
        List<Piece> list = new ArrayList<>();
        for (Piece[] row : grid)
            for (Piece p : row)
                if (p != null && p.getColor() == color) list.add(p);
        return list;
    }

    public Optional<Position> findKing(Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] instanceof King k && k.getColor() == color)
                    return Optional.of(new Position(r, c));
        return Optional.empty();
    }

    public boolean isUnderAttack(Position pos, Color byColor) {
        return getPieces(byColor).stream()
            .anyMatch(p -> p.getLegalMoves(this).contains(pos));
    }

    public boolean isInCheck(Color color) {
        return findKing(color)
            .map(kp -> isUnderAttack(kp, color.opponent()))
            .orElse(false);
    }

    public void display() {
        System.out.println("\n  a b c d e f g h");
        System.out.println("  -----------------");
        for (int r = 0; r < 8; r++) {
            System.out.print((8-r) + "|");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                System.out.print((p == null ? "." : p.getSymbol()) + " ");
            }
            System.out.println("|" + (8-r));
        }
        System.out.println("  -----------------");
        System.out.println("  a b c d e f g h\n");
    }
}
