package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import com.doranalley.ui.components.BrushCircle
import com.doranalley.ui.components.DancheongBand
import com.doranalley.ui.components.GlowBurst
import com.doranalley.ui.components.LightRays
import com.doranalley.ui.components.drawOrb
import com.doranalley.ui.components.glass
import com.doranalley.ui.components.jewel
import com.doranalley.ui.components.lighten
import com.doranalley.ui.theme.HangulSerif
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
import com.doranalley.ui.components.ConfettiBurst
import com.doranalley.ui.components.CountUpText
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer

@Composable
private fun Page(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) { content() }
}

// ------------------------------------------------------------------ onboarding

@Composable
fun OnboardingScreen(vm: AppViewModel, step: Int) {
    val p by vm.progress.collectAsState()
    BackHandler(enabled = step > 0) { vm.back() }
    key(step) { Page {
        when (step) {
            0 -> {
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                        LightRays(Modifier.requiredSize(420.dp), color = C.Gold, rays = 12, strength = 0.55f)
                        Text("도란", fontSize = 150.sp, fontWeight = FontWeight.Black, fontFamily = HangulSerif, color = C.Ink.copy(alpha = 0.08f), modifier = Modifier.enterRise(distance = 30.dp, durationMs = 900))
                        BrushCircle(Modifier.size(250.dp), color = C.Orange.copy(alpha = 0.75f), delayMs = 300, durationMs = 1100, width = 9.dp)
                        Mascot(size = 180.dp, mood = Mood.CHEER, modifier = Modifier.enterPop(delayMs = 150, from = 0.3f, durationMs = 650))
                    }
                    Text("Doran Korean", style = androidx.compose.material3.MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp).enterRise(delayMs = 380))
                    Text("도란도란 · chatting softly together", color = C.GoldInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp).enterRise(delayMs = 430))
                    Text("Learn Korean with Horangi the tiger. Listen, tap and speak, a few minutes a day.", textAlign = TextAlign.Center, color = C.Muted, modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp).enterRise(delayMs = 500))
                }
                Spacer(Modifier.weight(1f))
                ChunkyButton("Get started", { vm.go(Screen.Onboarding(1)) }, Modifier.fillMaxWidth().enterRise(delayMs = 600), tag = "getStarted")
            }
            1 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(size = 90.dp)
                    Bubble("How much do you want to practise each day?", Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(20.dp))
                listOf(10 to "Casual · about 5 min", 20 to "Regular · about 10 min", 30 to "Serious · about 15 min", 50 to "Intense · about 25 min").forEach { (xp, label) ->
                    SelectBox(p.dailyGoal == xp, { vm.setGoal(xp) }, Modifier.fillMaxWidth().padding(vertical = 6.dp), tag = "goal:$xp") {
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
                    Text("✅ Microphone is ready.", fontSize = 18.sp, color = C.GreenInk, fontWeight = FontWeight.ExtraBold, modifier = Modifier.enterPop(from = 0.8f).testTag("micReady"))
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
    } }
}

// ------------------------------------------------------------------ results

@Composable
fun CompleteScreen(vm: AppViewModel, s: Summary) {
    BackHandler { vm.afterComplete() }
    Box(Modifier.fillMaxSize()) {
        Page {
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    LightRays(Modifier.requiredSize(460.dp), color = C.Gold, rays = 16)
                    GlowBurst(Modifier.requiredSize(320.dp), color = C.Gold, durationMs = 1200)
                    Mascot(size = 170.dp, mood = Mood.CHEER, modifier = Modifier.enterPop(from = 0.2f, durationMs = 600))
                }
                Text(
                    s.title, style = androidx.compose.material3.MaterialTheme.typography.displaySmall, color = C.GoldInk, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).enterPop(delayMs = 250, from = 0.5f).testTag("completeTitle"),
                )
                DancheongBand(Modifier.padding(top = 10.dp).width(140.dp).height(8.dp).clip(CircleShape).enterRise(delayMs = 330), alpha = 0.9f)
                if (s.perfect) Text("Perfect, no mistakes!", color = C.GreenInk, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp).enterRise(delayMs = 400))
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultBox("Total XP", s.xp, C.Gold, C.GoldDark, C.GoldInk, 450, Modifier.weight(1f)) { "$it" }
                ResultBox("Accuracy", s.accuracy, C.Green, C.GreenDark, C.GreenInk, 560, Modifier.weight(1f)) { "$it%" }
                ResultBox("Time", s.seconds, C.Blue, C.BlueDark, C.BlueInk, 670, Modifier.weight(1f)) { "%d:%02d".format(it / 60, it % 60) }
            }
            Spacer(Modifier.weight(1f))
            ChunkyButton("Continue", { vm.afterComplete() }, Modifier.fillMaxWidth().enterRise(delayMs = 800), tag = "continue")
        }
        ConfettiBurst(Modifier.fillMaxSize(), C.Festive, count = if (s.perfect) 110 else 70, originY = 0.28f)
    }
}

