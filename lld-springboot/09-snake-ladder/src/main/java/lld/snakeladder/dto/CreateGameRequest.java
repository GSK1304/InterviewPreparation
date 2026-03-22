package lld.snakeladder.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
@Data
public class CreateGameRequest {
    @NotEmpty @Size(min=2, max=6, message="Need 2-6 players")
    @Valid
    private List<PlayerInfo> players;

    @Data
    public static class PlayerInfo {
        @NotBlank private String name;
        @NotBlank @Size(max=5) private String token;
    }
}
