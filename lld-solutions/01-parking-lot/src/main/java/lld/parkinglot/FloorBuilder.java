package lld.parkinglot;
import java.util.ArrayList;
import java.util.List;

public class FloorBuilder {
    private final int               floorNumber;
    private final List<ParkingSpot> spots = new ArrayList<>();
    private int spotCounter = 1;

    public FloorBuilder(int floorNumber) { this.floorNumber = floorNumber; }

    public FloorBuilder addSpots(int count, SpotSize size) {
        if (count <= 0) throw new IllegalArgumentException("Count must be positive");
        for (int i = 0; i < count; i++) {
            String id = String.format("F%d-%s-%02d", floorNumber, size.name().charAt(0), spotCounter++);
            spots.add(new ParkingSpot(id, floorNumber, size));
        }
        return this;
    }

    public ParkingFloor build() { return new ParkingFloor(floorNumber, spots); }
}
