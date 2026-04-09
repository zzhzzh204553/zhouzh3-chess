package com.zhouzh3.chess.util;

import cn.hutool.core.io.FileUtil;
import com.zhouzh3.chess.vision.CropParam;
import com.zhouzh3.chess.vision.ImageCrop;
import com.zhouzh3.chess.vision.Region;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil {
    public static BufferedImage getSubimage(BufferedImage src, Region region) {
        return src.getSubimage(region.x(), region.y(), region.width(), region.height());
    }

    public static BufferedImage cropCircle(BufferedImage src, int offsetX, int offsetY, int radiusOffset) {
        // 基础半径取子图最小边的一半
        int baseRadius = Math.min(src.getWidth(), src.getHeight()) / 2;
        int radius = baseRadius + radiusOffset;
        int diameter = radius * 2;

        // 创建透明背景的新图
        BufferedImage circleImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = circleImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置裁剪区域为圆形
        g2.setClip(new Ellipse2D.Double(0, 0, diameter, diameter));

        // 将原图绘制到新图上，偏移保证圆心正确
        g2.drawImage(src, -offsetX + (baseRadius - radius), -offsetY + (baseRadius - radius), null);

//        g2.dispose();
        return circleImage;
    }

    public static BufferedImage cropCircle(BufferedImage src) {
        return cropCircle(src, 0, 0, 0);
    }

    public static void write(BufferedImage bufferedImage,
                             Path output) throws IOException {
        String suffix = FileUtil.getSuffix(output.getFileName().toString());
        ImageIO.write(bufferedImage, suffix, output.toFile());
    }

    public static CropParam cropChessPieces() {
        Region chessBoardRegion = new Region(ImageCrop.BOARD_X, ImageCrop.BOARD_Y, ImageCrop.BOARD_WIDTH, ImageCrop.BOARD_HEIGHT);
        List<List<Region>> chessPieceGrid = new ArrayList<List<Region>>();
        for (int row = 0; row <= ImageCrop.END_ROW; row++) {
            List<Region> chessPieceRow = new ArrayList<Region>();
            for (int col = 0; col <= ImageCrop.END_COL; col++) {
                int x = (int) (ImageCrop.OFFSET_X + col * (ImageCrop.CHESS_WIDTH + ImageCrop.GAP_X));
                int y = (int) (ImageCrop.OFFSET_Y + row * (ImageCrop.CHESS_HEIGH + ImageCrop.GAP_Y));
                chessPieceRow.add(new Region(x, y, ImageCrop.CHESS_WIDTH, ImageCrop.CHESS_HEIGH));
            }
            chessPieceGrid.add(chessPieceRow);
        }

        return new CropParam(chessBoardRegion, chessPieceGrid);
    }
}
