package lld.chess;
import java.util.Optional;

public record Position(int row, int col) {
    public Position {
        if (row < 0 || row > 7) throw new IllegalArgumentException("Row must be 0-7, got: " + row);
        if (col < 0 || col > 7) throw new IllegalArgumentException("Col must be 0-7, got: " + col);
    }
    public static Optional<Position> of(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return Optional.empty();
        return Optional.of(new Position(row, col));
    }
    @Override public String toString() { return "" + (char)('a' + col) + (8 - row); }
}
