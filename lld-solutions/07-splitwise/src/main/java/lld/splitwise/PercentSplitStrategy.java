package lld.splitwise;
import java.util.*;

public class PercentSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money total, Map<String, Object> params) {
        Map<String, Double> pcts = (Map<String, Double>) params.get("percentages");
        if (pcts == null) throw new InvalidSplitException("percentages required");
        double sum = pcts.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > 0.01) throw new InvalidSplitException("Percentages must sum to 100, got: " + sum);
        Map<String, Money> result = new LinkedHashMap<>();
        long allocated = 0;
        List<String> keys = new ArrayList<>(pcts.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String uid = keys.get(i);
            long share = (i == keys.size()-1) ? total.paise() - allocated
                       : Math.round(total.paise() * pcts.get(uid) / 100.0);
            allocated += share;
            result.put(uid, new Money(share));
        }
        return result;
    }
}
