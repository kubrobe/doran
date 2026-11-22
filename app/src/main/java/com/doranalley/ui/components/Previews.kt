package com.doranalley.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranTheme
import com.doranalley.ui.theme.DoranType

/** Every component preview renders in night and day; each also has a 1.5× font-scale variant. */
class ThemeModes : PreviewParameterProvider<String> {
    override val values = sequenceOf("dark", "light")
}

@Composable
private fun Sample(mode: String, content: @Composable ColumnScope.() -> Unit) {
    DoranTheme(mode) {
        Column(Modifier.background(C.Bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Preview(name = "Buttons", widthDp = 360)
@Preview(name = "Buttons 1.5x", widthDp = 360, fontScale = 1.5f)
@Composable
private fun ButtonsPreview(@PreviewParameter(ThemeModes::class) mode: String) = Sample(mode) {
    DoranButton("Continue", {}, Modifier.fillMaxWidth())
    DoranButton("Practice to earn hearts", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
    DoranButton("Check", {}, Modifier.fillMaxWidth(), enabled = false)
    DoranButton("Keep my streak", {}, Modifier.fillMaxWidth(), tone = C.streak)
    DoranButton("Reset progress", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Destructive)
    DoranButton("Can't listen now", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Tertiary, tone = C.select)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DoranIconButton(Icons.Filled.Settings, "Settings", {})
        DoranIconButton(Icons.Filled.Hearing, "Listen", {}, style = IconStyle.Tonal, tone = C.lavender)
        DoranIconButton(Icons.Filled.Star, "Star", {}, style = IconStyle.Filled)
        DoranIconButton(Icons.Filled.DarkMode, "Night", {}, style = IconStyle.Plain)
    }
}

@Preview(name = "Choice tiles", widthDp = 360)
@Preview(name = "Choice tiles 1.5x", widthDp = 360, fontScale = 1.5f)
@Composable
private fun ChoicePreview(@PreviewParameter(ThemeModes::class) mode: String) = Sample(mode) {
    ChoiceState.entries.forEach { s ->
        ChoiceTile(s, {}, Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
            Text("${s.name} · good morning", style = DoranType.body)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceTile(ChoiceState.Normal, {}, Modifier.weight(1f)) { Text("가", style = DoranType.koPrompt) }
        ChoiceTile(ChoiceState.Correct, {}, Modifier.weight(1f)) { Text("나", style = DoranType.koPrompt) }
    }
    SegmentedControl(
        listOf(Segment("Night", "a", Icons.Filled.DarkMode), Segment("Day", "b", Icons.Filled.LightMode), Segment("Phone", "c", Icons.Filled.PhoneAndroid)),
        selected = 0, onSelect = {},
    )
}

@Preview(name = "Game", widthDp = 360)
@Preview(name = "Game 1.5x", widthDp = 360, fontScale = 1.5f)
@Composable
private fun GamePreview(@PreviewParameter(ThemeModes::class) mode: String) = Sample(mode) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatPill(Icons.Filled.LocalFireDepartment, "3", C.Streak, "Streak: 3 days")
        StatPill(Icons.Filled.Star, "12/20", C.Xp, "Today: 12 of 20 XP", progress = 0.6f)
        StatPill(Icons.Filled.Favorite, "5", C.Heart, "Hearts: 5", onClick = {})
    }
    ProgressBar(0.45f)
    ProgressBar(0.8f, color = C.Xp, height = 6.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RewardChip("+10 XP", C.xp, icon = Icons.Filled.Star)
        RewardChip("New word", C.lavender)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AudioButton(false, {})
        AudioButton(true, {})
        AudioButton(false, {}, slow = true)
        AudioButton(false, {}, size = 72.dp)
    }
}

@Preview(name = "Surfaces", widthDp = 360)
@Preview(name = "Surfaces 1.5x", widthDp = 360, fontScale = 1.5f)
@Composable
private fun SurfacesPreview(@PreviewParameter(ThemeModes::class) mode: String) = Sample(mode) {
    SectionHeader("Key points")
    DoranCard(Modifier.fillMaxWidth()) { Text("Raised card", style = DoranType.title); Text("Body text on a card.", style = DoranType.body, color = C.Text2) }
    DoranCard(Modifier.fillMaxWidth(), style = CardStyle.Highlighted, tone = C.heart) { Text("Mistakes review", style = DoranType.title) }
    DoranCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        ListRow("안녕하세요", subtitle = "hello", titleStyle = DoranType.koBody, leading = { AudioButton(false, {}) }, onClick = {})
        HairlineDivider()
        ListRow("Listening", subtitle = "Tap what you hear", leading = { RowIcon(Icons.Filled.Hearing, C.select) }, onClick = {})
    }
}

@Preview(name = "Horangi", widthDp = 360)
@Composable
private fun MascotPreview(@PreviewParameter(ThemeModes::class) mode: String) = Sample(mode) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mood.entries.forEach { Mascot(mood = it, size = 72.dp, animate = false) }
    }
    Row { Mascot(size = 64.dp, animate = false); Bubble("How much do you want to practise each day?") }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        SealMark()
        DancheongBand(Modifier.weight(1f).height(10.dp), alpha = 0.9f)
    }
}
