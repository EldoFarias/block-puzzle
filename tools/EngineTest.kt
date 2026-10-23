import com.farias.prismablocks.game.*
import kotlin.random.Random

fun check(cond: Boolean, msg: String) { if (!cond) throw AssertionError(msg) }

fun main() {
    check(Shapes.all.size == 39, "shapes ${Shapes.all.size}")
    check(Shapes.all.all { s -> s.cells.minOf { it.r } == 0 && s.cells.minOf { it.c } == 0 }, "normalized")

    // Row clear + scoring
    val e = GameEngine(Random(1))
    // fill row 0 except last cell via restore
    val boardStr = "1111111" + "0".repeat(57)
    val single = Shapes.all.indexOfFirst { it.size == 1 }
    check(e.restore("v1;0;0;0;0;$boardStr;$single:3,-,-"), "restore")
    val res = e.place(0, 0, 7)!!
    check(res.clearedRows == listOf(0), "cleared row ${res.clearedRows}")
    check(res.boardCleared, "board cleared")
    check(res.linePoints == 80 + GameEngine.BOARD_CLEAR_BONUS, "line points ${res.linePoints}")
    check(res.trayRefilled, "refilled")
    check(e.tray.all { it != null }, "tray full")

    // Serialization round trip
    val s = e.serialize()
    val e2 = GameEngine(Random(2))
    check(e2.restore(s), "roundtrip")
    check(e2.serialize() == s, "same")

    // Bad data falls back to new game
    check(!e2.restore("garbage"), "bad")
    check(e2.score == 0, "reset")

    // v1 saves still load
    val e3 = GameEngine(Random(3))
    check(e3.restore("v1;10;0;0;0;" + "0".repeat(64) + ";$single:3,-,-"), "v1 compat")
    check(e3.score == 10 && e3.linesCleared == 0, "v1 fields")

    // Undo restores the exact previous state
    val e4 = GameEngine(Random(4))
    val before = e4.serialize()
    val slot = e4.tray.indexOfFirst { it != null }
    var placedOk = false
    loop@ for (r in 0 until 8) for (c in 0 until 8) if (e4.place(slot, r, c) != null) { placedOk = true; break@loop }
    check(placedOk && e4.canUndo, "can undo")
    check(e4.undo(), "undo ok")
    check(e4.serialize() == before, "undo exact")
    check(!e4.canUndo, "single undo")

    // Bomb clears a 3x3 area (clipped at the corner)
    val e5 = GameEngine(Random(5))
    check(e5.restore("v2;0;0;0;0;" + "1".repeat(7) + "0" + "2".repeat(7) + "0" + "0".repeat(48) + ";$single:3,-,-;0"), "restore bomb")
    val b = e5.bomb(0, 0)!!
    check(b.clearedCells.size == 4, "bomb corner ${b.clearedCells.size}")
    check(e5.bomb(7, 7) == null, "bomb empty")

    // Shuffle refills all 3 slots
    e5.shuffleTray()
    check(e5.tray.all { it != null }, "shuffle")

    // Daily challenge is deterministic and pre-filled without full lines
    val d1 = GameEngine(); d1.newGame(seed = 20261023L, prefill = 22)
    val d2 = GameEngine(); d2.newGame(seed = 20261023L, prefill = 22)
    check(d1.serialize() == d2.serialize(), "daily deterministic")
    check(d1.board.count { it != 0 } == 22, "prefill count ${d1.board.count { it != 0 }}")
    check(!d1.gameOver, "daily playable")
    val d3 = GameEngine(); d3.newGame(seed = 20261024L, prefill = 22)
    check(d3.serialize() != d1.serialize(), "different days differ")

    // ---- adventure obstacles (boards built with the v4 save format)
    val zeros = "0".repeat(64)
    val noTimers = List(64) { 0 }
    fun v4(board: String, tray: String, ice: String = zeros, timers: List<Int> = noTimers, gems: String = zeros) =
        "v4;0;0;0;0;$board;$tray;0;$gems;0;${gems.count { it == '1' }};$ice;${timers.joinToString(",")};0;${ice.count { it == '1' }};0;0"
    val one = "$single:3,-,-"
    val two = "$single:3,$single:4,-"

    // Stone: the first clear cracks it, the second removes it
    val st = GameEngine(Random(6))
    check(st.restore(v4("8" + "1".repeat(6) + "0" + "0".repeat(56), one)), "restore stone")
    val st1 = st.place(0, 0, 7)!!
    check(st1.clearedRows == listOf(0) && st1.cracked == listOf(Cell(0, 0)), "stone cracked")
    check(st.board[0] == GameEngine.CRACKED && st.board.drop(1).all { it == 0 }, "only the stone stays")
    check(st.restore(v4("9" + "1".repeat(6) + "0" + "0".repeat(56), one)), "restore cracked")
    st.place(0, 0, 7)
    check(st.board.all { it == 0 }, "cracked stone removed")

    // Lock: its row can't be completed; a clear next to it opens it
    val lk = GameEngine(Random(7))
    check(lk.restore(v4("a" + "1".repeat(6) + "0" + "0".repeat(56), one)), "restore lock row")
    check(lk.place(0, 0, 7)!!.lines == 0, "a locked cell doesn't complete its row")
    check(lk.restore(v4("a" + "0".repeat(7) + "1".repeat(7) + "0" + "0".repeat(48), one)), "restore lock")
    check(!lk.canPlace(Shapes.all[single], 0, 0), "nothing fits on a lock")
    val lk1 = lk.place(0, 1, 7)!!
    check(lk1.unlocked == listOf(Cell(0, 0)) && lk.board[0] == 0, "unlocked by the clear below")

    // Ice melts when a cleared line goes through it
    val ic = GameEngine(Random(8))
    val iceStr = zeros.toCharArray().also { it[3] = '1'; it[45] = '1' }.concatToString()
    check(ic.restore(v4("1".repeat(7) + "0" + "0".repeat(56), one, ice = iceStr)), "restore ice")
    val ic1 = ic.place(0, 0, 7)!!
    check(ic1.iceMelted == listOf(Cell(0, 3)) && ic.iceMelted == 1 && !ic.ice[3] && ic.ice[45], "ice melted")

    // Time bomb: defused by clearing it, goes off when its counter reaches zero
    val tb = GameEngine(Random(9))
    check(tb.restore(v4("1".repeat(7) + "0" + "0".repeat(56), one, timers = noTimers.toMutableList().also { it[0] = 3 })), "restore timer")
    val tb1 = tb.place(0, 0, 7)!!
    check(tb1.defused == listOf(Cell(0, 0)) && tb.timers[0] == 0 && !tb.exploded, "defused")
    // Rows 5 and 6 are the fullest, so the revive clears them (and row 0) and keeps the bomb on row 3.
    val bombBoard = "0".repeat(24) + "00010000" + "0".repeat(8) + "11110000" + "11110000" + "00000001"
    check(tb.restore(v4(bombBoard, two, timers = noTimers.toMutableList().also { it[63] = 2; it[27] = 9 })), "restore ticking")
    check(tb.place(0, 0, 0)!!.exploded == null && tb.timers[63] == 1, "tick")
    val tb2 = tb.place(1, 0, 2)!!
    check(tb2.exploded == Cell(7, 7) && tb.exploded && tb.gameOver && tb2.gameOver, "exploded")
    check(tb.revive() != null && !tb.exploded && !tb.gameOver && tb.timers[27] == 12, "revive gives the bombs more time")

    // v4 round trip keeps every layer; undo restores them too
    val full = GameEngine(Random(10))
    full.newGame(seed = 99L, setup = BoardSetup(prefill = 14, gems = 3, stones = 2, locks = 2, ice = 5, timers = 2, timerMoves = 9))
    val fs = full.serialize()
    val full2 = GameEngine(Random(11))
    check(full2.restore(fs) && full2.serialize() == fs, "v4 roundtrip")
    check(full.iceTotal == 5 && full.gemsTotal == 3 && full.timers.count { it == 9 } == 2, "setup layers")
    check(full.board.count { it == GameEngine.STONE } == 2 && full.board.count { it == GameEngine.LOCK } == 2, "setup obstacles")
    val fslot = full.tray.indexOfFirst { it != null }
    loop2@ for (r in 0 until 8) for (c in 0 until 8) if (full.place(fslot, r, c) != null) break@loop2
    check(full.undo() && full.serialize() == fs, "undo restores obstacles and timers")

    // Every adventure level builds a playable board that matches its goals
    for (n in 1..300) {
        val lv = Adventure.level(n)
        val g = GameEngine()
        g.newGame(seed = lv.seed, setup = lv.setup)
        check(!g.gameOver && !lv.won(g), "level $n playable")
        for (o in lv.objectives) when (o.goal) {
            Goal.GEMS, Goal.PATTERN, Goal.CLEAR -> check(g.gemsTotal == o.target, "level $n ${o.goal} ${g.gemsTotal} vs ${o.target}")
            Goal.ICE -> check(g.iceTotal == o.target, "level $n ice ${g.iceTotal} vs ${o.target}")
            else -> Unit
        }
        val locks = (0 until 64).filter { g.board[it] == GameEngine.LOCK }
        check(locks.size == lv.setup.locks, "level $n locks ${locks.size} vs ${lv.setup.locks}")
        for (a in locks) for (b in locks) if (a < b) check(kotlin.math.abs(a / 8 - b / 8) >= 2 && kotlin.math.abs(a % 8 - b % 8) >= 2, "level $n locks apart")
        check(g.board.count { it == GameEngine.STONE } == lv.setup.stones, "level $n stones")
        check(g.timers.count { it > 0 } == lv.setup.timers, "level $n timers")
        check(lv.boss == (n % 10 == 0) && lv.objectives.size == (if (lv.boss) 2 else 1), "level $n boss")
    }

    // Simulate many random games: no crashes, game ends, scores sane
    val rng = Random(42)
    var totalMoves = 0; var maxScore = 0
    val t0 = System.currentTimeMillis()
    repeat(300) {
        val g = GameEngine(Random(rng.nextInt()))
        var guard = 0
        while (!g.gameOver && guard < 2000) {
            guard++
            val moves = ArrayList<Triple<Int, Int, Int>>()
            for (slot in 0..2) { val p = g.tray[slot] ?: continue
                for (r in 0 until 8) for (c in 0 until 8) if (g.canPlace(p.shape, r, c)) moves.add(Triple(slot, r, c)) }
            check(moves.isNotEmpty(), "not game over but no moves")
            val m = moves[rng.nextInt(moves.size)]
            val before = g.score
            val res2 = g.place(m.first, m.second, m.third)!!
            check(g.score == before + res2.totalPoints, "score accounting")
            check(g.linesCleared >= res2.lines, "lines")
            totalMoves++
        }
        if (g.gameOver && !g.revived) { val rv = g.revive(); check(rv != null, "revive"); check(g.revived, "revived flag") }
        maxScore = maxOf(maxScore, g.score)
    }
    val dt = System.currentTimeMillis() - t0
    println("OK moves=$totalMoves avgMoves=${totalMoves / 300} maxScore=$maxScore timeMs=$dt msPerMove=${"%.3f".format(dt.toDouble() / totalMoves)}")
}
