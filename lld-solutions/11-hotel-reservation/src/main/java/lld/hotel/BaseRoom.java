package lld.hotel;
import java.util.Objects;

public class BaseRoom implements RoomService {
    private final String   roomId;
    private final RoomType type;
    private final BedType  bedType;
    private final int      capacity, floorNumber;
    private final Money    baseRate;

    public BaseRoom(String roomId, RoomType type, BedType bedType, int capacity, int floorNumber, Money baseRate) {
        this.roomId      = Objects.requireNonNull(roomId); this.type    = Objects.requireNonNull(type);
        this.bedType     = Objects.requireNonNull(bedType); this.baseRate = Objects.requireNonNull(baseRate);
        this.capacity    = capacity; this.floorNumber = floorNumber;
        if (capacity < 1)          throw new IllegalArgumentException("Capacity must be >= 1");
        if (baseRate.paise() <= 0) throw new IllegalArgumentException("Base rate must be positive");
    }

    public String   getRoomId()     { return roomId; }
    public RoomType getType()       { return type; }
    public BedType  getBedType()    { return bedType; }
    public int      getCapacity()   { return capacity; }
    public int      getFloorNumber(){ return floorNumber; }

    @Override public String getDescription() {
        return String.format("%s Room %s | %s bed | Floor %d | Cap: %d", type, roomId, bedType, floorNumber, capacity);
    }
    @Override public Money getDailyRate() { return baseRate; }
}
