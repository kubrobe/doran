package com.doranalley.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Semantic colour roles. Every palette (night, day) gives a value for every role.
 * Contrast was checked with WCAG formulas: text roles reach 4.5:1 on Bg, Bg2, Surface and
 * SurfaceRaised; "On*" labels reach 4.5:1 on their fill; fills and OutlineStrong reach 3:1
 * against Bg and Surface so meaningful graphics stay visible.
 */
enum class T {
    Bg, Bg2, Surface, SurfaceRaised, Outline, OutlineStrong, Highlight, Shadow, Scrim, Lattice,
    Text, Text2, Muted, Disabled, DisabledBg,
    Accent, AccentStrong, AccentSoft, AccentInk, OnAccent,
    Xp, XpSoft, XpInk, OnXp,
    Streak, StreakSoft, StreakInk, OnStreak,
    Heart, HeartSoft, HeartInk, OnHeart,
    Lavender, LavenderSoft, LavenderInk, OnLavender,
    Select, SelectSoft, SelectInk, OnSelect,
    Sky, SkySoft, SkyInk, OnSky,
    Rose, RoseSoft, RoseInk, OnRose,
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

object Palettes {
    /** Ink & lantern: OLED-friendly near-black with blue-grey surfaces and a jade accent. */
    val Night = Palette(
        true,
        T.Bg to Color(0xFF0B0D12), T.Bg2 to Color(0xFF111722), T.Surface to Color(0xFF192231), T.SurfaceRaised to Color(0xFF202C3D),
        T.Outline to Color(0xFF2A3648), T.OutlineStrong to Color(0xFF66758E), T.Highlight to Color(0x14FFFFFF),
        T.Shadow to Color(0x66000000), T.Scrim to Color(0xB3000000), T.Lattice to Color(0x0AFFFFFF),
        T.Text to Color(0xFFF4F5F8), T.Text2 to Color(0xFFA4AEC0), T.Muted to Color(0xFF8B96AA),
        T.Disabled to Color(0xFF5F6A7C), T.DisabledBg to Color(0xFF1E2734),
        T.Accent to Color(0xFF55D6AD), T.AccentStrong to Color(0xFF2FB98E), T.AccentSoft to Color(0xFF14342D),
        T.AccentInk to Color(0xFF5EDDB4), T.OnAccent to Color(0xFF052019),
        T.Xp to Color(0xFFFFC45C), T.XpSoft to Color(0xFF3A2E14), T.XpInk to Color(0xFFFFCB70), T.OnXp to Color(0xFF2A1C00),
        T.Streak to Color(0xFFFF9B61), T.StreakSoft to Color(0xFF3B2418), T.StreakInk to Color(0xFFFFA571), T.OnStreak to Color(0xFF2B1203),
        T.Heart to Color(0xFFFF6B78), T.HeartSoft to Color(0xFF3B1B22), T.HeartInk to Color(0xFFFF8C96), T.OnHeart to Color(0xFF2B0509),
        T.Lavender to Color(0xFFA99BFF), T.LavenderSoft to Color(0xFF25214A), T.LavenderInk to Color(0xFFB9AEFF), T.OnLavender to Color(0xFF150F45),
        T.Select to Color(0xFF6CB8FF), T.SelectSoft to Color(0xFF13283F), T.SelectInk to Color(0xFF8DC8FF), T.OnSelect to Color(0xFF03203B),
        T.Sky to Color(0xFF6CC6E8), T.SkySoft to Color(0xFF10303D), T.SkyInk to Color(0xFF7CD0F0), T.OnSky to Color(0xFF04222E),
        T.Rose to Color(0xFFF49AC1), T.RoseSoft to Color(0xFF3A1A2A), T.RoseInk to Color(0xFFF7AACB), T.OnRose to Color(0xFF2E0A1C),
    )

