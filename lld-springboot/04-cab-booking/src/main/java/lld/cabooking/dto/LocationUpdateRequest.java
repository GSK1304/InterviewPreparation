package lld.cabooking.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class LocationUpdateRequest {
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")   private Double lat;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double lng;
}
