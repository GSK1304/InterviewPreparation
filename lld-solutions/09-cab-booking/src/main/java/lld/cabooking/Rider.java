package lld.cabooking;
import java.util.*;

public class Rider {
    private final String name, phone, email, riderId;
    private final List<Rating> ratings = new ArrayList<>();

    public Rider(String riderId, String name, String phone, String email) {
        this.riderId = Objects.requireNonNull(riderId); this.name  = Objects.requireNonNull(name);
        this.phone   = Objects.requireNonNull(phone);   this.email = Objects.requireNonNull(email);
        if (name.isBlank())       throw new IllegalArgumentException("Name required");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }

    public void addRating(Rating r) { ratings.add(r); }
    public double getAverageRating(){ return ratings.isEmpty() ? 5.0 : ratings.stream().mapToDouble(Rating::value).average().orElse(5.0); }
    public String getRiderId()      { return riderId; }
    public String getName()         { return name; }
    @Override public String toString() { return String.format("Rider[%s %s %.1f*]", riderId, name, getAverageRating()); }
}
