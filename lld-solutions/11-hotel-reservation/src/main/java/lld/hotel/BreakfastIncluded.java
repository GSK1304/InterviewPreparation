package lld.hotel;
public class BreakfastIncluded extends RoomAmenityDecorator {
    private static final Money COST = Money.ofRupees(600);
    public BreakfastIncluded(RoomService r) { super(r); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Breakfast"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(COST); }
}
