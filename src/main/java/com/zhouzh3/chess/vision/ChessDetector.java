package com.zhouzh3.chess.vision;

import cn.hutool.core.io.FileUtil;
import com.zhouzh3.chess.constants.ChessConstants;
import com.zhouzh3.chess.enums.ChessSide;
import com.zhouzh3.chess.fen.Board;
import com.zhouzh3.chess.model.ChessCell;
import com.zhouzh3.chess.model.RgbColor;
import com.zhouzh3.chess.model.CropParam;
import com.zhouzh3.chess.model.Region;
import com.zhouzh3.chess.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.zhouzh3.chess.util.ImageUtil.cropCircle;
import static com.zhouzh3.chess.util.ImageUtil.getSubimage;


/**
 * 最好的版本
 * @author haig
 */
@Slf4j
@Service
public class ChessDetector {
    private static final String TEMPLATE_PATH_SEPARATOR = "\\|";
    private static final double RED_DOMINANCE_RATIO = 2.00d;
    private static final long RED_DOMINANCE_DELTA = 80000L;
    private static final double EMPTY_FOREGROUND_COVERAGE_THRESHOLD = 0.10d;
    private static final double EMPTY_EDGE_DENSITY_THRESHOLD = 6.0d;
    private static final double EMPTY_CONTRAST_THRESHOLD = 18.0d;
    private static final double EMPTY_ACTIVE_COVERAGE_THRESHOLD = 0.18d;
    private static final long EMPTY_COLOR_SCORE_THRESHOLD = 900L;
    private static final double UNKNOWN_PIECE_SCORE_THRESHOLD = 0.72d;


    private final Map<String, TemplateFeature> pieceTemplates;
    private final boolean[][] circleMask;


    public ChessDetector() throws IOException {
        this.circleMask = createCircleMask();
        this.pieceTemplates = loadPieceTemplates();
    }

    private Map<String, TemplateFeature> loadPieceTemplates() throws IOException {
        Map<String, TemplateFeature> templates = new LinkedHashMap<>();
        ChessConstants.CANDIDATES.forEach((name, resourcePath) -> {
            try {
                loadAll(templates, name, resourcePath);
            } catch (IOException e) {
                log.error("加载模板失败: {}", resourcePath, e);
            }
        });
//        load(templates, "R红车", "images/red-ju.png");
//        load(templates, "N红马", "images/red-ma.png");
//        load(templates, "B红相", "images/red-xiang.png");
//        load(templates, "A红仕", "images/red-shi.png");
//        load(templates, "K红帅", "images/red-shuai.png");
//        load(templates, "C红炮", "images/red-pao.png");
//        load(templates, "P红兵", "images/red-bing.png");
//        load(templates, "r黑车", "images/black-ju.png");
//        load(templates, "n黑马", "images/black-ma.png");
//        load(templates, "b黑象", "images/black-xiang.png");
//        load(templates, "a黑士", "images/black-shi.png");
//        load(templates, "k黑将", "images/black-jiang.png");
//        load(templates, "c黑炮", "images/black-pao.png");
//        load(templates, "p黑卒", "images/black-zu.png");
        return Map.copyOf(templates);
    }

