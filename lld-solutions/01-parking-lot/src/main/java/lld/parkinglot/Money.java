package lld.parkinglot;
public record Money(long paise) {
    public Money { if (paise < 0) throw new IllegalArgumentException("Money cannot be negative"); }
    public static Money ofRupees(double r) { return new Money(Math.round(r * 100)); }
    public Money add(Money o) { return new Money(paise + o.paise); }
    public double toRupees() { return paise / 100.0; }
    @Override public String toString() { return String.format("₹%.2f", toRupees()); }
}
