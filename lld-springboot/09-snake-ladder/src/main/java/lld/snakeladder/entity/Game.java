package lld.snakeladder.entity;
import jakarta.persistence.*;
import lld.snakeladder.enums.GameStatus;
import lombok.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name = "game") @Getter @Setter @NoArgsConstructor
public class Game {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "board_size", nullable = false) private Integer boardSize = 100;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GameStatus status = GameStatus.WAITING;
    @Column(name = "current_player_index", nullable = false) private Integer currentPlayerIndex = 0;
    @Column(name = "total_turns", nullable = false) private Integer totalTurns = 0;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "winner_name") private String winnerName;
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("playerIndex ASC")
    private List<GamePlayer> players = new ArrayList<>();
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<BoardCell> cells = new ArrayList<>();
}
