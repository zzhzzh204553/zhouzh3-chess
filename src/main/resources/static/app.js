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
const renderButton = document.getElementById("renderButton");
const board = document.getElementById("board");
const message = document.getElementById("message");

let currentPieces = [];
let currentFenSuffix = "";
let selectedPiece = null;
let lastMove = null;
let messageTimer = null;
let currentSide = "w";
let legalMoves = [];
let audioContext = null;

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
    }, 1800);
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

    const markerOffsetX = -2;
    const lineOffsetX = -1;
    const fromX = metrics.startX + lastMove.from.col * metrics.stepX + markerOffsetX;
    const fromY = metrics.startY + lastMove.from.row * metrics.stepY;
    const toX = metrics.startX + lastMove.to.col * metrics.stepX + markerOffsetX;
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
        img.src = pieceImages[piece.code];
        img.alt = pieceNames[piece.code];
        img.style.left = `${metrics.startX + piece.col * metrics.stepX}px`;
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

    lastMove = {
        from,
        to
    };
    const isCapture = currentPieces.some(piece =>
        piece !== selectedPiece && piece.row === row && piece.col === col
    );
    currentPieces = currentPieces.filter(piece =>
        piece === selectedPiece || piece.row !== row || piece.col !== col
    );
    selectedPiece.row = row;
    selectedPiece.col = col;
    currentSide = currentSide === "w" ? "b" : "w";
    currentFenSuffix = rebuildFenSuffix(currentSide, currentFenSuffix);
    fenInput.value = piecesToFen(currentPieces, currentFenSuffix);
    selectedPiece = null;
    legalMoves = [];
    playMoveSound(isCapture);
    renderPieces(currentPieces);
}

function resetBoardState() {
    currentPieces = [];
    currentFenSuffix = "";
    selectedPiece = null;
    lastMove = null;
    currentSide = "w";
    legalMoves = [];
    board.innerHTML = "";
    window.clearTimeout(messageTimer);
    message.classList.remove("visible");
    message.textContent = "";
}

function renderBoard() {
    const fen = fenInput.value.trim();
    resetBoardState();
    const result = parseFen(fen);

    if (result.error) {
        fenInput.classList.add("error");
        alert(result.error);
        return;
    }

    fenInput.classList.remove("error");
    currentPieces = result.pieces;
    currentSide = result.side;
    currentFenSuffix = rebuildFenSuffix(currentSide, result.suffix);
    fenInput.value = piecesToFen(currentPieces, currentFenSuffix);
    legalMoves = [];
    renderPieces(currentPieces);
}

renderButton.addEventListener("click", renderBoard);
fenInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        renderBoard();
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

window.addEventListener("resize", () => renderPieces(currentPieces));

renderBoard();
