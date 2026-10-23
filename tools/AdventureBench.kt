import com.farias.prismablocks.game.*
import kotlin.random.Random

/**
 * Greedy bot: prefers moves that clear the most lines, weighted by what they collect
 * (gems, ice, time bombs), builds up the lines of time bombs, avoids moves that leave the
 * rest of the tray without room, else random. Returns (won, moves used).
 */
fun runLevel(lv: Level, botSeed: Int): Pair<Boolean, Int> {
    val g = GameEngine(); g.helpChance = lv.helpChance
    g.newGame(seed = lv.seed, setup = lv.setup)
    val rng = Random(botSeed)
    var moves = 0
    while (!g.gameOver && !lv.won(g) && moves < 300) {
        var best: Triple<Int, Int, Int>? = null; var bestV = Double.NEGATIVE_INFINITY
        val all = ArrayList<Triple<Int, Int, Int>>()
        for (s in 0..2) {
            val p = g.tray[s] ?: continue
            for (r in 0 until 8) for (c in 0 until 8) if (g.canPlace(p.shape, r, c)) {
                all.add(Triple(s, r, c))
                val (rr, cc) = g.previewLines(p.shape, r, c)
                val hit = HashSet<Int>()
                for (row in rr) for (col in 0 until 8) hit.add(row * 8 + col)
                for (col in cc) for (row in 0 until 8) hit.add(row * 8 + col)
                var bonus = 0.0
                for (i in hit) {
                    if (g.gems[i]) bonus += 1.5
                    if (g.ice[i]) bonus += 1.2
                    if (g.timers[i] > 0) bonus += 4.0
                    if (g.board[i] == GameEngine.STONE || g.board[i] == GameEngine.CRACKED) bonus += 0.5
                }
                // Like a person would: don't leave the other pieces of the tray without room.
                val after = g.board.copyOf()
                for (cell in p.shape.cells) after[(r + cell.r) * 8 + c + cell.c] = 1
                val stuck = (0..2).count { o -> o != s && g.tray[o]?.let { !g.fitsAnywhere(it.shape, after) } == true }
                // Builds up the lines of a time bomb, more urgently as it counts down.
                var defuse = 0.0
                for (cell in p.shape.cells) {
                    val pr = r + cell.r; val pc = c + cell.c
                    for (k in 0 until 8) {
                        val t = maxOf(g.timers[pr * 8 + k], g.timers[k * 8 + pc])
                        if (t > 0) defuse += 0.25 + (20 - t).coerceAtLeast(0) * 0.03
                    }
                }
                val v = rr.size + cc.size + bonus + defuse - stuck * 3 + rng.nextDouble() * 0.1
                if (v > bestV) { bestV = v; best = Triple(s, r, c) }
            }
        }
        val m = best ?: all[rng.nextInt(all.size)]
        g.place(m.first, m.second, m.third); moves++
    }
    return lv.won(g) to moves
}

fun main(args: Array<String>) {
    val levels = if (args.isNotEmpty()) args.map { it.toInt() }
    else listOf(1, 2, 3, 4, 5, 8, 9, 10, 12, 13, 15, 18, 19, 20, 21, 22, 24, 25, 30, 40, 41, 43, 47, 50,
        60, 61, 63, 64, 68, 70, 80, 81, 84, 88, 90, 100, 101, 104, 110, 130, 150)
    println("lvl  goals                    obst      | win% inLimit avgMoves | par3 par2 limit | stars 3/2/1")
    for (n in levels) {
        val l = Adventure.level(n)
        val runs = (1..40).map { runLevel(l, it) }
        val wins = runs.filter { it.first }
        val inLimit = wins.count { l.maxMoves == 0 || it.second <= l.maxMoves }
        val avg = if (wins.isEmpty()) 0 else wins.map { it.second }.average().toInt()
        val st = wins.map { l.stars(it.second) }
        val goals = l.objectives.joinToString("+") { "${it.goal}${it.target}" }
        val s = l.setup
        val obst = "s${s.stones} l${s.locks} t${s.timers}/${s.timerMoves}"
        println("%3d%s %-24s %-9s | %3d%% %3d%% %4d | %3d %3d %3d | %d/%d/%d".format(n, if (l.boss) "B" else " ", goals, obst,
            wins.size * 100 / runs.size, inLimit * 100 / runs.size, avg, l.par3, l.par2, l.maxMoves,
            st.count { it == 3 }, st.count { it == 2 }, st.count { it == 1 }))
    }
}
