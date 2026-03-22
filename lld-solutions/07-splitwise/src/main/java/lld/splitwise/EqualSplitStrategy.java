package lld.splitwise;
import java.util.*;

public class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Money> calculate(List<String> participants, Money total, Map<String, Object> params) {
        if (participants.isEmpty()) throw new InvalidSplitException("At least one participant required");
        int n = participants.size();
        long perPerson = total.paise() / n;
        long remainder = total.paise() % n;
        Map<String, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++)
            result.put(participants.get(i), new Money(perPerson + (i < remainder ? 1 : 0)));
        return result;
    }
}
