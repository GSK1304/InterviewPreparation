package lld.chess;
public record Move(Position from, Position to, Piece piece, Piece capturedPiece) {
    public Move(Position from, Position to, Piece piece) { this(from, to, piece, null); }
    public boolean isCapture() { return capturedPiece != null; }
    @Override public String toString() {
        return piece.getSymbol() + from + (isCapture() ? "x" : "-") + to;
    }
}
