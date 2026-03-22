package lld.library.dto;
import jakarta.validation.constraints.*;
import lld.library.enums.MemberType;
import lombok.Data;
@Data
public class RegisterMemberRequest {
    @NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{2,20}$") private String memberId;
    @NotBlank @Size(min=2, max=100) private String name;
    @NotBlank @Email private String email;
    @NotNull private MemberType type;
}
