package com.zhouzh3.chess.model;

/**
 * @author haig
 */
public record RgbColor(int r, int g, int b) {

    public static int MAX_COLOR_VALUE = 255;

    public RgbColor {
        if (r < 0 || r > MAX_COLOR_VALUE || g < 0 || g > MAX_COLOR_VALUE || b < 0 || b > MAX_COLOR_VALUE) {
            throw new IllegalArgumentException("RGB 值必须在 0-255 之间");
        }
    }
    public String toHex() {
        return String.format("#%02X%02X%02X", r, g, b);
    }

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
