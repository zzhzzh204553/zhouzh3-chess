package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;


@Slf4j
public class ImageFenService {

    private final Map<String, String> pieceHashes;
    private final boolean[][] circleMask = createCircleMask();

    public ImageFenService() throws IOException {
        this.pieceHashes = loadPieceHashes();
    }

    private Map<String, String> loadPieceHashes() throws IOException {
        Map<String, String> hashes = new LinkedHashMap<>();
        load(hashes, "R红车", "images/red-ju.png");
        load(hashes, "N红马", "images/red-ma.png");
        load(hashes, "B红相", "images/red-xiang.png");
        load(hashes, "A红仕", "images/red-shi.png");
        load(hashes, "K红帅", "images/red-shuai.png");
        load(hashes, "C红炮", "images/red-pao.png");
        load(hashes, "P红兵", "images/red-bing.png");
        load(hashes, "r黑车", "images/black-ju.png");
        load(hashes, "n黑马", "images/black-ma.png");
        load(hashes, "b黑象", "images/black-xiang.png");
        load(hashes, "a黑士", "images/black-shi.png");
        load(hashes, "k黑将", "images/black-jiang.png");
        load(hashes, "c黑炮", "images/black-pao.png");
        load(hashes, "p黑卒", "images/black-zu.png");
        return Map.copyOf(hashes);
    }

    public ChessCell identifyPiece(BufferedImage cellImage, int row, int col) {
        if (pieceHashes.isEmpty()) {
            throw new IllegalStateException("棋子模板尚未初始化");
        }
        BufferedImage normalized = resize(cellImage, Constants.SIZE, Constants.SIZE);
        PieceSide pieceSide = parsePieceSide(normalized);
        log.info("pieceSide={}", pieceSide);
//        if (pieceSide == PieceSide.EMPTY) {
//            return new ChessCell(row, col, "");
//        }

        String hash = generateHash(normalized);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : pieceHashes.entrySet()) {
            if (
                /*pieceSide != PieceSide.EMPTY &&*/
                    pieceSide != getPieceSide(entry.getKey())) {
                continue;
            }
            int distance = hammingDistance(hash, entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }

        if (pieceSide == PieceSide.EMPTY) {
//            if (best != null) {
//                String upperCase = best.substring(0, 1).toUpperCase();
//                if (Arrays.asList("B", "A", "K", "P").contains(upperCase)) {
//                    return new ChessCell(row, col, best);
//                }
//            }
            return new ChessCell(row, col, "");
        }


        // 巡河和骑河，是不可能有红仕的
        if (row == 4 || row == 5) {
            if (best != null && best.contains("A")) {
                best = "";
            }
        }


        return new ChessCell(row, col, best);
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
                Color color = getColor(img, x, y);
                if (color == null) {
                    continue;
                }

                int v = color.getV();
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

    private PieceSide parsePieceSide(BufferedImage img) {
        long redScore = 0;
        long darkScore = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (y >= Constants.SIZE || x >= Constants.SIZE || !circleMask[y][x]) {
                    continue;
                }
                Color color = getColor(img, x, y);
                if (color == null) {
                    continue;
                }
                if (color.isRedColor()) {
                    redScore += color.getRedDelta();
                } else if (color.isDarkColor()) {
                    darkScore += color.getDarkDelta();
                }
            }
        }
        log.info("redScore={}, darkScore={}", redScore, darkScore);
        return PieceSide.fromScore(redScore, darkScore);
    }

    @Nullable
    private static Color getColor(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 255;
        if (a == 0) {
            return null;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        Color color = new Color(r, g, b);
        return color;
    }

    private static boolean isDarkColor(int r, int g, int b) {
        return r + g + b < 360;
    }

    private static boolean isRedColor(int r, int g, int b) {
        return r > 120 && r > g + 12 && r > b + 12;
    }

    private PieceSide getPieceSide(String name) {
        if (name.isEmpty() || !Character.isAlphabetic(name.charAt(0))) {
            return PieceSide.EMPTY;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return PieceSide.RED;
        } else {
            return PieceSide.BLACK;
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
            for (int col = Constants.START_COL; col <= Constants.END_COL; col++) {
                StringBuilder sb = new StringBuilder();
                sb.append(MessageFormat.format("cell[{0}{1}]", row, col));

                Region region = regions.get(row - Constants.START_ROW).get(col - Constants.START_COL);
                BufferedImage cellImage = source.getSubimage(region.x(), region.y(), region.width(), region.height());
                BufferedImage cropCircle = ImageUtil.cropCircle(cellImage);

                // 骑河和巡河，是不可能有仕的
                ChessCell chessCell = identifyPiece(cropCircle, row, col);

                board.setPiece(row, col, chessCell.chessName().isEmpty() ? '.' : chessCell.chessName().charAt(0));

                if (debugOutput) {
                    writeSampleImage(cropCircle, outputDir, chessCell);
                    graphics.drawRect(region.x(), region.y(), region.width(), region.height());
                }


                sb.append(MessageFormat.format(" {0}", chessCell));
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
            for (int col = Constants.START_COL; col <= Constants.END_COL; col++) {
                rowRegions.add(buildRegion(source, sampleSize, scaleX, scaleY, row, col));
            }
            regions.add(rowRegions);
        }
        return regions;
    }

    private Region buildRegion(BufferedImage source, int sampleSize, double scaleX, double scaleY, int row, int col) {
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

    private void writeSampleImage(BufferedImage cropCircle, Path outputDir, ChessCell chessCell) throws IOException {
        Path samplePath = outputDir.resolve(String.format("cell_%d_%d_%s.png", chessCell.row(), chessCell.col(), chessCell.chessName()));
        ImageIO.write(cropCircle, "png", samplePath.toFile());
//        log.info("sample image: {}", samplePath);
    }
}
