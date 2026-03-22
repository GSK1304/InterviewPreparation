package lld.library.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ReturnRequest {
    @NotBlank(message = "Barcode is required") private String barcode;
}
