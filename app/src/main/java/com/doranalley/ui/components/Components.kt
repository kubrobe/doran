package com.doranalley.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Nunito

private fun Modifier.tagged(tag: String?) = if (tag != null) this.testTag(tag) else this

/**
 * The main button: a glossy pill of [color] with a specular top, a bright rim and a soft glow in its
 * own colour. It squeezes when pressed, and a light sheen crosses it once when it becomes enabled.
 * A [color] equal to the card colour (or an [outline]) gives a frosted glass button instead.
 */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = C.Green,
    edge: Color = C.GreenDark,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    tag: String? = null,
    icon: ImageVector? = null,
    outline: Color? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed && enabled) 0.965f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "squeeze")
    val on by animateFloatAsState(if (enabled) 1f else 0f, tween(260), label = "on")
    val fg by animateColorAsState(if (enabled) textColor else C.Muted, tween(220), label = "fg")
    val sheen = remember { Animatable(0f) }
    val wasEnabled = remember { mutableStateOf(enabled) }
    LaunchedEffect(enabled) {
        if (enabled && !wasEnabled.value) { sheen.snapTo(0f); sheen.animateTo(1f, tween(750, easing = Motion.EaseInOut)) }
        wasEnabled.value = enabled
    }
    val isGlass = outline != null || color == C.Card
    val shape = RoundedCornerShape(height / 2 + 2.dp)
    val glowC = if (isGlass) Color.Unspecified else color
    Box(
        modifier
            .height(height + 4.dp)
            .padding(vertical = 2.dp)
            .tagged(tag)
            .graphicsLayer { scaleX = squeeze; scaleY = squeeze }
            .then(
                if (isGlass || on < 0.99f) Modifier.glass(shape, elevation = 6.dp)
                else Modifier
            )
            .then(
                if (!isGlass && on > 0f) Modifier.shadow((12f * on).dp, shape, clip = false, ambientColor = glowC.copy(alpha = 0.5f), spotColor = glowC.copy(alpha = 0.6f + 0.3f * C.night))
                else Modifier
            )
            .clip(shape)
            .drawBehind {
                if (!isGlass && on > 0f) {
                    val r = this.size.height / 2f
                    drawRoundRect(Brush.verticalGradient(listOf(lighten(color, 0.14f), color, edge)), cornerRadius = CornerRadius(r), alpha = on)
                    drawRoundRect(
                        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.34f), 0.5f to Color.White.copy(alpha = 0.06f), 0.51f to Color.Transparent),
                        cornerRadius = CornerRadius(r), alpha = on,
                    )
                    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))), cornerRadius = CornerRadius(r), style = Stroke(1.3.dp.toPx()), alpha = on)
                    if (pressed) drawRoundRect(Color.Black.copy(alpha = 0.10f), cornerRadius = CornerRadius(r))
                    drawSheen(sheen.value, r)
                }
            }
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.padding(end = 8.dp).size(22.dp))
            Text(text, color = fg, fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.3.sp)
        }
    }
}

/** Frosted glass button (secondary action). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, textColor: Color = C.BlueInk, enabled: Boolean = true) {
    ChunkyButton(text, onClick, modifier, color = C.Card, edge = C.Line, textColor = textColor, tag = tag, enabled = enabled, outline = C.Line)
}

enum class BoxState { NORMAL, SELECTED, RIGHT, WRONG, DONE }

/** (accent, ink) for a tile state; accent is Unspecified for plain glass. */
private fun boxAccent(state: BoxState): Color = when (state) {
    BoxState.NORMAL, BoxState.DONE -> Color.Unspecified
    BoxState.SELECTED -> C.Blue
    BoxState.RIGHT -> C.Green
    BoxState.WRONG -> C.Red
}

/**
 * Glass tile (options, word tiles, match pairs). Picking it tints the glass and lights its rim;
 * it squeezes when pressed, pops when picked or right, and shakes when wrong.
 */
