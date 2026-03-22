package lld.splitwise.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class CreateUserRequest {
    @NotBlank(message = "User ID is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,20}$", message = "User ID must be 2-20 alphanumeric characters")
    private String userId;
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;
}
