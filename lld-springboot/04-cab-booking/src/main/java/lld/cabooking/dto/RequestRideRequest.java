package lld.cabooking.dto;
import jakarta.validation.constraints.*;
import lld.cabooking.enums.VehicleType;
import lombok.Data;
@Data
public class RequestRideRequest {
    @NotBlank private String riderId;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double pickupLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double pickupLng;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double dropoffLat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double dropoffLng;
    @NotNull private VehicleType vehicleType;
}
