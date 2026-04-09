package com.zhouzh3.chess.vision;

/**
 * @author haig
 */

public enum PieceSide {
    BLACK("黑色", 1),
    RED("红色", 2),
    EMPTY("无", 0);

    private final String label;
    private final int code;

    PieceSide(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public int getCode() {
        return code;
    }


    public static PieceSide fromScore(long redScore, long darkScore) {
//        darkScore == 303 || darkScore == 109 || darkScore == 202
        if (darkScore == 0 ) {
            return EMPTY;
        }
        return redScore >= darkScore ? RED : BLACK;
    }
}
