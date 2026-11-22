package com.doranalley.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Tone

// Migration wrappers: the old component names, drawn by the new design-system components so every
// screen picks up the new look before it is rebuilt. Screens call the new components directly once
// migrated; these are deleted when nothing references them.

private fun toneFor(color: Color): Tone = when (color) {
    C.Select -> C.select
    C.Heart -> C.heart
    C.Streak -> C.streak
    C.Xp -> C.xp
    C.Lavender -> C.lavender
    else -> C.accent
}

@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = C.Accent,
    @Suppress("UNUSED_PARAMETER") edge: Color = C.AccentStrong,
    @Suppress("UNUSED_PARAMETER") textColor: Color = Color.White,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") height: Dp = 52.dp,
    tag: String? = null,
    icon: ImageVector? = null,
    outline: Color? = null,
) {
    val secondary = outline != null || color == C.Surface || color == Color.White
    DoranButton(
        text, onClick, modifier,
        variant = if (secondary) ButtonVariant.Secondary else ButtonVariant.Primary,
        tone = toneFor(color), enabled = enabled, icon = icon, tag = tag,
    )
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, @Suppress("UNUSED_PARAMETER") textColor: Color = C.SelectInk, enabled: Boolean = true) {
    DoranButton(text, onClick, modifier, variant = ButtonVariant.Secondary, enabled = enabled, tag = tag)
}

enum class BoxState { NORMAL, SELECTED, RIGHT, WRONG, DONE }

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
    val s = when (state) {
        BoxState.NORMAL -> ChoiceState.Normal
        BoxState.SELECTED -> ChoiceState.Selected
        BoxState.RIGHT -> ChoiceState.Correct
        BoxState.WRONG -> ChoiceState.Wrong
        BoxState.DONE -> ChoiceState.Done
    }
    ChoiceTile(s, onClick, modifier, enabled = enabled, tag = tag, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) { content() }
}

@Composable
fun SpeakerButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 72.dp, slow: Boolean = false, tag: String? = null) {
    AudioButton(playing, onClick, modifier, size = size, slow = slow, tag = tag)
}

@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, tag: String? = null) {
    StatPill(icon, value, tint, value, modifier, onClick, tag)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = C.Text2) = SectionHeader(text, modifier, color)

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.surface(Level.Raised, RoundedCornerShape(Radius.card))) { content() }
}
