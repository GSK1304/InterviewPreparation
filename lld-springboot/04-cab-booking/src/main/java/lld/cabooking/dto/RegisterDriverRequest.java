package lld.cabooking.dto;
import jakarta.validation.constraints.*;
import lld.cabooking.enums.VehicleType;
import lombok.Data;
@Data
public class RegisterDriverRequest {
    @NotBlank private String driverId;
    @NotBlank @Size(min=2, max=100) private String name;
    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$", message="Invalid mobile number") private String phone;
    @NotBlank private String vehicleNumber;
    @NotBlank private String vehicleModel;
    @NotNull private VehicleType vehicleType;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double lat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double lng;
}
