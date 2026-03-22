package lld.atm.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class EnterPinRequest {
    @NotBlank @Pattern(regexp="\\d{4}", message="PIN must be 4 digits") private String pin;
}
