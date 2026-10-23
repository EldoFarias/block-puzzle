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
