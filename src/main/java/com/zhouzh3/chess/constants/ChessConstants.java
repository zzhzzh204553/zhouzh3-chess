package com.zhouzh3.chess.constants;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author haig
 */
public class ChessConstants {
    public static final int BOARD_X = 10;
    public static final int BOARD_Y = 676;
    public static final int BOARD_WIDTH = 1159;
    public static final int BOARD_HEIGHT = 1286;
    public static final int OFFSET_X = 22;
    public static final int OFFSET_Y = 14;
    public static final int CHESS_WIDTH = 136 - OFFSET_X;
    public static final int CHESS_HEIGH = 134 - OFFSET_Y;
    public static final float GAP_X = 11;
    public static final float GAP_Y = 5;
    public static final int END_ROW = 9;
    public static final int END_COL = 8;


    public static final String ADVISOR = "A";
//    public static final int END_ROW = 9;
//    public static final int END_COL = 8;
    public static final int IMAGE_SIZE = 114;
    public static final int IMAGE_CENTER = IMAGE_SIZE / 2;
    public static final int IMAGE_RADIUS = IMAGE_SIZE / 2;
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
}
