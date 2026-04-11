const pieceImages = {
    'R': 'images/red-ju.png',
    'N': 'images/red-ma.png',
    'B': 'images/red-xiang.png',
    'A': 'images/red-shi.png',
    'K': 'images/red-shuai.png',
    'C': 'images/red-pao.png',
    'P': 'images/red-bing.png',
    'r': 'images/black-ju.png',
    'n': 'images/black-ma.png',
    'b': 'images/black-xiang.png',
    'a': 'images/black-shi.png',
    'k': 'images/black-jiang.png',
    'c': 'images/black-pao.png',
    'p': 'images/black-zu.png'
};

const pieceNames = {
    'R': '红车',
    'N': '红马',
    'B': '红相',
    'A': '红仕',
    'K': '红帅',
    'C': '红炮',
    'P': '红兵',
    'r': '黑车',
    'n': '黑马',
    'b': '黑象',
    'a': '黑士',
    'k': '黑将',
    'c': '黑炮',
    'p': '黑卒'
};

const boardBase = {
    width: 570,
    height: 637,
    startX: 45,
    startY: 47,
    stepX: 60,
    stepY: 60
};

const fenInput = document.getElementById("fenInput");
const currentFenDisplay = document.getElementById("currentFenDisplay");
const movesInput = document.getElementById("movesInput");
const applyButton = document.getElementById("applyButton");
const cancelButton = document.getElementById("cancelButton");
const imageInput = document.getElementById("imageInput");
const imageApplyButton = document.getElementById("imageApplyButton");
const startButton = document.getElementById("startButton");
const undoButton = document.getElementById("undoButton");
const redoButton = document.getElementById("redoButton");
const endButton = document.getElementById("endButton");
const moveList = document.getElementById("moveList");
const board = document.getElementById("board");
const message = document.getElementById("message");
const treeList = document.getElementById("treeList");
const treeSearchInput = document.getElementById("treeSearchInput");

const collapsedTreeKeys = new Set();


let currentPieces = [];
let currentFenSuffix = "";
let originalFenInput = "";
let selectedPiece = null;
let lastMove = null;
let messageTimer = null;
let currentSide = "w";
let legalMoves = [];
let audioContext = null;
let historySnapshots = [];
let historyIndex = 0;
let moveHistory = [];
let pendingMoveAnimation = null;
let moveAnimationTimer = null;
let activeTreeKey = "";
let treeKeyword = "";


const defaultFen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w";

const chineseFiles = ["一", "二", "三", "四", "五", "六", "七", "八", "九"];
const pieceLabels = {
    R: "车",
    N: "马",
    B: "相",
    A: "仕",
    K: "帅",
    C: "炮",
    P: "兵",
    r: "车",
    n: "马",
    b: "象",
    a: "士",
    k: "将",
    c: "炮",
    p: "卒"
};

function normalizeSide(value) {
    if (!value) {
        return "w";
    }
    const side = value.toLowerCase();
    if (side === "b") {
        return "b";
    }
    return "w";
}

function getSideLabel(side) {
    return side === "b" ? "黑方" : "红方";
}

function isPieceOwnedByCurrentSide(piece) {
    const isRedPiece = piece.code === piece.code.toUpperCase();
    return currentSide === "w" ? isRedPiece : !isRedPiece;
}

function rebuildFenSuffix(side, suffix) {
    const parts = suffix ? suffix.split(/\s+/).filter(Boolean) : [];
    if (parts.length === 0) {
        return side;
    }
    parts[0] = side;
    return parts.join(" ");
}

function clonePieces(pieces) {
    return pieces.map(piece => ({
        code: piece.code,
        row: piece.row,
        col: piece.col
    }));
}

function cloneMove(move) {
    if (!move) {
        return null;
    }
    return {
        from: {...move.from},
        to: {...move.to}
    };
}

function createSnapshot() {
    return createSnapshotFromState(currentPieces, currentSide, currentFenSuffix, lastMove);
}

function createSnapshotFromState(pieces, side, fenSuffix, move) {
    return {
        pieces: clonePieces(pieces),
        side,
        fenSuffix,
        lastMove: cloneMove(move)
    };
}

function getFileLabel(col, isRed) {
    const file = isRed ? 9 - col : col + 1;
    return isRed ? chineseFiles[file - 1] : String(file);
}

function getMoveDistance(from, to) {
    return from.col === to.col
        ? Math.abs(to.row - from.row)
        : Math.abs(to.col - from.col);
}

