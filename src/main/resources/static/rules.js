(function () {
    function toKey(row, col) {
        return `${row},${col}`;
    }

    function buildBoardMap(pieces) {
        const boardMap = new Map();
        pieces.forEach(piece => {
            boardMap.set(toKey(piece.row, piece.col), piece);
        });
        return boardMap;
    }

    function getPieceAt(boardMap, row, col) {
        return boardMap.get(toKey(row, col)) || null;
    }

    function isInsideBoard(row, col) {
        return row >= 0 && row <= 9 && col >= 0 && col <= 8;
    }

    function isRedPiece(piece) {
        return piece.code === piece.code.toUpperCase();
    }

    function isSameSide(source, target) {
        return isRedPiece(source) === isRedPiece(target);
    }

    function countPiecesBetween(from, to, boardMap) {
        let count = 0;
        if (from.row === to.row) {
            const start = Math.min(from.col, to.col) + 1;
            const end = Math.max(from.col, to.col);
            for (let col = start; col < end; col += 1) {
                if (getPieceAt(boardMap, from.row, col)) {
                    count += 1;
                }
            }
            return count;
        }

        if (from.col === to.col) {
            const start = Math.min(from.row, to.row) + 1;
            const end = Math.max(from.row, to.row);
            for (let row = start; row < end; row += 1) {
                if (getPieceAt(boardMap, row, from.col)) {
                    count += 1;
                }
            }
            return count;
        }

        return -1;
    }

    function isInsidePalace(piece, row, col) {
        const top = isRedPiece(piece) ? 7 : 0;
        const bottom = isRedPiece(piece) ? 9 : 2;
        return row >= top && row <= bottom && col >= 3 && col <= 5;
    }

    function isForwardMove(piece, from, to) {
        return isRedPiece(piece) ? to.row === from.row - 1 : to.row === from.row + 1;
    }

    function hasCrossedRiver(piece, row) {
        return isRedPiece(piece) ? row <= 4 : row >= 5;
    }

    function invalid(message) {
        return { ok: false, message };
    }

    function valid() {
        return { ok: true };
    }

    function validateBasicMove(piece, from, to, boardMap) {
        if (!piece || !isInsideBoard(to.row, to.col)) {
            return invalid("目标位置超出棋盘范围");
        }
        if (from.row === to.row && from.col === to.col) {
            return invalid("请选择不同的落点");
        }

        const target = getPieceAt(boardMap, to.row, to.col);
        if (target && isSameSide(piece, target)) {
            return invalid("不能吃自己的棋子");
        }

        return valid();
    }

    function isLegalRookMove(from, to, boardMap) {
        if (from.row !== to.row && from.col !== to.col) {
            return invalid("车只能直线移动");
        }
        if (countPiecesBetween(from, to, boardMap) !== 0) {
            return invalid("车的路径上有棋子阻挡");
        }
        return valid();
    }

    function isLegalKnightMove(from, to, boardMap) {
        const rowDelta = to.row - from.row;
        const colDelta = to.col - from.col;
        const absRow = Math.abs(rowDelta);
        const absCol = Math.abs(colDelta);
        if (!((absRow === 2 && absCol === 1) || (absRow === 1 && absCol === 2))) {
            return invalid("马必须走日字");
        }

        const legRow = absRow === 2 ? from.row + rowDelta / 2 : from.row;
        const legCol = absCol === 2 ? from.col + colDelta / 2 : from.col;
        if (getPieceAt(boardMap, legRow, legCol)) {
            return invalid("马腿被蹩，不能这样走");
        }
        return valid();
    }

    function isLegalBishopMove(piece, from, to, boardMap) {
        const rowDelta = Math.abs(to.row - from.row);
        const colDelta = Math.abs(to.col - from.col);
        if (rowDelta !== 2 || colDelta !== 2) {
            return invalid("相或象必须走田字");
        }

        if (isRedPiece(piece) && to.row < 5) {
            return invalid("红相不能过河");
        }
        if (!isRedPiece(piece) && to.row > 4) {
            return invalid("黑象不能过河");
        }

        const eyeRow = (from.row + to.row) / 2;
        const eyeCol = (from.col + to.col) / 2;
        if (getPieceAt(boardMap, eyeRow, eyeCol)) {
            return invalid("象眼被堵住了");
        }
        return valid();
    }

    function isLegalAdvisorMove(piece, from, to) {
        const rowDelta = Math.abs(to.row - from.row);
        const colDelta = Math.abs(to.col - from.col);
        if (rowDelta !== 1 || colDelta !== 1) {
            return invalid("仕或士只能斜走一步");
        }
        if (!isInsidePalace(piece, to.row, to.col)) {
            return invalid("仕或士不能离开九宫");
        }
        return valid();
    }

    function isLegalKingMove(piece, from, to) {
        const rowDelta = Math.abs(to.row - from.row);
        const colDelta = Math.abs(to.col - from.col);
        if (rowDelta + colDelta !== 1) {
            return invalid("将或帅只能直走一步");
        }
        if (!isInsidePalace(piece, to.row, to.col)) {
            return invalid("将或帅不能离开九宫");
        }
        return valid();
    }

    function isLegalCannonMove(from, to, boardMap, target) {
        if (from.row !== to.row && from.col !== to.col) {
            return invalid("炮只能直线移动");
        }

        const betweenCount = countPiecesBetween(from, to, boardMap);
        if (target) {
            if (betweenCount !== 1) {
                return invalid("炮吃子时必须隔一个棋子");
            }
            return valid();
        }
        if (betweenCount !== 0) {
            return invalid("炮平移时路径上不能有棋子");
        }
        return valid();
    }

    function isLegalPawnMove(piece, from, to) {
        const rowDelta = to.row - from.row;
        const colDelta = to.col - from.col;
        const absCol = Math.abs(colDelta);
        const crossedRiver = hasCrossedRiver(piece, from.row);

        if (absCol > 1) {
            return invalid("兵或卒不能这样走");
        }

        if (isForwardMove(piece, from, to) && colDelta === 0) {
            return valid();
        }

        if (crossedRiver && rowDelta === 0 && absCol === 1) {
            return valid();
        }

        return invalid("兵或卒只能前进，过河后才能横走");
    }

    function isLegalMove(piece, from, to, pieces) {
        const boardMap = buildBoardMap(pieces);
        const basicResult = validateBasicMove(piece, from, to, boardMap);
        if (!basicResult.ok) {
            return basicResult;
        }

        const target = getPieceAt(boardMap, to.row, to.col);
        switch (piece.code.toUpperCase()) {
            case "R":
                return isLegalRookMove(from, to, boardMap);
            case "N":
                return isLegalKnightMove(from, to, boardMap);
            case "B":
                return isLegalBishopMove(piece, from, to, boardMap);
            case "A":
                return isLegalAdvisorMove(piece, from, to);
            case "K":
                return isLegalKingMove(piece, from, to);
            case "C":
                return isLegalCannonMove(from, to, boardMap, target);
            case "P":
                return isLegalPawnMove(piece, from, to);
            default:
                return invalid("暂不支持这个棋子的走法");
        }
    }

    function addCandidate(candidates, row, col) {
        if (isInsideBoard(row, col)) {
            candidates.push({ row, col });
        }
    }

    function getKnightCandidates(piece) {
        const candidates = [];
        const offsets = [
            [-2, -1], [-2, 1], [-1, -2], [-1, 2],
            [1, -2], [1, 2], [2, -1], [2, 1]
        ];
        offsets.forEach(([rowOffset, colOffset]) => {
            addCandidate(candidates, piece.row + rowOffset, piece.col + colOffset);
        });
        return candidates;
    }

    function getBishopCandidates(piece) {
        const candidates = [];
        const offsets = [[-2, -2], [-2, 2], [2, -2], [2, 2]];
        offsets.forEach(([rowOffset, colOffset]) => {
            addCandidate(candidates, piece.row + rowOffset, piece.col + colOffset);
        });
        return candidates;
    }

    function getAdvisorCandidates(piece) {
        const candidates = [];
        const offsets = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
        offsets.forEach(([rowOffset, colOffset]) => {
            addCandidate(candidates, piece.row + rowOffset, piece.col + colOffset);
        });
        return candidates;
    }

    function getKingCandidates(piece) {
        const candidates = [];
        const offsets = [[-1, 0], [1, 0], [0, -1], [0, 1]];
        offsets.forEach(([rowOffset, colOffset]) => {
            addCandidate(candidates, piece.row + rowOffset, piece.col + colOffset);
        });
        return candidates;
    }

    function getPawnCandidates(piece) {
        const candidates = [];
        const forwardOffset = isRedPiece(piece) ? -1 : 1;
        addCandidate(candidates, piece.row + forwardOffset, piece.col);
        if (hasCrossedRiver(piece, piece.row)) {
            addCandidate(candidates, piece.row, piece.col - 1);
            addCandidate(candidates, piece.row, piece.col + 1);
        }
        return candidates;
    }

    function collectDirectionalMoves(piece, boardMap, options = {}) {
        const candidates = [];
        const directions = [
            [-1, 0], [1, 0], [0, -1], [0, 1]
        ];

        directions.forEach(([rowStep, colStep]) => {
            let row = piece.row + rowStep;
            let col = piece.col + colStep;
            let seenScreen = false;

            while (isInsideBoard(row, col)) {
                const target = getPieceAt(boardMap, row, col);
                if (!options.cannon) {
                    candidates.push({ row, col });
                    if (target) {
                        break;
                    }
                } else if (!seenScreen) {
                    if (!target) {
                        candidates.push({ row, col });
                    } else {
                        seenScreen = true;
                    }
                } else if (target) {
                    candidates.push({ row, col });
                    break;
                }

                row += rowStep;
                col += colStep;
            }
        });

        return candidates;
    }

    function getCandidateMoves(piece, pieces) {
        const boardMap = buildBoardMap(pieces);
        switch (piece.code.toUpperCase()) {
            case "R":
                return collectDirectionalMoves(piece, boardMap);
            case "N":
                return getKnightCandidates(piece);
            case "B":
                return getBishopCandidates(piece);
            case "A":
                return getAdvisorCandidates(piece);
            case "K":
                return getKingCandidates(piece);
            case "C":
                return collectDirectionalMoves(piece, boardMap, { cannon: true });
            case "P":
                return getPawnCandidates(piece);
            default:
                return [];
        }
    }

    function getLegalMoves(piece, pieces) {
        const boardMap = buildBoardMap(pieces);
        return getCandidateMoves(piece, pieces)
            .map(to => {
                const result = isLegalMove(piece, { row: piece.row, col: piece.col }, to, pieces);
                if (!result.ok) {
                    return null;
                }
                return {
                    row: to.row,
                    col: to.col,
                    capture: Boolean(getPieceAt(boardMap, to.row, to.col))
                };
            })
            .filter(Boolean);
    }

    window.ChessRules = {
        isLegalMove,
        buildBoardMap,
        isSameSide,
        getLegalMoves
    };
})();
