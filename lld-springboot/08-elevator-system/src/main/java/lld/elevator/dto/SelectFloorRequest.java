package lld.elevator.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class SelectFloorRequest {
    @NotBlank private String elevatorId;
    @NotNull @Min(0) @Max(50) private Integer floorNumber;
}
