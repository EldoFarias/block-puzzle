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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farias.prismablocks.R
import com.farias.prismablocks.ads.AdsManager
import com.farias.prismablocks.game.GameEngine
import com.farias.prismablocks.game.GameViewModel
import kotlinx.coroutines.CancellationException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(vm: GameViewModel, ads: AdsManager) {
    val view = LocalView.current
    val density = LocalDensity.current

    LaunchedEffect(view) { vm.haptic = { kind -> performHaptic(view, kind) } }
    LaunchedEffect(density) { vm.liftPx = with(density) { 64.dp.toPx() } }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { vm.fx.step(it) }
    }
    BackHandler(enabled = !vm.paused && !vm.showGameOverPanel) { vm.paused = true }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            // Fixed parts: top bar 52, score 96, gap 18, banner ~64, margins 20.
            val boardSide = minOf(maxWidth - 28.dp, (maxHeight - 250.dp) / 1.34f).coerceAtLeast(200.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = vm.fx.shake.floatValue },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopBar(vm)
                ScoreHeader(vm)
                Spacer(Modifier.weight(1f))
                Board(vm, boardSide)
                Spacer(Modifier.height(18.dp))
                Tray(vm, boardSide)
                Spacer(Modifier.weight(1f))
                BannerAd(ads)
            }
        }

        DragAndParticlesLayer(vm)
        FloatingTexts(vm)

        AnimatedVisibility(
            visible = vm.paused,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) { PauseOverlay(vm, ads) }

        AnimatedVisibility(
            visible = vm.showGameOverPanel,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(tween(150)),
        ) { GameOverOverlay(vm, ads) }
    }
}

// --------------------------------------------------------------------------- background

@Composable
private fun AuroraBackground() {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Palette.BgTop, Palette.BgBottom)))
        val a = phase * 2f * PI.toFloat()
        val w = size.width
        val h = size.height
        blob(Palette.AuroraViolet, Offset(w * (0.25f + 0.18f * cos(a)), h * (0.18f + 0.07f * sin(a))), w * 0.85f, 0.32f)
        blob(Palette.AuroraCyan, Offset(w * (0.85f + 0.12f * sin(a)), h * (0.55f + 0.09f * cos(a))), w * 0.70f, 0.16f)
        blob(Palette.AuroraPink, Offset(w * (0.20f + 0.14f * sin(a + 1.3f)), h * (0.92f + 0.05f * cos(a + 2f))), w * 0.80f, 0.22f)
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius < 1f) return
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}

// --------------------------------------------------------------------------- header

@Composable
private fun TopBar(vm: GameViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrownIcon(Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(vm.best.toString(), style = Styles.glow(20, Palette.Gold))
        Spacer(Modifier.weight(1f))
        GlassCircleButton(onClick = { vm.paused = true }) {
            PauseIcon(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ScoreHeader(vm: GameViewModel) {
    val shown by animateIntAsState(vm.score, tween(450), label = "score")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(shown.toString(), style = Styles.gradient(56))
        val combo = vm.combo
        Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            if (combo >= 2) {
                Text(stringResource(R.string.combo_x, combo), style = Styles.glow(16, Palette.Gold, FontWeight.Black))
            }
        }
    }
}

// --------------------------------------------------------------------------- board

@Composable
private fun Board(vm: GameViewModel, side: Dp) {
    val density = LocalDensity.current
    val padDp = side * 0.03f
    Canvas(
        modifier = Modifier
            .size(side)
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                val p = with(density) { padDp.toPx() }
                vm.gridOrigin = Offset(b.left + p, b.top + p)
                vm.cellPx = (b.width - 2f * p) / GameEngine.N
            },
    ) {
        vm.fx.frame.longValue // redraw every frame for animations
        val n = GameEngine.N
        val p = padDp.toPx()
        val cell = (size.width - 2f * p) / n
        val corner = CornerRadius(p * 2.4f)

        drawRoundRect(Palette.Glass, cornerRadius = corner)
        drawRoundRect(Palette.GlassBorder, cornerRadius = corner, style = Stroke(1.dp.toPx()))

        val board = vm.board
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
private fun Tray(vm: GameViewModel, boardSide: Dp) {
    Row(
        modifier = Modifier
            .width(boardSide)
            .height(boardSide * 0.34f),
    ) {
        for (i in 0 until 3) {
            TraySlot(vm, i, Modifier.weight(1f).fillMaxHeight())
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

// --------------------------------------------------------------------------- overlay layers

@Composable
private fun DragAndParticlesLayer(vm: GameViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        vm.fx.frame.longValue
        for (pt in vm.fx.particles) {
            val a = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            drawCircle(pt.color.copy(alpha = a), pt.size * (0.5f + 0.5f * a), Offset(pt.x, pt.y))
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
