package lld.library;
import java.util.Objects;
public record Author(String firstName, String lastName) {
    public Author {
        Objects.requireNonNull(firstName); Objects.requireNonNull(lastName);
        if (firstName.isBlank()) throw new IllegalArgumentException("First name required");
        if (lastName.isBlank())  throw new IllegalArgumentException("Last name required");
    }
    public String fullName() { return firstName + " " + lastName; }
}
