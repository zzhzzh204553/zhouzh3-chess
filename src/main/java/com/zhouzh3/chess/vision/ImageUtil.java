package com.zhouzh3.chess.vision;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class ImageUtil {
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
}
