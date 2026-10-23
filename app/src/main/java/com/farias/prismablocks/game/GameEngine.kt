package com.farias.prismablocks.game

import kotlin.random.Random

/** What happened after a piece was placed (or after a revive / bomb). Drives scoring UI and effects. */
data class MoveResult(
    val placed: List<Cell>,
    val color: Int,
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    /** Cleared cells with the color they had before clearing (for particles). */
    val clearedCells: List<Pair<Cell, Int>>,
    val placePoints: Int,
    val linePoints: Int,
    val combo: Int,
    val boardCleared: Boolean,
    val gameOver: Boolean,
    val trayRefilled: Boolean,
) {
    val lines: Int get() = clearedRows.size + clearedCols.size
    val totalPoints: Int get() = placePoints + linePoints
}

/**
 * Pure game rules for an 8x8 block puzzle. No Android dependencies, so it can be unit tested.
 * Board values: 0 = empty, 1..7 = block color.
 */
class GameEngine(private var rng: Random = Random.Default) {

    companion object {
        const val N = 8
        const val COLORS = 7
        private val LINE_POINTS = intArrayOf(0, 80, 240, 480, 800, 1200, 1680, 2240)
        const val BOARD_CLEAR_BONUS = 600
        private const val SAVE_V1 = "v1"
        private const val SAVE_V2 = "v2"
    }

    var board = IntArray(N * N)
        private set
    val tray = arrayOfNulls<Piece>(3)
    var score = 0
        private set
    var combo = 0
        private set
    var movesSinceClear = 0
        private set
    var revived = false
        private set
    var gameOver = false
        private set
    /** Total rows + columns cleared this game (daily challenge goal). */
    var linesCleared = 0
        private set

    /** State before the last placement, for the "undo" power-up (one level). */
    private var undoSnapshot: String? = null
    val canUndo: Boolean get() = undoSnapshot != null

    init {
        newGame()
    }

    /**
     * Starts a new game. With a [seed] the whole game (pre-filled board and pieces) is
     * deterministic, so every player gets the same daily challenge.
     */
    fun newGame(seed: Long? = null, prefill: Int = 0) {
        rng = if (seed != null) Random(seed) else Random.Default
        board = IntArray(N * N)
        score = 0
        combo = 0
        movesSinceClear = 0
        revived = false
        gameOver = false
        linesCleared = 0
        undoSnapshot = null
        for (i in tray.indices) tray[i] = null
        if (prefill > 0) prefillBoard(prefill)
        generateTray()
        gameOver = !anyFits()
    }

    fun cellAt(r: Int, c: Int): Int = board[r * N + c]

    fun canPlace(shape: Shape, r: Int, c: Int, b: IntArray = board): Boolean {
        if (r < 0 || c < 0 || r + shape.rows > N || c + shape.cols > N) return false
        for (cell in shape.cells) {
            if (b[(r + cell.r) * N + (c + cell.c)] != 0) return false
        }
        return true
    }

    fun fitsAnywhere(shape: Shape, b: IntArray = board): Boolean {
        for (r in 0..N - shape.rows) for (c in 0..N - shape.cols) {
            if (canPlace(shape, r, c, b)) return true
        }
        return false
    }

    fun anyFits(): Boolean = tray.any { it != null && fitsAnywhere(it.shape) }

    /** Rows and columns that would be completed if [shape] were placed at (r, c). Used for the drag preview. */
    fun previewLines(shape: Shape, r: Int, c: Int): Pair<List<Int>, List<Int>> {
        if (!canPlace(shape, r, c)) return emptyList<Int>() to emptyList()
        val b = board.copyOf()
        for (cell in shape.cells) b[(r + cell.r) * N + (c + cell.c)] = 1
        return fullRows(b) to fullCols(b)
    }

