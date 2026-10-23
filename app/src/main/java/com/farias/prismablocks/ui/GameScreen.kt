package com.farias.prismablocks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farias.prismablocks.R
import com.google.android.gms.ads.AdSize
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.audio.Sfx
import com.farias.prismablocks.data.Progress
import com.farias.prismablocks.game.GameEngine
import com.farias.prismablocks.game.GameViewModel
import com.farias.prismablocks.game.Goal
import com.farias.prismablocks.game.Mode
import com.farias.prismablocks.game.Power
import kotlinx.coroutines.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

/** A power-up waiting for payment by video (the player didn't have enough coins). */
private class PendingPay(val power: Power, val action: () -> Unit)

@Composable
fun GameScreen(vm: GameViewModel, ads: AdsManager, onGreatMoment: () -> Unit, onHome: () -> Unit) {
    val view = LocalView.current
    val density = LocalDensity.current
    var pendingPay by remember { mutableStateOf<PendingPay?>(null) }

    LaunchedEffect(view) { vm.haptic = { kind -> performHaptic(view, kind) } }
    LaunchedEffect(density) { vm.liftPx = with(density) { 64.dp.toPx() } }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos {
            vm.fx.step(it)
            vm.tickEmbers()
        }
    }
    // A new record or a won daily is the best moment to ask for a store rating.
    LaunchedEffect(vm.showGameOverPanel, vm.showDailyWin, vm.showAdvWin) {
        if ((vm.showGameOverPanel && vm.newRecord) || vm.showDailyWin || (vm.showAdvWin && vm.advStars == 3)) onGreatMoment()
    }
    BackHandler(enabled = vm.bombMode) { vm.bombMode = false }
    BackHandler(enabled = !vm.bombMode && !vm.paused && !vm.showGameOverPanel && !vm.showDailyWin && !vm.showAdvWin) { vm.paused = true }

    fun pay(p: Power, action: () -> Unit) {
        if (Progress.spend(p.cost)) {
            action()
        } else {
            vm.playInvalid()
            pendingPay = PendingPay(p, action)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val context = LocalContext.current
            val widthDp = maxWidth.value.toInt()
            @Suppress("DEPRECATION")
            val bannerH = remember(widthDp) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp).height.dp
            }
            val onBombTap: (Int, Int) -> Unit = { r, c ->
                if (!vm.bombHits(r, c)) vm.playInvalid()
                else pay(Power.BOMB) { vm.bomb(r, c) }
            }
            val onPower: (Power) -> Unit = { p ->
                when (p) {
                    Power.UNDO -> pay(p) { vm.undo() }
                    Power.SHUFFLE -> pay(p) { vm.shuffle() }
                    Power.BOMB -> vm.bombMode = !vm.bombMode
                }
            }
            // Wide window (split screen, foldable, tablet in landscape): board in the middle,
            // info on the left, pieces on the right.
            val landscape = maxWidth > maxHeight * 1.05f
            if (landscape) {
                val boardSide = minOf(maxHeight - bannerH - 24.dp, maxWidth * 0.46f).coerceIn(160.dp, 640.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = vm.fx.shake.floatValue },
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TopBar(vm, GameDims.normal)
                            ScoreHeader(vm, GameDims.normal)
                            Spacer(Modifier.weight(1f))
                            PowerBar(vm, null, GameDims.normal, vertical = true, showLabels = true, onUse = onPower)
                            Spacer(Modifier.weight(1f))
                        }
                        Board(vm, boardSide, onBombTap)
                        TrayColumn(
                            vm,
                            Modifier
                                .weight(1f)
                                .height(boardSide),
                        )
                    }
                    BannerAd(ads)
                }
            } else {
                // Portrait: shrink the fixed parts on short phones, cap the board on tablets.
                val screenWidth = maxWidth
                val dims = if (maxHeight < 660.dp) GameDims.compact else GameDims.normal
                val fixed = dims.topBar + dims.header + dims.gap + dims.powerBar + bannerH + 12.dp
                val boardSide = minOf(maxWidth - 24.dp, (maxHeight - fixed) / (1f + dims.trayRatio)).coerceIn(150.dp, 720.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = vm.fx.shake.floatValue },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TopBar(vm, dims)
                    ScoreHeader(vm, dims)
                    Spacer(Modifier.weight(1f))
                    Board(vm, boardSide, onBombTap)
                    Spacer(Modifier.height(dims.gap))
                    Tray(vm, boardSide, dims.trayRatio)
                    // Narrow phones show icons + price only.
                    PowerBar(vm, screenWidth - 16.dp, dims, vertical = false, showLabels = screenWidth >= 392.dp, onUse = onPower)
                    Spacer(Modifier.weight(1f))
                    BannerAd(ads)
                }
            }
        }

        DragAndParticlesLayer(vm)
        FloatingTexts(vm)
        TutorialLayer(vm)

        AnimatedVisibility(
            visible = vm.paused,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) { PauseOverlay(vm, ads, onHome) }

        AnimatedVisibility(
            visible = vm.showGameOverPanel,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(tween(150)),
        ) { GameOverOverlay(vm, ads, onHome) }

        AnimatedVisibility(
            visible = vm.showAdvWin,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f)),
            exit = fadeOut(tween(150)),
        ) { AdventureWinOverlay(vm, ads, onHome) }

        AnimatedVisibility(
            visible = vm.showDailyWin,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.6f)),
            exit = fadeOut(tween(150)),
        ) { DailyWinOverlay(vm, onHome) }

        pendingPay?.let { pending ->
            PayWithVideoDialog(
                power = pending.power,
                ads = ads,
                onWatch = {
                    pendingPay = null
                    ads.showRewarded {
                        vm.sound.play(Sfx.COIN)
                        pending.action()
                    }
                },
                onCancel = { pendingPay = null },
            )
        }
    }
}

