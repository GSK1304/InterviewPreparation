package lld.atm.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class DepositRequest {
    @NotNull @Min(value=100, message="Minimum deposit Rs.100") private Long amountRupees;
}
