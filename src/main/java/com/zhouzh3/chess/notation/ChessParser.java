package com.zhouzh3.chess.notation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author haig
 */
@Slf4j
public class ChessParser {

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
                "中炮局/中炮过河炮对左三步虎.json",
                "中炮局/中炮两头蛇对左三步虎.json",
                "中炮局/中炮对反宫马后补左马.json",
                "中炮局/中炮对反宫马.json",
                "中炮局/中炮急进左马对反宫马.json",
                "中炮局/中炮过河车对反宫马.json"

        );

        String parent = "D:\\git.python\\chess_spider\\棋谱";
        for (String jsonFile : jsonFiles) {
            List<ChessLeaf> chessLeaves = chessParser.parseJsonFile(new File(parent, jsonFile));
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

    private String toJson(ChessLeaf chessLeaf) {
        try {
            return mapper.writeValueAsString(chessLeaf);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
