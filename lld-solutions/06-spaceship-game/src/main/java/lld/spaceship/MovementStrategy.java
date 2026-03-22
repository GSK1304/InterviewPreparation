package lld.spaceship;
@FunctionalInterface
public interface MovementStrategy {
    boolean move(Enemy enemy, int tick);
}
