package com.doranalley.ui.theme

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.doranalley.R

/** Every colour the app uses. Each palette (day, night) gives a value for every token. */
enum class T {
    Green, GreenDark, GreenLight, GreenInk,
    Blue, BlueDark, BlueLight, BlueInk,
    Red, RedDark, RedLight, RedInk,
    Orange, OrangeDark, OrangeInk,
    Gold, GoldDark, GoldLight, GoldInk,
    Purple, PurpleDark, PurpleInk,
    Plum, PlumDark,
    Ink, Muted, Line, LineDark, Bg, BgDeep, Card, Soft,
    GlassTop, GlassBottom, GlassRim, GlassLine, GlassShadow, Scrim,
    Lattice, GlowA, GlowB, GlowC, Dust,
}

class Palette(val dark: Boolean, vararg pairs: Pair<T, Color>) {
    private val colors: Array<Color> = Array(T.entries.size) { Color.Magenta }

    init {
        val set = BooleanArray(T.entries.size)
        pairs.forEach { (t, c) -> colors[t.ordinal] = c; set[t.ordinal] = true }
        val missing = T.entries.filter { !set[it.ordinal] }
        require(missing.isEmpty()) { "Palette is missing $missing" }
    }

    operator fun get(t: T): Color = colors[t.ordinal]
}

/**
 * Day: hanji paper with a celadon tint, meok-ink text, jade for "go".
 * Night ("ink & lantern"): deep meok navy, lantern-gold and jade glows, frosted glass surfaces.
 */
object Palettes {
    val Day = Palette(
        false,
        T.Green to Color(0xFF14A673), T.GreenDark to Color(0xFF0B7F55), T.GreenLight to Color(0xFFDDF4E9), T.GreenInk to Color(0xFF0A774F),
        T.Blue to Color(0xFF3B7BF2), T.BlueDark to Color(0xFF2659C4), T.BlueLight to Color(0xFFE3ECFF), T.BlueInk to Color(0xFF2155BD),
        T.Red to Color(0xFFE5484D), T.RedDark to Color(0xFFB73439), T.RedLight to Color(0xFFFDE6E6), T.RedInk to Color(0xFFAF2F35),
        T.Orange to Color(0xFFFF8A2A), T.OrangeDark to Color(0xFFD5661A), T.OrangeInk to Color(0xFFBF5610),
        T.Gold to Color(0xFFF2B22B), T.GoldDark to Color(0xFFC48A0E), T.GoldLight to Color(0xFFFFF3D6), T.GoldInk to Color(0xFF9C6B04),
        T.Purple to Color(0xFF4F5BD5), T.PurpleDark to Color(0xFF3540A6), T.PurpleInk to Color(0xFF3540A6),
        T.Plum to Color(0xFFC2489B), T.PlumDark to Color(0xFF93307A),
        T.Ink to Color(0xFF1C2140), T.Muted to Color(0xFF6B7290), T.Line to Color(0xFFE0E4DE), T.LineDark to Color(0xFFC3CBC5),
        T.Bg to Color(0xFFF6F4EC), T.BgDeep to Color(0xFFE6EEE7), T.Card to Color(0xFFFFFFFF), T.Soft to Color(0xFFEDF1EC),
        T.GlassTop to Color(0xC7FFFFFF), T.GlassBottom to Color(0x8CFFFFFF), T.GlassRim to Color(0xFFFFFFFF),
        T.GlassLine to Color(0x1C1C2140), T.GlassShadow to Color(0x331C2140), T.Scrim to Color(0x661C2140),
        T.Lattice to Color(0x111C2140), T.GlowA to Color(0xFFFFC98A), T.GlowB to Color(0xFF9FDCC2), T.GlowC to Color(0xFFBFCBFF),
        T.Dust to Color(0xFFE2A93C),
    )

