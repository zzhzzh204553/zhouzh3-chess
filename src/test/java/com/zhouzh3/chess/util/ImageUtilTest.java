package com.zhouzh3.chess.util;

import com.zhouzh3.chess.fen.Board;
import com.zhouzh3.chess.model.CropParam;
import com.zhouzh3.chess.vision.PieceDetector;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class ImageUtilTest {

    @Test
    public void testGetCrossingColor() throws IOException {
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\10.png");
        CropParam cropParam = ImageUtil.cropPiecesNew();

        ImageUtil.getCrossingColor(inputFile, cropParam);

//        PieceDetector chessDetector = new PieceDetector();
//        chessDetector.detectChessPieces(inputFile, cropParam);
    }

    @Test
    public void testDetectChessPieces() throws IOException {
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\9.png");
        CropParam cropParam = ImageUtil.cropPiecesNew();

        PieceDetector pieceDetector = new PieceDetector();
        Board board = pieceDetector.detectChessPieces(inputFile, cropParam);
        System.out.println(board.toFen());
    }

}