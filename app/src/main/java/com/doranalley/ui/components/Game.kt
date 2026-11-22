package com.doranalley.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Tone

private val PillValue = DoranType.bodySmall.copy(fontWeight = FontWeight.ExtraBold)

/**
 * A game stat (streak, today's XP, hearts): icon + number on a pill. The number rolls and the icon
 * pops when the value changes. [progress] draws a small ring around the icon (daily goal).
 * TalkBack reads [description], e.g. "Hearts: 5".
 */
@Composable
fun StatPill(
    icon: ImageVector,
    value: String,
    tint: Color,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
    progress: Float? = null,
    valueColor: Color = C.Text,
) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first.value) first.value = false else if (!reduce) pop.pop(1.25f)
    }
    val track = C.Outline
    Row(
        modifier
            .tagged(tag)
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Details", onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .heightIn(min = 40.dp)
            .background(C.Surface, CircleShape)
            .border(1.dp, C.Outline, CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .drawBehind {
                    val p = progress ?: return@drawBehind
                    val st = 2.5.dp.toPx()
                    val tl = Offset(st / 2, st / 2)
                    val sz = Size(size.width - st, size.height - st)
                    drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(st))
                    if (p > 0f) drawArc(tint, -90f, 360f * p.coerceIn(0f, 1f), false, tl, sz, style = Stroke(st, cap = StrokeCap.Round))
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(if (progress != null) 15.dp else 20.dp))
        }
        AnimatedContent(
            value,
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None
                else (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "stat",
            modifier = Modifier.clearAndSetSemantics { },
        ) { v ->
            Text(v, color = valueColor, style = PillValue, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** A small reward/label chip such as "+10 XP" or "New word". */
@Composable
fun RewardChip(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(CircleShape).background(tone.soft).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = tone.ink, modifier = Modifier.padding(end = 4.dp).size(16.dp))
        Text(text, style = DoranType.label, color = tone.ink)
    }
}

/**
 * The one progress bar. A rounded track with a fill that springs forward; tall bars (lesson
 * progress) get a soft highlight along the top of the fill.
 */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = C.Accent,
    height: Dp = 12.dp,
    track: Color = C.SurfaceRaised,
    description: String? = null,
) {
    val reduce = LocalReduceMotion.current
    val f by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        if (reduce) snap() else spring(dampingRatio = 0.8f, stiffness = 160f),
        label = "progress",
    )
    val outline = C.Outline
    Box(
        modifier.fillMaxWidth().height(height)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
                if (description != null) contentDescription = description
            }
            .drawBehind {
                val h = size.height
                val r = CornerRadius(h / 2f)
                drawRoundRect(track, cornerRadius = r)
                if (!C.isDark) drawRoundRect(outline, cornerRadius = r, style = Stroke(1.dp.toPx()))
                val w = (size.width * f).coerceAtLeast(if (f > 0.001f) h else 0f)
                if (w > 0f) {
                    drawRoundRect(color, size = Size(w, h), cornerRadius = r)
                    if (h >= 10.dp.toPx() && w > h) {
                        drawRoundRect(
                            Color.White.copy(alpha = 0.28f), topLeft = Offset(h * 0.4f, h * 0.2f),
                            size = Size(w - h * 0.8f, h * 0.24f), cornerRadius = CornerRadius(h * 0.12f),
                        )
                    }
                }
            },
    )
}

/** A circular progress ring (speaking score, goals). */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, color: Color = C.Accent, stroke: Dp = 8.dp, track: Color = C.SurfaceRaised) {
    Canvas(modifier) {
        val st = stroke.toPx()
        val tl = Offset(st / 2, st / 2)
        val sz = Size(size.width - st, size.height - st)
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(st))
        if (fraction > 0f) drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, sz, style = Stroke(st, cap = StrokeCap.Round))
    }
}
