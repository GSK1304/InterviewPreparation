package lld.parkinglot;
import java.util.Objects;

public record Vehicle(String licensePlate, VehicleType type) {
    public Vehicle {
        Objects.requireNonNull(licensePlate, "License plate required");
        Objects.requireNonNull(type, "Vehicle type required");
        if (licensePlate.isBlank()) throw new IllegalArgumentException("License plate cannot be blank");
        if (!licensePlate.matches("[A-Z0-9-]+"))
            throw new IllegalArgumentException("License plate must be alphanumeric: " + licensePlate);
    }
}
