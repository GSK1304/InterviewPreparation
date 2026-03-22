package lld.chess;
public class InvalidMoveException extends ChessException {
    public InvalidMoveException(String m) { super("Invalid move: " + m); }
}
