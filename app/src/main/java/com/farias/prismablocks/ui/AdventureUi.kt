package com.farias.prismablocks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farias.prismablocks.R
import com.farias.prismablocks.game.Adventure
import com.farias.prismablocks.game.Goal
import com.farias.prismablocks.game.Mechanic
import com.farias.prismablocks.game.Objective

/** Accent color of a level node on the map, by its (first) goal. */
fun goalColor(goal: Goal): Color = when (goal) {
    Goal.GEMS -> Palette.Cyan
    Goal.LINES -> Color(0xFFA3E635)
    Goal.SCORE -> Palette.Magenta
    Goal.ICE -> Color(0xFF93C5FD)
    Goal.PATTERN -> Color(0xFFF472B6)
    Goal.CLEAR -> Color(0xFFFBBF24)
    Goal.COMBO -> Color(0xFFFF9100)
}

/** Background of the adventure: the aurora of the world [world]. */
fun worldTheme(world: Int): GameTheme = Themes.byId(Adventure.themeOf(world))

@Composable
fun worldName(world: Int): String = stringResource(
    when (world) {
        0 -> R.string.world_aurora
        1 -> R.string.world_canyon
        2 -> R.string.world_glacier
        3 -> R.string.world_vault
        4 -> R.string.world_volcano
        else -> R.string.world_cosmos
    },
)

/** "Collect 5 gems", "Melt 8 ice tiles"... */
@Composable
fun goalText(o: Objective): String = stringResource(
    when (o.goal) {
        Goal.GEMS -> R.string.goal_gems
        Goal.LINES -> R.string.goal_lines
        Goal.SCORE -> R.string.goal_score
        Goal.ICE -> R.string.goal_ice
        Goal.PATTERN -> R.string.goal_pattern
        Goal.CLEAR -> R.string.goal_clear
        Goal.COMBO -> R.string.goal_combo
    },
    o.target,
)

/** Short label next to a progress bar: "Gems 3/5", "Goal 1500"... */
@Composable
fun progressText(o: Objective, current: Int): String {
    val c = current.coerceAtMost(o.target)
    return when (o.goal) {
        Goal.GEMS -> stringResource(R.string.gems_progress, c, o.target)
        Goal.LINES -> stringResource(R.string.lines_progress, c, o.target)
        Goal.SCORE -> stringResource(R.string.points_progress, o.target)
        Goal.ICE -> stringResource(R.string.ice_progress, c, o.target)
        Goal.PATTERN, Goal.CLEAR -> stringResource(R.string.blocks_progress, c, o.target)
        Goal.COMBO -> stringResource(R.string.combo_progress, c, o.target)
    }
}

/** What was missing when the level was lost: "3 gems to go!"... */
@Composable
fun missingText(o: Objective, current: Int): String {
    val left = (o.target - current).coerceAtLeast(1)
    return when (o.goal) {
        Goal.GEMS -> stringResource(R.string.left_gems, left)
        Goal.LINES -> stringResource(R.string.daily_fail, left)
        Goal.SCORE -> stringResource(R.string.left_points, left)
        Goal.ICE -> stringResource(R.string.left_ice, left)
        Goal.PATTERN, Goal.CLEAR -> stringResource(R.string.left_blocks, left)
        Goal.COMBO -> stringResource(R.string.left_combo, o.target)
    }
}

@Composable
fun mechanicText(m: Mechanic): String = stringResource(
    when (m) {
        Mechanic.STONE -> R.string.mech_stone
        Mechanic.ICE -> R.string.mech_ice
        Mechanic.LOCK -> R.string.mech_lock
        Mechanic.TIMER -> R.string.mech_timer
    },
)

/** Small picture of a goal, drawn with the same blocks as the board. */
@Composable
fun GoalIcon(goal: Goal, modifier: Modifier = Modifier.size(30.dp)) {
    when (goal) {
        Goal.SCORE -> CrownIcon(modifier)
        Goal.COMBO -> Box(modifier, contentAlignment = Alignment.Center) {
            Text("🔥", style = Styles.plain(20, Color.White))
        }
        else -> Canvas(modifier) {
            val w = size.width
            when (goal) {
                Goal.GEMS -> drawGemIcon(center, w)
                Goal.LINES -> for (i in 0 until 4) drawGem(Offset(i * w / 4f, size.height * 0.35f), w / 4f, Palette.block(3), glow = false)
                Goal.ICE -> {
                    drawEmptyCell(Offset.Zero, w)
                    drawIce(Offset.Zero, w)
                }
                Goal.PATTERN -> {
                    // A tiny heart, like the drawings on the board.
                    val c = w / 4f
                    for ((r, col) in listOf(0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 1)) {
                        drawGem(Offset(w * 0.125f + col * c, w * 0.12f + r * c), c, Color(0xFFF472B6), glow = false)
                    }
                }
                Goal.CLEAR -> {
                    val c = w / 2f
                    for (r in 0..1) for (col in 0..1) drawGem(Offset(col * c, r * c), c, Palette.block(1 + r * 2 + col), glow = false)
                    drawTargetMark(center, w * 0.9f)
                }
                else -> Unit
            }
        }
    }
}

/** Small picture of an obstacle for the level intro. */
@Composable
fun MechanicIcon(m: Mechanic, modifier: Modifier = Modifier.size(30.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            when (m) {
                Mechanic.STONE -> drawStone(Offset.Zero, w, cracked = false)
                Mechanic.ICE -> {
                    drawEmptyCell(Offset.Zero, w)
                    drawIce(Offset.Zero, w)
                }
                Mechanic.LOCK -> {
                    drawEmptyCell(Offset.Zero, w)
                    drawLock(Offset.Zero, w)
                }
                Mechanic.TIMER -> {
                    drawGem(Offset.Zero, w, Palette.block(4), glow = false)
                    drawTimerBadge(center, w, urgent = false)
                }
            }
        }
        if (m == Mechanic.TIMER) Text("9", style = Styles.plain(11, Color.White, FontWeight.Black))
    }
}

/** One goal with its picture, for dialogs. */
@Composable
fun ObjectiveRow(o: Objective) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GoalIcon(o.goal)
        Spacer(Modifier.width(12.dp))
        Text(goalText(o), style = Styles.plain(19, Palette.TextPrimary, FontWeight.Black))
    }
}

/** Glass dialog for a chest on the adventure trail or the star chest. */
@Composable
fun AdventureChestDialog(title: String, coins: Int, onCollect: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    DimOverlay {
        GlassPanel {
            Text(title, style = Styles.gradient(30))
            ChestIcon(Modifier.size(96.dp), shaking = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("+$coins", style = Styles.glow(26, Palette.Gold, FontWeight.Black))
            }
            PrimaryButton(stringResource(R.string.collect, coins)) { onCollect() }
            SecondaryButton(stringResource(R.string.cancel)) { onClose() }
        }
    }
}
