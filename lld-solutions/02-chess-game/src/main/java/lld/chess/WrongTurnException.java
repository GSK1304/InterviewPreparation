package lld.chess;
public class WrongTurnException extends ChessException {
    public WrongTurnException(Color expected, Color actual) {
        super("It's " + expected + "'s turn, but " + actual + " tried to move");
    }
}
