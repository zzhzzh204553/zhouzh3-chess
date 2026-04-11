package com.zhouzh3.chess.constants;

import java.util.Map;

/**
 * @author haig
 */
public class ChessConstants {
    public static final int SIDE_LENGTH = 125;
    public static final int ORIGIN_X = 78;
    public static final int ORIGIN_Y = 77;
//    public static final int CELL_RADIUS = 55;

    public static final int BOARD_X = 10;
    public static final int BOARD_Y = 676;
    public static final int BOARD_WIDTH = 1159;
    public static final int BOARD_HEIGHT = 1286;

    public static final int OFFSET_X = 20;
    public static final int OFFSET_Y = 14;

    public static final int CHESS_WIDTH = 135 - OFFSET_X;
    public static final int CHESS_HEIGH = 134 - OFFSET_Y;


    public static final float GAP_X = 10;
    public static final float GAP_Y = 5;

    public static final int END_ROW = 9;
    public static final int END_COL = 8;
    public static final int CROSSING_COUNT = (END_ROW + 1) * (END_COL + 1);


    public static final String ADVISOR = "A";

    public static final int IMAGE_SIZE = 114;
    public static final int IMAGE_CENTER = IMAGE_SIZE / 2;
    public static final int IMAGE_RADIUS = IMAGE_SIZE / 2;
    public static final Map<String, String> CANDIDATES = Map.ofEntries(
//            Map.entry("R红车", "images/red-ju.png"),
//            Map.entry("N红马", "images/red-ma.png"),
//            Map.entry("B红相", "images/red-xiang.png"),
//            Map.entry("A红仕", "images/red-shi.png"),
//            Map.entry("K红帅", "images/red-shuai.png"),
//            Map.entry("C红炮", "images/red-pao.png"),
//            Map.entry("P红兵", "images/red-bing.png"),
//            Map.entry("r黑车", "images/black-ju.png|images/black-ju2.png"),
//            Map.entry("n黑马", "images/black-ma.png|images/black-ma2.png"),
//            Map.entry("b黑象", "images/black-xiang.png|images/black-xiang2.png"),
//            Map.entry("a黑士", "images/black-shi.png"),
//            Map.entry("k黑将", "images/black-jiang.png"),
//            Map.entry("c黑炮", "images/black-pao.png|images/black-pao2.png"),
//            Map.entry("p黑卒", "images/black-zu.png|images/black-zu2.png|images/black-zu3.png")

            // 红方
            Map.entry("R红车", "红车.png"),
            Map.entry("N红马", "红马.png|红马2.png"),
            Map.entry("B红相", "红相.png|红相2.png"),
            Map.entry("A红仕", "红仕.png|红仕2.png"),
            Map.entry("K红帅", "红帥.png"),
            Map.entry("C红炮", "红炮.png|红炮2.png"),
            Map.entry("P红兵", "红兵.png|红兵2.png|红兵3.png|红兵4.png"),

            // 黑方
            Map.entry("r黑车", "黑车.png"),
            Map.entry("n黑马", "黑马.png|黑马2.png"),
            Map.entry("b黑象", "黑象.png|黑象2.png"),
            Map.entry("a黑士", "黑士.png|黑士2.png"),
            Map.entry("k黑将", "黑将.png"),
            Map.entry("c黑炮", "黑炮.png|黑炮2.png"),
            Map.entry("p黑卒", "黑卒.png|黑卒2.png|黑卒3.png|黑卒4.png")
    );
    public static final char RED_2_MOVE = 'w';
    public static final char BLACK_2_MOVE = 'b';
}
