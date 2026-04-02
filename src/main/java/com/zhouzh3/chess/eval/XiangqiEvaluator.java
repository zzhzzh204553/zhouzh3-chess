package com.zhouzh3.chess.eval;

import java.util.List;

public final class XiangqiEvaluator {

    private final FenParser fenParser = new FenParser();

    public EvalResult evaluate(String fen) {
        ParsedPosition position = fenParser.parse(fen);
        EvalContext context = buildContext(position);

        int redScore = evaluateSide(context, true);
        int blackScore = evaluateSide(context, false);

        return new EvalResult(redScore, blackScore, position.sideToMove());
    }

    private EvalContext buildContext(ParsedPosition position) {
        EvalContext context = new EvalContext(position);
        char[][] board = position.board();

        for (int row = 0; row < ParsedPosition.ROWS; row++) {
            for (int col = 0; col < ParsedPosition.COLS; col++) {
                char piece = board[row][col];
                if (piece == '.') {
                    continue;
                }

                PieceState pieceState = new PieceState(row, col, piece);
                boolean red = isRed(piece);
                if (red) {
                    context.getRedPieces().add(pieceState);
                } else {
                    context.getBlackPieces().add(pieceState);
                }

                switch (piece) {
                    case 'K':
                        context.setRedKing(new int[]{row, col});
                        break;
                    case 'k':
                        context.setBlackKing(new int[]{row, col});
                        break;
                    case 'A':
                        context.setRedAdvisors(context.getRedAdvisors() + 1);
                        break;
                    case 'a':
                        context.setBlackAdvisors(context.getBlackAdvisors() + 1);
                        break;
                    case 'B':
                        context.setRedBishops(context.getRedBishops() + 1);
                        break;
                    case 'b':
                        context.setBlackBishops(context.getBlackBishops() + 1);
                        break;
                    default:
                        break;
                }
            }
        }

        return context;
    }

    private int evaluateSide(EvalContext context, boolean red) {
        int score = 0;
        ParsedPosition position = context.getPosition();
        List<PieceState> pieces = red ? context.getRedPieces() : context.getBlackPieces();

        for (PieceState pieceState : pieces) {
            char piece = pieceState.piece();
            int row = pieceState.row();
            int col = pieceState.col();

            score += materialValue(piece);
            score += positionalBonus(piece, row, col);
            score += mobilityBonus(position, row, col, piece);
            score += attackDefenseBonus(position, row, col, piece);
        }

        score += kingSafetyBonus(context, red);
        score += structureBonus(context, red);
        return score;
    }

    private int materialValue(char piece) {
        return switch (Character.toUpperCase(piece)) {
            case 'K' -> 10000;
            case 'R' -> 600;
            case 'N' -> 270;
            case 'C' -> 285;
            case 'B', 'A' -> 120;
            case 'P' -> 70;
            default -> throw new IllegalArgumentException("Unknown piece: " + piece);
        };
    }

    private int positionalBonus(char piece, int row, int col) {
        return switch (Character.toUpperCase(piece)) {
            case 'P' -> pawnBonus(piece, row, col);
            case 'N' -> knightBonus(row, col);
            case 'R' -> rookBonus(row, col);
            case 'C' -> cannonBonus(row, col);
            case 'K' -> kingPositionBonus(piece, row, col);
            default -> 0;
        };
    }

    private int mobilityBonus(ParsedPosition position, int row, int col, char piece) {
        int moveCount = generatePseudoLegalMoves(position, row, col, piece);

        return switch (Character.toUpperCase(piece)) {
            case 'R', 'C' -> moveCount * 3;
            case 'N' -> moveCount * 4;
            case 'P' -> moveCount * 2;
            case 'A', 'B', 'K' -> moveCount;
            default -> 0;
        };
    }

    private int attackDefenseBonus(ParsedPosition position, int row, int col, char piece) {
        int attackers = countAttackers(position, row, col, !isRed(piece));
        int defenders = countAttackers(position, row, col, isRed(piece));

        int bonus = 0;
        int pieceValue = materialValue(piece);

        if (defenders > 0) {
            bonus += Math.min(20, defenders * 8);
        }

        if (attackers > 0) {
            bonus -= Math.min(24, attackers * 10);
        }

        if (attackers > defenders) {
            bonus -= Math.max(10, pieceValue / 20);
        }

        if (defenders > attackers) {
            bonus += Math.max(6, pieceValue / 40);
        }

        return bonus;
    }

