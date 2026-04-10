package com.zhouzh3.chess.util;

import com.zhouzh3.chess.model.CropParam;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ImageUtilTest {

    @Test
    public void testCropChessPieces() throws IOException {
        File inputFile = new File("D:\\git.codex\\zhouzh3-chess\\src\\main\\resources\\images\\10.png");
        CropParam cropParam = ImageUtil.cropChessPieces();
        ImageUtil.getCrossingColor(inputFile, cropParam);
    }

}