package lld.chess;
public enum Color {
    WHITE, BLACK;
    public Color opponent() { return this == WHITE ? BLACK : WHITE; }
}
