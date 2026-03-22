package lld.snakeladder;

public class SnakeLadderDemo {
    public static void main(String[] args) {
        Board board = new Board.Builder(100)
            .addSnake(99,5).addSnake(70,55).addSnake(52,42).addSnake(36,6).addSnake(28,8)
            .addLadder(4,56).addLadder(12,50).addLadder(14,55).addLadder(22,58)
            .addLadder(41,79).addLadder(54,88).addLadder(62,96)
            .build();

        System.out.println("=== Game 1: 3 Players ===");
        SnakeLadderGame game1 = new SnakeLadderGame.Builder()
            .board(board).dice(new FairDice())
            .addPlayer("P1","Alice","R").addPlayer("P2","Bob","B").addPlayer("P3","Carol","G")
            .build();
        game1.playToEnd();

        System.out.println("\n=== Game 2: 2 Players, Exact Finish ===");
        SnakeLadderGame game2 = new SnakeLadderGame.Builder()
            .board(board).dice(new FairDice())
            .config(new GameConfig(true, true, true, 100))
            .addPlayer("P1","Dave","Y").addPlayer("P2","Eve","P")
            .build();
        game2.playTurns(15);
        game2.displayPositions();

        System.out.println("\n=== Validation Tests ===");
        try { new Board.Builder(100).addSnake(10, 20).build(); }
        catch (IllegalArgumentException e) { System.out.println("Error: " + e.getMessage()); }

        try { new Board.Builder(100).addLadder(5,50).addSnake(5,3).build(); }
        catch (IllegalArgumentException e) { System.out.println("Error: " + e.getMessage()); }

        try {
            new SnakeLadderGame.Builder().board(board)
                .addPlayer("P1","P1","A").addPlayer("P2","P2","B").addPlayer("P3","P3","C")
                .addPlayer("P4","P4","D").addPlayer("P5","P5","E").addPlayer("P6","P6","F")
                .addPlayer("P7","P7","G").build();
        } catch (GameException e) { System.out.println("Error: " + e.getMessage()); }
    }
}
