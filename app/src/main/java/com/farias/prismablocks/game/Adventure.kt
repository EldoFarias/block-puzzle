package com.farias.prismablocks.game

import kotlin.math.roundToInt

/** CLEAR: clear every block the board started with. PATTERN: the same, but the blocks form a drawing. */
enum class Goal { GEMS, LINES, SCORE, ICE, PATTERN, COMBO, CLEAR }

/** Board obstacles; each world from the second one introduces one of them. */
enum class Mechanic(
    /** How much smaller the level's goals get to make room for this obstacle. */
    val ease: Float,
) { STONE(0.1f), ICE(0.1f), LOCK(0.12f), TIMER(0.12f) }

data class Objective(val goal: Goal, val target: Int)

/**
 * One adventure level. Everything is derived from the level number, so there are endless
 * levels and every player gets the same board for the same level.
 */
data class Level(
    val number: Int,
    /** One goal, or two for a boss level. All of them must be reached. */
    val objectives: List<Objective>,
    val setup: BoardSetup,
    val seed: Long,
    val helpChance: Float,
    /** Most pieces placed that still earn 3 stars / 2 stars. */
    val par3: Int,
    val par2: Int,
    /** Pieces the player may place before losing (0 = no limit). */
    val maxMoves: Int,
    val boss: Boolean,
    val world: Int,
    val mechanics: List<Mechanic>,
) {
    val goal: Goal get() = objectives[0].goal
    val hasPattern: Boolean get() = objectives.any { it.goal == Goal.PATTERN }

    fun stars(movesUsed: Int): Int = when {
        movesUsed <= par3 -> 3
        movesUsed <= par2 -> 2
        else -> 1
    }

    fun won(g: GameEngine): Boolean = objectives.all { Adventure.progress(it.goal, g) >= it.target }
}

object Adventure {
    const val WORLD_SIZE = 20
    const val BOSS_EVERY = 10
    const val CHEST_EVERY = 5
    const val STARS_PER_CHEST = 30
    const val STAR_CHEST_REWARD = 100
    /** Named worlds; from here on every world is a "Cosmos" that mixes two mechanics. */
    const val NAMED_WORLDS = 5

    /**
     * Difficulty offset by position inside each block of 10 levels: calm levels, a harder one
     * at positions 4 and 8, and the boss at 10. Keeps a rhythm instead of a flat climb.
     */
    private val WAVE = floatArrayOf(-0.12f, -0.08f, -0.04f, 0.10f, -0.08f, -0.04f, 0f, 0.12f, 0.04f, 0.10f)

    /** Mechanic pairs of the "Cosmos" worlds. Locks and time bombs together proved too harsh. */
    private val COSMOS_PAIRS = listOf(
        listOf(Mechanic.STONE, Mechanic.ICE),
        listOf(Mechanic.ICE, Mechanic.TIMER),
        listOf(Mechanic.STONE, Mechanic.LOCK),
        listOf(Mechanic.ICE, Mechanic.LOCK),
        listOf(Mechanic.STONE, Mechanic.TIMER),
    )

    private val COSMOS_THEMES = listOf("candy", "galaxy", "neon", "ocean", "gold")

    fun world(level: Int): Int = (level.coerceAtLeast(1) - 1) / WORLD_SIZE

    fun firstLevelOf(world: Int): Int = world * WORLD_SIZE + 1

    fun mechanicsOf(world: Int): List<Mechanic> = when (world) {
        0 -> emptyList()
        1 -> listOf(Mechanic.STONE)
        2 -> listOf(Mechanic.ICE)
        3 -> listOf(Mechanic.LOCK)
        4 -> listOf(Mechanic.TIMER)
        else -> COSMOS_PAIRS[(world - NAMED_WORLDS) % COSMOS_PAIRS.size]
    }

    /** Theme whose aurora colors the world's map and boards. */
    fun themeOf(world: Int): String = when (world) {
        0 -> "neon"
        1 -> "gold"
        2 -> "ocean"
        3 -> "galaxy"
        4 -> "lava"
        else -> COSMOS_THEMES[(world - NAMED_WORLDS) % COSMOS_THEMES.size]
    }

    /** True on the level where [mechanic] shows up for the first time. */
    fun introduces(level: Int, mechanic: Mechanic): Boolean {
        val w = world(level)
        return w in 1 until NAMED_WORLDS && level == firstLevelOf(w) && mechanic in mechanicsOf(w)
    }

    /** A chest sits on the trail after every [CHEST_EVERY] levels. */
    fun hasChestAfter(level: Int): Boolean = level % CHEST_EVERY == 0

    fun trailChestReward(level: Int): Int = (20 + level / CHEST_EVERY * 2).coerceAtMost(80)

    /** Coins for finishing a level: more for more stars, double for a boss, much less when replaying. */
    fun reward(stars: Int, firstTime: Boolean, boss: Boolean): Int {
        val base = if (firstTime) 15 + 10 * stars else 5 * stars
        return if (boss) base * 2 else base
    }

