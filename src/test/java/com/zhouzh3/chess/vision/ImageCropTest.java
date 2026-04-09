package com.zhouzh3.chess.vision;

import com.zhouzh3.chess.util.ImageUtil;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ImageCropTest {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // 读取原始图片
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\8.png");
        ImageCrop imageCrop = new ImageCrop();

        CropParam cropParam = ImageUtil.cropChessPieces();
        imageCrop.cropImages(inputFile, cropParam);

//        Map<String, String> map = imageCrop.loadPieceHashes();
//        map.forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println(map.size());
    }
}