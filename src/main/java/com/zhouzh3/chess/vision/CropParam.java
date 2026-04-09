package com.zhouzh3.chess.vision;

import java.util.List;

/**
 * 不可变的 CropParam
 * @author haig
 */
public record CropParam(Region chessBoardRegion, List<List<Region>> chessPieceGrid) {

    public CropParam {
        // 对外层 List 做不可变拷贝
        chessPieceGrid = chessPieceGrid.stream()
                // 对每个内层 List 也做不可变拷贝
                .map(List::copyOf)
                .toList();
    }

    /**
     * 获取指定行列的棋子区域
     */
    public Region getChessPieceRegion(int row, int col) {
        if (row < 0 || row >= chessPieceGrid.size()) {
            throw new IndexOutOfBoundsException("行索引超出范围: " + row);
        }
        List<Region> rowList = chessPieceGrid.get(row);
        if (col < 0 || col >= rowList.size()) {
            throw new IndexOutOfBoundsException("列索引超出范围: " + col);
        }
        return rowList.get(col);
    }
}
