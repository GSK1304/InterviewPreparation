package lld.bookmyshow;
import java.time.*;
import java.util.Objects;

public class Show {
    private static int counter = 5000;
    private final String        showId, movieName, language;
    private final Screen        screen;
    private final LocalDateTime showTime;

    public Show(String movieName, String language, Screen screen, LocalDateTime showTime) {
        this.showId    = "SHW-" + counter++;
        this.movieName = Objects.requireNonNull(movieName);
        this.language  = Objects.requireNonNull(language);
        this.screen    = Objects.requireNonNull(screen);
        this.showTime  = Objects.requireNonNull(showTime);
        if (movieName.isBlank()) throw new IllegalArgumentException("Movie name required");
    }

    public String        getShowId()    { return showId; }
    public String        getMovieName() { return movieName; }
    public String        getLanguage()  { return language; }
    public Screen        getScreen()    { return screen; }
    public LocalDateTime getShowTime()  { return showTime; }
    public long          availableSeats(){ return screen.availableCount(); }
    @Override public String toString() {
        return String.format("Show[%s '%s' %s %s | %d seats]", showId, movieName, language, showTime, availableSeats());
    }
}
