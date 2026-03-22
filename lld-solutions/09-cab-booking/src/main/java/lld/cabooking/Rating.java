package lld.cabooking;
import java.time.Instant;
public record Rating(double value, String comment, Instant ratedAt) {
    public Rating { if (value < 1.0 || value > 5.0) throw new IllegalArgumentException("Rating must be 1-5"); }
}
