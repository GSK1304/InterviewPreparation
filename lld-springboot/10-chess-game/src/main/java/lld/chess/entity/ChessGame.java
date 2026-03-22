package lld.chess.entity;
import jakarta.persistence.*;
import lld.chess.enums.GameStatus;
import lld.chess.enums.PieceColor;
import lombok.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name = "chess_game") @Getter @Setter @NoArgsConstructor
public class ChessGame {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "white_player", nullable = false) private String whitePlayer;
    @Column(name = "black_player", nullable = false) private String blackPlayer;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GameStatus status = GameStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(name = "current_turn", nullable = false) private PieceColor currentTurn = PieceColor.WHITE;
    @Column(name = "total_moves", nullable = false) private Integer totalMoves = 0;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "board_state", length = 512) private String boardState; // FEN-lite: "piece@col,row" CSV
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("moveNumber ASC")
    private List<ChessMove> moves = new ArrayList<>();
}
