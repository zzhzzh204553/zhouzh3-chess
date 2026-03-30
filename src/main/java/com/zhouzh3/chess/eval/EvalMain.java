package com.zhouzh3.chess.eval;

public class EvalMain {
    public static void main(String[] args) {
        String fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

        XiangqiEvaluator evaluator = new XiangqiEvaluator();
        EvalResult result = evaluator.evaluate(fen);

        System.out.println(result);
        System.out.println("redScore = " + result.getRedScore());
        System.out.println("blackScore = " + result.getBlackScore());
        System.out.println("redPerspectiveScore = " + result.getRedPerspectiveScore());
        System.out.println("sideToMoveScore = " + result.getSideToMoveScore());
    }
}
