package com.zhouzh3.chess.vision;

import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;


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


    private final Map<String, String> pieceHashes;

    public ImageCrop() throws IOException {
        this.pieceHashes = loadPieceHashes();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // 读取原始图片
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\8.png");
        ImageCrop imageCrop = new ImageCrop();

        imageCrop.aaa(inputFile);

//        Map<String, String> map = imageCrop.loadPieceHashes();
//        map.forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println(map.size());
    }

    private Map<String, String> loadPieceHashes() throws IOException {
        Map<String, String> hashes = new LinkedHashMap<>();
        load(hashes, "R红车", "images/red-ju.png");
        load(hashes, "N红马", "images/red-ma.png");
        load(hashes, "B红相", "images/red-xiang.png");
        load(hashes, "A红仕", "images/red-shi.png");
        load(hashes, "K红帅", "images/red-shuai.png");
        load(hashes, "C红炮", "images/red-pao.png");
        load(hashes, "P红兵", "images/red-bing.png");
        load(hashes, "r黑车", "images/black-ju.png");
        load(hashes, "n黑马", "images/black-ma.png");
        load(hashes, "b黑象", "images/black-xiang.png");
        load(hashes, "a黑士", "images/black-shi.png");
        load(hashes, "k黑将", "images/black-jiang.png");
        load(hashes, "c黑炮", "images/black-pao.png");
        load(hashes, "p黑卒", "images/black-zu.png");
        return Map.copyOf(hashes);
    }

    private void aaa(File inputFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);

        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = originalImage.getSubimage(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        Graphics2D graphics = boardImage.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.setStroke(new BasicStroke(1));

        // 保存截取后的图片
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "chess");
        Files.createDirectories(path.getParent());

        for (int row = 0; row <= END_ROW; row++) {
            for (int col = 0; col <= END_COL; col++) {
                int x = (int) (OFFSET_X + col * (CHESS_WIDTH + GAP_X));
                int y = (int) (OFFSET_Y + row * (CHESS_HEIGH + GAP_Y));
                BufferedImage cellImage = boardImage.getSubimage(x, y, CHESS_WIDTH, CHESS_HEIGH);

                BufferedImage cropCircle = cropCircle(cellImage);
                String hash = hashCirclePng(cropCircle);
                Path resolve = path.resolve("cell_" + row + "_" + col + ".png");
                write(cropCircle, resolve);
                System.out.println(resolve + ", hash = " + hash);

                graphics.drawRect(x, y, CHESS_WIDTH, CHESS_HEIGH);
            }
        }
        write(boardImage, path.resolve("zz_board.png"));
        System.out.println("图片截取完成，保存为" + path.toAbsolutePath().toString());
    }

    private void load(Map<String, String> hashes, String key, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("模板不存在: " + resourcePath);
            }
            hashes.put(key, hashCirclePng(ImageIO.read(inputStream)));
        }
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

    public static String hashCirclePng(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        // 用 ByteBuffer 收集非透明像素的 RGBA 值
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                // 只保留非透明像素
                if (alpha > 0) {
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = pixel & 0xff;
                    buffer.put((byte) red);
                    buffer.put((byte) green);
                    buffer.put((byte) blue);
                    buffer.put((byte) alpha);
                }
            }
        }

        buffer.flip(); // 准备读取

        // 计算 SHA-256 哈希
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hashBytes = digest.digest(buffer.array());

        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
