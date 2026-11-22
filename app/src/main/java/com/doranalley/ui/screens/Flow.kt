package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doranalley.audio.KoreanAsr
import com.doranalley.core.CourseUnit
import com.doranalley.core.LessonBuilder
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.SpeakState
import com.doranalley.ui.Summary
import com.doranalley.ui.components.BrushCircle
import com.doranalley.ui.components.Bubble
import com.doranalley.ui.components.ButtonVariant
import com.doranalley.ui.components.CardStyle
import com.doranalley.ui.components.ChoiceState
import com.doranalley.ui.components.ChoiceTile
import com.doranalley.ui.components.ConfettiBurst
import com.doranalley.ui.components.CountUpText
import com.doranalley.ui.components.DancheongBand
import com.doranalley.ui.components.DoranButton
import com.doranalley.ui.components.DoranCard
import com.doranalley.ui.components.DoranDialog
import com.doranalley.ui.components.DoranIconButton
import com.doranalley.ui.components.HairlineDivider
import com.doranalley.ui.components.IconStyle
import com.doranalley.ui.components.LightRays
import com.doranalley.ui.components.ListRow
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.ProgressBar
import com.doranalley.ui.components.RewardChip
import com.doranalley.ui.components.RowIcon
import com.doranalley.ui.components.SectionHeader
import com.doranalley.ui.components.Segment
import com.doranalley.ui.components.SegmentedControl
import com.doranalley.ui.components.TopBar
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Space
import com.doranalley.ui.theme.Tone

/** A full-screen page with system-bar insets and the standard gutter. */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Space.gutter, vertical = Space.l), content = content)
}

// ------------------------------------------------------------------ onboarding

@Composable
fun OnboardingScreen(vm: AppViewModel, step: Int) {
    val p by vm.progress.collectAsState()
    BackHandler(enabled = step > 0) { vm.back() }
    key(step) { Page {
        if (step == 0) {
            // Visible from the first frame: no staggered entrances on the welcome screen.
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                    BrushCircle(Modifier.size(232.dp), color = C.Accent.copy(alpha = 0.6f), delayMs = 150, durationMs = 1000, width = 8.dp)
                    Mascot(size = 168.dp, mood = Mood.CHEER)
                }
                Text("Doran Korean", style = DoranType.display, color = C.Text, modifier = Modifier.padding(top = Space.m).semantics { heading() })
                Text("도란도란 · chatting softly together", style = DoranType.label, color = C.XpInk, modifier = Modifier.padding(top = Space.xs))
                Text(
                    "Learn Korean with Horangi the tiger. Listen, tap and speak, a few minutes a day.",
                    style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Space.m, start = Space.m, end = Space.m),
                )
            }
            Spacer(Modifier.weight(1f))
            DoranButton("Get started", { vm.go(Screen.Onboarding(1)) }, Modifier.fillMaxWidth(), tag = "getStarted")
            return@Page
        }
        OnboardingTop(step) { vm.back() }
        when (step) {
            1 -> {
                HorangiSays("How much do you want to practise each day?")
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    listOf(
                        Triple(10, "Casual", "About 5 minutes"), Triple(20, "Regular", "About 10 minutes"),
                        Triple(30, "Serious", "About 15 minutes"), Triple(50, "Intense", "About 25 minutes"),
                    ).forEach { (xp, label, sub) ->
                        ChoiceTile(
                            if (p.dailyGoal == xp) ChoiceState.Selected else ChoiceState.Normal, { vm.setGoal(xp) }, Modifier.fillMaxWidth(),
                            tag = "goal:$xp", role = Role.RadioButton, contentAlignment = Alignment.CenterStart,
                            contentPadding = PaddingValues(start = Space.l, end = 44.dp, top = Space.m, bottom = Space.m),
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = DoranType.title)
                                    Text(sub, style = DoranType.bodySmall, color = C.Text2)
                                }
                                Text("$xp XP", style = DoranType.label, color = C.XpInk)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                DoranButton("Continue", { vm.go(Screen.Onboarding(2)) }, Modifier.fillMaxWidth(), tag = "continue")
            }
            2 -> {
                var asked by remember { mutableStateOf(false) }
                val (granted, request) = rememberMicPermission()
                HorangiSays("You'll speak Korean out loud! Your recordings never leave your phone.")
                DoranCard(Modifier.fillMaxWidth(), style = if (granted) CardStyle.Highlighted else CardStyle.Raised, tone = C.accent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(if (granted) Icons.Filled.CheckCircle else Icons.Filled.Mic, if (granted) C.accent else C.lavender, size = 48.dp)
                        Column(Modifier.weight(1f).padding(start = Space.m)) {
                            Text(
                                if (granted) "Microphone is ready" else "Microphone", style = DoranType.title, color = C.Text,
                                modifier = if (granted) Modifier.enterPop(from = 0.9f).testTag("micReady") else Modifier,
                            )
                            Text(
                                if (granted) "Speaking exercises will score your pronunciation offline." else "Used only while you practise speaking.",
                                style = DoranType.bodySmall, color = C.Text2,
                            )
                        }
                    }
                    if (!granted) {
                        Spacer(Modifier.height(Space.l))
                        DoranButton("Allow microphone", { asked = true; request() }, Modifier.fillMaxWidth(), tone = C.lavender, icon = Icons.Filled.Mic, tag = "allowMic")
                        if (asked) Text("If you said no, you can still learn. Speaking exercises will ask again.", style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = Space.m))
                    }
                }
                Spacer(Modifier.weight(1f))
                DoranButton("Continue", { vm.go(Screen.Onboarding(3)) }, Modifier.fillMaxWidth(), tag = "continue")
            }
            else -> {
                HorangiSays("How much Korean do you know?")
                LevelCard(Icons.Filled.Translate, C.lavender, "I'm new to Korean", "Start with the alphabet (Hangul)", "startScratch") { vm.finishOnboarding(placement = false) }
                Spacer(Modifier.height(Space.m))
                LevelCard(Icons.Filled.School, C.accent, "I already know some Korean", "Take a short placement test to find your level", "startPlacement") { vm.finishOnboarding(placement = true) }
            }
        }
    } }
}

