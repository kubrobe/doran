package com.doranalley.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Frosted "liquid glass": a translucent fill that is brighter at the top, an optional colour tint,
 * a specular rim that catches light along the upper edge, a hairline outline and a soft shadow
 * (a coloured glow at night when [glow] is set). Content is not clipped; clip after this if needed.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevation: Dp = 10.dp,
    glow: Color = Color.Unspecified,
    rim: Boolean = true,
    opaque: Float = 0f,
): Modifier {
    val night = C.night
    val shadowColor = if (glow != Color.Unspecified) glow.copy(alpha = 0.45f + 0.35f * night) else C.GlassShadow
    val base = C.Card
    val top = C.GlassTop
    val bottom = C.GlassBottom
    val rimC = C.GlassRim
    val line = C.GlassLine
    return this
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val spread = elevation.toPx()
            val fill = Brush.verticalGradient(listOf(top, bottom))
            val rimBrush = Brush.verticalGradient(0f to rimC, 0.45f to rimC.copy(alpha = 0f), 1f to rimC.copy(alpha = rimC.alpha * 0.25f))
            val sheen = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.10f + 0.06f * (1f - night)), Color.Transparent),
                center = Offset(size.width * 0.22f, 0f), radius = max(size.width, size.height) * 0.75f,
            )
            onDrawBehind {
                // Soft shadow (or coloured glow) outside the shape only, so it never shows through the glass.
                if (spread > 0f) clipPath(path, ClipOp.Difference) {
                    val steps = 7
                    translate(top = spread * (if (glow != Color.Unspecified) 0.1f else 0.35f)) {
                        for (i in steps downTo 1) {
                            val f = i / steps.toFloat()
                            drawPath(path, shadowColor.copy(alpha = shadowColor.alpha * 0.16f), style = Stroke(width = spread * 2.2f * f))
                        }
                    }
                }
                if (opaque > 0f) drawOutline(outline, base.copy(alpha = opaque))
                drawOutline(outline, fill)
                if (tint != Color.Unspecified) drawOutline(outline, Brush.verticalGradient(listOf(tint.copy(alpha = tintAlpha * 1.25f), tint.copy(alpha = tintAlpha * 0.7f))))
                drawOutline(outline, sheen)
                drawOutline(outline, line, style = Stroke(1.dp.toPx()))
                if (rim) drawOutline(outline, rimBrush, style = Stroke(1.4.dp.toPx()))
            }
        }
}

/** A box with a glass background. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevation: Dp = 10.dp,
    glow: Color = Color.Unspecified,
    opaque: Float = 0f,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glass(shape, tint, tintAlpha, elevation, glow, opaque = opaque), contentAlignment = contentAlignment, content = content)
}

/**
 * Stained-glass colour fill (unit cards, letter cards): a rich gradient of [color] with a glossy
 * top sheen and a bright rim, glowing in its own colour at night.
 */
fun Modifier.jewel(color: Color, deep: Color, shape: Shape = RoundedCornerShape(24.dp), elevation: Dp = 14.dp): Modifier {
    val night = C.night
    val glowC = color.copy(alpha = 0.30f + 0.40f * night)
    return this
        .shadow(elevation, shape, clip = false, ambientColor = glowC, spotColor = glowC)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val fill = Brush.linearGradient(listOf(lighten(color, 0.12f), color, deep), start = Offset(0f, 0f), end = Offset(size.width * 0.6f, size.height * 1.4f))
            val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.26f), 0.5f to Color.White.copy(alpha = 0.04f), 1f to Color.Transparent)
            val rim = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.65f), 0.5f to Color.White.copy(alpha = 0.08f), 1f to Color.White.copy(alpha = 0.18f))
            onDrawBehind {
                drawOutline(outline, fill)
                drawOutline(outline, sheen)
                drawOutline(outline, rim, style = Stroke(1.5.dp.toPx()))
            }
        }
}

fun lighten(c: Color, by: Float): Color = Color(
    red = c.red + (1f - c.red) * by, green = c.green + (1f - c.green) * by, blue = c.blue + (1f - c.blue) * by, alpha = c.alpha,
)

