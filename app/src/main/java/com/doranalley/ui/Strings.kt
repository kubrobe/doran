package com.doranalley.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.doranalley.core.L
import com.doranalley.core.pick

/** Interface text from assets/i18n/ui.json. Switching language only recomposes, so no place is lost. */
class Strings(private val table: Map<String, L>, val lang: String) {
    operator fun get(key: String): String = table[key]?.pick(lang) ?: key
    fun f(key: String, vararg args: Any): String {
        var s = get(key)
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
        return s
    }
}

val LocalStrings = staticCompositionLocalOf { Strings(emptyMap(), "en") }

@Composable
@ReadOnlyComposable
fun L.tr(): String = pick(LocalStrings.current.lang)

@Composable
@ReadOnlyComposable
fun s(key: String): String = LocalStrings.current[key]
