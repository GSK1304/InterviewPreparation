package lld.cabooking;
import java.time.*;
import java.util.Objects;

public class Ride {
    private static int counter = 9000;
    private final String      rideId;
    private final Rider       rider;
    private final VehicleType vehicleType;
    private final Location    pickup, dropoff;
    private Driver            driver;
    private RideStatus        status;
    private Money             estimatedFare, actualFare;
    private Instant           requestedAt, startedAt, completedAt;
    private CancelReason      cancelReason;

    public Ride(Rider rider, Location pickup, Location dropoff, VehicleType vehicleType) {
        this.rideId      = "RDE-" + counter++;
        this.rider       = Objects.requireNonNull(rider);
        this.pickup      = Objects.requireNonNull(pickup);
        this.dropoff     = Objects.requireNonNull(dropoff);
        this.vehicleType = Objects.requireNonNull(vehicleType);
        this.status      = RideStatus.REQUESTED;
        this.requestedAt = Instant.now();
    }

    public void assignDriver(Driver d, Money est) {
        requireStatus(RideStatus.REQUESTED);
        this.driver = Objects.requireNonNull(d); this.estimatedFare = est;
        this.status = RideStatus.DRIVER_ASSIGNED; d.setStatus(DriverStatus.ON_TRIP);
    }

    public void driverArrived() { requireStatus(RideStatus.DRIVER_ASSIGNED); status = RideStatus.DRIVER_ARRIVED; }

    public void startRide() { requireStatus(RideStatus.DRIVER_ARRIVED); status = RideStatus.IN_PROGRESS; startedAt = Instant.now(); }

    public void completeRide(Money fare) {
        requireStatus(RideStatus.IN_PROGRESS);
        this.actualFare = fare; this.status = RideStatus.COMPLETED; this.completedAt = Instant.now();
        if (driver != null) driver.setStatus(DriverStatus.AVAILABLE);
    }

    public void cancel(CancelReason reason) {
        if (status == RideStatus.COMPLETED || status == RideStatus.CANCELLED)
            throw new InvalidRideStateException("ACTIVE", status);
        status = RideStatus.CANCELLED; cancelReason = reason;
        if (driver != null) driver.setStatus(DriverStatus.AVAILABLE);
    }

    private void requireStatus(RideStatus req) {
        if (status != req) throw new InvalidRideStateException(req.name(), status);
    }

    public double   getDistanceKm()     { return pickup.distanceTo(dropoff); }
    public Duration getActualDuration() {
        if (startedAt == null || completedAt == null) return Duration.ZERO;
        return Duration.between(startedAt, completedAt);
    }

    public String      getRideId()        { return rideId; }
    public Rider       getRider()         { return rider; }
    public Driver      getDriver()        { return driver; }
    public Location    getPickup()        { return pickup; }
    public Location    getDropoff()       { return dropoff; }
    public VehicleType getVehicleType()   { return vehicleType; }
    public RideStatus  getStatus()        { return status; }
    public Money       getEstimatedFare() { return estimatedFare; }
    public Money       getActualFare()    { return actualFare; }

    @Override public String toString() {
        return String.format("Ride[%s %s->%s %s %s fare=%s]",
            rideId, pickup, dropoff, vehicleType, status,
            actualFare != null ? actualFare : estimatedFare);
    }
}
