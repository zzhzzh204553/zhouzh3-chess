package com.zhouzh3.chess.util;

import cn.hutool.core.io.FileUtil;
import com.zhouzh3.chess.constants.ChessConstants;
import com.zhouzh3.chess.model.CropParam;
import com.zhouzh3.chess.model.Region;
import com.zhouzh3.chess.model.RgbColor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.zhouzh3.chess.constants.ChessConstants.*;


/**
 * @author haig
 */
@Slf4j
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

    public static CropParam cropPieces() {
        Region chessBoardRegion = new Region(ChessConstants.BOARD_X, ChessConstants.BOARD_Y, ChessConstants.BOARD_WIDTH, ChessConstants.BOARD_HEIGHT);
        List<List<Region>> chessPieceGrid = new ArrayList<List<Region>>();
        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            List<Region> chessPieceRow = new ArrayList<Region>();
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
                int x = (int) (ChessConstants.OFFSET_X + col * (ChessConstants.CHESS_WIDTH + ChessConstants.GAP_X));
                int y = (int) (ChessConstants.OFFSET_Y + row * (CHESS_HEIGH + ChessConstants.GAP_Y));
                chessPieceRow.add(new Region(x, y, ChessConstants.CHESS_WIDTH, CHESS_HEIGH));
            }
            chessPieceGrid.add(chessPieceRow);
        }

        return new CropParam(chessBoardRegion, chessPieceGrid);
    }

    public static void cropImages(File inputFile, CropParam cropParam) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = getSubimage(originalImage, cropParam.chessBoardRegion());

        Graphics2D graphics = boardImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(1));

        // 保存截取后的图片
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()));
        Files.createDirectories(path);

        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
//                int x = (int) (OFFSET_X + col * (CHESS_WIDTH + GAP_X));
//                int y = (int) (OFFSET_Y + row * (CHESS_HEIGH + GAP_Y));
                Region region = cropParam.getChessPiece(row, col);
                BufferedImage cellImage = getSubimage(boardImage, region);

                BufferedImage cropCircle = cropCircle(cellImage);
                write(cropCircle, path.resolve("cell_" + row + "_" + col + ".png"));
                graphics.drawRect(region.x(), region.y(), region.width(), region.height());

            }
        }
        write(boardImage, path.resolve("zz_board_" + inputFile.getName()));
        log.info("图片截取完成，保存为{}", path.toAbsolutePath().toString());
    }

    public static CropParam cropPiecesNew() {
        Region chessBoardRegion = new Region(ChessConstants.BOARD_X, ChessConstants.BOARD_Y, ChessConstants.BOARD_WIDTH, ChessConstants.BOARD_HEIGHT);
        List<List<Region>> chessPieceGrid = new ArrayList<List<Region>>();
        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            List<Region> chessPieceRow = new ArrayList<Region>();
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
                int x = (int) (ChessConstants.ORIGIN_X + col * SIDE_LENGTH - CHESS_WIDTH / 2);
                int y = (int) (ChessConstants.ORIGIN_Y + row * SIDE_LENGTH - CHESS_HEIGH / 2);
                chessPieceRow.add(new Region(x, y, CHESS_WIDTH, CHESS_HEIGH));
//                chessPieceRow.add(new Region(x, y, CELL_RADIUS * 2, CELL_RADIUS * 2));
            }
            chessPieceGrid.add(chessPieceRow);
        }

        return new CropParam(chessBoardRegion, chessPieceGrid);
    }

    public static void getCrossingColor(File inputFile, CropParam cropParam) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = getSubimage(originalImage, cropParam.chessBoardRegion());
        Graphics2D g2d = boardImage.createGraphics();
        g2d.setColor(Color.RED);


        for (int row = 0; row <= ChessConstants.END_ROW; row++) {
            System.out.print("row_" + row + ": ");
            for (int col = 0; col <= ChessConstants.END_COL; col++) {
                Region region = cropParam.getChessPiece(row, col);
                int x = region.x() + CHESS_WIDTH / 2;
                int y = region.y() + CHESS_HEIGH / 2;
                int r = 5;

                RgbColor rgbColor = getColor(boardImage, x, y);
                System.out.print(row + "_" + col + Objects.requireNonNull(rgbColor).toHex() + "    ");
                g2d.fillOval(x - r, y - r, 2 * r, 2 * r);

            }
            System.out.println("-----------------\n\n");
        }
        g2d.dispose();
        Path chess = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()) + "_color.png");
        System.out.println(chess.toAbsolutePath().toString());
        write(boardImage, chess);
    }

    @Nullable
    public static RgbColor getColor(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 255;
        if (a == 0) {
            return null;
        }
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return new RgbColor(r, g, b);
    }
}
