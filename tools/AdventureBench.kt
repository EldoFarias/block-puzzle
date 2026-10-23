import com.farias.prismablocks.game.*
import kotlin.random.Random

fun progress(g: GameEngine, lv: Level) = when (lv.goal) {
    Goal.GEMS -> g.gemsCollected; Goal.LINES -> g.linesCleared; Goal.SCORE -> g.score }

/** Greedy bot: prefers moves that clear the most lines (and gems), else random. */
fun runLevel(lv: Level, botSeed: Int): Pair<Boolean, Int> {
    val g = GameEngine(); g.helpChance = lv.helpChance
    g.newGame(seed = lv.seed, prefill = lv.prefill, gemCount = lv.gems, gemEase = lv.gemEase)
    check(g.gemsTotal == lv.gems) { "gems ${g.gemsTotal} vs ${lv.gems}" }
    val rng = Random(botSeed)
    var moves = 0
    while (!g.gameOver && progress(g, lv) < lv.target && moves < 300) {
        var best: Triple<Int,Int,Int>? = null; var bestV = -1.0
        val all = ArrayList<Triple<Int,Int,Int>>()
        for (s in 0..2) { val p = g.tray[s] ?: continue
            for (r in 0 until 8) for (c in 0 until 8) if (g.canPlace(p.shape, r, c)) {
                all.add(Triple(s,r,c))
                val (rr, cc) = g.previewLines(p.shape, r, c)
                var gemsHit = 0
                for (row in rr) for (col in 0 until 8) if (g.gems[row*8+col]) gemsHit++
                for (col in cc) for (row in 0 until 8) if (g.gems[row*8+col]) gemsHit++
                val v = rr.size + cc.size + gemsHit * 1.5 + rng.nextDouble() * 0.1
                if (v > bestV) { bestV = v; best = Triple(s,r,c) } } }
        val m = if (bestV >= 1.0) best!! else all[rng.nextInt(all.size)]
        g.place(m.first, m.second, m.third); moves++
    }
    return (progress(g, lv) >= lv.target) to moves
}

fun main() {
    // Serialization round trip with gems
    val lv = Adventure.level(5)
    val e = GameEngine(); e.newGame(lv.seed, lv.prefill, lv.gems)
    val s = e.serialize(); val e2 = GameEngine(); check(e2.restore(s)); check(e2.serialize() == s) { "v3 roundtrip" }
    check(e2.gems.count { it } == lv.gems)
    println("level goal target prefill | win% avgMoves(wins) | par3 par2 | stars dist(3/2/1)")
    for (n in listOf(1,2,3,4,5,6,7,8,10,12,16,20,24,30,40,50,60,61,80,100)) {
        val l = Adventure.level(n)
        val runs = (1..40).map { runLevel(l, it) }
        val wins = runs.filter { it.first }
        val avg = if (wins.isEmpty()) 0 else wins.map { it.second }.average().toInt()
        val st = wins.map { l.stars(it.second) }
        println("%3d %-5s %5d %3d | %3d%% %3d | %3d %3d | %d/%d/%d".format(n, l.goal, l.target, l.prefill,
            wins.size * 100 / runs.size, avg, l.par3, l.par2, st.count{it==3}, st.count{it==2}, st.count{it==1}))
    }
}
