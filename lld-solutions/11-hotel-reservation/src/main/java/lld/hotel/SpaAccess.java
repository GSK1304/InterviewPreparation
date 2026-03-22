package lld.hotel;
public class SpaAccess extends RoomAmenityDecorator {
    private static final Money COST = Money.ofRupees(1500);
    public SpaAccess(RoomService r) { super(r); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Spa"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(COST); }
}
