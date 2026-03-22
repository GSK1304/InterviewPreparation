package lld.cabooking;

public class CabBookingDemo {
    public static void main(String[] args) {
        CabBookingService service = new CabBookingService(new StandardFareStrategy(), new NearestDriverStrategy());
        service.addListener((evt, ride) -> System.out.printf("  [%s] %s -> %s%n", evt, ride.getRideId(), ride.getStatus()));

        Location hitech     = new Location(17.4474, 78.3762);
        Location gachibowli = new Location(17.4401, 78.3489);
        Location madhapur   = new Location(17.4485, 78.3908);

        Vehicle auto  = new Vehicle("V1", "TS09EA1234", VehicleType.AUTO,  "Bajaj RE");
        Vehicle sedan = new Vehicle("V2", "TS10CB5678", VehicleType.SEDAN, "Swift");
        service.registerDriver("D1", "Ravi Kumar",   "9876543210", auto,  hitech);
        service.registerDriver("D2", "Suresh Reddy", "9876543211", sedan, gachibowli);
        service.registerRider("R1", "Priya Singh", "9111222333", "priya@example.com");
        service.registerRider("R2", "Arjun Nair",  "9444555666", "arjun@example.com");

        System.out.println("\n=== Complete Auto Ride ===");
        Ride r1 = service.requestRide("R1", hitech, gachibowli, VehicleType.AUTO);
        service.driverArrived(r1.getRideId());
        service.startRide(r1.getRideId());
        service.completeRide(r1.getRideId());
        service.rateDriver(r1.getRideId(), 4.5, "Good driver");

        System.out.println("\n=== Surge Pricing (1.5x) ===");
        CabBookingService surgeService = new CabBookingService(
            new SurgeFareStrategy(new StandardFareStrategy(), 1.5), new NearestDriverStrategy());
        surgeService.registerDriver("D3", "Vijay",  "9777888999", sedan, hitech);
        surgeService.registerRider("R3",  "Kavya",  "9222333444", "kavya@example.com");
        Ride sr = surgeService.requestRide("R3", hitech, madhapur, VehicleType.SEDAN);
        surgeService.startRide(sr.getRideId());
        System.out.println("Surge fare: " + surgeService.completeRide(sr.getRideId()));

        System.out.println("\n=== Cancellation ===");
        Ride r3 = service.requestRide("R2", hitech, madhapur, VehicleType.SEDAN);
        service.cancelRide(r3.getRideId(), CancelReason.RIDER_CANCELLED);

        System.out.println("\n=== No Driver Available ===");
        try { service.requestRide("R1", gachibowli, hitech, VehicleType.SUV); }
        catch (NoDriverAvailableException e) { System.out.println("Error: " + e.getMessage()); }

        System.out.println("\n=== Wrong State ===");
        Ride r5 = service.requestRide("R2", hitech, gachibowli, VehicleType.SEDAN);
        try { service.startRide(r5.getRideId()); }
        catch (InvalidRideStateException e) { System.out.println("Error: " + e.getMessage()); }

        service.displayDriverStats();
    }
}
