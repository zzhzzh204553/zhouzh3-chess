package com.zhouzh3.chess.service;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author haig
 */
@Slf4j
public class ChessManualParser {

    private static final String DEFAULT_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

    public static void main(String[] args) throws IOException {
        String dirPath = "E:\\棋谱\\03_开局大全\\中炮局\\中炮对单提马";

        List<String> jsonList = Files.walk(Paths.get(dirPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .map(path -> {
                    try {
                        return processFile(path.toFile());
                    } catch (IOException e) {
                        log.error("Error processing file: {}", path, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((o1, o2) -> o1.get("title").compareTo(o2.get("title")))
                .map(ChessManualParser::toJson)
                .toList();

        String output = jsonList.stream()
                .collect(Collectors.joining(",\n",
                        """
                                const gameTree = [
                                {
                                    title: "中炮局",
                                    children: [
                                        {
                                            title: "中炮对单提马",
                                            children: [
                                                {
                                                    title: "“古川杯“第二轮攀枝花赵攀伟先胜邛崃杨德金",
                                                    fen: "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w",
                                                    moves: "h2e2b9c7h0g2h9i7c3c4i9h9b0c2d9e8c2d4c9e7b2d2h7f7a0b0b7a7d4c6h9h4c0a2a7a3g3g4h4h5b0b3a3a4b3b4a6a5i0h0h5d5f0e1f7f6c6e7d5d7e7g6i7g6e2e6d7e7e6g6c7d5c4c5d5e3d2e2e3c2b4c4c2b0a2c0a9b9h0h3f6e6c5d5a4a0h3a3b0a2a3a2b9b0a2c2a5a4d5d6a4b4c4c9e8d9d6e6"
                                                },""",
                        """
                                
                                                ]
                                            }
                                        ]
                                    }
                                ];"""));

        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "chess");
        // 在该目录下生成一个临时文件，前缀为 "game"，后缀为 ".tmp"
        Path tempFile = Files.createTempFile(dir, "notation_", ".json");
        Files.writeString(tempFile, output);
        System.out.println(tempFile.toUri().toURL());

    }

    private static Map<String, String> processFile(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());

        String title = null;
        String fen = null;
        String moves = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (i == 1) {
                title = FileUtil.getPrefix(file.getName()).trim();
            } else if (line.startsWith("==== 初始布局 ====")) {
                if (i + 1 < lines.size()) {
                    fen = lines.get(i + 1).trim();
                    if (fen.equals("#")) {
                        fen = DEFAULT_FEN;
                    }
                }
            } else if (line.startsWith("==== 棋谱走法 ====")) {
                if (i + 1 < lines.size()) {
                    moves = lines.get(i + 1).trim();
                    if (moves.startsWith("#")) {
                        moves = moves.substring(2).trim();
                    }
                }
            }
        }

        if (title != null && fen != null && moves != null) {
            Map<String, String> obj = new LinkedHashMap<>();
            obj.put("title", title);
            obj.put("fen", fen);
            obj.put("moves", moves);
            return obj;
        }
        return null;
    }

    private static String toJson(Map<String, String> map) {
        return map.entrySet().stream()
                .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
                .collect(Collectors.joining(",\n", "{\n", "\n}"));
    }
}
