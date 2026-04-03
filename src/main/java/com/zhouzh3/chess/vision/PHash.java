package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;
import java.util.Arrays;

public class PHash {
    public static String getHash(BufferedImage img)  {
        // 缩放到 32x32
        BufferedImage resized = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, 32, 32, null);
        g.dispose();

        // 转灰度并获取像素
        double[][] pixels = new double[32][32];
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                Color c = new Color(resized.getRGB(x, y));
                pixels[x][y] = c.getRed();
            }
        }

        // DCT
        double[][] dctVals = applyDCT(pixels);

        // 取左上角 8x8
        double[] vals = new double[64];
        int idx = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                vals[idx++] = dctVals[x][y];
            }
        }

        // 中位数
        double median = Arrays.stream(vals).sorted().toArray()[32];

        // 生成 hash
        StringBuilder hash = new StringBuilder();
        for (double v : vals) {
            hash.append(v > median ? "1" : "0");
        }
        return hash.toString();
    }

    private static double[][] applyDCT(double[][] f) {
        int N = 32;
        double[][] F = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += f[i][j] *
                               Math.cos(((2 * i + 1) * u * Math.PI) / (2 * N)) *
                               Math.cos(((2 * j + 1) * v * Math.PI) / (2 * N));
                    }
                }
                double cu = (u == 0) ? Math.sqrt(1.0 / N) : Math.sqrt(2.0 / N);
                double cv = (v == 0) ? Math.sqrt(1.0 / N) : Math.sqrt(2.0 / N);
                F[u][v] = cu * cv * sum;
            }
        }
        return F;
    }

    public static int hammingDistance(String hash1, String hash2) {
        int dist = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                dist++;
            }
        }
        return dist;
    }

    public static void main(String[] args) throws Exception {
        String h1 = getHash(ImageIO.read(new File("image1.jpg")));
        String h2 = getHash(ImageIO.read(new File("image2.jpg")));
        System.out.println("Hamming Distance: " + hammingDistance(h1, h2));
    }
}