// --------------------------------------------------------------------------- header

@Composable
private fun TopBar(vm: GameViewModel, dims: GameDims) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.topBar)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (vm.mode) {
            Mode.CLASSIC -> {
                CrownIcon(Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(Progress.best.toString(), style = Styles.glow(20, Palette.Gold))
            }
            Mode.DAILY -> Text(stringResource(R.string.daily_title), style = Styles.glow(18, Palette.Cyan, FontWeight.Black))
            Mode.ADVENTURE -> Text(
                stringResource(R.string.level_x, vm.level?.number ?: 1),
                style = Styles.glow(20, Palette.Magenta, FontWeight.Black),
            )
        }
        Spacer(Modifier.weight(1f))
        CoinPill(Progress.coins)
        Spacer(Modifier.width(10.dp))
        GlassCircleButton(onClick = { vm.paused = true }) {
            PauseIcon(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ScoreHeader(vm: GameViewModel, dims: GameDims) {
    val shown by animateIntAsState(vm.score, tween(450), label = "score")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.header),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(shown.toString(), style = Styles.gradient(dims.scoreSp))
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            val lv = vm.level
            if (vm.mode == Mode.DAILY) {
                GoalProgress(vm.lines, vm.dailyTarget, stringResource(R.string.lines_progress, vm.lines.coerceAtMost(vm.dailyTarget), vm.dailyTarget))
            } else if (vm.mode == Mode.ADVENTURE && lv != null) {
                val label = when (lv.goal) {
                    Goal.GEMS -> stringResource(R.string.gems_progress, vm.gemsCollected.coerceAtMost(lv.target), lv.target)
                    Goal.LINES -> stringResource(R.string.lines_progress, vm.lines.coerceAtMost(lv.target), lv.target)
                    Goal.SCORE -> stringResource(R.string.points_progress, lv.target)
                }
                GoalProgress(vm.advProgress, lv.target, label, gem = lv.goal == Goal.GEMS)
            } else if (vm.combo >= 2) {
                ComboBadge(vm.combo)
            }
        }
    }
}

@Composable
private fun ComboBadge(combo: Int) {
    val transition = rememberInfiniteTransition(label = "combo")
    val pulse by transition.animateFloat(
        1f, 1.12f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "cp",
    )
    Text(
        "🔥 " + stringResource(R.string.combo_x, combo),
        style = Styles.glow(17, Color(0xFFFF9100), FontWeight.Black),
        modifier = Modifier.graphicsLayer {
            scaleX = pulse
            scaleY = pulse
        },
    )
}

