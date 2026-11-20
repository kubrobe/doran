package com.doranalley.ui.screens

import androidx.compose.foundation.background
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
        PracticeCard("Mistakes review", if (mistakes > 0) "$mistakes items to fix" else "Review your weakest items", Icons.Filled.Replay, C.Red, "practiceMistakes") { start(PracticeMode.MISTAKES) }
        PracticeCard("Mixed review", "Words and sentences from your units", Icons.Filled.Shuffle, C.Green, "practiceMixed") { start(PracticeMode.MIXED) }
        PracticeCard("Listening", "Tap what you hear", Icons.Filled.Hearing, C.Blue, "practiceListening") { start(PracticeMode.LISTENING) }
        PracticeCard("Speaking", "Say sentences out loud and get a score", Icons.Filled.Mic, C.Purple, "practiceSpeaking") { start(PracticeMode.SPEAKING) }
        if (empty) {
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Mascot(mood = Mood.THINK, size = 70.dp)
                Text("Finish a lesson first (and check that speaking or listening isn't turned off in Settings).", color = C.Ink, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun PracticeCard(title: String, sub: String, icon: ImageVector, color: Color, tag: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 3.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(shape)
            .clickable(source, null, onClick = onClick).testTag(tag)
            .padding(top = sink).background(C.Line, shape).padding(bottom = (3.dp - sink).coerceAtLeast(0.dp))
            .background(C.Card, shape).border(2.dp, C.Line, shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 18.sp, color = C.Ink)
            Text(sub, color = C.Muted, fontSize = 14.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.LineDark)
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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("wordsList")) {
        item {
            Text("Words", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp))
            Text("${words.size} words · tap to listen", color = C.Muted, modifier = Modifier.padding(bottom = 10.dp))
        }
        items(words, key = { it.second.id }) { (_, w) ->
            val st = p.items[w.id]
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.weight(1f)) { GuideRow(w.ko, w.en, playing == "ko:${w.ko}") { vm.audio.play(w.ko) } }
                StrengthBars(st?.strength ?: 0.0)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StrengthBars(s: Double) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(4) { i ->
            val on = s > i * 0.25 + 0.01
            Box(Modifier.width(6.dp).height((8 + i * 5).dp).clip(RoundedCornerShape(3.dp)).background(if (on) C.Gold else C.Line))
        }
    }
}

@Composable
fun ProfileTab(vm: AppViewModel, p: Progress) {
    val today = vm.today()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.go(Screen.Settings) }, modifier = Modifier.testTag("openSettings")) { Icon(Icons.Filled.Settings, "Settings", tint = C.Muted) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Orange.copy(alpha = 0.12f)).padding(12.dp),
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
            Stat("🔥", "${p.visibleStreak(today)}", "Day streak", Modifier.weight(1f))
            Stat("⚡", "${p.totalXp}", "Total XP", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("📚", "${p.lessonsCompleted}", "Lessons", Modifier.weight(1f))
            Stat("🏆", "${p.longestStreak}", "Best streak", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("🗣️", "$learnedWords", "Words seen", Modifier.weight(1f))
            Stat("💯", "${p.perfectLessons}", "Perfect lessons", Modifier.weight(1f))
        }
        com.doranalley.ui.components.SectionTitle("This week", Modifier.padding(top = 22.dp, bottom = 10.dp))
        WeekChart(p, today)
        Text("Daily goal: ${p.dailyGoal} XP · today ${p.xpOn(today)} XP", color = C.Muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    com.doranalley.ui.components.CardBox(modifier) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Column(Modifier.padding(start = 8.dp)) {
                val n = value.toIntOrNull()
                if (n != null) CountUpText(n, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), durationMs = 700)
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
    Row(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(C.Card).border(2.dp, C.Line, RoundedCornerShape(18.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val xp = p.xpOn(d)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Text("$xp", fontSize = 11.sp, color = C.Muted)
                Box(Modifier.width(22.dp).height((90f * xp / max * grow.value).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(7.dp)).background(if (xp >= p.dailyGoal) C.Gold else if (d == today) C.Green else C.Blue))
                Text(d.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = if (d == today) C.Ink else C.Muted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
