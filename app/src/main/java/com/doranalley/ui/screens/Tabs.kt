package com.doranalley.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doranalley.core.CourseUnit
import com.doranalley.core.ItemStat
import com.doranalley.core.LessonBuilder.PracticeMode
import com.doranalley.core.Progress
import com.doranalley.core.Word
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.components.CardStyle
import com.doranalley.ui.components.CountUpText
import com.doranalley.ui.components.DoranCard
import com.doranalley.ui.components.DoranIconButton
import com.doranalley.ui.components.EmptyState
import com.doranalley.ui.components.HairlineDivider
import com.doranalley.ui.components.ListRow
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.ProgressBar
import com.doranalley.ui.components.RewardChip
import com.doranalley.ui.components.RowIcon
import com.doranalley.ui.components.SectionHeader
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Space
import com.doranalley.ui.theme.TouchTarget
import com.doranalley.ui.theme.Tone

/** The large title that opens each tab's content (it scrolls with the content). */
@Composable
private fun TabTitle(title: String, subtitle: String?) {
    Column(Modifier.padding(top = Space.s, bottom = Space.l)) {
        Text(title, style = DoranType.h1, color = C.Text, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = DoranType.body, color = C.Text2, modifier = Modifier.padding(top = 2.dp))
    }
}

// ------------------------------------------------------------------ practice

private data class PracticeOption(val mode: PracticeMode, val title: String, val sub: String, val icon: ImageVector, val tone: Tone, val tag: String)

@Composable
fun PracticeTab(vm: AppViewModel) {
    var empty by remember { mutableStateOf<PracticeMode?>(null) }
    val p by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    val mistakes = p.items.count { it.value.wrong > 0 && it.value.streak == 0 }
    val filter = p.filter(now)
    fun start(m: PracticeMode) { empty = if (vm.startPractice(m)) null else m }
    val options = listOf(
        PracticeOption(
            PracticeMode.MISTAKES, "Fix your mistakes",
            if (mistakes > 0) "$mistakes ${if (mistakes == 1) "item" else "items"} to fix" else "Review your weakest items",
            Icons.Filled.Replay, C.heart, "practiceMistakes",
        ),
        PracticeOption(PracticeMode.MIXED, "Mixed review", "Words and sentences from your units", Icons.Filled.Shuffle, C.accent, "practiceMixed"),
        PracticeOption(
            PracticeMode.LISTENING, "Listening",
            if (!p.settings.listening) "Turned off in Settings" else if (!filter.listening) "Paused for a few minutes" else "Tap what you hear",
            Icons.Filled.Hearing, C.select, "practiceListening",
        ),
        PracticeOption(
            PracticeMode.SPEAKING, "Speaking",
            if (!p.settings.speaking) "Turned off in Settings" else if (!filter.speaking) "Paused for a few minutes" else "Say sentences out loud and get a score",
            Icons.Filled.Mic, C.lavender, "practiceSpeaking",
        ),
    )
    // Mistakes are promoted when there are any; otherwise mixed review is the suggestion.
    val recommended = if (mistakes > 0) options[0] else options[1]
    val rest = options.filter { it != recommended }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
        TabTitle("Practice", "Review what you've learned. Every finished session earns a heart.")
        SectionHeader("Recommended", Modifier.padding(bottom = Space.s), ornament = recommended.tone.fill)
        DoranCard(Modifier.fillMaxWidth(), style = CardStyle.Highlighted, tone = recommended.tone, onClick = { start(recommended.mode) }, tag = recommended.tag) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(recommended.icon, recommended.tone, size = 52.dp)
                Column(Modifier.weight(1f).padding(start = Space.m)) {
                    Text(recommended.title, style = DoranType.h2, color = C.Text)
                    Text(recommended.sub, style = DoranType.bodySmall, color = C.Text2)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = recommended.tone.ink)
            }
            Row(Modifier.padding(top = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                RewardChip("+10 XP", C.xp, icon = Icons.Filled.Bolt)
                RewardChip("+1 heart", C.heart, icon = Icons.Filled.Favorite)
            }
        }
        SectionHeader("More ways to practise", Modifier.padding(top = Space.xxl, bottom = Space.s))
        DoranCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            rest.forEachIndexed { i, o ->
                ListRow(
                    o.title, subtitle = o.sub,
                    leading = { RowIcon(o.icon, o.tone) },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.Muted) },
                    onClick = { start(o.mode) }, tag = o.tag,
                )
                if (i < rest.lastIndex) HairlineDivider(Modifier.padding(start = 68.dp))
            }
        }
        empty?.let { m ->
            DoranCard(Modifier.fillMaxWidth().padding(top = Space.l).enterRise(distance = 8.dp, durationMs = Motion.STATE + 60), style = CardStyle.Flat) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(mood = Mood.THINK, size = 64.dp)
                    val why = when {
                        m == PracticeMode.LISTENING && !p.settings.listening -> "Listening exercises are turned off in Settings."
                        m == PracticeMode.SPEAKING && !p.settings.speaking -> "Speaking exercises are turned off in Settings."
                        m == PracticeMode.LISTENING && !filter.listening -> "Listening is paused for a few minutes because you chose “Can't listen now”."
                        m == PracticeMode.SPEAKING && !filter.speaking -> "Speaking is paused for a few minutes because you chose “Can't speak now”."
                        else -> "Finish a lesson first, then come back to practise it."
                    }
                    Text(why, style = DoranType.body, color = C.Text, modifier = Modifier.padding(start = Space.m))
                }
            }
        }
        Spacer(Modifier.height(TabBarSpace))
    }
}