@Composable
private fun GoalProgress(current: Int, target: Int, label: String, gem: Boolean = false) {
    val progress = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (gem) {
            Canvas(Modifier.size(22.dp)) { drawGemIcon(center, size.width) }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            Modifier
                .width(150.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Palette.Cyan, Palette.Magenta))),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = Styles.plain(14, Palette.TextPrimary, FontWeight.Bold))
    }
}

// --------------------------------------------------------------------------- board

@Composable
private fun Board(vm: GameViewModel, side: Dp, onBombTap: (Int, Int) -> Unit) {
    val density = LocalDensity.current
    val padDp = side * 0.03f
    val pulse = rememberInfiniteTransition(label = "bomb")
    val bombPulse by pulse.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "bp",
    )
    Canvas(
        modifier = Modifier
            .size(side)
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                val p = with(density) { padDp.toPx() }
                vm.gridOrigin = Offset(b.left + p, b.top + p)
                vm.cellPx = (b.width - 2f * p) / GameEngine.N
            }
            .pointerInput(vm.bombMode) {
                if (!vm.bombMode) return@pointerInput
                detectTapGestures { local ->
                    val p = padDp.toPx()
                    val cell = (size.width - 2f * p) / GameEngine.N
                    val c = ((local.x - p) / cell).toInt()
                    val r = ((local.y - p) / cell).toInt()
                    if (local.x >= p && local.y >= p && r in 0 until GameEngine.N && c in 0 until GameEngine.N) {
                        onBombTap(r, c)
                    }
                }
            },
    ) {
        vm.fx.frame.longValue // redraw every frame for animations
        val n = GameEngine.N
        val p = padDp.toPx()
        val cell = (size.width - 2f * p) / n
        val corner = CornerRadius(p * 2.4f)

        drawRoundRect(Palette.Glass, cornerRadius = corner)
        val combo = vm.combo
        if (combo >= 2 && !vm.finished && !vm.bombMode) {
            // Combo streak: a flickering flame border that grows with the combo.
            val intensity = ((combo - 1) / 4f).coerceIn(0.35f, 1f)
            val flicker = 0.8f + 0.2f * kotlin.math.sin(vm.fx.now / 70_000_000.0).toFloat()
            val phase = (vm.fx.now / 1_600_000_000.0 % 1.0).toFloat()
            val flame = listOf(Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFD54F), Color(0xFFFF9100), Color(0xFFFF3D00))
            val stops = flame.mapIndexed { i, col -> ((i / (flame.size - 1f)) + phase) % 1f to col }.sortedBy { it.first }
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRoundRect(
                Color(0xFFFF6D00).copy(alpha = 0.22f * intensity * flicker),
                cornerRadius = corner,
                style = Stroke((6.dp + 8.dp * intensity).toPx()),
            )
            drawRoundRect(
                brush = Brush.sweepGradient(*stops.toTypedArray(), center = center),
                cornerRadius = corner,
                alpha = (0.55f + 0.45f * intensity) * flicker,
                style = Stroke((2.dp + 2.dp * intensity).toPx()),
            )
        } else if (vm.bombMode) {
            drawRoundRect(Palette.Danger.copy(alpha = bombPulse), cornerRadius = corner, style = Stroke(3.dp.toPx()))
        } else {
            drawRoundRect(Palette.GlassBorder, cornerRadius = corner, style = Stroke(1.dp.toPx()))
        }

        val board = vm.board
        val gems = vm.gems
        val drag = vm.drag?.takeIf { it.valid }
        val previewColor = drag?.let { Palette.block(it.piece.color) }
        val previewRows = drag?.previewRows ?: emptyList()
        val previewCols = drag?.previewCols ?: emptyList()
        val now = vm.fx.now
        val overT = if (vm.gameOver) ((now - vm.gameOverAt) / 600_000_000f).coerceIn(0f, 1f) else 0f

        for (r in 0 until n) for (c in 0 until n) {
            val tl = Offset(p + c * cell, p + r * cell)
            val idx = r * n + c
            val v = board[idx]
            if (v == 0) {
                drawEmptyCell(tl, cell)
                continue
            }
            var color = Palette.block(v)
            if (previewColor != null && (r in previewRows || c in previewCols)) color = previewColor
            if (overT > 0f) color = lerp(color, Palette.gray(color), overT)
            val popStart = vm.fx.pops[idx]
            val pt = if (popStart == 0L) 1f else ((now - popStart) / Fx.POP_NANOS.toFloat()).coerceIn(0f, 1f)
            val scale = if (pt < 1f) 0.6f + 0.4f * easeOutBack(pt) else 1f
            drawGem(tl, cell, color, glow = overT < 0.5f, scale = scale)
            if (gems[idx]) {
                val tw = 0.75f + 0.25f * kotlin.math.sin(now / 180_000_000.0 + idx).toFloat()
                drawGemIcon(Offset(tl.x + cell / 2f, tl.y + cell / 2f), cell * scale, tw, alpha = 1f - overT * 0.5f)
            }
        }

        if (drag != null && previewColor != null) {
            // Light up empty cells of the lines that will be cleared
            for (r in 0 until n) for (c in 0 until n) {
                if (board[r * n + c] != 0) continue
                if (r !in previewRows && c !in previewCols) continue
                drawRoundRect(
                    color = previewColor.copy(alpha = 0.22f),
                    topLeft = Offset(p + c * cell + cell * 0.05f, p + r * cell + cell * 0.05f),
                    size = Size(cell * 0.9f, cell * 0.9f),
                    cornerRadius = CornerRadius(cell * 0.18f),
                )
            }
            // Ghost of the piece where it will land
            for (cl in drag.piece.shape.cells) {
                val rr = drag.targetRow + cl.r
                val cc = drag.targetCol + cl.c
                drawGem(Offset(p + cc * cell, p + rr * cell), cell, previewColor, alpha = 0.42f, glow = false)
            }
        }

        // Cleared cells bursting out
        for (f in vm.fx.flashes) {
            val t = ((now - f.start) / Fx.FLASH_NANOS.toFloat()).coerceIn(0f, 1f)
            val r = f.index / n
            val c = f.index % n
            drawGem(
                Offset(p + c * cell, p + r * cell), cell,
                lerp(f.color, Color.White, 0.7f * (1f - t)),
                alpha = 1f - t, glow = true, scale = 1f + 0.35f * t,
            )
        }
    }
}