/** Back button and a three-step progress indicator for onboarding steps 1–3. */
@Composable
private fun OnboardingTop(step: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = Space.l), verticalAlignment = Alignment.CenterVertically) {
        DoranIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, style = IconStyle.Plain, tint = C.Text)
        Row(
            Modifier.weight(1f).padding(start = Space.s, end = Space.l).semantics(mergeDescendants = true) { contentDescription = "Step $step of 3" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { i ->
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (i < step) C.Accent else C.SurfaceRaised))
            }
        }
    }
}

@Composable
private fun HorangiSays(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.xxl)) {
        Mascot(size = 80.dp)
        Spacer(Modifier.width(Space.xs))
        Bubble(text)
    }
}

@Composable
private fun LevelCard(icon: ImageVector, tone: Tone, title: String, sub: String, tag: String, onClick: () -> Unit) {
    DoranCard(Modifier.fillMaxWidth(), style = CardStyle.Interactive, onClick = onClick, tag = tag) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon, tone, size = 48.dp)
            Column(Modifier.weight(1f).padding(start = Space.m)) {
                Text(title, style = DoranType.title, color = C.Text)
                Text(sub, style = DoranType.bodySmall, color = C.Text2)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.Muted)
        }
    }
}

// ------------------------------------------------------------------ results

/**
 * Lesson complete: the one big celebration. Horangi cheers, the XP counts up, and confetti falls
 * once (with rays and a dancheong trim when a whole unit is finished). Everything ends by itself.
 */