    fun progress(goal: Goal, g: GameEngine): Int = when (goal) {
        Goal.GEMS, Goal.PATTERN, Goal.CLEAR -> g.gemsCollected
        Goal.LINES -> g.linesCleared
        Goal.SCORE -> g.score
        Goal.ICE -> g.iceMelted
        Goal.COMBO -> g.maxCombo
    }

    fun level(n: Int): Level {
        val number = n.coerceAtLeast(1)
        val w = world(number)
        val pos = (number - 1) % BOSS_EVERY
        val boss = pos == BOSS_EVERY - 1
        // Goals stop growing around level 70; after that obstacles and the wave keep it interesting.
        val base = ((number - 1) / 90f).coerceIn(0f, 0.85f)
        val mechanics = mechanicsOf(w)
        // Obstacles already make the hard levels harder, so their peaks are softer there.
        val wave = if (WAVE[pos] > 0f && mechanics.isNotEmpty()) WAVE[pos] * 0.6f else WAVE[pos]
        val d = (base + wave).coerceIn(0f, 1.1f)
        // 0 at the first level of a world, 1 at its last: obstacles grow inside each world.
        val inWorld = ((number - 1) % WORLD_SIZE) / (WORLD_SIZE - 1f)
        // Worlds after the named ones keep getting a little denser, up to a cap.
        val extra = ((w - NAMED_WORLDS + 1).coerceAtLeast(0) * 0.25f).coerceAtMost(1f)
        val icy = Mechanic.ICE in mechanics
        // Obstacles already make a level harder, so its goals are a little smaller.
        val dt = (d - mechanics.sumOf { it.ease.toDouble() }.toFloat()).coerceAtLeast(0f)
        val seed = number * 104_729L + 17

        val goals: List<Goal> = if (boss) {
            when ((number / BOSS_EVERY) % 4) {
                1 -> listOf(if (icy) Goal.ICE else Goal.GEMS, Goal.LINES)
                2 -> listOf(Goal.PATTERN, Goal.SCORE)
                3 -> listOf(Goal.LINES, Goal.COMBO)
                else -> listOf(if (icy) Goal.ICE else Goal.GEMS, Goal.SCORE)
            }
        } else {
            listOf(
                when (pos) {
                    0, 4 -> if (icy) Goal.ICE else Goal.GEMS
                    1, 6 -> Goal.LINES
                    2 -> if (number >= 12) Goal.PATTERN else Goal.GEMS
                    3 -> Goal.SCORE
                    5 -> if (number >= 15) Goal.COMBO else Goal.LINES
                    7 -> if (icy) Goal.ICE else Goal.GEMS
                    else -> if (number >= 18) Goal.CLEAR else Goal.SCORE
                },
            )
        }
        // Two goals at once: each one a bit smaller.
        val scale = if (boss) 0.65f else 1f
        val objectives = goals.map { Objective(it, target(it, dt, scale)) }

        var prefill = goals.maxOf { prefillFor(it, dt) }
        var gems = 0
        var gemEase = 0f
        var pattern: Pattern? = null
        var ice = 0
        for (o in objectives) when (o.goal) {
            Goal.GEMS -> {
                gems = o.target
                gemEase = (1f - (number - 1) / 40f).coerceIn(0.3f, 0.95f)
                prefill = maxOf(prefill, (gems + 6 + (14 * dt).roundToInt()).coerceAtMost(30))
            }
            Goal.CLEAR -> prefill = o.target
            Goal.ICE -> ice = o.target
            Goal.PATTERN -> pattern = patternFor(number, d)
            else -> Unit
        }
        val p = pattern
        if (p != null) {
            prefill = 0
            val i = objectives.indexOfFirst { it.goal == Goal.PATTERN }
            return build(number, w, boss, mechanics, seed, d, inWorld, extra,
                objectives.toMutableList().also { it[i] = Objective(Goal.PATTERN, p.cells.size) },
                BoardSetup(pattern = p.cells, patternColor = p.color, ice = ice))
        }
        val clear = goals.contains(Goal.CLEAR)
        return build(number, w, boss, mechanics, seed, d, inWorld, extra, objectives,
            BoardSetup(prefill = prefill, gems = gems, gemEase = gemEase, ice = ice, markPrefill = clear))
    }