    private int kingSafetyBonus(EvalContext context, boolean red) {
        ParsedPosition position = context.getPosition();
        int advisors = red ? context.getRedAdvisors() : context.getBlackAdvisors();
        int bishops = red ? context.getRedBishops() : context.getBlackBishops();
        int[] king = red ? context.getRedKing() : context.getBlackKing();

        if (king == null) {
            return -100000;
        }

        int score = 0;
        score += advisors * 15;
        score += bishops * 15;

        if (isInPalace(king[0], king[1], red)) {
            score += 20;
        } else {
            score -= 30;
        }

        if (hasFlyingGeneral(context)) {
            score -= 80;
        }

        int enemyAttackersNearKing = countEnemyAttackersNearKing(position, red, king[0], king[1]);
        score -= enemyAttackersNearKing * 12;

        return score;
    }

    private int structureBonus(EvalContext context, boolean red) {
        int score = 0;
        char pawn = red ? 'P' : 'p';
        ParsedPosition position = context.getPosition();
        List<PieceState> pieces = red ? context.getRedPieces() : context.getBlackPieces();

        for (PieceState pieceState : pieces) {
            if (pieceState.piece() != pawn) {
                continue;
            }

            int row = pieceState.row();
            int col = pieceState.col();

            if (hasAdjacentFriendlyPawn(position, row, col, pawn)) {
                score += 12;
            }

            if (isAdvancedPawn(row, red)) {
                score += 10;
            }
        }

        return score;
    }

    private int pawnBonus(char piece, int row, int col) {
        boolean red = isRed(piece);
        int bonus = 0;

        if (red) {
            if (row <= 4) {
                bonus += 30;
            }
            if (row <= 2) {
                bonus += 20;
            }
        } else {
            if (row >= 5) {
                bonus += 30;
            }
            if (row >= 7) {
                bonus += 20;
            }
        }

        if (col >= 3 && col <= 5) {
            bonus += 8;
        }

        return bonus;
    }

    private int knightBonus(int row, int col) {
        int centerDistance = Math.abs(4 - col) + Math.abs(4 - row);
        return Math.max(0, 18 - centerDistance * 3);
    }

    private int rookBonus(int row, int col) {
        int bonus = 0;
        if (col >= 2 && col <= 6) {
            bonus += 8;
        }
        if (row >= 2 && row <= 7) {
            bonus += 8;
        }
        return bonus;
    }

    private int cannonBonus(int row, int col) {
        int bonus = 0;
        if (col >= 2 && col <= 6) {
            bonus += 6;
        }
        if (row >= 2 && row <= 7) {
            bonus += 6;
        }
        return bonus;
    }

    private int kingPositionBonus(char piece, int row, int col) {
        return isInPalace(row, col, isRed(piece)) ? 10 : -20;
    }

    private int generatePseudoLegalMoves(ParsedPosition position, int row, int col, char piece) {
        return switch (Character.toUpperCase(piece)) {
            case 'R' -> rookMoves(position, row, col, piece);
            case 'N' -> knightMoves(position, row, col, piece);
            case 'C' -> cannonMoves(position, row, col, piece);
            case 'P' -> pawnMoves(position, row, col, piece);
            case 'A' -> advisorMoves(position, row, col, piece);
            case 'B' -> bishopMoves(position, row, col, piece);
            case 'K' -> kingMoves(position, row, col, piece);
            default -> 0;
        };
    }

    private int rookMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        for (int[] dir : directions) {
            int nextCol = col + dir[0];
            int nextRow = row + dir[1];

            while (position.isInside(nextRow, nextCol)) {
                char target = position.getPiece(nextRow, nextCol);
                if (target == '.') {
                    count++;
                } else {
                    if (isRed(target) != isRed(piece)) {
                        count++;
                    }
                    break;
                }

                nextCol += dir[0];
                nextRow += dir[1];
            }
        }

