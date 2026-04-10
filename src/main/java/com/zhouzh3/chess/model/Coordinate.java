package com.zhouzh3.chess.model;

/**
 * @author haig
 */
public record Coordinate(int x, int y) {
    public static Coordinate of(int x, int y) {
        return new Coordinate(x, y);
    }

}
