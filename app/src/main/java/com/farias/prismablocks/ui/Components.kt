package com.farias.prismablocks.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farias.prismablocks.game.Haptic

fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

fun performHaptic(view: View, kind: Haptic) {
    val constant = when (kind) {
        Haptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.PLACE -> HapticFeedbackConstants.KEYBOARD_TAP
        Haptic.CLEAR -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.HEAVY -> HapticFeedbackConstants.LONG_PRESS
    }
    view.performHapticFeedback(constant)
}

object Styles {
    val brandBrush = Brush.linearGradient(listOf(Palette.Cyan, Palette.Magenta))

    @OptIn(ExperimentalTextApi::class)
    fun gradient(sizeSp: Int, glow: Color = Palette.Magenta) = TextStyle(
        brush = brandBrush,
        fontSize = sizeSp.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        shadow = Shadow(color = glow.copy(alpha = 0.55f), offset = Offset.Zero, blurRadius = 30f),
    )

    fun glow(sizeSp: Int, color: Color, weight: FontWeight = FontWeight.ExtraBold) = TextStyle(
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = weight,
        shadow = Shadow(color = color.copy(alpha = 0.8f), offset = Offset.Zero, blurRadius = 22f),
    )

    fun plain(sizeSp: Int, color: Color = Palette.TextPrimary, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = weight,
    )
}

/** Swallows touches so nothing behind an overlay reacts. */
fun Modifier.blockTouches(): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = {},
)

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFDB2777)))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3560), Color(0xFF3A3560)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Styles.plain(18, if (enabled) Color.White else Palette.TextSecondary, FontWeight.ExtraBold))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Styles.plain(17, Palette.TextPrimary, FontWeight.Bold))
    }
}

@Composable
fun GlassCircleButton(size: Dp = 44.dp, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Glass)
            .border(1.dp, Palette.GlassBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.28f
        val r = CornerRadius(w / 2f)
        drawRoundRect(Palette.TextPrimary, Offset(size.width * 0.14f, 0f), Size(w, size.height), r)
        drawRoundRect(Palette.TextPrimary, Offset(size.width * 0.58f, 0f), Size(w, size.height), r)
    }
}

@Composable
fun CrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0.05f * w, 0.78f * h)
            lineTo(0.0f * w, 0.25f * h)
            lineTo(0.30f * w, 0.50f * h)
            lineTo(0.50f * w, 0.10f * h)
            lineTo(0.70f * w, 0.50f * h)
            lineTo(1.0f * w, 0.25f * h)
            lineTo(0.95f * w, 0.78f * h)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFE680), Palette.Gold, Color(0xFFD97706))))
        drawRoundRect(
            Palette.Gold,
            Offset(0.05f * w, 0.84f * h),
            Size(0.9f * w, 0.14f * h),
            CornerRadius(0.05f * h),
        )
    }
}
