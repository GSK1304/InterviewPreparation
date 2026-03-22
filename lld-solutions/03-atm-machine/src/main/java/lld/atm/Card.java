package lld.atm;
import java.time.Instant;
import java.util.Objects;
public record Card(String cardNumber, String bankCode, Instant expiryDate) {
    public Card {
        Objects.requireNonNull(cardNumber); Objects.requireNonNull(bankCode); Objects.requireNonNull(expiryDate);
        if (!cardNumber.matches("\\d{16}")) throw new IllegalArgumentException("Card number must be 16 digits");
    }
    public boolean isExpired()     { return Instant.now().isAfter(expiryDate); }
    public String  maskedNumber()  { return "**** **** **** " + cardNumber.substring(12); }
}