fun darken(c: Color, by: Float): Color = Color(red = c.red * (1f - by), green = c.green * (1f - by), blue = c.blue * (1f - by), alpha = c.alpha)

/** A glossy sphere (path nodes, mic and speaker orbs). */
fun DrawScope.drawOrb(color: Color, deep: Color, center: Offset, radius: Float, highlight: Float = 1f) {
    drawCircle(Brush.radialGradient(listOf(lighten(color, 0.22f), color, deep), center = center + Offset(-radius * 0.35f, -radius * 0.45f), radius = radius * 1.65f), radius, center)
    // inner rim light along the bottom (light bouncing inside the glass)
    drawCircle(
        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.White.copy(alpha = 0.22f * highlight), startY = center.y - radius, endY = center.y + radius),
        radius, center,
    )
    // specular highlight
    drawOval(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f * highlight), Color.White.copy(alpha = 0f)), startY = center.y - radius * 0.86f, endY = center.y - radius * 0.1f),
        topLeft = Offset(center.x - radius * 0.58f, center.y - radius * 0.86f), size = Size(radius * 1.16f, radius * 0.72f),
    )
    drawCircle(Color.White.copy(alpha = 0.35f * highlight), radius, center, style = Stroke(radius * 0.035f))
}

/**
 * The living backdrop behind every screen: an ink-to-deep gradient, slow lantern glows, a hanok
 * lattice (띠살문) fading down from the top, and drifting lantern dust. [warm] (0..1) brightens the
 * lantern glow for celebrations.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, warm: Float = 0f) {
    val t = rememberInfiniteTransition(label = "ambient")
    val drift = t.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "drift")
    val warmth by animateFloatAsState(warm, tween(900), label = "warm")
    val dust = remember {
        val r = Random(11)
        List(22) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, r.nextFloat(), 0.4f + r.nextFloat() * 0.8f) }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val night = C.night
        val d = drift.value
        drawRect(Brush.verticalGradient(listOf(C.Bg, C.BgDeep)))
        val a = d * 2f * PI.toFloat()
        // lantern glows
        glow(C.GlowA, Offset(w * (0.86f + 0.05f * sin(a)), h * (0.10f + 0.03f * cos(a))), w * (0.75f + 0.25f * warmth), 0.20f + 0.12f * night + 0.25f * warmth)
        glow(C.GlowB, Offset(w * (0.05f + 0.06f * cos(a)), h * (0.62f + 0.04f * sin(a))), w * 0.80f, 0.14f + 0.08f * night)
        glow(C.GlowC, Offset(w * (0.70f + 0.06f * sin(a + 1.7f)), h * (1.02f)), w * 0.85f, 0.16f + 0.14f * night)
        // hanok lattice: vertical slats with paired cross bars, fading out downwards
        val step = 34.dp.toPx()
        val latticeH = h * 0.42f
        val lat = C.Lattice
        val fade = Brush.verticalGradient(listOf(lat, lat.copy(alpha = 0f)), startY = 0f, endY = latticeH)
        var x = step / 2f
        while (x < w) { drawLine(fade, Offset(x, 0f), Offset(x, latticeH), 1.dp.toPx()); x += step }
        var y = step * 0.8f
        var row = 0
        while (y < latticeH) {
            val alpha = (1f - y / latticeH).coerceIn(0f, 1f)
            val c = lat.copy(alpha = lat.alpha * alpha)
            drawLine(c, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            if (row % 2 == 0) drawLine(c, Offset(0f, y + 5.dp.toPx()), Offset(w, y + 5.dp.toPx()), 1.dp.toPx())
            y += step * 1.6f; row++
        }
        // lantern dust rising slowly
        val dustC = C.Dust
        dust.forEach { p ->
            val speed = p[4]
            val py = ((p[1] - d * speed * 1.6f) % 1f + 1f) % 1f
            val px = p[0] + 0.02f * sin((d * 6f + p[3] * 6f) * PI.toFloat())
            val tw = 0.5f + 0.5f * sin((d * 14f * speed + p[3] * 10f) * PI.toFloat())
            val alpha = (0.10f + 0.45f * night) * tw * (0.35f + 0.65f * py)
            val r = p[2].dp.toPx()
            val center = Offset(px * w, py * h)
            drawCircle(dustC.copy(alpha = alpha * 0.35f), r * 3.2f, center)
            drawCircle(dustC.copy(alpha = alpha), r, center)
        }
    }
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha.coerceIn(0f, 1f)), color.copy(alpha = 0f)), center = center, radius = radius), radius, center)
}

/**
 * A dancheong band: a row of lotus-diamond motifs between coloured bars, in the five traditional
 * colours. Decorative trim for unit cards, popups and results.
 */