@Composable
fun CompleteScreen(vm: AppViewModel, s: Summary) {
    BackHandler { vm.afterComplete() }
    val unitDone = s.title.startsWith("Unit")
    Box(Modifier.fillMaxSize()) {
        Page {
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    if (unitDone) LightRays(Modifier.requiredSize(420.dp), color = C.Xp, rays = 14, strength = 0.8f)
                    Mascot(size = 156.dp, mood = Mood.CHEER, modifier = Modifier.enterPop(from = 0.5f, durationMs = 420))
                }
                Text(
                    s.title, style = DoranType.display, color = C.XpInk, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Space.s).enterRise(delayMs = 120, distance = 10.dp).testTag("completeTitle").semantics { heading() },
                )
                if (unitDone) DancheongBand(Modifier.padding(top = Space.m).width(120.dp).height(8.dp).clip(RoundedCornerShape(2.dp)), alpha = 0.9f)
                if (s.perfect) RewardChip("Perfect, no mistakes!", C.accent, Modifier.padding(top = Space.m).enterPop(delayMs = 260, from = 0.7f), icon = Icons.Filled.TaskAlt)
            }
            Spacer(Modifier.height(Space.x3))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                ResultStat(Icons.Filled.Bolt, C.xp, "XP earned", s.xp, 300, Modifier.weight(1f)) { "+$it" }
                ResultStat(Icons.Filled.TaskAlt, C.accent, "Accuracy", s.accuracy, 420, Modifier.weight(1f)) { "$it%" }
                ResultStat(Icons.Filled.Timer, C.select, "Time", s.seconds, 540, Modifier.weight(1f)) { "%d:%02d".format(it / 60, it % 60) }
            }
            Spacer(Modifier.weight(1f))
            DoranButton("Continue", { vm.afterComplete() }, Modifier.fillMaxWidth(), tag = "continue")
        }
        ConfettiBurst(Modifier.fillMaxSize(), C.Festive, count = if (unitDone) 100 else if (s.perfect) 80 else 56, originY = 0.26f)
    }
}

@Composable
private fun ResultStat(icon: ImageVector, tone: Tone, label: String, value: Int, delayMs: Int, modifier: Modifier, format: (Int) -> String) {
    DoranCard(
        modifier.enterRise(delayMs = delayMs, distance = 14.dp).semantics(mergeDescendants = true) { contentDescription = "$label: ${format(value)}" },
        contentPadding = PaddingValues(horizontal = Space.s, vertical = Space.l),
    ) {
        Column(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(tone.soft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tone.ink, modifier = Modifier.size(20.dp))
            }
            CountUpText(value, color = C.Text, delayMs = delayMs + 120, format = format, style = DoranType.h2, modifier = Modifier.padding(top = Space.s))
            Text(label, style = DoranType.caption, color = C.Text2, textAlign = TextAlign.Center)
        }
    }
}

