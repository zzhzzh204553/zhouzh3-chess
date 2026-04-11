package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.enums.ChessSide;
import com.zhouzh3.chess.fen.Board;
import com.zhouzh3.chess.model.ChessPiece;
import com.zhouzh3.chess.model.Region;
import com.zhouzh3.chess.model.RgbColor;
import com.zhouzh3.chess.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zhouzh3.chess.vision.Constants.END_COL;


@Slf4j
public class ImageFenService {

    private final Map<String, String> pieceHashes;
    private final boolean[][] circleMask = createCircleMask();

    public ImageFenService() throws IOException {
        this.pieceHashes = loadPieceHashes();
    }

    /**
     * 将输入的方形 BufferedImage 转换为圆形，周围透明。
     * 半径自动取最小边长的一半。
     *
     * @param cellImage 输入的方形图像
     * @return 输出的圆形透明图像
     */
    public static BufferedImage cropCircle(BufferedImage cellImage) {
        int width = cellImage.getWidth();
        int height = cellImage.getHeight();
//        半径
        int radius = Math.min(width, height) / 2 - Constants.RADIUS_OFFSET;

        BufferedImage circleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

//        计算圆心坐标
        int cx = width / 2;
        int cy = height / 2 - Constants.Y_OFFSET;
        Ellipse2D.Double circle = new Ellipse2D.Double(
                cx - radius,
                cy - radius,
                radius * 2,
                radius * 2
        );
        g2.setClip(circle);
        g2.drawImage(cellImage, 0, 0, null);
        g2.dispose();

        return circleImage;
    }

    public static ChessSide fromScore(long redScore, long darkScore) {
//        darkScore == 303 || darkScore == 109 || darkScore == 202
        if (darkScore == 0) {
            return ChessSide.EMPTY;
        }
        return redScore >= darkScore ? ChessSide.RED : ChessSide.BLACK;
    }

    private Map<String, String> loadPieceHashes() throws IOException {
        Map<String, String> hashes = new LinkedHashMap<>();
        load(hashes, "R红车", "images-old/red-ju.png");
        load(hashes, "N红马", "images-old/red-ma.png");
        load(hashes, "B红相", "images-old/red-xiang.png");
        load(hashes, "A红仕", "images-old/red-shi.png");
        load(hashes, "K红帅", "images-old/red-shuai.png");
        load(hashes, "C红炮", "images-old/red-pao.png");
        load(hashes, "P红兵", "images-old/red-bing.png");
        load(hashes, "r黑车", "images-old/black-ju.png");
        load(hashes, "n黑马", "images-old/black-ma.png");
        load(hashes, "b黑象", "images-old/black-xiang.png");
        load(hashes, "a黑士", "images-old/black-shi.png");
        load(hashes, "k黑将", "images-old/black-jiang.png");
        load(hashes, "c黑炮", "images-old/black-pao.png");
        load(hashes, "p黑卒", "images-old/black-zu.png");
        return Map.copyOf(hashes);
    }

    public ChessPiece identifyPiece(BufferedImage cellImage, int row, int col) {
        if (pieceHashes.isEmpty()) {
            throw new IllegalStateException("棋子模板尚未初始化");
        }
        BufferedImage normalized = resize(cellImage, Constants.SIZE, Constants.SIZE);
        ChessSide chessSide = parsePieceSide(normalized);
        log.info("chessSide={}", chessSide);
//        if (chessSide == ChessSide.EMPTY) {
//            return new ChessPiece(row, col, "");
//        }

        String hash = generateHash(normalized);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : pieceHashes.entrySet()) {
            if (
                /*chessSide != ChessSide.EMPTY &&*/
                    chessSide != getPieceSide(entry.getKey())) {
                continue;
            }
            int distance = hammingDistance(hash, entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }

        if (chessSide == ChessSide.EMPTY) {
//            if (best != null) {
//                String upperCase = best.substring(0, 1).toUpperCase();
//                if (Arrays.asList("B", "A", "K", "P").contains(upperCase)) {
//                    return new ChessPiece(row, col, best);
//                }
//            }
            return new ChessPiece(row, col, "");
        }


        // 巡河和骑河，是不可能有红仕的
        if (row == 4 || row == 5) {
            if (best != null && best.contains(Constants.ADVISOR)) {
                best = "";
            }
        }


        return new ChessPiece(row, col, best);
    }

