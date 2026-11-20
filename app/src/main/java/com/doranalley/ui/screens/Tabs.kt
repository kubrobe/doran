package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.doranalley.ui.components.darken
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.components.glass
import com.doranalley.ui.components.jewel
import com.doranalley.ui.components.lighten
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.core.LessonBuilder.PracticeMode
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Nunito
import com.doranalley.ui.components.CountUpText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.LaunchedEffect

@Composable
fun PracticeTab(vm: AppViewModel) {
    var empty by remember { mutableStateOf(false) }
    val p by vm.progress.collectAsState()
    val mistakes = p.items.count { it.value.wrong > 0 && it.value.streak == 0 }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Practice", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Review what you've learned. Each finished practice earns a heart.", color = C.Muted, modifier = Modifier.padding(bottom = 16.dp))
        fun start(m: PracticeMode) { empty = !vm.startPractice(m) }
        PracticeCard("Mistakes review", if (mistakes > 0) "$mistakes items to fix" else "Review your weakest items", Icons.Filled.Replay, C.Red, "practiceMistakes", 0) { start(PracticeMode.MISTAKES) }
        PracticeCard("Mixed review", "Words and sentences from your units", Icons.Filled.Shuffle, C.Green, "practiceMixed", 1) { start(PracticeMode.MIXED) }
        PracticeCard("Listening", "Tap what you hear", Icons.Filled.Hearing, C.Blue, "practiceListening", 2) { start(PracticeMode.LISTENING) }
        PracticeCard("Speaking", "Say sentences out loud and get a score", Icons.Filled.Mic, C.Purple, "practiceSpeaking", 3) { start(PracticeMode.SPEAKING) }
        if (empty) {
            Row(Modifier.padding(top = 16.dp).enterRise(), verticalAlignment = Alignment.CenterVertically) {
                Mascot(mood = Mood.THINK, size = 70.dp)
                Text("Finish a lesson first (and check that speaking or listening isn't turned off in Settings).", color = C.Ink, modifier = Modifier.padding(start = 10.dp))
            }
        }
        Spacer(Modifier.height(TabBarSpace))
    }
}

@Composable
private fun PracticeCard(title: String, sub: String, icon: ImageVector, color: Color, tag: String, order: Int, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "squeeze")
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).enterRise(delayMs = 60 * order, distance = 18.dp)
            .graphicsLayer { scaleX = squeeze; scaleY = squeeze }
            .glass(shape, tint = color, tintAlpha = 0.06f, elevation = 10.dp).clip(shape)
            .clickable(source, null, onClick = onClick).testTag(tag)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).jewel(color, darken(color, 0.3f), RoundedCornerShape(18.dp), elevation = 8.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 18.sp, color = C.Ink)
            Text(sub, color = C.Muted, fontSize = 14.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.Muted)
    }
}

