package lld.spaceship.entity;
import jakarta.persistence.*;
import lld.spaceship.enums.GameStatus;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "space_game") @Getter @Setter @NoArgsConstructor
public class SpaceGame {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "player_name", nullable = false) private String playerName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GameStatus status = GameStatus.ACTIVE;
    @Column(nullable = false) private Integer score = 0;
    @Column(nullable = false) private Integer lives = 3;
    @Column(name = "player_x", nullable = false) private Integer playerX = 400;
    @Column(name = "player_y", nullable = false) private Integer playerY = 550;
    @Column(name = "shield_active", nullable = false) private Boolean shieldActive = false;
    @Column(name = "rapid_fire", nullable = false) private Boolean rapidFire = false;
    @Column(name = "wave_number", nullable = false) private Integer waveNumber = 1;
    @Column(name = "enemies_killed", nullable = false) private Integer enemiesKilled = 0;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "last_updated", nullable = false) private Instant lastUpdated = Instant.now();
}