@Composable
fun DancheongBand(modifier: Modifier = Modifier, colors: List<Color> = C.Festive, alpha: Float = 1f) {
    Canvas(modifier) {
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

/** Slowly turning rays of light (lesson complete, streak). */
@Composable
fun LightRays(modifier: Modifier = Modifier, color: Color = C.Gold, rays: Int = 14, strength: Float = 1f) {
    val t = rememberInfiniteTransition(label = "rays")
    val turn = t.animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "turn")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(900, easing = Motion.EaseOut)) }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.75f * (0.6f + 0.4f * appear.value)
        val a = appear.value * strength
        rotate(turn.value, c) {
            for (i in 0 until rays) {
                val ang = i * 2f * PI.toFloat() / rays
                val spread = PI.toFloat() / rays * 0.45f
                val p = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(ang - spread) * r, c.y + sin(ang - spread) * r)
                    lineTo(c.x + cos(ang + spread) * r, c.y + sin(ang + spread) * r)
                    close()
                }
                drawPath(p, Brush.radialGradient(listOf(color.copy(alpha = 0.30f * a), color.copy(alpha = 0f)), center = c, radius = r))
            }
        }
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * a), color.copy(alpha = 0f)), center = c, radius = r * 0.55f), r * 0.55f, c)
    }
}

/** A one-off ring of light that expands and fades (a correct answer, a new letter). */
@Composable
fun GlowBurst(modifier: Modifier = Modifier, color: Color, key: Any? = Unit, durationMs: Int = 700) {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(durationMs, easing = Motion.EaseOut)) }
    Canvas(modifier) {
        val v = p.value
        if (v >= 1f) return@Canvas
        val r = size.minDimension / 2f * (0.35f + 0.65f * v)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.40f * (1f - v)), color.copy(alpha = 0f)), center = center, radius = r), r)
        drawCircle(color.copy(alpha = 0.7f * (1f - v)), r * 0.92f, style = Stroke((6f * (1f - v) + 1f) * density))
        // sparks
        for (i in 0 until 10) {
            val ang = i * 36f * PI.toFloat() / 180f + 0.3f
            val d1 = r * (0.7f + 0.35f * v)
            val d2 = d1 + r * 0.18f * (1f - v)
            drawLine(color.copy(alpha = 0.9f * (1f - v)), center + Offset(cos(ang) * d1, sin(ang) * d1), center + Offset(cos(ang) * d2, sin(ang) * d2), 3f * density, StrokeCap.Round)
        }
    }
}

/**
 * An ink-brush circle (ensō) that paints itself around content once: thick at the start,
 * tapering as the brush lifts.
 */
@Composable
fun BrushCircle(modifier: Modifier = Modifier, color: Color, delayMs: Int = 150, durationMs: Int = 900, width: Dp = 7.dp) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durationMs, delayMs, Motion.EaseInOut)) }
    Canvas(modifier.graphicsLayer { alpha = color.alpha; compositingStrategy = CompositingStrategy.Offscreen }) {
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

/** A light sweep that crosses a shape once (used when a button becomes ready). */
fun DrawScope.drawSheen(progress: Float, shapeRadius: Float) {
    if (progress <= 0f || progress >= 1f) return
    val w = size.width
    val x = -w * 0.4f + progress * w * 1.8f
    translate(left = x) {
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent), startX = 0f, endX = w * 0.35f),
            size = Size(w * 0.35f, size.height), cornerRadius = CornerRadius(shapeRadius),
        )
    }
}
