package com.farias.prismablocks.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.farias.prismablocks.game.Shape

/** Draws one glossy, glowing "gem" block inside the square at [topLeft] with side [cell]. */
fun DrawScope.drawGem(
    topLeft: Offset,
    cell: Float,
    color: Color,
    alpha: Float = 1f,
    glow: Boolean = true,
    scale: Float = 1f,
) {
    if (alpha <= 0.01f || scale <= 0.01f) return
    val s = cell * scale
    // Gradients crash with a zero radius/height (e.g. first frame or start of an animation).
    if (s < 1f) return
    val o = Offset(topLeft.x + (cell - s) / 2f, topLeft.y + (cell - s) / 2f)
    val inset = s * 0.05f
    val d = s - inset * 2f
    val x = o.x + inset
    val y = o.y + inset
    val corner = CornerRadius(d * 0.22f)
    val center = Offset(o.x + s / 2f, o.y + s / 2f)

    if (glow) {
        val radius = s * 0.95f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.42f * alpha), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    // Body: vertical gradient for depth
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Palette.lighter(color, 0.28f), color, Palette.darker(color, 0.30f)),
            startY = y,
            endY = y + d,
        ),
        topLeft = Offset(x, y),
        size = Size(d, d),
        cornerRadius = corner,
        alpha = alpha,
    )
    // Inner bevel
    drawRoundRect(
        color = Color.White.copy(alpha = 0.20f),
        topLeft = Offset(x + d * 0.09f, y + d * 0.09f),
        size = Size(d * 0.82f, d * 0.82f),
        cornerRadius = CornerRadius(d * 0.16f),
        style = Stroke(width = d * 0.045f),
        alpha = alpha,
    )
    // Glossy top highlight
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
            startY = y + d * 0.08f,
            endY = y + d * 0.48f,
        ),
        topLeft = Offset(x + d * 0.12f, y + d * 0.08f),
        size = Size(d * 0.76f, d * 0.38f),
        cornerRadius = CornerRadius(d * 0.14f),
        alpha = alpha,
    )
    // Sparkle
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = d * 0.055f,
        center = Offset(x + d * 0.25f, y + d * 0.23f),
        alpha = alpha,
    )
    // Crisp edge
    drawRoundRect(
        color = Palette.darker(color, 0.55f).copy(alpha = 0.55f),
        topLeft = Offset(x, y),
        size = Size(d, d),
        cornerRadius = corner,
        style = Stroke(width = d * 0.03f),
        alpha = alpha,
    )
}

/** Empty board slot: a soft, recessed glass square. */
fun DrawScope.drawEmptyCell(topLeft: Offset, cell: Float) {
    if (cell < 1f) return
    val inset = cell * 0.05f
    val d = cell - inset * 2f
    val corner = CornerRadius(d * 0.2f)
    drawRoundRect(
        color = Palette.EmptyCell,
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(d, d),
        cornerRadius = corner,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Palette.EmptyCellInner, Color.Transparent),
            startY = topLeft.y + inset,
            endY = topLeft.y + inset + d * 0.6f,
        ),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(d, d),
        cornerRadius = corner,
    )
}

/** Draws a whole shape with its top-left corner at [topLeft]. */
fun DrawScope.drawShape(
    shape: Shape,
    topLeft: Offset,
    cell: Float,
    color: Color,
    alpha: Float = 1f,
    glow: Boolean = true,
) {
    for (c in shape.cells) {
        drawGem(Offset(topLeft.x + c.c * cell, topLeft.y + c.r * cell), cell, color, alpha, glow)
    }
}

