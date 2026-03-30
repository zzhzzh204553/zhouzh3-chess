package com.zhouzh3.chess.fen;

public class Board {
    private static final int ROWS = 10;
    private static final int COLS = 9;
    private final char[][] board = new char[ROWS][COLS];

    public void initBoard() {
        String fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR";
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
            if (empty > 0) sb.append(empty);
            if (i < ROWS - 1) sb.append('/');
        }
        return sb.toString();
    }
}