@Composable
private fun ResultBox(label: String, value: Int, color: Color, edge: Color, ink: Color, delayMs: Int, modifier: Modifier, format: (Int) -> String) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    Column(
        modifier.enterRise(delayMs = delayMs, distance = 24.dp).glass(shape, tint = color, tintAlpha = 0.10f, elevation = 12.dp, glow = color).clip(shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().jewel(color, edge, androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp), elevation = 0.dp).padding(vertical = 6.dp),
        )
        CountUpText(value, color = ink, delayMs = delayMs + 150, format = format, modifier = Modifier.padding(vertical = 14.dp),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
    }
}

@Composable
fun StreakScreen(vm: AppViewModel, days: Int) {
    BackHandler { vm.go(Screen.Home()) }
    val flicker = rememberInfiniteTransition(label = "flame")
    val f by flicker.animateFloat(0f, 1f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    // The number ticks from yesterday's streak to today's.
    var shown by remember { mutableStateOf((days - 1).coerceAtLeast(0)) }
    LaunchedEffect(days) { kotlinx.coroutines.delay(500); shown = days }
    Page {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                LightRays(Modifier.requiredSize(420.dp), color = C.Orange, rays = 12, strength = 0.8f)
                Embers(Modifier.requiredSize(width = 220.dp, height = 320.dp), C.Orange)
                Text("🔥", fontSize = 116.sp, modifier = Modifier.enterPop(from = 0.2f, durationMs = 600).graphicsLayer {
                    scaleY = 1f + 0.05f * f; scaleX = 1f - 0.02f * f; rotationZ = (f - 0.5f) * 4f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                })
            }
            AnimatedContent(
                shown,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                modifier = Modifier.testTag("streakDays"), label = "streak",
            ) { d -> Text("$d", fontSize = 76.sp, fontWeight = FontWeight.Black, color = C.Orange, fontFamily = com.doranalley.ui.theme.Nunito) }
            Text("day streak!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = C.OrangeInk)
            Text(
                if (days == 1) "Come back tomorrow to keep it going." else "Keep learning every day to grow your streak.",
                color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp).enterRise(delayMs = 300),
            )
            Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val lit = days.coerceAtMost(7)
                repeat(7) { i ->
                    val on = i < lit
                    val orange = C.Orange
                    val deep = C.OrangeDark
                    Box(
                        Modifier.size(32.dp).enterPop(delayMs = 350 + i * 60, from = 0.3f)
                            .then(if (on) Modifier.shadow(8.dp, CircleShape, clip = false, ambientColor = orange, spotColor = orange).clip(CircleShape).drawBehind { drawOrb(orange, deep, center, size.minDimension / 2f) }
                            else Modifier.glass(CircleShape, elevation = 2.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
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
            Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(260.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(C.Blue.copy(alpha = 0.22f), Color.Transparent))) })
                Mascot(size = 150.dp, mood = Mood.SAD)
            }
            Row(Modifier.padding(vertical = 14.dp)) {
                repeat(Progress.MAX_HEARTS) { i ->
                    Icon(Icons.Filled.Favorite, null, tint = if (i < p.heartsNow(now)) C.Red else C.LineDark, modifier = Modifier.size(30.dp).enterRise(delayMs = 150 + i * 70, distance = (-16).dp))
                }
            }
            Text("You ran out of hearts", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Practise to earn a heart, or wait: a new one arrives in ${(p.nextHeartIn(now) / 60000) + 1} min.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
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
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                LightRays(Modifier.requiredSize(400.dp), color = C.Green, rays = 12, strength = 0.7f)
                Mascot(size = 160.dp, mood = Mood.CHEER, modifier = Modifier.enterPop(from = 0.3f, durationMs = 600))
            }
            Text(if (u == null) "Wow, you know it all!" else "Your starting point", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp).enterRise(delayMs = 200))
            Text(
                if (u == null) "You placed past every unit. You can still replay any lesson." else "Unit ${u.number}: ${u.title}\n${u.section}",
                textAlign = TextAlign.Center, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = C.Ink, modifier = Modifier.padding(top = 10.dp).enterRise(delayMs = 320).testTag("placedUnit"),
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
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).glass(CircleShape, elevation = 4.dp).clip(CircleShape).clickable { vm.back() }.testTag("back"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.Ink, modifier = Modifier.size(22.dp)) }
            Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 14.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Section("Appearance")
            ThemePicker(st.theme) { vm.setTheme(it) }

            Section("Lessons")
            SettingsGroup {
                Toggle("Sound effects", st.sounds, "setSounds") { v -> vm.setSettings { it.copy(sounds = v) } }
                Toggle("Speaking exercises", st.speaking, "setSpeaking") { v -> vm.setSettings { it.copy(speaking = v) } }
                Toggle("Listening exercises", st.listening, "setListening") { v -> vm.setSettings { it.copy(listening = v) } }
                Toggle("Unlimited hearts", st.unlimitedHearts, "setHearts") { v -> vm.setSettings { it.copy(unlimitedHearts = v) } }
                Toggle("Reduce motion", st.reduceMotion, "setReduceMotion", last = true) { v -> vm.setSettings { it.copy(reduceMotion = v) } }
            }

            Section("Daily goal")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 50).forEach { xp ->
                    SelectBox(p.dailyGoal == xp, { vm.setGoal(xp) }, Modifier.weight(1f), tag = "goal:$xp") { Text("$xp XP", color = C.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }

            Section("Check your phone")
            Text("Use these if you can't hear anything or the microphone doesn't seem to work.", color = C.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            GhostButton("Test sound", { vm.audio.play("안녕하세요") }, Modifier.fillMaxWidth(), tag = "testSound")
            if (vm.audio.isMuted()) Text("Your media volume is at 0. Turn it up with the volume buttons.", color = C.RedInk, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(10.dp))
            GhostButton(if (testingMic) "Hide microphone test" else "Test microphone", { testingMic = !testingMic; vm.resetSpeak() }, Modifier.fillMaxWidth(), tag = "testMic")
            if (testingMic) MicTest(vm)

            Section("Progress")
            GhostButton("Reset all progress", { confirmReset = true }, Modifier.fillMaxWidth(), textColor = C.RedInk, tag = "reset")

            Section("About")
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
            containerColor = C.Card,
            titleContentColor = C.Ink,
            textContentColor = C.Muted,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This deletes your XP, streak and lesson progress on this phone.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetProgress() }, modifier = Modifier.testTag("confirmReset")) { Text("Reset", color = C.RedInk, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = C.Ink) } },
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
                Text("Nothing heard. The microphone may be blocked or covered.", color = C.RedInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Play recording", { vm.playTake() }, Modifier.weight(1f))
                    GhostButton("Again", { vm.startSpeaking("안녕하세요") }, Modifier.weight(1f))
                }
            }
            is SpeakState.Error -> {
                Text(s.message, color = C.RedInk)
                GhostButton("Again", { vm.resetSpeak() }, Modifier.fillMaxWidth())
            }
            SpeakState.Idle -> ChunkyButton("Record", { vm.startSpeaking("안녕하세요") }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark, tag = "micTestRecord")
        }
    }
}

