package com.doranalley.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Migration shims: the old glass / stained-glass / orb effects, now drawn as the new flat surfaces.
// Screens move to [surface], [DoranCard] and solid fills; these go once nothing calls them.

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevation: Dp = 10.dp,
    @Suppress("UNUSED_PARAMETER") glow: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") rim: Boolean = true,
    @Suppress("UNUSED_PARAMETER") opaque: Float = 0f,
): Modifier {
    val level = when {
        elevation <= 0.dp -> Level.Sunken
        elevation >= 14.dp -> Level.Overlay
        else -> Level.Raised
    }
    val base = this.surface(level, shape)
    return if (tint != Color.Unspecified) base.background(tint.copy(alpha = (tintAlpha * 0.8f).coerceAtMost(0.22f)), shape) else base
}

fun Modifier.jewel(color: Color, @Suppress("UNUSED_PARAMETER") deep: Color, shape: Shape = RoundedCornerShape(24.dp), @Suppress("UNUSED_PARAMETER") elevation: Dp = 14.dp): Modifier =
    this.clip(shape).background(color)

fun lighten(c: Color, by: Float): Color = com.doranalley.ui.theme.lighten(c, by)

fun darken(c: Color, by: Float): Color = com.doranalley.ui.theme.darken(c, by)

fun DrawScope.drawOrb(color: Color, @Suppress("UNUSED_PARAMETER") deep: Color, center: Offset, radius: Float, @Suppress("UNUSED_PARAMETER") highlight: Float = 1f) {
    drawCircle(color, radius, center)
}

@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, warm: Float = 0f) = DoranBackdrop(modifier, warm)
