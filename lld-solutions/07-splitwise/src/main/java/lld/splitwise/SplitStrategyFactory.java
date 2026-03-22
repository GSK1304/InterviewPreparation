package lld.splitwise;
import java.util.Map;

public class SplitStrategyFactory {
    private static final Map<SplitType, SplitStrategy> STRATEGIES = Map.of(
        SplitType.EQUAL,   new EqualSplitStrategy(),
        SplitType.EXACT,   new ExactSplitStrategy(),
        SplitType.PERCENT, new PercentSplitStrategy(),
        SplitType.SHARE,   new ShareSplitStrategy()
    );
    public static SplitStrategy get(SplitType type) {
        SplitStrategy s = STRATEGIES.get(type);
        if (s == null) throw new SplitwiseException("No strategy for: " + type);
        return s;
    }
}
