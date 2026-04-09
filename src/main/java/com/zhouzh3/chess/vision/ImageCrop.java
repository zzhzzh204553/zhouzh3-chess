package com.zhouzh3.chess.vision;

import cn.hutool.core.io.FileUtil;
import com.zhouzh3.chess.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author haig
 */
@Slf4j
@Service
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


    protected void cropImages(File inputFile, CropParam cropParam) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = ImageUtil.getSubimage(originalImage, cropParam.chessBoardRegion());

        Graphics2D graphics = boardImage.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.setStroke(new BasicStroke(1));

        // 保存截取后的图片
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()));
        Files.createDirectories(path);

        for (int row = 0; row <= END_ROW; row++) {
            for (int col = 0; col <= END_COL; col++) {
//                int x = (int) (OFFSET_X + col * (CHESS_WIDTH + GAP_X));
//                int y = (int) (OFFSET_Y + row * (CHESS_HEIGH + GAP_Y));
                Region region = cropParam.getChessPieceRegion(row, col);
                BufferedImage cellImage = ImageUtil.getSubimage(boardImage, region);

                BufferedImage cropCircle = ImageUtil.cropCircle(cellImage);
                ImageUtil.write(cropCircle, path.resolve("cell_" + row + "_" + col + ".png"));
                graphics.drawRect(region.x(), region.y(), region.width(), region.height());

            }
        }
        ImageUtil.write(boardImage, path.resolve("zz_board_" + inputFile.getName()));
        log.info("图片截取完成，保存为{}", path.toAbsolutePath().toString());
    }


    @Deprecated
    protected void cropImages(File inputFile, boolean debug) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        // 定义矩形区域 (x, y, width, height)
        // 截取矩形区域
        BufferedImage boardImage = originalImage.getSubimage(BOARD_X, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);

        Graphics2D graphics = null;
        if (debug) {
            graphics = boardImage.createGraphics();
            graphics.setColor(java.awt.Color.RED);
            graphics.setStroke(new BasicStroke(1));
        }

        // 保存截取后的图片
        Path path = null;
        if (debug) {
            path = Paths.get(System.getProperty("java.io.tmpdir"), "chess", FileUtil.getPrefix(inputFile.getName()));
            Files.createDirectories(path);
        }

        for (int row = 0; row <= END_ROW; row++) {
            for (int col = 0; col <= END_COL; col++) {
                int x = (int) (OFFSET_X + col * (CHESS_WIDTH + GAP_X));
                int y = (int) (OFFSET_Y + row * (CHESS_HEIGH + GAP_Y));


                BufferedImage cellImage = boardImage.getSubimage(x, y, CHESS_WIDTH, CHESS_HEIGH);

                BufferedImage cropCircle = ImageUtil.cropCircle(cellImage);
                if (debug) {
                    Path resolve = path.resolve("cell_" + row + "_" + col + ".png");
                    ImageUtil.write(cropCircle, resolve);
                    graphics.drawRect(x, y, CHESS_WIDTH, CHESS_HEIGH);
                }

            }
        }
        if (debug) {
            ImageUtil.write(boardImage, path.resolve("zz_board_" + inputFile.getName()));
            log.info("图片截取完成，保存为{}", path.toAbsolutePath().toString());
        }
    }


}
