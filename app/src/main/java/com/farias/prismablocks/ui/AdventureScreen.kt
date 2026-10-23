package com.farias.prismablocks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farias.prismablocks.R
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.Adventure
import com.farias.prismablocks.game.Goal
import com.farias.prismablocks.game.Level
import kotlin.math.sin

private val ROW_HEIGHT = 112.dp
private const val LOCKED_AHEAD = 12

/** Horizontal position (0..1) of a level node along the winding path. */
private fun pathX(level: Int): Float = 0.5f + 0.28f * sin(level * 0.85f)

@Composable
fun AdventureScreen(onBack: () -> Unit, onPlay: (Int) -> Unit) {
    BackHandler(onBack = onBack)
    val current = Progress.adventureLevel
    val shown = current + LOCKED_AHEAD
    var intro by remember { mutableStateOf<Level?>(null) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Palette.TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.adventure),
                    style = Styles.gradient(24),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StarIcon(true, Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(Progress.totalStars.toString(), style = Styles.glow(18, Palette.Gold))
                Spacer(Modifier.width(12.dp))
                CoinPill(Progress.coins)
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewport = maxHeight
                val mapWidth = maxWidth
                // Scroll so the current level sits in the middle of the screen.
                LaunchedEffect(current) {
                    // Wait for the first layout so the scroll range is known.
                    snapshotFlow { scroll.maxValue }.first { it in 1 until Int.MAX_VALUE }
                    val rowPx = with(density) { ROW_HEIGHT.toPx() }
                    val target = (shown - current) * rowPx + rowPx - with(density) { viewport.toPx() } / 2f
                    scroll.scrollTo(target.toInt().coerceIn(0, scroll.maxValue))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                ) {
                    MapPath(shown, current)
                    Column {
                        Spacer(Modifier.height(ROW_HEIGHT / 2))
                        for (n in shown downTo 1) {
                            LevelNode(n, current, mapWidth) { intro = Adventure.level(n) }
                        }
                        Spacer(Modifier.height(ROW_HEIGHT / 2))
                    }
                }
            }
        }

        intro?.let { lv ->
            LevelIntroDialog(lv, onPlay = {
                intro = null
                onPlay(lv.number)
            }, onCancel = { intro = null })
        }
    }
}

@Composable
private fun MapPath(shown: Int, current: Int) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT * (shown + 1)),
    ) {
        val row = ROW_HEIGHT.toPx()
        fun point(level: Int) = Offset(size.width * pathX(level), row / 2f + (shown - level) * row + row / 2f)
        for (n in 1 until shown) {
            val a = point(n)
            val b = point(n + 1)
            val unlocked = n + 1 <= current
            drawLine(
                color = if (unlocked) Palette.Cyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.15f),
                start = a, end = b,
                strokeWidth = if (unlocked) 6.dp.toPx() else 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (unlocked) null else PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx())),
            )
        }
    }
}

@Composable
private fun LevelNode(n: Int, current: Int, width: Dp, onClick: () -> Unit) {
    val locked = n > current
    val isCurrent = n == current
    val stars = Progress.starsFor(n)
    val nodeSize = if (isCurrent) 76.dp else 62.dp
    val pulse = if (isCurrent) pulseValue() else 1f
    Box(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
    ) {
        Column(
            modifier = Modifier
                .offset(x = width * pathX(n) - 50.dp)
                .width(100.dp)
                .align(Alignment.CenterStart),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val goalColor = when (Adventure.level(n).goal) {
                Goal.GEMS -> Palette.Cyan
                Goal.LINES -> Color(0xFFA3E635)
                Goal.SCORE -> Palette.Magenta
            }
            Box(
                modifier = Modifier
                    .size(nodeSize)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(CircleShape)
                    .background(
                        when {
                            locked -> Brush.verticalGradient(listOf(Color(0xFF2A2547), Color(0xFF17132E)))
                            isCurrent -> Brush.verticalGradient(listOf(Color(0xFFF0ABFC), Color(0xFF8B5CF6)))
                            else -> Brush.verticalGradient(listOf(goalColor, Palette.darker(goalColor, 0.45f)))
                        },
                    )
                    .border(if (isCurrent) 3.dp else 2.dp, if (locked) Color(0x33FFFFFF) else Color.White.copy(alpha = 0.7f), CircleShape)
                    .clickable(enabled = !locked, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                if (locked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0x88FFFFFF), modifier = Modifier.size(24.dp))
                } else {
                    Text(n.toString(), style = Styles.plain(if (isCurrent) 26 else 22, Color.White, FontWeight.Black))
                }
            }
            Spacer(Modifier.height(4.dp))
            if (!locked && !isCurrent) {
                Row {
                    for (i in 0 until 3) StarIcon(i < stars, Modifier.padding(horizontal = 1.dp).size(16.dp))
                }
            } else if (isCurrent) {
                Text(stringResource(R.string.play), style = Styles.glow(14, Palette.Gold, FontWeight.Black))
            }
        }
    }
}

@Composable
private fun pulseValue(): Float {
    val transition = rememberInfiniteTransition(label = "current")
    val pulse by transition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    return pulse
}

@Composable
private fun LevelIntroDialog(lv: Level, onPlay: () -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    DimOverlay {
        GlassPanel {
            Text(stringResource(R.string.level_x, lv.number), style = Styles.gradient(36))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (lv.goal) {
                    Goal.GEMS -> Canvas(Modifier.size(34.dp)) { drawGemIcon(center, size.width) }
                    Goal.LINES -> Canvas(Modifier.size(30.dp)) {
                        for (i in 0 until 4) drawGem(Offset(i * size.width / 4f, size.height * 0.35f), size.width / 4f, Palette.block(3), glow = false)
                    }
                    Goal.SCORE -> CrownIcon(Modifier.size(30.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(
                        when (lv.goal) {
                            Goal.GEMS -> R.string.goal_gems
                            Goal.LINES -> R.string.goal_lines
                            Goal.SCORE -> R.string.goal_score
                        },
                        lv.target,
                    ),
                    style = Styles.plain(20, Palette.TextPrimary, FontWeight.Black),
                )
            }
            Row {
                for (i in 0 until 3) StarIcon(i < Progress.starsFor(lv.number), Modifier.padding(horizontal = 3.dp).size(28.dp))
            }
            Text(
                stringResource(R.string.stars_hint, lv.par3),
                style = Styles.plain(14, Palette.TextSecondary, FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.play)) { onPlay() }
            SecondaryButton(stringResource(R.string.cancel)) { onCancel() }
        }
    }
}