/** The daily streak: calmer than lesson complete — the flame pops, the number ticks up, the week fills in. */
@Composable
fun StreakScreen(vm: AppViewModel, days: Int) {
    BackHandler { vm.go(Screen.Home()) }
    val reduce = LocalReduceMotion.current
    // The number ticks from yesterday's streak to today's.
    var shown by remember { mutableStateOf(if (reduce) days else (days - 1).coerceAtLeast(0)) }
    LaunchedEffect(days) { if (!reduce) kotlinx.coroutines.delay(450); shown = days }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.enterPop(from = 0.4f, durationMs = 480).size(168.dp).clip(CircleShape).background(C.StreakSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = C.Streak, modifier = Modifier.size(104.dp))
            }
            AnimatedContent(
                shown,
                transitionSpec = {
                    if (reduce) EnterTransition.None togetherWith ExitTransition.None
                    else (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                modifier = Modifier.padding(top = Space.l).testTag("streakDays").semantics { contentDescription = "$days day streak" }, label = "streak",
            ) { d -> Text("$d", style = DoranType.display.copy(fontSize = DoranType.display.fontSize * 2, lineHeight = DoranType.display.lineHeight * 2), color = C.StreakInk) }
            Text("day streak!", style = DoranType.h1, color = C.StreakInk)
            Text(
                if (days == 1) "Come back tomorrow to keep it going." else "Keep learning every day to grow your streak.",
                style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.s),
            )
            val lit = days.coerceAtMost(7)
            Row(
                Modifier.padding(top = Space.xxl).semantics(mergeDescendants = true) { contentDescription = "$lit of 7 days this week" },
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                repeat(7) { i ->
                    val on = i < lit
                    Box(
                        Modifier.enterPop(delayMs = 300 + i * 50, from = 0.4f).size(32.dp).clip(CircleShape)
                            .background(if (on) C.Streak else C.Surface)
                            .then(if (on) Modifier else Modifier.border(1.5.dp, C.OutlineStrong, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Filled.Check, null, tint = C.OnStreak, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        DoranButton("Continue", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), tone = C.streak, tag = "continue")
    }
}

@Composable
fun OutOfHeartsScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    BackHandler { vm.go(Screen.Home()) }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(size = 140.dp, mood = Mood.SAD)
            val hearts = p.heartsNow(now)
            Row(
                Modifier.padding(vertical = Space.l).semantics(mergeDescendants = true) { contentDescription = "$hearts of ${Progress.MAX_HEARTS} hearts" },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(Progress.MAX_HEARTS) { i ->
                    val full = i < hearts
                    Icon(
                        if (full) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                        tint = if (full) C.Heart else C.OutlineStrong, modifier = Modifier.size(30.dp),
                    )
                }
            }
            Text("You ran out of hearts", style = DoranType.h1, color = C.Text, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(
                "Practise to earn a heart, or take a break: a new one arrives in ${(p.nextHeartIn(now) / 60000) + 1} min.",
                style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.s),
            )
        }
        Spacer(Modifier.weight(1f))
        DoranButton("Practice to earn hearts", { vm.startPractice(LessonBuilder.PracticeMode.MIXED) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Favorite, tag = "practiceHearts")
        Spacer(Modifier.height(Space.s))
        DoranButton("No thanks", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, tag = "noThanks")
    }
}

@Composable
fun PlacedScreen(vm: AppViewModel, u: CourseUnit?) {
    BackHandler { vm.go(Screen.Home()) }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                LightRays(Modifier.requiredSize(380.dp), color = C.Accent, rays = 12, strength = 0.6f)
                Mascot(size = 150.dp, mood = Mood.CHEER, modifier = Modifier.enterPop(from = 0.5f, durationMs = 420))
            }
            Text(
                if (u == null) "Wow, you know it all!" else "Your starting point", style = DoranType.h1, color = C.Text,
                modifier = Modifier.padding(top = Space.s).semantics { heading() },
            )
            if (u == null) {
                Text(
                    "You placed past every unit. You can still replay any lesson.", style = DoranType.body, color = C.Text2,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.m).testTag("placedUnit"),
                )
            } else {
                val tone = C.section(u.section)
                DoranCard(Modifier.fillMaxWidth().padding(top = Space.l), style = CardStyle.Highlighted, tone = tone) {
                    RewardChip("Unit ${u.number} · ${u.section}", tone)
                    Text("Unit ${u.number}: ${u.title}", style = DoranType.h2, color = C.Text, modifier = Modifier.padding(top = Space.s).testTag("placedUnit"))
                }
            }
            Text("Everything before it is unlocked, so you can go back any time.", style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.l))
        }
        Spacer(Modifier.weight(1f))
        DoranButton("Let's go", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), tag = "continue")
    }
}

