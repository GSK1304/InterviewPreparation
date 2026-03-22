package lld.elevator.entity;
import jakarta.persistence.*;
import lld.elevator.enums.Direction;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "elevator_request") @Getter @Setter @NoArgsConstructor
public class ElevatorRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "floor_number", nullable = false) private Integer floorNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Direction direction;
    @Column(name = "assigned_elevator_id") private String assignedElevatorId;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt = Instant.now();
    @Column(name = "fulfilled", nullable = false) private Boolean fulfilled = false;
}
