package lld.atm;
public record Money(long paise) {
    static final Money ZERO = new Money(0);
    public Money { if (paise < 0) throw new IllegalArgumentException("Amount cannot be negative"); }
    public static Money ofRupees(long rupees) { return new Money(rupees * 100); }
    public Money add(Money o) { return new Money(paise + o.paise); }
    public Money subtract(Money o) {
        if (o.paise > paise) throw new InsufficientFundsException(this, o);
        return new Money(paise - o.paise);
    }
    public boolean isZero()                { return paise == 0; }
    public boolean isGreaterThan(Money o)  { return paise > o.paise; }
    public long    toRupees()              { return paise / 100; }
    @Override public String toString()     { return "Rs." + toRupees(); }
}
