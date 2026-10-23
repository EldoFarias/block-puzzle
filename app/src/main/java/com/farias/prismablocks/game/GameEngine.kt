package com.farias.prismablocks.game

import kotlin.random.Random

/** What happened after a piece was placed (or after a revive / bomb). Drives scoring UI and effects. */
data class MoveResult(
    val placed: List<Cell>,
    val color: Int,
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    /** Cleared cells with the value they had before clearing (for particles). */
    val clearedCells: List<Pair<Cell, Int>>,
    val placePoints: Int,
    val linePoints: Int,
    val combo: Int,
    val boardCleared: Boolean,
    val gameOver: Boolean,
    val trayRefilled: Boolean,
    /** Gems (or drawing blocks) collected by this move (adventure mode). */
    val gems: List<Cell> = emptyList(),
    val iceMelted: List<Cell> = emptyList(),
    /** Stones hit for the first time: they stay on the board, cracked. */
    val cracked: List<Cell> = emptyList(),
    val unlocked: List<Cell> = emptyList(),
    /** Time bombs cleared before going off. */
    val defused: List<Cell> = emptyList(),
    /** Time bomb that reached zero with this move (ends the game). */
    val exploded: Cell? = null,
) {
    val lines: Int get() = clearedRows.size + clearedCols.size
    val totalPoints: Int get() = placePoints + linePoints
}

/** Starting board of an adventure level. Everything is optional. */
data class BoardSetup(
    val prefill: Int = 0,
    val gems: Int = 0,
    /** 0..1: how much gems favour nearly-complete lines (easier early levels). */
    val gemEase: Float = 0f,
    /** A drawing to clear: its blocks are the targets (counted like gems). */
    val pattern: List<Cell> = emptyList(),
    val patternColor: Int = 1,
    /** Every pre-filled block is a target: the goal is to clear them all. */
    val markPrefill: Boolean = false,
    val stones: Int = 0,
    val locks: Int = 0,
    val ice: Int = 0,
    val timers: Int = 0,
    /** Moves before a time bomb goes off. */
    val timerMoves: Int = 0,
)

/**
 * Pure game rules for an 8x8 block puzzle. No Android dependencies, so it can be unit tested.
 * Board values: 0 = empty, 1..7 = block color, then the obstacles [STONE], [CRACKED] and [LOCK].
 */
class GameEngine(private var rng: Random = Random.Default) {

    companion object {
        const val N = 8
        const val COLORS = 7
        /** Stone block: the first clear only cracks it, the second removes it. */
        const val STONE = 8
        const val CRACKED = 9
        /** Locked cell: nothing fits there and it doesn't count for lines; a clear next to it unlocks it. */
        const val LOCK = 10
        private val LINE_POINTS = intArrayOf(0, 80, 240, 480, 800, 1200, 1680, 2240)
        const val BOARD_CLEAR_BONUS = 600
        private const val SAVE_V1 = "v1"
        private const val SAVE_V2 = "v2"
        private const val SAVE_V3 = "v3"
        private const val SAVE_V4 = "v4"

        /** True for cells that count towards completing a line. */
        fun filled(v: Int): Boolean = v != 0 && v != LOCK
    }

    var board = IntArray(N * N)
        private set
    val tray = arrayOfNulls<Piece>(3)
    var score = 0
        private set
    var combo = 0
        private set
    /** Highest combo reached this game. */
    var maxCombo = 0
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
    /** Cells holding a gem (adventure mode). A gem is collected when its block is cleared. */
    var gems = BooleanArray(N * N)
        private set
    var gemsCollected = 0
        private set
    var gemsTotal = 0
        private set
    /** Frozen cells: they melt when a cleared line goes through them. */
    var ice = BooleanArray(N * N)
        private set
    var iceMelted = 0
        private set
    var iceTotal = 0
        private set
    /** Moves left on each time bomb (0 = no bomb on that cell). */
    var timers = IntArray(N * N)
        private set
    /** A time bomb went off: the game is lost unless the player revives. */
    var exploded = false
        private set

    /** State before the last placement, for the "undo" power-up (one level). */
    private var undoSnapshot: String? = null
    val canUndo: Boolean get() = undoSnapshot != null

    /** Chance of handing the player the most "helpful" of several valid trays. */
    var helpChance = 0.65f

    init {
        newGame()
    }

