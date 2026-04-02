package com.zhouzh3.chess.vision;

import java.io.IOException;
import java.nio.file.Path;

public class ImageFenMain {

    private final ImageFenService imageFenService ;

    public ImageFenMain(ImageFenService imageFenService) {
        this.imageFenService = imageFenService;
    }

    public static void main(String[] args) throws IOException {
        Path imageDir = Path.of("src", "main", "resources", "images");
        Path screenshot = imageDir.resolve("3.jpg");
        Path outputDir = Path.of("C:\\Users\\haig\\Desktop\\素材\\sample\\3");

        ImageFenService imageFenService = new ImageFenService();
        imageFenService.init();

        ImageFenMain imageFenMain = new ImageFenMain(imageFenService);
        try {
            imageFenMain.imageFenService.exportBoardSamples(screenshot, outputDir, imageFenMain);
        } catch (IOException e) {
            throw new RuntimeException("导出整盘样本失败", e);
        }
    }

}
