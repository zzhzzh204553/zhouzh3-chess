package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.fen.Board;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


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

    public static final String ADVISOR = "A";
    public static final int END_ROW = 9;
    public static final int END_COL = 8;
    public static final int IMAGE_SIZE = 114;
    public static final int IMAGE_RADIUS = IMAGE_SIZE / 2;
    public static final int IMAGE_CENTER = IMAGE_SIZE / 2;

    public static final Map<String, String> CANDIDATES = Map.ofEntries(
            Map.entry("R红车", "images/red-ju.png"),
            Map.entry("N红马", "images/red-ma.png"),
            Map.entry("B红相", "images/red-xiang.png"),
            Map.entry("A红仕", "images/red-shi.png"),
            Map.entry("K红帅", "images/red-shuai.png"),
            Map.entry("C红炮", "images/red-pao.png"),
            Map.entry("P红兵", "images/red-bing.png"),
            Map.entry("r黑车", "images/black-ju.png"),
            Map.entry("n黑马", "images/black-ma.png"),
            Map.entry("b黑象", "images/black-xiang.png"),
            Map.entry("a黑士", "images/black-shi.png"),
            Map.entry("k黑将", "images/black-jiang.png"),
            Map.entry("c黑炮", "images/black-pao.png"),
            Map.entry("p黑卒", "images/black-zu.png|images/black-zu2.png")
    );

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
        boolean[][] mask = new boolean[IMAGE_SIZE][IMAGE_SIZE];
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int dx = x - IMAGE_CENTER;
                int dy = y - IMAGE_CENTER;
                mask[y][x] = dx * dx + dy * dy <= IMAGE_RADIUS * IMAGE_RADIUS;
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
        int[][] gray = new int[IMAGE_SIZE][IMAGE_SIZE];
        int pixelCount = 0;
        int foregroundCount = 0;
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
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
        StringBuilder sb = new StringBuilder(IMAGE_SIZE * IMAGE_SIZE);
        double edgeSum = 0d;
        int edgeCount = 0;
        int activeCount = 0;
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
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
        ImageMetrics metrics = extractMetrics(cellImage);
        PieceSide pieceSide = parsePieceSide(cellImage, metrics);
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
            if (best != null && best.contains(ADVISOR)) {
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
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
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
        for (int row = 0; row <= END_ROW; row++) {
            System.out.println("row " + row + ": ");
            for (int col = 0; col <= END_COL; col++) {
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

        Chooser chooser = new Chooser();
        Board board = chooser.buildBoard(INPUT_IMAGES);
        System.out.println(board.toFen());

//        String name = "chess/cell_6_6.png";
//        InputStream resourceAsStream = Objects.requireNonNull(Chooser.class.getClassLoader().getResourceAsStream(name));
//        BufferedImage bufferedImage = ImageIO.read(resourceAsStream);
//        ChessCell cell = chooser.choose(bufferedImage, 6, 6);
//        System.out.println(cell);
    }
}
