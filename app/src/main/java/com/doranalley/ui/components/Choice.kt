package com.doranalley.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.TouchTarget

enum class ChoiceState { Normal, Selected, Correct, Wrong, Done }

/**
 * An answer tile (options, letters, match pairs, word-bank words, goal choices). Every state is
 * shown by colour and by shape: selected = thick ring + radio dot, correct = check badge,
 * wrong = cross badge, done = faded with a check. TalkBack hears the same states.
 * It never fixes its height, so it grows with the phone's font size.
 */
@Composable
fun ChoiceTile(
    state: ChoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String? = null,
    role: Role = Role.Button,
    badge: Boolean = true,
    minHeight: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val (fillT, borderT) = when (state) {
        ChoiceState.Normal, ChoiceState.Done -> C.Surface to lerp(C.Outline, C.OutlineStrong, if (C.isDark) 0.15f else 0.35f)
        ChoiceState.Selected -> C.SelectSoft to C.Select
        ChoiceState.Correct -> C.AccentSoft to C.Accent
        ChoiceState.Wrong -> C.HeartSoft to C.Heart
    }
    val fill by animateColorAsState(fillT, tween(Motion.STATE), label = "tileFill")
    val border by animateColorAsState(borderT, tween(Motion.STATE), label = "tileBorder")
    val width by animateDpAsState(if (state == ChoiceState.Normal || state == ChoiceState.Done) 1.5.dp else 2.5.dp, tween(Motion.STATE), label = "tileWidth")
    val source = remember { MutableInteractionSource() }
    val shakeX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val first = remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        if (first.value) { first.value = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        when (state) {
            ChoiceState.Selected -> scale.pop(1.03f)
            ChoiceState.Correct -> scale.pop(1.05f)
            ChoiceState.Wrong -> shakeX.shake(8f)
            else -> {}
        }
    }
    val shape = RoundedCornerShape(Radius.tile)
    val stateText = when (state) {
        ChoiceState.Correct -> "Correct"
        ChoiceState.Wrong -> "Wrong"
        ChoiceState.Done -> "Matched"
        else -> null
    }
    val interaction = if (role == Role.RadioButton || role == Role.Checkbox) {
        Modifier.selectable(state == ChoiceState.Selected, source, null, enabled = enabled, role = role, onClick = onClick)
    } else {
        Modifier.clickable(source, null, enabled = enabled, role = role, onClick = onClick)
    }
    Box(
        modifier
            .tagged(tag)
            .graphicsLayer {
                translationX = shakeX.value.dp.toPx()
                scaleX = scale.value; scaleY = scale.value
            }
            .pressScale(source, enabled)
            .heightIn(min = maxOf(minHeight, TouchTarget))
            .clip(shape)
            .background(fill)
            .border(width, border, shape)
            .then(interaction)
            .semantics { if (stateText != null) stateDescription = stateText },
        contentAlignment = contentAlignment,
    ) {
        val ink = if (state == ChoiceState.Done) C.Muted else C.Text
        CompositionLocalProvider(LocalContentColor provides ink) {
            Box(Modifier.padding(contentPadding).alpha(if (state == ChoiceState.Done) 0.6f else 1f), contentAlignment = contentAlignment, content = content)
        }
        if (badge && state != ChoiceState.Normal) {
            StateBadge(state, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
    }
}

/** The small corner mark that carries a tile's state without relying on colour. */
@Composable
fun StateBadge(state: ChoiceState, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    when (state) {
        ChoiceState.Selected -> Box(modifier.size(size).clip(CircleShape).border(2.dp, C.Select, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * 0.45f).clip(CircleShape).background(C.Select))
        }
        ChoiceState.Correct -> Box(modifier.size(size).clip(CircleShape).background(C.Accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = C.OnAccent, modifier = Modifier.size(size * 0.75f))
        }
        ChoiceState.Wrong -> Box(modifier.size(size).clip(CircleShape).background(C.Heart), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, null, tint = C.OnHeart, modifier = Modifier.size(size * 0.75f))
        }
        ChoiceState.Done -> Box(modifier.size(size).clip(CircleShape).background(C.SurfaceRaised), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = C.Muted, modifier = Modifier.size(size * 0.75f))
        }
        ChoiceState.Normal -> {}
    }
}

data class Segment(val label: String, val tag: String, val icon: ImageVector? = null)

/**
 * A segmented control (theme, daily goal). The selected segment sits on a raised pill that slides
 * between options; each segment is a radio button for TalkBack.
 */
@Composable
fun SegmentedControl(segments: List<Segment>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val n = segments.size.coerceAtLeast(1)
    val pos by animateFloatAsState(selected.coerceIn(0, n - 1).toFloat(), if (reduce) snap() else tween(Motion.STATE + 60, easing = Motion.EaseOut), label = "segment")
    val shape = RoundedCornerShape(Radius.tile + 4.dp)
    val pill = C.SurfaceRaised
    val pillLine = C.Outline
    Row(
        modifier.fillMaxWidth().clip(shape).background(C.Bg2).border(1.dp, C.Outline, shape).padding(4.dp)
            .drawBehind {
                val w = size.width / n
                val r = CornerRadius(Radius.tile.toPx())
                drawRoundRect(pill, topLeft = Offset(w * pos, 0f), size = Size(w, size.height), cornerRadius = r)
                drawRoundRect(pillLine, topLeft = Offset(w * pos, 0f), size = Size(w, size.height), cornerRadius = r, style = Stroke(1.dp.toPx()))
            },
    ) {
        segments.forEachIndexed { i, s ->
            val on = i == selected
            val fg by animateColorAsState(if (on) C.Text else C.Muted, tween(Motion.STATE), label = "segFg")
            Row(
                Modifier.weight(1f).heightIn(min = TouchTarget).clip(RoundedCornerShape(Radius.tile))
                    .selectable(on, role = Role.RadioButton) { onSelect(i) }
                    .tagged(s.tag)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (s.icon != null) Icon(s.icon, null, tint = if (on) C.AccentInk else C.Muted, modifier = Modifier.padding(end = 6.dp).size(18.dp))
                Text(s.label, style = DoranType.label, color = fg, textAlign = TextAlign.Center)
            }
        }
    }
}