    /** Places the piece from tray [slot] at (r, c). Returns null if the move is illegal. */
    fun place(slot: Int, r: Int, c: Int): MoveResult? {
        if (gameOver) return null
        val piece = tray.getOrNull(slot) ?: return null
        val shape = piece.shape
        if (!canPlace(shape, r, c)) return null

        undoSnapshot = serialize()

        val placed = shape.cells.map { Cell(r + it.r, c + it.c) }
        for (cell in placed) board[cell.r * N + cell.c] = piece.color
        tray[slot] = null

        val rows = fullRows(board)
        val cols = fullCols(board)
        val clearedIdx = LinkedHashSet<Int>()
        for (row in rows) for (col in 0 until N) clearedIdx.add(row * N + col)
        for (col in cols) for (row in 0 until N) clearedIdx.add(row * N + col)
        val clearedCells = clearedIdx.map { Cell(it / N, it % N) to board[it] }
        for (i in clearedIdx) board[i] = 0

        val lines = rows.size + cols.size
        linesCleared += lines
        if (lines > 0) {
            combo += 1
            movesSinceClear = 0
        } else {
            movesSinceClear += 1
            if (movesSinceClear >= 3) combo = 0
        }

        val placePoints = shape.size
        var linePoints = 0
        val boardCleared = lines > 0 && board.all { it == 0 }
        if (lines > 0) {
            val base = if (lines < LINE_POINTS.size) LINE_POINTS[lines] else LINE_POINTS.last() + (lines - LINE_POINTS.size + 1) * 600
            linePoints = base * maxOf(1, combo)
            if (boardCleared) linePoints += BOARD_CLEAR_BONUS
        }
        score += placePoints + linePoints

        var refilled = false
        if (tray.all { it == null }) {
            generateTray()
            refilled = true
        }
        gameOver = !anyFits()

        return MoveResult(
            placed = placed,
            color = piece.color,
            clearedRows = rows,
            clearedCols = cols,
            clearedCells = clearedCells,
            placePoints = placePoints,
            linePoints = linePoints,
            combo = combo,
            boardCleared = boardCleared,
            gameOver = gameOver,
            trayRefilled = refilled,
        )
    }

    /** Reverts the last placement. Returns false if there is nothing to undo. */
    fun undo(): Boolean {
        val snap = undoSnapshot ?: return false
        undoSnapshot = null
        val ok = restore(snap)
        undoSnapshot = null
        return ok
    }

    /** Clears the 3x3 area centered on (r, c). Returns null if there was nothing to clear. */
    fun bomb(r: Int, c: Int): MoveResult? {
        val cleared = ArrayList<Pair<Cell, Int>>()
        for (rr in r - 1..r + 1) for (cc in c - 1..c + 1) {
            if (rr !in 0 until N || cc !in 0 until N) continue
            val i = rr * N + cc
            if (board[i] != 0) {
                cleared.add(Cell(rr, cc) to board[i])
                board[i] = 0
            }
        }
        if (cleared.isEmpty()) return null
        undoSnapshot = null
        gameOver = !anyFits()
        return MoveResult(
            placed = emptyList(), color = 0, clearedRows = emptyList(), clearedCols = emptyList(),
            clearedCells = cleared, placePoints = 0, linePoints = 0, combo = combo,
            boardCleared = false, gameOver = gameOver, trayRefilled = false,
        )
    }

    /** Replaces the tray with 3 new pieces (power-up). */
    fun shuffleTray() {
        undoSnapshot = null
        for (i in tray.indices) tray[i] = null
        generateTray()
        gameOver = !anyFits()
    }

    /**
     * Second chance (after a rewarded ad): clears the 3 fullest rows and makes sure
     * the tray has something that fits. Allowed once per game.
     */
    fun revive(): MoveResult? {
        if (!gameOver || revived) return null
        revived = true
        undoSnapshot = null
        val rowsByFill = (0 until N).sortedByDescending { row ->
            (0 until N).count { col -> board[row * N + col] != 0 } * 10 + rng.nextInt(10)
        }.take(3).sorted()
        val cleared = ArrayList<Pair<Cell, Int>>()
        for (row in rowsByFill) for (col in 0 until N) {
            val i = row * N + col
            if (board[i] != 0) cleared.add(Cell(row, col) to board[i])
            board[i] = 0
        }
        if (!anyFits()) {
            for (i in tray.indices) tray[i] = null
            generateTray()
        }
        gameOver = !anyFits()
        return MoveResult(
            placed = emptyList(), color = 0, clearedRows = rowsByFill, clearedCols = emptyList(),
            clearedCells = cleared, placePoints = 0, linePoints = 0, combo = combo,
            boardCleared = false, gameOver = gameOver, trayRefilled = true,
        )
    }

    // ---------------------------------------------------------------- board / tray generation

    /** Scatters [count] blocks in small clusters, never completing a row or column. */
    private fun prefillBoard(count: Int) {
        var placed = 0
        var guard = 0
        while (placed < count && guard < 2000) {
            guard++
            val r = rng.nextInt(N)
            val c = rng.nextInt(N)
            val i = r * N + c
            if (board[i] != 0) continue
            // Favour cells next to existing blocks so the board looks like a real game.
            val neighbours = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
                .count { (rr, cc) -> rr in 0 until N && cc in 0 until N && board[rr * N + cc] != 0 }
            if (placed > 3 && neighbours == 0 && rng.nextFloat() < 0.7f) continue
            board[i] = 1 + rng.nextInt(COLORS)
            if (fullRows(board).isNotEmpty() || fullCols(board).isNotEmpty()) {
                board[i] = 0
                continue
            }
            placed++
        }
    }

