package com.farias.prismablocks.data

import androidx.annotation.StringRes
import com.farias.prismablocks.R
import kotlin.random.Random

enum class MissionType(@StringRes val textRes: Int) {
    LINES(R.string.mission_lines),
    PIECES(R.string.mission_pieces),
    COMBO(R.string.mission_combo),
    SCORE(R.string.mission_score),
    GAMES(R.string.mission_games),
    GEMS(R.string.mission_gems),
    LEVELS(R.string.mission_levels),
}

data class Mission(val type: MissionType, val target: Int, val reward: Int)

/** Three daily missions, the same for every player on the same day. */
object Missions {
    const val COUNT = 3

    fun forDay(day: Int): List<Mission> {
        val rnd = Random(day * 31L + 7)
        return MissionType.entries.shuffled(rnd).take(COUNT).map { type ->
            val tier = rnd.nextInt(3) // 0 easy, 1 medium, 2 hard
            val target = when (type) {
                MissionType.LINES -> listOf(15, 25, 40)[tier]
                MissionType.PIECES -> listOf(40, 70, 110)[tier]
                MissionType.COMBO -> listOf(3, 4, 5)[tier]
                MissionType.SCORE -> listOf(1000, 2000, 3500)[tier]
                MissionType.GAMES -> listOf(2, 3, 5)[tier]
                MissionType.GEMS -> listOf(5, 10, 16)[tier]
                MissionType.LEVELS -> listOf(1, 2, 3)[tier]
            }
            Mission(type, target, listOf(30, 45, 70)[tier])
        }
    }
}
