package lld.snakeladder;
import java.util.*;

public class Board {
    private final int                        size;
    private final Map<Integer, BoardEntity>  entities;
    private final Map<Integer, Integer>      teleports;

    private Board(Builder b) { this.size = b.size; this.entities = Collections.unmodifiableMap(b.entities); this.teleports = Collections.unmodifiableMap(b.teleports); }

    public int applyEffect(int pos)              { return teleports.getOrDefault(pos, pos); }
    public Optional<BoardEntity> getEntityAt(int pos) { return Optional.ofNullable(entities.get(pos)); }
    public boolean isSnakeHead(int pos)          { BoardEntity e = entities.get(pos); return e != null && e.getType() == BoardEntityType.SNAKE; }
    public boolean isLadderBottom(int pos)       { BoardEntity e = entities.get(pos); return e != null && e.getType() == BoardEntityType.LADDER; }
    public int getSize()                         { return size; }

    public static final class Builder {
        private final int                       size;
        private final Map<Integer, BoardEntity> entities  = new LinkedHashMap<>();
        private final Map<Integer, Integer>     teleports = new HashMap<>();

        public Builder(int size) {
            if (size < 10 || size % 10 != 0) throw new IllegalArgumentException("Board size must be multiple of 10, >= 10");
            this.size = size;
        }

        public Builder addSnake(int head, int tail) {
            if (teleports.containsKey(head)) throw new IllegalArgumentException("Position already occupied: " + head);
            BoardEntity s = BoardEntity.snake(head, tail);
            entities.put(head, s); teleports.put(head, tail); return this;
        }

        public Builder addLadder(int bottom, int top) {
            if (teleports.containsKey(bottom)) throw new IllegalArgumentException("Position already occupied: " + bottom);
            BoardEntity l = BoardEntity.ladder(bottom, top);
            entities.put(bottom, l); teleports.put(bottom, top); return this;
        }

        public Board build() { return new Board(this); }
    }
}
