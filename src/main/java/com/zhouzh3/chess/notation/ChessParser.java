package com.zhouzh3.chess.notation;

import cn.hutool.core.io.FileUtil;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author haig
 */
@Slf4j
public class ChessParser {

    private static final String DEFAULT_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        ChessParser chessParser = new ChessParser();

//        List<ChessLeaf> chessLeaves =chessParser. parseTextDir(Paths.get("E:\\棋谱\\03_开局大全\\中炮局\\中炮对单提马"));
//        List<ChessLeaf> chessLeaves = chessParser.parseJsonFile(new File("D:\\git.python\\chess_spider\\中炮局-中炮对单提马.json"));
        List<ChessLeaf> chessLeaves = chessParser.parseJsonFile(new File("D:\\git.python\\chess_spider\\中炮局-中炮对单提马横车.json"));

        chessParser.output(chessLeaves);

    }

    public List<ChessLeaf> parseJsonFile(File file) throws IOException {
        List<ChessJson> games = mapper.readValue(file, new TypeReference<>() {
        });

        return games.stream()
                .map(obj -> {
                    String title = Objects.toString(obj.getTitle(), "");
                    String fen = DEFAULT_FEN;
                    String moves = "";

                    List<ChessGame> jsonData = obj.getJsonData();
                    if (!jsonData.isEmpty()) {
                        ChessGame first = jsonData.get(0);

                        fen = Objects.toString(first.getFen(), DEFAULT_FEN);
                        moves = Objects.toString(first.getMoves(), "");

                    }

//                    Map<String, String> result = new LinkedHashMap<>();
//                    result.put("title", title);
//                    result.put("fen", fen);
//                    result.put("moves", moves);
//                    return result;

                    return new ChessLeaf(title, fen, moves);
                })
                .collect(Collectors.toList());
    }

    private void output(List<ChessLeaf> chessLeaves) throws IOException {
        String output = chessLeaves.stream()
                .sorted((o1, o2) -> o1.getTitle().compareTo(o2.getTitle()))
                .map(this::toJson)
                .collect(Collectors.joining(",\n",
                        """
                                {
                                    title: "中炮对单提马",
                                    children: [""",
                        """
                                ]
                                """));

        Path chessDir = Paths.get(System.getProperty("java.io.tmpdir"), "chess");
        Files.createDirectories(chessDir);
        // 在该目录下生成一个临时文件，前缀为 "game"，后缀为 ".tmp"
        Path tempFile = Files.createTempFile(chessDir, "notation_", ".json");
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
//            Map<String, String> obj = new LinkedHashMap<>();
//            obj.put("title", title);
//            obj.put("fen", fen);
//            obj.put("moves", moves);
//            return obj;

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
