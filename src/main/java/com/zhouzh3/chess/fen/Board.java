package com.zhouzh3.chess.fen;

import com.zhouzh3.chess.constants.ChessConstants;
import lombok.Getter;
import lombok.Setter;

public class Board {
    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final char[][] board = new char[ROWS][COLS];

    /**
     * 'w' 表示红方，'b' 表示黑方
     */
    @Getter
    @Setter
    private char sideToMove = ChessConstants.RED_2_MOVE;

    public void initBoard() {
        String fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";
        loadFen(fen);
    }

    private void loadFen(String fen) {
        String[] rows = fen.split("/");
        for (int i = 0; i < ROWS; i++) {
            int col = 0;
            for (char c : rows[i].toCharArray()) {
                if (Character.isDigit(c)) {
                    int empty = c - '0';
                    for (int k = 0; k < empty; k++) {
                        board[i][col++] = '.';
                    }
                } else {
                    board[i][col++] = c;
                }
            }
        }
    }

    public char[][] getBoard() {
        return board;
    }

    public void setPiece(int row, int col, char piece) {
        board[row][col] = piece;
    }

    public char getPiece(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            throw new IllegalArgumentException("Invalid position: (" + row + ", " + col + ")");
        }


        return board[row][col];
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROWS; i++) {
            int empty = 0;
            for (int j = 0; j < COLS; j++) {
                char c = board[i][j];
                if (c == '.') {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(c);
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (i < ROWS - 1) {
                sb.append('/');
            }
        }
        sb.append(' ').append(sideToMove);
        return sb.toString();
    }
}