function getMoveAction(piece, from, to) {
    if (from.row === to.row) {
        return "平";
    }
    const isRedPiece = piece.code === piece.code.toUpperCase();
    const forward = isRedPiece ? to.row < from.row : to.row > from.row;
    return forward ? "进" : "退";
}

function generateMoveNotation(piece, from, to) {
    const isRedPiece = piece.code === piece.code.toUpperCase();
    const pieceName = pieceLabels[piece.code] || piece.code;
    const action = getMoveAction(piece, from, to);
    const startFile = getFileLabel(from.col, isRedPiece);
    const targetFile = getFileLabel(to.col, isRedPiece);
    let targetText;

    switch (piece.code.toUpperCase()) {
        case "N":
        case "B":
        case "A":
            targetText = targetFile;
            break;
        case "R":
        case "C":
        case "K":
        case "P":
            targetText = action === "平"
                ? targetFile
                : (isRedPiece ? chineseFiles[getMoveDistance(from, to) - 1] : String(getMoveDistance(from, to)));
            break;
        default:
            targetText = action === "平" ? targetFile : String(getMoveDistance(from, to));
            break;
    }

    return `${pieceName}${startFile}${action}${targetText}`;
}

function syncFenInput() {
    fenInput.value = originalFenInput;
}

function getFenInputValue() {
    const fen = fenInput.value.trim() || defaultFen;
    fenInput.value = fen;
    return fen;
}

function syncCurrentFenDisplay() {
    currentFenDisplay.value = piecesToFen(currentPieces, currentFenSuffix);
}

function syncFenViews() {
    syncFenInput();
    syncCurrentFenDisplay();
}

function applyParsedFen(result) {
    currentPieces = result.pieces;
    currentSide = result.side;
    currentFenSuffix = rebuildFenSuffix(currentSide, result.suffix);
    legalMoves = [];
    setHistoryToCurrentState();
    syncFenViews();
    renderPieces(currentPieces);
    renderMoveHistory();
}

function splitMoveSequence(text) {
    const normalized = text.trim();
    if (!normalized) {
        return [];
    }
    if (normalized.length % 4 !== 0) {
        throw new Error("走子序列长度必须是 4 的倍数");
    }

    const moves = [];
    for (let i = 0; i < normalized.length; i += 4) {
        moves.push(normalized.slice(i, i + 4));
    }
    return moves;
}

function parseSquare(square) {
    if (!/^[a-i][0-9]$/i.test(square)) {
        throw new Error("坐标格式错误");
    }

    return {
        col: square.toLowerCase().charCodeAt(0) - 97,
        row: 9 - Number(square[1])
    };
}

function applyMoveToState(state, moveText, stepNumber) {
    const from = parseSquare(moveText.slice(0, 2));
    const to = parseSquare(moveText.slice(2, 4));
    const piece = state.pieces.find(current => current.row === from.row && current.col === from.col);

    if (!piece) {
        throw new Error(`第 ${stepNumber} 步起点没有棋子`);
    }

    const isRedPiece = piece.code === piece.code.toUpperCase();
    const movingSide = isRedPiece ? "w" : "b";
    if (movingSide !== state.side) {
        throw new Error(`第 ${stepNumber} 步非法：当前轮到${getSideLabel(state.side)}走棋`);
    }

    const moveResult = window.ChessRules.isLegalMove(piece, from, to, state.pieces);
    if (!moveResult.ok) {
        throw new Error(`第 ${stepNumber} 步非法：${moveResult.message}`);
    }

    const notation = generateMoveNotation(piece, from, to);
    const isCapture = state.pieces.some(current =>
        current !== piece && current.row === to.row && current.col === to.col
    );

    state.lastMove = {from, to};
    state.pieces = state.pieces.filter(current =>
        current === piece || current.row !== to.row || current.col !== to.col
    );
    piece.row = to.row;
    piece.col = to.col;
    state.side = state.side === "w" ? "b" : "w";
    state.fenSuffix = rebuildFenSuffix(state.side, state.fenSuffix);
    state.moveHistory.push({
        step: state.historySnapshots.length,
        side: movingSide,
        notation
    });
    state.historySnapshots.push(
        createSnapshotFromState(state.pieces, state.side, state.fenSuffix, state.lastMove)
    );
    state.historyIndex = state.historySnapshots.length - 1;
    return isCapture;
}

