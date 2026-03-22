# LLD — Snake and Ladder Game — Complete Java 21

## Design Summary
| Aspect | Decision |
|--------|----------|
| Board config | Builder pattern — snakes and ladders added declaratively |
| Game entities | Sealed interface `BoardEntity` — Snake | Ladder | Empty |
| Dice | Strategy — `DiceStrategy` (Fair, Loaded, MultiDice) |
| Players | Supports 2–6 players with configurable names |
| Win condition | Exactly land on 100 OR first to reach ≥ 100 (configurable) |
| Turn tracking | Circular iteration via index |
| Special rules | Bonus turn on 6, skip turn on snake bite (configurable) |

## Complete Solution

```java
package lld.snakeladder;

import java.util.*;
import java.util.stream.*;

// ── Sealed Board Entities ─────────────────────────────────────────────────────

sealed interface BoardEntity permits BoardEntity.Snake, BoardEntity.Ladder, BoardEntity.Empty {

    record Snake(int head, int tail) implements BoardEntity {
        Snake {
            if (head <= tail)
                throw new IllegalArgumentException(
                    "Snake head must be above tail. head=" + head + " tail=" + tail);
            if (head < 2 || head > 99 || tail < 1 || tail > 98)
                throw new IllegalArgumentException(
                    "Snake positions must be between 1-99 (head) and 1-98 (tail)");
        }
        public int from() { return head; }
        public int to()   { return tail; }
    }

    record Ladder(int bottom, int top) implements BoardEntity {
        Ladder {
            if (bottom >= top)
                throw new IllegalArgumentException(
                    "Ladder bottom must be below top. bottom=" + bottom + " top=" + top);
            if (bottom < 1 || bottom > 98 || top < 2 || top > 100)
                throw new IllegalArgumentException(
                    "Ladder positions must be between 1-98 (bottom) and 2-100 (top)");
        }
        public int from() { return bottom; }
        public int to()   { return top; }
    }

    record Empty() implements BoardEntity {}
}

// ── Dice Strategy ─────────────────────────────────────────────────────────────

interface DiceStrategy {
    int roll();
    int getMaxValue();
}

class FairDice implements DiceStrategy {
    private final int   faces;
    private final Random random = new Random();

    FairDice(int faces) {
        if (faces < 2) throw new IllegalArgumentException("Dice must have at least 2 faces");
        this.faces = faces;
    }

    FairDice() { this(6); }

    @Override public int roll()        { return random.nextInt(faces) + 1; }
    @Override public int getMaxValue() { return faces; }
}

class MultiDice implements DiceStrategy {
    private final List<DiceStrategy> dice;

    MultiDice(int numberOfDice, int facesPerDie) {
        if (numberOfDice < 1) throw new IllegalArgumentException("Need at least one die");
        dice = IntStream.range(0, numberOfDice)
            .mapToObj(i -> new FairDice(facesPerDie))
            .collect(Collectors.toList());
    }

    @Override public int roll()        { return dice.stream().mapToInt(DiceStrategy::roll).sum(); }
    @Override public int getMaxValue() { return dice.stream().mapToInt(DiceStrategy::getMaxValue).sum(); }
}

class LoadedDice implements DiceStrategy {
    private final int fixedValue;  // for testing — always rolls same value

    LoadedDice(int value) {
        if (value < 1 || value > 6)
            throw new IllegalArgumentException("Dice value must be 1-6");
        this.fixedValue = value;
    }

    @Override public int roll()        { return fixedValue; }
    @Override public int getMaxValue() { return 6; }
}

// ── Player ────────────────────────────────────────────────────────────────────

class Player {
    private final String  playerId;
    private final String  name;
    private final String  token;   // display symbol
    private int           position = 0;  // starts off the board
    private int           snakeBites = 0;
    private int           laddersClimbed = 0;
    private int           turnsPlayed = 0;
    private boolean       skipNextTurn = false;

    Player(String playerId, String name, String token) {
        this.playerId = Objects.requireNonNull(playerId);
        this.name     = Objects.requireNonNull(name);
        this.token    = Objects.requireNonNull(token);
        if (name.isBlank()) throw new IllegalArgumentException("Player name required");
    }

    void moveTo(int pos)           { this.position = pos; }
    void incrementSnakeBites()     { snakeBites++; }
    void incrementLaddersClimbed() { laddersClimbed++; }
    void incrementTurns()          { turnsPlayed++; }
    void setSkipNextTurn(boolean v){ skipNextTurn = v; }

    boolean shouldSkipTurn() {
        if (skipNextTurn) { skipNextTurn = false; return true; }
        return false;
    }

    public String  getPlayerId()      { return playerId; }
    public String  getName()          { return name; }
    public String  getToken()         { return token; }
    public int     getPosition()      { return position; }
    public int     getSnakeBites()    { return snakeBites; }
    public int     getLaddersClimbed(){ return laddersClimbed; }
    public int     getTurnsPlayed()   { return turnsPlayed; }

    @Override public String toString() {
        return String.format("Player[%s %s at pos=%d]", token, name, position);
    }
}

// ── Game Configuration ────────────────────────────────────────────────────────

record GameConfig(
    boolean exactFinishRequired,  // must land exactly on 100
    boolean bonusTurnOnSix,       // roll again if 6
    boolean skipTurnOnSnake,      // skip next turn after snake bite
    int     boardSize             // default 100
) {
    static GameConfig defaultConfig() {
        return new GameConfig(false, true, false, 100);
    }
}

// ── Board ─────────────────────────────────────────────────────────────────────

class Board {
    private final int                          size;
    private final Map<Integer, BoardEntity>    entities;   // position → entity
    private final Map<Integer, Integer>        teleports;  // from → to (snake+ladder combined)

    private Board(Builder builder) {
        this.size     = builder.size;
        this.entities = Collections.unmodifiableMap(builder.entities);
        this.teleports = Collections.unmodifiableMap(builder.teleports);
    }

    /** Returns final position after applying any snake/ladder at the given position */
    int applyEffect(int position) {
        return teleports.getOrDefault(position, position);
    }

    Optional<BoardEntity> getEntityAt(int position) {
        return Optional.ofNullable(entities.get(position));
    }

    boolean isSnakeHead(int position) {
        return entities.get(position) instanceof BoardEntity.Snake;
    }

    boolean isLadderBottom(int position) {
        return entities.get(position) instanceof BoardEntity.Ladder;
    }

    int getSize() { return size; }

    void display() {
        System.out.println("\n═══ Board ═══");
        for (int row = size / 10; row >= 1; row--) {
            int start = (row - 1) * 10 + 1;
            int end   = row * 10;
            // Alternate direction: odd rows left-to-right, even rows right-to-left
            boolean leftToRight = (row % 2 == 1);
            IntStream range = leftToRight
                ? IntStream.rangeClosed(start, end)
                : IntStream.iterate(end, i -> i >= start, i -> i - 1);
            range.forEach(pos -> {
                String marker = "";
                if (entities.containsKey(pos)) {
                    marker = switch (entities.get(pos)) {
                        case BoardEntity.Snake  s -> "S";
                        case BoardEntity.Ladder l -> "L";
                        default                   -> " ";
                    };
                }
                System.out.printf("%3d%-1s", pos, marker);
            });
            System.out.println();
        }
        System.out.println("S=Snake Head, L=Ladder Bottom\n");
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    static final class Builder {
        private final int                       size;
        private final Map<Integer, BoardEntity> entities  = new LinkedHashMap<>();
        private final Map<Integer, Integer>     teleports = new HashMap<>();

        Builder(int size) {
            if (size < 10) throw new IllegalArgumentException("Board size must be at least 10");
            if (size % 10 != 0) throw new IllegalArgumentException("Board size must be multiple of 10");
            this.size = size;
        }

        Builder addSnake(int head, int tail) {
            if (teleports.containsKey(head))
                throw new IllegalArgumentException("Position already occupied: " + head);
            BoardEntity.Snake snake = new BoardEntity.Snake(head, tail);
            entities.put(head, snake);
            teleports.put(head, tail);
            return this;
        }

        Builder addLadder(int bottom, int top) {
            if (teleports.containsKey(bottom))
                throw new IllegalArgumentException("Position already occupied: " + bottom);
            BoardEntity.Ladder ladder = new BoardEntity.Ladder(bottom, top);
            entities.put(bottom, ladder);
            teleports.put(bottom, top);
            return this;
        }

        Board build() { return new Board(this); }
    }
}

// ── Exceptions ────────────────────────────────────────────────────────────────

class GameException extends RuntimeException {
    GameException(String msg) { super(msg); }
}

// ── Game ──────────────────────────────────────────────────────────────────────

class SnakeLadderGame {
    private final Board        board;
    private final DiceStrategy dice;
    private final GameConfig   config;
    private final List<Player> players;
    private int                currentPlayerIndex = 0;
    private boolean            gameOver           = false;
    private Player             winner             = null;
    private int                totalTurns         = 0;

    private SnakeLadderGame(Builder builder) {
        this.board   = builder.board;
        this.dice    = builder.dice;
        this.config  = builder.config;
        this.players = Collections.unmodifiableList(builder.players);
        if (players.isEmpty())
            throw new GameException("At least one player required");
        if (players.size() > 6)
            throw new GameException("Maximum 6 players allowed");
    }

    // ── Core Turn Logic ───────────────────────────────────────────────────────

    /** Play one full turn for the current player. Returns the winner if game ends. */
    public Optional<Player> playTurn() {
        if (gameOver) throw new GameException("Game is already over");

        Player current = players.get(currentPlayerIndex);
        current.incrementTurns();
        totalTurns++;

        // Check skip
        if (current.shouldSkipTurn()) {
            System.out.printf("⏭  %s skips their turn (snake penalty)%n", current.getName());
            advanceTurn(false);
            return Optional.empty();
        }

        int rolled = dice.roll();
        System.out.printf("%n🎲 %s %s rolls: %d%n",
            current.getToken(), current.getName(), rolled);

        int newPosition = current.getPosition() + rolled;

        // Check exact finish rule
        if (config.exactFinishRequired() && newPosition > config.boardSize()) {
            System.out.printf("   Needs exact roll to finish. Stays at %d%n",
                current.getPosition());
            advanceTurn(false);
            return Optional.empty();
        }

        // Cap at board size
        newPosition = Math.min(newPosition, config.boardSize());
        current.moveTo(newPosition);
        System.out.printf("   Moved to: %d%n", newPosition);

        // Apply snake or ladder
        Optional<BoardEntity> entity = board.getEntityAt(newPosition);
        if (entity.isPresent()) {
            switch (entity.get()) {
                case BoardEntity.Snake s -> {
                    System.out.printf("   🐍 Snake at %d! Slides down to %d%n",
                        s.head(), s.tail());
                    current.moveTo(s.tail());
                    current.incrementSnakeBites();
                    if (config.skipTurnOnSnake()) current.setSkipNextTurn(true);
                }
                case BoardEntity.Ladder l -> {
                    System.out.printf("   🪜 Ladder at %d! Climbs up to %d%n",
                        l.bottom(), l.top());
                    current.moveTo(l.top());
                    current.incrementLaddersClimbed();
                }
                default -> {}
            }
        }

        System.out.printf("   Final position: %d%n", current.getPosition());

        // Win check
        if (current.getPosition() >= config.boardSize()) {
            gameOver = true;
            winner   = current;
            System.out.printf("%n🏆 %s %s WINS in %d turns! 🎉%n",
                current.getToken(), current.getName(), current.getTurnsPlayed());
            printStats();
            return Optional.of(current);
        }

        // Bonus turn on 6
        boolean bonusTurn = config.bonusTurnOnSix() && rolled == dice.getMaxValue();
        if (bonusTurn) {
            System.out.printf("   🎯 Rolled %d! Bonus turn!%n", rolled);
        }
        advanceTurn(bonusTurn);
        return Optional.empty();
    }

    /** Play until someone wins */
    public Player playToEnd() {
        while (!gameOver) {
            playTurn();
        }
        return winner;
    }

    /** Play N turns (useful for testing) */
    public void playTurns(int n) {
        for (int i = 0; i < n && !gameOver; i++) playTurn();
    }

    private void advanceTurn(boolean sameTurn) {
        if (!sameTurn) currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    // ── Display ───────────────────────────────────────────────────────────────

    public void displayPositions() {
        System.out.println("\n─── Current Positions ───");
        players.forEach(p -> System.out.printf("  %s %-12s → %d%n",
            p.getToken(), p.getName(), p.getPosition()));
    }

    private void printStats() {
        System.out.println("\n─── Game Statistics ───");
        System.out.println("Total turns played: " + totalTurns);
        players.forEach(p -> System.out.printf(
            "  %s %-12s | Turns: %d | Snakes: %d | Ladders: %d%n",
            p.getToken(), p.getName(),
            p.getTurnsPlayed(), p.getSnakeBites(), p.getLaddersClimbed()));
    }

    public boolean isGameOver() { return gameOver; }
    public Player  getWinner()  { return winner; }

    // ── Builder ───────────────────────────────────────────────────────────────

    static final class Builder {
        private Board        board;
        private DiceStrategy dice   = new FairDice();
        private GameConfig   config = GameConfig.defaultConfig();
        private final List<Player> players = new ArrayList<>();

        Builder board(Board board) {
            this.board = Objects.requireNonNull(board);
            return this;
        }

        Builder dice(DiceStrategy dice) {
            this.dice = Objects.requireNonNull(dice);
            return this;
        }

        Builder config(GameConfig config) {
            this.config = Objects.requireNonNull(config);
            return this;
        }

        Builder addPlayer(String id, String name, String token) {
            if (players.size() >= 6)
                throw new GameException("Maximum 6 players");
            players.add(new Player(id, name, token));
            return this;
        }

        SnakeLadderGame build() {
            if (board == null) throw new GameException("Board is required");
            return new SnakeLadderGame(this);
        }
    }
}

// ── Main Demo ─────────────────────────────────────────────────────────────────

public class SnakeLadderDemo {
    public static void main(String[] args) {
        // Build classic 10×10 board
        Board board = new Board.Builder(100)
            // Snakes: head → tail
            .addSnake(99, 5)
            .addSnake(70, 55)
            .addSnake(52, 42)
            .addSnake(36, 6)
            .addSnake(28, 8)
            // Ladders: bottom → top
            .addLadder(4, 56)
            .addLadder(12, 50)
            .addLadder(14, 55)
            .addLadder(22, 58)
            .addLadder(41, 79)
            .addLadder(54, 88)
            .addLadder(62, 96)
            .build();

        board.display();

        // ── Scenario 1: Normal game with 3 players ────────────────────────────
        System.out.println("=== Game 1: 3 Players, Fair Dice ===");
        SnakeLadderGame game1 = new SnakeLadderGame.Builder()
            .board(board)
            .dice(new FairDice())
            .config(GameConfig.defaultConfig())
            .addPlayer("P1", "Alice", "🔴")
            .addPlayer("P2", "Bob",   "🔵")
            .addPlayer("P3", "Carol", "🟢")
            .build();

        Player winner1 = game1.playToEnd();
        System.out.println("Winner: " + winner1.getName());

        // ── Scenario 2: Loaded dice for testing ───────────────────────────────
        System.out.println("\n=== Game 2: 2 Players, Exact Finish Rule ===");
        SnakeLadderGame game2 = new SnakeLadderGame.Builder()
            .board(board)
            .dice(new FairDice())
            .config(new GameConfig(true, true, true, 100))  // exact finish + skip on snake
            .addPlayer("P1", "Dave",  "🟡")
            .addPlayer("P2", "Erica", "🟣")
            .build();

        // Play 20 turns and show positions
        game2.playTurns(20);
        game2.displayPositions();

        // ── Scenario 3: Invalid board setup ───────────────────────────────────
        System.out.println("\n=== Validation Tests ===");
        try {
            new Board.Builder(100).addSnake(10, 20).build();  // snake head < tail
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + e.getMessage());
        }

        try {
            new Board.Builder(100)
                .addLadder(5, 50)
                .addSnake(5, 3)   // position 5 already a ladder
                .build();
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + e.getMessage());
        }

        try {
            new SnakeLadderGame.Builder()
                .board(board)
                .addPlayer("P1", "P1", "A")
                .addPlayer("P2", "P2", "B")
                .addPlayer("P3", "P3", "C")
                .addPlayer("P4", "P4", "D")
                .addPlayer("P5", "P5", "E")
                .addPlayer("P6", "P6", "F")
                .addPlayer("P7", "P7", "G")  // 7th player — exceeds limit
                .build();
        } catch (GameException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }
}
```

## Extension Q&A

**Q: How do you add special cells (like "Lose a Turn" or "Roll Again")?**
Extend the sealed `BoardEntity` interface with new permits: `record LoseTurn(int position)` and `record RollAgain(int position)`. In `playTurn()`, the switch expression already handles all BoardEntity subtypes — add the new cases. The sealed interface ensures the compiler warns if you add a new type but forget to handle it somewhere.

**Q: How do you save and resume a game?**
Serialize `GameState(players, currentPlayerIndex, totalTurns)` to JSON. Each `Player` serializes their `position, snakeBites, laddersClimbed, turnsPlayed`. On resume: deserialize and reconstruct the `SnakeLadderGame` with the restored state. The `Board` config (snakes/ladders) is deterministic — recreate from the same config, don't serialize the board.

**Q: How would you add an AI player?**
Add a `PlayerType` enum (HUMAN, AI). AI player's turn: instead of waiting for input, call `dice.roll()` automatically. For a smarter AI: before rolling, analyze the board — if current position + 6 lands on a snake head, "prefer" not to roll 6 (though this isn't possible with a fair die, it could influence strategy in a variant). Implement as a `TurnStrategy` injected per player.
