package com.doranalley.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Palette {
    val Cream = Color(0xFFFBF5EC)
    val Paper = Color(0xFFFFFCF7)
    val Ink = Color(0xFF2F2A26)
    val Muted = Color(0xFF85796E)
    val Persimmon = Color(0xFFD9673E)
    val PersimmonSoft = Color(0xFFF6D9CB)
    val Teal = Color(0xFF3F7F7A)
    val TealSoft = Color(0xFFD5E8E5)
    val Gold = Color(0xFFE9B949)
    val GoldSoft = Color(0xFFFAEFCF)
    val Line = Color(0xFFE7DCCD)
    val Danger = Color(0xFFB8463A)
}

private val colors = lightColorScheme(
    primary = Palette.Persimmon,
    onPrimary = Color.White,
    primaryContainer = Palette.PersimmonSoft,
    onPrimaryContainer = Palette.Ink,
    secondary = Palette.Teal,
    onSecondary = Color.White,
    secondaryContainer = Palette.TealSoft,
    onSecondaryContainer = Palette.Ink,
    tertiary = Palette.Gold,
    tertiaryContainer = Palette.GoldSoft,
    background = Palette.Cream,
    onBackground = Palette.Ink,
    surface = Palette.Paper,
    onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFF3EADD),
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Line,
    error = Palette.Danger,
)

@Composable
fun DoranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
