package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.lang.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ChessPieceRecognizer {

    // 生成平均哈希
    public static String getHash(Mat img) {
        Mat resized = new Mat();
        Imgproc.resize(img, resized, new Size(8, 8));
        Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2GRAY);

        double avg = Core.mean(resized).val[0];
        StringBuilder hash = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double val = resized.get(y, x)[0];
                hash.append(val > avg ? "1" : "0");
            }
        }
        return hash.toString();
    }

    // 计算汉明距离
    public static int hammingDistance(String h1, String h2) {
        int dist = 0;
        for (int i = 0; i < h1.length(); i++) {
            if (h1.charAt(i) != h2.charAt(i)) {
                dist++;
            }
        }
        return dist;
    }

    // 识别棋子
    public String recognize(Mat piece) {
        String hash = getHash(piece);
        String bestMatch = null;
        int minDist = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : library.entrySet()) {
            int dist = hammingDistance(hash, entry.getValue());
            if (dist < minDist) {
                minDist = dist;
                bestMatch = entry.getKey();
            }
        }
        return bestMatch;
    }


    private final Map<String, String> library;

    public ChessPieceRecognizer() {
        this.library = loadLibrary();
    }

    private String recognize(String name) {
        // 识别一个棋子
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(name)) {
            Mat testPiece = bufferedImageToMat(ImageIO.read(Objects.requireNonNull(resourceAsStream)));
            return recognize(testPiece);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static Map<String, String> loadLibrary() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // 构建棋子哈希库

        Map<String, String> library = new HashMap<>(16);
        ClassLoader classLoader = ChessPieceRecognizer.class.getClassLoader();
        Constants.CANDIDATES.forEach((key, path) -> {
            try (InputStream resourceAsStream = classLoader.getResourceAsStream(path)) {
                Mat piece = bufferedImageToMat(ImageIO.read(Objects.requireNonNull(resourceAsStream)));
                library.put(key, getHash(piece));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return Map.copyOf(library);
    }

    public static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat;
        switch (bi.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
                break;
            case BufferedImage.TYPE_3BYTE_BGR:
                mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                // 保留透明通道
                mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC4);
                break;
            default:
                // 转换成 TYPE_4BYTE_ABGR，保证透明度
                BufferedImage converted = new BufferedImage(
                        bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                converted.getGraphics().drawImage(bi, 0, 0, null);
                return bufferedImageToMat(converted);
        }
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }


    public static void main(String[] args) {
//        System.out.println(System.getProperty("java.library.path"));
        ChessPieceRecognizer chessPieceRecognizer = new ChessPieceRecognizer();

        Board board = new Board();
        for (int row = 0; row <= Constants.END_ROW; row++) {
            System.out.println("row " + row + ": ");
            for (int col = 0; col <= Constants.END_COL; col++) {
                String inputImage = Constants.INPUT_IMAGES.get(row).get(col);
                String recognize = chessPieceRecognizer.recognize(inputImage);
                board.setPiece(row, col, recognize.charAt(0));
            }
        }

        System.out.println(board.toFen());

    }
}
