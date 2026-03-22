package lld.splitwise;
public record Money(long paise) {
    static final Money ZERO = new Money(0);
    public Money { if (paise < 0) throw new IllegalArgumentException("Money cannot be negative"); }
    public static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    public Money add(Money o)              { return new Money(paise + o.paise); }
    public Money subtract(Money o) {
        if (o.paise > paise) throw new IllegalArgumentException("Cannot subtract larger amount");
        return new Money(paise - o.paise);
    }
    public boolean isZero()                { return paise == 0; }
    public double  toRupees()              { return paise / 100.0; }
    @Override public String toString()     { return String.format("Rs.%.2f", toRupees()); }
}
