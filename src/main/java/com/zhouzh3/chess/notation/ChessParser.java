package com.zhouzh3.chess.notation;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author haig
 */
@Slf4j
public class ChessParser {

    private static final String DEFAULT_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

    private final ObjectMapper mapper;

    public ChessParser() {
        this.mapper = new ObjectMapper();
        // 如果还想不输出空集合、空字符串等，可以用：
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public static void main(String[] args) throws IOException {
        ChessParser chessParser = new ChessParser();

//        Path start = Paths.get("E:\\棋谱\\03_开局大全\\中炮局\\中炮对单提马");
//        List<ChessLeaf> chessLeaves =chessParser. parseTextDir(start);

        List<String> jsonFiles = Arrays.asList(
                "中炮局/中炮对单提马横车.json",
                "中炮局/中炮对单提马.json",
                "中炮局/中炮对龟背炮.json",
                "中炮局/中炮对右三步虎.json",
                "中炮局/中炮对进右马.json",
                "中炮局/中炮对进右马先上士.json",
                "中炮局/中炮对鸳鸯炮.json",
                "中炮局/中炮对进左马.json",
                "中炮局/中炮对左炮封车.json",
                "中炮局/中炮对士角炮转单提马.json",
                "中炮局/中炮进七兵对单提马横车.json",
                "中炮局/中炮对左三步虎.json",
                "中炮局/中炮右横车对左三步虎.json",
                "中炮局/中炮巡河炮对左三步虎.json",
                "中炮局/中炮过河炮对左三步虎.json"
//                "中炮局/中炮两头蛇对左三步虎.json",
        );

        String parent = "D:\\git.python\\chess_spider\\棋谱";
        for (String jsonFile : jsonFiles) {
            List<ChessLeaf> chessLeaves = chessParser.parseJsonFile(new File(parent, jsonFile));
//            String prefix = FileUtil.getPrefix(jsonFile);
//            String title = prefix.replace("-", "/");
            System.out.println("jsonFile = " + jsonFile);
            chessParser.output(jsonFile, chessLeaves);
        }
    }

    public List<ChessLeaf> parseJsonFile(File file) throws IOException {
        List<ChessGame> games = mapper.readValue(file, new TypeReference<>() {
        });
        return games.stream()
                .map(game -> new ChessLeaf(game.getTitle(), game.getFen(), game.getMoves()))
                .collect(Collectors.toList());
    }

    private void output(String jsonFile, List<ChessLeaf> chessLeaves) throws IOException {
        String output = chessLeaves.stream()
                .sorted(Comparator.comparing(ChessLeaf::getTitle))
                .map(this::toJson)
                .collect(Collectors.joining(
                        ",\n",
                        """
                                [
                                """,
                        """                                
                                
                                ]"""));

        Path chessDir = Paths.get(System.getProperty("java.io.tmpdir"), "chess");
        // 在该目录下生成一个临时文件，前缀为 "game"，后缀为 ".tmp"
        Path tempFile = chessDir.resolve(jsonFile);
        Files.createDirectories(tempFile.getParent());
        Files.writeString(tempFile, output);
        System.out.println(tempFile.toUri().toURL());
    }

    @NonNull
    private List<ChessLeaf> parseTextDir(Path start) throws IOException {
        return Files.walk(start)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .map(path -> {
                    try {
                        return processTextFile(path.toFile());
                    } catch (IOException e) {
                        log.error("Error processing file: {}", path, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static ChessLeaf processTextFile(File file) throws IOException {
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
                    if ("#".equals(fen)) {
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
            return new ChessLeaf(title, fen, moves);
        }
        return null;
    }

    private String toJson(ChessLeaf chessLeaf) {
        try {
            return mapper.writeValueAsString(chessLeaf);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
