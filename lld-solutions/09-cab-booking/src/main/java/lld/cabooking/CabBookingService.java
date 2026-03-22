package lld.cabooking;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class CabBookingService {
    private final Map<String, Driver>  drivers  = new ConcurrentHashMap<>();
    private final Map<String, Rider>   riders   = new ConcurrentHashMap<>();
    private final Map<String, Ride>    rides    = new ConcurrentHashMap<>();
    private final FareStrategy         fareStrategy;
    private final DriverMatchStrategy  matchStrategy;
    private final List<BiConsumer<String, Ride>> listeners = new CopyOnWriteArrayList<>();

    public CabBookingService(FareStrategy fareStrategy, DriverMatchStrategy matchStrategy) {
        this.fareStrategy  = Objects.requireNonNull(fareStrategy);
        this.matchStrategy = Objects.requireNonNull(matchStrategy);
    }

    public void addListener(BiConsumer<String, Ride> l) { listeners.add(l); }
    private void notify(String event, Ride ride) { listeners.forEach(l -> l.accept(event, ride)); }

    public Driver registerDriver(String id, String name, String phone, Vehicle vehicle, Location loc) {
        Driver d = new Driver(id, name, phone, vehicle, loc);
        drivers.put(id, d); System.out.println("Driver registered: " + d); return d;
    }

    public Rider registerRider(String id, String name, String phone, String email) {
        Rider r = new Rider(id, name, phone, email);
        riders.put(id, r); System.out.println("Rider registered: " + r); return r;
    }

    public Ride requestRide(String riderId, Location pickup, Location dropoff, VehicleType type) {
        Rider rider = getOrThrow(riders, riderId, "Rider");
        List<Driver> avail = drivers.values().stream().filter(Driver::isAvailable).collect(Collectors.toList());
        Driver driver = matchStrategy.match(avail, pickup, type).orElseThrow(() -> new NoDriverAvailableException(type));
        double dist    = pickup.distanceTo(dropoff);
        Money  estimate = fareStrategy.calculate(dist, Duration.ofMinutes((long)(dist*3)), type);
        Ride ride = new Ride(rider, pickup, dropoff, type);
        ride.assignDriver(driver, estimate);
        rides.put(ride.getRideId(), ride);
        notify("REQUESTED", ride);
        System.out.printf("Ride requested: %s | Driver: %s | Est: %s%n", ride.getRideId(), driver.getName(), estimate);
        return ride;
    }

    public void driverArrived(String rideId) { Ride r = getRide(rideId); r.driverArrived(); notify("ARRIVED", r); System.out.println("Driver arrived: " + rideId); }
    public void startRide(String rideId)     { Ride r = getRide(rideId); r.startRide();    notify("STARTED", r); System.out.println("Ride started: " + rideId); }

    public Money completeRide(String rideId) {
        Ride r   = getRide(rideId);
        Duration dur  = Duration.ofMinutes((long)(r.getDistanceKm()*2.5+5));
        Money fare    = fareStrategy.calculate(r.getDistanceKm(), dur, r.getVehicleType());
        r.completeRide(fare);
        notify("COMPLETED", r);
        System.out.printf("Completed: %s | Fare: %s | %.2fkm%n", rideId, fare, r.getDistanceKm());
        return fare;
    }

    public void cancelRide(String rideId, CancelReason reason) {
        Ride r = getRide(rideId); r.cancel(reason); notify("CANCELLED", r);
        System.out.println("Cancelled: " + rideId + " | " + reason);
    }

    public void rateDriver(String rideId, double rating, String comment) {
        Ride r = getRide(rideId);
        if (r.getStatus() != RideStatus.COMPLETED) throw new CabException("Can only rate completed rides");
        r.getDriver().addRating(new Rating(rating, comment, Instant.now()));
        System.out.printf("Driver %s rated: %.1f%n", r.getDriver().getName(), rating);
    }

    public void displayDriverStats() {
        System.out.println("\n=== Driver Statistics ===");
        drivers.values().forEach(d -> System.out.printf("  %s | Rating: %.2f* | %s%n", d.getName(), d.getAverageRating(), d.getStatus()));
    }

    private Ride getRide(String id) { return getOrThrow(rides, id, "Ride"); }
    private <T> T getOrThrow(Map<String, T> map, String id, String type) {
        T v = map.get(id); if (v == null) throw new CabException(type + " not found: " + id); return v;
    }
}
