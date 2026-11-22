package com.doranalley.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Space
import com.doranalley.ui.theme.TouchTarget

/**
 * Plays Korean audio. Small sizes are a soft lavender circle, 64 dp and up a solid one. While its
 * clip plays, the icon becomes a sound wave and a ring pulses (only for as long as the audio plays).
 * [slow] marks the slow-speed version.
 */
@Composable
fun AudioButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = TouchTarget,
    slow: Boolean = false,
    tag: String? = null,
) {
    val reduce = LocalReduceMotion.current
    val source = remember { MutableInteractionSource() }
    val big = size >= 64.dp
    val bg = if (big) C.Lavender else C.LavenderSoft
    val fg = if (big) C.OnLavender else C.LavenderInk
    val ring = C.Lavender
    val wave = if (playing && !reduce) {
        rememberInfiniteTransition(label = "wave").animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "w")
    } else null
    val visualRadius = size / 2
    Box(
        modifier
            .tagged(tag)
            .size(maxOf(size, TouchTarget))
            .drawBehind {
                val w = wave?.value ?: return@drawBehind
                val r = visualRadius.toPx()
                drawCircle(ring.copy(alpha = 0.45f * (1f - w)), r * (1f + w * 0.35f), style = Stroke(2.dp.toPx()))
            }
            .pressScale(source, scale = 0.92f)
            .clip(CircleShape)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (slow) "Play slowly" else "Play audio"
                if (playing) stateDescription = "Playing"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            val icon = when {
                playing -> Icons.Filled.GraphicEq
                slow -> Icons.Filled.SlowMotionVideo
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }
            Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.46f))
        }
    }
}

/** Section title: a small dancheong diamond, the label, and room for an action on the right. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = C.Text2,
    ornament: Color = C.Accent,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Diamond(ornament.copy(alpha = 0.9f), Modifier.padding(end = 10.dp), size = 7.dp)
        Text(text.uppercase(), style = DoranType.label.copy(letterSpacing = DoranType.label.letterSpacing * 4), color = color,
            modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) trailing()
    }
}

/**
 * A list row (words, guidebook, settings, practice modes): optional leading content, a title and
 * subtitle, optional trailing content. Tappable rows get button semantics and press feedback.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = DoranType.title,
    titleColor: Color = C.Text,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .tagged(tag)
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(source, null, role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(Space.m)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = titleStyle, color = titleColor)
            if (subtitle != null) Text(subtitle, style = DoranType.bodySmall, color = C.Text2)
        }
        if (trailing != null) { Spacer(Modifier.width(Space.s)); trailing() }
    }
}

/** A tinted round icon used at the start of list rows. */
@Composable
fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tone: com.doranalley.ui.theme.Tone, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CircleShape).background(tone.soft), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tone.ink, modifier = Modifier.size(size * 0.55f))
    }
}

/** A top bar with a back button, a title and optional actions. */
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backTag: String = "back",
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = Space.s, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) DoranIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, style = IconStyle.Plain, tint = C.Text, tag = backTag)
        Text(title, style = DoranType.h2, color = C.Text, modifier = Modifier.weight(1f).padding(start = if (onBack != null) Space.xs else Space.m).semantics { heading() })
        if (actions != null) actions()
    }
}

/** Horangi with a short message, for empty lists and first-time screens. */
@Composable
fun EmptyState(mood: Mood, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(Space.x3), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Mascot(mood = mood, size = 104.dp)
        Text(title, style = DoranType.h2, color = C.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.m))
        Text(body, style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.xs))
        if (action != null) { Spacer(Modifier.size(Space.l)); action() }
    }
}
