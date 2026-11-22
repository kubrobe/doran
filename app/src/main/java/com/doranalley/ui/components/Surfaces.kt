package com.doranalley.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Tone

/**
 * Surface levels. Calm, flat surfaces carry the learning content; colour and glow are kept for
 * the game layer. At night depth comes from lighter surfaces and a faint top hairline (shadows
 * vanish on near-black); by day from a soft shadow and an outline.
 */
enum class Level { Sunken, Flat, Raised, Overlay }

fun Modifier.surface(level: Level = Level.Raised, shape: Shape = RoundedCornerShape(Radius.card), color: Color = Color.Unspecified): Modifier {
    val dark = C.isDark
    val fill = when {
        color != Color.Unspecified -> color
        level == Level.Sunken -> C.Bg2
        level == Level.Overlay -> C.SurfaceRaised
        else -> C.Surface
    }
    val outline = C.Outline
    val shadowC = C.Shadow
    val lit = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), outline))
    return when (level) {
        Level.Sunken -> this.clip(shape).background(fill)
        Level.Flat -> this.clip(shape).background(fill).border(1.dp, outline, shape)
        Level.Raised ->
            if (dark) this.clip(shape).background(fill).border(1.dp, lit, shape)
            else this.shadow(2.dp, shape, clip = false, ambientColor = shadowC, spotColor = shadowC).clip(shape).background(fill).border(1.dp, outline, shape)
        Level.Overlay ->
            if (dark) this.clip(shape).background(fill).border(1.dp, lit, shape)
            else this.shadow(12.dp, shape, clip = false, ambientColor = shadowC, spotColor = shadowC).clip(shape).background(fill).border(1.dp, outline, shape)
    }
}

enum class CardStyle { Flat, Raised, Interactive, Highlighted }

/**
 * A card. [CardStyle.Interactive] is tappable (press feedback, button semantics);
 * [CardStyle.Highlighted] uses a soft tint and an outline in [tone] to promote one item.
 */
@Composable
fun DoranCard(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.Raised,
    tone: Tone = C.accent,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
    shape: Shape = RoundedCornerShape(Radius.card),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val base = when (style) {
        CardStyle.Flat -> Modifier.surface(Level.Flat, shape)
        CardStyle.Raised, CardStyle.Interactive -> Modifier.surface(Level.Raised, shape)
        CardStyle.Highlighted -> Modifier.clip(shape).background(tone.soft).border(1.5.dp, tone.fill.copy(alpha = 0.7f), shape)
    }
    Column(
        modifier
            .tagged(tag)
            .then(if (onClick != null) Modifier.pressScale(source, scale = 0.98f) else Modifier)
            .then(base)
            .then(if (onClick != null) Modifier.clickable(source, null, role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** A centred dialog on the overlay surface. Opens with a short pop; closes at once. */
@Composable
fun DoranDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier, tag: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier.enterPop(from = 0.94f, durationMs = 220).fillMaxWidth()
                .surface(Level.Overlay, RoundedCornerShape(Radius.sheet))
                .padding(24.dp)
                .tagged(tag),
            content = content,
        )
    }
}

/**
 * A bottom sheet: slides up over a dimmed screen; tapping above it or pressing back closes it.
 * It is a dialog window, so it also covers the tab bar.
 */
@Composable
fun DoranSheet(onDismiss: () -> Unit, tag: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClickLabel = "Close", onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
            Column(
                Modifier.fillMaxWidth()
                    .enterRise(distance = 40.dp, durationMs = Motion.SCREEN)
                    .surface(Level.Overlay, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
                    .tagged(tag),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp).width(36.dp).height(4.dp).clip(CircleShape).background(C.OutlineStrong.copy(alpha = 0.6f)))
                content()
            }
        }
    }
}

/** A thin divider for lists inside cards. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(C.Outline))
}

/** A small round colour dot (legends, list bullets). */
@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
