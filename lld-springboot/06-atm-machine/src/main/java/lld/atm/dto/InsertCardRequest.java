package lld.atm.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class InsertCardRequest {
    @NotBlank @Pattern(regexp="\\d{16}", message="Card number must be 16 digits") private String cardNumber;
}
