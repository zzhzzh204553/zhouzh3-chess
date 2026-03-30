package com.zhouzh3.chess.eval;


import lombok.Value;

@Value
public class PieceState {
    private final char piece;
    private final int row;
    private final int col;

    public PieceState(char piece, int row, int col) {
        this.piece = piece;
        this.row = row;
        this.col = col;
    }
}