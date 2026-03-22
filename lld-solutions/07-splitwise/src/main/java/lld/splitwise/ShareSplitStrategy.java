package lld.splitwise;
import java.util.*;

public class ShareSplitStrategy implements SplitStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Money> calculate(List<String> participants, Money total, Map<String, Object> params) {
        Map<String, Integer> shares = (Map<String, Integer>) params.get("shares");
        if (shares == null) throw new InvalidSplitException("shares required");
        int totalShares = shares.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShares <= 0) throw new InvalidSplitException("Total shares must be positive");
        Map<String, Money> result = new LinkedHashMap<>();
        long allocated = 0;
        List<String> keys = new ArrayList<>(shares.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String uid = keys.get(i);
            long share = (i == keys.size()-1) ? total.paise() - allocated
                       : Math.round((double) total.paise() * shares.get(uid) / totalShares);
            allocated += share;
            result.put(uid, new Money(share));
        }
        return result;
    }
}
