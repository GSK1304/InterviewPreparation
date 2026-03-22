package lld.hotel;
import java.util.Objects;
public record Guest(String guestId, String name, String email, String phone, String idProof) {
    public Guest {
        Objects.requireNonNull(guestId); Objects.requireNonNull(name);
        Objects.requireNonNull(email);   Objects.requireNonNull(phone); Objects.requireNonNull(idProof);
        if (name.isBlank())       throw new IllegalArgumentException("Name required");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email: " + email);
        if (phone.length() < 10)  throw new IllegalArgumentException("Invalid phone: " + phone);
    }
}
