package com.zhouzh3.chess.vision;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageFenServiceTest {
    private static final int BOARD_X = 10;
    private static final int BOARD_Y = 676;
    private static final int BOARD_WIDTH = 1159;
    private static final int BOARD_HEIGHT = 1286;

    private static final double BOARD_BASE_WIDTH = 570.0;
    private static final double BOARD_BASE_HEIGHT = 637.0;
    private static final double START_X = 45.0;
    private static final double START_Y = 47.0;
    private static final double STEP_X = 60.0;
    private static final double STEP_Y = 60.0;
    private static final int OFFSET_X = -14;
    private static final int OFFSET_Y = -22;
    private static final int EXTRA_GAP_X = 4;
    private static final int EXTRA_GAP_Y = 4;
    private static final int SAMPLE_SIZE = 122;
    private static final int MATCH_SIZE = 92;
    private static final double PIECE_MATCH_THRESHOLD = 0.60;
    private static final double PIECE_MARGIN_THRESHOLD = 0.035;
    private static final double OCCUPIED_DIFF_THRESHOLD = 0.10;

    private static final Map<String, Character> PIECE_CODE_MAP = Map.ofEntries(
            Map.entry("red-ju.png", 'R'),
            Map.entry("red-ma.png", 'N'),
            Map.entry("red-xiang.png", 'B'),
            Map.entry("red-shi.png", 'A'),
            Map.entry("red-shuai.png", 'K'),
            Map.entry("red-pao.png", 'C'),
            Map.entry("red-bing.png", 'P'),
            Map.entry("black-ju.png", 'r'),
            Map.entry("black-ma.png", 'n'),
            Map.entry("black-xiang.png", 'b'),
            Map.entry("black-shi.png", 'a'),
            Map.entry("black-jiang.png", 'k'),
            Map.entry("black-pao.png", 'c'),
            Map.entry("black-zu.png", 'p')
    );

    static {
        OpenCV.loadLocally();
    }

    private final Path imageDir;

    public ImageFenServiceTest(Path imageDir) {
        this.imageDir = imageDir;
    }

    public String recognizeFen(Path screenshotPath) {
        return recognizeFen(screenshotPath, null);
    }

    public String recognizeFen(Path screenshotPath, String sideToMove) {
        Mat screenshot = Imgcodecs.imread(screenshotPath.toString(), Imgcodecs.IMREAD_COLOR);
        if (screenshot.empty()) {
            throw new IllegalArgumentException("无法读取图片: " + screenshotPath);
        }

        char[][] board = createEmptyBoard();
        double scaleX = BOARD_WIDTH / BOARD_BASE_WIDTH;
        double scaleY = BOARD_HEIGHT / BOARD_BASE_HEIGHT;
        List<PieceTemplate> templates = loadTemplates(MATCH_SIZE);
        Mat boardBackground = loadBoardBackground();

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 9; col++) {
                int centerX = BOARD_X
                        + (int) Math.round((START_X + col * STEP_X) * scaleX)
                        + OFFSET_X
                        + col * EXTRA_GAP_X;
                int centerY = BOARD_Y
                        + (int) Math.round((START_Y + row * STEP_Y) * scaleY)
                        + OFFSET_Y
                        + row * EXTRA_GAP_Y;
                Rect patchRect = squareAround(centerX, centerY, MATCH_SIZE, screenshot.width(), screenshot.height());
                Mat patch = new Mat(screenshot, patchRect);
                Rect boardPatchRect = squareAround(centerX - BOARD_X, centerY - BOARD_Y, MATCH_SIZE,
                        boardBackground.width(), boardBackground.height());
                Mat emptyPatch = new Mat(boardBackground, boardPatchRect);
                double occupiedDiff = calculatePatchDifference(patch, emptyPatch);

                MatchResult match = matchBest(patch, emptyPatch, templates);
                System.out.printf("cell[%d,%d] diff=%.4f code=%s score=%.4f margin=%.4f%n",
                        row, col, occupiedDiff, match.code, match.score, match.margin);
                if (occupiedDiff >= OCCUPIED_DIFF_THRESHOLD
                        && match.score >= PIECE_MATCH_THRESHOLD) {
                    board[row][col] = match.code;
                }
            }
        }

        String boardFen = toFen(board);
        String normalizedSide = normalizeSide(sideToMove);
        return normalizedSide == null ? boardFen : boardFen + " " + normalizedSide;
    }

    private List<PieceTemplate> loadTemplates(int patchSize) {
        List<PieceTemplate> templates = new ArrayList<>();
        for (Map.Entry<String, Character> entry : PIECE_CODE_MAP.entrySet()) {
            Path path = imageDir.resolve(entry.getKey());
            Mat src = Imgcodecs.imread(path.toString(), Imgcodecs.IMREAD_UNCHANGED);
            if (src.empty()) {
                throw new IllegalArgumentException("无法读取棋子模板: " + path);
            }

            List<Mat> channels = new ArrayList<>();
            Core.split(src, channels);
            if (channels.size() < 4) {
                throw new IllegalArgumentException("棋子模板缺少 alpha 通道: " + path);
            }

            List<Mat> bgrChannels = channels.subList(0, 3);
            Mat bgr = new Mat();
            Core.merge(bgrChannels, bgr);

            Mat resizedImage = new Mat();
            Imgproc.resize(bgr, resizedImage, new Size(patchSize, patchSize));

            Mat alpha = channels.get(3);
            Mat resizedMask = new Mat();
            Imgproc.resize(alpha, resizedMask, new Size(patchSize, patchSize), 0, 0, Imgproc.INTER_NEAREST);
            Imgproc.threshold(resizedMask, resizedMask, 1, 255, Imgproc.THRESH_BINARY);

            templates.add(new PieceTemplate(entry.getValue(), resizedImage, resizedMask));
        }
        return templates;
    }

    private Mat loadBoardBackground() {
        Path boardPath = imageDir.resolve("chessboard.png");
        Mat board = Imgcodecs.imread(boardPath.toString(), Imgcodecs.IMREAD_COLOR);
        if (board.empty()) {
            throw new IllegalArgumentException("无法读取空棋盘图片: " + boardPath);
        }

        Mat resized = new Mat();
        Imgproc.resize(board, resized, new Size(BOARD_WIDTH, BOARD_HEIGHT));
        return resized;
    }

    private MatchResult matchBest(Mat patch, Mat emptyPatch, List<PieceTemplate> templates) {
        double bestScore = -1.0;
        double secondScore = -1.0;
        char bestCode = '.';

        for (PieceTemplate template : templates) {
            Mat expected = blendTemplateOnBoard(emptyPatch, template);
            double score = calculatePatchSimilarity(patch, expected);
            if (score > bestScore) {
                secondScore = bestScore;
                bestScore = score;
                bestCode = template.code;
            } else if (score > secondScore) {
                secondScore = score;
            }
        }

        return new MatchResult(bestCode, bestScore, bestScore - secondScore);
    }

    private Mat blendTemplateOnBoard(Mat emptyPatch, PieceTemplate template) {
        Mat boardFloat = new Mat();
        Mat pieceFloat = new Mat();
        emptyPatch.convertTo(boardFloat, CvType.CV_32FC3, 1.0 / 255.0);
        template.image.convertTo(pieceFloat, CvType.CV_32FC3, 1.0 / 255.0);

        Mat alphaFloat = new Mat();
        template.mask.convertTo(alphaFloat, CvType.CV_32FC1, 1.0 / 255.0);

        List<Mat> alphaChannels = List.of(alphaFloat, alphaFloat, alphaFloat);
        Mat alpha3 = new Mat();
        Core.merge(alphaChannels, alpha3);

        Mat inverseAlpha3 = Mat.ones(alpha3.size(), alpha3.type());
        Core.subtract(inverseAlpha3, alpha3, inverseAlpha3);

        Mat boardPart = new Mat();
        Mat piecePart = new Mat();
        Core.multiply(boardFloat, inverseAlpha3, boardPart);
        Core.multiply(pieceFloat, alpha3, piecePart);

        Mat blended = new Mat();
        Core.add(boardPart, piecePart, blended);

        Mat result = new Mat();
        blended.convertTo(result, CvType.CV_8UC3, 255.0);
        return result;
    }

    private double calculatePatchSimilarity(Mat patch, Mat expectedPatch) {
        Mat patchFloat = new Mat();
        Mat expectedFloat = new Mat();
        patch.convertTo(patchFloat, CvType.CV_32FC3);
        expectedPatch.convertTo(expectedFloat, CvType.CV_32FC3);

        Mat diff = new Mat();
        Core.absdiff(patchFloat, expectedFloat, diff);

        List<Mat> channels = new ArrayList<>();
        Core.split(diff, channels);

        double totalMean = 0.0;
        for (Mat channel : channels) {
            totalMean += Core.mean(channel).val[0];
        }

        double mean = totalMean / channels.size();
        return 1.0 - mean / 255.0;
    }

    private double calculatePatchDifference(Mat patch, Mat emptyPatch) {
        Mat patchFloat = new Mat();
        Mat emptyFloat = new Mat();
        patch.convertTo(patchFloat, CvType.CV_32FC3);
        emptyPatch.convertTo(emptyFloat, CvType.CV_32FC3);

        Mat diff = new Mat();
        Core.absdiff(patchFloat, emptyFloat, diff);

        List<Mat> channels = new ArrayList<>();
        Core.split(diff, channels);

        double totalMean = 0.0;
        for (Mat channel : channels) {
            totalMean += Core.mean(channel).val[0];
        }
        return (totalMean / channels.size()) / 255.0;
    }

    private Rect squareAround(int centerX, int centerY, int size, int maxWidth, int maxHeight) {
        int half = size / 2;
        int x = Math.max(0, centerX - half);
        int y = Math.max(0, centerY - half);
        int width = Math.min(size, maxWidth - x);
        int height = Math.min(size, maxHeight - y);
        return new Rect(x, y, width, height);
    }

    private char[][] createEmptyBoard() {
        char[][] board = new char[10][9];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 9; col++) {
                board[row][col] = '.';
            }
        }
        return board;
    }

    private String normalizeSide(String sideToMove) {
        if (sideToMove == null || sideToMove.isBlank()) {
            return null;
        }
        String side = sideToMove.trim().toLowerCase();
        if (!"w".equals(side) && !"b".equals(side)) {
            throw new IllegalArgumentException("sideToMove 只能是 w 或 b");
        }
        return side;
    }

    private String toFen(char[][] board) {
        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < 10; row++) {
            int empty = 0;
            for (int col = 0; col < 9; col++) {
                char cell = board[row][col];
                if (cell == '.') {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    fen.append(empty);
                    empty = 0;
                }
                fen.append(cell);
            }
            if (empty > 0) {
                fen.append(empty);
            }
            if (row < 9) {
                fen.append('/');
            }
        }
        return fen.toString();
    }

    private record PieceTemplate(char code, Mat image, Mat mask) {
    }

    private record MatchResult(char code, double score, double margin) {
    }

    public static void main(String[] args) {
        Path imageDir = Path.of("src", "main", "resources", "static", "images");
        Path screenshot = imageDir.resolve("222.jpg");
        String fen = new ImageFenServiceTest(imageDir).recognizeFen(screenshot);
        System.out.println(fen);

    }

}
