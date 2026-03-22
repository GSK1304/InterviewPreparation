package lld.snakeladder.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "game_player") @Getter @Setter @NoArgsConstructor
public class GamePlayer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "game_id") private Game game;
    @Column(name = "player_name", nullable = false) private String playerName;
    @Column(name = "player_token", nullable = false) private String playerToken;
    @Column(name = "player_index", nullable = false) private Integer playerIndex;
    @Column(nullable = false) private Integer position = 0;
    @Column(name = "snake_bites", nullable = false) private Integer snakeBites = 0;
    @Column(name = "ladders_climbed", nullable = false) private Integer laddersClimbed = 0;
    @Column(name = "turns_played", nullable = false) private Integer turnsPlayed = 0;
}
