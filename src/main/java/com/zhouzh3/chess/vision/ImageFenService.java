package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageFenService {
    private static final int SIZE = 121;
    private static final int CENTER = SIZE / 2 - 8;
    private static final int COMPARE_RADIUS = SIZE / 2 - 20;
    private static final int BACKGROUND_R = 0xFF;
    private static final int BACKGROUND_G = 0xCC;
    private static final int BACKGROUND_B = 0x90;
    private static final int EDGE_OFFSET = 10;
    private static final double COLOR_TOLERANCE = 26.0;

    private final Map<String, String> pieceHashes = new LinkedHashMap<>();

    public void init() throws IOException {
        pieceHashes.clear();
        load("R红车", "images/red-ju.png");
        load("N红马", "images/red-ma.png");
        load("B红相", "images/red-xiang.png");
        load("A红仕", "images/red-shi.png");
        load("K红帅", "images/red-shuai.png");
        load("C红炮", "images/red-pao.png");
        load("P红兵", "images/red-bing.png");
        load("r黑车", "images/black-ju.png");
        load("n黑马", "images/black-ma.png");
        load("b黑象", "images/black-xiang.png");
        load("a黑士", "images/black-shi.png");
        load("k黑将", "images/black-jiang.png");
        load("c黑炮", "images/black-pao.png");
        load("p黑卒", "images/black-zu.png");
    }

    public String identifyPiece(BufferedImage cellImage, Map<String, Object> params) {
        if (pieceHashes.isEmpty()) {
            try {
                init();
            } catch (IOException e) {
                throw new RuntimeException("加载棋子模板失败", e);
            }
        }
        BufferedImage normalized = resize(cellImage, SIZE, SIZE);
//        boolean hasPiece = hasPieceByVerticalEdgePoints(normalized);
//        if (!hasPiece) {
//            System.out.println(params + "============================ hasPiece=false");
//            return "";
//        }

//        boolean redFirst = isRedPiece(normalized);
        PieceSide redFirst = getPieceSide(normalized);
        if (redFirst == PieceSide.EMPTY) {
            return "";
        }
        String hash = generateHash(normalized);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        System.out.println(params + "============================ hasPiece=true");
        for (Map.Entry<String, String> entry : pieceHashes.entrySet()) {
//            if (redFirst != isRedName(entry.getKey())) {
            if (redFirst != getPieceSide(entry.getKey())) {
                continue;
            }
            int distance = hammingDistance(hash, entry.getValue());

            System.out.print(entry.getKey() + ": " + distance + "  ");
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }
        System.out.println();
        return best;
    }

    private void load(String name, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("模板不存在: " + resourcePath);
            }
            pieceHashes.put(name, generateHash(resize(ImageIO.read(inputStream), SIZE, SIZE)));
        }
    }

    private BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return resized;
    }

    private String generateHash(BufferedImage img) {
        long sum = 0;
        int[][] gray = new int[SIZE][SIZE];
        int pixelCount = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!isInsideCircle(x, y)) {
                    continue;
                }
                int rgb = img.getRGB(x, y);
                int a = (rgb >>> 24) & 255;
                if (a == 0) {
                    continue;
                }
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;
                int v = (r + g + b) / 3;
                gray[y][x] = v;
                sum += v;
                pixelCount++;
            }
        }
        if (pixelCount == 0) {
            throw new IllegalArgumentException("圆形区域内没有可比较的像素");
        }
        int avg = (int) (sum / pixelCount);
        StringBuilder sb = new StringBuilder(SIZE * SIZE);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!isInsideCircle(x, y)) {
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

    private PieceSide getPieceSide(BufferedImage img) {
        long redScore = 0;
        long darkScore = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 255;
                if (a == 0) {
                    continue;
                }
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                if (r > 120 && r > g + 12 && r > b + 12) {
                    redScore += r - Math.max(g, b);
                } else if (r + g + b < 360) {
                    darkScore += 360 - (r + g + b);
                }
            }
        }
        System.out.println("redScore=" + redScore + ", darkScore=" + darkScore);

//        return redScore >= darkScore;
        return PieceSide.fromScore(redScore, darkScore);
    }

    private PieceSide getPieceSide(String name) {
        return !name.isEmpty() && Character.isUpperCase(name.charAt(0)) ? PieceSide.RED : PieceSide.BLACK;
    }

    public String toRGB16(int rgb) {
// 去掉透明度，只保留 RGB
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

// 转换成 #RRGGBB 格式
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private boolean isInsideCircle(int x, int y) {
        int dx = x - CENTER;
        int dy = y - CENTER;
        return dx * dx + dy * dy <= COMPARE_RADIUS * COMPARE_RADIUS;
    }

    private boolean hasPieceByVerticalEdgePoints(BufferedImage img) {
        int topY = CENTER - COMPARE_RADIUS + EDGE_OFFSET;
        int bottomY = CENTER + COMPARE_RADIUS - EDGE_OFFSET;
        int leftX = CENTER - COMPARE_RADIUS + EDGE_OFFSET;
        int rightX = CENTER + COMPARE_RADIUS - EDGE_OFFSET;

        int colorTopY = img.getRGB(CENTER, topY);
        int colorBottomY = img.getRGB(CENTER, bottomY);
        int colorLeftX = img.getRGB(leftX, CENTER - EDGE_OFFSET);
        int colorRightX = img.getRGB(rightX, CENTER - EDGE_OFFSET);

        System.out.println("===============" + toRGB16(colorTopY) + ", " + toRGB16(colorBottomY) + ", " + toRGB16(colorLeftX) + ", " + toRGB16(colorRightX));
        System.out.println("===============" + isBgColor(colorTopY) + ", " + isBgColor(colorBottomY) + ", " + isBgColor(colorLeftX) + ", " + isBgColor(colorRightX));


        return isBgColor(colorTopY) && isBgColor(colorBottomY);
    }

    private boolean isBgColor(int argb) {
        int a = (argb >>> 24) & 255;
        if (a == 0) {
            return false;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int dr = r - BACKGROUND_R;
        int dg = g - BACKGROUND_G;
        int db = b - BACKGROUND_B;
        return Math.sqrt(dr * dr + dg * dg + db * db) <= COLOR_TOLERANCE;
    }

}
