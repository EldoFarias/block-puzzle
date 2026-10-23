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
