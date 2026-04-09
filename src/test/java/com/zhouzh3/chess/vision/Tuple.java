package com.zhouzh3.chess.vision;

/**
 * @author haig
 */
public record Tuple(int x, int y) {



    public static Tuple of(int x, int y) {
        return new Tuple(x, y);
    }
}
