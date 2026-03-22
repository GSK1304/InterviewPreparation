package lld.cabooking;
public record Money(long paise) {
    public Money { if (paise < 0) throw new IllegalArgumentException("Negative money"); }
    public static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    public Money add(Money o)              { return new Money(paise + o.paise); }
    public Money multiply(double f)        { return new Money(Math.round(paise * f)); }
    public double toRupees()               { return paise / 100.0; }
    @Override public String toString()     { return String.format("Rs.%.2f", toRupees()); }
}
