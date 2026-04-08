package com.zhouzh3.chess.vision;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Constants {

    public static final String ADVISOR = "A";

    public static final List<List<String>> INPUT_IMAGES = Arrays.asList(
            Arrays.asList("chess/cell_0_0.png", "chess/cell_0_1.png", "chess/cell_0_2.png", "chess/cell_0_3.png", "chess/cell_0_4.png", "chess/cell_0_5.png", "chess/cell_0_6.png", "chess/cell_0_7.png", "chess/cell_0_8.png"),
            Arrays.asList("chess/cell_1_0.png", "chess/cell_1_1.png", "chess/cell_1_2.png", "chess/cell_1_3.png", "chess/cell_1_4.png", "chess/cell_1_5.png", "chess/cell_1_6.png", "chess/cell_1_7.png", "chess/cell_1_8.png"),
            Arrays.asList("chess/cell_2_0.png", "chess/cell_2_1.png", "chess/cell_2_2.png", "chess/cell_2_3.png", "chess/cell_2_4.png", "chess/cell_2_5.png", "chess/cell_2_6.png", "chess/cell_2_7.png", "chess/cell_2_8.png"),
            Arrays.asList("chess/cell_3_0.png", "chess/cell_3_1.png", "chess/cell_3_2.png", "chess/cell_3_3.png", "chess/cell_3_4.png", "chess/cell_3_5.png", "chess/cell_3_6.png", "chess/cell_3_7.png", "chess/cell_3_8.png"),
            Arrays.asList("chess/cell_4_0.png", "chess/cell_4_1.png", "chess/cell_4_2.png", "chess/cell_4_3.png", "chess/cell_4_4.png", "chess/cell_4_5.png", "chess/cell_4_6.png", "chess/cell_4_7.png", "chess/cell_4_8.png"),
            Arrays.asList("chess/cell_5_0.png", "chess/cell_5_1.png", "chess/cell_5_2.png", "chess/cell_5_3.png", "chess/cell_5_4.png", "chess/cell_5_5.png", "chess/cell_5_6.png", "chess/cell_5_7.png", "chess/cell_5_8.png"),
            Arrays.asList("chess/cell_6_0.png", "chess/cell_6_1.png", "chess/cell_6_2.png", "chess/cell_6_3.png", "chess/cell_6_4.png", "chess/cell_6_5.png", "chess/cell_6_6.png", "chess/cell_6_7.png", "chess/cell_6_8.png"),
            Arrays.asList("chess/cell_7_0.png", "chess/cell_7_1.png", "chess/cell_7_2.png", "chess/cell_7_3.png", "chess/cell_7_4.png", "chess/cell_7_5.png", "chess/cell_7_6.png", "chess/cell_7_7.png", "chess/cell_7_8.png"),
            Arrays.asList("chess/cell_8_0.png", "chess/cell_8_1.png", "chess/cell_8_2.png", "chess/cell_8_3.png", "chess/cell_8_4.png", "chess/cell_8_5.png", "chess/cell_8_6.png", "chess/cell_8_7.png", "chess/cell_8_8.png"),
            Arrays.asList("chess/cell_9_0.png", "chess/cell_9_1.png", "chess/cell_9_2.png", "chess/cell_9_3.png", "chess/cell_9_4.png", "chess/cell_9_5.png", "chess/cell_9_6.png", "chess/cell_9_7.png", "chess/cell_9_8.png")
    );
    public static final Map<String, String> CANDIDATES = Map.ofEntries(
            Map.entry("R红车", "images/red-ju.png"),
            Map.entry("N红马", "images/red-ma.png"),
            Map.entry("B红相", "images/red-xiang.png"),
            Map.entry("A红仕", "images/red-shi.png"),
            Map.entry("K红帅", "images/red-shuai.png"),
            Map.entry("C红炮", "images/red-pao.png"),
            Map.entry("P红兵", "images/red-bing.png"),
            Map.entry("r黑车", "images/black-ju.png"),
            Map.entry("n黑马", "images/black-ma.png"),
            Map.entry("b黑象", "images/black-xiang.png"),
            Map.entry("a黑士", "images/black-shi.png"),
            Map.entry("k黑将", "images/black-jiang.png"),
            Map.entry("c黑炮", "images/black-pao.png"),
            Map.entry("p黑卒", "images/black-zu.png|images/black-zu2.png")
    );
    public static final int END_ROW = 9;
    public static final int END_COL = 8;

    public static final int IMAGE_SIZE = 114;
    public static final int IMAGE_CENTER = IMAGE_SIZE / 2;
    public static final int IMAGE_RADIUS = IMAGE_SIZE / 2;


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

    public static final int START_COL = 0;
    public static final int SIZE = 121;

    public static final int COMPARE_RADIUS = SIZE / 2 - RADIUS_OFFSET;
    public static final int CENTER = SIZE / 2 - Y_OFFSET;


}