function parseMoveSequence() {
    const fenResult = parseFen(getFenInputValue());
    if (fenResult.error) {
        fenInput.classList.add("error");
        throw new Error(fenResult.error);
    }

    renderBoard();

    const moveTexts = splitMoveSequence(movesInput.value);
    if (moveTexts.length === 0) {
        showMessage("已按当前 FEN 重置局面");
        return;
    }

    const initialSide = "w";
    const initialFenSuffix = rebuildFenSuffix(initialSide, currentFenSuffix);
    const state = {
        pieces: clonePieces(currentPieces),
        side: initialSide,
        fenSuffix: initialFenSuffix,
        lastMove: cloneMove(lastMove),
        historySnapshots: [createSnapshotFromState(currentPieces, initialSide, initialFenSuffix, lastMove)],
        historyIndex: 0,
        moveHistory: []
    };

    let lastCapture = false;
    moveTexts.forEach((moveText, index) => {
        try {
            lastCapture = applyMoveToState(state, moveText, index + 1);
        } catch (error) {
            throw new Error(error.message.includes(`第 ${index + 1} 步`) ? error.message : `第 ${index + 1} 步${error.message}`);
        }
    });

    currentPieces = state.pieces;
    currentSide = state.side;
    currentFenSuffix = state.fenSuffix;
    lastMove = state.lastMove;
    historySnapshots = state.historySnapshots;
    historyIndex = state.historyIndex;
    moveHistory = state.moveHistory;
    selectedPiece = null;
    legalMoves = [];
    syncFenViews();
    if (window.ChessRules.isInCheck(currentSide === "w", currentPieces)) {
        showMessage("将军");
    } else {
        showMessage(`已解析 ${moveTexts.length} 步棋谱`);
    }
    if (lastCapture) {
        playMoveSound(true);
    }
    renderPieces(currentPieces);
    renderMoveHistory();
}

function setHistoryToCurrentState() {
    historySnapshots = [createSnapshot()];
    historyIndex = 0;
    moveHistory = [];
}

function updateHistoryButtons() {
    startButton.disabled = historyIndex <= 0;
    undoButton.disabled = historyIndex <= 0;
    redoButton.disabled = historyIndex >= historySnapshots.length - 1;
    endButton.disabled = historyIndex >= historySnapshots.length - 1;
}

function renderMoveHistory() {
    moveList.innerHTML = "";

    if (moveHistory.length === 0) {
        const empty = document.createElement("div");
        empty.className = "move-list-empty";
        empty.textContent = "暂无走子记录。每走一步棋会显示中文棋谱。";
        moveList.appendChild(empty);
        updateHistoryButtons();
        return;
    }

    for (let i = 0; i < moveHistory.length; i += 2) {
        const row = document.createElement("div");
        row.className = "move-row";

        const round = document.createElement("div");
        round.className = "move-round";
        round.textContent = `${Math.floor(i / 2) + 1}.`;
        row.appendChild(round);

        const moves = [moveHistory[i], moveHistory[i + 1]];
        moves.forEach(entry => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "move-entry";

            if (!entry) {
                button.classList.add("empty");
                row.appendChild(button);
                return;
            }

            button.classList.add(entry.side === "w" ? "red" : "black");
            if (historyIndex === entry.step) {
                button.classList.add("active");
            }

            const text = document.createElement("div");
            text.className = "move-text";
            text.textContent = entry.notation;

            button.appendChild(text);
            button.addEventListener("click", () => {
                restoreHistoryStep(entry.step);
            });
            row.appendChild(button);
        });

        moveList.appendChild(row);
    }

    updateHistoryButtons();
}

function restoreHistoryStep(step) {
    const snapshot = historySnapshots[step];
    if (!snapshot) {
        return;
    }

    pendingMoveAnimation = buildAnimationFromHistoryStep(step);
    historyIndex = step;
    currentPieces = clonePieces(snapshot.pieces);
    currentSide = snapshot.side;
    currentFenSuffix = snapshot.fenSuffix;
    lastMove = cloneMove(snapshot.lastMove);
    selectedPiece = null;
    legalMoves = [];
    syncFenViews();
    renderPieces(currentPieces);
    renderMoveHistory();
}

function appendHistoryEntry(entry) {
    if (historyIndex < historySnapshots.length - 1) {
        historySnapshots = historySnapshots.slice(0, historyIndex + 1);
        moveHistory = moveHistory.slice(0, historyIndex);
    }

    moveHistory.push(entry);
    historySnapshots.push(createSnapshot());
    historyIndex = historySnapshots.length - 1;
}

