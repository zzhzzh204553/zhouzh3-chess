package com.zhouzh3.chess.eval;

public final class FenParser {

    public ParsedPosition parse(String fen) {
        if (fen == null || fen.trim().isEmpty()) {
            throw new IllegalArgumentException("FEN must not be empty");
        }

        String[] parts = fen.trim().split("\\s+");
        String boardPart = parts[0];
        char sideToMove = parts.length > 1 ? parts[1].charAt(0) : 'w';

        String[] ranks = boardPart.split("/");
        if (ranks.length != ParsedPosition.ROWS) {
            throw new IllegalArgumentException("Invalid Xiangqi FEN: expected 10 ranks");
        }

        char[][] board = new char[ParsedPosition.ROWS][ParsedPosition.COLS];

        for (int row = 0; row < ParsedPosition.ROWS; row++) {
            String rank = ranks[row];
            int col = 0;

            for (int i = 0; i < rank.length(); i++) {
                char ch = rank.charAt(i);
                if (Character.isDigit(ch)) {
                    int emptyCount = ch - '0';
                    for (int j = 0; j < emptyCount; j++) {
                        if (col >= ParsedPosition.COLS) {
                            throw new IllegalArgumentException("Invalid Xiangqi FEN: rank too long");
                        }
                        board[row][col++] = '.';
                    }
                } else {
                    if (col >= ParsedPosition.COLS) {
                        throw new IllegalArgumentException("Invalid Xiangqi FEN: rank too long");
                    }
                    board[row][col++] = ch;
                }
            }

            if (col != ParsedPosition.COLS) {
                throw new IllegalArgumentException("Invalid Xiangqi FEN: rank length is not 9");
            }
        }

        return new ParsedPosition(board, sideToMove);
    }
}