@Composable
fun SelectBox(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: BoxState = if (selected) BoxState.SELECTED else BoxState.NORMAL,
    enabled: Boolean = true,
    tag: String? = null,
    content: @Composable () -> Unit,
) {
    val target = boxAccent(state)
    val accent by animateColorAsState(if (target == Color.Unspecified) C.Blue.copy(alpha = 0f) else target, tween(180), label = "accent")
    val lit by animateFloatAsState(if (target == Color.Unspecified) 0f else 1f, tween(180), label = "lit")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "squeeze")
    val shakeX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        if (first.value) { first.value = false; return@LaunchedEffect }
        when (state) {
            BoxState.SELECTED, BoxState.RIGHT -> scale.pop(1.05f)
            BoxState.WRONG -> shakeX.shake(9f)
            else -> {}
        }
    }
    val shape = RoundedCornerShape(18.dp)
    val solid = accent.copy(alpha = 1f)
    Box(
        modifier
            .tagged(tag)
            .graphicsLayer {
                translationX = shakeX.value.dp.toPx()
                val sc = scale.value * squeeze
                scaleX = sc; scaleY = sc
            }
            .glass(shape, elevation = (4f + 6f * lit).dp, glow = if (lit > 0.05f) solid else Color.Unspecified)
            .clip(shape)
            .drawBehind {
                if (lit > 0f) {
                    val r = CornerRadius(18.dp.toPx())
                    drawRoundRect(Brush.verticalGradient(listOf(solid.copy(alpha = 0.30f * lit), solid.copy(alpha = 0.14f * lit))), cornerRadius = r)
                    drawRoundRect(solid.copy(alpha = 0.95f * lit), cornerRadius = r, style = Stroke(2.dp.toPx()))
                }
            }
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Glossy blue orb-button; sound waves ripple out while its audio plays. */
@Composable
fun SpeakerButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 72.dp, slow: Boolean = false, tag: String? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "squeeze")
    val grow by animateFloatAsState(if (playing) 1.06f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "grow")
    val wave = if (playing) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "w")
    } else null
    val face = if (slow) lighten(C.Blue, 0.22f) else C.Blue
    val deep = C.BlueDark
    val corner = RoundedCornerShape(size * 0.32f)
    Box(
        modifier
            .size(size)
            .drawBehind {
                val w = wave?.value ?: return@drawBehind
                for (k in 0..1) {
                    val p = (w + k * 0.5f) % 1f
                    val r = this.size.minDimension / 2f * (0.95f + p * 0.5f)
                    drawCircle(face.copy(alpha = 0.38f * (1f - p)), r, style = Stroke(3.dp.toPx()))
                }
            }
            .graphicsLayer { val sc = grow * squeeze; scaleX = sc; scaleY = sc }
            .tagged(tag)
            .shadow(if (size > 50.dp) 14.dp else 8.dp, corner, clip = false, ambientColor = face.copy(alpha = 0.55f), spotColor = face.copy(alpha = 0.7f))
            .clip(corner)
            .drawBehind {
                val r = CornerRadius(this.size.minDimension * 0.32f)
                drawRoundRect(Brush.linearGradient(listOf(lighten(face, 0.18f), face, deep), start = Offset.Zero, end = Offset(this.size.width, this.size.height)), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.32f), 0.48f to Color.White.copy(alpha = 0.05f), 0.5f to Color.Transparent), cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))), cornerRadius = r, style = Stroke(1.3.dp.toPx()))
            }
            .clickable(source, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (slow) Text("🐢", fontSize = (size.value * 0.42f).sp)
        else Icon(Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/** Glass track with a glowing fill that springs forward; a bright bead marks the leading edge. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = C.Green, height: Dp = 14.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.75f, stiffness = 140f), label = "progress")
    val track = C.Line
    Box(
        modifier.height(height).glass(CircleShape, elevation = 0.dp).drawBehind {
            val h = this.size.height
            val r = CornerRadius(h / 2f)
            drawRoundRect(track.copy(alpha = 0.55f), cornerRadius = r)
            val w = (this.size.width * f).coerceAtLeast(if (f > 0.001f) h else 0f)
            if (w > 0f) {
                drawRoundRect(color.copy(alpha = 0.28f), size = Size(w + 6.dp.toPx(), h), cornerRadius = r)
                drawRoundRect(Brush.horizontalGradient(listOf(darken(color, 0.12f), lighten(color, 0.18f)), endX = w), size = Size(w, h), cornerRadius = r)
                drawRoundRect(Color.White.copy(alpha = 0.35f), topLeft = Offset(h * 0.45f, h * 0.2f), size = Size((w - h * 0.9f).coerceAtLeast(0f), h * 0.22f), cornerRadius = CornerRadius(h * 0.11f))
                drawCircle(Color.White.copy(alpha = 0.85f), h * 0.22f, Offset(w - h / 2f, h / 2f))
            }
        },
    )
}

enum class Mood { HAPPY, CHEER, SAD, THINK }

/**
 * Horangi, the app's tiger cub, drawn in code. He breathes and blinks while idle,
 * hops when cheering and droops when sad. Set [animate] to false for a still pose.
 */
@Composable
fun Mascot(modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY, size: Dp = 120.dp, animate: Boolean = true) {
    var bob = 0f
    var blink = 0f
    if (animate) {
        val t = rememberInfiniteTransition(label = "horangi")
        val period = when (mood) { Mood.CHEER -> 520; Mood.SAD -> 2400; else -> 1700 }
        bob = t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob").value
        blink = t.animateFloat(
            0f, 0f,
            infiniteRepeatable(keyframes { durationMillis = 3900; 0f at 3480; 1f at 3540; 1f at 3600; 0f at 3680 }),
            label = "blink",
        ).value
    }
    val hop = when (mood) { Mood.CHEER -> 0.07f; Mood.SAD -> 0.012f; else -> 0.025f }
    val mascotOrange = C.Orange
    val nightMix = C.night
    val shadowC = if (C.isDark) Color(0x66000000) else Color(0x1A1E2340)
    Canvas(
        modifier.size(size).graphicsLayer {
            translationY = -bob * hop * this.size.height
            // A little squash at the bottom of each cheer hop.
            if (mood == Mood.CHEER) { scaleY = 0.96f + 0.04f * bob; scaleX = 1.03f - 0.03f * bob }
            if (mood == Mood.SAD) rotationZ = (bob - 0.5f) * 3f
        },
    ) {
        val w = this.size.width
        val orange = mascotOrange
        val night = nightMix
        val shadow = shadowC
        val dark = Color(0xFF3A2A20)
        val cream = Color(0xFFFFF3E3)
        val cx = w / 2
        val cy = w * 0.55f
        val r = w * 0.38f
        val round = StrokeCap.Round
        // soft ground shadow, and at night a warm lantern glow behind him
        if (night > 0f) drawCircle(Brush.radialGradient(listOf(C.GlowA.copy(alpha = 0.32f * night), Color.Transparent), Offset(cx, cy), r * 1.7f), r * 1.7f, Offset(cx, cy))
        drawOval(shadow, Offset(cx - r * 0.7f, cy + r * 1.02f), Size(r * 1.4f, r * 0.16f))
        // ears
        val earDrop = if (mood == Mood.SAD) r * 0.12f else 0f
        drawCircle(orange, r * 0.32f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(orange, r * 0.32f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        // head
        drawCircle(orange, r, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.16f), r * 0.55f, Offset(cx - r * 0.3f, cy - r * 0.38f))
        if (night > 0f) drawCircle(Color(0xFFFFD58A).copy(alpha = 0.55f * night), r, Offset(cx, cy), style = Stroke(r * 0.04f))
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
        val closed = blink > 0.5f
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
                    if (closed) drawLine(dark, c - Offset(r * 0.11f, 0f), c + Offset(r * 0.11f, 0f), r * 0.07f, round)
                    else {
                        drawCircle(dark, r * 0.11f, c)
                        drawCircle(Color.White, r * 0.04f, c + Offset(r * 0.03f, -r * 0.04f))
                    }
                }
            }
            Mood.SAD -> for (s in listOf(-1f, 1f)) {
                val c = Offset(cx + s * r * 0.42f, eyeY + r * 0.04f)
                if (closed) drawLine(dark, c - Offset(r * 0.09f, 0f), c + Offset(r * 0.09f, 0f), r * 0.07f, round)
                else drawCircle(dark, r * 0.09f, c)
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

/** A glass speech bubble with a tail; holds any content. */
@Composable
fun BubbleBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = remember { BubbleShape() }
    Box(
        modifier.glass(shape, elevation = 8.dp, opaque = 0.35f)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) { content() }
}

/** Speech bubble next to the mascot. */
@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 16.sp) {
    BubbleBox(modifier.enterRise(distance = 8.dp)) {
        Text(text, fontSize = fontSize, color = C.Ink, textAlign = TextAlign.Start, lineHeight = (fontSize.value * 1.35f).sp)
    }
}

/** Glass capsule with an icon + number; the number rolls and the icon pops when it changes. */
@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, tag: String? = null) {
    val pop = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first.value) { first.value = false } else { pop.pop(1.3f) }
    }
    Row(
        modifier.glass(CircleShape, elevation = 4.dp).clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .tagged(tag)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.drawBehind {
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.35f), Color.Transparent)), this.size.minDimension / 2f)
        }) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        AnimatedContent(
            value,
            transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
            label = "stat",
        ) { v ->
            Text(v, color = tint, fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

/** Section title: a small dancheong diamond, the label, and a line fading out. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) {
    val line = C.LineDark
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 8.dp).size(9.dp).graphicsLayer { rotationZ = 45f }.clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.85f)))
        Text(text, color = color, fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.3.sp)
        Box(
            Modifier.padding(start = 10.dp).weight(1f).height(1.5.dp)
                .background(Brush.horizontalGradient(listOf(line, line.copy(alpha = 0f)))),
        )
    }
}

/** A glass card, used for lists and stats. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.glass(shape, elevation = 8.dp).clip(shape)) { content() }
}

@Suppress("unused")
@Composable
private fun KeepImports() = Box(Modifier.fillMaxSize())