function parseFen(fen) {
    const fields = fen.trim().split(/\s+/).filter(Boolean);
    const boardFen = fields[0] || "";
    const rows = boardFen.split("/");
    if (rows.length !== 10) {
        return {error: "FEN 格式错误：必须包含 10 行！"};
    }

    const pieces = [];
    for (const [rowIndex, row] of rows.entries()) {
        let colIndex = 0;

        for (const ch of row) {
            if (/[1-9]/.test(ch)) {
                colIndex += Number(ch);
            } else if (pieceImages[ch]) {
                if (colIndex >= 9) {
                    return {error: "FEN 格式错误：每一行必须刚好表示 9 列，且只能包含合法棋子字符！"};
                }
                pieces.push({code: ch, row: rowIndex, col: colIndex});
                colIndex += 1;
            } else {
                return {error: "FEN 格式错误：每一行必须刚好表示 9 列，且只能包含合法棋子字符！"};
            }
        }

        if (colIndex !== 9) {
            return {error: "FEN 格式错误：每一行必须刚好表示 9 列，且只能包含合法棋子字符！"};
        }
    }

    return {
        pieces,
        suffix: fields.slice(1).join(" "),
        side: normalizeSide(fields[1])
    };
}

function getBoardMetrics() {
    const scaleX = board.clientWidth / boardBase.width;
    const scaleY = board.clientHeight / boardBase.height;
    return {
        startX: boardBase.startX * scaleX,
        startY: boardBase.startY * scaleY,
        stepX: boardBase.stepX * scaleX,
        stepY: boardBase.stepY * scaleY
    };
}

function getBoardPoint(metrics, row, col) {
    return {
        x: metrics.startX + col * metrics.stepX + 1,
        y: metrics.startY + row * metrics.stepY
    };
}

function clearMoveAnimation() {
    pendingMoveAnimation = null;
    window.clearTimeout(moveAnimationTimer);
    board.querySelectorAll(".piece.move-animation, .piece.capture-animation, .landing-pulse")
        .forEach(element => element.remove());
    board.querySelectorAll(".piece.animation-hidden")
        .forEach(element => element.classList.remove("animation-hidden"));
}

function buildMoveAnimation(code, captureCode, from, to) {
    if (!code || !from || !to) {
        return null;
    }

    return {
        code,
        captureCode: captureCode || null,
        from: {...from},
        to: {...to}
    };
}

function buildAnimationFromHistoryStep(step) {
    if (step <= 0) {
        return null;
    }

    const snapshot = historySnapshots[step];
    const previousSnapshot = historySnapshots[step - 1];
    const move = snapshot?.lastMove;
    if (!previousSnapshot || !move) {
        return null;
    }

    const movingPiece = previousSnapshot.pieces.find(piece =>
        piece.row === move.from.row && piece.col === move.from.col
    );
    if (!movingPiece) {
        return null;
    }

    const capturedPiece = previousSnapshot.pieces.find(piece =>
        piece.row === move.to.row && piece.col === move.to.col
    );

    return buildMoveAnimation(
        movingPiece.code,
        capturedPiece ? capturedPiece.code : null,
        move.from,
        move.to
    );
}

function createMoveMarker(type, x, y) {
    const marker = document.createElement("div");
    marker.className = `move-marker ${type}`;
    marker.style.left = `${x}px`;
    marker.style.top = `${y}px`;
    board.appendChild(marker);
}

function showMessage(text) {
    window.clearTimeout(messageTimer);
    message.textContent = text;
    message.classList.add("visible");
    messageTimer = window.setTimeout(() => {
        message.classList.remove("visible");
        message.textContent = "";
    }, 3000);
}

function getAudioContext() {
    if (!audioContext) {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextClass) {
            return null;
        }
        audioContext = new AudioContextClass();
    }
    if (audioContext.state === "suspended") {
        audioContext.resume();
    }
    return audioContext;
}

function playTone({
                      frequency,
                      duration,
                      type = "sine",
                      volume = 0.05,
                      delay = 0,
                      attack = Math.min(0.01, duration * 0.25),
                      decay = Math.max(duration - Math.min(0.01, duration * 0.25), 0.01)
                  }) {
    const context = getAudioContext();
    if (!context) {
        return;
    }

    const oscillator = context.createOscillator();
    const gainNode = context.createGain();
    const startAt = context.currentTime + delay;
    const peakAt = startAt + attack;
    const endAt = peakAt + decay;

    oscillator.type = type;
    oscillator.frequency.setValueAtTime(frequency, startAt);
    gainNode.gain.setValueAtTime(0.0001, startAt);
    gainNode.gain.exponentialRampToValueAtTime(volume, peakAt);
    gainNode.gain.exponentialRampToValueAtTime(0.0001, endAt);

    oscillator.connect(gainNode);
    gainNode.connect(context.destination);
    oscillator.start(startAt);
    oscillator.stop(endAt);
}

