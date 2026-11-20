package com.doranalley.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.doranalley.R

/**
 * Doran palette: celadon-tinted paper, meok (ink) navy, jade for "go / correct",
 * dancheong red for mistakes, tiger orange for Horangi, and one colour per course section.
 */
object C {
    val Green = Color(0xFF16A974)       // jade
    val GreenDark = Color(0xFF0D8257)
    val GreenLight = Color(0xFFDCF4E9)
    val Blue = Color(0xFF3B7BF2)        // selection
    val BlueDark = Color(0xFF2659C4)
    val BlueLight = Color(0xFFE3ECFF)
    val Red = Color(0xFFE5484D)         // dancheong red
    val RedDark = Color(0xFFB73439)
    val RedLight = Color(0xFFFDE5E5)
    val Orange = Color(0xFFFF8A2A)      // Horangi
    val OrangeDark = Color(0xFFD5661A)
    val Gold = Color(0xFFF5B52E)
    val GoldDark = Color(0xFFC78D10)
    val GoldLight = Color(0xFFFFF3D6)
    val Purple = Color(0xFF4F5BD5)      // cheong indigo (Hangul)
    val PurpleDark = Color(0xFF3540A6)
    val Plum = Color(0xFFC2489B)        // jaju plum
    val PlumDark = Color(0xFF93307A)
    val Ink = Color(0xFF1E2340)         // meok
    val Muted = Color(0xFF7A8197)
    val Line = Color(0xFFE1E7E3)
    val LineDark = Color(0xFFC5CDC8)
    val Bg = Color(0xFFF4F8F5)          // celadon paper
    val Card = Color(0xFFFFFFFF)
    val Soft = Color(0xFFEBF1ED)

    /** Colour (face, edge) for each course section. */
    fun section(name: String): Pair<Color, Color> = when (name) {
        "Hangul" -> Purple to PurpleDark
        "Basics" -> Green to GreenDark
        "Everyday" -> Plum to PlumDark
        else -> Orange to OrangeDark
    }

    /** Colours used for celebrations (the five traditional colours plus Horangi orange). */
    val Festive = listOf(Purple, Red, Gold, Green, Orange, Plum)
}

/** Nunito: rounded and friendly for Latin text. Korean falls back to the phone's own Hangul font. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

private fun s(size: Int, weight: FontWeight = FontWeight.Normal, line: Int? = null) =
    TextStyle(fontFamily = Nunito, fontSize = size.sp, fontWeight = weight, color = C.Ink, lineHeight = (line ?: (size * 1.3).toInt()).sp)

private val Type = Typography(
    displaySmall = s(34, FontWeight.Black),
    headlineMedium = s(27, FontWeight.Black, 32),
    headlineSmall = s(23, FontWeight.ExtraBold),
    titleLarge = s(21, FontWeight.ExtraBold),
    titleMedium = s(17, FontWeight.ExtraBold),
    titleSmall = s(15, FontWeight.Bold),
    bodyLarge = s(17),
    bodyMedium = s(15),
    bodySmall = s(13),
    labelLarge = s(16, FontWeight.ExtraBold),
    labelMedium = s(13, FontWeight.Bold),
)

@Composable
fun DoranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Green, onPrimary = Color.White, secondary = C.Blue, background = C.Bg,
            surface = C.Card, onSurface = C.Ink, onBackground = C.Ink, error = C.Red,
            surfaceContainerHigh = C.Card, surfaceContainerHighest = C.Card,
        ),
        typography = Type,
        content = content,
    )
}
