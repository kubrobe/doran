package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.audio.KoreanAsr
import com.doranalley.core.CourseUnit
import com.doranalley.core.LessonBuilder
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.SpeakState
import com.doranalley.ui.Summary
import com.doranalley.ui.Tab
import com.doranalley.ui.components.Bubble
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.GhostButton
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.SelectBox
import com.doranalley.ui.theme.C

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding().padding(20.dp)) { content() }
}

// ------------------------------------------------------------------ onboarding

@Composable
fun OnboardingScreen(vm: AppViewModel, step: Int) {
    val p by vm.progress.collectAsState()
    BackHandler(enabled = step > 0) { vm.back() }
    Page {
        when (step) {
            0 -> {
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Mascot(size = 180.dp, mood = Mood.CHEER)
                    Text("Doran Korean", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                    Text("Learn Korean with Horangi the tiger. Listen, tap, and speak — a few minutes a day.", textAlign = TextAlign.Center, color = C.Muted, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.weight(1f))
                ChunkyButton("Get started", { vm.go(Screen.Onboarding(1)) }, Modifier.fillMaxWidth(), tag = "getStarted")
            }
            1 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(size = 90.dp)
                    Bubble("How much do you want to practise each day?", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(20.dp))
                listOf(10 to "Casual · about 5 min", 20 to "Regular · about 10 min", 30 to "Serious · about 15 min", 50 to "Intense · about 25 min").forEach { (xp, label) ->
                    SelectBox(p.dailyGoal == xp, { vm.setGoal(xp) }, Modifier.fillMaxWidth().padding(vertical = 5.dp), tag = "goal:$xp") {
                        Row(Modifier.fillMaxWidth()) {
                            Text(label, fontSize = 17.sp, color = C.Ink, modifier = Modifier.weight(1f))
                            Text("$xp XP", fontSize = 15.sp, color = C.Muted)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                ChunkyButton("Continue", { vm.go(Screen.Onboarding(2)) }, Modifier.fillMaxWidth(), tag = "continue")
            }
            2 -> {
                var mic by remember { mutableStateOf(false) }
                val (granted, request) = rememberMicPermission()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(size = 90.dp)
                    Bubble("You'll speak Korean out loud! Your recordings never leave your phone.", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(30.dp))
                if (granted) {
                    Text("✅ Microphone is ready.", fontSize = 18.sp, color = C.GreenDark, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("micReady"))
                } else {
                    ChunkyButton("Allow microphone", { mic = true; request() }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark, tag = "allowMic")
                    if (mic) Text("If you said no, you can still learn. Speaking exercises will ask again.", color = C.Muted, modifier = Modifier.padding(top = 10.dp))
                }
                Spacer(Modifier.weight(1f))
                ChunkyButton("Continue", { vm.go(Screen.Onboarding(3)) }, Modifier.fillMaxWidth(), tag = "continue")
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(size = 90.dp)
                    Bubble("How much Korean do you know?", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(24.dp))
                SelectBox(false, { vm.finishOnboarding(placement = false) }, Modifier.fillMaxWidth().padding(vertical = 6.dp), tag = "startScratch") {
                    Column(Modifier.fillMaxWidth()) {
                        Text("I'm new to Korean", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = C.Ink)
                        Text("Start with the alphabet (Hangul)", color = C.Muted)
                    }
                }
                SelectBox(false, { vm.finishOnboarding(placement = true) }, Modifier.fillMaxWidth().padding(vertical = 6.dp), tag = "startPlacement") {
                    Column(Modifier.fillMaxWidth()) {
                        Text("I already know some Korean", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = C.Ink)
                        Text("Take a short placement test to find your level", color = C.Muted)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ results

@Composable
fun CompleteScreen(vm: AppViewModel, s: Summary) {
    BackHandler { vm.afterComplete() }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(size = 170.dp, mood = Mood.CHEER)
            Text(s.title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = C.Gold, modifier = Modifier.padding(top = 10.dp).testTag("completeTitle"))
            if (s.perfect) Text("Perfect — no mistakes!", color = C.GreenDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultBox("TOTAL XP", "${s.xp}", C.Gold, Modifier.weight(1f))
            ResultBox("ACCURACY", "${s.accuracy}%", C.Green, Modifier.weight(1f))
            ResultBox("TIME", "%d:%02d".format(s.seconds / 60, s.seconds % 60), C.Blue, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Continue", { vm.afterComplete() }, Modifier.fillMaxWidth(), tag = "continue")
    }
}

@Composable
private fun ResultBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.background(color, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(3.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), textAlign = TextAlign.Center)
        Column(Modifier.fillMaxWidth().background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(13.dp)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
    }
}

@Composable
fun StreakScreen(vm: AppViewModel, days: Int) {
    BackHandler { vm.go(Screen.Home()) }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥", fontSize = 110.sp)
            Text("$days", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = C.Orange, modifier = Modifier.testTag("streakDays"))
            Text("day streak!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = C.Orange)
            Text(if (days == 1) "Come back tomorrow to keep it going." else "Keep learning every day to grow your streak.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Continue", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), color = C.Orange, edge = C.OrangeDark, tag = "continue")
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
            Mascot(size = 150.dp, mood = Mood.SAD)
            Row(Modifier.padding(vertical = 14.dp)) { repeat(Progress.MAX_HEARTS) { i -> Icon(Icons.Filled.Favorite, null, tint = if (i < p.heartsNow(now)) C.Red else C.Line) } }
            Text("You ran out of hearts", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Practise to earn a heart, or wait — a new one arrives in ${(p.nextHeartIn(now) / 60000) + 1} min.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Practice to earn hearts", { vm.startPractice(LessonBuilder.PracticeMode.MIXED) }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark, tag = "practiceHearts")
        Spacer(Modifier.height(10.dp))
        GhostButton("No thanks", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), tag = "noThanks")
    }
}

@Composable
fun PlacedScreen(vm: AppViewModel, u: CourseUnit?) {
    BackHandler { vm.go(Screen.Home()) }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(size = 160.dp, mood = Mood.CHEER)
            Text(if (u == null) "Wow, you know it all!" else "Your starting point", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            Text(
                if (u == null) "You placed past every unit. You can still replay any lesson." else "Unit ${u.number}: ${u.title}\n${u.section}",
                textAlign = TextAlign.Center, fontSize = 18.sp, color = C.Ink, modifier = Modifier.padding(top = 10.dp).testTag("placedUnit"),
            )
            Text("Everything before it is unlocked, so you can go back any time.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Let's go", { vm.go(Screen.Home()) }, Modifier.fillMaxWidth(), tag = "continue")
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
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.back() }, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.Muted) }
            Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Section("LESSONS")
            Toggle("Sound effects", st.sounds, "setSounds") { v -> vm.setSettings { it.copy(sounds = v) } }
            Toggle("Speaking exercises", st.speaking, "setSpeaking") { v -> vm.setSettings { it.copy(speaking = v) } }
            Toggle("Listening exercises", st.listening, "setListening") { v -> vm.setSettings { it.copy(listening = v) } }
            Toggle("Unlimited hearts", st.unlimitedHearts, "setHearts") { v -> vm.setSettings { it.copy(unlimitedHearts = v) } }

            Section("DAILY GOAL")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 50).forEach { xp ->
                    SelectBox(p.dailyGoal == xp, { vm.setGoal(xp) }, Modifier.weight(1f), tag = "goal:$xp") { Text("$xp", color = C.Ink, fontWeight = FontWeight.Bold) }
                }
            }

            Section("CHECK YOUR PHONE")
            Text("Use these if you can't hear anything or the microphone doesn't seem to work.", color = C.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            GhostButton("Test sound", { vm.audio.play("안녕하세요") }, Modifier.fillMaxWidth(), tag = "testSound")
            if (vm.audio.isMuted()) Text("Your media volume is at 0. Turn it up with the volume buttons.", color = C.RedDark, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            GhostButton(if (testingMic) "Hide microphone test" else "Test microphone", { testingMic = !testingMic; vm.resetSpeak() }, Modifier.fillMaxWidth(), tag = "testMic")
            if (testingMic) MicTest(vm)

            Section("PROGRESS")
            GhostButton("Reset all progress", { confirmReset = true }, Modifier.fillMaxWidth(), textColor = C.Red, tag = "reset")

            Section("ABOUT")
            Text(
                "Doran Korean · version ${com.doranalley.BuildConfig.VERSION_NAME}\n" +
                    "Speech checking runs offline with sherpa-onnx and a Korean zipformer model (Apache-2.0). " +
                    "Korean audio was generated with Microsoft neural voices.",
                color = C.Muted, fontSize = 13.sp,
            )
            Spacer(Modifier.height(30.dp))
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This deletes your XP, streak and lesson progress on this phone.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetProgress() }, modifier = Modifier.testTag("confirmReset")) { Text("Reset", color = C.Red, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MicTest(vm: AppViewModel) {
    val state by vm.speak.collectAsState()
    val level by vm.recorder.level.collectAsState()
    val asr by vm.asr.state.collectAsState()
    val (granted, request) = rememberMicPermission()
    DisposableEffect(Unit) { onDispose { vm.stopSpeaking(); vm.resetSpeak() } }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("micTest")) {
        Text("Say: 안녕하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink)
        Text(
            "Speech checker: " + when (val a = asr) { KoreanAsr.State.Loading -> "loading…"; KoreanAsr.State.Ready -> "ready"; is KoreanAsr.State.Failed -> "failed (${a.reason})" },
            color = C.Muted, fontSize = 13.sp, modifier = Modifier.testTag("asrState"),
        )
        Spacer(Modifier.height(8.dp))
        if (!granted) {
            ChunkyButton("Allow microphone", request, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark)
            return@Column
        }
        when (val s = state) {
            SpeakState.Recording -> {
                com.doranalley.ui.components.ProgressBar(level, Modifier.fillMaxWidth(), color = C.Red)
                Spacer(Modifier.height(8.dp))
                ChunkyButton("Stop", { vm.stopSpeaking() }, Modifier.fillMaxWidth(), color = C.Red, edge = C.RedDark)
            }
            SpeakState.Checking -> Text("Checking…", color = C.Muted)
            is SpeakState.Scored -> {
                Text("Heard: ${s.result.heard} · score ${s.result.score}%", color = C.Ink, modifier = Modifier.testTag("micResult"))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Play my voice", { vm.playTake() }, Modifier.weight(1f))
                    GhostButton("Again", { vm.startSpeaking("안녕하세요") }, Modifier.weight(1f))
                }
            }
            is SpeakState.NotHeard -> {
                Text("Nothing heard. The microphone may be blocked or covered.", color = C.RedDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Play recording", { vm.playTake() }, Modifier.weight(1f))
                    GhostButton("Again", { vm.startSpeaking("안녕하세요") }, Modifier.weight(1f))
                }
            }
            is SpeakState.Error -> {
                Text(s.message, color = C.RedDark)
                GhostButton("Again", { vm.resetSpeak() }, Modifier.fillMaxWidth())
            }
            SpeakState.Idle -> ChunkyButton("Record", { vm.startSpeaking("안녕하세요") }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark, tag = "micTestRecord")
        }
    }
}

@Composable
private fun Section(t: String) = Text(t, color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))

@Composable
private fun Toggle(label: String, value: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, color = C.Ink, modifier = Modifier.weight(1f))
        Switch(value, onChange, modifier = Modifier.testTag(tag), colors = SwitchDefaults.colors(checkedTrackColor = C.Green))
    }
}

@Suppress("unused") private val unusedTab = Tab.LEARN
