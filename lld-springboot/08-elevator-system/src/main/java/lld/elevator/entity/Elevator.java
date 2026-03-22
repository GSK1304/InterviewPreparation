package lld.elevator.entity;
import jakarta.persistence.*;
import lld.elevator.enums.Direction;
import lld.elevator.enums.ElevatorStatus;
import lombok.*;
@Entity @Table(name = "elevator") @Getter @Setter @NoArgsConstructor
public class Elevator {
    @Id @Column(name = "elevator_id") private String elevatorId;
    @Column(nullable = false) private String name;
    @Column(name = "current_floor", nullable = false) private Integer currentFloor;
    @Column(name = "min_floor", nullable = false) private Integer minFloor;
    @Column(name = "max_floor", nullable = false) private Integer maxFloor;
    @Column(name = "max_capacity", nullable = false) private Integer maxCapacity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ElevatorStatus status = ElevatorStatus.IDLE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Direction direction = Direction.IDLE;
    @Column(name = "pending_stops") private String pendingStops = "";
}
