package lld.chess;

public class ChessDemo {
    public static void main(String[] args) {
        ChessGame game = new ChessGame("Alice (White)", "Bob (Black)");
        game.displayBoard();

        System.out.println("=== Scholar's Mate Attempt ===");
        game.makeMove("e2", "e4");
        game.makeMove("e7", "e5");
        game.makeMove("f1", "c4");
        game.makeMove("b8", "c6");
        game.makeMove("d1", "h5");
        game.makeMove("g8", "f6");  // Black defends

        game.displayBoard();

        System.out.println("=== Error Cases ===");
        try { game.makeMove("a1", "a1"); }
        catch (InvalidMoveException e) { System.out.println("Error: " + e.getMessage()); }

        try { game.makeMove("e5", "e4"); }  // Black pawn — white's turn
        catch (WrongTurnException e) { System.out.println("Error: " + e.getMessage()); }

        System.out.println("Status: " + game.getStatus());
        System.out.println("Turn:   " + game.getCurrentTurn());
        System.out.println("Moves:  " + game.getMoveCount());

        System.out.println("\n=== Resignation ===");
        game.resign(Color.BLACK);
        System.out.println("Final status: " + game.getStatus());
    }
}
