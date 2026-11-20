package com.doranalley.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.ui.theme.C
import androidx.compose.foundation.Canvas

/** A chunky button with a darker "edge" underneath that sinks when pressed. */
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
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg = if (enabled) color else C.Line
    val ed = if (enabled) edge else C.LineDark
    val fg = if (enabled) textColor else C.Muted
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(height + 4.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(height).offset(y = 4.dp).clip(shape).background(ed))
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = if (pressed) 4.dp else 0.dp).clip(shape).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.padding(end = 8.dp).size(22.dp))
                Text(text.uppercase(), color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.8.sp)
            }
        }
    }
}

/** Outlined white button with a grey edge (secondary action). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, textColor: Color = C.Blue, enabled: Boolean = true) {
    ChunkyButton(text, onClick, modifier.border(2.dp, C.Line, RoundedCornerShape(16.dp)), color = Color.White, edge = C.Line,
        textColor = textColor, tag = tag, enabled = enabled)
}

/** Card-like selectable box (options, tiles, match pairs). */
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
    val (border, bg, edge) = when (state) {
        BoxState.NORMAL -> Triple(C.Line, Color.White, C.Line)
        BoxState.SELECTED -> Triple(C.Blue, C.BlueLight, C.Blue)
        BoxState.RIGHT -> Triple(C.Green, C.GreenLight, C.Green)
        BoxState.WRONG -> Triple(C.Red, C.RedLight, C.Red)
        BoxState.DONE -> Triple(C.Line, C.Soft, C.Line)
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(shape)
            .background(edge)
            .padding(bottom = 3.dp)
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

enum class BoxState { NORMAL, SELECTED, RIGHT, WRONG, DONE }

/** Round blue speaker button; pulses while its audio plays. */
@Composable
fun SpeakerButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 72.dp, slow: Boolean = false, tag: String? = null) {
    val scale by animateFloatAsState(if (playing) 1.08f else 1f, tween(250), label = "spk")
    val color = if (slow) C.Blue.copy(alpha = 0.85f) else C.Blue
    Box(
        modifier
            .size(size * scale)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .background(C.BlueDark)
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (slow) Text("🐢", fontSize = (size.value * 0.42f).sp)
        else Icon(Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = C.Green, height: Dp = 16.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "progress")
    Box(modifier.height(height).clip(CircleShape).background(C.Line)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(height / 4).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)).align(Alignment.TopCenter))
        }
    }
}

enum class Mood { HAPPY, CHEER, SAD, THINK }

/** Horangi, the app's tiger cub mascot, drawn in code. */
@Composable
fun Mascot(modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY, size: Dp = 120.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val orange = C.Orange
        val dark = Color(0xFF3A2A20)
        val cream = Color(0xFFFFF3E3)
        val cx = w / 2
        val cy = w * 0.55f
        val r = w * 0.38f
        // ears
        drawCircle(orange, r * 0.32f, Offset(cx - r * 0.72f, cy - r * 0.78f))
        drawCircle(orange, r * 0.32f, Offset(cx + r * 0.72f, cy - r * 0.78f))
        drawCircle(cream, r * 0.16f, Offset(cx - r * 0.72f, cy - r * 0.78f))
        drawCircle(cream, r * 0.16f, Offset(cx + r * 0.72f, cy - r * 0.78f))
        // head
        drawCircle(orange, r, Offset(cx, cy))
        // stripes
        val stripe = Stroke(width = r * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(dark, Offset(cx, cy - r * 0.98f), Offset(cx, cy - r * 0.68f), r * 0.1f, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(dark, Offset(cx - r * 0.25f, cy - r * 0.95f), Offset(cx - r * 0.18f, cy - r * 0.7f), r * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(dark, Offset(cx + r * 0.25f, cy - r * 0.95f), Offset(cx + r * 0.18f, cy - r * 0.7f), r * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
        for (s in listOf(-1f, 1f)) {
            drawLine(dark, Offset(cx + s * r * 0.98f, cy - r * 0.1f), Offset(cx + s * r * 0.7f, cy - r * 0.05f), r * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(dark, Offset(cx + s * r * 0.95f, cy + r * 0.2f), Offset(cx + s * r * 0.72f, cy + r * 0.18f), r * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
        }
        // muzzle
        drawOval(cream, Offset(cx - r * 0.5f, cy + r * 0.05f), Size(r * 1.0f, r * 0.72f))
        // eyes
        val eyeY = cy - r * 0.2f
        when (mood) {
            Mood.CHEER, Mood.HAPPY -> {
                for (s in listOf(-1f, 1f)) {
                    val p = Path().apply {
                        moveTo(cx + s * r * 0.45f - r * 0.14f, eyeY + r * 0.04f)
                        quadraticTo(cx + s * r * 0.45f, eyeY - r * 0.16f, cx + s * r * 0.45f + r * 0.14f, eyeY + r * 0.04f)
                    }
                    if (mood == Mood.CHEER) drawPath(p, dark, style = stripe)
                    else drawCircle(dark, r * 0.11f, Offset(cx + s * r * 0.42f, eyeY))
                }
                if (mood == Mood.HAPPY) {
                    drawCircle(Color.White, r * 0.04f, Offset(cx - r * 0.38f, eyeY - r * 0.04f))
                    drawCircle(Color.White, r * 0.04f, Offset(cx + r * 0.46f, eyeY - r * 0.04f))
                }
            }
            Mood.SAD -> for (s in listOf(-1f, 1f)) {
                drawCircle(dark, r * 0.09f, Offset(cx + s * r * 0.42f, eyeY + r * 0.04f))
                drawLine(dark, Offset(cx + s * r * 0.28f, eyeY - r * 0.2f), Offset(cx + s * r * 0.56f, eyeY - r * 0.12f), r * 0.05f)
            }
            Mood.THINK -> for (s in listOf(-1f, 1f)) drawCircle(dark, r * 0.1f, Offset(cx + s * r * 0.42f + r * 0.05f, eyeY - r * 0.03f))
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
        if (mood == Mood.CHEER) {
            drawCircle(Color(0xFFFF9DA7).copy(alpha = 0.6f), r * 0.12f, Offset(cx - r * 0.62f, cy + r * 0.2f))
            drawCircle(Color(0xFFFF9DA7).copy(alpha = 0.6f), r * 0.12f, Offset(cx + r * 0.62f, cy + r * 0.2f))
        }
    }
}

/** Speech bubble next to the mascot. */
@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 16.sp) {
    Box(modifier.border(2.dp, C.Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text, fontSize = fontSize, color = C.Ink, textAlign = TextAlign.Start)
    }
}

@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, tag: String? = null) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Text(value, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

