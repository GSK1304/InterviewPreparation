package lld.elevator.dto;
import jakarta.validation.constraints.*;
import lld.elevator.enums.Direction;
import lombok.Data;
@Data
public class CallElevatorRequest {
    @NotNull @Min(0) @Max(50) private Integer floorNumber;
    @NotNull private Direction direction;
    private lld.elevator.enums.DispatchStrategy strategy;
}
