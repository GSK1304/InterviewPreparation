package lld.cabooking;
import java.util.*;

public class Driver {
    private final String     driverId, name, phone;
    private final Vehicle    vehicle;
    private Location         currentLocation;
    private DriverStatus     status = DriverStatus.AVAILABLE;
    private final List<Rating> ratings = new ArrayList<>();

    public Driver(String driverId, String name, String phone, Vehicle vehicle, Location loc) {
        this.driverId = Objects.requireNonNull(driverId); this.name    = Objects.requireNonNull(name);
        this.phone    = Objects.requireNonNull(phone);    this.vehicle = Objects.requireNonNull(vehicle);
        this.currentLocation = Objects.requireNonNull(loc);
        if (name.isBlank()) throw new IllegalArgumentException("Driver name required");
    }

    public void updateLocation(Location loc) { this.currentLocation = loc; }
    public void setStatus(DriverStatus s)    { this.status = s; }
    public void addRating(Rating r)          { ratings.add(r); }
    public double getAverageRating()         { return ratings.isEmpty() ? 5.0 : ratings.stream().mapToDouble(Rating::value).average().orElse(5.0); }
    public double distanceTo(Location t)     { return currentLocation.distanceTo(t); }

    public String       getDriverId()       { return driverId; }
    public String       getName()           { return name; }
    public Vehicle      getVehicle()        { return vehicle; }
    public Location     getCurrentLocation(){ return currentLocation; }
    public DriverStatus getStatus()         { return status; }
    public boolean      isAvailable()       { return status == DriverStatus.AVAILABLE; }

    @Override public String toString() {
        return String.format("Driver[%s %s %s %.1f* %s]", driverId, name, vehicle.type(), getAverageRating(), status);
    }
}