    private void load(Map<String, String> hashes, String name, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("模板不存在: " + resourcePath);
            }
            hashes.put(name, generateHash(resize(ImageIO.read(inputStream), Constants.SIZE, Constants.SIZE)));
        }
    }

    private BufferedImage resize(BufferedImage img, int width, int heigh) {
        BufferedImage resized = new BufferedImage(width, heigh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, width, heigh, null);
        g.dispose();
        return resized;
    }

    private String generateHash(BufferedImage img) {
        long sum = 0;
        int[][] gray = new int[Constants.SIZE][Constants.SIZE];
        int pixelCount = 0;
        for (int y = 0; y < Constants.SIZE; y++) {
            for (int x = 0; x < Constants.SIZE; x++) {
                if (!circleMask[y][x]) {
                    continue;
                }
                RgbColor rgbColor = ImageUtil.getColor(img, x, y);
                if (rgbColor == null) {
                    continue;
                }

                int v = rgbColor.getV();
                gray[y][x] = v;
                sum += v;
                pixelCount++;
            }
        }
        if (pixelCount == 0) {
            throw new IllegalArgumentException("圆形区域内没有可比较的像素");
        }
        int avg = (int) (sum / pixelCount);
        StringBuilder sb = new StringBuilder(Constants.SIZE * Constants.SIZE);
        for (int y = 0; y < Constants.SIZE; y++) {
            for (int x = 0; x < Constants.SIZE; x++) {
                if (!circleMask[y][x]) {
                    continue;
                }
                sb.append(gray[y][x] > avg ? '1' : '0');
            }
        }
        return sb.toString();
    }

    private int hammingDistance(String a, String b) {
        int distance = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    private ChessSide parsePieceSide(BufferedImage img) {
        long redScore = 0;
        long darkScore = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (y >= Constants.SIZE || x >= Constants.SIZE || !circleMask[y][x]) {
                    continue;
                }
                RgbColor rgbColor = ImageUtil.getColor(img, x, y);
                if (rgbColor == null) {
                    continue;
                }
                if (rgbColor.isRedColor()) {
                    redScore += rgbColor.getRedDelta();
                } else if (rgbColor.isDarkColor()) {
                    darkScore += rgbColor.getDarkDelta();
                }
            }
        }
        log.info("redScore={}, darkScore={}", redScore, darkScore);
        return fromScore(redScore, darkScore);
    }

    private static boolean isDarkColor(int r, int g, int b) {
        return r + g + b < 360;
    }

    private static boolean isRedColor(int r, int g, int b) {
        return r > 120 && r > g + 12 && r > b + 12;
    }

    private ChessSide getPieceSide(String name) {
        if (name.isEmpty() || !Character.isAlphabetic(name.charAt(0))) {
            return ChessSide.EMPTY;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return ChessSide.RED;
        } else {
            return ChessSide.BLACK;
        }
    }

    private boolean[][] createCircleMask() {
        boolean[][] mask = new boolean[Constants.SIZE][Constants.SIZE];
        for (int y = 0; y < Constants.SIZE; y++) {
            for (int x = 0; x < Constants.SIZE; x++) {
                int dx = x - Constants.CENTER;
                int dy = y - Constants.CENTER;
                mask[y][x] = dx * dx + dy * dy <= Constants.COMPARE_RADIUS * Constants.COMPARE_RADIUS;
            }
        }
        return mask;
    }

    public static String getBaseName(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public String parseImageFen(Path screenshot) throws IOException {
        return parseImageFen(screenshot, false);
    }

    public String parseImageFen(Path screenshot, boolean debugOutput) throws IOException {
        BufferedImage source = ImageIO.read(screenshot.toFile());
        if (source == null) {
            throw new IOException("无法读取图片: " + screenshot);
        }
        Path outputDir = null;
        BufferedImage marked = null;
        Graphics2D graphics = null;
        if (debugOutput) {
            outputDir = Paths.get(System.getProperty("java.io.tmpdir"), "chess", getBaseName(screenshot));
            Files.createDirectories(outputDir);

            marked = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);

            graphics = marked.createGraphics();
            graphics.drawImage(source, 0, 0, null);
            graphics.setColor(java.awt.Color.RED);
            graphics.setStroke(new BasicStroke(1));
        }

        Board board = new Board();
        List<List<Region>> regions = listBoardRegions(source);
        for (int row = Constants.START_ROW; row <= Constants.END_ROW; row++) {
            for (int col = Constants.START_COL; col <= END_COL; col++) {
                StringBuilder sb = new StringBuilder();
                sb.append(MessageFormat.format("cell[{0}{1}]", row, col));

                Region region = regions.get(row - Constants.START_ROW).get(col - Constants.START_COL);
                BufferedImage cellImage = source.getSubimage(region.x(), region.y(), region.width(), region.height());
                BufferedImage cropCircle = cropCircle(cellImage);

                // 骑河和巡河，是不可能有仕的
                ChessPiece chessPiece = identifyPiece(cropCircle, row, col);

                board.setPiece(row, col, chessPiece.chessName().isEmpty() ? '.' : chessPiece.chessName().charAt(0));

                if (debugOutput) {
                    writeSampleImage(cropCircle, outputDir, chessPiece);
                    graphics.drawRect(region.x(), region.y(), region.width(), region.height());
                }


                sb.append(MessageFormat.format(" {0}", chessPiece));
                log.info(sb.toString());
                log.info("===========================\n");
            }
        }

        if (debugOutput) {
            graphics.dispose();
            Path markedPath = outputDir.resolve("zz_board_marked.png");
            ImageIO.write(marked, "png", markedPath.toFile());
            log.info("marked image: {}", markedPath);
        }
        return board.toFen();
    }

    private List<List<Region>> listBoardRegions(BufferedImage source) {
        double scaleX = Constants.BOARD_WIDTH / Constants.BOARD_BASE_WIDTH;
        double scaleY = Constants.BOARD_HEIGHT / Constants.BOARD_BASE_HEIGHT;
        int sampleSize = Math.max(40, (int) Math.round(Math.min(scaleX * Constants.STEP_X, scaleY * Constants.STEP_Y)));
        List<List<Region>> regions = new ArrayList<>();
        for (int row = Constants.START_ROW; row <= Constants.END_ROW; row++) {
            List<Region> rowRegions = new ArrayList<>();
            for (int col = Constants.START_COL; col <= END_COL; col++) {
                rowRegions.add(buildRegion(source, sampleSize, scaleX, scaleY, row, col));
            }
            regions.add(rowRegions);
        }
        return regions;
    }

    private Region buildRegion(BufferedImage source,
                               int sampleSize,
                               double scaleX, double scaleY,
                               int row, int col) {
        int centerX = Constants.BOARD_X
                + (int) Math.round((Constants.START_X + col * Constants.STEP_X) * scaleX)
                + Constants.OFFSET_X
                + col * Constants.EXTRA_GAP_X;
        int centerY = Constants.BOARD_Y
                + (int) Math.round((Constants.START_Y + row * Constants.STEP_Y) * scaleY)
                + Constants.OFFSET_Y
                + row * Constants.EXTRA_GAP_Y;
        int left = Math.max(0, centerX - sampleSize / 2);
        int top = Math.max(0, centerY - sampleSize / 2);
        int width = Math.min(sampleSize, source.getWidth() - left);
        int height = Math.min(sampleSize, source.getHeight() - top);
        if (col >= 4) {
            left -= 2;
        }
        return new Region(left, top, width, height);
    }

    private void writeSampleImage(BufferedImage cropCircle, Path outputDir, ChessPiece chessPiece) throws IOException {
        Path samplePath = outputDir.resolve(String.format("cell_%d_%d_%s.png", chessPiece.row(), chessPiece.col(), chessPiece.chessName()));
        ImageIO.write(cropCircle, "png", samplePath.toFile());
//        log.info("sample image: {}", samplePath);
    }
}
