package lld.chess;
import java.util.Objects;
public record Player(String name, Color color) {
    public Player {
        Objects.requireNonNull(name); Objects.requireNonNull(color);
        if (name.isBlank()) throw new IllegalArgumentException("Player name required");
    }
}
