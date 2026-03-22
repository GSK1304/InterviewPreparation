package lld.cabooking;
import java.util.Objects;
public record Vehicle(String vehicleId, String licensePlate, VehicleType type, String model) {
    public Vehicle {
        Objects.requireNonNull(vehicleId); Objects.requireNonNull(licensePlate);
        Objects.requireNonNull(type);      Objects.requireNonNull(model);
        if (licensePlate.isBlank()) throw new IllegalArgumentException("License plate required");
    }
}
