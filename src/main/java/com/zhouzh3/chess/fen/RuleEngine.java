package com.zhouzh3.chess.fen;

public class RuleEngine {
    public static boolean inBounds(int row, int col) {
        return row >= 0 && row < 10 && col >= 0 && col < 9;
    }

    public static boolean inPalace(int row, int col, boolean isRed) {
        if (isRed) return row >= 7 && row <= 9 && col >= 3 && col <= 5;
        else return row >= 0 && row <= 2 && col >= 3 && col <= 5;
    }

    public static boolean isSameSidePiece(char piece, boolean isRed) {
        if (piece == '.') {
            return false;
        }
        return isRed ? Character.isUpperCase(piece) : Character.isLowerCase(piece);
    }

    public static int countPiecesBetween(char[][] board, int r1, int c1, int r2, int c2) {
        int count = 0;
        if (r1 == r2) {
            for (int j = Math.min(c1, c2) + 1; j < Math.max(c1, c2); j++) {
                if (board[r1][j] != '.') count++;
            }
        } else if (c1 == c2) {
            for (int i = Math.min(r1, r2) + 1; i < Math.max(r1, r2); i++) {
                if (board[i][c1] != '.') count++;
            }
        }
        return count;
    }
}
