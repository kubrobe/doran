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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, C.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = C.Ink)
            Text(sub, color = C.Muted, fontSize = 14.sp)
        }
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
            Text("Words you learn will appear here.", color = C.Muted, textAlign = TextAlign.Center)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            Mascot(size = 90.dp, mood = Mood.HAPPY)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Learning Korean", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = C.Ink)
                val cur = p.current(vm.course)
                Text(cur?.let { "Unit ${it.first.number}: ${it.first.title}" } ?: "Course complete", color = C.Muted)
            }
        }
        Text("STATISTICS", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
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
        Text("THIS WEEK", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
        WeekChart(p, today)
        Text("Daily goal: ${p.dailyGoal} XP · today ${p.xpOn(today)} XP", color = C.Muted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).border(2.dp, C.Line, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 24.sp)
        Column(Modifier.padding(start = 8.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = C.Ink)
            Text(label, color = C.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WeekChart(p: Progress, today: java.time.LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val max = maxOf(days.maxOf { p.xpOn(it) }, p.dailyGoal, 1)
    Row(Modifier.fillMaxWidth().height(150.dp).border(2.dp, C.Line, RoundedCornerShape(14.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val xp = p.xpOn(d)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Text("$xp", fontSize = 11.sp, color = C.Muted)
                Box(Modifier.width(22.dp).height((90f * xp / max).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(6.dp)).background(if (xp >= p.dailyGoal) C.Gold else C.Blue))
                Text(d.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = if (d == today) C.Ink else C.Muted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