@Composable
private fun Section(t: String) = com.doranalley.ui.components.SectionTitle(t, Modifier.padding(top = 26.dp, bottom = 10.dp))

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().glass(shape, elevation = 8.dp).padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
}

@Composable
private fun Toggle(label: String, value: Boolean, tag: String, last: Boolean = false, onChange: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 17.sp, color = C.Ink, modifier = Modifier.weight(1f))
            Switch(value, onChange, modifier = Modifier.testTag(tag), colors = SwitchDefaults.colors(
                checkedTrackColor = C.Green, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = C.Line, uncheckedBorderColor = C.LineDark, uncheckedThumbColor = if (C.isDark) C.Muted else Color.White,
            ))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(C.GlassLine))
    }
}

private data class ThemeOption(val mode: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/**
 * Night / Day / Phone, as a glass segmented control. The lit lens slides between options with a
 * spring; night shows a lantern-gold glow, day a soft sky one.
 */
@Composable
private fun ThemePicker(mode: String, onPick: (String) -> Unit) {
    val options = listOf(
        ThemeOption("dark", "Night", Icons.Filled.DarkMode),
        ThemeOption("light", "Day", Icons.Filled.LightMode),
        ThemeOption("system", "Phone", Icons.Filled.PhoneAndroid),
    )
    val index = options.indexOfFirst { it.mode == mode }.coerceAtLeast(0)
    val pos = remember { androidx.compose.animation.core.Animatable(index.toFloat()) }
    LaunchedEffect(index) { pos.animateTo(index.toFloat(), androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 380f)) }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val lens = if (mode == "light") C.Gold else C.Blue
    Column {
        Box(
            Modifier.fillMaxWidth().height(64.dp).glass(shape, elevation = 8.dp).drawBehind {
                val slot = (size.width - 12.dp.toPx()) / options.size
                val stretch = 1f + (kotlin.math.abs(pos.value - index) * 0.8f).coerceAtMost(0.5f)
                val w = slot * stretch
                val cx = 6.dp.toPx() + slot * (pos.value + 0.5f)
                val h = size.height - 12.dp.toPx()
                val tl = Offset(cx - w / 2f, 6.dp.toPx())
                val r = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                val sz = androidx.compose.ui.geometry.Size(w, h)
                drawRoundRect(Brush.verticalGradient(listOf(lens.copy(alpha = 0.38f), lens.copy(alpha = 0.16f)), startY = tl.y, endY = tl.y + h), tl, sz, r)
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.06f)), startY = tl.y, endY = tl.y + h), tl, sz, r, style = Stroke(1.2.dp.toPx()))
            }.padding(6.dp),
        ) {
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, o ->
                    val on = i == index
                    val fg by androidx.compose.animation.animateColorAsState(if (on) C.Ink else C.Muted, label = "fg")
                    Row(
                        Modifier.weight(1f).fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .clickable { onPick(o.mode) }.testTag("theme:${o.mode}"),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(o.icon, null, tint = if (on) (if (o.mode == "light") C.GoldInk else C.BlueInk) else C.Muted, modifier = Modifier.size(20.dp))
                        Text(o.label, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        Text(
            when (mode) { "light" -> "Hanji paper and celadon, for daytime."; "system" -> "Follows your phone's dark mode."; else -> "Ink and lantern light, easy on the eyes at night." },
            color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }
}

/** Rising embers around the streak flame. */
@Composable
private fun Embers(modifier: Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "embers")
    val time = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = androidx.compose.animation.core.LinearEasing)), label = "e")
    val bits = remember { val r = kotlin.random.Random(3); List(18) { floatArrayOf(r.nextFloat(), r.nextFloat(), 1.5f + r.nextFloat() * 2.5f, r.nextFloat()) } }
    androidx.compose.foundation.Canvas(modifier) {
        bits.forEach { b ->
            val p = (time.value + b[1]) % 1f
            val x = size.width * (0.3f + 0.4f * b[0]) + kotlin.math.sin((p * 4f + b[3] * 6f) * Math.PI.toFloat()) * 10.dp.toPx()
            val y = size.height * (0.72f - 0.7f * p)
            val a = (1f - p) * (if (p < 0.1f) p * 10f else 1f)
            drawCircle(color.copy(alpha = 0.25f * a), b[2].dp.toPx() * 2.6f, Offset(x, y))
            drawCircle(lighten(color, 0.4f).copy(alpha = 0.9f * a), b[2].dp.toPx(), Offset(x, y))
        }
    }
}

@Suppress("unused") private val unusedTab = Tab.LEARN