    private fun generateTray() {
        val fill = board.count { it != 0 } / (N * N).toFloat()
        var last: List<Piece> = emptyList()
        for (attempt in 0 until 40) {
            val pieces = randomTriple(fill)
            last = pieces
            val ok = when {
                attempt < 25 -> canPlaceAll(board, pieces.map { it.shape }, intArrayOf(0))
                else -> pieces.any { fitsAnywhere(it.shape) }
            }
            if (ok) break
        }
        for (i in 0 until 3) tray[i] = last[i]
    }

    private fun randomTriple(fill: Float): List<Piece> {
        val colors = (1..COLORS).shuffled(rng)
        return List(3) { i -> Piece(randomShape(fill), colors[i]) }
    }

    private fun randomShape(fill: Float): Shape {
        val weights = Shapes.all.map { s ->
            var w = s.weight.toFloat()
            if (fill > 0.5f) {
                if (s.size <= 3) w *= 2f
                if (s.size >= 5) w *= 0.4f
            } else if (fill < 0.2f && s.size >= 4) {
                w *= 1.3f
            }
            w
        }
        var pick = rng.nextFloat() * weights.sum()
        for (i in weights.indices) {
            pick -= weights[i]
            if (pick <= 0f) return Shapes.all[i]
        }
        return Shapes.all.last()
    }

    /** Depth-first check that all shapes can be placed in some order (line clears included). Node-capped for speed. */
    private fun canPlaceAll(b: IntArray, shapes: List<Shape>, budget: IntArray): Boolean {
        if (shapes.isEmpty()) return true
        for (i in shapes.indices) {
            val shape = shapes[i]
            val rest = shapes.filterIndexed { j, _ -> j != i }
            for (r in 0..N - shape.rows) for (c in 0..N - shape.cols) {
                if (!canPlace(shape, r, c, b)) continue
                budget[0]++
                if (budget[0] > 4000) return false
                val copy = b.copyOf()
                for (cell in shape.cells) copy[(r + cell.r) * N + (c + cell.c)] = 1
                clearFull(copy)
                if (canPlaceAll(copy, rest, budget)) return true
            }
        }
        return false
    }

    private fun clearFull(b: IntArray) {
        val rows = fullRows(b)
        val cols = fullCols(b)
        for (row in rows) for (col in 0 until N) b[row * N + col] = 0
        for (col in cols) for (row in 0 until N) b[row * N + col] = 0
    }

    private fun fullRows(b: IntArray): List<Int> =
        (0 until N).filter { row -> (0 until N).all { col -> b[row * N + col] != 0 } }

    private fun fullCols(b: IntArray): List<Int> =
        (0 until N).filter { col -> (0 until N).all { row -> b[row * N + col] != 0 } }

    // ---------------------------------------------------------------- save / restore

    fun serialize(): String {
        val trayStr = tray.joinToString(",") { p -> if (p == null) "-" else "${p.shape.id}:${p.color}" }
        return listOf(
            SAVE_V2, score, combo, movesSinceClear, if (revived) 1 else 0,
            board.joinToString(""), trayStr, linesCleared,
        ).joinToString(";")
    }

    /** Restores a game saved with [serialize]. Returns false (and leaves a fresh game) if the data is invalid. */
    fun restore(data: String?): Boolean {
        if (data.isNullOrBlank()) return false
        return try {
            val parts = data.split(";")
            require((parts.size == 7 && parts[0] == SAVE_V1) || (parts.size == 8 && parts[0] == SAVE_V2))
            val b = parts[5].map { it.digitToInt() }.toIntArray()
            require(b.size == N * N && b.all { it in 0..COLORS })
            val t = parts[6].split(",").map { token ->
                if (token == "-") null else {
                    val (id, color) = token.split(":").map { it.toInt() }
                    Piece(requireNotNull(Shapes.byId(id)), color.coerceIn(1, COLORS))
                }
            }
            require(t.size == 3)
            board = b
            score = parts[1].toInt()
            combo = parts[2].toInt()
            movesSinceClear = parts[3].toInt()
            revived = parts[4] == "1"
            linesCleared = if (parts.size == 8) parts[7].toInt() else 0
            undoSnapshot = null
            for (i in 0 until 3) tray[i] = t[i]
            if (tray.all { it == null }) generateTray()
            gameOver = !anyFits()
            true
        } catch (e: Exception) {
            newGame()
            false
        }
    }
}