    private void loadAll(Map<String, TemplateFeature> templates, String name, String resourcePaths) throws IOException {
        String[] paths = resourcePaths.split(TEMPLATE_PATH_SEPARATOR);
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i].trim();
            if (path.isEmpty()) {
                continue;
            }
            String templateName = paths.length == 1 ? name : name + "#" + (i + 1);
            load(templates, templateName, path);
        }
    }

    private void load(Map<String, TemplateFeature> templates, String name, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("模板不存在: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(inputStream);
            templates.put(name, new TemplateFeature(getPieceSide(name), extractMetrics(image)));
        }
    }

    private boolean[][] createCircleMask() {
        boolean[][] mask = new boolean[ChessConstants.IMAGE_SIZE][ChessConstants.IMAGE_SIZE];
        for (int y = 0; y < ChessConstants.IMAGE_SIZE; y++) {
            for (int x = 0; x < ChessConstants.IMAGE_SIZE; x++) {
                int dx = x - ChessConstants.IMAGE_CENTER;
                int dy = y - ChessConstants.IMAGE_CENTER;
                mask[y][x] = dx * dx + dy * dy <= ChessConstants.IMAGE_RADIUS * ChessConstants.IMAGE_RADIUS;
            }
        }
        return mask;
    }

    public static void printMask(boolean[][] mask) {
        for (boolean[] row : mask) {
            for (boolean col : row) {
                System.out.print(col ? "● " : "○ ");
            }
            System.out.println();
        }
    }


    private String generateHash(BufferedImage img) {
        return extractMetrics(img).hash();
    }

    private ImageMetrics extractMetrics(BufferedImage img) {
        long sum = 0;
        long squareSum = 0;
        long redScore = 0;
        long darkScore = 0;
        int[][] gray = new int[ChessConstants.IMAGE_SIZE][ChessConstants.IMAGE_SIZE];
        int pixelCount = 0;
        int foregroundCount = 0;
        for (int y = 0; y < ChessConstants.IMAGE_SIZE; y++) {
            for (int x = 0; x < ChessConstants.IMAGE_SIZE; x++) {
                if (!circleMask[y][x]) {
                    continue;
                }
                RgbColor rgbColor = getColor(img, x, y);
                if (rgbColor == null) {
                    continue;
                }

                int v = rgbColor.getV();
                gray[y][x] = v;
                sum += v;
                squareSum += (long) v * v;
                pixelCount++;
                if (isForegroundPixel(rgbColor)) {
                    foregroundCount++;
                }
                if (rgbColor.isRedColor()) {
                    redScore += rgbColor.getRedDelta();
                } else if (rgbColor.isDarkColor()) {
                    darkScore += rgbColor.getDarkDelta();
                }
            }
        }
        if (pixelCount == 0) {
            throw new IllegalArgumentException("圆形区域内没有可比较的像素");
        }
        double avg = (double) sum / pixelCount;
        double variance = Math.max(0d, ((double) squareSum / pixelCount) - avg * avg);
        double contrast = Math.sqrt(variance);
        double activeDeltaThreshold = Math.max(12d, contrast * 0.65d);
        StringBuilder sb = new StringBuilder(ChessConstants.IMAGE_SIZE * ChessConstants.IMAGE_SIZE);
        double edgeSum = 0d;
        int edgeCount = 0;
        int activeCount = 0;
        for (int y = 0; y < ChessConstants.IMAGE_SIZE; y++) {
            for (int x = 0; x < ChessConstants.IMAGE_SIZE; x++) {
                if (!circleMask[y][x]) {
                    continue;
                }
                sb.append(gray[y][x] > avg ? '1' : '0');
                if (Math.abs(gray[y][x] - avg) >= activeDeltaThreshold) {
                    activeCount++;
                }
                if (x > 0 && y > 0 && circleMask[y][x - 1] && circleMask[y - 1][x]) {
                    edgeSum += Math.abs(gray[y][x] - gray[y][x - 1]);
                    edgeSum += Math.abs(gray[y][x] - gray[y - 1][x]);
                    edgeCount += 2;
                }
            }
        }
        double edgeDensity = edgeCount == 0 ? 0d : edgeSum / edgeCount;
        double foregroundCoverage = (double) foregroundCount / pixelCount;
        double activeCoverage = (double) activeCount / pixelCount;
        return new ImageMetrics(sb.toString(), avg, contrast, edgeDensity, foregroundCoverage, activeCoverage, gray, redScore, darkScore);
    }

    @Nullable
    private static RgbColor getColor(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 255;
        if (a == 0) {
            return null;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return new RgbColor(r, g, b);
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

    public ChessCell choose(BufferedImage cellImage, int row, int col) {
        if (pieceTemplates.isEmpty()) {
            throw new IllegalStateException("棋子模板尚未初始化");
        }
        ImageMetrics metrics = extractMetrics(cellImage);
        ChessSide chessSide = parsePieceSide(cellImage, metrics);
        log.info("chessSide={}, mean={}, contrast={}, edgeDensity={}, foregroundCoverage={}",
                chessSide, metrics.mean(), metrics.contrast(), metrics.edgeDensity(), metrics.foregroundCoverage());
        if (chessSide == ChessSide.EMPTY) {
            return new ChessCell(row, col, ".");
        }

        String best = null;
        double bestScore = Double.MAX_VALUE;

        for (Map.Entry<String, TemplateFeature> entry : pieceTemplates.entrySet()) {
            TemplateFeature template = entry.getValue();
            double score = score(metrics, template, chessSide);
            if (score < bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        log.info("best={}, score={}", best, bestScore);
        if (best == null || bestScore > UNKNOWN_PIECE_SCORE_THRESHOLD) {
            return new ChessCell(row, col, ".");
        }

        // 巡河和骑河，是不可能有红仕的
        if (row == 4 || row == 5) {
            if (best != null && best.contains(ChessConstants.ADVISOR)) {
                best = "";
            }
        }


        return new ChessCell(row, col, normalizeName(best));
    }

    private String normalizeName(String name) {
        int idx = name.indexOf('#');
        return idx >= 0 ? name.substring(0, idx) : name;
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


    private double score(ImageMetrics metrics, TemplateFeature template, ChessSide chessSide) {
        ImageMetrics templateMetrics = template.metrics();
        int hashDistance = hammingDistance(metrics.hash(), templateMetrics.hash());
        double normalizedHash = (double) hashDistance / metrics.hash().length();
        double structuralGap = structuralGap(metrics, templateMetrics);
        double contrastGap = Math.abs(metrics.contrast() - templateMetrics.contrast()) / 255.0d;
        double edgeGap = Math.abs(metrics.edgeDensity() - templateMetrics.edgeDensity()) / 255.0d;
        double coverageGap = Math.abs(metrics.foregroundCoverage() - templateMetrics.foregroundCoverage());
        double activeCoverageGap = Math.abs(metrics.activeCoverage() - templateMetrics.activeCoverage());
        double colorGap = colorGap(metrics, templateMetrics);
        double sidePenalty = chessSide == ChessSide.EMPTY || chessSide == template.side() ? 0d : 0.20d;
        return structuralGap * 0.70d
                + normalizedHash * 0.15d
                + colorGap * 0.12d
                + contrastGap * 0.05d
                + edgeGap * 0.03d
                + coverageGap * 0.01d
                + activeCoverageGap * 0.03d
                + sidePenalty;
    }

    private double structuralGap(ImageMetrics metrics, ImageMetrics templateMetrics) {
        double sourceContrast = Math.max(metrics.contrast(), 1d);
        double templateContrast = Math.max(templateMetrics.contrast(), 1d);
        double totalGap = 0d;
        int count = 0;
        for (int y = 0; y < ChessConstants.IMAGE_SIZE; y++) {
            for (int x = 0; x < ChessConstants.IMAGE_SIZE; x++) {
                if (!circleMask[y][x]) {
                    continue;
                }
                double source = (metrics.gray()[y][x] - metrics.mean()) / sourceContrast;
                double target = (templateMetrics.gray()[y][x] - templateMetrics.mean()) / templateContrast;
                totalGap += Math.abs(source - target);
                count++;
            }
        }
        return count == 0 ? Double.MAX_VALUE : totalGap / count;
    }

    private double colorGap(ImageMetrics metrics, ImageMetrics templateMetrics) {
        double sourceTotal = metrics.redScore() + metrics.darkScore() + 1d;
        double templateTotal = templateMetrics.redScore() + templateMetrics.darkScore() + 1d;
        double sourceRedRatio = metrics.redScore() / sourceTotal;
        double templateRedRatio = templateMetrics.redScore() / templateTotal;
        double sourceDarkRatio = metrics.darkScore() / sourceTotal;
        double templateDarkRatio = templateMetrics.darkScore() / templateTotal;
        return (Math.abs(sourceRedRatio - templateRedRatio) + Math.abs(sourceDarkRatio - templateDarkRatio)) / 2d;
    }

    private boolean isForegroundPixel(RgbColor rgbColor) {
        return rgbColor.isRedColor() || rgbColor.isDarkColor();
    }

    private ChessSide parsePieceSide(BufferedImage img, ImageMetrics metrics) {
        long redScore = metrics.redScore();
        long darkScore = metrics.darkScore();
        long colorScore = Math.max(redScore, darkScore);
        boolean looksEmpty = metrics.foregroundCoverage() < EMPTY_FOREGROUND_COVERAGE_THRESHOLD
                && metrics.edgeDensity() < EMPTY_EDGE_DENSITY_THRESHOLD
                && metrics.contrast() < EMPTY_CONTRAST_THRESHOLD;
        boolean lacksPieceStructure = metrics.activeCoverage() < EMPTY_ACTIVE_COVERAGE_THRESHOLD
                && metrics.edgeDensity() < EMPTY_EDGE_DENSITY_THRESHOLD * 1.15d
                && metrics.contrast() < EMPTY_CONTRAST_THRESHOLD * 1.2d;
        if (looksEmpty || lacksPieceStructure || colorScore < EMPTY_COLOR_SCORE_THRESHOLD) {
            log.info("empty cell detected: redScore={}, darkScore={}, mean={}, contrast={}, edgeDensity={}, foregroundCoverage={}, activeCoverage={}",
                    redScore, darkScore, metrics.mean(), metrics.contrast(), metrics.edgeDensity(),
                    metrics.foregroundCoverage(), metrics.activeCoverage());
            return ChessSide.EMPTY;
        }
        log.info("redScore={}, darkScore={}", redScore, darkScore);
        if (redScore > darkScore * RED_DOMINANCE_RATIO && redScore - darkScore > RED_DOMINANCE_DELTA) {
            return ChessSide.RED;
        }
        return ChessSide.BLACK;
    }

    private record TemplateFeature(ChessSide side, ImageMetrics metrics) {
    }

    private record ImageMetrics(String hash, double mean, double contrast, double edgeDensity,
                                double foregroundCoverage, double activeCoverage, int[][] gray,
                                long redScore, long darkScore) {
    }

    @Deprecated
    protected Board ocrBoard(List<List<String>> inputImages) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Board board = new Board();
        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            System.out.println("row " + row + ": ");
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
                String inputImage = inputImages.get(row).get(col);
                try (InputStream inputStream = classLoader.getResourceAsStream(inputImage)) {
                    BufferedImage bufferedImage = ImageIO.read(Objects.requireNonNull(inputStream));
                    ChessCell cell = this.choose(bufferedImage, row, col);
                    System.out.println("====" + cell);
                    board.setPiece(row, col, cell.chessName().charAt(0));
                }
            }
        }
        return board;
    }


    public Board detectChessPieces(File inputFile, CropParam cropParam) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = getSubimage(originalImage, cropParam.chessBoardRegion());

        Graphics2D graphics = boardImage.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.setStroke(new BasicStroke(1));

        // 保存截取后的图片
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()));
        Files.createDirectories(path);

        Board board = new Board();
        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
                Region region = cropParam.getChessPiece(row, col);
                BufferedImage cellImage = getSubimage(boardImage, region);
                BufferedImage cropCircle = cropCircle(cellImage);
                ChessCell cell = this.choose(cropCircle, row, col);
                board.setPiece(row, col, cell.chessName().charAt(0));

                ImageUtil.write(cropCircle, path.resolve("cell_" + row + "_" + col + ".png"));
                graphics.drawRect(region.x(), region.y(), region.width(), region.height());

            }
        }

        ImageUtil.write(boardImage, path.resolve("zz_board_" + inputFile.getName()));
        log.info("图片截取完成，保存为{}", path.toAbsolutePath().toString());

        return board;
    }




}
