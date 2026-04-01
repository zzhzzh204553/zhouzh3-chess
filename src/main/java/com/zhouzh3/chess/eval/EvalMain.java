package com.zhouzh3.chess.eval;

public class EvalMain {
    public static void main(String[] args) {
//        String fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

        if (args.length == 0) {
            args = new String[]{"rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w"};
        }
        XiangqiEvaluator evaluator = new XiangqiEvaluator();
        for (String arg : args) {
            System.out.println("==============="+arg);
            EvalResult result = evaluator.evaluate(arg);
            System.out.println(result);
        }


    }
}