function playSelectSound() {
    playTone({
        frequency: 600,
        duration: 0.13,
        type: "sine",
        volume: 0.03,
        attack: 0.03,
        decay: 0.1
    });
}

function playMoveSound(isCapture) {
    if (isCapture) {
        playTone({
            frequency: 400,
            duration: 0.2,
            type: "triangle",
            volume: 0.08,
            attack: 0.02,
            decay: 0.18
        });
        playTone({
            frequency: 600,
            duration: 0.2,
            type: "triangle",
            volume: 0.08,
            delay: 0.2,
            attack: 0.02,
            decay: 0.18
        });
        return;
    }

    playTone({
        frequency: 200,
        duration: 0.15,
        type: "sine",
        volume: 0.06,
        attack: 0.01,
        decay: 0.14
    });
    playTone({
        frequency: 300,
        duration: 0.15,
        type: "sine",
        volume: 0.06,
        delay: 0.15,
        attack: 0.01,
        decay: 0.14
    });
}

function renderLastMove(metrics) {
    if (!lastMove) {
        return;
    }

    const pieceOffsetX = 1;
    const markerOffsetX = -2;
    const lineOffsetX = -1;
    const fromX = metrics.startX + lastMove.from.col * metrics.stepX + pieceOffsetX + markerOffsetX;
    const fromY = metrics.startY + lastMove.from.row * metrics.stepY;
    const toX = metrics.startX + lastMove.to.col * metrics.stepX + pieceOffsetX + markerOffsetX;
    const toY = metrics.startY + lastMove.to.row * metrics.stepY;

    const svgNS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(svgNS, "svg");
    svg.setAttribute("class", "move-line");
    svg.setAttribute("viewBox", `0 0 ${board.clientWidth} ${board.clientHeight}`);

    const defs = document.createElementNS(svgNS, "defs");
    const marker = document.createElementNS(svgNS, "marker");
    marker.setAttribute("id", "move-arrow");
    marker.setAttribute("markerWidth", "10");
    marker.setAttribute("markerHeight", "10");
    marker.setAttribute("refX", "8");
    marker.setAttribute("refY", "3");
    marker.setAttribute("orient", "auto");

    const arrowPath = document.createElementNS(svgNS, "path");
    arrowPath.setAttribute("d", "M0,0 L0,6 L9,3 z");
    arrowPath.setAttribute("fill", "rgba(255, 255, 255, 0.82)");
    marker.appendChild(arrowPath);
    defs.appendChild(marker);
    svg.appendChild(defs);

    const line = document.createElementNS(svgNS, "line");
    line.setAttribute("x1", String(fromX + lineOffsetX - markerOffsetX));
    line.setAttribute("y1", fromY);
    line.setAttribute("x2", String(toX + lineOffsetX - markerOffsetX));
    line.setAttribute("y2", toY);
    line.setAttribute("stroke", "rgba(255, 255, 255, 0.7)");
    line.setAttribute("stroke-width", "4");
    line.setAttribute("stroke-linecap", "round");
    line.setAttribute("stroke-dasharray", "8 6");
    line.setAttribute("marker-end", "url(#move-arrow)");
    svg.appendChild(line);
    board.appendChild(svg);

    createMoveMarker("from", fromX, fromY);
    createMoveMarker("to", toX, toY);
}

function renderLegalMoves(metrics) {
    legalMoves.forEach(move => {
        const marker = document.createElement("div");
        marker.className = `legal-move ${move.capture ? "capture" : "dot"}`;
        marker.style.left = `${metrics.startX + move.col * metrics.stepX}px`;
        marker.style.top = `${metrics.startY + move.row * metrics.stepY}px`;
        board.appendChild(marker);
    });
}

function shouldHidePieceForAnimation(piece) {
    return pendingMoveAnimation
        && piece.code === pendingMoveAnimation.code
        && piece.row === pendingMoveAnimation.to.row
        && piece.col === pendingMoveAnimation.to.col;
}

