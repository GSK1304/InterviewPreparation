package lld.parkinglot;
public enum VehicleType {
    MOTORCYCLE(SpotSize.MOTORCYCLE, 10.0),
    CAR       (SpotSize.COMPACT,    20.0),
    TRUCK     (SpotSize.LARGE,      40.0);

    private final SpotSize minSpotSize;
    private final double   hourlyRate;

    VehicleType(SpotSize minSpotSize, double hourlyRate) {
        this.minSpotSize = minSpotSize;
        this.hourlyRate  = hourlyRate;
    }

    public SpotSize getMinSpotSize() { return minSpotSize; }
    public double   getHourlyRate()  { return hourlyRate; }
    public boolean  fitsIn(SpotSize size) { return size.ordinal() >= minSpotSize.ordinal(); }
}
