package lld.splitwise;
import java.util.List;
import java.util.Map;

public interface SplitStrategy {
    Map<String, Money> calculate(List<String> participants, Money total, Map<String, Object> params);
}
