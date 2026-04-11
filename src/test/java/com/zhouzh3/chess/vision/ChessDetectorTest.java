package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;
import com.zhouzh3.chess.model.CropParam;
import com.zhouzh3.chess.util.ImageUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ChessDetectorTest {

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


    public static void main(String[] args) throws IOException {

        ChessDetector chessDetector = new ChessDetector();
        Board board = chessDetector.ocrBoard(INPUT_IMAGES);
        System.out.println(board.toFen());

//        String name = "chess/cell_6_6.png";
//        InputStream resourceAsStream = Objects.requireNonNull(ChessDetector.class.getClassLoader().getResourceAsStream(name));
//        BufferedImage bufferedImage = ImageIO.read(resourceAsStream);
//        ChessPiece cell = chooser.choose(bufferedImage, 6, 6);
//        System.out.println(cell);
    }


//    @Test
//    public void test() throws IOException {
//        // 读取原始图片
//        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\5.png");
//
//        CropParam cropParam = ImageUtil.cropPiecesNew();
//
//
//        ChessDetector chessDetector = new ChessDetector();
//        Board board = chessDetector.detectChessPieces(inputFile, cropParam);
//        System.out.println(board.toFen());
//    }
}