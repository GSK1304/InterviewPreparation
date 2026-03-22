package lld.library.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class PayFineRequest {
    @NotNull @DecimalMin("1.0") private Double amountRupees;
}
