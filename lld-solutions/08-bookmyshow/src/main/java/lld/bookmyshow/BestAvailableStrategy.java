package lld.bookmyshow;
import java.util.*;
import java.util.stream.Collectors;

public class BestAvailableStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        int mid = screen.getSeats().stream().mapToInt(Seat::getRow).max().orElse(0) / 2;
        return screen.getAvailableSeats().stream()
            .sorted(Comparator.comparingInt(s -> Math.abs(s.getRow() - mid)))
            .limit(count).collect(Collectors.toList());
    }
}
