package com.doranalley.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The backdrop behind every screen: the plain background colour with a faint hanok lattice (띠살문)
 * fading down from the top. It is static. [warm] (0..1) fades in a soft gold glow for celebrations.
 */
@Composable
fun DoranBackdrop(modifier: Modifier = Modifier, warm: Float = 0f) {
    val reduce = LocalReduceMotion.current
    val warmth by animateFloatAsState(warm, tween(if (reduce) 0 else 700), label = "warm")
    Canvas(modifier) {
        val w = size.width
        drawRect(C.Bg)
        val step = 36.dp.toPx()
        val latticeH = size.height * 0.30f
        val lat = C.Lattice
        val fade = Brush.verticalGradient(listOf(lat, lat.copy(alpha = 0f)), startY = 0f, endY = latticeH)
        var x = step / 2f
        while (x < w) { drawLine(fade, Offset(x, 0f), Offset(x, latticeH), 1.dp.toPx()); x += step }
        var y = step * 0.8f
        var row = 0
        while (y < latticeH) {
            val a = (1f - y / latticeH).coerceIn(0f, 1f)
            val c = lat.copy(alpha = lat.alpha * a)
            drawLine(c, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            if (row % 2 == 0) drawLine(c, Offset(0f, y + 5.dp.toPx()), Offset(w, y + 5.dp.toPx()), 1.dp.toPx())
            y += step * 1.6f; row++
        }
        if (warmth > 0f) {
            val center = Offset(w / 2f, size.height * 0.30f)
            val radius = w * 0.9f
            drawCircle(Brush.radialGradient(listOf(C.Xp.copy(alpha = 0.16f * warmth), Color.Transparent), center, radius), radius, center)
        }
    }
}

/**
 * A dancheong band: lotus-diamond motifs between coloured bars, in the traditional colours.
 * Used sparingly: section starts and big wins.
 */
@Composable
fun DancheongBand(modifier: Modifier = Modifier, colors: List<Color> = C.Festive, alpha: Float = 1f) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val h = size.height
        val unit = h * 1.6f
        val n = (size.width / unit).toInt() + 1
        val bar = h * 0.16f
        drawRect(colors[0].copy(alpha = 0.85f * alpha), size = Size(size.width, bar))
        drawRect(colors[2].copy(alpha = 0.85f * alpha), topLeft = Offset(0f, h - bar), size = Size(size.width, bar))
        for (i in 0 until n) {
            val cx = i * unit + unit / 2f
            val cy = h / 2f
            val c = colors[(i + 3) % colors.size].copy(alpha = alpha)
            val r = (h - bar * 2f) * 0.42f
            val diamond = Path().apply { moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close() }
            drawPath(diamond, c)
            drawCircle(Color.White.copy(alpha = 0.75f * alpha), r * 0.32f, Offset(cx, cy))
            drawLine(colors[(i + 1) % colors.size].copy(alpha = 0.6f * alpha), Offset(cx + unit / 2f, bar), Offset(cx + unit / 2f, h - bar), h * 0.08f)
        }
    }
}

/** The small diamond (마름모) used before section titles. */
@Composable
fun Diamond(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).graphicsLayer { rotationZ = 45f }.clip(RoundedCornerShape(2.dp)).background(color))
}

/**
 * The app mark: "도" on a jade seal (도장), like a stamp on hanji. Decorative; the wordmark beside it
 * carries the name for TalkBack.
 */
@Composable
fun SealMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(C.Accent).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text("도", style = DoranType.koBody.copy(fontSize = DoranType.koBody.fontSize * (size.value / 36f)), color = C.OnAccent)
    }
}

/**
 * Rays of light that fan out and turn a little, once, behind a big win, then rest. Not drawn with
 * reduced motion.
 */
@Composable
fun LightRays(modifier: Modifier = Modifier, color: Color = C.Xp, rays: Int = 14, strength: Float = 1f) {
    if (LocalReduceMotion.current) return
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(2600, easing = Motion.EaseOut)) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val c = center
        val p = appear.value
        val r = size.minDimension * 0.75f * (0.6f + 0.4f * p)
        val a = (p * 2f).coerceAtMost(1f) * strength
        rotate(24f * p, c) {
            for (i in 0 until rays) {
                val ang = i * 2f * PI.toFloat() / rays
                val spread = PI.toFloat() / rays * 0.45f
                val path = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(ang - spread) * r, c.y + sin(ang - spread) * r)
                    lineTo(c.x + cos(ang + spread) * r, c.y + sin(ang + spread) * r)
                    close()
                }
                drawPath(path, Brush.radialGradient(listOf(color.copy(alpha = 0.22f * a), color.copy(alpha = 0f)), center = c, radius = r))
            }
        }
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.30f * a), color.copy(alpha = 0f)), center = c, radius = r * 0.55f), r * 0.55f, c)
    }
}

/** A one-off ring of light that expands and fades (a great score). Not drawn with reduced motion. */
@Composable
fun GlowBurst(modifier: Modifier = Modifier, color: Color, key: Any? = Unit, durationMs: Int = 700) {
    if (LocalReduceMotion.current) return
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(durationMs, easing = Motion.EaseOut)) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val v = p.value
        if (v >= 1f) return@Canvas
        val r = size.minDimension / 2f * (0.35f + 0.65f * v)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.32f * (1f - v)), color.copy(alpha = 0f)), center = center, radius = r), r)
        drawCircle(color.copy(alpha = 0.6f * (1f - v)), r * 0.92f, style = Stroke((5f * (1f - v) + 1f) * density))
    }
}

/**
 * An ink-brush circle (ensō) that paints itself around content once: thick at the start,
 * tapering as the brush lifts. With reduced motion it appears already painted.
 */
@Composable
fun BrushCircle(modifier: Modifier = Modifier, color: Color, delayMs: Int = 150, durationMs: Int = 900, width: Dp = 7.dp) {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) p.animateTo(1f, tween(durationMs, delayMs, Motion.EaseInOut)) }
    Canvas(modifier.clearAndSetSemantics { }.graphicsLayer { alpha = color.alpha; compositingStrategy = CompositingStrategy.Offscreen }) {
        val sweep = 330f * p.value
        if (sweep <= 0f) return@Canvas
        val r = size.minDimension / 2f - width.toPx()
        val steps = 60
        val start = -110f
        for (i in 0 until steps) {
            val f0 = i / steps.toFloat()
            if (f0 * 330f > sweep) break
            val a0 = (start + f0 * 330f) * PI.toFloat() / 180f
            val a1 = (start + ((i + 1) / steps.toFloat()).coerceAtMost(sweep / 330f) * 330f) * PI.toFloat() / 180f
            val taper = (1f - f0 * 0.75f)
            val wob = 1f + 0.025f * sin(f0 * 17f)
            drawLine(
                color.copy(alpha = 1f),
                center + Offset(cos(a0) * r * wob, sin(a0) * r * wob), center + Offset(cos(a1) * r * wob, sin(a1) * r * wob),
                width.toPx() * taper, StrokeCap.Round,
            )
        }
    }
}
