package lld.splitwise;
import java.util.Objects;
public record Settlement(String fromUserId, String toUserId, Money amount) {
    public Settlement {
        Objects.requireNonNull(fromUserId); Objects.requireNonNull(toUserId); Objects.requireNonNull(amount);
        if (fromUserId.equals(toUserId)) throw new IllegalArgumentException("Cannot settle with yourself");
        if (amount.isZero())             throw new IllegalArgumentException("Settlement amount cannot be zero");
    }
    @Override public String toString() { return fromUserId + " owes " + toUserId + " -> " + amount; }
}
