package lld.spaceship;
public record Position(int x, int y) {
    public boolean isInBounds() {
        return x >= 0 && x < GameConfig.GRID_WIDTH && y >= 0 && y < GameConfig.GRID_HEIGHT;
    }
}
