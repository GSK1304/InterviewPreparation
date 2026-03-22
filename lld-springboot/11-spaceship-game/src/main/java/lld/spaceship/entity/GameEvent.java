package lld.spaceship.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "game_event") @Getter @Setter @NoArgsConstructor
public class GameEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "game_id", nullable = false) private Long gameId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(nullable = false) private String details;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();
}
