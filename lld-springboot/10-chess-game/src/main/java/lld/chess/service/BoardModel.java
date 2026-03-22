package lld.chess.service;

import lld.chess.enums.PieceColor;
import lld.chess.enums.PieceType;

import java.util.*;

/**
 * In-memory 8x8 board model — parsed from and serialised to boardState string in DB.
 * Format: "COLOR_TYPE@col,row|COLOR_TYPE@col,row|..."
 */
public class BoardModel {

    public record Piece(PieceColor color, PieceType type) {
        @Override public String toString() { return color.name() + "_" + type.name(); }
        public static Piece parse(String s) {
            String[] p = s.split("_", 2);
            return new Piece(PieceColor.valueOf(p[0]), PieceType.valueOf(p[1]));
        }
    }

    private final Map<String, Piece> cells = new HashMap<>();

    public static BoardModel initial() {
        BoardModel b = new BoardModel();
        b.place(PieceColor.WHITE, PieceType.ROOK,   0,0); b.place(PieceColor.WHITE, PieceType.ROOK,   7,0);
        b.place(PieceColor.WHITE, PieceType.KNIGHT, 1,0); b.place(PieceColor.WHITE, PieceType.KNIGHT, 6,0);
        b.place(PieceColor.WHITE, PieceType.BISHOP, 2,0); b.place(PieceColor.WHITE, PieceType.BISHOP, 5,0);
        b.place(PieceColor.WHITE, PieceType.QUEEN,  3,0); b.place(PieceColor.WHITE, PieceType.KING,   4,0);
        for (int c=0;c<8;c++) b.place(PieceColor.WHITE, PieceType.PAWN, c, 1);
        b.place(PieceColor.BLACK, PieceType.ROOK,   0,7); b.place(PieceColor.BLACK, PieceType.ROOK,   7,7);
        b.place(PieceColor.BLACK, PieceType.KNIGHT, 1,7); b.place(PieceColor.BLACK, PieceType.KNIGHT, 6,7);
        b.place(PieceColor.BLACK, PieceType.BISHOP, 2,7); b.place(PieceColor.BLACK, PieceType.BISHOP, 5,7);
        b.place(PieceColor.BLACK, PieceType.QUEEN,  3,7); b.place(PieceColor.BLACK, PieceType.KING,   4,7);
        for (int c=0;c<8;c++) b.place(PieceColor.BLACK, PieceType.PAWN, c, 6);
        return b;
    }

    public static BoardModel parse(String state) {
        BoardModel b = new BoardModel();
        if (state == null || state.isBlank()) return b;
        for (String token : state.split("\\|")) {
            if (token.isBlank()) continue;
            String[] parts = token.split("@");
            String[] pos   = parts[1].split(",");
            b.cells.put(key(Integer.parseInt(pos[0]), Integer.parseInt(pos[1])), Piece.parse(parts[0]));
        }
        return b;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        cells.forEach((k, p) -> { String[] idx = k.split(","); sb.append(p).append("@").append(idx[0]).append(",").append(idx[1]).append("|"); });
        return sb.toString();
    }

    public Piece  get(int col, int row)               { return cells.get(key(col,row)); }
    public void   place(PieceColor color, PieceType type, int col, int row) { cells.put(key(col,row), new Piece(color, type)); }
    public Piece  remove(int col, int row)             { return cells.remove(key(col,row)); }
    public boolean empty(int col, int row)             { return !cells.containsKey(key(col,row)); }
    public boolean inBounds(int col, int row)          { return col>=0&&col<8&&row>=0&&row<8; }

    public boolean isValidMove(int fc, int fr, int tc, int tr, PieceColor turn) {
        if (!inBounds(fc,fr)||!inBounds(tc,tr)) return false;
        Piece piece = get(fc,fr);
        if (piece == null || piece.color() != turn) return false;
        if (!empty(tc,tr) && get(tc,tr).color() == turn) return false;  // can't capture own piece
        return switch (piece.type()) {
            case PAWN   -> validPawn(fc,fr,tc,tr,turn);
            case ROOK   -> validRook(fc,fr,tc,tr);
            case KNIGHT -> validKnight(fc,fr,tc,tr);
            case BISHOP -> validBishop(fc,fr,tc,tr);
            case QUEEN  -> validRook(fc,fr,tc,tr) || validBishop(fc,fr,tc,tr);
            case KING   -> Math.abs(tc-fc)<=1 && Math.abs(tr-fr)<=1;
        };
    }

    public Optional<Piece> findKing(PieceColor color) {
        return cells.entrySet().stream()
            .filter(e -> e.getValue().type()==PieceType.KING && e.getValue().color()==color)
            .map(e -> { String[] p=e.getKey().split(","); return cells.get(key(Integer.parseInt(p[0]),Integer.parseInt(p[1]))); })
            .findFirst();
    }

    public boolean isInCheck(PieceColor color) {
        // Find king position
        int[] kingPos = cells.entrySet().stream()
            .filter(e -> e.getValue().type()==PieceType.KING && e.getValue().color()==color)
            .map(e -> { String[] p=e.getKey().split(","); return new int[]{Integer.parseInt(p[0]),Integer.parseInt(p[1])}; })
            .findFirst().orElse(null);
        if (kingPos==null) return false;
        PieceColor opponent = color==PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
        return cells.entrySet().stream()
            .filter(e -> e.getValue().color()==opponent)
            .anyMatch(e -> { String[] p=e.getKey().split(","); return isValidMove(Integer.parseInt(p[0]),Integer.parseInt(p[1]),kingPos[0],kingPos[1],opponent); });
    }

    private boolean validPawn(int fc, int fr, int tc, int tr, PieceColor color) {
        int dir = color==PieceColor.WHITE ? 1 : -1;
        int startRow = color==PieceColor.WHITE ? 1 : 6;
        if (tc==fc && tr==fr+dir && empty(tc,tr)) return true;
        if (tc==fc && fr==startRow && tr==fr+2*dir && empty(tc,tr) && empty(tc,fr+dir)) return true;
        if (Math.abs(tc-fc)==1 && tr==fr+dir && !empty(tc,tr) && get(tc,tr).color()!=color) return true;
        return false;
    }
    private boolean validRook(int fc, int fr, int tc, int tr) {
        if (fc!=tc && fr!=tr) return false;
        int dc=Integer.signum(tc-fc), dr=Integer.signum(tr-fr);
        for (int c=fc+dc,r=fr+dr; c!=tc||r!=tr; c+=dc,r+=dr) if (!empty(c,r)) return false;
        return true;
    }
    private boolean validKnight(int fc,int fr,int tc,int tr) { int dc=Math.abs(tc-fc),dr=Math.abs(tr-fr); return (dc==2&&dr==1)||(dc==1&&dr==2); }
    private boolean validBishop(int fc,int fr,int tc,int tr) {
        if (Math.abs(tc-fc)!=Math.abs(tr-fr)) return false;
        int dc=Integer.signum(tc-fc), dr=Integer.signum(tr-fr);
        for (int c=fc+dc,r=fr+dr; c!=tc||r!=tr; c+=dc,r+=dr) if (!empty(c,r)) return false;
        return true;
    }
    private static String key(int col, int row) { return col+","+row; }
}
