package lld.splitwise;
import java.util.*;

public class ExactSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money total, Map<String, Object> params) {
        Map<String, Money> exact = (Map<String, Money>) params.get("exactAmounts");
        if (exact == null) throw new InvalidSplitException("exactAmounts required");
        for (String uid : participants)
            if (!exact.containsKey(uid)) throw new InvalidSplitException("No exact amount for: " + uid);
        Money sum = exact.values().stream().reduce(Money.ZERO, Money::add);
        if (sum.paise() != total.paise()) throw new InvalidSplitException("Exact amounts sum " + sum + " != total " + total);
        return new LinkedHashMap<>(exact);
    }
}
