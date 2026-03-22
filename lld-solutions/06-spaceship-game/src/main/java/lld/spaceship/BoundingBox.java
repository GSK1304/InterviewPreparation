package lld.spaceship;
public record BoundingBox(int x, int y, int w, int h) {
    public boolean intersects(BoundingBox o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }
}
