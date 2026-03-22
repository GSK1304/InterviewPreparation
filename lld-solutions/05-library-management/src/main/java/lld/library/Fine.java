package lld.library;
public record Fine(double amountRupees, int overdueDays) {
    public static final Fine NONE = new Fine(0, 0);
    public Fine { if (amountRupees < 0) throw new IllegalArgumentException("Fine cannot be negative"); }
    public boolean isOwed() { return amountRupees > 0; }
    @Override public String toString() {
        return isOwed() ? String.format("Rs.%.2f (%d days overdue)", amountRupees, overdueDays) : "No fine";
    }
}
