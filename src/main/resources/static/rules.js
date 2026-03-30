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

    function clonePieces(pieces) {
        return pieces.map(piece => ({
            code: piece.code,
            row: piece.row,
            col: piece.col
        }));
    }

    function applyMove(piece, from, to, pieces) {
        const nextPieces = clonePieces(pieces)
            .filter(current => current.row !== to.row || current.col !== to.col);

        const movingPiece = nextPieces.find(current =>
            current.row === from.row && current.col === from.col && current.code === piece.code
        );

        if (!movingPiece) {
            return nextPieces;
        }

        movingPiece.row = to.row;
        movingPiece.col = to.col;
        return nextPieces;
    }

    function findKing(pieces, redSide) {
        const targetCode = redSide ? "K" : "k";
        return pieces.find(piece => piece.code === targetCode) || null;
    }

    function canRookAttack(piece, targetRow, targetCol, boardMap) {
        if (piece.row !== targetRow && piece.col !== targetCol) {
            return false;
        }
        return countPiecesBetween(piece, { row: targetRow, col: targetCol }, boardMap) === 0;
    }

    function canCannonAttack(piece, targetRow, targetCol, boardMap) {
        if (piece.row !== targetRow && piece.col !== targetCol) {
            return false;
        }
        return countPiecesBetween(piece, { row: targetRow, col: targetCol }, boardMap) === 1;
    }

    function canKnightAttack(piece, targetRow, targetCol, boardMap) {
        const rowDelta = targetRow - piece.row;
        const colDelta = targetCol - piece.col;
        const absRow = Math.abs(rowDelta);
        const absCol = Math.abs(colDelta);
        if (!((absRow === 2 && absCol === 1) || (absRow === 1 && absCol === 2))) {
            return false;
        }

        const legRow = absRow === 2 ? piece.row + rowDelta / 2 : piece.row;
        const legCol = absCol === 2 ? piece.col + colDelta / 2 : piece.col;
        return !getPieceAt(boardMap, legRow, legCol);
    }

    function canPawnAttack(piece, targetRow, targetCol) {
        const rowDelta = targetRow - piece.row;
        const colDelta = targetCol - piece.col;
        const forwardStep = isRedPiece(piece) ? -1 : 1;

        if (rowDelta === forwardStep && colDelta === 0) {
            return true;
        }

        return hasCrossedRiver(piece, piece.row) && rowDelta === 0 && Math.abs(colDelta) === 1;
    }

    function canKingAttack(piece, targetRow, targetCol) {
        return isInsidePalace(piece, targetRow, targetCol)
            && Math.abs(targetRow - piece.row) + Math.abs(targetCol - piece.col) === 1;
    }

    function isFlyingGeneral(pieces) {
        const redKing = findKing(pieces, true);
        const blackKing = findKing(pieces, false);
        if (!redKing || !blackKing || redKing.col !== blackKing.col) {
            return false;
        }

        const minRow = Math.min(redKing.row, blackKing.row) + 1;
        const maxRow = Math.max(redKing.row, blackKing.row);
        return !pieces.some(piece =>
            piece.col === redKing.col && piece.row >= minRow && piece.row < maxRow
        );
    }

    function isSquareAttacked(row, col, attackerRed, pieces) {
        const boardMap = buildBoardMap(pieces);
        return pieces.some(piece => {
            if (isRedPiece(piece) !== attackerRed) {
                return false;
            }

            switch (piece.code.toUpperCase()) {
                case "R":
                    return canRookAttack(piece, row, col, boardMap);
                case "N":
                    return canKnightAttack(piece, row, col, boardMap);
                case "C":
                    return canCannonAttack(piece, row, col, boardMap);
                case "P":
                    return canPawnAttack(piece, row, col);
                case "K":
                    return canKingAttack(piece, row, col);
                default:
                    return false;
            }
        });
    }

    function isInCheck(redSide, pieces) {
        const king = findKing(pieces, redSide);
        if (!king) {
            return false;
        }

        if (isFlyingGeneral(pieces)) {
            return true;
        }

        return isSquareAttacked(king.row, king.col, !redSide, pieces);
    }

    function isLegalPositionAfterMove(piece, from, to, pieces) {
        const movingRed = isRedPiece(piece);
        const wasInCheck = isInCheck(movingRed, pieces);
        const nextPieces = applyMove(piece, from, to, pieces);

        if (isFlyingGeneral(nextPieces)) {
            return invalid("将帅不能照面");
        }

        if (isInCheck(movingRed, nextPieces)) {
            return wasInCheck
                ? invalid("当前正在被将军，必须先解将")
                : invalid("不能走成送将局面");
        }

        return valid();
    }

    function isLegalMove(piece, from, to, pieces) {
        const boardMap = buildBoardMap(pieces);
        let basicResult = validateBasicMove(piece, from, to, boardMap);
        if (!basicResult.ok) {
            return basicResult;
        }

        const target = getPieceAt(boardMap, to.row, to.col);
        switch (piece.code.toUpperCase()) {
            case "R":
                basicResult = isLegalRookMove(from, to, boardMap);
                break;
            case "N":
                basicResult = isLegalKnightMove(from, to, boardMap);
                break;
            case "B":
                basicResult = isLegalBishopMove(piece, from, to, boardMap);
                break;
            case "A":
                basicResult = isLegalAdvisorMove(piece, from, to);
                break;
            case "K":
                basicResult = isLegalKingMove(piece, from, to);
                break;
            case "C":
                basicResult = isLegalCannonMove(from, to, boardMap, target);
                break;
            case "P":
                basicResult = isLegalPawnMove(piece, from, to);
                break;
            default:
                return invalid("暂不支持这个棋子的走法");
        }

        if (!basicResult.ok) {
            return basicResult;
        }

        return isLegalPositionAfterMove(piece, from, to, pieces);
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
        isInCheck,
        buildBoardMap,
        isSameSide,
        getLegalMoves
    };
})();
