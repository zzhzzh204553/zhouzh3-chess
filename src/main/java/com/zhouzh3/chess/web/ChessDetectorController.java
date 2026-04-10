package com.zhouzh3.chess.web;

import com.zhouzh3.chess.fen.Board;
import com.zhouzh3.chess.model.CropParam;
import com.zhouzh3.chess.util.ImageUtil;
import com.zhouzh3.chess.vision.ChessDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


/**
 * @author haig
 */
@Slf4j
@RestController
@RequestMapping("/api/chess")
public class ChessDetectorController {

    @Autowired
    private ChessDetector chessDetector;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "detect")
    public Map<String, String> detectChessPieces(@RequestParam("file") MultipartFile file) throws Exception {
        String suffix = StringUtils.getFilenameExtension(file.getOriginalFilename());
        Path screenshot = Files.createTempFile("image-fen-", suffix == null ? ".png" : "." + suffix);
        try {
            file.transferTo(screenshot);

            CropParam cropParam = ImageUtil.cropPiecesNew();
            Board board = chessDetector.detectChessPieces(screenshot.toFile(), cropParam);
//            ImageFenService imageFenService = new ImageFenService();
//            String fen = imageFenService.parseImageFen(screenshot, true);
            return Map.of("fen", board.toFen());
        } catch (Exception e) {
            log.error("解析图片失败", e);
            throw e;
        } finally {
            Files.deleteIfExists(screenshot);
        }
    }
}
