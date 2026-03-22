package lld.bookmyshow;
import java.util.*;
import java.util.stream.Collectors;

public class SpecificTierStrategy implements SeatSelectionStrategy {
    @Override
    public List<Seat> select(Screen screen, int count, Map<String, Object> params) {
        SeatTier tier = (SeatTier) params.getOrDefault("tier", SeatTier.NORMAL);
        List<Seat> avail = screen.getAvailableByTier(tier);
        if (avail.size() < count) throw new BookingException("Not enough " + tier + " seats: " + avail.size());
        return avail.stream().limit(count).collect(Collectors.toList());
    }
}