// --------------------------------------------------------------------------- tray

@Composable
private fun Tray(vm: GameViewModel, boardSide: Dp, ratio: Float) {
    Row(
        modifier = Modifier
            .width(boardSide)
            .height(boardSide * ratio),
    ) {
        for (i in 0 until 3) {
            TraySlot(vm, i, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Pieces stacked vertically, for the wide (landscape) layout. */
@Composable
private fun TrayColumn(vm: GameViewModel, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp)) {
        for (i in 0 until 3) {
            TraySlot(vm, i, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun TraySlot(vm: GameViewModel, slot: Int, modifier: Modifier) {
    val piece = vm.tray[slot]
    val fits = vm.trayFits[slot]
    val appear = remember(piece) { Animatable(if (piece == null) 1f else 0f) }
    LaunchedEffect(piece) {
        if (piece != null) appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { vm.slotRects[slot] = it.boundsInRoot() }
            .pointerInput(slot) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val origin = vm.slotRects[slot].topLeft
                    vm.startDrag(slot, origin + down.position)
                    if (vm.drag?.slot != slot) return@awaitEachGesture
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            vm.dragTo(origin + change.position)
                            change.consume()
                        }
                        vm.endDrag()
                    } catch (e: CancellationException) {
                        vm.cancelDrag()
                        throw e
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pc = piece ?: return@Canvas
            if (vm.drag?.slot == slot) return@Canvas
            val tc = min(size.width / 5.6f, size.height / 5.4f)
            val s = appear.value.coerceAtLeast(0f)
            val cell = tc * s
            val w = pc.shape.cols * cell
            val h = pc.shape.rows * cell
            val tl = Offset((size.width - w) / 2f, (size.height - h) / 2f)
            val color = Palette.block(pc.color)
            drawShape(pc.shape, tl, cell, if (fits) color else Palette.gray(color), alpha = if (fits) 1f else 0.45f, glow = fits)
        }
    }
}

// --------------------------------------------------------------------------- power-ups

@Composable
private fun PowerBar(
    vm: GameViewModel,
    width: Dp?,
    dims: GameDims,
    vertical: Boolean,
    showLabels: Boolean,
    onUse: (Power) -> Unit,
) {
    val buttons: @Composable () -> Unit = {
        PowerButton(vm, Power.UNDO, stringResource(R.string.power_undo), dims, showLabels, onUse) { UndoIcon(it) }
        PowerButton(vm, Power.BOMB, stringResource(R.string.power_bomb), dims, showLabels, onUse) { BombIcon(it) }
        PowerButton(vm, Power.SHUFFLE, stringResource(R.string.power_shuffle), dims, showLabels, onUse) { ShuffleIcon(it) }
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { buttons() }
    } else {
        Row(
            modifier = Modifier
                .then(if (width != null) Modifier.width(width) else Modifier)
                .height(dims.powerBar)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) { buttons() }
    }
}

@Composable
private fun PowerButton(
    vm: GameViewModel,
    power: Power,
    label: String,
    dims: GameDims,
    showLabels: Boolean,
    onUse: (Power) -> Unit,
    icon: @Composable (Modifier) -> Unit,
) {
    val active = power == Power.BOMB && vm.bombMode
    val enabled = active || vm.powerAvailable(power)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .height(dims.powerBar - 14.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (active) Palette.Danger.copy(alpha = 0.25f) else Palette.Glass)
            .border(if (active) 2.dp else 1.dp, if (active) Palette.Danger else Palette.GlassBorder, shape)
            .clickable(enabled = enabled) { onUse(power) }
            .padding(horizontal = if (showLabels) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(Modifier.size(if (showLabels) 26.dp else 22.dp))
        Spacer(Modifier.width(if (showLabels) 8.dp else 6.dp))
        Column {
            if (showLabels) Text(label, style = Styles.plain(13, Palette.TextPrimary, FontWeight.Bold), maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(power.cost.toString(), style = Styles.plain(12, Palette.Gold, FontWeight.Bold))
            }
        }
    }
}

// --------------------------------------------------------------------------- overlay layers

@Composable
private fun DragAndParticlesLayer(vm: GameViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        vm.fx.frame.longValue
        for (pt in vm.fx.particles) {
            val a = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            val r = pt.size * (0.5f + 0.5f * a)
            if (r > 0.1f) drawCircle(pt.color.copy(alpha = a), r, Offset(pt.x, pt.y))
        }
        val d = vm.drag ?: return@Canvas
        val cell = vm.cellPx
        val tl = vm.dragTopLeft(d)
        for (c in d.piece.shape.cells) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(tl.x + c.c * cell + cell * 0.08f, tl.y + c.r * cell + cell * 0.22f),
                size = Size(cell * 0.86f, cell * 0.86f),
                cornerRadius = CornerRadius(cell * 0.2f),
            )
        }
        drawShape(d.piece.shape, tl, cell, Palette.block(d.piece.color), alpha = if (d.valid) 1f else 0.85f)
    }
}

/** First game: an animated finger shows how to drag a piece. Bomb mode: a hint pill. */
@Composable
private fun TutorialLayer(vm: GameViewModel) {
    val showTutorial = !Progress.tutorialDone && vm.mode == Mode.CLASSIC && vm.drag == null &&
        !vm.finished && !vm.paused && !vm.bombMode
    val hint = when {
        vm.bombMode -> stringResource(R.string.bomb_hint)
        showTutorial -> stringResource(R.string.tutorial_drag) + "\n" + stringResource(R.string.tutorial_lines)
        else -> null
    }
    if (hint != null) {
        HintPill(hint, vm)
    }
    if (!showTutorial) return
    val slot = vm.tray.indexOfFirst { it != null }
    if (slot < 0) return
    val piece = vm.tray[slot] ?: return

    val transition = rememberInfiniteTransition(label = "tutorial")
    val t by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        val from = vm.slotRects[slot].center
        if (from == Offset.Zero || vm.cellPx <= 1f) return@Canvas
        val to = Offset(vm.gridOrigin.x + vm.cellPx * 4f, vm.gridOrigin.y + vm.cellPx * 5f)
        // 0-0.15 press, 0.15-0.75 move, 0.75-1 fade out
        val move = ((t - 0.15f) / 0.6f).coerceIn(0f, 1f)
        val eased = move * move * (3f - 2f * move)
        val pos = Offset(from.x + (to.x - from.x) * eased, from.y + (to.y - from.y) * eased)
        val alpha = if (t > 0.75f) (1f - (t - 0.75f) / 0.25f) else (t / 0.1f).coerceAtMost(1f)
        val cell = vm.cellPx
        val w = piece.shape.cols * cell
        val h = piece.shape.rows * cell
        drawShape(
            piece.shape, Offset(pos.x - w / 2f, pos.y - vm.liftPx - h), cell,
            Palette.block(piece.color), alpha = 0.55f * alpha, glow = false,
        )
        val fingerR = 16.dp.toPx()
        val press = if (t < 0.15f) t / 0.15f else 1f
        drawCircle(Color.White.copy(alpha = 0.25f * alpha), fingerR * (1.2f + 0.6f * (1f - press)), pos)
        drawCircle(Color.White.copy(alpha = 0.9f * alpha), fingerR * 0.7f, pos)
    }
}

