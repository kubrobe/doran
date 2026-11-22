package com.doranalley.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

enum class Mood { HAPPY, CHEER, SAD, THINK }

/** Horangi's orange stays the same in both themes so he is always recognisable. */
private val HorangiOrange = Color(0xFFFF9447)
private val HorangiDark = Color(0xFF3A2A20)
private val HorangiCream = Color(0xFFFFF3E3)

/**
 * Horangi, the app's tiger cub, drawn in code. He is still while the learner works and reacts
 * once when his mood changes: a hop when cheering, a small droop when sad, a head tilt when
 * thinking. With reduced motion (or [animate] = false) he simply changes expression.
 */
@Composable
fun Mascot(modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY, size: Dp = 120.dp, animate: Boolean = true) {
    val reduce = LocalReduceMotion.current
    val t = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        if (!animate || reduce) return@LaunchedEffect
        t.snapTo(0f)
        t.animateTo(1f, tween(if (mood == Mood.CHEER) 720 else 520, easing = Motion.EaseInOut))
    }
    val shadowC = if (C.isDark) Color(0x66000000) else Color(0x1A1E2340)
    Canvas(
        modifier.size(size).graphicsLayer {
            val p = t.value
            when (mood) {
                // Two hops that settle.
                Mood.CHEER -> {
                    val hop = kotlin.math.abs(sin(p * 2f * PI.toFloat())) * (1f - p)
                    translationY = -hop * 0.10f * this.size.height
                    scaleY = 1f - 0.03f * (1f - hop) * (1f - p)
                }
                Mood.SAD -> { translationY = sin(p * PI.toFloat()) * 0.03f * this.size.height; rotationZ = -sin(p * PI.toFloat()) * 4f }
                Mood.THINK -> rotationZ = sin(p * PI.toFloat()) * 6f
                Mood.HAPPY -> translationY = -sin(p * PI.toFloat()) * 0.03f * this.size.height
            }
        },
    ) {
        val w = this.size.width
        val orange = HorangiOrange
        val dark = HorangiDark
        val cream = HorangiCream
        val cx = w / 2
        val cy = w * 0.55f
        val r = w * 0.38f
        val round = StrokeCap.Round
        drawOval(shadowC, Offset(cx - r * 0.7f, cy + r * 1.02f), Size(r * 1.4f, r * 0.16f))
        // ears
        val earDrop = if (mood == Mood.SAD) r * 0.12f else 0f
        drawCircle(orange, r * 0.32f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(orange, r * 0.32f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        // head
        drawCircle(orange, r, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.14f), r * 0.55f, Offset(cx - r * 0.3f, cy - r * 0.38f))
        // stripes
        val stripe = Stroke(width = r * 0.09f, cap = round)
        drawLine(dark, Offset(cx, cy - r * 0.98f), Offset(cx, cy - r * 0.68f), r * 0.1f, round)
        drawLine(dark, Offset(cx - r * 0.25f, cy - r * 0.95f), Offset(cx - r * 0.18f, cy - r * 0.7f), r * 0.08f, round)
        drawLine(dark, Offset(cx + r * 0.25f, cy - r * 0.95f), Offset(cx + r * 0.18f, cy - r * 0.7f), r * 0.08f, round)
        for (s in listOf(-1f, 1f)) {
            drawLine(dark, Offset(cx + s * r * 0.98f, cy - r * 0.1f), Offset(cx + s * r * 0.7f, cy - r * 0.05f), r * 0.08f, round)
            drawLine(dark, Offset(cx + s * r * 0.95f, cy + r * 0.2f), Offset(cx + s * r * 0.72f, cy + r * 0.18f), r * 0.08f, round)
        }
        // muzzle
        drawOval(cream, Offset(cx - r * 0.5f, cy + r * 0.05f), Size(r * 1.0f, r * 0.72f))
        // eyes
        val eyeY = cy - r * 0.2f
        when (mood) {
            Mood.CHEER -> for (s in listOf(-1f, 1f)) {
                val p = Path().apply {
                    moveTo(cx + s * r * 0.45f - r * 0.14f, eyeY + r * 0.04f)
                    quadraticTo(cx + s * r * 0.45f, eyeY - r * 0.16f, cx + s * r * 0.45f + r * 0.14f, eyeY + r * 0.04f)
                }
                drawPath(p, dark, style = stripe)
            }
            Mood.HAPPY, Mood.THINK -> {
                val look = if (mood == Mood.THINK) r * 0.06f else 0f
                for (s in listOf(-1f, 1f)) {
                    val c = Offset(cx + s * r * 0.42f + look, eyeY - (if (mood == Mood.THINK) r * 0.04f else 0f))
                    drawCircle(dark, r * 0.11f, c)
                    drawCircle(Color.White, r * 0.04f, c + Offset(r * 0.03f, -r * 0.04f))
                }
            }
            Mood.SAD -> for (s in listOf(-1f, 1f)) {
                val c = Offset(cx + s * r * 0.42f, eyeY + r * 0.04f)
                drawCircle(dark, r * 0.09f, c)
                drawLine(dark, Offset(cx + s * r * 0.28f, eyeY - r * 0.2f), Offset(cx + s * r * 0.56f, eyeY - r * 0.12f), r * 0.05f, round)
            }
        }
        // nose
        drawOval(dark, Offset(cx - r * 0.13f, cy + r * 0.1f), Size(r * 0.26f, r * 0.17f))
        // mouth
        val mouth = Path().apply {
            if (mood == Mood.SAD) {
                moveTo(cx - r * 0.18f, cy + r * 0.52f); quadraticTo(cx, cy + r * 0.38f, cx + r * 0.18f, cy + r * 0.52f)
            } else {
                moveTo(cx - r * 0.22f, cy + r * 0.38f); quadraticTo(cx, cy + r * (if (mood == Mood.CHEER) 0.66f else 0.52f), cx + r * 0.22f, cy + r * 0.38f)
            }
        }
        drawPath(mouth, dark, style = stripe)
        if (mood != Mood.SAD) {
            val blush = Color(0xFFFF8C9A).copy(alpha = if (mood == Mood.CHEER) 0.6f else 0.35f)
            drawCircle(blush, r * 0.12f, Offset(cx - r * 0.62f, cy + r * 0.2f))
            drawCircle(blush, r * 0.12f, Offset(cx + r * 0.62f, cy + r * 0.2f))
        }
    }
}

