package lld.parkinglot;
import java.util.Objects;

public class ParkingSpot {
    private final String    id;
    private final int       floor;
    private final SpotSize  size;
    private SpotStatus      status;

    public ParkingSpot(String id, int floor, SpotSize size) {
        this.id     = Objects.requireNonNull(id);
        this.floor  = floor;
        this.size   = Objects.requireNonNull(size);
        this.status = SpotStatus.AVAILABLE;
        if (floor < 0) throw new IllegalArgumentException("Floor cannot be negative");
    }

    public synchronized boolean tryOccupy(VehicleType vehicleType) {
        if (status != SpotStatus.AVAILABLE) return false;
        if (!vehicleType.fitsIn(size))      return false;
        status = SpotStatus.OCCUPIED;
        return true;
    }

    public synchronized void release() {
        if (status != SpotStatus.OCCUPIED)
            throw new ParkingException("Spot " + id + " is not occupied");
        status = SpotStatus.AVAILABLE;
    }

    public synchronized void setMaintenance(boolean maintenance) {
        if (maintenance && status == SpotStatus.OCCUPIED)
            throw new ParkingException("Cannot mark occupied spot for maintenance: " + id);
        status = maintenance ? SpotStatus.MAINTENANCE : SpotStatus.AVAILABLE;
    }

    public String     getId()       { return id; }
    public int        getFloor()    { return floor; }
    public SpotSize   getSize()     { return size; }
    public SpotStatus getStatus()   { return status; }
    public boolean    isAvailable() { return status == SpotStatus.AVAILABLE; }

    @Override public String toString() {
        return String.format("[%s F%d %s %s]", id, floor, size, status);
    }
}
