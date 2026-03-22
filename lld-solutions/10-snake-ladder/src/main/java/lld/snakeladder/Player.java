package lld.snakeladder;
import java.util.Objects;
public class Player {
    private final String playerId, name, token;
    private int  position = 0, snakeBites = 0, laddersClimbed = 0, turnsPlayed = 0;
    private boolean skipNextTurn = false;

    public Player(String playerId, String name, String token) {
        this.playerId = Objects.requireNonNull(playerId); this.name  = Objects.requireNonNull(name);
        this.token    = Objects.requireNonNull(token);
        if (name.isBlank()) throw new IllegalArgumentException("Name required");
    }

    public void moveTo(int pos)           { position = pos; }
    public void incrementSnakeBites()     { snakeBites++; }
    public void incrementLaddersClimbed() { laddersClimbed++; }
    public void incrementTurns()          { turnsPlayed++; }
    public void setSkipNextTurn(boolean v){ skipNextTurn = v; }
    public boolean shouldSkipTurn()       { if (skipNextTurn) { skipNextTurn = false; return true; } return false; }

    public String getPlayerId()      { return playerId; }
    public String getName()          { return name; }
    public String getToken()         { return token; }
    public int    getPosition()      { return position; }
    public int    getSnakeBites()    { return snakeBites; }
    public int    getLaddersClimbed(){ return laddersClimbed; }
    public int    getTurnsPlayed()   { return turnsPlayed; }
    @Override public String toString(){ return String.format("Player[%s %s pos=%d]", token, name, position); }
}
