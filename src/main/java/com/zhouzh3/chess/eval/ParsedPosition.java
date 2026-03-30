package com.zhouzh3.chess.eval;

public final class ParsedPosition {
    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final char[][] board;
    private final char sideToMove;

    public ParsedPosition(char[][] board, char sideToMove) {
        this.board = board;
        this.sideToMove = sideToMove;
    }

    public char[][] getBoard() {
        return board;
    }

    public char getSideToMove() {
        return sideToMove;
    }

    public char getPiece(int row, int col) {
        return board[row][col];
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }
}
