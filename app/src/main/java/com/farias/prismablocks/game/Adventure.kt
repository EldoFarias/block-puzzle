package com.farias.prismablocks.game

import kotlin.math.roundToInt

enum class Goal { GEMS, LINES, SCORE }

/**
 * One adventure level. Everything is derived from the level number, so there are endless
 * levels and every player gets the same board for the same level.
 */
data class Level(
    val number: Int,
    val goal: Goal,
    /** Gems to collect, lines to clear or points to score. */
    val target: Int,
    val prefill: Int,
    val gems: Int,
    val seed: Long,
    val helpChance: Float,
    /** 0..1: how much gems favour nearly-complete lines (easier early levels). */
    val gemEase: Float,
    /** Most pieces placed that still earn 3 stars / 2 stars. */
    val par3: Int,
    val par2: Int,
) {
    fun stars(movesUsed: Int): Int = when {
        movesUsed <= par3 -> 3
        movesUsed <= par2 -> 2
        else -> 1
    }
}

object Adventure {

    fun level(n: Int): Level {
        val number = n.coerceAtLeast(1)
        val d = ((number - 1) / 60f).coerceIn(0f, 1f) // 0 at level 1, 1 from level 61
        val goal = when (number % 4) {
            1 -> Goal.GEMS
            2 -> Goal.LINES
            3 -> Goal.GEMS
            else -> Goal.SCORE
        }
        val seed = number * 104_729L + 17
        val help = 0.9f - 0.35f * d
        val ease = (1f - (number - 1) / 40f).coerceIn(0.3f, 0.95f)
        return when (goal) {
            Goal.GEMS -> {
                val gems = (2 + number / 4).coerceAtMost(12)
                val prefill = (gems + 6 + (14 * d).roundToInt()).coerceAtMost(30)
                Level(number, goal, gems, prefill, gems, seed, help, ease, par(goal, gems, 3), par(goal, gems, 2))
            }
            Goal.LINES -> {
                val target = (4 + number / 3).coerceAtMost(16)
                val prefill = 6 + (8 * d).roundToInt()
                Level(number, goal, target, prefill, 0, seed, help, ease, par(goal, target, 3), par(goal, target, 2))
            }
            Goal.SCORE -> {
                val target = ((600 + number * 50).coerceAtMost(3500) / 50) * 50
                val prefill = 6 + (10 * d).roundToInt()
                Level(number, goal, target, prefill, 0, seed, help, ease, par(goal, target, 3), par(goal, target, 2))
            }
        }
    }

    /** Move budgets for stars, calibrated with simulated games of a line-clearing bot. */
    private fun par(goal: Goal, target: Int, stars: Int): Int {
        val base = when (goal) {
            Goal.GEMS -> 16f + 2f * target
            Goal.LINES -> 2.4f * target + 3f
            Goal.SCORE -> target / 100f + 8f
        }
        return (if (stars == 3) base else base * 1.6f).roundToInt()
    }

    /** Coins for finishing a level: more for more stars, much less when replaying. */
    fun reward(stars: Int, firstTime: Boolean): Int = if (firstTime) 15 + 10 * stars else 5 * stars
}
