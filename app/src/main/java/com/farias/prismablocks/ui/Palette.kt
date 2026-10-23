package com.farias.prismablocks.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object Palette {
    val BgTop = Color(0xFF070716)
    val BgBottom = Color(0xFF140C33)
    val AuroraViolet = Color(0xFF7C3AED)
    val AuroraCyan = Color(0xFF06B6D4)
    val AuroraPink = Color(0xFFDB2777)

    val Glass = Color(0x14FFFFFF)
    val GlassBorder = Color(0x26FFFFFF)
    val EmptyCell = Color(0x0FFFFFFF)
    val EmptyCellInner = Color(0x33000000)

    val TextPrimary = Color(0xFFF5F3FF)
    val TextSecondary = Color(0xFFA5A3C9)
    val Gold = Color(0xFFFACC15)
    val Cyan = Color(0xFF22D3EE)
    val Magenta = Color(0xFFE879F9)

    /** Block colors, index 1..7 (index 0 unused). */
    val blocks = listOf(
        Color.Transparent,
        Color(0xFF22D3EE), // electric cyan
        Color(0xFFE879F9), // magenta
        Color(0xFFA3E635), // neon lime
        Color(0xFFFB923C), // sunset orange
        Color(0xFFF43F5E), // hot pink
        Color(0xFF8B5CF6), // royal violet
        Color(0xFFFACC15), // gold
    )

    fun block(index: Int): Color = blocks.getOrElse(index) { blocks[1] }

    fun lighter(c: Color, f: Float = 0.35f) = lerp(c, Color.White, f)
    fun darker(c: Color, f: Float = 0.35f) = lerp(c, Color.Black, f)

    /** Desaturated gray version, used when the game is over. */
    fun gray(c: Color): Color {
        val l = c.red * 0.3f + c.green * 0.59f + c.blue * 0.11f
        return Color(l * 0.55f, l * 0.55f, l * 0.62f, c.alpha)
    }
}
