package com.zhouzh3.chess.notation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 棋谱叶子节点，包含完整字段
 * @author haig
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChessGame {
    private String uuid;
    private String fen;
    private String moves;
    private String opening;
    private String result;
    private String plycount;
    private String site;


    @JsonProperty("red_team")
    private String redTeam;
    private String red;
    private String round;

    @JsonProperty("black_team")
    private String blackTeam;
    private String black;

    @JsonProperty("日期")
    private String date;

    private String title;
    private String event;
    private String tongkaiju;
    private String tongqishou;
    private String tongsaishi;
    private String tuwen;

    @JsonProperty("has_png")
    private String hasPng;

    @JsonProperty("field_type")
    private String fieldType;
    private String count;

    @JsonProperty("comment_count")
    private String commentCount;

    private String uid;
    private String name;
    private String fen2;
}
