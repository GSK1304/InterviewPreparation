package lld.snakeladder;
public record GameConfig(boolean exactFinish, boolean bonusTurnOnSix, boolean skipOnSnake, int boardSize) {
    public static GameConfig defaultConfig() { return new GameConfig(false, true, false, 100); }
}