// ------------------------------------------------------------------ settings

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    val st = p.settings
    var confirmReset by remember { mutableStateOf(false) }
    var testingMic by remember { mutableStateOf(false) }
    BackHandler { vm.back() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar("Settings", onBack = { vm.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Space.gutter)) {
            Section("Appearance")
            val themes = listOf("dark", "light", "system")
            SegmentedControl(
                listOf(Segment("Night", "theme:dark", Icons.Filled.DarkMode), Segment("Day", "theme:light", Icons.Filled.LightMode), Segment("Phone", "theme:system", Icons.Filled.PhoneAndroid)),
                selected = themes.indexOf(st.theme).coerceAtLeast(0), onSelect = { vm.setTheme(themes[it]) },
            )
            Text(
                when (st.theme) { "light" -> "Hanji paper and jade, for daytime."; "system" -> "Follows your phone's dark mode."; else -> "Ink and lantern light, easy on the eyes." },
                style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = Space.s, start = Space.xs),
            )
            DoranCard(Modifier.fillMaxWidth().padding(top = Space.m), contentPadding = PaddingValues(0.dp)) {
                ToggleRow("Reduce motion", "Fewer animations and no celebration effects", Icons.Filled.Animation, C.lavender, st.reduceMotion, "setReduceMotion") { v -> vm.setSettings { it.copy(reduceMotion = v) } }
            }

            Section("Lessons")
            DoranCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                ToggleRow("Sound effects", "Chimes for right and wrong answers", Icons.Filled.MusicNote, C.accent, st.sounds, "setSounds") { v -> vm.setSettings { it.copy(sounds = v) } }
                HairlineDivider(Modifier.padding(start = 68.dp))
                ToggleRow("Speaking exercises", "Say sentences out loud", Icons.Filled.RecordVoiceOver, C.lavender, st.speaking, "setSpeaking") { v -> vm.setSettings { it.copy(speaking = v) } }
                HairlineDivider(Modifier.padding(start = 68.dp))
                ToggleRow("Listening exercises", "Tap what you hear", Icons.Filled.Hearing, C.select, st.listening, "setListening") { v -> vm.setSettings { it.copy(listening = v) } }
                HairlineDivider(Modifier.padding(start = 68.dp))
                ToggleRow("Unlimited hearts", "Mistakes never cost a heart", Icons.Filled.Favorite, C.heart, st.unlimitedHearts, "setHearts") { v -> vm.setSettings { it.copy(unlimitedHearts = v) } }
            }

            Section("Daily goal")
            val goals = listOf(10, 20, 30, 50)
            SegmentedControl(goals.map { Segment("$it XP", "goal:$it") }, selected = goals.indexOf(p.dailyGoal).coerceAtLeast(0), onSelect = { vm.setGoal(goals[it]) })

            Section("Check your phone")
            Text("Use these if you can't hear anything or the microphone doesn't seem to work.", style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(bottom = Space.m))
            DoranCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                ListRow(
                    "Test sound", subtitle = "Plays 안녕하세요", leading = { RowIcon(Icons.AutoMirrored.Filled.VolumeUp, C.lavender) },
                    onClick = { vm.audio.play("안녕하세요") }, tag = "testSound",
                )
                HairlineDivider(Modifier.padding(start = 68.dp))
                ListRow(
                    if (testingMic) "Hide microphone test" else "Test microphone", subtitle = "Record yourself and see what the checker hears",
                    leading = { RowIcon(Icons.Filled.Mic, C.lavender) },
                    onClick = { testingMic = !testingMic; vm.resetSpeak() }, tag = "testMic",
                )
                if (testingMic) MicTest(vm)
            }
            if (vm.audio.isMuted()) Text("Your media volume is at 0. Turn it up with the volume buttons.", style = DoranType.bodySmall, color = C.HeartInk, modifier = Modifier.padding(top = Space.s))

            Section("Progress")
            DoranCard(Modifier.fillMaxWidth(), style = CardStyle.Flat) {
                Text("Reset all progress", style = DoranType.title, color = C.Text)
                Text(
                    "Deletes your XP, streak and lesson progress on this phone. Your theme and motion settings stay.",
                    style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = 2.dp, bottom = Space.m),
                )
                DoranButton("Reset progress…", { confirmReset = true }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, tag = "reset")
            }

            Section("About")
            Text(
                "Doran Korean · version ${com.doranalley.BuildConfig.VERSION_NAME}\n" +
                    "Speech checking runs offline with sherpa-onnx and a Korean zipformer model (Apache-2.0). " +
                    "Korean audio was generated with Microsoft neural voices.",
                style = DoranType.bodySmall, color = C.Muted,
            )
            Spacer(Modifier.height(Space.x3))
        }
    }
    if (confirmReset) {
        DoranDialog({ confirmReset = false }) {
            Text("Reset progress?", style = DoranType.h2, color = C.Text, modifier = Modifier.semantics { heading() })
            Text(
                "This deletes your XP, streak and lesson progress on this phone. It can't be undone.",
                style = DoranType.body, color = C.Text2, modifier = Modifier.padding(top = Space.s, bottom = Space.xl),
            )
            DoranButton("Reset", { confirmReset = false; vm.resetProgress() }, Modifier.fillMaxWidth(), variant = ButtonVariant.Destructive, tag = "confirmReset")
            Spacer(Modifier.height(Space.s))
            DoranButton("Cancel", { confirmReset = false }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
        }
    }
}