    /**
     * Starts a new game. With a [seed] the whole game (pre-filled board and pieces) is
     * deterministic, so every player gets the same daily challenge or adventure level.
     */
    fun newGame(seed: Long? = null, prefill: Int = 0, setup: BoardSetup? = null) {
        rng = if (seed != null) Random(seed) else Random.Default
        board = IntArray(N * N)
        score = 0
        combo = 0
        maxCombo = 0
        movesSinceClear = 0
        revived = false
        gameOver = false
        linesCleared = 0
        gems = BooleanArray(N * N)
        gemsCollected = 0
        gemsTotal = 0
        ice = BooleanArray(N * N)
        iceMelted = 0
        iceTotal = 0
        timers = IntArray(N * N)
        exploded = false
        undoSnapshot = null
        for (i in tray.indices) tray[i] = null
        val s = setup ?: BoardSetup(prefill = prefill)
        if (s.pattern.isNotEmpty()) {
            for (cell in s.pattern) {
                val i = cell.r * N + cell.c
                board[i] = s.patternColor
                gems[i] = true
            }
            gemsTotal = s.pattern.size
        } else {
            if (s.prefill > 0 || s.gems > 0) prefillBoard(maxOf(s.prefill, s.gems + s.timers))
            if (s.gems > 0) placeGems(s.gems, s.gemEase)
            if (s.markPrefill) {
                for (i in board.indices) gems[i] = board[i] != 0
                gemsTotal = gems.count { it }
            }
        }
        if (s.timers > 0) placeTimers(s.timers, s.timerMoves)
        if (s.stones > 0) placeStones(s.stones)
        if (s.locks > 0) placeLocks(s.locks)
        if (s.ice > 0) placeIce(s.ice)
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
        val collected = collectGems(clearedIdx)
        val melted = meltIce(clearedIdx)
        val defused = defuse(clearedIdx)
        val cracked = ArrayList<Cell>()
        for (i in clearedIdx) {
            if (board[i] == STONE) {
                board[i] = CRACKED
                cracked.add(Cell(i / N, i % N))
            } else {
                board[i] = 0
            }
        }
        val unlocked = unlockAround(clearedIdx)

        val lines = rows.size + cols.size
        linesCleared += lines
        if (lines > 0) {
            combo += 1
            movesSinceClear = 0
        } else {
            movesSinceClear += 1
            if (movesSinceClear >= 3) combo = 0
        }
        maxCombo = maxOf(maxCombo, combo)

        val placePoints = shape.size
        var linePoints = 0
        val boardCleared = lines > 0 && board.all { it == 0 }
        if (lines > 0) {
            val base = if (lines < LINE_POINTS.size) LINE_POINTS[lines] else LINE_POINTS.last() + (lines - LINE_POINTS.size + 1) * 600
            linePoints = base * maxOf(1, combo)
            if (boardCleared) linePoints += BOARD_CLEAR_BONUS
        }
        score += placePoints + linePoints

        val boom = tickTimers()

        var refilled = false
        if (tray.all { it == null }) {
            generateTray()
            refilled = true
        }
        gameOver = exploded || !anyFits()

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
            gems = collected,
            iceMelted = melted,
            cracked = cracked,
            unlocked = unlocked,
            defused = defused,
            exploded = boom,
        )
    }

    /** Collects the gems sitting on [indices] (cells about to be cleared). */
    private fun collectGems(indices: Collection<Int>): List<Cell> {
        val out = ArrayList<Cell>()
        for (i in indices) if (gems[i]) {
            gems[i] = false
            gemsCollected++
            out.add(Cell(i / N, i % N))
        }
        return out
    }

    private fun meltIce(indices: Collection<Int>): List<Cell> {
        val out = ArrayList<Cell>()
        for (i in indices) if (ice[i]) {
            ice[i] = false
            iceMelted++
            out.add(Cell(i / N, i % N))
        }
        return out
    }

    private fun defuse(indices: Collection<Int>): List<Cell> {
        val out = ArrayList<Cell>()
        for (i in indices) if (timers[i] > 0) {
            timers[i] = 0
            out.add(Cell(i / N, i % N))
        }
        return out
    }

    /** Opens the locked cells next to the cleared ones. */
    private fun unlockAround(cleared: Collection<Int>): List<Cell> {
        val out = ArrayList<Cell>()
        for (i in cleared) {
            val r = i / N
            val c = i % N
            for ((rr, cc) in listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)) {
                if (rr !in 0 until N || cc !in 0 until N) continue
                val j = rr * N + cc
                if (board[j] == LOCK) {
                    board[j] = 0
                    out.add(Cell(rr, cc))
                }
            }
        }
        return out
    }

    /** Every move brings the time bombs one step closer. Returns the bomb that went off, if any. */
    private fun tickTimers(): Cell? {
        var boom: Cell? = null
        for (i in timers.indices) if (timers[i] > 0) {
            timers[i]--
            if (timers[i] == 0) {
                exploded = true
                if (boom == null) boom = Cell(i / N, i % N)
            }
        }
        return boom
    }

    /**
     * Puts [count] gems on colored cells, spread over different rows when possible.
     * With a higher [ease] gems prefer cells on fuller lines, which are quicker to clear.
     */
    private fun placeGems(count: Int, ease: Float) {
        var filled = (0 until N * N).filter { board[it] in 1..COLORS }.shuffled(rng)
        if (rng.nextFloat() < ease) filled = filled.sortedByDescending { lineFill(it) }
        val usedRows = HashSet<Int>()
        val picks = ArrayList<Int>()
        // First pass: one gem per row, then fill up the rest.
        for (i in filled) if (picks.size < count && usedRows.add(i / N)) picks.add(i)
        for (i in filled) if (picks.size < count && i !in picks) picks.add(i)
        for (i in picks) gems[i] = true
        gemsTotal = picks.size
    }

    /**
     * Time bombs go on colored blocks without a gem, picked among those on the fullest lines so they
     * can be defused in time; an empty cell gets a block if there are too few.
     */
    private fun placeTimers(count: Int, moves: Int) {
        val blocks = (0 until N * N).filter { board[it] in 1..COLORS && !gems[it] }
            .sortedByDescending { lineFill(it) }.take(count * 3).shuffled(rng).toMutableList()
        var placed = 0
        while (placed < count) {
            val i = blocks.removeFirstOrNull() ?: addBlock(1 + rng.nextInt(COLORS), nearFullLines = true) ?: break
            timers[i] = moves
            placed++
        }
    }

    private fun placeStones(count: Int) {
        repeat(count) { addBlock(STONE) }
    }

    /** Locks keep two rows and two columns apart, so the lines next to each one can always be cleared. */
    private fun placeLocks(count: Int) {
        // A greedy pick can paint itself into a corner, so try a few orders and keep the best.
        var best = emptyList<Int>()
        for (attempt in 0 until 30) {
            val picked = ArrayList<Int>()
            for (i in (0 until N * N).shuffled(rng)) {
                if (picked.size >= count) break
                if (board[i] != 0) continue
                val r = i / N
                val c = i % N
                if (picked.any { kotlin.math.abs(it / N - r) < 2 || kotlin.math.abs(it % N - c) < 2 }) continue
                picked.add(i)
            }
            if (picked.size > best.size) best = picked
            if (best.size >= count) break
        }
        for (i in best) board[i] = LOCK
    }

    /** Ice on empty or colored cells, one per row first so it is spread over the board. */
    private fun placeIce(count: Int) {
        val cells = (0 until N * N).filter { board[it] == 0 || board[it] in 1..COLORS }.shuffled(rng)
        val usedRows = HashSet<Int>()
        val picks = ArrayList<Int>()
        for (i in cells) if (picks.size < count && usedRows.add(i / N)) picks.add(i)
        for (i in cells) if (picks.size < count && i !in picks) picks.add(i)
        for (i in picks) ice[i] = true
        iceTotal = picks.size
    }

    /** Blocks on the fuller of the row and column through cell [i]. */
    private fun lineFill(i: Int): Int {
        val r = i / N
        val c = i % N
        return maxOf((0 until N).count { filled(board[r * N + it]) }, (0 until N).count { filled(board[it * N + c]) })
    }

    /** Puts [value] on a random empty cell (or one on the fullest lines) without completing a line. Returns its index. */
    private fun addBlock(value: Int, nearFullLines: Boolean = false): Int? {
        var cells = (0 until N * N).shuffled(rng)
        if (nearFullLines) cells = cells.sortedByDescending { lineFill(it) }
        for (i in cells) {
            if (board[i] != 0) continue
            board[i] = value
            if (fullRows(board).isEmpty() && fullCols(board).isEmpty()) return i
            board[i] = 0
        }
        return null
    }

    /** Reverts the last placement. Returns false if there is nothing to undo. */
    fun undo(): Boolean {
        val snap = undoSnapshot ?: return false
        undoSnapshot = null
        val ok = restore(snap)
        undoSnapshot = null
        return ok
    }

    /** Clears the 3x3 area centered on (r, c), obstacles included. Returns null if there was nothing to clear. */
    fun bomb(r: Int, c: Int): MoveResult? {
        val cleared = ArrayList<Pair<Cell, Int>>()
        val area = ArrayList<Int>()
        for (rr in r - 1..r + 1) for (cc in c - 1..c + 1) {
            if (rr !in 0 until N || cc !in 0 until N) continue
            val i = rr * N + cc
            area.add(i)
            if (board[i] != 0) cleared.add(Cell(rr, cc) to board[i])
        }
        if (cleared.isEmpty() && area.none { ice[it] }) return null
        val idx = cleared.map { it.first.r * N + it.first.c }
        val collected = collectGems(idx)
        val melted = meltIce(area)
        val defused = defuse(idx)
        for (i in idx) board[i] = 0
        undoSnapshot = null
        gameOver = exploded || !anyFits()
        return MoveResult(
            placed = emptyList(), color = 0, clearedRows = emptyList(), clearedCols = emptyList(),
            clearedCells = cleared, placePoints = 0, linePoints = 0, combo = combo,
            boardCleared = false, gameOver = gameOver, trayRefilled = false, gems = collected,
            iceMelted = melted, defused = defused,
        )
    }

    /** Replaces the tray with 3 new pieces (power-up). */
    fun shuffleTray() {
        undoSnapshot = null
        for (i in tray.indices) tray[i] = null
        generateTray()
        gameOver = exploded || !anyFits()
    }

    /**
     * Second chance (after a rewarded ad): clears the 3 fullest rows, gives every time bomb
     * 5 more moves and makes sure the tray has something that fits. Allowed once per game.
     */
    fun revive(): MoveResult? {
        if (!gameOver || revived) return null
        revived = true
        undoSnapshot = null
        val rowsByFill = (0 until N).sortedByDescending { row ->
            (0 until N).count { col -> board[row * N + col] != 0 } * 10 + rng.nextInt(10)
        }.take(3).sorted()
        val cleared = ArrayList<Pair<Cell, Int>>()
        val idx = ArrayList<Int>()
        for (row in rowsByFill) for (col in 0 until N) {
            val i = row * N + col
            if (board[i] != 0) cleared.add(Cell(row, col) to board[i])
            idx.add(i)
        }
        val collected = collectGems(idx)
        val melted = meltIce(idx)
        val defused = defuse(idx)
        for (i in idx) board[i] = 0
        if (exploded) {
            exploded = false
            for (i in timers.indices) if (timers[i] > 0) timers[i] += 5
        }
        if (!anyFits()) {
            for (i in tray.indices) tray[i] = null
            generateTray()
        }
        gameOver = !anyFits()
        return MoveResult(
            placed = emptyList(), color = 0, clearedRows = rowsByFill, clearedCols = emptyList(),
            clearedCells = cleared, placePoints = 0, linePoints = 0, combo = combo,
            boardCleared = false, gameOver = gameOver, trayRefilled = true, gems = collected,
            iceMelted = melted, defused = defused,
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
        val candidates = ArrayList<List<Piece>>()
        var last: List<Piece> = emptyList()
        // Only bother looking for helpful trays once there is something on the board to complete.
        val wanted = if (fill > 0.12f) 5 else 1
        for (attempt in 0 until 40) {
            val pieces = randomTriple(fill)
            last = pieces
            val ok = when {
                attempt < 25 -> canPlaceAll(board, pieces.map { it.shape }, intArrayOf(0))
                else -> pieces.any { fitsAnywhere(it.shape) }
            }
            if (ok) {
                candidates.add(pieces)
                if (candidates.size >= wanted || attempt >= 25) break
            }
        }
        val chosen = when {
            candidates.isEmpty() -> last
            candidates.size == 1 -> candidates[0]
            rng.nextFloat() < helpChance -> candidates.maxBy { helpScore(it) }
            else -> candidates[0]
        }
        for (i in 0 until 3) tray[i] = chosen[i]
    }

    /** How many lines each piece could complete right now (capped), summed over the tray. */
    private fun helpScore(pieces: List<Piece>): Int = pieces.sumOf { minOf(2, bestLines(it.shape)) }

    private fun bestLines(shape: Shape): Int {
        var best = 0
        for (r in 0..N - shape.rows) for (c in 0..N - shape.cols) {
            if (!canPlace(shape, r, c)) continue
            var lines = 0
            for (row in r until r + shape.rows) {
                if ((0 until N).all { col -> filled(board[row * N + col]) || shape.cells.any { it.r == row - r && it.c == col - c } }) lines++
            }
            for (col in c until c + shape.cols) {
                if ((0 until N).all { row -> filled(board[row * N + col]) || shape.cells.any { it.r == row - r && it.c == col - c } }) lines++
            }
            if (lines > best) best = lines
        }
        return best
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
        (0 until N).filter { row -> (0 until N).all { col -> filled(b[row * N + col]) } }

    private fun fullCols(b: IntArray): List<Int> =
        (0 until N).filter { col -> (0 until N).all { row -> filled(b[row * N + col]) } }

    // ---------------------------------------------------------------- save / restore

    fun serialize(): String {
        val trayStr = tray.joinToString(",") { p -> if (p == null) "-" else "${p.shape.id}:${p.color}" }
        return listOf(
            SAVE_V4, score, combo, movesSinceClear, if (revived) 1 else 0,
            board.joinToString("") { it.toString(16) }, trayStr, linesCleared, bits(gems), gemsCollected, gemsTotal,
            bits(ice), timers.joinToString(","), iceMelted, iceTotal, maxCombo, if (exploded) 1 else 0,
        ).joinToString(";")
    }

    private fun bits(a: BooleanArray) = a.joinToString("") { if (it) "1" else "0" }

    private fun parseBits(s: String): BooleanArray = s.map { it == '1' }.toBooleanArray().also { require(it.size == N * N) }

    /** Restores a game saved with [serialize]. Returns false (and leaves a fresh game) if the data is invalid. */
    fun restore(data: String?): Boolean {
        if (data.isNullOrBlank()) return false
        return try {
            val parts = data.split(";")
            require(
                (parts.size == 7 && parts[0] == SAVE_V1) ||
                    (parts.size == 8 && parts[0] == SAVE_V2) ||
                    (parts.size == 11 && parts[0] == SAVE_V3) ||
                    (parts.size == 17 && parts[0] == SAVE_V4)
            )
            val v4 = parts.size == 17
            val g = if (parts.size >= 11) parseBits(parts[8]) else BooleanArray(N * N)
            val b = parts[5].map { it.digitToInt(16) }.toIntArray()
            require(b.size == N * N && b.all { it in 0..LOCK })
            val t = parts[6].split(",").map { token ->
                if (token == "-") null else {
                    val (id, color) = token.split(":").map { it.toInt() }
                    Piece(requireNotNull(Shapes.byId(id)), color.coerceIn(1, COLORS))
                }
            }
            require(t.size == 3)
            val iceArr = if (v4) parseBits(parts[11]) else BooleanArray(N * N)
            val tm = if (v4) parts[12].split(",").map { it.toInt() }.toIntArray() else IntArray(N * N)
            require(tm.size == N * N)
            board = b
            score = parts[1].toInt()
            combo = parts[2].toInt()
            movesSinceClear = parts[3].toInt()
            revived = parts[4] == "1"
            linesCleared = if (parts.size >= 8) parts[7].toInt() else 0
            gems = g
            gemsCollected = if (parts.size >= 11) parts[9].toInt() else 0
            gemsTotal = if (parts.size >= 11) parts[10].toInt() else 0
            ice = iceArr
            timers = tm
            iceMelted = if (v4) parts[13].toInt() else 0
            iceTotal = if (v4) parts[14].toInt() else 0
            maxCombo = if (v4) parts[15].toInt() else combo
            exploded = v4 && parts[16] == "1"
            undoSnapshot = null
            for (i in 0 until 3) tray[i] = t[i]
            if (tray.all { it == null }) generateTray()
            gameOver = exploded || !anyFits()
            true
        } catch (e: Exception) {
            newGame()
            false
        }
    }
}
