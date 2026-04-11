package com.zhouzh3.chess.fen;

import com.zhouzh3.chess.eval.ChessEvaluator;

public class FenMain {
    public static void main(String[] args) {
        ChessFen game = new ChessFen();
        game.initBoard();

        // 棋谱（红黑交替，共 39 回合，78 步）
        String[] moves = {
                "兵七进一", "卒7进1",
                "炮二平三", "炮8平5",
                "马八进七", "马8进7",
                "相七进五", "马7进6",
                "炮三进三", "马6进5",
                "马七进五", "炮5进4",
                "仕六进五", "车9平8",
                "马二进三", "炮5退2",
                "马三进五", "象3进5",
                "炮三平四", "马2进4",
                "车九平六", "马4进6",
                "车一进二", "车8进6",
                "炮八进四", "士4进5",
                "车一平三", "车1平4",
                "马五进三", "车4进9",
                "帅五平六", "炮2平4",
                "炮八平五", "车8退2",
                "车三平四", "车8平7",
                "车四进一", "炮5平4",
                "帅六平五", "马6进8",
                "炮四进一", "马8进7",
                "兵三进一", "车7进1",
                "车四平八", "车7平5",
                "车八进六", "炮4退2",
                "炮五平九", "车5进1",
                "兵九进一", "炮4平3",
                "炮四平五", "将5平4",
                "炮五平六", "将4平5",
                "炮九进三", "士5退4",
                "车八退三", "车5退3",
                "车八平七", "炮4平8",
                "兵七进一", "卒9进1",
                "炮六进二", "车5平3",
                "兵七进一", "炮3平2",
                "炮六平二", "炮8退2",
                "兵九进一", "士6进5",
                "兵九平八", "士5进4",
                "炮九退四", "炮8平9",
                "炮二平一"
        };


        ChessEvaluator evaluator = new ChessEvaluator();

        // 每两步为一个回合
        for (int i = 0; i < moves.length; i += 2) {
            // 红方走
            game.move(moves[i], true);
            String fenAfterRed = game.toFen();

            // 黑方走（防止最后一步只有红方）
            String fenAfterBlack = "";
            if (i + 1 < moves.length) {
                game.move(moves[i + 1], false);
                fenAfterBlack = game.toFen();
            }

            // 输出一个回合
            int round = (i / 2) + 1;
            System.out.println("Round " + round + ":");
            println("红", moves[i], fenAfterRed);
            System.out.println(evaluator.evaluate(fenAfterRed));

            if (!fenAfterBlack.isEmpty()) {
                println("黑", moves[i + 1], fenAfterBlack);
                System.out.println(evaluator.evaluate(fenAfterBlack));
            }
            System.out.println(); // 回合之间空一行
        }
    }

    private static void println(String redBlack, String move, String fenAfterMove) {
        System.out.println("  " + redBlack + " (" + move + "): " + fenAfterMove);
    }

}
