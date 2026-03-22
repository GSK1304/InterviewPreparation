package lld.chess.dto;
import lld.chess.enums.GameStatus;
import lld.chess.enums.PieceColor;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class MoveResponse {
    private String     playerName;
    private String     piece;
    private String     from;
    private String     to;
    private String     capturedPiece;
    private boolean    isCheck;
    private boolean    isCheckmate;
    private GameStatus gameStatus;
    private PieceColor nextTurn;
    private int        totalMoves;
    private String     algebraicNotation;
    private String     message;
}
