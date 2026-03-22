package lld.parking.enums;
public enum VehicleType {
    MOTORCYCLE(SpotSize.MOTORCYCLE, 1000L),
    CAR       (SpotSize.COMPACT,    2000L),
    TRUCK     (SpotSize.LARGE,      4000L);

    private final SpotSize requiredSize;
    private final long     hourlyRatePaise;

    VehicleType(SpotSize requiredSize, long hourlyRatePaise) {
        this.requiredSize    = requiredSize;
        this.hourlyRatePaise = hourlyRatePaise;
    }

    public SpotSize getRequiredSize()    { return requiredSize; }
    public long     getHourlyRatePaise() { return hourlyRatePaise; }

    public boolean fitsIn(SpotSize size) {
        return size.ordinal() >= requiredSize.ordinal();
    }
}
