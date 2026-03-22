package lld.atm;
import java.util.*;
public abstract class CashDispenser {
    protected CashDispenser next;
    protected final CashCassette cassette;

    public CashDispenser(CashCassette cassette) { this.cassette = cassette; }

    public CashDispenser setNext(CashDispenser next) { this.next = next; return next; }

    public Map<Integer, Integer> dispense(long remainingRupees) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        int denom = cassette.getDenomination();
        if (remainingRupees >= denom) {
            int notes = (int) Math.min(remainingRupees / denom, cassette.getNoteCount());
            if (notes > 0) {
                cassette.dispense(notes);
                result.put(denom, notes);
                remainingRupees -= (long) notes * denom;
            }
        }
        if (next != null && remainingRupees > 0) result.putAll(next.dispense(remainingRupees));
        return result;
    }
}
