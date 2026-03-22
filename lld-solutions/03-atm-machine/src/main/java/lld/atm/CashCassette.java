package lld.atm;
public class CashCassette {
    private final int denomination;
    private int       noteCount;

    public CashCassette(int denomination, int noteCount) {
        if (denomination <= 0) throw new IllegalArgumentException("Denomination must be positive");
        if (noteCount < 0)     throw new IllegalArgumentException("Note count cannot be negative");
        this.denomination = denomination;
        this.noteCount    = noteCount;
    }

    public int   getDenomination() { return denomination; }
    public int   getNoteCount()    { return noteCount; }

    public synchronized boolean canDispense(int n) { return noteCount >= n; }
    public synchronized void dispense(int n) {
        if (!canDispense(n)) throw new ATMException("Not enough notes of Rs." + denomination);
        noteCount -= n;
    }
    public synchronized void refill(int n) { if (n < 0) throw new IllegalArgumentException(); noteCount += n; }
}
