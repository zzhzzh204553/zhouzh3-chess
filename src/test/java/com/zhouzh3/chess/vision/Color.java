package com.zhouzh3.chess.vision;

/**
 * @author haig
 */
public record Color(int r, int g, int b) {

    public int getV() {
        return (r + g + b) / 3;
    }

    public boolean isRedColor() {
        return r > 120 && r > g + 12 && r > b + 12;
    }

    public boolean isDarkColor() {
        return r + g + b < 360;
    }


    public int getRedDelta() {
        return r() - Math.max(g(), b());
    }


    public int getDarkDelta() {
        return 360 - (r() + g() + b());
    }


}