    val Night = Palette(
        true,
        T.Green to Color(0xFF27C98F), T.GreenDark to Color(0xFF0E8A5E), T.GreenLight to Color(0xFF103A2E), T.GreenInk to Color(0xFF74E8BC),
        T.Blue to Color(0xFF5B8CFF), T.BlueDark to Color(0xFF2F5BCF), T.BlueLight to Color(0xFF17264C), T.BlueInk to Color(0xFFA9C2FF),
        T.Red to Color(0xFFFF5F67), T.RedDark to Color(0xFFC23A44), T.RedLight to Color(0xFF3B1621), T.RedInk to Color(0xFFFFA2A7),
        T.Orange to Color(0xFFFF9A3D), T.OrangeDark to Color(0xFFCF6914), T.OrangeInk to Color(0xFFFFBC7D),
        T.Gold to Color(0xFFF6BE45), T.GoldDark to Color(0xFFBF870F), T.GoldLight to Color(0xFF392C0F), T.GoldInk to Color(0xFFFFD98A),
        T.Purple to Color(0xFF7C85FF), T.PurpleDark to Color(0xFF4B55D2), T.PurpleInk to Color(0xFFC3C8FF),
        T.Plum to Color(0xFFE26DBA), T.PlumDark to Color(0xFFA6448A),
        T.Ink to Color(0xFFEEF0F8), T.Muted to Color(0xFF8C94B4), T.Line to Color(0xFF242D4B), T.LineDark to Color(0xFF353F66),
        T.Bg to Color(0xFF0B1021), T.BgDeep to Color(0xFF05080F), T.Card to Color(0xFF161D35), T.Soft to Color(0xFF1B2340),
        T.GlassTop to Color(0x24FFFFFF), T.GlassBottom to Color(0x0DFFFFFF), T.GlassRim to Color(0x66FFFFFF),
        T.GlassLine to Color(0x1FFFFFFF), T.GlassShadow to Color(0x8C000000), T.Scrim to Color(0x99000000),
        T.Lattice to Color(0x0FFFFFFF), T.GlowA to Color(0xFFE59A2F), T.GlowB to Color(0xFF17A777), T.GlowC to Color(0xFF4048C8),
        T.Dust to Color(0xFFFFD27A),
    )
}

/**
 * The current colours. [mix] is 0 for day and 1 for night; it animates when the theme changes,
 * so every colour cross-fades. Reading a colour is a snapshot read, so screens repaint on change.
 */
object C {
    internal var mix by mutableFloatStateOf(1f)

    private fun c(t: T): Color {
        val m = mix
        return when {
            m <= 0f -> Palettes.Day[t]
            m >= 1f -> Palettes.Night[t]
            else -> lerp(Palettes.Day[t], Palettes.Night[t], m)
        }
    }

    val isDark: Boolean get() = mix >= 0.5f
    /** 0 = day, 1 = night (animated). Use to fade night-only touches in and out. */
    val night: Float get() = mix

    val Green get() = c(T.Green)
    val GreenDark get() = c(T.GreenDark)
    val GreenLight get() = c(T.GreenLight)
    val GreenInk get() = c(T.GreenInk)
    val Blue get() = c(T.Blue)
    val BlueDark get() = c(T.BlueDark)
    val BlueLight get() = c(T.BlueLight)
    val BlueInk get() = c(T.BlueInk)
    val Red get() = c(T.Red)
    val RedDark get() = c(T.RedDark)
    val RedLight get() = c(T.RedLight)
    val RedInk get() = c(T.RedInk)
    val Orange get() = c(T.Orange)
    val OrangeDark get() = c(T.OrangeDark)
    val OrangeInk get() = c(T.OrangeInk)
    val Gold get() = c(T.Gold)
    val GoldDark get() = c(T.GoldDark)
    val GoldLight get() = c(T.GoldLight)
    val GoldInk get() = c(T.GoldInk)
    val Purple get() = c(T.Purple)
    val PurpleDark get() = c(T.PurpleDark)
    val PurpleInk get() = c(T.PurpleInk)
    val Plum get() = c(T.Plum)
    val PlumDark get() = c(T.PlumDark)
    val Ink get() = c(T.Ink)
    val Muted get() = c(T.Muted)
    val Line get() = c(T.Line)
    val LineDark get() = c(T.LineDark)
    val Bg get() = c(T.Bg)
    val BgDeep get() = c(T.BgDeep)
    val Card get() = c(T.Card)
    val Soft get() = c(T.Soft)
    val GlassTop get() = c(T.GlassTop)
    val GlassBottom get() = c(T.GlassBottom)
    val GlassRim get() = c(T.GlassRim)
    val GlassLine get() = c(T.GlassLine)
    val GlassShadow get() = c(T.GlassShadow)
    val Scrim get() = c(T.Scrim)
    val Lattice get() = c(T.Lattice)
    val GlowA get() = c(T.GlowA)
    val GlowB get() = c(T.GlowB)
    val GlowC get() = c(T.GlowC)
    val Dust get() = c(T.Dust)

