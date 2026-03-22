package lld.splitwise;
import java.util.Objects;
public record User(String userId, String name, String email) {
    public User {
        Objects.requireNonNull(userId); Objects.requireNonNull(name); Objects.requireNonNull(email);
        if (userId.isBlank()) throw new IllegalArgumentException("User ID required");
        if (name.isBlank())   throw new IllegalArgumentException("Name required");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
    }
}
