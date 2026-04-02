package com.zhouzh3.chess.eval;

/**
 * @author haig
 */
public record ParsedPosition(char[][] board, char sideToMove) {
    public static final int ROWS = 10;
    public static final int COLS = 9;

    public char getPiece(int row, int col) {
        return board[row][col];
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }
}
