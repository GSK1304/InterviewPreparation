package lld.parkinglot;

public class ParkingLotDemo {
    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot.Builder("Phoenix Mall Parking")
            .addFloor(new FloorBuilder(0)
                .addSpots(5, SpotSize.MOTORCYCLE)
                .addSpots(10, SpotSize.COMPACT)
                .addSpots(3, SpotSize.LARGE)
                .build())
            .addFloor(new FloorBuilder(1)
                .addSpots(5, SpotSize.MOTORCYCLE)
                .addSpots(10, SpotSize.COMPACT)
                .addSpots(3, SpotSize.LARGE)
                .build())
            .addFloor(new FloorBuilder(2)
                .addSpots(2, SpotSize.MOTORCYCLE)
                .addSpots(8, SpotSize.COMPACT)
                .addSpots(5, SpotSize.LARGE)
                .build())
            .strategy(new NearestFloorStrategy())
            .build();

        lot.displayAvailability();

        Vehicle bike  = new Vehicle("KA01HB1234", VehicleType.MOTORCYCLE);
        Vehicle car1  = new Vehicle("MH02CB5678", VehicleType.CAR);
        Vehicle car2  = new Vehicle("TN03DE9012", VehicleType.CAR);
        Vehicle truck = new Vehicle("DL04EF3456", VehicleType.TRUCK);

        ParkingTicket bikeTicket  = lot.park(bike);
        ParkingTicket carTicket1  = lot.park(car1);
        ParkingTicket carTicket2  = lot.park(car2);
        ParkingTicket truckTicket = lot.park(truck);

        lot.displayAvailability();
        lot.unpark(carTicket1.ticketId());

        try { lot.unpark("TKT-INVALID"); }
        catch (InvalidTicketException e) { System.out.println("❌ " + e.getMessage()); }

        try { lot.park(new Vehicle("MH02CB5678", VehicleType.CAR)); }
        catch (VehicleAlreadyParkedException e) { System.out.println("❌ " + e.getMessage()); }

        lot.markMaintenance("F0-M-02", 0, true);
        lot.displayAvailability();

        lot.unpark(bikeTicket.ticketId());
        lot.unpark(carTicket2.ticketId());
        lot.unpark(truckTicket.ticketId());

        System.out.println("\nActive tickets remaining: " + lot.getActiveTicketCount());
    }
}
