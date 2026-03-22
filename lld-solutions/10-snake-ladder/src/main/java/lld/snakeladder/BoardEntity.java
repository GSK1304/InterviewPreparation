package lld.snakeladder;
import java.util.Objects;

public class BoardEntity {
    private final BoardEntityType type;
    private final int             from;
    private final int             to;

    private BoardEntity(BoardEntityType type, int from, int to) {
        this.type = type; this.from = from; this.to = to;
    }

    public static BoardEntity snake(int head, int tail) {
        if (head <= tail) throw new IllegalArgumentException("Snake head must be above tail. head=" + head + " tail=" + tail);
        if (head < 2 || head > 99 || tail < 1 || tail > 98) throw new IllegalArgumentException("Snake positions out of range");
        return new BoardEntity(BoardEntityType.SNAKE, head, tail);
    }

    public static BoardEntity ladder(int bottom, int top) {
        if (bottom >= top) throw new IllegalArgumentException("Ladder bottom must be below top. bottom=" + bottom + " top=" + top);
        if (bottom < 1 || bottom > 98 || top < 2 || top > 100) throw new IllegalArgumentException("Ladder positions out of range");
        return new BoardEntity(BoardEntityType.LADDER, bottom, top);
    }

    public BoardEntityType getType() { return type; }
    public int getFrom()             { return from; }
    public int getTo()               { return to; }

    @Override public String toString() { return type + "[" + from + "->" + to + "]"; }
}
