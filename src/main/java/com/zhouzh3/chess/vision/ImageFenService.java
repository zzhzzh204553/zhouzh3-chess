package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageFenService {

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

    public String identifyPiece(BufferedImage cellImage, Cell cell, Path outputDir) {
        if (pieceHashes.isEmpty()) {
            try {
                init();
            } catch (IOException e) {
                throw new RuntimeException("加载棋子模板失败", e);
            }
        }
        BufferedImage normalized = resize(cellImage, Constants.SIZE, Constants.SIZE);
//        boolean hasPiece = hasPieceByVerticalEdgePoints(normalized);
//        if (!hasPiece) {
//            System.out.println(cell + "============================ hasPiece=false");
//            return "";
//        }

//        boolean pieceSide = isRedPiece(normalized);
        PieceSide pieceSide = parsePieceSide(normalized);
        if (pieceSide == PieceSide.EMPTY) {
            return "";
        }
        String hash = generateHash(normalized);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        System.out.println(cell + "============================ hasPiece=true");
        for (Map.Entry<String, String> entry : pieceHashes.entrySet()) {
            if (pieceSide != getPieceSide(entry.getKey())) {
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

        Path samplePath = outputDir.resolve(String.format("cell_%d_%d_%s.png", cell.getRow(), cell.getCol(), best));
//        ImageIO.write(cellImage, "png", samplePath.toFile());
        System.out.println("sample image: " + samplePath);
        return best;
    }

    private void load(String name, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("模板不存在: " + resourcePath);
            }
            pieceHashes.put(name, generateHash(resize(ImageIO.read(inputStream), Constants.SIZE, Constants.SIZE)));
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
        int[][] gray = new int[Constants.SIZE][Constants.SIZE];
        int pixelCount = 0;
        for (int y = 0; y < Constants.SIZE; y++) {
            for (int x = 0; x < Constants.SIZE; x++) {
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
        StringBuilder sb = new StringBuilder(Constants.SIZE * Constants.SIZE);
        for (int y = 0; y < Constants.SIZE; y++) {
            for (int x = 0; x < Constants.SIZE; x++) {
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

    private PieceSide parsePieceSide(BufferedImage img) {
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
        int dx = x - Constants.CENTER;
        int dy = y - Constants.CENTER;
        return dx * dx + dy * dy <= Constants.COMPARE_RADIUS * Constants.COMPARE_RADIUS;
    }

    private boolean hasPieceByVerticalEdgePoints(BufferedImage img) {
        int topY = Constants.CENTER - Constants.COMPARE_RADIUS + Constants.EDGE_OFFSET;
        int bottomY = Constants.CENTER + Constants.COMPARE_RADIUS - Constants.EDGE_OFFSET;
//        int leftX = CENTER - COMPARE_RADIUS + EDGE_OFFSET;
//        int rightX = CENTER + COMPARE_RADIUS - EDGE_OFFSET;

        int colorTopY = img.getRGB(Constants.CENTER, topY);
        int colorBottomY = img.getRGB(Constants.CENTER, bottomY);
//        int colorLeftX = img.getRGB(leftX, CENTER - EDGE_OFFSET);
//        int colorRightX = img.getRGB(rightX, CENTER - EDGE_OFFSET);

        System.out.println("===============" + toRGB16(colorTopY) + ", " + toRGB16(colorBottomY)  );


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
        int dr = r - Constants.BACKGROUND_R;
        int dg = g - Constants.BACKGROUND_G;
        int db = b - Constants.BACKGROUND_B;
        return Math.sqrt(dr * dr + dg * dg + db * db) <= Constants.COLOR_TOLERANCE;
    }

    public void exportBoardSamples(Path screenshot, Path outputDir, ImageFenMain imageFenMain) throws IOException {
        BufferedImage source = ImageIO.read(screenshot.toFile());
        if (source == null) {
            throw new IOException("无法读取图片: " + screenshot);
        }

        Files.createDirectories(outputDir);

        double scaleX = Constants.BOARD_WIDTH / Constants.BOARD_BASE_WIDTH;
        double scaleY = Constants.BOARD_HEIGHT / Constants.BOARD_BASE_HEIGHT;
        int sampleSize = Math.max(40, (int) Math.round(Math.min(scaleX * Constants.STEP_X, scaleY * Constants.STEP_Y) * 1));

        BufferedImage marked = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(1));

        for (int row = Constants.START_ROW; row <= Constants.END_ROW; row++) {
            for (int col = Constants.START_COL; col <= Constants.END_COL; col++) {
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
                    left = left - 2;
                }

                BufferedImage cellImage = source.getSubimage(left, top, width, height);
                BufferedImage cropCircle = ImageUtil.cropCircle(cellImage);


                Cell cell = new Cell(row, col);

                String chessName = identifyPiece(cropCircle, cell, outputDir);
                Path samplePath = outputDir.resolve(String.format("cell_%d_%d_%s.png", cell.getRow(), cell.getCol(), chessName));
                ImageIO.write(cropCircle, "png", samplePath.toFile());
                System.out.println("sample image: " + samplePath);

                graphics.drawRect(left, top, width, height);
                System.out.printf("cell[%d,%d] center=(%d,%d), rect=(x=%d,y=%d,w=%d,h=%d)%n, %s",
                        row, col, centerX, centerY, left, top, width, height, chessName);
                System.out.println();
            }
        }

        graphics.dispose();

        Path markedPath = outputDir.resolve("zz_board_marked.png");
        ImageIO.write(marked, "png", markedPath.toFile());
        System.out.println("marked image: " + markedPath);
    }
}
