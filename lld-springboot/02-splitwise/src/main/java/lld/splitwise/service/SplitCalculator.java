package lld.splitwise.service;
import lld.splitwise.dto.AddExpenseRequest;
import lld.splitwise.enums.SplitType;
import lld.splitwise.exception.SplitwiseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SplitCalculator {

    public Map<String, Long> calculate(AddExpenseRequest req) {
        long totalPaise = Math.round(req.getAmountRupees() * 100);
        List<String> participants = req.getParticipantIds();
        return switch (req.getSplitType()) {
            case EQUAL   -> equalSplit(participants, totalPaise);
            case EXACT   -> exactSplit(req, totalPaise);
            case PERCENT -> percentSplit(req, totalPaise);
            case SHARE   -> shareSplit(req, totalPaise);
        };
    }

    private Map<String, Long> equalSplit(List<String> participants, long totalPaise) {
        int n = participants.size();
        long perPerson = totalPaise / n;
        long remainder = totalPaise % n;
        Map<String, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++)
            result.put(participants.get(i), perPerson + (i < remainder ? 1 : 0));
        return result;
    }

    private Map<String, Long> exactSplit(AddExpenseRequest req, long totalPaise) {
        if (req.getExactAmounts() == null || req.getExactAmounts().isEmpty())
            throw new SplitwiseException("exactAmounts required for EXACT split", HttpStatus.BAD_REQUEST);
        long sum = req.getExactAmounts().values().stream().mapToLong(v -> Math.round(v * 100)).sum();
        if (Math.abs(sum - totalPaise) > 1)
            throw new SplitwiseException("Exact amounts sum Rs." + sum/100.0 + " does not match total Rs." + totalPaise/100.0, HttpStatus.BAD_REQUEST);
        Map<String, Long> result = new LinkedHashMap<>();
        req.getExactAmounts().forEach((uid, amt) -> result.put(uid, Math.round(amt * 100)));
        return result;
    }

    private Map<String, Long> percentSplit(AddExpenseRequest req, long totalPaise) {
        if (req.getPercentages() == null || req.getPercentages().isEmpty())
            throw new SplitwiseException("percentages required for PERCENT split", HttpStatus.BAD_REQUEST);
        double sum = req.getPercentages().values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > 0.01)
            throw new SplitwiseException("Percentages must sum to 100, got: " + sum, HttpStatus.BAD_REQUEST);
        Map<String, Long> result = new LinkedHashMap<>();
        long allocated = 0;
        List<String> keys = new ArrayList<>(req.getPercentages().keySet());
        for (int i = 0; i < keys.size(); i++) {
            String uid = keys.get(i);
            long share = (i == keys.size()-1) ? totalPaise - allocated
                : Math.round(totalPaise * req.getPercentages().get(uid) / 100.0);
            allocated += share;
            result.put(uid, share);
        }
        return result;
    }

    private Map<String, Long> shareSplit(AddExpenseRequest req, long totalPaise) {
        if (req.getShares() == null || req.getShares().isEmpty())
            throw new SplitwiseException("shares required for SHARE split", HttpStatus.BAD_REQUEST);
        int totalShares = req.getShares().values().stream().mapToInt(Integer::intValue).sum();
        if (totalShares <= 0)
            throw new SplitwiseException("Total shares must be positive", HttpStatus.BAD_REQUEST);
        Map<String, Long> result = new LinkedHashMap<>();
        long allocated = 0;
        List<String> keys = new ArrayList<>(req.getShares().keySet());
        for (int i = 0; i < keys.size(); i++) {
            String uid = keys.get(i);
            long share = (i == keys.size()-1) ? totalPaise - allocated
                : Math.round((double) totalPaise * req.getShares().get(uid) / totalShares);
            allocated += share;
            result.put(uid, share);
        }
        return result;
    }
}
