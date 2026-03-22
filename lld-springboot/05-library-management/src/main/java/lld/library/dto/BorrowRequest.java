package lld.library.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class BorrowRequest {
    @NotBlank(message = "Member ID is required") private String memberId;
    @NotBlank(message = "ISBN is required")      private String isbn;
}
