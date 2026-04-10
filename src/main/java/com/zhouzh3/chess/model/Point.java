package com.zhouzh3.chess.model;

/**
 * @author haig
 */
public record Point(int x, int y) {
    public static Point of(int x, int y) {
        return new Point(x, y);
    }

}
