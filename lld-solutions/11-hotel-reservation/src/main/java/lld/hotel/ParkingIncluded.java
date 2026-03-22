package lld.hotel;
public class ParkingIncluded extends RoomAmenityDecorator {
    private static final Money COST = Money.ofRupees(200);
    public ParkingIncluded(RoomService r) { super(r); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Parking"; }
    @Override public Money  getDailyRate()   { return wrapped.getDailyRate().add(COST); }
}
