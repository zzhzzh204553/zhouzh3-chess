package com.zhouzh3.chess.vision;

import java.io.IOException;
import java.nio.file.Path;

public class ImageFenMain {

    public static void main(String[] args) {
        System.setProperty("logback.configurationFile", "classpath:logback-spring.xml");
        Path imageDir = Path.of("src", "main", "resources", "images-old");
        Path screenshot = imageDir.resolve("7.jpg");
        try {
            ImageFenService imageFenService = new ImageFenService();
            String fen = imageFenService.parseImageFen(screenshot, true);
            System.out.println(fen);
        } catch (IOException e) {
            throw new RuntimeException("导出整盘样本失败", e);
        }
    }

}
