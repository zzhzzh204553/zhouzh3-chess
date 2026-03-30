package com.zhouzh3.chess.fen;

public class MoveParser {
    public static int chineseNumberToInt(String s) {
        return switch (s) {
            case "一", "1" -> 1;
            case "二", "2" -> 2;
            case "三", "3" -> 3;
            case "四", "4" -> 4;
            case "五", "5" -> 5;
            case "六", "6" -> 6;
            case "七", "7" -> 7;
            case "八", "8" -> 8;
            case "九", "9" -> 9;
            default -> throw new IllegalArgumentException("未知数字: " + s);
        };
    }

    public static Move parse(String notation) {
        String pieceStr = notation.substring(0, 1);
        String colStr = notation.substring(1, 2);
        String action = notation.substring(2, 3);
        String targetStr = notation.substring(3);

        int col = chineseNumberToInt(colStr);
        int target = chineseNumberToInt(targetStr);

        return new Move(pieceStr, col, action, target);
    }

}


