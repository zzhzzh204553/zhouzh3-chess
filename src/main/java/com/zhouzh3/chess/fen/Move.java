package com.zhouzh3.chess.fen;

public class Move {
    String pieceStr;
    int col;
    String action;
    int target;

    public Move(String pieceStr, int col, String action, int target) {
        this.pieceStr = pieceStr;
        this.col = col;
        this.action = action;
        this.target = target;
    }
}
