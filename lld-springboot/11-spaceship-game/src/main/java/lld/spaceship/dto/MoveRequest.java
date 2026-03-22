package lld.spaceship.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MoveRequest {
    @NotNull @Min(-800) @Max(800) private Integer deltaX;
    private boolean fire = false;
}