function renderMoveAnimation(metrics) {
    if (!pendingMoveAnimation) {
        return;
    }

    window.clearTimeout(moveAnimationTimer);
    const animation = pendingMoveAnimation;
    const fromPoint = getBoardPoint(metrics, animation.from.row, animation.from.col);
    const toPoint = getBoardPoint(metrics, animation.to.row, animation.to.col);

    if (animation.captureCode) {
        const capturedPiece = document.createElement("img");
        capturedPiece.className = "piece capture-animation";
        capturedPiece.src = pieceImages[animation.captureCode];
        capturedPiece.alt = pieceNames[animation.captureCode];
        capturedPiece.style.left = `${toPoint.x}px`;
        capturedPiece.style.top = `${toPoint.y}px`;
        board.appendChild(capturedPiece);
    }

    const movingPiece = document.createElement("img");
    movingPiece.className = "piece move-animation";
    movingPiece.src = pieceImages[animation.code];
    movingPiece.alt = pieceNames[animation.code];
    movingPiece.style.left = `${fromPoint.x}px`;
    movingPiece.style.top = `${fromPoint.y}px`;
    board.appendChild(movingPiece);

    const landingPulse = document.createElement("div");
    landingPulse.className = "landing-pulse";
    landingPulse.style.left = `${toPoint.x}px`;
    landingPulse.style.top = `${toPoint.y}px`;
    board.appendChild(landingPulse);

    window.requestAnimationFrame(() => {
        movingPiece.classList.add("primed");
        window.setTimeout(() => {
            movingPiece.style.left = `${toPoint.x}px`;
            movingPiece.style.top = `${toPoint.y}px`;
            movingPiece.classList.add("moving");
        },/*移动过渡*/ 200);
    });

    moveAnimationTimer = window.setTimeout(() => {
        pendingMoveAnimation = null;
        movingPiece.remove();
        landingPulse.remove();
        board.querySelectorAll(".piece.capture-animation").forEach(element => element.remove());
        board.querySelectorAll(".piece.animation-hidden")
            .forEach(element => element.classList.remove("animation-hidden"));
    }, 560);
}

function renderPieces(pieces) {
    const metrics = getBoardMetrics();
    board.innerHTML = "";
    renderLastMove(metrics);
    renderLegalMoves(metrics);

    pieces.forEach(piece => {
        const img = document.createElement("img");
        img.className = "piece";
        if (piece === selectedPiece) {
            img.classList.add("selected");
        }
        if (shouldHidePieceForAnimation(piece)) {
            img.classList.add("animation-hidden");
        }
        img.src = pieceImages[piece.code];
        img.alt = pieceNames[piece.code];
        img.style.left = `${metrics.startX + piece.col * metrics.stepX + 1}px`;
        img.style.top = `${metrics.startY + piece.row * metrics.stepY}px`;
        img.addEventListener("click", event => {
            event.stopPropagation();
            if (!selectedPiece) {
                if (!isPieceOwnedByCurrentSide(piece)) {
                    showMessage(`现在轮到${getSideLabel(currentSide)}走棋`);
                    return;
                }
                selectedPiece = piece;
                legalMoves = window.ChessRules.getLegalMoves(piece, currentPieces);
                playSelectSound();
                renderPieces(currentPieces);
                return;
            }

            if (selectedPiece === piece) {
                selectedPiece = null;
                legalMoves = [];
                renderPieces(currentPieces);
                return;
            }

            const sameSide = window.ChessRules.isSameSide(selectedPiece, piece);
            if (sameSide) {
                if (!isPieceOwnedByCurrentSide(piece)) {
                    showMessage(`现在轮到${getSideLabel(currentSide)}走棋`);
                    return;
                }
                selectedPiece = piece;
                legalMoves = window.ChessRules.getLegalMoves(piece, currentPieces);
                playSelectSound();
                renderPieces(currentPieces);
                return;
            }

            moveSelectedPieceTo(piece.row, piece.col);
        });
        board.appendChild(img);
    });

    renderMoveAnimation(metrics);
}

function piecesToFen(pieces, suffix = "") {
    const rows = Array.from({length: 10}, () => Array(9).fill(""));
    pieces.forEach(piece => {
        rows[piece.row][piece.col] = piece.code;
    });

    const boardFen = rows.map(row => {
        let result = "";
        let emptyCount = 0;

        row.forEach(cell => {
            if (!cell) {
                emptyCount += 1;
                return;
            }
            if (emptyCount > 0) {
                result += emptyCount;
                emptyCount = 0;
            }
            result += cell;
        });

        if (emptyCount > 0) {
            result += emptyCount;
        }
        return result;
    }).join("/");

    return suffix ? `${boardFen} ${suffix}` : boardFen;
}

