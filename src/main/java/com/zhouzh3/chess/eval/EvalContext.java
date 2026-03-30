package com.zhouzh3.chess.eval;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EvalContext {
    private final ParsedPosition position;
    private final List<PieceState> redPieces = new ArrayList<>();
    private final List<PieceState> blackPieces = new ArrayList<>();
    private int[] redKing;
    private int[] blackKing;
    private int redAdvisors;
    private int blackAdvisors;
    private int redBishops;
    private int blackBishops;

    public EvalContext(ParsedPosition position) {
        this.position = position;
    }
}