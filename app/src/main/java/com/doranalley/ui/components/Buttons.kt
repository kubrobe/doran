package com.doranalley.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.TouchTarget
import com.doranalley.ui.theme.Tone

internal fun Modifier.tagged(tag: String?) = if (tag != null) this.testTag(tag) else this

enum class ButtonVariant {
    /** The one main action on a screen: a solid pill in the tone's colour. */
    Primary,
    /** A second option next to a primary one: a raised surface with an outline. */
    Secondary,
    /** Low-emphasis action: coloured text on no background. */
    Tertiary,
    /** A confirmed destructive action: a solid red pill. */
    Destructive,
}

private data class ButtonColors(val bg: Color, val fg: Color, val border: Color)

private fun buttonColors(variant: ButtonVariant, tone: Tone, enabled: Boolean): ButtonColors = when {
    !enabled && variant == ButtonVariant.Tertiary -> ButtonColors(Color.Transparent, C.Disabled, Color.Transparent)
    !enabled -> ButtonColors(C.DisabledBg, C.Disabled, Color.Transparent)
    variant == ButtonVariant.Primary -> ButtonColors(tone.fill, tone.on, Color.Transparent)
    variant == ButtonVariant.Secondary -> ButtonColors(C.SurfaceRaised, C.Text, C.Outline)
    variant == ButtonVariant.Tertiary -> ButtonColors(Color.Transparent, tone.ink, Color.Transparent)
    else -> ButtonColors(C.Heart, C.OnHeart, Color.Transparent)
}

/**
 * The app's button. At least 52 dp tall and as wide as its modifier allows; it grows with the
 * phone's font size instead of clipping. Pressing shrinks it slightly and darkens it.
 */
@Composable
fun DoranButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    tone: Tone = C.accent,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tag: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = buttonColors(variant, tone, enabled)
    val bg by animateColorAsState(colors.bg, tween(Motion.STATE), label = "btnBg")
    val fg by animateColorAsState(colors.fg, tween(Motion.STATE), label = "btnFg")
    val shape = RoundedCornerShape(26.dp)
    val overlay = if (C.isDark && variant != ButtonVariant.Primary && variant != ButtonVariant.Destructive) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.10f)
    Box(
        modifier
            .tagged(tag)
            .pressScale(source, enabled)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(bg)
            .then(if (colors.border != Color.Transparent) Modifier.border(1.5.dp, colors.border, shape) else Modifier)
            .drawWithContent { drawContent(); if (pressed && enabled) drawRect(overlay) }
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            Text(text, color = fg, style = DoranType.button, textAlign = TextAlign.Center)
        }
    }
}

enum class IconStyle { Plain, Tonal, Outlined, Filled }

/**
 * A round icon button with a 48 dp touch target. [Tonal] sits on a soft circle, [Outlined] on a
 * surface with a hairline, [Filled] on the tone colour; [Plain] is just the icon.
 */
@Composable
fun DoranIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconStyle = IconStyle.Outlined,
    tone: Tone = C.accent,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
    size: Dp = TouchTarget,
    tag: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val (bg, border, fg) = when (style) {
        IconStyle.Plain -> Triple(Color.Transparent, Color.Transparent, C.Text2)
        IconStyle.Tonal -> Triple(tone.soft, Color.Transparent, tone.ink)
        IconStyle.Outlined -> Triple(C.Surface, C.Outline, C.Text)
        IconStyle.Filled -> Triple(tone.fill, Color.Transparent, tone.on)
    }
    val iconTint = if (!enabled) C.Disabled else if (tint != Color.Unspecified) tint else fg
    val label = contentDescription
    Box(
        modifier
            .tagged(tag)
            .sizeIn(minWidth = TouchTarget, minHeight = TouchTarget)
            .size(size)
            .pressScale(source, enabled, 0.92f)
            .clip(CircleShape)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val visual = if (style == IconStyle.Plain) size else (size - 8.dp).coerceAtLeast(36.dp)
        Box(
            Modifier.size(visual).clip(CircleShape).background(bg)
                .then(if (border != Color.Transparent) Modifier.border(1.dp, border, CircleShape) else Modifier)
                .drawWithContent { drawContent(); if (pressed && enabled) drawRect(C.Text.copy(alpha = 0.08f)) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size((visual.value * 0.5f).coerceIn(20f, 44f).dp))
        }
    }
}
