package lld.chess.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MakeMoveRequest {
    @NotBlank(message = "Player name required") private String playerName;
    @NotNull @Min(0) @Max(7) private Integer fromCol;
    @NotNull @Min(0) @Max(7) private Integer fromRow;
    @NotNull @Min(0) @Max(7) private Integer toCol;
    @NotNull @Min(0) @Max(7) private Integer toRow;
}
