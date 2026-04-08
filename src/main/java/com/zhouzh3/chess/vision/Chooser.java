package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.zhouzh3.chess.vision.Constants.CANDIDATES;

/**
 * 最好的版本
 * @author haig
 */
@Slf4j
public class Chooser {
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


    public Chooser() throws IOException {
        this.circleMask = createCircleMask();
        this.pieceTemplates = loadPieceTemplates();
    }

    private Map<String, TemplateFeature> loadPieceTemplates() throws IOException {
        Map<String, TemplateFeature> templates = new LinkedHashMap<>();
        CANDIDATES.forEach((name, resourcePath) -> {
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
        boolean[][] mask = new boolean[Constants.IMAGE_SIZE][Constants.IMAGE_SIZE];
        for (int y = 0; y < Constants.IMAGE_SIZE; y++) {
            for (int x = 0; x < Constants.IMAGE_SIZE; x++) {
                int dx = x - Constants.IMAGE_CENTER;
                int dy = y - Constants.IMAGE_CENTER;
                mask[y][x] = dx * dx + dy * dy <= Constants.IMAGE_RADIUS * Constants.IMAGE_RADIUS;
            }
        }
        return mask;
    }

    public static void printMask(boolean[][] mask) {
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                System.out.print(mask[y][x] ? "● " : "○ ");
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
        int[][] gray = new int[Constants.IMAGE_SIZE][Constants.IMAGE_SIZE];
        int pixelCount = 0;
        int foregroundCount = 0;
        for (int y = 0; y < Constants.IMAGE_SIZE; y++) {
            for (int x = 0; x < Constants.IMAGE_SIZE; x++) {
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
                squareSum += (long) v * v;
                pixelCount++;
                if (isForegroundPixel(color)) {
                    foregroundCount++;
                }
                if (color.isRedColor()) {
                    redScore += color.getRedDelta();
                } else if (color.isDarkColor()) {
                    darkScore += color.getDarkDelta();
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
        StringBuilder sb = new StringBuilder(Constants.IMAGE_SIZE * Constants.IMAGE_SIZE);
        double edgeSum = 0d;
        int edgeCount = 0;
        int activeCount = 0;
        for (int y = 0; y < Constants.IMAGE_SIZE; y++) {
            for (int x = 0; x < Constants.IMAGE_SIZE; x++) {
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
    private static Color getColor(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 255;
        if (a == 0) {
            return null;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return new Color(r, g, b);
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

    public ChessCell choose(BufferedImage cellImage, int row, int col) {
        if (pieceTemplates.isEmpty()) {
            throw new IllegalStateException("棋子模板尚未初始化");
        }
        BufferedImage normalized = cellImage;
        ImageMetrics metrics = extractMetrics(normalized);
        PieceSide pieceSide = parsePieceSide(normalized, metrics);
        log.info("pieceSide={}, mean={}, contrast={}, edgeDensity={}, foregroundCoverage={}",
                pieceSide, metrics.mean(), metrics.contrast(), metrics.edgeDensity(), metrics.foregroundCoverage());
        if (pieceSide == PieceSide.EMPTY) {
            return new ChessCell(row, col, ".");
        }

        String best = null;
        double bestScore = Double.MAX_VALUE;

        for (Map.Entry<String, TemplateFeature> entry : pieceTemplates.entrySet()) {
            TemplateFeature template = entry.getValue();
            double score = score(metrics, template, pieceSide);
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
            if (best != null && best.contains(Constants.ADVISOR)) {
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


    private double score(ImageMetrics metrics, TemplateFeature template, PieceSide pieceSide) {
        ImageMetrics templateMetrics = template.metrics();
        int hashDistance = hammingDistance(metrics.hash(), templateMetrics.hash());
        double normalizedHash = (double) hashDistance / metrics.hash().length();
        double structuralGap = structuralGap(metrics, templateMetrics);
        double contrastGap = Math.abs(metrics.contrast() - templateMetrics.contrast()) / 255.0d;
        double edgeGap = Math.abs(metrics.edgeDensity() - templateMetrics.edgeDensity()) / 255.0d;
        double coverageGap = Math.abs(metrics.foregroundCoverage() - templateMetrics.foregroundCoverage());
        double activeCoverageGap = Math.abs(metrics.activeCoverage() - templateMetrics.activeCoverage());
        double colorGap = colorGap(metrics, templateMetrics);
        double sidePenalty = pieceSide == PieceSide.EMPTY || pieceSide == template.side() ? 0d : 0.20d;
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
        for (int y = 0; y < Constants.IMAGE_SIZE; y++) {
            for (int x = 0; x < Constants.IMAGE_SIZE; x++) {
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

    private boolean isForegroundPixel(Color color) {
        return color.isRedColor() || color.isDarkColor();
    }

    private PieceSide parsePieceSide(BufferedImage img, ImageMetrics metrics) {
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
            return PieceSide.EMPTY;
        }
        log.info("redScore={}, darkScore={}", redScore, darkScore);
        if (redScore > darkScore * RED_DOMINANCE_RATIO && redScore - darkScore > RED_DOMINANCE_DELTA) {
            return PieceSide.RED;
        }
        return PieceSide.BLACK;
    }

    private record TemplateFeature(PieceSide side, ImageMetrics metrics) {
    }

    private record ImageMetrics(String hash, double mean, double contrast, double edgeDensity,
                                double foregroundCoverage, double activeCoverage, int[][] gray,
                                long redScore, long darkScore) {
    }

    @NonNull
    private Board buildBoard(List<List<String>> inputImages) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        Board board = new Board();
        for (int row = 0; row <= Constants.END_ROW; row++) {
            System.out.println("row " + row + ": ");
            for (int col = 0; col <= Constants.END_COL; col++) {
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

    public static void main(String[] args) throws IOException {
        List<List<String>> inputImages = Constants.INPUT_IMAGES;

        Chooser chooser = new Chooser();
        Board board = chooser.buildBoard(inputImages);
        System.out.println(board.toFen());

//        String name = "chess/cell_6_6.png";
//        InputStream resourceAsStream = Objects.requireNonNull(Chooser.class.getClassLoader().getResourceAsStream(name));
//        BufferedImage bufferedImage = ImageIO.read(resourceAsStream);
//        ChessCell cell = chooser.choose(bufferedImage, 6, 6);
//        System.out.println(cell);
    }
}
