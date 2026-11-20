package com.doranalley.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.doranalley.core.CourseUnit
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.Tab
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.StatChip
import com.doranalley.ui.theme.C
import kotlin.math.sin

@Composable
fun HomeScreen(vm: AppViewModel, tab: Tab) {
    val p by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        TopStats(vm, p, now)
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.LEARN -> PathView(vm, p)
                Tab.PRACTICE -> PracticeTab(vm)
                Tab.WORDS -> WordsTab(vm, p)
                Tab.PROFILE -> ProfileTab(vm, p)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(C.Line))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem(Icons.Filled.Home, "Learn", tab == Tab.LEARN, "tabLearn") { vm.go(Screen.Home(Tab.LEARN)) }
            NavItem(Icons.Filled.FitnessCenter, "Practice", tab == Tab.PRACTICE, "tabPractice") { vm.go(Screen.Home(Tab.PRACTICE)) }
            NavItem(Icons.AutoMirrored.Filled.MenuBook, "Words", tab == Tab.WORDS, "tabWords") { vm.go(Screen.Home(Tab.WORDS)) }
            NavItem(Icons.Filled.Person, "Profile", tab == Tab.PROFILE, "tabProfile") { vm.go(Screen.Home(Tab.PROFILE)) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp))
            .then(if (selected) Modifier.border(2.dp, C.Blue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).background(C.BlueLight) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (selected) C.Blue else C.Muted, modifier = Modifier.size(28.dp))
        Text(label, fontSize = 12.sp, color = if (selected) C.Blue else C.Muted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopStats(vm: AppViewModel, p: Progress, now: Long) {
    val today = vm.today()
    var heartsInfo by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🇰🇷", fontSize = 26.sp)
        Spacer(Modifier.weight(1f))
        val streak = p.visibleStreak(today)
        StatChip(Icons.Filled.LocalFireDepartment, "$streak", if (p.extendedToday(today)) C.Orange else C.Muted, tag = "streak")
        Spacer(Modifier.width(10.dp))
        StatChip(Icons.Filled.Star, "${p.xpOn(today)}/${p.dailyGoal}", C.Gold, tag = "dailyXp")
        Spacer(Modifier.width(10.dp))
        StatChip(Icons.Filled.Favorite, if (p.settings.unlimitedHearts) "∞" else "${p.heartsNow(now)}", C.Red, onClick = { heartsInfo = true }, tag = "heartsChip")
    }
    Box(Modifier.fillMaxWidth().height(2.dp).background(C.Line))
    if (heartsInfo) HeartsDialog(vm, p, now) { heartsInfo = false }
}

@Composable
fun HeartsDialog(vm: AppViewModel, p: Progress, now: Long, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hearts", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Row(Modifier.padding(vertical = 12.dp)) {
                repeat(Progress.MAX_HEARTS) { i ->
                    Icon(Icons.Filled.Favorite, null, tint = if (i < p.heartsNow(now)) C.Red else C.Line, modifier = Modifier.size(34.dp))
                }
            }
            val next = p.nextHeartIn(now)
            Text(
                when {
                    p.settings.unlimitedHearts -> "Unlimited hearts are on (Settings)."
                    next == 0L -> "You have full hearts. You lose one for each mistake in a lesson."
                    else -> "Next heart in ${(next / 60000) + 1} min. Finish a practice session to earn one now."
                },
                textAlign = TextAlign.Center, color = C.Ink,
            )
            Spacer(Modifier.height(16.dp))
            ChunkyButton("Practice to earn hearts", { onDismiss(); vm.startPractice(com.doranalley.core.LessonBuilder.PracticeMode.MIXED) }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    }
}

// ------------------------------------------------------------------ path

private sealed interface PathRow {
    data class Header(val unit: CourseUnit, val sectionStart: Boolean) : PathRow
    data class Node(val unit: CourseUnit, val lesson: Int, val idx: Int) : PathRow
}

