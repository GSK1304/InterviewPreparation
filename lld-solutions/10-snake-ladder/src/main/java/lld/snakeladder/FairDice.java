package lld.snakeladder;
import java.util.Random;
public class FairDice implements DiceStrategy {
    private final int faces; private final Random rng = new Random();
    public FairDice()        { this(6); }
    public FairDice(int f)   { if (f < 2) throw new IllegalArgumentException("Need at least 2 faces"); this.faces = f; }
    @Override public int roll()        { return rng.nextInt(faces) + 1; }
    @Override public int getMaxValue() { return faces; }
}