@Composable
private fun MicTest(vm: AppViewModel) {
    val state by vm.speak.collectAsState()
    val level by vm.recorder.level.collectAsState()
    val asr by vm.asr.state.collectAsState()
    val (granted, request) = rememberMicPermission()
    DisposableEffect(Unit) { onDispose { vm.stopSpeaking(); vm.resetSpeak() } }
    Column(Modifier.fillMaxWidth().padding(start = Space.l, end = Space.l, top = Space.s, bottom = Space.l).testTag("micTest")) {
        HairlineDivider(Modifier.padding(bottom = Space.m))
        Text("Say: 안녕하세요", style = DoranType.koBody, color = C.Text)
        Text(
            "Speech checker: " + when (val a = asr) { KoreanAsr.State.Loading -> "loading…"; KoreanAsr.State.Ready -> "ready"; is KoreanAsr.State.Failed -> "failed (${a.reason})" },
            style = DoranType.caption, color = C.Muted, modifier = Modifier.padding(top = 2.dp, bottom = Space.m).testTag("asrState"),
        )
        if (!granted) {
            DoranButton("Allow microphone", request, Modifier.fillMaxWidth(), tone = C.lavender, icon = Icons.Filled.Mic)
            return@Column
        }
        when (val s = state) {
            SpeakState.Recording -> {
                ProgressBar(level, Modifier.fillMaxWidth(), color = C.Heart, height = 8.dp, description = "Microphone level")
                Spacer(Modifier.height(Space.m))
                DoranButton("Stop", { vm.stopSpeaking() }, Modifier.fillMaxWidth(), tone = C.heart, icon = Icons.Filled.Stop)
            }
            SpeakState.Checking -> Text("Checking…", style = DoranType.body, color = C.Text2)
            is SpeakState.Scored -> {
                Text("Heard: ${s.result.heard} · score ${s.result.score}%", style = DoranType.body, color = C.Text, modifier = Modifier.testTag("micResult"))
                Spacer(Modifier.height(Space.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    DoranButton("Play my voice", { vm.playTake() }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                    DoranButton("Again", { vm.startSpeaking("안녕하세요") }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                }
            }
            is SpeakState.NotHeard -> {
                Text("Nothing heard. The microphone may be blocked or covered.", style = DoranType.body, color = C.HeartInk)
                Spacer(Modifier.height(Space.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    DoranButton("Play recording", { vm.playTake() }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                    DoranButton("Again", { vm.startSpeaking("안녕하세요") }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                }
            }
            is SpeakState.Error -> {
                Text(s.message, style = DoranType.body, color = C.HeartInk)
                Spacer(Modifier.height(Space.m))
                DoranButton("Again", { vm.resetSpeak() }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
            }
            SpeakState.Idle -> DoranButton("Record", { vm.startSpeaking("안녕하세요") }, Modifier.fillMaxWidth(), tone = C.lavender, icon = Icons.Filled.Mic, tag = "micTestRecord")
        }
    }
}

@Composable
private fun Section(t: String) = SectionHeader(t, Modifier.padding(top = Space.x3, bottom = Space.m))

/** A settings row with an icon, label, explanation and a switch; the whole row toggles (one TalkBack stop). */
@Composable
private fun ToggleRow(label: String, sub: String, icon: ImageVector, tone: Tone, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value, role = Role.Switch, onValueChange = onChange)
            .testTag(tag)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, tone)
        Column(Modifier.weight(1f).padding(start = Space.m, end = Space.s)) {
            Text(label, style = DoranType.body.copy(fontWeight = FontWeight.SemiBold), color = C.Text)
            Text(sub, style = DoranType.bodySmall, color = C.Text2)
        }
        Switch(
            value, null, modifier = Modifier.clearAndSetSemantics { },
            colors = SwitchDefaults.colors(
                checkedTrackColor = C.Accent, checkedThumbColor = C.OnAccent, checkedBorderColor = C.Accent,
                uncheckedTrackColor = C.SurfaceRaised, uncheckedBorderColor = C.OutlineStrong, uncheckedThumbColor = C.OutlineStrong,
            ),
        )
    }
}
