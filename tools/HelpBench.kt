import com.farias.prismablocks.game.*
import kotlin.random.Random

/** Greedy-ish player: prefers moves that clear lines, otherwise random. Compares help on/off. */
fun play(help: Float, seed: Int): Triple<Int, Int, Int> {
    val g = GameEngine(Random(seed)); g.helpChance = help; g.newGame(seed = seed.toLong())
    g.helpChance = help
    val rng = Random(seed * 31)
    var moves = 0
    while (!g.gameOver && moves < 400) {
        var bestMove: Triple<Int, Int, Int>? = null; var bestL = -1
        val all = ArrayList<Triple<Int, Int, Int>>()
        for (s in 0..2) { val p = g.tray[s] ?: continue
            for (r in 0 until 8) for (c in 0 until 8) if (g.canPlace(p.shape, r, c)) {
                all.add(Triple(s, r, c))
                val (rr, cc) = g.previewLines(p.shape, r, c); val l = rr.size + cc.size
                if (l > bestL) { bestL = l; bestMove = Triple(s, r, c) } } }
        val m = if (bestL > 0) bestMove!! else all[rng.nextInt(all.size)]
        g.place(m.first, m.second, m.third); moves++
    }
    return Triple(moves, g.linesCleared, g.score)
}

fun main() {
    for (h in listOf(0f, 0.65f, 1f)) {
        val t0 = System.currentTimeMillis()
        val res = (1..200).map { play(h, it) }
        val dt = System.currentTimeMillis() - t0
        println("help=$h avgMoves=${res.map{it.first}.average().toInt()} avgLines=${"%.1f".format(res.map{it.second}.average())} avgScore=${res.map{it.third}.average().toInt()} msPerMove=${"%.2f".format(dt.toDouble()/res.sumOf{it.first})}")
    }
}
