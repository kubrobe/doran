package com.doranalley.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * True when motion should be kept to a minimum: the in-app "Reduce motion" setting is on, or the
 * phone's animations are switched off. Decorative loops, confetti and rays then do not run, and
 * entrances become instant. Learning content and state changes are unaffected.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Whether [mode] ("dark", "light", "system") means night right now. */
@Composable
fun isNight(mode: String): Boolean = when (mode) {
    "light" -> false
    "system" -> isSystemInDarkTheme()
    else -> true
}

/** The phone's "remove animations" / animator-scale-0 setting, re-read whenever the app resumes. */
@Suppress("DEPRECATION")
@Composable
fun rememberSystemAnimationsOff(): Boolean {
    var off by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) off = !ValueAnimator.areAnimatorsEnabled() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return off
}

@Composable
fun DoranTheme(mode: String = "dark", reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    val night = isNight(mode)
    val reduce = reduceMotion || rememberSystemAnimationsOff()
    // First frame: jump straight to the saved theme. After that: cross-fade (or switch at once with reduced motion).
    val first = remember { booleanArrayOf(true) }
    if (first[0]) { C.mix = if (night) 1f else 0f }
    LaunchedEffect(night) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        val target = if (night) 1f else 0f
        if (reduce) C.mix = target
        else animate(C.mix, target, animationSpec = tween(420)) { v, _ -> C.mix = v }
    }
    val scheme = if (C.isDark) {
        darkColorScheme(
            primary = C.Accent, onPrimary = C.OnAccent, primaryContainer = C.AccentSoft, onPrimaryContainer = C.AccentInk,
            inversePrimary = C.AccentStrong,
            secondary = C.Lavender, onSecondary = C.OnLavender, secondaryContainer = C.LavenderSoft, onSecondaryContainer = C.LavenderInk,
            tertiary = C.Xp, onTertiary = C.OnXp, tertiaryContainer = C.XpSoft, onTertiaryContainer = C.XpInk,
            background = C.Bg, onBackground = C.Text, surface = C.Surface, onSurface = C.Text,
            surfaceVariant = C.SurfaceRaised, onSurfaceVariant = C.Text2, surfaceTint = Color.Transparent,
            inverseSurface = C.Text, inverseOnSurface = C.Bg,
            error = C.Heart, onError = C.OnHeart, errorContainer = C.HeartSoft, onErrorContainer = C.HeartInk,
            outline = C.OutlineStrong, outlineVariant = C.Outline, scrim = C.Scrim,
            surfaceBright = C.SurfaceRaised, surfaceDim = C.Bg, surfaceContainerLowest = C.Bg, surfaceContainerLow = C.Bg2,
            surfaceContainer = C.Surface, surfaceContainerHigh = C.Surface, surfaceContainerHighest = C.SurfaceRaised,
        )
    } else {
        lightColorScheme(
            primary = C.Accent, onPrimary = C.OnAccent, primaryContainer = C.AccentSoft, onPrimaryContainer = C.AccentInk,
            inversePrimary = C.AccentStrong,
            secondary = C.Lavender, onSecondary = C.OnLavender, secondaryContainer = C.LavenderSoft, onSecondaryContainer = C.LavenderInk,
            tertiary = C.Xp, onTertiary = C.OnXp, tertiaryContainer = C.XpSoft, onTertiaryContainer = C.XpInk,
            background = C.Bg, onBackground = C.Text, surface = C.Surface, onSurface = C.Text,
            surfaceVariant = C.SurfaceRaised, onSurfaceVariant = C.Text2, surfaceTint = Color.Transparent,
            inverseSurface = C.Text, inverseOnSurface = C.Bg,
            error = C.Heart, onError = C.OnHeart, errorContainer = C.HeartSoft, onErrorContainer = C.HeartInk,
            outline = C.OutlineStrong, outlineVariant = C.Outline, scrim = C.Scrim,
            surfaceBright = C.SurfaceRaised, surfaceDim = C.Bg2, surfaceContainerLowest = C.SurfaceRaised, surfaceContainerLow = C.Surface,
            surfaceContainer = C.Surface, surfaceContainerHigh = C.Surface, surfaceContainerHighest = C.SurfaceRaised,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = doranTypography(C.Text)) {
        CompositionLocalProvider(LocalContentColor provides C.Text, LocalReduceMotion provides reduce, content = content)
    }
}
