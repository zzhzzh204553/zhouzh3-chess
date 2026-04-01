package com.zhouzh3.chess.vision;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageFenMain {
    private static final int BOARD_X = 10;
    private static final int BOARD_Y = 676;
    private static final int BOARD_WIDTH = 1159;
    private static final int BOARD_HEIGHT = 1286;

    private static final double BOARD_BASE_WIDTH = 570.0;
    private static final double BOARD_BASE_HEIGHT = 637.0;
    private static final double START_X = 45.0;
    private static final double START_Y = 47.0;
    private static final double STEP_X = 60.0;
    private static final double STEP_Y = 60.0;
    private static final int OFFSET_X = -14;
    private static final int OFFSET_Y = -22;
    private static final int EXTRA_GAP_X = 4;
    private static final int EXTRA_GAP_Y = 4;

    private static final int START_ROW = 0;
    private static final int END_ROW = 9;
    private static final int START_COL = 0;
    private static final int END_COL = 8;


    public static void main(String[] args) throws IOException {
        Path imageDir = Path.of("src", "main", "resources", "images");
        Path screenshot = imageDir.resolve("3.jpg");
        Path outputDir = Path.of("C:\\Users\\haig\\Desktop\\素材\\sample\\3");

        ImageFenMain imageFenMain = new ImageFenMain();

        ImageFenService imageFenService = new ImageFenService();
        imageFenService.init();
        try {
            imageFenMain.exportBoardSamples(screenshot, outputDir, imageFenService);
        } catch (IOException e) {
            throw new RuntimeException("导出整盘样本失败", e);
        }
    }

    private void exportBoardSamples(Path screenshot, Path outputDir, ImageFenService imageFenService) throws IOException {
        BufferedImage source = ImageIO.read(screenshot.toFile());
        if (source == null) {
            throw new IOException("无法读取图片: " + screenshot);
        }

        Files.createDirectories(outputDir);

        double scaleX = BOARD_WIDTH / BOARD_BASE_WIDTH;
        double scaleY = BOARD_HEIGHT / BOARD_BASE_HEIGHT;
        int sampleSize = Math.max(40, (int) Math.round(Math.min(scaleX * STEP_X, scaleY * STEP_Y) * 1));
//        System.out.printf("sample size: %d x %d%n", 40, sampleSize);

        BufferedImage marked = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setColor(Color.RED);
//        graphics.setStroke(new BasicStroke(1));

        for (int row = START_ROW; row <= END_ROW; row++) {
            for (int col = START_COL; col <= END_COL; col++) {
                int centerX = BOARD_X
                        + (int) Math.round((START_X + col * STEP_X) * scaleX)
                        + OFFSET_X
                        + col * EXTRA_GAP_X;
                int centerY = BOARD_Y
                        + (int) Math.round((START_Y + row * STEP_Y) * scaleY)
                        + OFFSET_Y
                        + row * EXTRA_GAP_Y;
                int left = Math.max(0, centerX - sampleSize / 2);
                int top = Math.max(0, centerY - sampleSize / 2);
                int width = Math.min(sampleSize, source.getWidth() - left);
                int height = Math.min(sampleSize, source.getHeight() - top);
                if (col >= 4) {
                    left = left - 2;
                }

                BufferedImage cellImage = source.getSubimage(left, top, width, height);
                BufferedImage cropCircleAuto = cropCircle(cellImage);
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("row", row);
                params.put("col", col);
                String chessName = imageFenService.identifyPiece(cropCircleAuto, params);
                Path samplePath = outputDir.resolve(String.format("cell_%d_%d_%s.png", row, col, chessName));
                ImageIO.write(cropCircleAuto, "png", samplePath.toFile());

                graphics.drawRect(left, top, width, height);
                System.out.printf("cell[%d,%d] center=(%d,%d), rect=(x=%d,y=%d,w=%d,h=%d)%n, %s",
                        row, col, centerX, centerY, left, top, width, height, chessName);
                System.out.println("sample image: " + samplePath);
                System.out.println();
            }
        }

        graphics.dispose();

        Path markedPath = outputDir.resolve("zz_board_marked.png");
        ImageIO.write(marked, "png", markedPath.toFile());
        System.out.println("marked image: " + markedPath);
    }

    /**
     * 将输入的方形 BufferedImage 转换为圆形，周围透明。
     * 半径自动取最小边长的一半。
     *
     * @param cellImage 输入的方形图像
     * @return 输出的圆形透明图像
     */
    public BufferedImage cropCircle(BufferedImage cellImage) {
        int width = cellImage.getWidth();
        int height = cellImage.getHeight();
//        半径
        int radius = Math.min(width, height) / 2 - 20;

        BufferedImage circleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

//        计算圆心坐标
        int cx = width / 2;
        int cy = height / 2 - 8;
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

    /**
     * 批量裁剪：输入多张图像，返回对应的圆形透明图像列表
     *
     * @param images 输入的图像列表
     * @return 输出的圆形透明图像列表
     */
    public java.util.List<BufferedImage> cropCircleBatch(java.util.List<BufferedImage> images) {
        java.util.List<BufferedImage> results = new ArrayList<>(images.size());
        for (BufferedImage img : images) {
            results.add(cropCircle(img));
        }
        return results;
    }
}
