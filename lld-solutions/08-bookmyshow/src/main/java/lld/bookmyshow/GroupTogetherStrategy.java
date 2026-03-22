package lld.bookmyshow;
import java.util.*;
import java.util.stream.Collectors;

public class GroupTogetherStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        Map<Integer, List<Seat>> byRow = screen.getAvailableSeats().stream()
            .collect(Collectors.groupingBy(Seat::getRow));
        for (var entry : byRow.entrySet()) {
            List<Seat> row = entry.getValue().stream().sorted(Comparator.comparingInt(Seat::getCol)).collect(Collectors.toList());
            List<Seat> consec = findConsecutive(row, count);
            if (consec != null) return consec;
        }
        throw new BookingException("No " + count + " consecutive seats available");
    }
    private List<Seat> findConsecutive(List<Seat> row, int count) {
        for (int i = 0; i <= row.size()-count; i++) {
            List<Seat> cand = row.subList(i, i+count);
            boolean ok = true;
            for (int j = 1; j < cand.size(); j++) if (cand.get(j).getCol() != cand.get(j-1).getCol()+1) { ok=false; break; }
            if (ok) return new ArrayList<>(cand);
        }
        return null;
    }
}
