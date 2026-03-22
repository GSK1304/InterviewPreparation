package lld.atm.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class WithdrawRequest {
    @NotNull @Min(value=100, message="Minimum withdrawal Rs.100")
    @Max(value=50000, message="Maximum withdrawal Rs.50,000") private Long amountRupees;
}
