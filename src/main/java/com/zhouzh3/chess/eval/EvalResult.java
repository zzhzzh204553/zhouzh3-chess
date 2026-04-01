package com.zhouzh3.chess.eval;


import lombok.Value;

@Value
public final class EvalResult {

    /** 红方总评估分，表示从红方自身角度累计得到的分值。 */
    private final int redScore;

    /** 黑方总评估分，表示从黑方自身角度累计得到的分值。 */
    private final int blackScore;

    /** 红方视角最终分，等于 redScore - blackScore，正数表示红优，负数表示黑优。 */
    private final int redPerspectiveScore;

    /** 当前行棋方视角分，正数表示当前要走的一方更优。 */
    private final int sideToMoveScore;

    /** 当前行棋方，'w' 表示红方走，'b' 表示黑方走。 */
    private final char sideToMove;

    public EvalResult(int redScore, int blackScore, char sideToMove) {
        this.redScore = redScore;
        this.blackScore = blackScore;
        this.redPerspectiveScore = redScore - blackScore;
        this.sideToMove = sideToMove;
//        sideToMove=='w'，表示下一步红方走，现在已经走的应该是黑方
        this.sideToMoveScore = sideToMove == 'w'
                ? redPerspectiveScore
                : -redPerspectiveScore;
    }


    @Override
    public String toString() {
        return "红方得分=" + redScore +
                ", 黑方得分=" + blackScore +
                ", 红方视角得分=" + redPerspectiveScore +
                ", 当前视角得分=" + sideToMoveScore +
                ", 当前行棋方=" + sideToMove;
    }
}