/** Rounded speech bubble with a small tail pointing left (toward Horangi). */
class BubbleShape(private val tail: Dp = 10.dp, private val radius: Dp = 16.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val t = tail.toPx()
        val r = radius.toPx().coerceAtMost(size.height / 2f)
        val ty = (size.height / 2f).coerceAtMost(28.dp.toPx())
        val body = Path().apply { addRoundRect(RoundRect(t, 0f, size.width, size.height, CornerRadius(r))) }
        val tip = Path().apply {
            moveTo(t + 2f, ty - t * 0.85f)
            lineTo(0f, ty + t * 0.15f)
            lineTo(t + 2f, ty + t * 0.75f)
            close()
        }
        Outline.Generic(Path.combine(PathOperation.Union, body, tip))
    }
}

/** Horangi's speech bubble: a raised surface with a tail; holds any content. */
@Composable
fun BubbleBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = remember { BubbleShape() }
    Box(
        modifier.surface(Level.Raised, shape)
            .padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) { content() }
}

/** A speech bubble with plain text. */
@Composable
fun Bubble(text: String, modifier: Modifier = Modifier) {
    BubbleBox(modifier.enterRise(distance = 8.dp, durationMs = Motion.STATE + 60)) {
        Text(text, style = DoranType.body, color = C.Text)
    }
}
