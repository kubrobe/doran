package com.doranalley.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.doranalley.R

/** Nunito: rounded and friendly for Latin text. Hangul glyphs fall back to the phone's own Korean font. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** Korean text uses the phone's Korean sans (Noto Sans CJK / One UI) so every phone renders it the same way. */
val Hangul: FontFamily = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight, family: FontFamily = Nunito, spacing: TextUnit = 0.sp) =
    TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = spacing)

/**
 * The type scale. Every text in the app uses one of these; sizes are in sp so they follow the
 * phone's font-size setting. Weights: Regular for reading, SemiBold for secondary emphasis,
 * ExtraBold for titles, numbers and buttons; Korean uses Bold in the system Korean font.
 */
object DoranType {
    val display = style(32, 38, FontWeight.ExtraBold)
    val h1 = style(26, 32, FontWeight.ExtraBold)
    val h2 = style(20, 26, FontWeight.ExtraBold)
    val title = style(17, 22, FontWeight.ExtraBold)
    val body = style(16, 22, FontWeight.Normal)
    val bodySmall = style(14, 20, FontWeight.Normal)
    val label = style(13, 18, FontWeight.ExtraBold, spacing = 0.2.sp)
    val caption = style(12, 16, FontWeight.SemiBold, spacing = 0.2.sp)
    val button = style(16, 22, FontWeight.ExtraBold, spacing = 0.2.sp)
    /** Big numbers on results and statistics. */
    val number = style(32, 38, FontWeight.ExtraBold)
    /** Latin answer options and word-bank words. */
    val option = style(17, 24, FontWeight.SemiBold)

    // Korean
    val koPrompt = style(36, 46, FontWeight.Bold, Hangul)
    val koBody = style(20, 28, FontWeight.Bold, Hangul)
    val koTile = style(22, 30, FontWeight.Bold, Hangul)
    /** A single Hangul letter shown on its own (letter cards, letter questions). */
    val koLetter = style(88, 100, FontWeight.Bold, Hangul)
}

/**
 * A text size that stays the same physical size whatever the phone's font scale. Only for text
 * that acts as a graphic inside a fixed shape (the seal mark, a letter inside a path node, faint
 * watermarks); reading text always uses the scalable styles above.
 */
@Composable
fun Dp.fixedSp(): TextUnit = with(LocalDensity.current) { this@fixedSp.toSp() }

/** Material's slots mapped onto the Doran scale, so stock components (dialogs, switches, text fields) match. */
fun doranTypography(ink: Color) = Typography(
    displayLarge = DoranType.display.copy(color = ink),
    displayMedium = DoranType.display.copy(color = ink),
    displaySmall = DoranType.display.copy(color = ink),
    headlineLarge = DoranType.h1.copy(color = ink),
    headlineMedium = DoranType.h1.copy(color = ink),
    headlineSmall = DoranType.h2.copy(color = ink),
    titleLarge = DoranType.h2.copy(color = ink),
    titleMedium = DoranType.title.copy(color = ink),
    titleSmall = DoranType.label.copy(color = ink),
    bodyLarge = DoranType.body.copy(color = ink),
    bodyMedium = DoranType.bodySmall.copy(color = ink),
    bodySmall = DoranType.caption.copy(color = ink),
    labelLarge = DoranType.button.copy(color = ink),
    labelMedium = DoranType.label.copy(color = ink),
    labelSmall = DoranType.caption.copy(color = ink),
)
