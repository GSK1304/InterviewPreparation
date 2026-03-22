package lld.cabooking.service;
import jakarta.transaction.Transactional;
import lld.cabooking.dto.*;
import lld.cabooking.entity.*;
import lld.cabooking.enums.*;
import lld.cabooking.exception.CabException;
import lld.cabooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CabBookingService {
    private static final Logger log = LoggerFactory.getLogger(CabBookingService.class);
    private static final double MAX_RADIUS_KM = 5.0;
    private static final Map<VehicleType, Double> BASE_FARE = Map.of(VehicleType.AUTO,30.0,VehicleType.MINI,40.0,VehicleType.SEDAN,60.0,VehicleType.SUV,80.0,VehicleType.PREMIUM,120.0);
    private static final Map<VehicleType, Double> PER_KM    = Map.of(VehicleType.AUTO,8.0,VehicleType.MINI,10.0,VehicleType.SEDAN,14.0,VehicleType.SUV,18.0,VehicleType.PREMIUM,25.0);

    private final DriverRepository driverRepo;
    private final RiderRepository  riderRepo;
    private final RideRepository   rideRepo;

    @Transactional
    public Driver registerDriver(RegisterDriverRequest req) {
        log.info("[CabService] Registering driver | id={} type={}", req.getDriverId(), req.getVehicleType());
        if (driverRepo.existsById(req.getDriverId())) throw new CabException("Driver already exists: " + req.getDriverId(), HttpStatus.CONFLICT);
        Driver d = new Driver(); d.setDriverId(req.getDriverId()); d.setName(req.getName()); d.setPhone(req.getPhone());
        d.setVehicleNumber(req.getVehicleNumber()); d.setVehicleModel(req.getVehicleModel());
        d.setVehicleType(req.getVehicleType()); d.setCurrentLat(req.getLat()); d.setCurrentLng(req.getLng());
        driverRepo.save(d);
        log.info("[CabService] Driver registered | id={} name={}", d.getDriverId(), d.getName());
        return d;
    }

    @Transactional
    public Map<String, Object> requestRide(RequestRideRequest req) {
        log.info("[CabService] Ride request | riderId={} type={} pickup=({},{})", req.getRiderId(), req.getVehicleType(), req.getPickupLat(), req.getPickupLng());
        if (!riderRepo.existsById(req.getRiderId())) throw new CabException("Rider not found: " + req.getRiderId(), HttpStatus.NOT_FOUND);
        // Find nearest available driver
        List<Driver> available = driverRepo.findByStatusAndVehicleType(DriverStatus.AVAILABLE, req.getVehicleType());
        Driver driver = available.stream()
            .filter(d -> d.distanceTo(req.getPickupLat(), req.getPickupLng()) <= MAX_RADIUS_KM)
            .min(Comparator.comparingDouble(d -> d.distanceTo(req.getPickupLat(), req.getPickupLng())))
            .orElseThrow(() -> new CabException("No available " + req.getVehicleType() + " driver within " + MAX_RADIUS_KM + "km", HttpStatus.NOT_FOUND));
        double distKm = haversine(req.getPickupLat(), req.getPickupLng(), req.getDropoffLat(), req.getDropoffLng());
        long estimatedFare = calculateFare(req.getVehicleType(), distKm, (long)(distKm * 3));
        Ride ride = new Ride();
        ride.setRideId("RDE-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        ride.setRiderId(req.getRiderId()); ride.setDriverId(driver.getDriverId());
        ride.setVehicleType(req.getVehicleType());
        ride.setPickupLat(req.getPickupLat()); ride.setPickupLng(req.getPickupLng());
        ride.setDropoffLat(req.getDropoffLat()); ride.setDropoffLng(req.getDropoffLng());
        ride.setStatus(RideStatus.DRIVER_ASSIGNED); ride.setEstimatedFarePaise(estimatedFare);
        rideRepo.save(ride);
        driver.setStatus(DriverStatus.ON_TRIP); driverRepo.save(driver);
        log.info("[CabService] Ride assigned | rideId={} driver={} dist={}km estimatedFare=Rs.{}", ride.getRideId(), driver.getName(), String.format("%.2f",distKm), estimatedFare/100.0);
        return Map.of("rideId", ride.getRideId(), "driverName", driver.getName(), "driverPhone", driver.getPhone(),
            "vehicleNumber", driver.getVehicleNumber(), "estimatedFare", String.format("Rs.%.2f", estimatedFare/100.0),
            "distanceKm", String.format("%.2f", distKm), "status", ride.getStatus().name());
    }

    @Transactional
    public Map<String,Object> updateRideStatus(String rideId, String action) {
        Ride ride = getRide(rideId);
        log.info("[CabService] Ride status update | rideId={} action={} currentStatus={}", rideId, action, ride.getStatus());
        switch (action.toUpperCase()) {
            case "ARRIVE"   -> { require(ride, RideStatus.DRIVER_ASSIGNED); ride.setStatus(RideStatus.DRIVER_ARRIVED); }
            case "START"    -> { require(ride, RideStatus.DRIVER_ARRIVED);  ride.setStatus(RideStatus.IN_PROGRESS); ride.setStartedAt(Instant.now()); }
            case "COMPLETE" -> {
                require(ride, RideStatus.IN_PROGRESS); ride.setStatus(RideStatus.COMPLETED); ride.setCompletedAt(Instant.now());
                double dist = haversine(ride.getPickupLat(), ride.getPickupLng(), ride.getDropoffLat(), ride.getDropoffLng());
                long dur = Duration.between(ride.getStartedAt(), ride.getCompletedAt()).toMinutes();
                ride.setActualFarePaise(calculateFare(ride.getVehicleType(), dist, dur));
                Driver d = driverRepo.findById(ride.getDriverId()).orElseThrow();
                d.setStatus(DriverStatus.AVAILABLE); driverRepo.save(d);
                log.info("[CabService] Ride completed | rideId={} fare=Rs.{}", rideId, ride.getActualFarePaise()/100.0);
            }
            case "CANCEL"   -> {
                if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED)
                    throw new CabException("Cannot cancel ride in status: " + ride.getStatus(), HttpStatus.CONFLICT);
                ride.setStatus(RideStatus.CANCELLED);
                if (ride.getDriverId() != null) { Driver d = driverRepo.findById(ride.getDriverId()).orElseThrow(); d.setStatus(DriverStatus.AVAILABLE); driverRepo.save(d); }
            }
            default -> throw new CabException("Unknown action: " + action, HttpStatus.BAD_REQUEST);
        }
        rideRepo.save(ride);
        return Map.of("rideId", ride.getRideId(), "status", ride.getStatus().name(),
            "fare", ride.getActualFarePaise() != null ? String.format("Rs.%.2f",ride.getActualFarePaise()/100.0) : "pending");
    }

    @Transactional
    public void updateLocation(String driverId, LocationUpdateRequest req) {
        Driver d = driverRepo.findById(driverId).orElseThrow(() -> new CabException("Driver not found: " + driverId, HttpStatus.NOT_FOUND));
        d.setCurrentLat(req.getLat()); d.setCurrentLng(req.getLng()); driverRepo.save(d);
        log.debug("[CabService] Location updated | driverId={} lat={} lng={}", driverId, req.getLat(), req.getLng());
    }

    @Transactional
    public Map<String,Object> rateDriver(String rideId, RateRequest req) {
        Ride ride = getRide(rideId);
        if (ride.getStatus() != RideStatus.COMPLETED) throw new CabException("Can only rate completed rides", HttpStatus.CONFLICT);
        Driver d = driverRepo.findById(ride.getDriverId()).orElseThrow();
        d.setRatingSum(d.getRatingSum() + req.getRating()); d.setTotalRatings(d.getTotalRatings() + 1);
        driverRepo.save(d);
        log.info("[CabService] Driver rated | driverId={} rating={} avg={}", d.getDriverId(), req.getRating(), String.format("%.2f",d.getAverageRating()));
        return Map.of("driverName", d.getName(), "newRating", String.format("%.2f", d.getAverageRating()));
    }

    private long calculateFare(VehicleType type, double distKm, long durationMin) {
        double fare = BASE_FARE.getOrDefault(type,40.0) + PER_KM.getOrDefault(type,10.0)*distKm + 1.5*durationMin;
        return Math.round(fare * 100);
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R=6371, dLat=Math.toRadians(lat2-lat1), dLng=Math.toRadians(lng2-lng1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLng/2)*Math.sin(dLng/2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    private Ride getRide(String id) { return rideRepo.findById(id).orElseThrow(() -> new CabException("Ride not found: " + id, HttpStatus.NOT_FOUND)); }
    private void require(Ride ride, RideStatus expected) { if (ride.getStatus() != expected) throw new CabException("Expected " + expected + " but got: " + ride.getStatus(), HttpStatus.CONFLICT); }
}