function moveSelectedPieceTo(row, col) {
    if (!selectedPiece) {
        return;
    }

    const from = {row: selectedPiece.row, col: selectedPiece.col};
    const to = {row, col};
    const moveResult = window.ChessRules.isLegalMove(selectedPiece, from, to, currentPieces);
    if (!moveResult.ok) {
        showMessage(moveResult.message);
        return;
    }

    const movingPieceCode = selectedPiece.code;
    const notation = generateMoveNotation(selectedPiece, from, to);
    const capturedPiece = currentPieces.find(piece =>
        piece !== selectedPiece && piece.row === row && piece.col === col
    );

    lastMove = {
        from,
        to
    };
    const isCapture = Boolean(capturedPiece);
    pendingMoveAnimation = buildMoveAnimation(
        movingPieceCode,
        capturedPiece ? capturedPiece.code : null,
        from,
        to
    );
    currentPieces = currentPieces.filter(piece =>
        piece === selectedPiece || piece.row !== row || piece.col !== col
    );
    selectedPiece.row = row;
    selectedPiece.col = col;
    currentSide = currentSide === "w" ? "b" : "w";
    currentFenSuffix = rebuildFenSuffix(currentSide, currentFenSuffix);
    appendHistoryEntry({
        step: historyIndex + 1,
        side: movingPieceCode === movingPieceCode.toUpperCase() ? "w" : "b",
        notation
    });
    syncFenViews();
    selectedPiece = null;
    legalMoves = [];
    playMoveSound(isCapture);
    if (window.ChessRules.isInCheck(currentSide === "w", currentPieces)) {
        showMessage("将军");
    }
    renderPieces(currentPieces);
    renderMoveHistory();
}

function resetBoardState() {
    clearMoveAnimation();
    currentPieces = [];
    currentFenSuffix = "";
    originalFenInput = "";
    selectedPiece = null;
    lastMove = null;
    currentSide = "w";
    legalMoves = [];
    historySnapshots = [];
    historyIndex = 0;
    moveHistory = [];
    board.innerHTML = "";
    moveList.innerHTML = "";
    updateHistoryButtons();
    window.clearTimeout(messageTimer);
    message.classList.remove("visible");
    message.textContent = "";
}

function clearAllInformation() {
    fenInput.value = "";
    fenInput.classList.remove("error");
    movesInput.value = "";
    imageInput.value = "";
    currentFenDisplay.value = "";
    resetBoardState();
}

function renderBoard() {
    const fen = getFenInputValue();
    resetBoardState();
    const result = parseFen(fen);

    if (result.error) {
        fenInput.classList.add("error");
        updateHistoryButtons();
        alert(result.error);
        return;
    }

    fenInput.classList.remove("error");
    originalFenInput = fen;
    applyParsedFen(result);
}

function applyPosition() {
    if (!movesInput.value.trim()) {
        renderBoard();
        return;
    }

    try {
        parseMoveSequence();
    } catch (error) {
        showMessage(error.message);
    }
}

async function applyImagePosition() {
    const file = imageInput.files[0];
    if (!file) {
        showMessage("请选择图片");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/api/chess/detect", {
            method: "POST",
            body: formData
        });
        const data = await response.json();
        const fen = (data.fen || "").trim();

        if (!response.ok) {
            throw new Error(data.message || "图片识别失败");
        }
        if (!fen) {
            throw new Error("未返回fen");
        }

        fenInput.value = fen;
        applyPosition();
    } catch (error) {
        showMessage(error.message || "图片识别失败");
    }
}

function isLeafNode(node) {
    return node && typeof node.fen === "string" && typeof node.moves === "string";
}

function loadTreeLeaf(node, key) {
    fenInput.value = node.fen || "";
    movesInput.value = node.moves || "";
    activeTreeKey = key;
    renderGameTree();
    applyPosition();
}

function toggleTreeBranch(key) {
    if (collapsedTreeKeys.has(key)) {
        collapsedTreeKeys.delete(key);
    } else {
        collapsedTreeKeys.add(key);
    }
    renderGameTree();
}