@Composable
private fun HintPill(text: String, vm: GameViewModel) {
    Box(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints(maxWidth = constraints.maxWidth - 48.dp.roundToPx()))
            layout(constraints.maxWidth, constraints.maxHeight) {
                vm.fx.frame.longValue // board position is known only after the first layout
                val x = (constraints.maxWidth - placeable.width) / 2
                val y = (vm.gridOrigin.y + vm.cellPx * 0.4f).roundToInt()
                placeable.place(x, y)
            }
        },
    ) {
        Text(
            text = text,
            style = Styles.plain(15, Color.White, FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xD9120B2E))
                .border(1.dp, Palette.GlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FloatingTexts(vm: GameViewModel) {
    Box(Modifier.fillMaxSize()) {
        for (t in vm.fx.texts) {
            key(t.id) { FloatingTextItem(t, vm.fx) }
        }
    }
}

@Composable
private fun FloatingTextItem(t: FloatText, fx: Fx) {
    val style = if (t.big) Styles.glow(32, t.color, FontWeight.Black) else Styles.glow(22, t.color, FontWeight.ExtraBold)
    Box(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints())
            layout(0, 0) {
                val margin = 8.dp.roundToPx()
                val maxX = (constraints.maxWidth - placeable.width - margin).coerceAtLeast(margin)
                val x = (t.x - placeable.width / 2f).roundToInt().coerceIn(margin, maxX)
                val y = (t.y - placeable.height / 2f).roundToInt()
                placeable.place(x, y)
            }
        },
    ) {
        Text(
            text = t.text,
            style = style,
            modifier = Modifier.graphicsLayer {
                val prog = (fx.frame.longValue - t.start) / t.duration.toFloat()
                if (prog < 0f) {
                    alpha = 0f
                    return@graphicsLayer
                }
                val appear = (prog / 0.15f).coerceAtMost(1f)
                val s = 0.4f + 0.6f * easeOutBack(appear)
                scaleX = s
                scaleY = s
                translationY = -prog * 56.dp.toPx()
                alpha = if (prog > 0.7f) (1f - (prog - 0.7f) / 0.3f).coerceIn(0f, 1f) else 1f
            },
        )
    }
}

/** Sizes of the fixed parts of the game screen. */
private class GameDims(
    val topBar: Dp,
    val header: Dp,
    val scoreSp: Int,
    val gap: Dp,
    val powerBar: Dp,
    /** Tray height as a fraction of the board side. */
    val trayRatio: Float,
) {
    companion object {
        val normal = GameDims(topBar = 52.dp, header = 100.dp, scoreSp = 52, gap = 14.dp, powerBar = 70.dp, trayRatio = 0.34f)
        val compact = GameDims(topBar = 44.dp, header = 74.dp, scoreSp = 38, gap = 8.dp, powerBar = 58.dp, trayRatio = 0.30f)
    }
}
