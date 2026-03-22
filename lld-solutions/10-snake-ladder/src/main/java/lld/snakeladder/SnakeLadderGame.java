package lld.snakeladder;
import java.util.*;

public class SnakeLadderGame {
    private final Board        board;
    private final DiceStrategy dice;
    private final GameConfig   config;
    private final List<Player> players;
    private int                currentIdx = 0, totalTurns = 0;
    private boolean            gameOver   = false;
    private Player             winner     = null;

    private SnakeLadderGame(Builder b) {
        this.board   = b.board; this.dice   = b.dice; this.config  = b.config;
        this.players = Collections.unmodifiableList(b.players);
        if (players.isEmpty()) throw new GameException("At least one player required");
        if (players.size() > 6) throw new GameException("Max 6 players");
    }

    public Optional<Player> playTurn() {
        if (gameOver) throw new GameException("Game is already over");
        Player cur = players.get(currentIdx);
        cur.incrementTurns(); totalTurns++;

        if (cur.shouldSkipTurn()) {
            System.out.printf("  %s skips (snake penalty)%n", cur.getName());
            advance(false); return Optional.empty();
        }

        int rolled = dice.roll();
        System.out.printf("%n%s %s rolls: %d%n", cur.getToken(), cur.getName(), rolled);

        int newPos = cur.getPosition() + rolled;
        if (config.exactFinish() && newPos > config.boardSize()) {
            System.out.printf("  Needs exact roll to finish. Stays at %d%n", cur.getPosition());
            advance(false); return Optional.empty();
        }

        newPos = Math.min(newPos, config.boardSize());
        cur.moveTo(newPos);
        System.out.printf("  Moved to: %d%n", newPos);

        Optional<BoardEntity> entity = board.getEntityAt(newPos);
        if (entity.isPresent()) {
            BoardEntity e = entity.get();
            if (e.getType() == BoardEntityType.SNAKE) {
                System.out.printf("  Snake at %d! Down to %d%n", e.getFrom(), e.getTo());
                cur.moveTo(e.getTo()); cur.incrementSnakeBites();
                if (config.skipOnSnake()) cur.setSkipNextTurn(true);
            } else if (e.getType() == BoardEntityType.LADDER) {
                System.out.printf("  Ladder at %d! Up to %d%n", e.getFrom(), e.getTo());
                cur.moveTo(e.getTo()); cur.incrementLaddersClimbed();
            }
        }

        System.out.printf("  Final: %d%n", cur.getPosition());

        if (cur.getPosition() >= config.boardSize()) {
            gameOver = true; winner = cur;
            System.out.printf("%n%s %s WINS in %d turns!%n", cur.getToken(), cur.getName(), cur.getTurnsPlayed());
            printStats(); return Optional.of(cur);
        }

        boolean bonus = config.bonusTurnOnSix() && rolled == dice.getMaxValue();
        if (bonus) System.out.printf("  Bonus turn!%n");
        advance(bonus);
        return Optional.empty();
    }

    public Player playToEnd() { while (!gameOver) playTurn(); return winner; }
    public void   playTurns(int n) { for (int i = 0; i < n && !gameOver; i++) playTurn(); }

    private void advance(boolean sameTurn) { if (!sameTurn) currentIdx = (currentIdx + 1) % players.size(); }

    public void displayPositions() {
        System.out.println("\n--- Positions ---");
        players.forEach(p -> System.out.printf("  %s %-12s -> %d%n", p.getToken(), p.getName(), p.getPosition()));
    }

    private void printStats() {
        System.out.println("\n--- Stats ---");
        players.forEach(p -> System.out.printf("  %s %-12s | Turns: %d | Snakes: %d | Ladders: %d%n",
            p.getToken(), p.getName(), p.getTurnsPlayed(), p.getSnakeBites(), p.getLaddersClimbed()));
    }

    public boolean isGameOver() { return gameOver; }
    public Player  getWinner()  { return winner; }

    public static final class Builder {
        private Board        board;
        private DiceStrategy dice   = new FairDice();
        private GameConfig   config = GameConfig.defaultConfig();
        private final List<Player> players = new ArrayList<>();

        public Builder board(Board board)           { this.board  = board;  return this; }
        public Builder dice(DiceStrategy dice)      { this.dice   = dice;   return this; }
        public Builder config(GameConfig config)    { this.config = config; return this; }
        public Builder addPlayer(String id, String name, String token) {
            if (players.size() >= 6) throw new GameException("Max 6 players");
            players.add(new Player(id, name, token)); return this;
        }
        public SnakeLadderGame build() {
            if (board == null) throw new GameException("Board required");
            return new SnakeLadderGame(this);
        }
    }
}