/** A faceted diamond drawn on top of a block that holds a collectible gem (adventure mode). */
fun DrawScope.drawGemIcon(center: Offset, size: Float, twinkle: Float = 1f, alpha: Float = 1f) {
    if (size < 2f || alpha <= 0.01f) return
    val w = size * 0.62f
    val h = size * 0.56f
    val top = center.y - h * 0.48f
    val girdle = center.y - h * 0.12f
    val bottom = center.y + h * 0.52f
    val left = center.x - w / 2f
    val right = center.x + w / 2f
    // glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF67E8F9).copy(alpha = 0.55f * alpha * twinkle), Color.Transparent),
            center = center, radius = size * 0.6f,
        ),
        radius = size * 0.6f, center = center,
    )
    val crown = androidx.compose.ui.graphics.Path().apply {
        moveTo(left + w * 0.22f, top)
        lineTo(right - w * 0.22f, top)
        lineTo(right, girdle)
        lineTo(left, girdle)
        close()
    }
    val pavilion = androidx.compose.ui.graphics.Path().apply {
        moveTo(left, girdle)
        lineTo(right, girdle)
        lineTo(center.x, bottom)
        close()
    }
    drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFA5F3FC)), top, girdle), alpha = alpha)
    drawPath(pavilion, Brush.verticalGradient(listOf(Color(0xFF67E8F9), Color(0xFF0E7490)), girdle, bottom), alpha = alpha)
    val edge = Stroke(width = size * 0.035f)
    drawPath(crown, Color(0xFF0E7490).copy(alpha = 0.8f), alpha = alpha, style = edge)
    drawPath(pavilion, Color(0xFF0E7490).copy(alpha = 0.8f), alpha = alpha, style = edge)
    drawLine(Color(0xFF0E7490).copy(alpha = 0.5f * alpha), Offset(center.x, girdle), Offset(center.x, bottom), size * 0.025f)
    // sparkle
    val s = size * 0.12f * twinkle
    val sp = Offset(left + w * 0.3f, top + (girdle - top) * 0.45f)
    drawLine(Color.White.copy(alpha = alpha), Offset(sp.x - s, sp.y), Offset(sp.x + s, sp.y), size * 0.03f)
    drawLine(Color.White.copy(alpha = alpha), Offset(sp.x, sp.y - s), Offset(sp.x, sp.y + s), size * 0.03f)
}

/** Stone obstacle: a matte gray block with speckles; [cracked] after its first clear. */
fun DrawScope.drawStone(topLeft: Offset, cell: Float, cracked: Boolean, alpha: Float = 1f, tint: Color = Palette.Stone) {
    if (cell < 1f || alpha <= 0.01f) return
    drawGem(topLeft, cell, tint, alpha, glow = false)
    val d = cell * 0.9f
    val x = topLeft.x + cell * 0.05f
    val y = topLeft.y + cell * 0.05f
    val speck = Palette.darker(tint, 0.45f).copy(alpha = 0.55f * alpha)
    for ((fx, fy, fr) in listOf(Triple(0.68f, 0.30f, 0.05f), Triple(0.30f, 0.66f, 0.06f), Triple(0.72f, 0.72f, 0.04f), Triple(0.48f, 0.48f, 0.035f))) {
        drawCircle(speck, d * fr, Offset(x + d * fx, y + d * fy))
    }
    if (cracked) {
        val crack = androidx.compose.ui.graphics.Path().apply {
            moveTo(x + d * 0.18f, y + d * 0.12f)
            lineTo(x + d * 0.42f, y + d * 0.40f)
            lineTo(x + d * 0.34f, y + d * 0.55f)
            lineTo(x + d * 0.62f, y + d * 0.86f)
            moveTo(x + d * 0.42f, y + d * 0.40f)
            lineTo(x + d * 0.78f, y + d * 0.30f)
        }
        drawPath(crack, Color(0xFF1F2230).copy(alpha = 0.85f * alpha), style = Stroke(width = d * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(crack, Color.White.copy(alpha = 0.25f * alpha), style = Stroke(width = d * 0.02f))
    }
}

/** Locked cell: a dim glass slot with a padlock. */
fun DrawScope.drawLock(topLeft: Offset, cell: Float, alpha: Float = 1f) {
    if (cell < 2f || alpha <= 0.01f) return
    val inset = cell * 0.05f
    val d = cell - inset * 2f
    drawRoundRect(
        color = Color(0x2EFFFFFF).copy(alpha = 0.18f * alpha),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(d, d),
        cornerRadius = CornerRadius(d * 0.2f),
    )
    drawRoundRect(
        color = Palette.Gold.copy(alpha = 0.35f * alpha),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(d, d),
        cornerRadius = CornerRadius(d * 0.2f),
        style = Stroke(width = d * 0.03f),
    )
    val cx = topLeft.x + cell / 2f
    val bodyW = d * 0.46f
    val bodyH = d * 0.36f
    val bodyTop = topLeft.y + cell * 0.5f - bodyH * 0.15f
    // shackle
    drawArc(
        color = Color(0xFFE5E7EB).copy(alpha = alpha),
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - bodyW * 0.32f, bodyTop - bodyW * 0.42f),
        size = Size(bodyW * 0.64f, bodyW * 0.84f),
        style = Stroke(width = d * 0.07f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706)), bodyTop, bodyTop + bodyH),
        topLeft = Offset(cx - bodyW / 2f, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(d * 0.06f),
        alpha = alpha,
    )
    drawCircle(Color(0xFF78350F).copy(alpha = alpha), d * 0.045f, Offset(cx, bodyTop + bodyH * 0.45f))
}

