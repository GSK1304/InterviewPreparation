package lld.bookmyshow;
import java.util.*;
import java.util.stream.Collectors;

public class Screen {
    private final String      screenId, name;
    private final List<Seat>  seats;

    public Screen(String screenId, String name, int rows, int cols, Map<Integer, SeatTier> rowTiers) {
        this.screenId = screenId; this.name = name;
        List<Seat> s = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            SeatTier tier = rowTiers.getOrDefault(r, SeatTier.NORMAL);
            for (int c = 0; c < cols; c++)
                s.add(new Seat(screenId + "-R" + r + "C" + c, r, c, tier));
        }
        this.seats = Collections.unmodifiableList(s);
    }

    public List<Seat> getAvailableSeats()       { return seats.stream().filter(Seat::isAvailable).collect(Collectors.toList()); }
    public List<Seat> getAvailableByTier(SeatTier t) { return seats.stream().filter(Seat::isAvailable).filter(s -> s.getTier()==t).collect(Collectors.toList()); }
    public Optional<Seat> findById(String id)   { return seats.stream().filter(s -> s.getSeatId().equals(id)).findFirst(); }
    public long availableCount()                { return seats.stream().filter(Seat::isAvailable).count(); }
    public int  getTotalSeats()                 { return seats.size(); }
    public String getScreenId()                 { return screenId; }
    public List<Seat> getSeats()                { return seats; }
}