@Composable
fun WordsTab(vm: AppViewModel, p: Progress) {
    val playing by vm.audio.playing.collectAsState()
    val studied = p.studiedUnits(vm.course)
    val words = studied.flatMap { u -> u.words.map { u to it } }.distinctBy { it.second.ko }
    if (words.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Mascot(mood = Mood.THINK)
            Text("No words yet", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Text("Finish your first lesson and its words will appear here, ready to replay.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("wordsList"), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = TabBarSpace)) {
        item {
            Text("Words", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp))
            Text("${words.size} words · tap to listen", color = C.Muted, modifier = Modifier.padding(bottom = 10.dp))
        }
        items(words, key = { it.second.id }) { (_, w) ->
            val st = p.items[w.id]
            Box(Modifier.padding(vertical = 0.dp)) {
                GuideRow(w.ko, w.en, playing == "ko:${w.ko}") { vm.audio.play(w.ko) }
                StrengthBars(st?.strength ?: 0.0, Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StrengthBars(s: Double, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(4) { i ->
            val on = s > i * 0.25 + 0.01
            Box(
                Modifier.width(6.dp).height((8 + i * 5).dp).clip(RoundedCornerShape(3.dp))
                    .background(if (on) Brush.verticalGradient(listOf(lighten(C.Gold, 0.2f), C.GoldDark)) else Brush.verticalGradient(listOf(C.LineDark, C.LineDark))),
            )
        }
    }
}

@Composable
fun ProfileTab(vm: AppViewModel, p: Progress) {
    val today = vm.today()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            val night = C.isDark
            GlassIconButton(if (night) Icons.Filled.LightMode else Icons.Filled.DarkMode, if (night) "Day mode" else "Night mode", "themeToggle") {
                vm.setTheme(if (night) "light" else "dark")
            }
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Filled.Settings, "Settings", "openSettings") { vm.go(Screen.Settings) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp), tint = C.Orange, tintAlpha = 0.10f, elevation = 12.dp).padding(12.dp),
        ) {
            Mascot(size = 84.dp, mood = Mood.HAPPY)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Learning Korean", fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 20.sp, color = C.Ink)
                val cur = p.current(vm.course)
                Text(cur?.let { "Unit ${it.first.number}: ${it.first.title}" } ?: "Course complete", color = C.Muted)
            }
        }
        com.doranalley.ui.components.SectionTitle("Statistics", Modifier.padding(top = 8.dp, bottom = 10.dp))
        val learnedWords = p.studiedUnits(vm.course).sumOf { it.words.size }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("🔥", "${p.visibleStreak(today)}", "Day streak", Modifier.weight(1f), C.Orange)
            Stat("⚡", "${p.totalXp}", "Total XP", Modifier.weight(1f), C.Gold)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("📚", "${p.lessonsCompleted}", "Lessons", Modifier.weight(1f), C.Blue)
            Stat("🏆", "${p.longestStreak}", "Best streak", Modifier.weight(1f), C.Plum)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("🗣️", "$learnedWords", "Words seen", Modifier.weight(1f), C.Purple)
            Stat("💯", "${p.perfectLessons}", "Perfect lessons", Modifier.weight(1f), C.Green)
        }
        com.doranalley.ui.components.SectionTitle("This week", Modifier.padding(top = 22.dp, bottom = 10.dp))
        WeekChart(p, today)
        Text("Daily goal: ${p.dailyGoal} XP · today ${p.xpOn(today)} XP", color = C.Muted, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(TabBarSpace))
    }
}

/** A round glass button with an icon (Profile header). */
@Composable
fun GlassIconButton(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).glass(CircleShape, elevation = 6.dp).clip(CircleShape).clickable(onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.Crossfade(icon, label = "icon") { ic -> Icon(ic, label, tint = C.Ink, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String, modifier: Modifier, accent: Color) {
    com.doranalley.ui.components.CardBox(modifier) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0f)))) }, contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 22.sp)
            }
            Column(Modifier.padding(start = 8.dp)) {
                val n = value.toIntOrNull()
                if (n != null) CountUpText(n, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = C.Ink, durationMs = 700)
                else Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = C.Ink)
                Text(label, color = C.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WeekChart(p: Progress, today: java.time.LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val max = maxOf(days.maxOf { p.xpOn(it) }, p.dailyGoal, 1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(700, 100, com.doranalley.ui.components.Motion.EaseOut)) }
    Row(Modifier.fillMaxWidth().height(160.dp).glass(RoundedCornerShape(22.dp), elevation = 10.dp).padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val xp = p.xpOn(d)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Text("$xp", fontSize = 11.sp, color = C.Muted)
                val barC = if (xp >= p.dailyGoal) C.Gold else if (d == today) C.Green else C.Blue
                Box(
                    Modifier.padding(vertical = 3.dp).width(20.dp).height((90f * xp / max * grow.value).dp.coerceAtLeast(4.dp))
                        .shadow(if (xp > 0) 6.dp else 0.dp, RoundedCornerShape(8.dp), clip = false, ambientColor = barC, spotColor = barC)
                        .clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(lighten(barC, 0.25f), barC))),
                )
                Text(d.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = if (d == today) C.Ink else C.Muted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
