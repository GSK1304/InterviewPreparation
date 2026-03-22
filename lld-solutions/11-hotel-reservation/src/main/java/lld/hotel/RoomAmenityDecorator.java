package lld.hotel;
import java.util.Objects;
public abstract class RoomAmenityDecorator implements RoomService {
    protected final RoomService wrapped;
    public RoomAmenityDecorator(RoomService wrapped) { this.wrapped = Objects.requireNonNull(wrapped); }
}