        return count;
    }

    private int cannonMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        for (int[] dir : directions) {
            int nextCol = col + dir[0];
            int nextRow = row + dir[1];
            boolean jumped = false;

            while (position.isInside(nextRow, nextCol)) {
                char target = position.getPiece(nextRow, nextCol);

                if (!jumped) {
                    if (target == '.') {
                        count++;
                    } else {
                        jumped = true;
                    }
                } else {
                    if (target != '.') {
                        if (isRed(target) != isRed(piece)) {
                            count++;
                        }
                        break;
                    }
                }

                nextCol += dir[0];
                nextRow += dir[1];
            }
        }

        return count;
    }

    private int knightMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;

        int[][] patterns = {
                {0, 1, -1, 2}, {0, 1, 1, 2},
                {0, -1, -1, -2}, {0, -1, 1, -2},
                {1, 0, 2, -1}, {1, 0, 2, 1},
                {-1, 0, -2, -1}, {-1, 0, -2, 1}
        };

        for (int[] pattern : patterns) {
            int legCol = col + pattern[0];
            int legRow = row + pattern[1];
            int targetCol = col + pattern[2];
            int targetRow = row + pattern[3];

            if (!position.isInside(legRow, legCol) || !position.isInside(targetRow, targetCol)) {
                continue;
            }

            if (position.getPiece(legRow, legCol) != '.') {
                continue;
            }

            char target = position.getPiece(targetRow, targetCol);
            if (target == '.' || isRed(target) != isRed(piece)) {
                count++;
            }
        }

        return count;
    }

    private int pawnMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        boolean red = isRed(piece);

        int forwardRow = red ? row - 1 : row + 1;
        if (position.isInside(forwardRow, col) && canMoveTo(position.getPiece(forwardRow, col), piece)) {
            count++;
        }

        boolean crossedRiver = red ? row <= 4 : row >= 5;
        if (crossedRiver) {
            int leftCol = col - 1;
            int rightCol = col + 1;

            if (position.isInside(row, leftCol) && canMoveTo(position.getPiece(row, leftCol), piece)) {
                count++;
            }
            if (position.isInside(row, rightCol) && canMoveTo(position.getPiece(row, rightCol), piece)) {
                count++;
            }
        }

        return count;
    }

    private int advisorMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        int[][] directions = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] dir : directions) {
            int nextCol = col + dir[0];
            int nextRow = row + dir[1];

            if (!position.isInside(nextRow, nextCol)) {
                continue;
            }
            if (!isInPalace(nextRow, nextCol, isRed(piece))) {
                continue;
            }

            char target = position.getPiece(nextRow, nextCol);
            if (target == '.' || isRed(target) != isRed(piece)) {
                count++;
            }
        }

        return count;
    }

    private int bishopMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        boolean red = isRed(piece);

        int[][] directions = {
                {2, 2}, {2, -2}, {-2, 2}, {-2, -2}
        };

        for (int[] dir : directions) {
            int nextCol = col + dir[0];
            int nextRow = row + dir[1];
            int eyeCol = col + dir[0] / 2;
            int eyeRow = row + dir[1] / 2;

            if (!position.isInside(nextRow, nextCol) || !position.isInside(eyeRow, eyeCol)) {
                continue;
            }
            if (position.getPiece(eyeRow, eyeCol) != '.') {
                continue;
            }
            if (red && nextRow < 5) {
                continue;
            }
            if (!red && nextRow > 4) {
                continue;
            }

            char target = position.getPiece(nextRow, nextCol);
            if (target == '.' || isRed(target) != isRed(piece)) {
                count++;
            }
        }

        return count;
    }

    private int kingMoves(ParsedPosition position, int row, int col, char piece) {
        int count = 0;
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        for (int[] dir : directions) {
            int nextCol = col + dir[0];
            int nextRow = row + dir[1];

            if (!position.isInside(nextRow, nextCol)) {
                continue;
            }
            if (!isInPalace(nextRow, nextCol, isRed(piece))) {
                continue;
            }

            char target = position.getPiece(nextRow, nextCol);
            if (target == '.' || isRed(target) != isRed(piece)) {
                count++;
            }
        }

        return count;
    }

    private int countAttackers(ParsedPosition position, int targetRow, int targetCol, boolean attackerRed) {
        int count = 0;
        char[][] board = position.board();

        for (int row = 0; row < ParsedPosition.ROWS; row++) {
            for (int col = 0; col < ParsedPosition.COLS; col++) {
                char piece = board[row][col];
                if (piece == '.' || isRed(piece) != attackerRed) {
                    continue;
                }

                if (attacksSquare(position, row, col, piece, targetRow, targetCol)) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean attacksSquare(ParsedPosition position, int row, int col, char piece, int targetRow, int targetCol) {
        if (row == targetRow && col == targetCol) {
            return false;
        }

        return switch (Character.toUpperCase(piece)) {
            case 'R' -> rookAttacks(position, row, col, targetRow, targetCol);
            case 'C' -> cannonAttacks(position, row, col, targetRow, targetCol);
            case 'N' -> knightAttacks(position, row, col, targetRow, targetCol);
            case 'P' -> pawnAttacks(piece, row, col, targetRow, targetCol);
            case 'A' -> advisorAttacks(piece, row, col, targetRow, targetCol);
            case 'B' -> bishopAttacks(position, piece, row, col, targetRow, targetCol);
            case 'K' -> kingAttacks(piece, row, col, targetRow, targetCol);
            default -> false;
        };
    }

    private boolean rookAttacks(ParsedPosition position, int row, int col, int targetRow, int targetCol) {
        if (row != targetRow && col != targetCol) {
            return false;
        }

        if (row == targetRow) {
            int step = targetCol > col ? 1 : -1;
            for (int c = col + step; c != targetCol; c += step) {
                if (position.getPiece(row, c) != '.') {
                    return false;
                }
            }
            return true;
        }

        int step = targetRow > row ? 1 : -1;
        for (int r = row + step; r != targetRow; r += step) {
            if (position.getPiece(r, col) != '.') {
                return false;
            }
        }
        return true;
    }

    private boolean cannonAttacks(ParsedPosition position, int row, int col, int targetRow, int targetCol) {
        if (row != targetRow && col != targetCol) {
            return false;
        }

        int blockers = 0;

        if (row == targetRow) {
            int step = targetCol > col ? 1 : -1;
            for (int c = col + step; c != targetCol; c += step) {
                if (position.getPiece(row, c) != '.') {
                    blockers++;
                }
            }
        } else {
            int step = targetRow > row ? 1 : -1;
            for (int r = row + step; r != targetRow; r += step) {
                if (position.getPiece(r, col) != '.') {
                    blockers++;
                }
            }
        }

        return blockers == 1;
    }

    private boolean knightAttacks(ParsedPosition position, int row, int col, int targetRow, int targetCol) {
        int rowDiff = targetRow - row;
        int colDiff = targetCol - col;

        if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1) {
            int legRow = row + rowDiff / 2;
            return position.getPiece(legRow, col) == '.';
        }

        if (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2) {
            int legCol = col + colDiff / 2;
            return position.getPiece(row, legCol) == '.';
        }

        return false;
    }

    private boolean pawnAttacks(char piece, int row, int col, int targetRow, int targetCol) {
        boolean red = isRed(piece);

        if (red) {
            if (targetRow == row - 1 && targetCol == col) {
                return true;
            }
            return row <= 4 && targetRow == row && Math.abs(targetCol - col) == 1;
        } else {
            if (targetRow == row + 1 && targetCol == col) {
                return true;
            }
            return row >= 5 && targetRow == row && Math.abs(targetCol - col) == 1;
        }
    }

    private boolean advisorAttacks(char piece, int row, int col, int targetRow, int targetCol) {
        return Math.abs(targetRow - row) == 1
                && Math.abs(targetCol - col) == 1
                && isInPalace(targetRow, targetCol, isRed(piece));
    }

    private boolean bishopAttacks(ParsedPosition position, char piece, int row, int col, int targetRow, int targetCol) {
        if (Math.abs(targetRow - row) != 2 || Math.abs(targetCol - col) != 2) {
            return false;
        }

        int eyeRow = (row + targetRow) / 2;
        int eyeCol = (col + targetCol) / 2;
        if (position.getPiece(eyeRow, eyeCol) != '.') {
            return false;
        }

        boolean red = isRed(piece);
        return (!red || targetRow >= 5) && (red || targetRow <= 4);
    }

    private boolean kingAttacks(char piece, int row, int col, int targetRow, int targetCol) {
        return Math.abs(targetRow - row) + Math.abs(targetCol - col) == 1
                && isInPalace(targetRow, targetCol, isRed(piece));
    }

    private boolean hasFlyingGeneral(EvalContext context) {
        ParsedPosition position = context.getPosition();
        int[] redKing = context.getRedKing();
        int[] blackKing = context.getBlackKing();

        if (redKing == null || blackKing == null) {
            return false;
        }
        if (redKing[1] != blackKing[1]) {
            return false;
        }

        int col = redKing[1];
        int start = Math.min(redKing[0], blackKing[0]) + 1;
        int end = Math.max(redKing[0], blackKing[0]);

        for (int row = start; row < end; row++) {
            if (position.getPiece(row, col) != '.') {
                return false;
            }
        }

        return true;
    }

    private int countEnemyAttackersNearKing(ParsedPosition position, boolean red, int kingRow, int kingCol) {
        int count = 0;

        for (int row = kingRow - 1; row <= kingRow + 1; row++) {
            for (int col = kingCol - 1; col <= kingCol + 1; col++) {
                if (!position.isInside(row, col)) {
                    continue;
                }
                count += countAttackers(position, row, col, !red);
            }
        }

        return count;
    }

    private boolean hasAdjacentFriendlyPawn(ParsedPosition position, int row, int col, char pawn) {
        int leftCol = col - 1;
        int rightCol = col + 1;

        return (position.isInside(row, leftCol) && position.getPiece(row, leftCol) == pawn)
                || (position.isInside(row, rightCol) && position.getPiece(row, rightCol) == pawn);
    }

    private boolean isAdvancedPawn(int row, boolean red) {
        return red ? row <= 4 : row >= 5;
    }

    private boolean canMoveTo(char target, char piece) {
        return target == '.' || isRed(target) != isRed(piece);
    }

    private boolean isInPalace(int row, int col, boolean red) {
        if (col < 3 || col > 5) {
            return false;
        }

        if (red) {
            return row >= 7 && row <= 9;
        } else {
            return row >= 0 && row <= 2;
        }
    }

    private boolean isRed(char piece) {
        return Character.isUpperCase(piece);
    }


}
