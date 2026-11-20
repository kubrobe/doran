package com.doranalley.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val Green = Color(0xFF34C77B)
    val GreenDark = Color(0xFF239A5B)
    val GreenLight = Color(0xFFDDF7E8)
    val Blue = Color(0xFF3A8DFF)
    val BlueDark = Color(0xFF2468C9)
    val BlueLight = Color(0xFFE1EEFF)
    val Red = Color(0xFFF2545B)
    val RedDark = Color(0xFFC63B42)
    val RedLight = Color(0xFFFFE3E4)
    val Orange = Color(0xFFFF8A3D)
    val OrangeDark = Color(0xFFD96A22)
    val Gold = Color(0xFFF7C948)
    val GoldDark = Color(0xFFD1A21F)
    val Purple = Color(0xFF8E6CF0)
    val PurpleDark = Color(0xFF6B4BC9)
    val Ink = Color(0xFF2B2D42)
    val Muted = Color(0xFF8A8FA3)
    val Line = Color(0xFFE4E6EE)
    val LineDark = Color(0xFFC9CCD8)
    val Bg = Color(0xFFFFFFFF)
    val Soft = Color(0xFFF6F7FB)

    /** Colour for each course section. */
    fun section(name: String): Pair<Color, Color> = when (name) {
        "Hangul" -> Purple to PurpleDark
        "Basics" -> Green to GreenDark
        "Everyday" -> Blue to BlueDark
        else -> Orange to OrangeDark
    }
}

private val Type = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = C.Ink),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = C.Ink),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.Ink),
    bodyLarge = TextStyle(fontSize = 17.sp, color = C.Ink),
    bodyMedium = TextStyle(fontSize = 15.sp, color = C.Ink),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
)

@Composable
fun DoranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Green, onPrimary = Color.White, secondary = C.Blue, background = C.Bg,
            surface = C.Bg, onSurface = C.Ink, onBackground = C.Ink, error = C.Red,
        ),
        typography = Type,
        content = content,
    )
}