// ------------------------------------------------------------------ words

private enum class WordFilter(val label: String) { ALL("All"), NEW("New"), LEARNING("Learning"), STRONG("Strong") }

private enum class Strength { NEW, LEARNING, STRONG }

private fun strengthOf(st: ItemStat?): Strength = when {
    st == null || st.seen == 0 -> Strength.NEW
    st.strength >= 0.75 -> Strength.STRONG
    else -> Strength.LEARNING
}

@Composable
fun WordsTab(vm: AppViewModel, p: Progress) {
    val playing by vm.audio.playing.collectAsState()
    val studied = p.studiedUnits(vm.course)
    val words = studied.flatMap { u -> u.words.map { u to it } }.distinctBy { it.second.ko }
    if (words.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.gutter)) {
            TabTitle("Words", null)
            EmptyState(Mood.THINK, "No words yet", "Finish your first lesson and its words will appear here, ready to replay.")
        }
        return
    }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(WordFilter.ALL) }
    val counts = remember(words, p.items) { words.groupingBy { strengthOf(p.items[it.second.id]) }.eachCount() }
    val q = query.trim().lowercase()
    val shown = words.filter { (_, w) ->
        (q.isEmpty() || w.ko.contains(q) || w.en.lowercase().contains(q)) &&
            when (filter) {
                WordFilter.ALL -> true
                WordFilter.NEW -> strengthOf(p.items[w.id]) == Strength.NEW
                WordFilter.LEARNING -> strengthOf(p.items[w.id]) == Strength.LEARNING
                WordFilter.STRONG -> strengthOf(p.items[w.id]) == Strength.STRONG
            }
    }
    val groups: List<Pair<CourseUnit, List<Word>>> = shown.groupBy({ it.first }, { it.second }).toList()
    LazyColumn(
        Modifier.fillMaxSize().testTag("wordsList"),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = TabBarSpace),
    ) {
        item(key = "title") { TabTitle("Words", "${words.size} words from ${studied.size} ${if (studied.size == 1) "unit" else "units"} · tap to listen") }
        item(key = "search") { SearchField(query, { query = it }) }
        item(key = "filters") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = Space.m, bottom = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                WordFilter.entries.forEach { f ->
                    val n = when (f) {
                        WordFilter.ALL -> words.size
                        WordFilter.NEW -> counts[Strength.NEW] ?: 0
                        WordFilter.LEARNING -> counts[Strength.LEARNING] ?: 0
                        WordFilter.STRONG -> counts[Strength.STRONG] ?: 0
                    }
                    FilterPill("${f.label} $n", f == filter) { filter = f }
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "none") {
                Text(
                    if (q.isNotEmpty()) "No words match “$query”." else "No words in this group yet.",
                    style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Space.x3),
                )
            }
        }
        groups.forEach { (u, list) ->
            item(key = "unit:${u.id}") {
                SectionHeader("Unit ${u.number} · ${u.title}", Modifier.padding(top = Space.xl, bottom = Space.s), ornament = C.section(u.section).fill)
            }
            items(list, key = { it.id }) { w ->
                val first = w == list.first()
                val last = w == list.last()
                val shape = RoundedCornerShape(
                    topStart = if (first) Radius.card else 0.dp, topEnd = if (first) Radius.card else 0.dp,
                    bottomStart = if (last) Radius.card else 0.dp, bottomEnd = if (last) Radius.card else 0.dp,
                )
                Column(Modifier.fillMaxWidth().clip(shape).background(C.Surface)) {
                    if (!first) HairlineDivider(Modifier.padding(start = 76.dp))
                    GuideRow(w.ko, w.en, playing == "ko:${w.ko}", trailing = { StrengthBadge(strengthOf(p.items[w.id]), p.items[w.id]?.strength ?: 0.0) }) { vm.audio.play(w.ko) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(Radius.tile)
    Row(
        Modifier.fillMaxWidth().heightIn(min = TouchTarget + 4.dp).clip(shape).background(C.Bg2).border(1.dp, C.Outline, shape).padding(start = Space.m, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = C.Muted, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = Space.s, vertical = Space.m)) {
            if (value.isEmpty()) Text("Search Korean or English", style = DoranType.body, color = C.Muted, modifier = Modifier.clearAndSetSemantics { })
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = DoranType.body.copy(color = C.Text),
                cursorBrush = SolidColor(C.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search words" },
            )
        }
        if (value.isNotEmpty()) DoranIconButton(Icons.Filled.Close, "Clear search", { onChange("") }, style = com.doranalley.ui.components.IconStyle.Plain, size = 40.dp)
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        Modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (selected) C.AccentSoft else C.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) C.Accent else C.Outline, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = DoranType.label, color = if (selected) C.AccentInk else C.Text2)
    }
}

