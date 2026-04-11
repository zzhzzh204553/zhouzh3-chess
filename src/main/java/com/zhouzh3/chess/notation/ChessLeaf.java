package com.zhouzh3.chess.notation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author haig
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChessLeaf {
    private String title;
    private String fen;
    private String moves;

}
