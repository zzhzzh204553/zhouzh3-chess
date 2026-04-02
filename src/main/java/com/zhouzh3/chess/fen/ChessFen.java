package com.zhouzh3.chess.fen;

public class ChessFen {
    private final Board board = new Board();
    private boolean redToMove = true;

    public void initBoard() {
        board.initBoard();
        redToMove = true;
    }

    public String toFen() {
        return board.toFen() + " " + (redToMove ? "w" : "b");
    }

    public void move(String notation, boolean isRed) {
        if (isRed != redToMove) {
            throw new IllegalStateException("当前走子方不匹配: notation=" + notation + ", redToMove=" + redToMove);
        }

        Move move = MoveParser.parse(notation);

        // 棋子类型映射
        char piece;
        switch (move.pieceStr) {
            case "兵":
                piece = 'P';
                break;
            case "卒":
                piece = 'p';
                isRed = false;
                break;
            case "炮":
                piece = isRed ? 'C' : 'c';
                break;
            case "车":
                piece = isRed ? 'R' : 'r';
                break;
            case "马":
                piece = isRed ? 'N' : 'n';
                break;
            case "相":
                piece = 'B';
                break;
            case "象":
                piece = 'b';
                isRed = false;
                break;
            case "仕":
                piece = 'A';
                break;
            case "士":
                piece = 'a';
                isRed = false;
                break;
            case "帅":
                piece = 'K';
                break;
            case "将":
                piece = 'k';
                isRed = false;
                break;
            default:
                throw new IllegalArgumentException("未知棋子: " + move.pieceStr);
        }

        // 找到棋子位置
        int startRow = -1;
        int startCol = fileToBoardCol(move.col, isRed);
        char[][] b = board.getBoard();
        for (int i = 0; i < 10; i++) {
            if (b[i][startCol] == piece) {
                startRow = i;
                break;
            }
        }
        if (startRow == -1) {
            throw new IllegalStateException("未找到棋子: " + notation);
        }

        int newRow = startRow, newCol = startCol;

        // 根据棋子类型和规则计算目标位置
        if (piece == 'N' || piece == 'n') {
            // 马走日字
            int targetCol = fileToBoardCol(move.target, isRed);
            int colDelta = Math.abs(targetCol - startCol);
            int rowDelta;
            if (colDelta == 1) {
                rowDelta = 2;
            } else if (colDelta == 2) {
                rowDelta = 1;
            } else {
                throw new IllegalStateException("马的目标列不合法: " + notation);
            }

            if ("进".equals(move.action)) {
                newRow += isRed ? -rowDelta : rowDelta;
                newCol = targetCol;
            } else if ("退".equals(move.action)) {
                newRow += isRed ? rowDelta : -rowDelta;
                newCol = targetCol;
            }
            // 蹩马腿检查
            int legRow;
            int legCol;
            if (rowDelta == 2) {
                legRow = (newRow + startRow) / 2;
                legCol = startCol;
            } else {
                legRow = startRow;
                legCol = (newCol + startCol) / 2;
            }
            if (b[legRow][legCol] != '.') {
                throw new IllegalStateException("蹩马腿: " + notation);
            }
        } else if (piece == 'B' || piece == 'b') {
            // 相/象斜走
            if ("进".equals(move.action)) {
                newRow += isRed ? -2 : 2;
                newCol = fileToBoardCol(move.target, isRed);
            } else if ("退".equals(move.action)) {
                newRow += isRed ? 2 : -2;
                newCol = fileToBoardCol(move.target, isRed);
            }
            if (isRed && newRow < 5) {
                throw new IllegalStateException("红相不能过河: " + notation);
            }
            if (!isRed && newRow > 4) {
                throw new IllegalStateException("黑象不能过河: " + notation);
            }
            int eyeRow = (startRow + newRow) / 2;
            int eyeCol = (startCol + newCol) / 2;
            if (b[eyeRow][eyeCol] != '.') {
                throw new IllegalStateException("塞象眼: " + notation);
            }

        } else if (piece == 'A' || piece == 'a') {
            // 仕/士斜走
            if ("进".equals(move.action)) {
                newRow += isRed ? -1 : 1;
                newCol = fileToBoardCol(move.target, isRed);
            } else if ("退".equals(move.action)) {
                newRow += isRed ? 1 : -1;
                newCol = fileToBoardCol(move.target, isRed);
            }
            if (!RuleEngine.inPalace(newRow, newCol, isRed)) {
                throw new IllegalStateException("仕/士必须在九宫格内: " + notation);
            }

        } else if (piece == 'C' || piece == 'c') {
            // 炮走法
            if ("进".equals(move.action)) {
                newRow += isRed ? -move.target : move.target;
            } else if ("退".equals(move.action)) {
                newRow += isRed ? move.target : -move.target;
            } else if ("平".equals(move.action)) {
                newCol = fileToBoardCol(move.target, isRed);
            }

            // 炮打隔子规则
            if (b[newRow][newCol] != '.') {
                int count = RuleEngine.countPiecesBetween(b, startRow, startCol, newRow, newCol);
                if (count != 1) {
                    throw new IllegalStateException("炮打隔子规则不符: " + notation);
                }
            } else {
                int count = RuleEngine.countPiecesBetween(b, startRow, startCol, newRow, newCol);
                if (count != 0) {
                    throw new IllegalStateException("炮走直线不能跨子: " + notation);
                }
            }

        } else if (piece == 'P' || piece == 'p') {
            // 兵/卒走法
            switch (move.action) {
                case "进" -> newRow += isRed ? -1 : 1;
                case "退" -> throw new IllegalStateException("兵/卒不能退: " + notation);
                case "平" -> {
                    if (isRed && startRow <= 4) {
                        newCol = fileToBoardCol(move.target, isRed);
                    } else if (!isRed && startRow >= 5) {
                        newCol = fileToBoardCol(move.target, isRed);
                    } else {
                        throw new IllegalStateException("兵/卒未过河不能横走: " + notation);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + move.action);
            }

        } else {
            // 车等直线走子
            switch (move.action) {
                case "进" -> newRow += isRed ? -move.target : move.target;
                case "退" -> newRow += isRed ? move.target : -move.target;
                case "平" -> newCol = fileToBoardCol(move.target, isRed);
                default -> throw new IllegalStateException("Unexpected value: " + move.action);
            }

            if (piece == 'R' || piece == 'r' || piece == 'K' || piece == 'k') {
                int count = RuleEngine.countPiecesBetween(b, startRow, startCol, newRow, newCol);
                if (count != 0) {
                    throw new IllegalStateException("直线走子不能跨子: " + notation);
                }
            }

            if (piece == 'K' || piece == 'k') {
                if (!RuleEngine.inPalace(newRow, newCol, isRed)) {
                    throw new IllegalStateException("帅/将必须在九宫格内: " + notation);
                }
            }
        }

        if (!RuleEngine.inBounds(newRow, newCol)) {
            throw new IllegalStateException("目标位置越界: " + notation);
        }
        if (RuleEngine.isSameSidePiece(b[newRow][newCol], isRed)) {
            throw new IllegalStateException("不能吃己方棋子: " + notation);
        }

        // 更新棋盘
        board.setPiece(newRow, newCol, piece);
        board.setPiece(startRow, startCol, '.');
        redToMove = !redToMove;
    }

    public void move(String notation) {
        boolean isRed = containsChineseNumber(notation) || notation.startsWith("兵") || notation.startsWith("相")
                || notation.startsWith("仕") || notation.startsWith("帅");
        move(notation, isRed);
    }

    private int fileToBoardCol(int file, boolean isRed) {
        return isRed ? 9 - file : file - 1;
    }

    private boolean containsChineseNumber(String notation) {
        return notation.matches(".*[一二三四五六七八九].*");
    }
}
