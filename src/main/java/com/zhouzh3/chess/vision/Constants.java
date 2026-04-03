package com.zhouzh3.chess.vision;


public final class Constants {
    public static final String ADVISOR = "A";

    private Constants() {
    } // 防止实例化

    public static final int Y_OFFSET = 8;

    public static final int RADIUS_OFFSET = 20;


    public static final int BOARD_X = 10;
    public static final int BOARD_Y = 676;

    public static final int BOARD_WIDTH = 1159;
    public static final int BOARD_HEIGHT = 1286;

    public static final double BOARD_BASE_WIDTH = 570.0;
    public static final double BOARD_BASE_HEIGHT = 637.0;

    public static final double START_X = 45.0;
    public static final double START_Y = 47.0;
    public static final double STEP_X = 60.0;
    public static final double STEP_Y = 60.0;

    public static final int OFFSET_X = -14;
    public static final int OFFSET_Y = -22;

    public static final int EXTRA_GAP_X = 4;
    public static final int EXTRA_GAP_Y = 4;

    public static final int START_ROW = 0;
    public static final int END_ROW = 9;
    public static final int START_COL = 0;
    public static final int END_COL = 8;
    public static final int SIZE = 121;

    public static final int COMPARE_RADIUS = SIZE / 2 - RADIUS_OFFSET;
    public static final int CENTER = SIZE / 2 - Y_OFFSET;


}

