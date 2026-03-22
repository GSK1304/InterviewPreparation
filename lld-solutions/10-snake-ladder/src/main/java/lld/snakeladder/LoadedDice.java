package lld.snakeladder;
public class LoadedDice implements DiceStrategy {
    private final int fixedValue;
    public LoadedDice(int v) { if (v<1||v>6) throw new IllegalArgumentException("Value must be 1-6"); fixedValue = v; }
    @Override public int roll()        { return fixedValue; }
    @Override public int getMaxValue() { return 6; }
}
