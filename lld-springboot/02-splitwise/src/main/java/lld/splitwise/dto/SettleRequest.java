package lld.splitwise.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class SettleRequest {
    @NotBlank(message = "Payer ID is required")
    private String payerUserId;
    @NotBlank(message = "Receiver ID is required")
    private String receiverUserId;
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Settlement must be at least Re.1")
    private Double amountRupees;
}
