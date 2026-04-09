package com.zhouzh3.chess.vision;

import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;


public class ImageCrop {
    public static final int BOARD_X = 10;
    public static final int BOARD_Y = 676;
    public static final int BOARD_WIDTH = 1159;
    public static final int BOARD_HEIGHT = 1286;


    public static final int OFFSET_X = 22;
    public static final int OFFSET_Y = 14;

    public static final int CHESS_WIDTH = 136 - OFFSET_X;
    public static final int CHESS_HEIGH = 134 - OFFSET_Y;
    public static final float GAP_X = 11;
    public static final float GAP_Y = 5;


    public static final int END_ROW = 9;
    public static final int END_COL = 8;



    public ImageCrop() {
    }

    private void cropImages(File inputFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);

        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = originalImage.getSubimage(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        Graphics2D graphics = boardImage.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.setStroke(new BasicStroke(1));

        // 保存截取后的图片
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()));
        Files.createDirectories(path);

        for (int row = 0; row <= END_ROW; row++) {
            for (int col = 0; col <= END_COL; col++) {
                int x = (int) (OFFSET_X + col * (CHESS_WIDTH + GAP_X));
                int y = (int) (OFFSET_Y + row * (CHESS_HEIGH + GAP_Y));
                BufferedImage cellImage = boardImage.getSubimage(x, y, CHESS_WIDTH, CHESS_HEIGH);

                BufferedImage cropCircle = cropCircle(cellImage);
                Path resolve = path.resolve("cell_" + row + "_" + col + ".png");
                write(cropCircle, resolve);

                graphics.drawRect(x, y, CHESS_WIDTH, CHESS_HEIGH);
            }
        }
        write(boardImage, path.resolve("zz_board" + inputFile.getName()));
        System.out.println("图片截取完成，保存为" + path.toAbsolutePath().toString());
    }

    private static BufferedImage cropCircle(BufferedImage src) {
        return cropCircle(src, 0, 0, 0);
    }

    private static BufferedImage cropCircle(BufferedImage src, int offsetX, int offsetY, int radiusOffset) {
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


    private static void write(BufferedImage bufferedImage,
                              Path output) throws IOException {
        String suffix = FileUtil.getSuffix(output.getFileName().toString());
        ImageIO.write(bufferedImage, suffix, output.toFile());
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // 读取原始图片
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\8.png");
        ImageCrop imageCrop = new ImageCrop();

        imageCrop.cropImages(inputFile);

//        Map<String, String> map = imageCrop.loadPieceHashes();
//        map.forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println(map.size());
    }

}
