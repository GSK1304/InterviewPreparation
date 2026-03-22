package lld.chess;
public class GameOverException extends ChessException {
    public GameOverException(GameStatus s) { super("Game is over: " + s); }
}