function filterTreeNodes(nodes, keyword) {
    if (!keyword) {
        return nodes;
    }

    const normalizedKeyword = keyword.toLowerCase();
    const result = [];

    nodes.forEach(node => {
        const title = (node.title || "").toLowerCase();
        const matched = title.includes(normalizedKeyword);

        if (node.children && node.children.length) {
            const filteredChildren = filterTreeNodes(node.children, keyword);
            if (matched || filteredChildren.length > 0) {
                result.push({
                    ...node,
                    children: filteredChildren
                });
            }
            return;
        }

        if (matched) {
            result.push(node);
        }
    });

    return result;
}

function isTreeSearching() {
    return Boolean(treeKeyword.trim());
}


function renderTreeNodes(nodes, container, level = 0, path = "") {
    nodes.forEach((node, index) => {
        const key = path ? `${path}-${index}` : String(index);
        const item = document.createElement("div");
        item.className = `tree-node ${node.children ? "branch" : "leaf"}`;
        item.style.paddingLeft = `${10 + level * 14}px`;

        if (activeTreeKey === key) {
            item.classList.add("active");
        }

        /*       if (node.children && node.children.length) {
                   item.textContent = `▾ ${node.title}`;
                   container.appendChild(item);

                   const children = document.createElement("div");
                   children.className = "tree-children";
                   container.appendChild(children);
                   renderTreeNodes(node.children, children, level + 1, key);
                   return;
               }*/
        /*      if (node.children && node.children.length) {
                  const collapsed = collapsedTreeKeys.has(key);
                  item.textContent = `${collapsed ? "▸" : "▾"} ${node.title}`;
                  item.addEventListener("click", () => toggleTreeBranch(key));
                  container.appendChild(item);

                  if (!collapsed) {
                      const children = document.createElement("div");
                      children.className = "tree-children";
                      container.appendChild(children);
                      renderTreeNodes(node.children, children, level + 1, key);
                  }
                  return;
              }*/
        if (node.children && node.children.length) {
            const collapsed = isTreeSearching() ? false : collapsedTreeKeys.has(key);
            item.textContent = `${collapsed ? "▸" : "▾"} ${node.title}`;
            item.addEventListener("click", () => toggleTreeBranch(key));
            container.appendChild(item);

            if (!collapsed) {
                const children = document.createElement("div");
                children.className = "tree-children";
                container.appendChild(children);
                renderTreeNodes(node.children, children, level + 1, key);
            }
            return;
        }


        item.textContent = `· ${node.title}`;
        item.addEventListener("click", () => loadTreeLeaf(node, key));
        container.appendChild(item);
    });
}

/*function renderGameTree() {
    if (!treeList || !Array.isArray(window.gameTree || gameTree)) {
        return;
    }

    treeList.innerHTML = "";
    renderTreeNodes(window.gameTree || gameTree, treeList);
}*/

function renderGameTree() {
    if (!treeList || !Array.isArray(window.gameTree || gameTree)) {
        return;
    }

    const sourceTree = window.gameTree || gameTree;
    const filteredTree = filterTreeNodes(sourceTree, treeKeyword.trim());

    treeList.innerHTML = "";
    renderTreeNodes(filteredTree, treeList);
}


applyButton.addEventListener("click", applyPosition);
cancelButton.addEventListener("click", clearAllInformation);
imageApplyButton.addEventListener("click", applyImagePosition);
startButton.addEventListener("click", () => {
    if (historyIndex > 0) {
        restoreHistoryStep(0);
    }
});
undoButton.addEventListener("click", () => {
    if (historyIndex > 0) {
        restoreHistoryStep(historyIndex - 1);
    }
});
redoButton.addEventListener("click", () => {
    if (historyIndex < historySnapshots.length - 1) {
        restoreHistoryStep(historyIndex + 1);
    }
});
endButton.addEventListener("click", () => {
    if (historyIndex < historySnapshots.length - 1) {
        restoreHistoryStep(historySnapshots.length - 1);
    }
});
fenInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        applyPosition();
    }
});
movesInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        applyPosition();
    }
});

board.addEventListener("click", event => {
    if (!selectedPiece) {
        return;
    }

    const rect = board.getBoundingClientRect();
    const metrics = getBoardMetrics();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const col = Math.max(0, Math.min(8, Math.round((x - metrics.startX) / metrics.stepX)));
    const row = Math.max(0, Math.min(9, Math.round((y - metrics.startY) / metrics.stepY)));

    moveSelectedPieceTo(row, col);
});

treeSearchInput.addEventListener("input", event => {
    treeKeyword = event.target.value || "";
    renderGameTree();
});


window.addEventListener("resize", () => renderPieces(currentPieces));



renderBoard();
renderGameTree();
