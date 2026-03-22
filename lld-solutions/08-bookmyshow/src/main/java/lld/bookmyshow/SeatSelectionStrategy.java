package lld.bookmyshow;
import java.util.List;
import java.util.Map;

public interface SeatSelectionStrategy {
    List<Seat> select(Screen screen, int count, Map<String, Object> params);
}