/** How well a word is known: four bars plus a word label, so it never depends on colour alone. */
@Composable
private fun StrengthBadge(s: Strength, value: Double) {
    val (label, tone) = when (s) {
        Strength.NEW -> "New" to null
        Strength.LEARNING -> "Learning" to C.xp
        Strength.STRONG -> "Strong" to C.accent
    }
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Strength: $label" }) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.clearAndSetSemantics { }) {
            repeat(4) { i ->
                val on = value > i * 0.25 + 0.01
                Box(Modifier.width(4.dp).height((5 + i * 3).dp).clip(RoundedCornerShape(2.dp)).background(if (on && tone != null) tone.fill else C.Outline))
            }
        }
        Text(label, style = DoranType.caption, color = tone?.ink ?: C.Muted, modifier = Modifier.padding(top = 3.dp).clearAndSetSemantics { })
    }
}

// ------------------------------------------------------------------ profile

private data class Achievement(val title: String, val detail: String, val icon: ImageVector, val tone: Tone, val have: Int, val need: Int) {
    val unlocked get() = have >= need
}

@Composable
fun ProfileTab(vm: AppViewModel, p: Progress) {
    val today = vm.today()
    val course = vm.course
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TabTitle("Profile", null) }
            val night = C.isDark
            DoranIconButton(
                if (night) Icons.Filled.LightMode else Icons.Filled.DarkMode, if (night) "Switch to day mode" else "Switch to night mode",
                { vm.setTheme(if (night) "light" else "dark") }, tag = "themeToggle",
            )
            Spacer(Modifier.width(Space.xs))
            DoranIconButton(Icons.Filled.Settings, "Settings", { vm.go(Screen.Settings) }, tag = "openSettings")
        }
        // Where the learner is on the path.
        val totalNodes = course.units.sumOf { it.lessonCount }
        val doneNodes = course.units.sumOf { p.lessonsDone(it) }
        val cur = p.current(course)
        DoranCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(size = 72.dp, mood = if (cur == null) Mood.CHEER else Mood.HAPPY)
                Column(Modifier.weight(1f).padding(start = Space.m)) {
                    Text("Learning Korean", style = DoranType.title, color = C.Text)
                    Text(cur?.let { "Unit ${it.first.number}: ${it.first.title}" } ?: "Course complete", style = DoranType.bodySmall, color = C.Text2)
                    ProgressBar(doneNodes.toFloat() / totalNodes.coerceAtLeast(1), Modifier.padding(top = Space.s), height = 6.dp, description = "Course progress")
                    Text("$doneNodes of $totalNodes lessons on the path", style = DoranType.caption, color = C.Muted, modifier = Modifier.padding(top = Space.xs))
                }
            }
        }
        Row(Modifier.padding(top = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            val extended = p.extendedToday(today)
            HeroStat(Icons.Filled.LocalFireDepartment, C.streak, p.visibleStreak(today), "Day streak", if (extended) "Done today" else "Not yet today", Modifier.weight(1f))
            HeroStat(Icons.Filled.Bolt, C.xp, p.totalXp, "Total XP", "Today ${p.xpOn(today)} of ${p.dailyGoal}", Modifier.weight(1f))
        }

        SectionHeader("This week", Modifier.padding(top = Space.xxl, bottom = Space.s))
        DoranCard(Modifier.fillMaxWidth()) {
            WeekChart(p, today)
            val days = (0..6).map { today.minusDays(it.toLong()) }
            val active = days.count { p.xpOn(it) > 0 }
            val met = days.count { p.xpOn(it) >= p.dailyGoal }
            HairlineDivider(Modifier.padding(vertical = Space.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active $active of 7 days", style = DoranType.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = C.Text, modifier = Modifier.weight(1f))
                Text("Goal met on $met ${if (met == 1) "day" else "days"}", style = DoranType.bodySmall, color = C.Text2)
            }
        }

        SectionHeader("Statistics", Modifier.padding(top = Space.xxl, bottom = Space.s))
        val wordsUnlocked = p.studiedUnits(course).sumOf { it.words.size }
        DoranCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            StatRow(Icons.Filled.School, C.accent, "Lessons completed", "${p.lessonsCompleted}")
            HairlineDivider(Modifier.padding(start = 68.dp))
            StatRow(Icons.Filled.TaskAlt, C.accent, "Perfect lessons", "${p.perfectLessons}", "No mistakes")
            HairlineDivider(Modifier.padding(start = 68.dp))
            StatRow(Icons.Filled.LocalFireDepartment, C.streak, "Best streak", "${p.longestStreak} ${if (p.longestStreak == 1) "day" else "days"}")
            HairlineDivider(Modifier.padding(start = 68.dp))
            // Counts every word in the units studied so far (not only words already practised).
            StatRow(Icons.AutoMirrored.Filled.MenuBook, C.lavender, "Words unlocked", "$wordsUnlocked", "In the units you've studied")
        }

        val hangulUnits = course.units.filter { it.isHangul }
        val achievements = listOf(
            Achievement("First steps", "Finish a lesson", Icons.Filled.Star, C.accent, p.lessonsCompleted, 1),
            Achievement("Flawless", "A lesson with no mistakes", Icons.Filled.TaskAlt, C.accent, p.perfectLessons, 1),
            Achievement("Hangul hero", "Finish the Hangul units", Icons.Filled.Translate, C.lavender, hangulUnits.count { p.unitDone(it) }, hangulUnits.size.coerceAtLeast(1)),
            Achievement("On fire", "A 7-day streak", Icons.Filled.LocalFireDepartment, C.streak, p.longestStreak, 7),
            Achievement("Word hoard", "Unlock 100 words", Icons.AutoMirrored.Filled.MenuBook, C.lavender, wordsUnlocked, 100),
            Achievement("XP collector", "Earn 500 XP", Icons.Filled.Bolt, C.xp, p.totalXp, 500),
            Achievement("Dedicated", "Finish 25 lessons", Icons.Filled.MilitaryTech, C.accent, p.lessonsCompleted, 25),
            Achievement("Perfectionist", "10 perfect lessons", Icons.Filled.WorkspacePremium, C.xp, p.perfectLessons, 10),
            Achievement("Unstoppable", "A 30-day streak", Icons.Filled.EmojiEvents, C.streak, p.longestStreak, 30),
        )
        SectionHeader("Achievements", Modifier.padding(top = Space.xxl, bottom = Space.s)) {
            Text("${achievements.count { it.unlocked }}/${achievements.size}", style = DoranType.label, color = C.Text2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            achievements.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    row.forEach { a -> AchievementTile(a, Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(TabBarSpace))
    }
}

@Composable
private fun HeroStat(icon: ImageVector, tone: Tone, value: Int, label: String, sub: String, modifier: Modifier) {
    DoranCard(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value. $sub" }) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tone.soft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tone.ink, modifier = Modifier.size(20.dp))
        }
        CountUpText(value, style = DoranType.number, color = C.Text, durationMs = 700, modifier = Modifier.padding(top = Space.s).clearAndSetSemantics { })
        Text(label, style = DoranType.label, color = C.Text, modifier = Modifier.clearAndSetSemantics { })
        Text(sub, style = DoranType.caption, color = C.Muted, modifier = Modifier.padding(top = 2.dp).clearAndSetSemantics { })
    }
}

@Composable
private fun StatRow(icon: ImageVector, tone: Tone, label: String, value: String, sub: String? = null) {
    ListRow(
        label, subtitle = sub, titleStyle = DoranType.body.copy(fontWeight = FontWeight.SemiBold),
        leading = { RowIcon(icon, tone) },
        trailing = { Text(value, style = DoranType.title, color = C.Text) },
    )
}

@Composable
private fun AchievementTile(a: Achievement, modifier: Modifier) {
    val shape = RoundedCornerShape(Radius.tile + 2.dp)
    val state = if (a.unlocked) "unlocked" else "locked, ${a.have.coerceAtMost(a.need)} of ${a.need}"
    Column(
        modifier.clip(shape)
            .background(if (a.unlocked) a.tone.soft else C.Surface)
            .border(1.dp, if (a.unlocked) a.tone.fill.copy(alpha = 0.6f) else C.Outline, shape)
            .padding(horizontal = Space.s, vertical = Space.m)
            .semantics(mergeDescendants = true) { contentDescription = "${a.title}: ${a.detail}, $state" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (a.unlocked) a.tone.fill else C.SurfaceRaised).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (a.unlocked) a.icon else Icons.Filled.Lock, null, tint = if (a.unlocked) a.tone.on else C.Muted, modifier = Modifier.size(22.dp))
        }
        Text(a.title, style = DoranType.label, color = C.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.s).clearAndSetSemantics { })
        Text(
            if (a.unlocked) a.detail else "${a.have.coerceAtMost(a.need)}/${a.need}",
            style = DoranType.caption, color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * Seven days of XP as bars, with the daily goal as a dashed line. Days that reached the goal are
 * gold; today's label is bold and reads "Today". TalkBack reads the whole week as one sentence.
 */
@Composable
private fun WeekChart(p: Progress, today: java.time.LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val goal = p.dailyGoal.coerceAtLeast(1)
    val max = maxOf(days.maxOf { p.xpOn(it) }, goal, 1).toFloat()
    val reduce = LocalReduceMotion.current
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) grow.animateTo(1f, tween(600, 80, Motion.EaseOut)) }
    val names = days.map { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) }
    val spoken = "This week: " + days.mapIndexed { i, d -> "${if (d == today) "today" else names[i]} ${p.xpOn(d)} XP" }.joinToString(", ") + ". Daily goal $goal XP."
    val xpC = C.Xp
    val barC = C.Accent
    val todayC = C.Accent
    val track = C.SurfaceRaised
    val goalC = C.OutlineStrong
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            days.forEach { d ->
                Text("${p.xpOn(d)}", style = DoranType.caption, color = if (p.xpOn(d) > 0) C.Text2 else C.Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = Space.xs).clearAndSetSemantics { }) {
            val slot = size.width / 7f
            val bw = (slot * 0.52f).coerceAtMost(28.dp.toPx())
            val r = CornerRadius(6.dp.toPx())
            days.forEachIndexed { i, d ->
                val xp = p.xpOn(d)
                val x = slot * i + (slot - bw) / 2f
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), r)
                val h = (size.height * xp / max * grow.value).coerceAtLeast(if (xp > 0) 6.dp.toPx() else 0f)
                if (h > 0f) {
                    val c = if (xp >= goal) xpC else if (d == today) todayC else barC.copy(alpha = 0.6f)
                    drawRoundRect(c, Offset(x, size.height - h), Size(bw, h), r)
                }
            }
            val gy = size.height - size.height * goal / max
            drawLine(goalC, Offset(0f, gy), Offset(size.width, gy), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            days.forEachIndexed { i, d ->
                val isToday = d == today
                Text(
                    if (isToday) "Today" else names[i], style = if (isToday) DoranType.caption.copy(fontWeight = FontWeight.ExtraBold) else DoranType.caption,
                    color = if (isToday) C.Text else C.Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.padding(top = Space.s).clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(16.dp).height(2.dp).background(goalC))
            Text("Daily goal $goal XP", style = DoranType.caption, color = C.Muted, modifier = Modifier.padding(start = 6.dp, end = Space.l))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(xpC))
            Text("Goal met", style = DoranType.caption, color = C.Muted, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
