package com.farias.prismablocks.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.farias.prismablocks.R
import com.farias.prismablocks.audio.Sfx
import com.farias.prismablocks.audio.SoundManager
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.ui.Fx
import com.farias.prismablocks.ui.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Haptic { TICK, PLACE, CLEAR, HEAVY }

enum class Mode { CLASSIC, DAILY }

enum class Power(val cost: Int) { UNDO(30), BOMB(50), SHUFFLE(40) }

/** Piece currently held by the player. [pointer] is in root coordinates. */
data class DragState(
    val slot: Int,
    val piece: Piece,
    val pointer: Offset,
    val targetRow: Int = -1,
    val targetCol: Int = -1,
    val valid: Boolean = false,
    val previewRows: List<Int> = emptyList(),
    val previewCols: List<Int> = emptyList(),
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    init {
        Progress.init(app)
    }

    private val engine = GameEngine()
    val sound = SoundManager.get(app)
    val fx = Fx()

    /** Set by the UI so the view model can trigger vibration. */
    var haptic: (Haptic) -> Unit = {}

    // ------------------------------------------------------------- observable state
    var mode by mutableStateOf(Mode.CLASSIC)
        private set
    var board by mutableStateOf(IntArray(GameEngine.N * GameEngine.N))
        private set
    var tray by mutableStateOf(listOf<Piece?>(null, null, null))
        private set
    var trayFits by mutableStateOf(listOf(true, true, true))
        private set
    var score by mutableIntStateOf(0)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var lines by mutableIntStateOf(0)
        private set
    var gameOver by mutableStateOf(false)
        private set
    /** Game over panel appears a moment after the last move, for drama. */
    var showGameOverPanel by mutableStateOf(false)
        private set
    var revived by mutableStateOf(false)
        private set
    var newRecord by mutableStateOf(false)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var paused by mutableStateOf(false)
    var drag by mutableStateOf<DragState?>(null)
        private set
    var bombMode by mutableStateOf(false)
    var coinsEarned by mutableIntStateOf(0)
        private set
    var gameOverAt = 0L
        private set

    // Daily challenge
    var dailyTarget by mutableIntStateOf(0)
        private set
    var dailyWon by mutableStateOf(false)
        private set
    var showDailyWin by mutableStateOf(false)
        private set
    var dailyStreak by mutableIntStateOf(0)
        private set
    private var dailyDay = 0

    /** No more moves for this game (lost, or the daily goal was reached). */
    val finished: Boolean get() = gameOver || dailyWon

    // ------------------------------------------------------------- geometry (root px), set by the UI
    var gridOrigin = Offset.Zero
    var cellPx = 1f
    var liftPx = 0f
    val slotRects = arrayOf(Rect.Zero, Rect.Zero, Rect.Zero)

    private var bestAtGameStart = Progress.best
    private var coinsAwardedThisGame = 0
    private var finishCounted = false
    private var panelJob: Job? = null
    /** False until a game is started from the menu, so nothing is saved for an untouched engine. */
    private var active = false

    // ------------------------------------------------------------- starting games

    fun hasClassicSave(): Boolean = Progress.has(KEY_SAVE)

    fun startClassic(continueSaved: Boolean) {
        resetSession(Mode.CLASSIC)
        if (!(continueSaved && engine.restore(Progress.loadString(KEY_SAVE)) && !engine.gameOver)) {
            engine.newGame()
            Progress.remove(KEY_SAVE)
        }
        sync()
    }

    fun startDaily() {
        resetSession(Mode.DAILY)
        dailyDay = Progress.today()
        dailyTarget = dailyTargetFor(dailyDay)
        val saved = Progress.loadString(KEY_DAILY)
        val restored = saved != null && saved.substringBefore("|") == dailyDay.toString() &&
            engine.restore(saved.substringAfter("|")) && !engine.gameOver
        if (!restored) newDailyGame()
        sync()
    }

    /** "Play again" / "Try again": a fresh game in the current mode. */
    fun restart() {
        resetSession(mode)
        if (mode == Mode.DAILY) {
            newDailyGame()
        } else {
            engine.newGame()
            Progress.remove(KEY_SAVE)
        }
        sync()
        sound.play(Sfx.WHOOSH, 0.6f)
    }

    private fun newDailyGame() {
        Progress.remove(KEY_DAILY)
        engine.newGame(seed = dailyDay.toLong() * 7919L, prefill = 16 + dailyDay % 9)
    }

    private fun resetSession(newMode: Mode) {
        active = true
        panelJob?.cancel()
        mode = newMode
        showGameOverPanel = false
        showDailyWin = false
        dailyWon = false
        newRecord = false
        paused = false
        bombMode = false
        drag = null
        coinsEarned = 0
        coinsAwardedThisGame = 0
        finishCounted = false
        bestAtGameStart = Progress.best
        fx.pops.fill(0L)
    }

    // ------------------------------------------------------------- drag & drop

    fun startDrag(slot: Int, pointer: Offset) {
        if (finished || paused || drag != null || bombMode) return
        val piece = engine.tray.getOrNull(slot) ?: return
        drag = computeTarget(DragState(slot, piece, pointer))
        sound.play(Sfx.PICK, 0.6f)
        vibrate(Haptic.TICK)
    }

    fun dragTo(pointer: Offset) {
        val d = drag ?: return
        val next = computeTarget(d.copy(pointer = pointer))
        if (next.valid && (next.targetRow != d.targetRow || next.targetCol != d.targetCol)) {
            vibrate(Haptic.TICK)
        }
        drag = next
    }

    fun endDrag() {
        val d = drag ?: return
        drag = null
        if (!d.valid) {
            sound.play(Sfx.INVALID, 0.5f)
            return
        }
        val result = engine.place(d.slot, d.targetRow, d.targetCol) ?: return
        onMove(result)
    }

    fun cancelDrag() {
        drag = null
    }

    /** Top-left of the dragged piece in root coordinates (piece floats above the finger). */
    fun dragTopLeft(d: DragState): Offset {
        val w = d.piece.shape.cols * cellPx
        val h = d.piece.shape.rows * cellPx
        return Offset(d.pointer.x - w / 2f, d.pointer.y - liftPx - h)
    }

    private fun computeTarget(d: DragState): DragState {
        val tl = dragTopLeft(d)
        val col = ((tl.x - gridOrigin.x) / cellPx).roundToInt()
        val row = ((tl.y - gridOrigin.y) / cellPx).roundToInt()
        val valid = engine.canPlace(d.piece.shape, row, col)
        if (!valid) return d.copy(targetRow = row, targetCol = col, valid = false, previewRows = emptyList(), previewCols = emptyList())
        val (rows, cols) = if (row == d.targetRow && col == d.targetCol && d.valid) {
            d.previewRows to d.previewCols
        } else {
            engine.previewLines(d.piece.shape, row, col)
        }
        return d.copy(targetRow = row, targetCol = col, valid = true, previewRows = rows, previewCols = cols)
    }

    // ------------------------------------------------------------- move outcome

    private fun onMove(r: MoveResult) {
        val ctx = getApplication<Application>()
        val color = Palette.block(r.color)
        if (!Progress.tutorialDone) Progress.setTutorialDone(true)

        for (cell in r.placed) fx.pop(cell.r * GameEngine.N + cell.c)

        if (r.lines > 0) {
            for ((cell, c) in r.clearedCells) {
                val col = Palette.block(c)
                fx.flash(cell.r * GameEngine.N + cell.c, col)
                fx.burst(cellCenter(cell.r, cell.c), col, cellPx, if (r.lines >= 3) 9 else 6)
            }
            val center = centroid(r.clearedCells.map { it.first })
            val praise = when {
                r.boardCleared -> ctx.getString(R.string.praise_board_clear)
                r.lines >= 4 -> ctx.getString(R.string.praise_4)
                r.lines == 3 -> ctx.getString(R.string.praise_3)
                r.lines == 2 -> ctx.getString(R.string.praise_2)
                else -> null
            }
            fx.text("+${r.totalPoints}", center, Palette.TextPrimary, big = false)
            if (praise != null) fx.text(praise, center.copy(y = center.y - cellPx * 1.2f), color, big = true, delayMs = 120)
            if (r.combo >= 2) {
                fx.text(ctx.getString(R.string.combo_x, r.combo), center.copy(y = center.y + cellPx * 1.1f), Palette.Gold, big = false, delayMs = 220)
            }
            if (r.lines >= 3 || r.combo >= 4 || r.boardCleared) fx.shake(cellPx * 0.22f)
            sound.play(if (r.combo >= 2 || r.lines >= 2) Sfx.COMBO else Sfx.CLEAR, 1f, 1f + 0.05f * (r.combo - 1).coerceIn(0, 8))
            vibrate(if (r.lines >= 3) Haptic.HEAVY else Haptic.CLEAR)
        } else {
            sound.play(Sfx.PLACE, 0.8f, 0.95f + (r.placed.size % 3) * 0.04f)
            vibrate(Haptic.PLACE)
        }

        if (mode == Mode.CLASSIC && !newRecord && bestAtGameStart > 0 && engine.score > bestAtGameStart) {
            newRecord = true
            fx.text(ctx.getString(R.string.new_record), Offset(gridOrigin.x + cellPx * 4f, gridOrigin.y + cellPx * 2f), Palette.Gold, big = true, delayMs = 400)
            sound.play(Sfx.RECORD, 0.9f)
        }

        if (mode == Mode.DAILY && !dailyWon && engine.linesCleared >= dailyTarget) {
            sync()
            onDailyWon()
            return
        }
        sync()
        if (r.gameOver) onGameOver()
    }

    private fun onGameOver() {
        gameOverAt = fx.now
        sound.play(Sfx.GAME_OVER, 0.9f)
        vibrate(Haptic.HEAVY)
        Progress.remove(if (mode == Mode.DAILY) KEY_DAILY else KEY_SAVE)
        if (mode == Mode.CLASSIC) {
            val total = engine.score / COIN_DIVISOR
            val add = (total - coinsAwardedThisGame).coerceAtLeast(0)
            coinsAwardedThisGame += add
            coinsEarned = coinsAwardedThisGame
            Progress.addCoins(add)
        }
        countFinish()
        panelJob?.cancel()
        panelJob = viewModelScope.launch {
            delay(900)
            showGameOverPanel = true
        }
    }

    private fun onDailyWon() {
        dailyWon = true
        bombMode = false
        dailyStreak = Progress.completeDaily()
        val reward = DAILY_REWARD + 10 * (dailyStreak - 1).coerceIn(0, 5)
        coinsEarned = reward
        Progress.addCoins(reward)
        Progress.remove(KEY_DAILY)
        countFinish()
        val ctx = getApplication<Application>()
        fx.text(ctx.getString(R.string.daily_win), Offset(gridOrigin.x + cellPx * 4f, gridOrigin.y + cellPx * 3f), Palette.Gold, big = true, delayMs = 200)
        fx.shake(cellPx * 0.2f)
        sound.play(Sfx.RECORD)
        vibrate(Haptic.HEAVY)
        panelJob?.cancel()
        panelJob = viewModelScope.launch {
            delay(1300)
            showDailyWin = true
        }
    }

    private fun countFinish() {
        if (finishCounted) return
        finishCounted = true
        Progress.onGameFinished()
    }

    // ------------------------------------------------------------- revive and power-ups

    val canRevive: Boolean get() = gameOver && !revived

    fun revive() {
        val r = engine.revive() ?: return
        showGameOverPanel = false
        for ((cell, c) in r.clearedCells) {
            val col = Palette.block(c)
            fx.flash(cell.r * GameEngine.N + cell.c, col)
            fx.burst(cellCenter(cell.r, cell.c), col, cellPx, 5)
        }
        sound.play(Sfx.REVIVE)
        vibrate(Haptic.CLEAR)
        sync()
        if (r.gameOver) onGameOver()
    }

    fun powerAvailable(p: Power): Boolean {
        if (finished || paused) return false
        return when (p) {
            Power.UNDO -> canUndo
            Power.BOMB -> board.any { it != 0 }
            Power.SHUFFLE -> true
        }
    }

    fun undo() {
        if (!engine.undo()) return
        sound.play(Sfx.WHOOSH, 0.8f, 0.9f)
        vibrate(Haptic.PLACE)
        fx.pops.fill(0L)
        sync()
    }

    fun shuffle() {
        engine.shuffleTray()
        sound.play(Sfx.WHOOSH, 0.8f, 1.15f)
        vibrate(Haptic.PLACE)
        sync()
        if (engine.gameOver) onGameOver()
    }

    /** Board cell under a tap in root coordinates, or null if outside the grid. */
    fun cellAt(root: Offset): Pair<Int, Int>? {
        val c = ((root.x - gridOrigin.x) / cellPx).toInt()
        val r = ((root.y - gridOrigin.y) / cellPx).toInt()
        if (root.x < gridOrigin.x || root.y < gridOrigin.y) return null
        if (r !in 0 until GameEngine.N || c !in 0 until GameEngine.N) return null
        return r to c
    }

    /** True if a bomb at (r, c) would destroy at least one block. */
    fun bombHits(r: Int, c: Int): Boolean {
        for (rr in r - 1..r + 1) for (cc in c - 1..c + 1) {
            if (rr in 0 until GameEngine.N && cc in 0 until GameEngine.N && board[rr * GameEngine.N + cc] != 0) return true
        }
        return false
    }

    fun bomb(r: Int, c: Int) {
        val res = engine.bomb(r, c) ?: return
        bombMode = false
        for ((cell, col) in res.clearedCells) {
            val color = Palette.block(col)
            fx.flash(cell.r * GameEngine.N + cell.c, color)
            fx.burst(cellCenter(cell.r, cell.c), color, cellPx, 10)
        }
        fx.shake(cellPx * 0.3f)
        sound.play(Sfx.BOMB)
        vibrate(Haptic.HEAVY)
        sync()
        if (res.gameOver) onGameOver()
    }

    fun playInvalid() {
        sound.play(Sfx.INVALID, 0.5f)
    }

    // ------------------------------------------------------------- persistence

    fun save() {
        if (!active || finished) return
        when (mode) {
            Mode.CLASSIC -> Progress.saveString(KEY_SAVE, engine.serialize())
            Mode.DAILY -> Progress.saveString(KEY_DAILY, "$dailyDay|${engine.serialize()}")
        }
    }

    private fun sync() {
        board = engine.board.copyOf()
        tray = engine.tray.toList()
        trayFits = engine.tray.map { it == null || engine.fitsAnywhere(it.shape) }
        score = engine.score
        combo = engine.combo
        lines = engine.linesCleared
        gameOver = engine.gameOver
        revived = engine.revived
        canUndo = engine.canUndo
        if (mode == Mode.CLASSIC) Progress.submitScore(engine.score)
        save()
    }

    // ------------------------------------------------------------- helpers

    private fun vibrate(kind: Haptic) {
        if (Progress.vibrationOn) haptic(kind)
    }

    private fun cellCenter(r: Int, c: Int) =
        Offset(gridOrigin.x + (c + 0.5f) * cellPx, gridOrigin.y + (r + 0.5f) * cellPx)

    private fun centroid(cells: List<Cell>): Offset {
        if (cells.isEmpty()) return cellCenter(4, 4)
        val r = cells.map { it.r }.average().toFloat()
        val c = cells.map { it.c }.average().toFloat()
        return Offset(gridOrigin.x + (c + 0.5f) * cellPx, gridOrigin.y + (r + 0.5f) * cellPx)
    }

    companion object {
        const val KEY_SAVE = "save"
        const val KEY_DAILY = "daily_save"
        const val COIN_DIVISOR = 40
        const val DAILY_REWARD = 60

        /** Lines to clear in the daily challenge of [day] (yyyyMMdd): 10 to 14. */
        fun dailyTargetFor(day: Int): Int = 10 + day % 5
    }
}
