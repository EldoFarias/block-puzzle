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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farias.prismablocks.R
import com.farias.prismablocks.audio.Sfx
import com.farias.prismablocks.audio.SoundManager
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.Adventure
import com.farias.prismablocks.game.Level
import kotlin.math.sin

private val ROW_HEIGHT = 112.dp
private const val LOCKED_AHEAD = 12

/** Horizontal position (0..1) of a level node along the winding path. */
private fun pathX(level: Int): Float = 0.5f + 0.28f * sin(level * 0.85f)

/** A chest waiting to be opened from the map (trail or star chest). */
private class ChestOffer(val title: String, val coins: Int, val open: () -> Int)

@Composable
fun AdventureScreen(sound: SoundManager, onBack: () -> Unit, onPlay: (Int) -> Unit) {
    // Tablets: draw the whole map 30% bigger so nodes and texts keep a phone-like size on screen.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = if (minOf(maxWidth, maxHeight) >= 600.dp) 1.3f else 1f
        Scaled(scale) { AdventureContent(sound, onBack, onPlay) }
    }
}

@Composable
private fun AdventureContent(sound: SoundManager, onBack: () -> Unit, onPlay: (Int) -> Unit) {
    BackHandler(onBack = onBack)
    val current = Progress.adventureLevel
    val shown = current + LOCKED_AHEAD
    var intro by remember { mutableStateOf<Level?>(null) }
    var chest by remember { mutableStateOf<ChestOffer?>(null) }
    val list = rememberLazyListState()
    val density = LocalDensity.current
    val trailTitle = stringResource(R.string.trail_chest)
    val starTitle = stringResource(R.string.star_chest)

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(theme = worldTheme(Adventure.world(current)))
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
                FitText(
                    stringResource(R.string.adventure),
                    Styles.gradient(24),
                    modifier = Modifier.weight(1f),
                    minSp = 14f,
                )
                Spacer(Modifier.width(8.dp))
                StarChestPill {
                    chest = ChestOffer(starTitle, Adventure.STAR_CHEST_REWARD) { Progress.openStarChest() }
                }
                Spacer(Modifier.width(10.dp))
                CoinPill(Progress.coins)
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewportPx = with(density) { maxHeight.toPx() }
                val mapWidth = maxWidth
                // Scroll so the current level sits in the middle of the screen.
                LaunchedEffect(current) {
                    // Wait for the first layout, then jump straight to the right pixel.
                    snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > 0 }
                    val rowPx = with(density) { ROW_HEIGHT.toPx() }
                    val target = ((shown - current) * rowPx - (viewportPx - rowPx) / 2f).coerceAtLeast(0f)
                    list.scrollToItem((target / rowPx).toInt(), (target % rowPx).toInt())
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = ROW_HEIGHT / 2),
                ) {
                    items(shown, key = { shown - it }) { i ->
                        val n = shown - i
                        MapRow(
                            n = n,
                            shown = shown,
                            current = current,
                            width = mapWidth,
                            onLevel = { intro = Adventure.level(n) },
                            onChest = {
                                chest = ChestOffer(trailTitle, Adventure.trailChestReward(n)) { Progress.openTrailChest(n) }
                            },
                        )
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
        chest?.let { offer ->
            AdventureChestDialog(
                title = offer.title,
                coins = offer.coins,
                onCollect = {
                    if (offer.open() > 0) sound.play(Sfx.RECORD, 0.8f)
                    chest = null
                },
                onClose = { chest = null },
            )
        }
    }
}

/** Stars earned and the goal of the next star chest; the chest wiggles when it can be opened. */
@Composable
private fun StarChestPill(onOpen: () -> Unit) {
    val ready = Progress.starChestReady()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (ready) Palette.Gold.copy(alpha = 0.18f) else Palette.Glass)
            .border(1.dp, if (ready) Palette.Gold else Palette.GlassBorder, shape)
            .clickable(enabled = ready, onClick = onOpen)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StarIcon(true, Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text("${Progress.totalStars}/${Progress.nextStarChest}", style = Styles.plain(14, Palette.Gold, FontWeight.Black))
        Spacer(Modifier.width(5.dp))
        ChestIcon(Modifier.size(22.dp).alpha(if (ready) 1f else 0.5f), shaking = ready)
    }
}

