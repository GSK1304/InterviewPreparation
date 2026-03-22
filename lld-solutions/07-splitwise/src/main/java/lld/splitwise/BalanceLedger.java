package lld.splitwise;
import java.util.*;

public class BalanceLedger {
    private final Map<String, Map<String, Long>> ledger = new HashMap<>();

    public void addDebt(String owerId, String creditorId, Money amount) {
        if (owerId.equals(creditorId)) return;
        adjust(owerId,     creditorId, -amount.paise());
        adjust(creditorId, owerId,      amount.paise());
    }

    public void settle(String payerId, String creditorId, Money amount) {
        adjust(payerId,    creditorId,  amount.paise());
        adjust(creditorId, payerId,    -amount.paise());
    }

    private void adjust(String from, String to, long delta) {
        ledger.computeIfAbsent(from, k -> new HashMap<>()).merge(to, delta, Long::sum);
    }

    public Map<String, Long> getUserBalances(String userId) {
        return Collections.unmodifiableMap(ledger.getOrDefault(userId, Collections.emptyMap()));
    }

    public List<Settlement> simplifyDebts(Set<String> userIds) {
        Map<String, Long> net = new HashMap<>();
        for (String uid : userIds) {
            long total = getUserBalances(uid).values().stream().mapToLong(Long::longValue).sum();
            net.put(uid, total);
        }
        PriorityQueue<Map.Entry<String, Long>> creditors =
            new PriorityQueue<>((a, b) -> Long.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<String, Long>> debtors =
            new PriorityQueue<>((a, b) -> Long.compare(a.getValue(), b.getValue()));
        for (var entry : net.entrySet()) {
            if (entry.getValue() > 0)      creditors.add(entry);
            else if (entry.getValue() < 0) debtors.add(entry);
        }
        List<Settlement> settlements = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var cr = creditors.poll();
            var dr = debtors.poll();
            long amount = Math.min(cr.getValue(), -dr.getValue());
            settlements.add(new Settlement(dr.getKey(), cr.getKey(), new Money(amount)));
            long crLeft = cr.getValue() - amount;
            long drLeft = dr.getValue() + amount;
            if (crLeft > 0) creditors.add(Map.entry(cr.getKey(), crLeft));
            if (drLeft < 0) debtors.add(Map.entry(dr.getKey(), drLeft));
        }
        return settlements;
    }
}