/** Frosted ice over a cell (empty or holding a block). */
fun DrawScope.drawIce(topLeft: Offset, cell: Float, alpha: Float = 1f) {
    if (cell < 2f || alpha <= 0.01f) return
    val inset = cell * 0.04f
    val d = cell - inset * 2f
    val x = topLeft.x + inset
    val y = topLeft.y + inset
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0x99E0F2FE), Color(0x4D7DD3FC)), y, y + d),
        topLeft = Offset(x, y),
        size = Size(d, d),
        cornerRadius = CornerRadius(d * 0.2f),
        alpha = alpha,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.75f * alpha),
        topLeft = Offset(x, y),
        size = Size(d, d),
        cornerRadius = CornerRadius(d * 0.2f),
        style = Stroke(width = d * 0.04f),
    )
    val streak = Color.White.copy(alpha = 0.6f * alpha)
    drawLine(streak, Offset(x + d * 0.22f, y + d * 0.52f), Offset(x + d * 0.52f, y + d * 0.22f), d * 0.06f, androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(streak, Offset(x + d * 0.42f, y + d * 0.74f), Offset(x + d * 0.74f, y + d * 0.42f), d * 0.04f, androidx.compose.ui.graphics.StrokeCap.Round)
}

/** Four-point sparkle on the blocks of a drawing to clear (or the starting blocks to clear). */
fun DrawScope.drawTargetMark(center: Offset, size: Float, twinkle: Float = 1f, alpha: Float = 1f) {
    if (size < 2f || alpha <= 0.01f) return
    val r = size * 0.26f * (0.85f + 0.15f * twinkle)
    val w = r * 0.32f
    val star = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + w, center.y - w)
        lineTo(center.x + r, center.y)
        lineTo(center.x + w, center.y + w)
        lineTo(center.x, center.y + r)
        lineTo(center.x - w, center.y + w)
        lineTo(center.x - r, center.y)
        lineTo(center.x - w, center.y - w)
        close()
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Palette.Gold.copy(alpha = 0.45f * alpha * twinkle), Color.Transparent), center = center, radius = r * 1.6f),
        radius = r * 1.6f, center = center,
    )
    drawPath(star, Color(0xFFFFF7D6).copy(alpha = alpha))
    drawPath(star, Color(0xFFB45309).copy(alpha = 0.6f * alpha), style = Stroke(width = size * 0.02f))
}

/** Round badge of a time bomb; the number is drawn on top by the caller. Red and pulsing when [urgent]. */
fun DrawScope.drawTimerBadge(center: Offset, cell: Float, urgent: Boolean, pulse: Float = 1f, alpha: Float = 1f) {
    if (cell < 2f || alpha <= 0.01f) return
    val r = cell * 0.3f * (if (urgent) 1f + 0.08f * pulse else 1f)
    if (urgent) {
        drawCircle(
            brush = Brush.radialGradient(listOf(Palette.Danger.copy(alpha = 0.6f * alpha * pulse), Color.Transparent), center = center, radius = r * 1.9f),
            radius = r * 1.9f, center = center,
        )
    }
    drawCircle(Color(0xFF1A1030).copy(alpha = 0.88f * alpha), r, center)
    drawCircle((if (urgent) Palette.Danger else Color(0xFFFF9100)).copy(alpha = alpha), r, center, style = Stroke(width = cell * 0.05f))
    // fuse spark
    drawCircle(Color(0xFFFFD166).copy(alpha = alpha), cell * 0.05f, Offset(center.x + r * 0.7f, center.y - r * 0.7f))
}