    /** Adds the world's obstacles, move limit and star budgets to a level. */
    private fun build(
        number: Int, w: Int, boss: Boolean, mechanics: List<Mechanic>, seed: Long,
        d: Float, inWorld: Float, extra: Float, objectives: List<Objective>, base: BoardSetup,
    ): Level {
        val amount = (inWorld + extra).coerceAtMost(1.5f)
        var setup = base
        var obstacleMoves = 0f
        for (m in mechanics) when (m) {
            Mechanic.STONE -> {
                val n = (2 + (3 * amount).roundToInt()).coerceAtMost(5)
                setup = setup.copy(stones = n)
                obstacleMoves += n * 0.8f
            }
            Mechanic.LOCK -> {
                val n = (2 + (2 * amount).roundToInt()).coerceAtMost(4)
                setup = setup.copy(locks = n)
                obstacleMoves += n * 1.2f
            }
            Mechanic.TIMER -> {
                val n = (1 + amount.roundToInt()).coerceAtMost(if (boss) 1 else 2)
                setup = setup.copy(timers = n, timerMoves = (18 - (4 * d).roundToInt()).coerceIn(14, 18))
                obstacleMoves += n * 1f
            }
            // Ice is an obstacle only through the ICE goal, which icy worlds use instead of gems.
            Mechanic.ICE -> Unit
        }
        val budget = objectives.maxOf { movesFor(it) } * (if (objectives.size > 1) 1.1f else 1f) + obstacleMoves
        val par3 = budget.roundToInt()
        val par2 = (budget * 1.6f).roundToInt()
        val pos = (number - 1) % BOSS_EVERY
        // The harder levels of each wave (and bosses) can be lost by running out of moves.
        val limited = number > 3 && (pos == 3 || pos == 7 || boss)
        return Level(
            number = number,
            objectives = objectives,
            setup = setup,
            seed = seed,
            helpChance = 0.9f - 0.35f * d.coerceAtMost(1f),
            par3 = par3,
            par2 = par2,
            maxMoves = if (limited) (par2 * (if (boss) 1.3f else 1.15f)).roundToInt() else 0,
            boss = boss,
            world = w,
            mechanics = mechanics,
        )
    }

    private fun target(goal: Goal, d: Float, scale: Float): Int {
        val t = when (goal) {
            Goal.GEMS -> (2 + 9 * d).coerceAtMost(12f)
            Goal.LINES -> (4 + 12 * d).coerceAtMost(16f)
            Goal.SCORE -> (600 + 2900 * d).coerceAtMost(3500f)
            Goal.ICE -> (5 + 9 * d).coerceAtMost(14f)
            Goal.COMBO -> 3 + 2.5f * d
            Goal.CLEAR -> 8 + 10 * d
            Goal.PATTERN -> 0f // the drawing decides
        }
        return when (goal) {
            Goal.SCORE -> ((t * scale).roundToInt() / 50) * 50
            Goal.COMBO -> t.roundToInt().coerceAtMost(5)
            else -> (t * scale).roundToInt().coerceAtLeast(2)
        }
    }

    private fun prefillFor(goal: Goal, d: Float): Int = when (goal) {
        Goal.LINES -> 6 + (8 * d).roundToInt()
        Goal.SCORE -> 6 + (10 * d).roundToInt()
        Goal.ICE -> 8 + (8 * d).roundToInt()
        Goal.COMBO -> 10 + (8 * d).roundToInt()
        Goal.GEMS, Goal.PATTERN, Goal.CLEAR -> 0
    }

    /** Pieces a line-clearing bot needs on average, calibrated with tools/AdventureBench.kt. */
    private fun movesFor(o: Objective): Float = when (o.goal) {
        Goal.GEMS -> 16f + 2f * o.target
        Goal.LINES -> 2.4f * o.target + 3f
        Goal.SCORE -> o.target / 100f + 8f
        Goal.ICE -> 18f + 3f * o.target
        Goal.PATTERN -> 4f + 2f * o.target
        Goal.COMBO -> 6f + 4f * o.target
        Goal.CLEAR -> 14f + 2.2f * o.target
    }

    // ---------------------------------------------------------------- drawings

    class Pattern(val cells: List<Cell>, val color: Int)

    private fun pattern(color: Int, vararg rows: String): Pattern {
        val cells = ArrayList<Cell>()
        rows.forEachIndexed { r, line -> line.forEachIndexed { c, ch -> if (ch == '#') cells.add(Cell(r, c)) } }
        return Pattern(cells, color)
    }

    /** Small drawings, roughly from easy to hard. None fills a whole row or column. */
    val PATTERNS: List<Pattern> = listOf(
        pattern(3, "...#....", "..###...", ".#####..", "...#....", "...#....", "...#....", "........", "........"), // arrow
        pattern(1, "........", "...#....", "..###...", ".#####..", "..###...", "...#....", "........", "........"), // diamond
        pattern(5, "........", "..#.#...", ".#####..", ".#####..", "..###...", "...#....", "........", "........"), // heart
        pattern(2, "........", "..####..", ".#....#.", ".#....#.", ".#....#.", "..####..", "........", "........"), // ring
        pattern(7, "........", ".##..##.", ".##..##.", "........", "#......#", ".#....#.", "..####..", "........"), // smile
        pattern(4, "........", "...#....", "..###...", ".#####..", ".##.##..", ".##.##..", "........", "........"), // house
        pattern(6, "........", "...##...", "...##...", ".######.", ".######.", "...##...", "...##...", "........"), // plus
        pattern(5, "#......#", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "#......#"), // X
    )

    private fun patternFor(number: Int, d: Float): Pattern {
        // Harder drawings as the difficulty grows, with some variety between neighbours.
        val top = (2 + (d * (PATTERNS.size - 2)).roundToInt()).coerceIn(2, PATTERNS.size)
        return PATTERNS[(number * 7 + 3) % top]
    }
}
