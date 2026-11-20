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

/** A chunky button with a darker "edge" underneath; the face sinks onto the edge when pressed. */
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
    val sink by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
    val bg by animateColorAsState(if (enabled) color else C.Line, tween(220), label = "bg")
    val ed by animateColorAsState(if (enabled) edge else C.LineDark, tween(220), label = "edge")
    val fg by animateColorAsState(if (enabled) textColor else C.Muted, tween(220), label = "fg")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(height + 4.dp)
            .tagged(tag)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(height).offset(y = 4.dp).clip(shape).background(ed))
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = sink).clip(shape).background(bg)
                .then(if (outline != null) Modifier.border(2.dp, outline, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // A soft highlight along the top edge gives the face some depth.
            if (enabled && outline == null) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 5.dp)
                        .fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.padding(end = 8.dp).size(22.dp))
                Text(text, color = fg, fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.2.sp)
            }
        }
    }
}

/** White button with a grey outline and edge (secondary action). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, textColor: Color = C.Blue, enabled: Boolean = true) {
    ChunkyButton(text, onClick, modifier, color = C.Card, edge = C.Line, textColor = textColor, tag = tag, enabled = enabled, outline = C.Line)
}

enum class BoxState { NORMAL, SELECTED, RIGHT, WRONG, DONE }

private fun boxColors(state: BoxState): Triple<Color, Color, Color> = when (state) {
    BoxState.NORMAL -> Triple(C.Line, C.Card, C.Line)
    BoxState.SELECTED -> Triple(C.Blue, C.BlueLight, C.Blue)
    BoxState.RIGHT -> Triple(C.Green, C.GreenLight, C.Green)
    BoxState.WRONG -> Triple(C.Red, C.RedLight, C.Red)
    BoxState.DONE -> Triple(C.Line, C.Soft, C.Line)
}

/**
 * Card-like selectable tile (options, word tiles, match pairs). Colours ease between states,
 * the face sinks when pressed, it pops when picked or right, and shakes when wrong.
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
    val (bTarget, bgTarget, edTarget) = boxColors(state)
    val border by animateColorAsState(bTarget, tween(160), label = "border")
    val bg by animateColorAsState(bgTarget, tween(160), label = "bg")
    val edge by animateColorAsState(edTarget, tween(160), label = "edge")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed && enabled) 3.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
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
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .tagged(tag)
            .graphicsLayer {
                translationX = shakeX.value.dp.toPx()
                scaleX = scale.value; scaleY = scale.value
            }
            .clip(shape)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(top = sink)
            .background(edge, shape)
            .padding(bottom = (3.dp - sink).coerceAtLeast(0.dp))
            .background(bg, shape)
            .border(2.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Chunky blue speaker button; sound waves ripple out while its audio plays. */
@Composable
fun SpeakerButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 72.dp, slow: Boolean = false, tag: String? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 4.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
    val grow by animateFloatAsState(if (playing) 1.06f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "grow")
    val wave = if (playing) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "w").value
    } else -1f
    val face = if (slow) Color(0xFF6E9BF5) else C.Blue
    val corner = RoundedCornerShape(size * 0.27f)
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (wave >= 0f) {
                    for (k in 0..1) {
                        val p = (wave + k * 0.5f) % 1f
                        val r = this.size.minDimension / 2f * (0.95f + p * 0.45f)
                        drawCircle(C.Blue.copy(alpha = 0.28f * (1f - p)), r, style = Stroke(3.dp.toPx()))
                    }
                }
            }
            .graphicsLayer { scaleX = grow; scaleY = grow }
            .tagged(tag)
            .clip(corner)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .padding(top = sink)
            .background(C.BlueDark, corner)
            .padding(bottom = (4.dp - sink).coerceAtLeast(0.dp))
            .background(face, corner),
        contentAlignment = Alignment.Center,
    ) {
        if (slow) Text("🐢", fontSize = (size.value * 0.42f).sp)
        else Icon(Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/** Rounded progress bar that springs forward, with a highlight stripe. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = C.Green, height: Dp = 16.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.75f, stiffness = 140f), label = "progress")
    Box(modifier.height(height).clip(CircleShape).background(C.Line)) {
        val w = f.coerceIn(0f, 1f)
        if (w > 0.001f) {
            Box(Modifier.fillMaxWidth(w).fillMaxHeight().clip(CircleShape).background(color)) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = height * 0.22f).height(height / 4)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.32f)).align(Alignment.TopCenter),
                )
            }
        }
    }
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
    Canvas(
        modifier.size(size).graphicsLayer {
            translationY = -bob * hop * this.size.height
            // A little squash at the bottom of each cheer hop.
            if (mood == Mood.CHEER) { scaleY = 0.96f + 0.04f * bob; scaleX = 1.03f - 0.03f * bob }
            if (mood == Mood.SAD) rotationZ = (bob - 0.5f) * 3f
        },
    ) {
        val w = this.size.width
        val orange = C.Orange
        val dark = Color(0xFF3A2A20)
        val cream = Color(0xFFFFF3E3)
        val cx = w / 2
        val cy = w * 0.55f
        val r = w * 0.38f
        val round = StrokeCap.Round
        // soft ground shadow
        drawOval(Color(0x1A1E2340), Offset(cx - r * 0.7f, cy + r * 1.02f), Size(r * 1.4f, r * 0.16f))
        // ears
        val earDrop = if (mood == Mood.SAD) r * 0.12f else 0f
        drawCircle(orange, r * 0.32f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(orange, r * 0.32f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx - r * 0.72f, cy - r * 0.78f + earDrop))
        drawCircle(cream, r * 0.16f, Offset(cx + r * 0.72f, cy - r * 0.78f + earDrop))
        // head
        drawCircle(orange, r, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.16f), r * 0.55f, Offset(cx - r * 0.3f, cy - r * 0.38f))
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

/** A white bubble with a tail; holds any content. */
@Composable
fun BubbleBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = remember { BubbleShape() }
    Box(
        modifier.background(C.Card, shape).border(2.dp, C.Line, shape)
            .padding(start = 22.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    ) { content() }
}

/** Speech bubble next to the mascot. */
@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 16.sp) {
    BubbleBox(modifier.enterRise(distance = 8.dp)) {
        Text(text, fontSize = fontSize, color = C.Ink, textAlign = TextAlign.Start, lineHeight = (fontSize.value * 1.35f).sp)
    }
}

/** Icon + number in the top bar; the number rolls and the icon pops when it changes. */
@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, tag: String? = null) {
    val pop = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first.value) { first.value = false } else { pop.pop(1.3f) }
    }
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .tagged(tag)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
        AnimatedContent(
            value,
            transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
            label = "stat",
        ) { v ->
            Text(v, color = tint, fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Thin labelled divider used for section titles (sentence case, no all-caps eyebrows). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = color, fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Box(Modifier.padding(start = 10.dp).weight(1f).height(2.dp).clip(CircleShape).background(C.Line))
    }
}

/** A white rounded card with a soft edge, used for lists and stats. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.clip(shape).background(C.Line).padding(bottom = 3.dp).clip(shape).background(C.Card).border(2.dp, C.Line, shape)) { content() }
}

@Suppress("unused")
@Composable
private fun KeepImports() = Box(Modifier.fillMaxSize())