    /** Colour (face, edge) for each course section. */
    fun section(name: String): Pair<Color, Color> = when (name) {
        "Hangul" -> Purple to PurpleDark
        "Basics" -> Green to GreenDark
        "Everyday" -> Plum to PlumDark
        else -> Orange to OrangeDark
    }

    /** Obangsaek (the five traditional colours) plus Horangi orange, for celebrations and dancheong bands. */
    val Festive: List<Color> get() = listOf(Purple, Red, Gold, Green, Orange, Plum)
}

/** Nunito: rounded and friendly for Latin text. Korean falls back to the phone's own Hangul font. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** Brush-like serif Hangul (the phone's Myeongjo-style serif) for big decorative characters. */
val HangulSerif: FontFamily = FontFamily.Serif

private fun s(ink: Color, size: Int, weight: FontWeight = FontWeight.Normal, line: Int? = null) =
    TextStyle(fontFamily = Nunito, fontSize = size.sp, fontWeight = weight, color = ink, lineHeight = (line ?: (size * 1.3).toInt()).sp)

private fun type(ink: Color) = Typography(
    displaySmall = s(ink, 34, FontWeight.Black),
    headlineMedium = s(ink, 28, FontWeight.Black, 33),
    headlineSmall = s(ink, 23, FontWeight.ExtraBold),
    titleLarge = s(ink, 21, FontWeight.ExtraBold),
    titleMedium = s(ink, 17, FontWeight.ExtraBold),
    titleSmall = s(ink, 15, FontWeight.Bold),
    bodyLarge = s(ink, 17),
    bodyMedium = s(ink, 15),
    bodySmall = s(ink, 13),
    labelLarge = s(ink, 16, FontWeight.ExtraBold),
    labelMedium = s(ink, 13, FontWeight.Bold),
)

/** Whether [mode] ("dark", "light", "system") means night right now. */
@Composable
fun isNight(mode: String): Boolean = when (mode) {
    "light" -> false
    "system" -> isSystemInDarkTheme()
    else -> true
}

@Composable
fun DoranTheme(mode: String = "dark", content: @Composable () -> Unit) {
    val night = isNight(mode)
    // First frame: jump straight to the saved theme. After that: cross-fade.
    val first = remember { booleanArrayOf(true) }
    if (first[0]) { C.mix = if (night) 1f else 0f }
    LaunchedEffect(night) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        animate(C.mix, if (night) 1f else 0f, animationSpec = tween(520)) { v, _ -> C.mix = v }
    }
    val base = if (C.isDark) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = C.Green, onPrimary = Color.White, secondary = C.Blue, background = C.Bg,
            surface = C.Card, onSurface = C.Ink, onBackground = C.Ink, error = C.Red,
            surfaceContainerHigh = C.Card, surfaceContainerHighest = C.Card, surfaceContainer = C.Card,
            onSurfaceVariant = C.Muted, outline = C.LineDark, outlineVariant = C.Line,
        ),
        typography = type(C.Ink),
        content = content,
    )
}
