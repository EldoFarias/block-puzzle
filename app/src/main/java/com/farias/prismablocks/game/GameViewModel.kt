package com.farias.prismablocks.game

import android.app.Application
import android.content.Context
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
import com.farias.prismablocks.ui.Fx
import com.farias.prismablocks.ui.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Haptic { TICK, PLACE, CLEAR, HEAVY }

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

    private val prefs = app.getSharedPreferences("prisma_blocks", Context.MODE_PRIVATE)
    private val engine = GameEngine()
    val sound = SoundManager(app)
    val fx = Fx()

    /** Set by the UI so the view model can trigger vibration. */
    var haptic: (Haptic) -> Unit = {}

    // ------------------------------------------------------------- observable state
    var board by mutableStateOf(IntArray(GameEngine.N * GameEngine.N))
        private set
    var tray by mutableStateOf(listOf<Piece?>(null, null, null))
        private set
    var trayFits by mutableStateOf(listOf(true, true, true))
        private set
    var score by mutableIntStateOf(0)
        private set
    var best by mutableIntStateOf(prefs.getInt(KEY_BEST, 0))
        private set
    var combo by mutableIntStateOf(0)
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
    var paused by mutableStateOf(false)
    var drag by mutableStateOf<DragState?>(null)
        private set
    var gameOverAt = 0L
        private set

    var soundOn by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set
    var vibrationOn by mutableStateOf(prefs.getBoolean(KEY_VIBRATION, true))
        private set

    // ------------------------------------------------------------- geometry (root px), set by the UI
    var gridOrigin = Offset.Zero
    var cellPx = 1f
    var liftPx = 0f
    val slotRects = arrayOf(Rect.Zero, Rect.Zero, Rect.Zero)

    private var bestAtGameStart = best
    private var gameOverJob: Job? = null

    init {
        sound.enabled = soundOn
        if (!engine.restore(prefs.getString(KEY_SAVE, null)) || engine.gameOver) {
            engine.newGame()
        }
        sync()
    }

    // ------------------------------------------------------------- drag & drop

    fun startDrag(slot: Int, pointer: Offset) {
        if (gameOver || paused || drag != null) return
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

        if (!newRecord && bestAtGameStart > 0 && engine.score > bestAtGameStart) {
            newRecord = true
            fx.text(ctx.getString(R.string.new_record), Offset(gridOrigin.x + cellPx * 4f, gridOrigin.y + cellPx * 2f), Palette.Gold, big = true, delayMs = 400)
            sound.play(Sfx.RECORD, 0.9f)
        }
        sync()
        if (r.gameOver) onGameOver()
    }

    private fun onGameOver() {
        gameOverAt = fx.now
        sound.play(Sfx.GAME_OVER, 0.9f)
        vibrate(Haptic.HEAVY)
        prefs.edit().remove(KEY_SAVE).apply()
        gameOverJob?.cancel()
        gameOverJob = viewModelScope.launch {
            delay(900)
            showGameOverPanel = true
        }
    }

    // ------------------------------------------------------------- actions

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

    fun restart() {
        gameOverJob?.cancel()
        engine.newGame()
        showGameOverPanel = false
        newRecord = false
        paused = false
        drag = null
        bestAtGameStart = best
        fx.pops.fill(0L)
        prefs.edit().remove(KEY_SAVE).apply()
        sync()
        // Animate the fresh tray in.
        sound.play(Sfx.REVIVE, 0.5f, 1.2f)
    }

    fun toggleSound() {
        soundOn = !soundOn
        sound.enabled = soundOn
        prefs.edit().putBoolean(KEY_SOUND, soundOn).apply()
    }

    fun toggleVibration() {
        vibrationOn = !vibrationOn
        prefs.edit().putBoolean(KEY_VIBRATION, vibrationOn).apply()
        if (vibrationOn) haptic(Haptic.PLACE)
    }

    fun save() {
        if (!engine.gameOver) prefs.edit().putString(KEY_SAVE, engine.serialize()).apply()
    }

    // ------------------------------------------------------------- helpers

    private fun sync() {
        board = engine.board.copyOf()
        tray = engine.tray.toList()
        trayFits = engine.tray.map { it == null || engine.fitsAnywhere(it.shape) }
        score = engine.score
        combo = engine.combo
        gameOver = engine.gameOver
        revived = engine.revived
        if (engine.score > best) {
            best = engine.score
            prefs.edit().putInt(KEY_BEST, best).apply()
        }
        save()
    }

    private fun vibrate(kind: Haptic) {
        if (vibrationOn) haptic(kind)
    }

    private fun cellCenter(r: Int, c: Int) =
        Offset(gridOrigin.x + (c + 0.5f) * cellPx, gridOrigin.y + (r + 0.5f) * cellPx)

    private fun centroid(cells: List<Cell>): Offset {
        if (cells.isEmpty()) return cellCenter(4, 4)
        val r = cells.map { it.r }.average().toFloat()
        val c = cells.map { it.c }.average().toFloat()
        return Offset(gridOrigin.x + (c + 0.5f) * cellPx, gridOrigin.y + (r + 0.5f) * cellPx)
    }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }

    companion object {
        private const val KEY_SAVE = "save"
        private const val KEY_BEST = "best"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATION = "vibration"
    }
}
