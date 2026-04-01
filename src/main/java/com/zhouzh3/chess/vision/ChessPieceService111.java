package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChessPieceService111 {

    private final Map<String, String> pieceHashes = new HashMap<>();

    public ChessPieceService111() {
    }

    public void init() throws IOException, IOException {
        Map<String, String> map = Map.ofEntries(
                Map.entry("R红车", "red-ju.png"),
                Map.entry("N红马", "red-ma.png"),
                Map.entry("B红相", "red-xiang.png"),
                Map.entry("A红仕", "red-shi.png"),
                Map.entry("K红帅", "red-shuai.png"),
                Map.entry("C红炮", "red-pao.png"),
                Map.entry("P红兵", "red-bing.png"),
                Map.entry("r黑车", "black-ju.png"),
                Map.entry("n黑马", "black-ma.png"),
                Map.entry("b黑象", "black-xiang.png"),
                Map.entry("a黑士", "black-shi.png"),
                Map.entry("k黑将", "black-jiang.png"),
                Map.entry("c黑炮", "black-pao.png"),
                Map.entry("p黑卒", "black-zu.png")
        );


        Map<String, BufferedImage> templates = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            File input = new File("src/main/resources/images/" + entry.getValue());
            System.out.println(input);
            BufferedImage template = ImageIO.read(input);
            templates.put(entry.getKey(), template);
        }
        loadTemplates(templates);
    }

    // 加载模板库并生成哈希
    public void loadTemplates(Map<String, BufferedImage> templates) {
        for (Map.Entry<String, BufferedImage> entry : templates.entrySet()) {
            // 强制缩放到 121x121，保证和截图一致
            BufferedImage normalized = resize(entry.getValue(), 121, 121);
            String hash = generateHash(normalized);
            pieceHashes.put(entry.getKey(), hash);
        }
    }

    // 识别棋子
    public String identifyPiece(BufferedImage cellImage, Map<String, Object> params) {
        BufferedImage normalized = resize(cellImage, 121, 121);
        String cellHash = generateHash(normalized);

        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        System.out.println("==" + params + "===============================================");
        for (Map.Entry<String, String> entry : pieceHashes.entrySet()) {
            int distance = hammingDistance(cellHash, entry.getValue());
            System.out.println(entry.getKey() + ": " + distance);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = entry.getKey();
            }
        }
        return bestMatch;
    }

    // 生成简单哈希：灰度均值法
    private String generateHash(BufferedImage img) {
        double[][] gray = toGrayMatrix(img);
        double avg = 0;
        for (double[] row : gray) {
            for (double val : row) {
                avg += val;
            }
        }
        avg /= (121 * 121);

        StringBuilder sb = new StringBuilder();
        for (double[] row : gray) {
            for (double val : row) {
                sb.append(val > avg ? '1' : '0');
            }
        }
        return sb.toString();
    }

    // 缩放图片
    private BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return resized;
    }

    // 转灰度矩阵
    private double[][] toGrayMatrix(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] gray = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                gray[y][x] = (r + g + b) / 3.0;
            }
        }
        return gray;
    }

    // 汉明距离
    private int hammingDistance(String s1, String s2) {
        int dist = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) dist++;
        }
        return dist;
    }
}
