package lld.chess.entity;
import jakarta.persistence.*;
import lld.chess.enums.PieceColor;
import lld.chess.enums.PieceType;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "chess_move") @Getter @Setter @NoArgsConstructor
public class ChessMove {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "game_id") private ChessGame game;
    @Column(name = "move_number", nullable = false) private Integer moveNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PieceColor color;
    @Enumerated(EnumType.STRING) @Column(name = "piece_type", nullable = false) private PieceType pieceType;
    @Column(name = "from_col", nullable = false) private Integer fromCol;
    @Column(name = "from_row", nullable = false) private Integer fromRow;
    @Column(name = "to_col", nullable = false) private Integer toCol;
    @Column(name = "to_row", nullable = false) private Integer toRow;
    @Column(name = "captured_piece") private String capturedPiece;
    @Column(name = "is_check", nullable = false) private Boolean isCheck = false;
    @Column(name = "is_checkmate", nullable = false) private Boolean isCheckmate = false;
    @Column(name = "algebraic_notation") private String algebraicNotation;
    @Column(name = "played_at", nullable = false) private Instant playedAt = Instant.now();
}
