package com.zhouzh3.chess.vision;


import lombok.Getter;

@Getter
public class Cell {
    private final int row;
    private final int col;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }


}