    /** Hanji paper: warm off-white surfaces, meok-ink text, deep jade. Not an inversion of night. */
    val Day = Palette(
        false,
        T.Bg to Color(0xFFF6F1E7), T.Bg2 to Color(0xFFEEE7D9), T.Surface to Color(0xFFFFFCF6), T.SurfaceRaised to Color(0xFFFFFFFF),
        T.Outline to Color(0xFFE2D8C6), T.OutlineStrong to Color(0xFF8E8472), T.Highlight to Color(0x00FFFFFF),
        T.Shadow to Color(0x2E3B2F1A), T.Scrim to Color(0x80141820), T.Lattice to Color(0x0D3B2F1A),
        T.Text to Color(0xFF1B1F29), T.Text2 to Color(0xFF454C5B), T.Muted to Color(0xFF5E6573),
        T.Disabled to Color(0xFF868C97), T.DisabledBg to Color(0xFFE8E1D3),
        T.Accent to Color(0xFF0B7F55), T.AccentStrong to Color(0xFF086444), T.AccentSoft to Color(0xFFDCF0E6),
        T.AccentInk to Color(0xFF0A7049), T.OnAccent to Color(0xFFFFFFFF),
        T.Xp to Color(0xFFB67D0A), T.XpSoft to Color(0xFFFBEBCB), T.XpInk to Color(0xFF865600), T.OnXp to Color(0xFF2A1C00),
        T.Streak to Color(0xFFDC6320), T.StreakSoft to Color(0xFFFBE2D2), T.StreakInk to Color(0xFFA9460C), T.OnStreak to Color(0xFF2B1203),
        T.Heart to Color(0xFFD2334A), T.HeartSoft to Color(0xFFFAE0E3), T.HeartInk to Color(0xFFB02538), T.OnHeart to Color(0xFFFFFFFF),
        T.Lavender to Color(0xFF6555D8), T.LavenderSoft to Color(0xFFEAE6FF), T.LavenderInk to Color(0xFF5242C4), T.OnLavender to Color(0xFFFFFFFF),
        T.Select to Color(0xFF2570C4), T.SelectSoft to Color(0xFFDFECFB), T.SelectInk to Color(0xFF1C61B0), T.OnSelect to Color(0xFFFFFFFF),
        T.Sky to Color(0xFF1F7FA6), T.SkySoft to Color(0xFFDDF0F7), T.SkyInk to Color(0xFF17698A), T.OnSky to Color(0xFFFFFFFF),
        T.Rose to Color(0xFFB83A78), T.RoseSoft to Color(0xFFF8E1EC), T.RoseInk to Color(0xFFA23069), T.OnRose to Color(0xFFFFFFFF),
    )
}

/** A colour family: a fill, the label colour on that fill, a soft background tint and a text colour for use on Bg/Surface. */
data class Tone(val fill: Color, val on: Color, val soft: Color, val ink: Color)

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
    /** 0 = day, 1 = night (animated). */
    val night: Float get() = mix

    // Surfaces and text
    val Bg get() = c(T.Bg)
    val Bg2 get() = c(T.Bg2)
    val Surface get() = c(T.Surface)
    val SurfaceRaised get() = c(T.SurfaceRaised)
    val Outline get() = c(T.Outline)
    val OutlineStrong get() = c(T.OutlineStrong)
    val Highlight get() = c(T.Highlight)
    val Shadow get() = c(T.Shadow)
    val Scrim get() = c(T.Scrim)
    val Lattice get() = c(T.Lattice)
    val Text get() = c(T.Text)
    val Text2 get() = c(T.Text2)
    val Muted get() = c(T.Muted)
    val Disabled get() = c(T.Disabled)
    val DisabledBg get() = c(T.DisabledBg)

    // Brand and game colours
    val Accent get() = c(T.Accent)
    val AccentStrong get() = c(T.AccentStrong)
    val AccentSoft get() = c(T.AccentSoft)
    val AccentInk get() = c(T.AccentInk)
    val OnAccent get() = c(T.OnAccent)
    val Xp get() = c(T.Xp)
    val XpSoft get() = c(T.XpSoft)
    val XpInk get() = c(T.XpInk)
    val OnXp get() = c(T.OnXp)
    val Streak get() = c(T.Streak)
    val StreakSoft get() = c(T.StreakSoft)
    val StreakInk get() = c(T.StreakInk)
    val OnStreak get() = c(T.OnStreak)
    val Heart get() = c(T.Heart)
    val HeartSoft get() = c(T.HeartSoft)
    val HeartInk get() = c(T.HeartInk)
    val OnHeart get() = c(T.OnHeart)
    val Lavender get() = c(T.Lavender)
    val LavenderSoft get() = c(T.LavenderSoft)
    val LavenderInk get() = c(T.LavenderInk)
    val OnLavender get() = c(T.OnLavender)
    val Select get() = c(T.Select)
    val SelectSoft get() = c(T.SelectSoft)
    val SelectInk get() = c(T.SelectInk)
    val OnSelect get() = c(T.OnSelect)

