package lld.parking.dto;

import jakarta.validation.constraints.*;
import lld.parking.enums.StrategyType;
import lld.parking.enums.VehicleType;
import lombok.Data;

@Data
public class ParkRequest {

    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z0-9-]{4,15}$",
             message = "License plate must be 4-15 alphanumeric characters or hyphens")
    private String licensePlate;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private StrategyType strategy = StrategyType.NEAREST_FLOOR;
}
