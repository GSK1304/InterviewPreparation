package lld.cabooking.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class RateRequest {
    @NotNull @DecimalMin("1.0") @DecimalMax("5.0") private Double rating;
    private String comment;
}
