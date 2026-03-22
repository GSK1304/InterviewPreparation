package lld.snakeladder.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class TurnResponse {
    private String  playerName;
    private String  token;
    private int     diceRoll;
    private int     fromPosition;
    private int     toPosition;
    private String  event;
    private boolean gameOver;
    private String  winner;
    private String  message;
}
