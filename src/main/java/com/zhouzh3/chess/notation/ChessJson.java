package com.zhouzh3.chess.notation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author haig
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChessJson {
    private String title;
    private String url;
    private String uuid;

    @JsonProperty("json_data")
    private List<ChessGame> jsonData;
}