package lld.hotel;
public record Money(long paise) {
    static final Money ZERO = new Money(0);
    public Money { if (paise < 0) throw new IllegalArgumentException("Negative money"); }
    public static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    public Money add(Money o)              { return new Money(paise + o.paise); }
    public Money multiply(double f)        { return new Money(Math.round(paise * f)); }
    public Money multiply(long n)          { return new Money(paise * n); }
    public double toRupees()               { return paise / 100.0; }
    public boolean isGreaterThan(Money o)  { return paise > o.paise; }
    @Override public String toString()     { return String.format("Rs.%.2f", toRupees()); }
}
