package lld.chess;
public class BoardInitialiser {
    public static void setup(Board board) {
        // Black (row 0)
        board.setPiece(new Position(0,0), new Rook(Color.BLACK,   new Position(0,0)));
        board.setPiece(new Position(0,1), new Knight(Color.BLACK, new Position(0,1)));
        board.setPiece(new Position(0,2), new Bishop(Color.BLACK, new Position(0,2)));
        board.setPiece(new Position(0,3), new Queen(Color.BLACK,  new Position(0,3)));
        board.setPiece(new Position(0,4), new King(Color.BLACK,   new Position(0,4)));
        board.setPiece(new Position(0,5), new Bishop(Color.BLACK, new Position(0,5)));
        board.setPiece(new Position(0,6), new Knight(Color.BLACK, new Position(0,6)));
        board.setPiece(new Position(0,7), new Rook(Color.BLACK,   new Position(0,7)));
        for (int c = 0; c < 8; c++) board.setPiece(new Position(1,c), new Pawn(Color.BLACK, new Position(1,c)));
        // White (row 6-7)
        for (int c = 0; c < 8; c++) board.setPiece(new Position(6,c), new Pawn(Color.WHITE, new Position(6,c)));
        board.setPiece(new Position(7,0), new Rook(Color.WHITE,   new Position(7,0)));
        board.setPiece(new Position(7,1), new Knight(Color.WHITE, new Position(7,1)));
        board.setPiece(new Position(7,2), new Bishop(Color.WHITE, new Position(7,2)));
        board.setPiece(new Position(7,3), new Queen(Color.WHITE,  new Position(7,3)));
        board.setPiece(new Position(7,4), new King(Color.WHITE,   new Position(7,4)));
        board.setPiece(new Position(7,5), new Bishop(Color.WHITE, new Position(7,5)));
        board.setPiece(new Position(7,6), new Knight(Color.WHITE, new Position(7,6)));
        board.setPiece(new Position(7,7), new Rook(Color.WHITE,   new Position(7,7)));
    }
}