    val accent: Tone get() = Tone(Accent, OnAccent, AccentSoft, AccentInk)
    val xp: Tone get() = Tone(Xp, OnXp, XpSoft, XpInk)
    val streak: Tone get() = Tone(Streak, OnStreak, StreakSoft, StreakInk)
    val heart: Tone get() = Tone(Heart, OnHeart, HeartSoft, HeartInk)
    val lavender: Tone get() = Tone(Lavender, OnLavender, LavenderSoft, LavenderInk)
    val select: Tone get() = Tone(Select, OnSelect, SelectSoft, SelectInk)
    val sky: Tone get() = Tone(c(T.Sky), c(T.OnSky), c(T.SkySoft), c(T.SkyInk))
    val rose: Tone get() = Tone(c(T.Rose), c(T.OnRose), c(T.RoseSoft), c(T.RoseInk))

    /** One colour family per course section, used sparingly (unit stripe, path nodes, unit progress). */
    fun section(name: String): Tone = when (name) {
        "Hangul" -> lavender
        "Basics" -> accent
        "Everyday" -> sky
        else -> rose
    }

    /** Obangsaek-inspired set for celebrations and dancheong trims only. */
    val Festive: List<Color> get() = listOf(Lavender, Heart, Xp, Accent, Streak, c(T.Rose))

    // ---- Legacy names, mapped onto the semantic roles while screens migrate. Removed once unused.
    val Green get() = Accent
    val GreenDark get() = AccentStrong
    val GreenLight get() = AccentSoft
    val GreenInk get() = AccentInk
    val Blue get() = Select
    val BlueDark get() = Select
    val BlueLight get() = SelectSoft
    val BlueInk get() = SelectInk
    val Red get() = Heart
    val RedDark get() = Heart
    val RedLight get() = HeartSoft
    val RedInk get() = HeartInk
    val Orange get() = Streak
    val OrangeDark get() = Streak
    val OrangeInk get() = StreakInk
    val Gold get() = Xp
    val GoldDark get() = Xp
    val GoldLight get() = XpSoft
    val GoldInk get() = XpInk
    val Purple get() = Lavender
    val PurpleDark get() = Lavender
    val PurpleInk get() = LavenderInk
    val Plum get() = c(T.Rose)
    val PlumDark get() = c(T.Rose)
    val Ink get() = Text
    val Line get() = Outline
    val LineDark get() = OutlineStrong
    val BgDeep get() = Bg2
    val Card get() = Surface
    val Soft get() = SurfaceRaised
    val GlassTop get() = Surface
    val GlassBottom get() = Surface
    val GlassRim get() = Highlight
    val GlassLine get() = Outline
    val GlassShadow get() = Shadow
    val GlowA get() = Xp
    val GlowB get() = Accent
    val GlowC get() = Lavender
    val Dust get() = Xp
}

/** 4 dp spacing scale. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val x3 = 32.dp
    val x4 = 48.dp
    /** Default left/right screen margin. */
    val gutter = 20.dp
}

/** Corner radii: badges, tiles/inputs, cards, sheets/dialogs. Pills use a radius of half their height. */
object Radius {
    val badge = 8.dp
    val tile = 14.dp
    val card = 20.dp
    val sheet = 28.dp
}

/** Minimum touch target for anything tappable. */
val TouchTarget = 48.dp

fun lighten(c: Color, by: Float): Color = Color(
    red = c.red + (1f - c.red) * by, green = c.green + (1f - c.green) * by, blue = c.blue + (1f - c.blue) * by, alpha = c.alpha,
)

fun darken(c: Color, by: Float): Color = Color(red = c.red * (1f - by), green = c.green * (1f - by), blue = c.blue * (1f - by), alpha = c.alpha)