/** One row of the map: half of the path to each neighbour, the level node, and a chest or world banner. */
@Composable
private fun MapRow(n: Int, shown: Int, current: Int, width: Dp, onLevel: () -> Unit, onChest: () -> Unit) {
    // Decorations go on the side of the row away from the node.
    val nodeOnRight = pathX(n) > 0.5f
    Box(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val row = size.height
            val c = Offset(size.width * pathX(n), row / 2f)
            if (n < shown) pathHalf(c, Offset(size.width * (pathX(n) + pathX(n + 1)) / 2f, 0f), unlocked = n + 1 <= current)
            if (n > 1) pathHalf(c, Offset(size.width * (pathX(n) + pathX(n - 1)) / 2f, row), unlocked = n <= current)
        }
        val world = Adventure.world(n)
        if (n == Adventure.firstLevelOf(world)) {
            WorldBanner(
                world,
                Modifier
                    .align(if (nodeOnRight) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 14.dp),
            )
        }
        if (Adventure.hasChestAfter(n)) {
            TrailChest(
                n,
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = width * pathX(n) + (if (nodeOnRight) (-118).dp else 74.dp), y = 2.dp),
                onChest,
            )
        }
        LevelNode(n, current, width, onLevel)
    }
}

private fun DrawScope.pathHalf(from: Offset, to: Offset, unlocked: Boolean) {
    drawLine(
        color = if (unlocked) Palette.Cyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.15f),
        start = from, end = to,
        strokeWidth = if (unlocked) 6.dp.toPx() else 4.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = if (unlocked) null else PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx())),
    )
}

/** Name of the world that starts here, with the obstacle it brings. */
@Composable
private fun WorldBanner(world: Int, modifier: Modifier) {
    val accent = worldTheme(world).aurora[0]
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(118.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color(0x66110B2A))))
            .border(1.dp, accent.copy(alpha = 0.8f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.world_x, world + 1), style = Styles.plain(11, Palette.TextSecondary, FontWeight.Bold))
        FitText(worldName(world), Styles.glow(15, Palette.TextPrimary, FontWeight.Black), minSp = 10f)
        val mechanics = Adventure.mechanicsOf(world)
        if (mechanics.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                for (m in mechanics) MechanicIcon(m, Modifier.size(22.dp))
            }
        }
    }
}

/** Chest on the trail after every few levels: wiggles when ready, faded before and after. */
@Composable
private fun TrailChest(level: Int, modifier: Modifier, onOpen: () -> Unit) {
    val ready = Progress.trailChestReady(level)
    val opened = level in Progress.trailChests
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = ready, onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        ChestIcon(Modifier.size(40.dp).alpha(if (ready) 1f else 0.4f), shaking = ready)
        if (opened) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LevelNode(n: Int, current: Int, width: Dp, onClick: () -> Unit) {
    val lv = Adventure.level(n)
    val locked = n > current
    val isCurrent = n == current
    val stars = Progress.starsFor(n)
    val nodeSize = (if (isCurrent) 76.dp else 62.dp) + (if (lv.boss) 10.dp else 0.dp)
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
            val goalColor = goalColor(lv.goal)
            Box(contentAlignment = Alignment.Center) {
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
                                lv.boss -> Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706)))
                                else -> Brush.verticalGradient(listOf(goalColor, Palette.darker(goalColor, 0.45f)))
                            },
                        )
                        .border(
                            if (isCurrent || lv.boss) 3.dp else 2.dp,
                            when {
                                lv.boss -> Palette.Gold.copy(alpha = if (locked) 0.45f else 1f)
                                locked -> Color(0x33FFFFFF)
                                else -> Color.White.copy(alpha = 0.7f)
                            },
                            CircleShape,
                        )
                        .clickable(enabled = !locked, onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (locked) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0x88FFFFFF), modifier = Modifier.size(24.dp))
                    } else {
                        Text(n.toString(), style = Styles.plain(if (isCurrent) 26 else 22, Color.White, FontWeight.Black))
                    }
                }
                if (lv.boss) {
                    CrownIcon(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-16).dp)
                            .size(26.dp)
                            .alpha(if (locked) 0.5f else 1f),
                    )
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
            if (lv.boss) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownIcon(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.boss), style = Styles.glow(20, Palette.Gold, FontWeight.Black))
                    Spacer(Modifier.width(8.dp))
                    CrownIcon(Modifier.size(24.dp))
                }
            }
            Text(stringResource(R.string.level_x, lv.number), style = Styles.gradient(36))
            Text(
                stringResource(R.string.world_x, lv.world + 1) + " · " + worldName(lv.world),
                style = Styles.plain(14, Palette.TextSecondary, FontWeight.Bold),
            )
            for (o in lv.objectives) ObjectiveRow(o)
            if (lv.maxMoves > 0) {
                Text(stringResource(R.string.move_limit, lv.maxMoves), style = Styles.glow(15, Color(0xFFFB923C), FontWeight.Black))
            }
            for (m in lv.mechanics) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    MechanicIcon(m, Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        mechanicText(m),
                        style = Styles.plain(14, Palette.TextPrimary, FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    if (Adventure.introduces(lv.number, m)) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.new_badge),
                            style = Styles.plain(11, Color(0xFF1A1030), FontWeight.Black),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Palette.Gold)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
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
