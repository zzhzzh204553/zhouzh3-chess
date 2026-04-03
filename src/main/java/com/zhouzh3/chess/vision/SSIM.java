package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class SSIM {
    public static double computeSSIM(BufferedImage img1, BufferedImage img2) throws IOException {
        ClassLoader classLoader = SSIM.class.getClassLoader();

        int width = Math.min(img1.getWidth(), img2.getWidth());
        int height = Math.min(img1.getHeight(), img2.getHeight());

        double mean1 = 0, mean2 = 0;
        double var1 = 0, var2 = 0, cov = 0;
        int n = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y) & 0xFF;
                int rgb2 = img2.getRGB(x, y) & 0xFF;
                mean1 += rgb1;
                mean2 += rgb2;
            }
        }
        mean1 /= n;
        mean2 /= n;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y) & 0xFF;
                int rgb2 = img2.getRGB(x, y) & 0xFF;
                var1 += Math.pow(rgb1 - mean1, 2);
                var2 += Math.pow(rgb2 - mean2, 2);
                cov += (rgb1 - mean1) * (rgb2 - mean2);
            }
        }
        var1 /= (n - 1);
        var2 /= (n - 1);
        cov /= (n - 1);

        double c1 = 6.5025, c2 = 58.5225;
        return ((2 * mean1 * mean2 + c1) * (2 * cov + c2)) /
                ((mean1 * mean1 + mean2 * mean2 + c1) * (var1 + var2 + c2));
    }

}
