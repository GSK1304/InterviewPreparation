package lld.snakeladder.entity;
import jakarta.persistence.*;
import lld.snakeladder.enums.CellType;
import lombok.*;
@Entity @Table(name = "board_cell") @Getter @Setter @NoArgsConstructor
public class BoardCell {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "game_id") private Game game;
    @Column(name = "position", nullable = false) private Integer position;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CellType type = CellType.NORMAL;
    @Column(name = "target_position") private Integer targetPosition;
}
