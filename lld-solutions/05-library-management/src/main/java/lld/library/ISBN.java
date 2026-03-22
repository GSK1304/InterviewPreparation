package lld.library;
import java.util.Objects;
public record ISBN(String value) {
    public ISBN {
        Objects.requireNonNull(value);
        String d = value.replaceAll("[-\\s]", "");
        if (d.length() != 10 && d.length() != 13)
            throw new IllegalArgumentException("ISBN must be 10 or 13 digits: " + value);
    }
    @Override public String toString() { return value; }
}
