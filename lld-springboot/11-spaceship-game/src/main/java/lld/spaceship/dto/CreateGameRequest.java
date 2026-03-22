package lld.spaceship.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateGameRequest {
    @NotBlank(message = "Player name required")
    @Size(min=2, max=50, message="Player name must be 2-50 characters")
    private String playerName;
}