@Composable
private fun PathView(vm: AppViewModel, p: Progress) {
    val course = vm.course
    val rows = remember(course) {
        buildList {
            var lastSection = ""
            var idx = 0
            course.units.forEach { u ->
                add(PathRow.Header(u, u.section != lastSection)); lastSection = u.section
                repeat(u.lessonCount) { add(PathRow.Node(u, it, idx++)) }
            }
        }
    }
    val cur = p.current(course)
    val currentRow = rows.indexOfFirst { it is PathRow.Node && cur != null && it.unit.id == cur.first.id && it.lesson == cur.second }
    val state = rememberLazyListState()
    LaunchedEffect(currentRow) { if (currentRow > 2) state.scrollToItem((currentRow - 2).coerceAtLeast(0)) }
    var open by remember { mutableStateOf<PathRow.Node?>(null) }

    LazyColumn(state = state, modifier = Modifier.fillMaxSize().testTag("path")) {
        items(rows.size) { i ->
            when (val r = rows[i]) {
                is PathRow.Header -> UnitHeader(r.unit, r.sectionStart) { vm.go(Screen.Guidebook(r.unit)) }
                is PathRow.Node -> {
                    val done = p.lessonsDone(r.unit) > r.lesson
                    val isCurrent = cur != null && r.unit.id == cur.first.id && r.lesson == cur.second
                    val unlocked = p.isUnlocked(course, r.unit, r.lesson)
                    PathNode(r, done, isCurrent, unlocked) { open = r }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(mood = if (cur == null) Mood.CHEER else Mood.THINK, size = 110.dp)
                Text(if (cur == null) "You finished the whole course! 정말 잘했어요!" else "More units are on the way.", color = C.Muted, textAlign = TextAlign.Center)
            }
        }
    }

    open?.let { node ->
        val done = p.lessonsDone(node.unit) > node.lesson
        val unlocked = p.isUnlocked(course, node.unit, node.lesson)
        LessonPopup(node.unit, node.lesson, done, unlocked, onStart = { open = null; vm.startLesson(node.unit, node.lesson) }, onDismiss = { open = null })
    }
}

@Composable
private fun UnitHeader(u: CourseUnit, sectionStart: Boolean, onGuide: () -> Unit) {
    val (col, edge) = C.section(u.section)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = if (sectionStart) 20.dp else 28.dp, bottom = 12.dp)) {
        if (sectionStart) {
            Text("SECTION · ${u.section.uppercase()}", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(edge).padding(bottom = 4.dp).clip(RoundedCornerShape(16.dp)).background(col)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("UNIT ${u.number}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(u.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                    Text(u.blurb, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onGuide).padding(10.dp).testTag("guide:${u.id}"),
                ) { Icon(Icons.AutoMirrored.Filled.MenuBook, "Guidebook", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun PathNode(r: PathRow.Node, done: Boolean, current: Boolean, unlocked: Boolean, onClick: () -> Unit) {
    val u = r.unit
    val (col, edge) = C.section(u.section)
    val isReview = r.lesson == u.lessonCount - 1
    val x = (sin(r.idx * 0.9) * 70).dp
    val pulse = rememberInfiniteTransition(label = "node")
    val s by pulse.animateFloat(1f, 1.06f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s")
    val fill = when { done -> C.Gold; unlocked -> col; else -> C.Line }
    val rim = when { done -> C.GoldDark; unlocked -> edge; else -> C.LineDark }
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.offset(x = x), horizontalAlignment = Alignment.CenterHorizontally) {
            if (current) {
                Text("START", color = col, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.border(2.dp, C.Line, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.height(6.dp))
            }
            Box(
                Modifier.size(76.dp).scale(if (current) s else 1f).clip(CircleShape).background(rim).padding(bottom = 6.dp)
                    .clip(CircleShape).background(fill).clickable(onClick = onClick).testTag("node:${u.id}:${r.lesson}"),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when { done -> Icons.Filled.Check; !unlocked -> Icons.Filled.Lock; isReview -> Icons.Filled.EmojiEvents; u.isHangul -> Icons.Filled.Translate; else -> Icons.Filled.Star }
                Icon(icon, null, tint = if (unlocked || done) Color.White else C.Muted, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun LessonPopup(u: CourseUnit, lesson: Int, done: Boolean, unlocked: Boolean, onStart: () -> Unit, onDismiss: () -> Unit) {
    val (col, edge) = C.section(u.section)
    val isReview = lesson == u.lessonCount - 1
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(20.dp)).background(if (unlocked) col else Color.White).padding(20.dp).testTag("lessonPopup")) {
            val fg = if (unlocked) Color.White else C.Ink
            Text(u.title, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(if (isReview) "Unit review" else "Lesson ${lesson + 1} of ${u.lessonCount - 1}", color = fg.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            if (unlocked) {
                ChunkyButton(
                    if (done) "Practice +10 XP" else if (isReview) "Start +15 XP" else "Start +10 XP",
                    onStart, Modifier.fillMaxWidth(), color = Color.White, edge = C.Line, textColor = edge, tag = "startLesson",
                )
            } else {
                Text("Finish everything above to unlock this.", color = C.Muted)
            }
        }
    }
}

// ------------------------------------------------------------------ guidebook

@Composable
fun GuidebookScreen(vm: AppViewModel, u: CourseUnit) {
    val playing by vm.audio.playing.collectAsState()
    androidx.activity.compose.BackHandler { vm.back() }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = { vm.back() }, modifier = Modifier.testTag("back")) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.Muted)
            }
            Text("Unit ${u.number} guidebook", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Text(u.title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text(u.blurb, color = C.Muted, modifier = Modifier.padding(bottom = 16.dp))
                Text("KEY POINTS", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)
            }
            items(u.tips) { t ->
                Text("• $t", fontSize = 16.sp, color = C.Ink, modifier = Modifier.padding(vertical = 6.dp))
            }
            if (u.letters.isNotEmpty()) {
                item { Text("LETTERS", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
                items(u.letters) { l -> GuideRow(l.ch, "${l.rom} · ${l.hint}", playing == "ko:${l.say}") { vm.audio.play(l.say) } }
            }
            item { Text("WORDS", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
            items(u.words) { w -> GuideRow(w.ko, w.en, playing == "ko:${w.ko}") { vm.audio.play(w.ko) } }
            if (u.sentences.isNotEmpty()) {
                item { Text("PHRASES", color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
                items(u.sentences) { s -> GuideRow(s.ko, s.en, playing == "ko:${s.ko}") { vm.audio.play(s.ko) } }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun GuideRow(ko: String, en: String, playing: Boolean, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onPlay).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        com.doranalley.ui.components.SpeakerButton(playing, onPlay, size = 40.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(ko, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            Text(en, fontSize = 14.sp, color = C.Muted)
        }
    }
}
