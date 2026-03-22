package lld.chess.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateGameRequest {
    @NotBlank(message = "White player name required") private String whitePlayer;
    @NotBlank(message = "Black player name required") private String blackPlayer;
}
