package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.doranalley.ui.components.GlowBurst
import com.doranalley.ui.components.darken
import com.doranalley.ui.components.drawOrb
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.components.glass
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.core.Exercise
import com.doranalley.core.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Feedback
import com.doranalley.ui.SessionType
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.ProgressBar
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterSlide
import com.doranalley.ui.components.pop
import com.doranalley.ui.components.shake
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Nunito
import kotlinx.coroutines.launch

private val praise = listOf("Excellent!", "Great job!", "Nice!", "Correct!", "Awesome!", "You got it!", "잘했어요!")

@Composable
fun LessonScreen(vm: AppViewModel) {
    val session by vm.session.collectAsState()
    val progress by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    val s = session ?: return
    var askQuit by remember { mutableStateOf(false) }
    BackHandler { askQuit = true }

    // Keep the last feedback so the panel can slide away with its content.
    val lastFeedback = remember { mutableStateOf<Feedback?>(null) }
    SideEffect { if (s.feedback != null) { lastFeedback.value = s.feedback } }
    val haptics = LocalHapticFeedback.current
    val shakeX = remember { Animatable(0f) }
    // A soft wash of colour from the bottom edge: green for right, red for wrong.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(s.feedback) {
        val f = s.feedback ?: return@LaunchedEffect
        if (f.correct) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        else haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { flash.snapTo(1f); flash.animateTo(0f, tween(900, easing = com.doranalley.ui.components.Motion.EaseOut)) }
        if (!f.correct) shakeX.shake(10f)
    }
    val flashColor = if (s.feedback?.correct == false) C.Red else C.Green

    Box(
        Modifier.fillMaxSize().drawBehind {
            val a = flash.value
            if (a > 0f) drawRect(Brush.verticalGradient(listOf(Color.Transparent, flashColor.copy(alpha = 0.28f * a)), startY = size.height * 0.35f))
        }.statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).glass(CircleShape, elevation = 4.dp).clip(CircleShape).clickable { askQuit = true }.testTag("quit"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, "Quit", tint = C.Muted, modifier = Modifier.size(22.dp)) }
                ProgressBar(s.fraction, Modifier.weight(1f).padding(horizontal = 10.dp), color = if (s.combo >= 5) C.Orange else C.Green)
                when (s.type) {
                    SessionType.LESSON -> if (!progress.settings.unlimitedHearts) HeartCounter(progress.heartsNow(now))
                    SessionType.PLACEMENT -> TextButton(onClick = { vm.stopPlacementHere() }, modifier = Modifier.testTag("stopPlacement")) {
                        Text("Stop here", color = C.BlueInk, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito)
                    }
                    else -> Spacer(Modifier.width(8.dp))
                }
            }
            AnimatedVisibility(
                visible = s.combo >= 5 && s.feedback?.correct == true,
                enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f), initialScale = 0.5f) + fadeIn(),
                exit = fadeOut(tween(150)),
            ) {
                Text(
                    "🔥 ${s.combo} in a row!", color = C.OrangeInk, fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 15.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp).glass(CircleShape, tint = C.Orange, tintAlpha = 0.22f, elevation = 8.dp, glow = C.Orange)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            val ex = s.current
            if (ex != null) {
                Box(Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationX = shakeX.value.dp.toPx() }) {
                    key(s.serial) {
                        Box(Modifier.fillMaxSize().enterSlide()) {
                            ExerciseHost(vm, ex, locked = s.feedback != null, placement = s.type == SessionType.PLACEMENT)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = s.feedback != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.78f, stiffness = 450f)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
        ) {
            val f = s.feedback ?: lastFeedback.value
            // While sliding away the panel is shown but can't be tapped again.
            if (f != null) FeedbackPanel(f, vm, interactive = s.feedback != null)
        }
    }

    if (askQuit) {
        AlertDialog(
            onDismissRequest = { askQuit = false },
            containerColor = C.Card,
            titleContentColor = C.Ink,
            textContentColor = C.Muted,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Quit now?", fontFamily = Nunito, fontWeight = FontWeight.Black) },
            text = { Text(if (s.type == SessionType.PLACEMENT) "We'll place you based on what you've answered so far." else "You'll lose your progress in this lesson.") },
            confirmButton = {
                TextButton(onClick = { askQuit = false; if (s.type == SessionType.PLACEMENT) vm.stopPlacementHere() else vm.quitSession() }, modifier = Modifier.testTag("confirmQuit")) {
                    Text(if (s.type == SessionType.PLACEMENT) "Finish" else "Quit", color = C.RedInk, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = { TextButton(onClick = { askQuit = false }) { Text("Keep learning", fontWeight = FontWeight.ExtraBold, color = C.GreenInk) } },
        )
    }
}

/** Heart count that jolts and rolls down when a heart is lost. */
@Composable
private fun HeartCounter(n: Int) {
    val shakeX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val prev = remember { mutableIntStateOf(n) }
    LaunchedEffect(n) {
        if (n < prev.intValue) {
            launch { shakeX.shake(6f) }
            scale.pop(1.4f)
        }
        prev.intValue = n
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.glass(CircleShape, elevation = 4.dp).padding(horizontal = 10.dp, vertical = 4.dp).testTag("hearts")) {
        Icon(
            Icons.Filled.Favorite, null, tint = C.Red,
            modifier = Modifier.size(22.dp).graphicsLayer {
                translationX = shakeX.value.dp.toPx(); scaleX = scale.value; scaleY = scale.value
            },
        )
        AnimatedContent(n, transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) }, label = "hearts") { v ->
            Text("$v", color = C.Red, fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun FeedbackPanel(f: Feedback, vm: AppViewModel, interactive: Boolean) {
    val good = f.correct
    val accent = if (good) C.Green else C.Red
    val fg = if (good) C.GreenInk else C.RedInk
    val title = remember(f) { if (good) praise.random() else "Correct solution:" }
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Column(
        Modifier.fillMaxWidth()
            .glass(shape, tint = accent, tintAlpha = 0.16f, elevation = 24.dp, glow = accent, opaque = 0.9f)
            .drawBehind {
                // a bright accent thread along the top edge
                drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = 0f), accent, accent.copy(alpha = 0f))), size = androidx.compose.ui.geometry.Size(size.width, 2.5.dp.toPx()))
            }
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
            .testTag(if (good) "feedbackRight" else "feedbackWrong"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (good) GlowBurst(Modifier.requiredSize(120.dp), color = accent, key = f)
                Box(
                    Modifier.size(40.dp).enterPop(delayMs = 60, from = 0.2f, durationMs = 380)
                        .shadow(10.dp, CircleShape, clip = false, ambientColor = accent, spotColor = accent)
                        .clip(CircleShape).drawBehind { drawOrb(accent, darken(accent, 0.3f), center, size.minDimension / 2f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (good) Icons.Filled.Check else Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Text(title, color = fg, fontWeight = FontWeight.Black, fontFamily = Nunito, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp).weight(1f).enterRise(delayMs = 80, distance = 6.dp))
            val solKo = f.solution
            if (solKo.isNotBlank() && Hangul.syllables(solKo).isNotEmpty() && vm.audio.hasAudio(solKo)) {
                IconButton(onClick = { if (interactive) vm.audio.play(solKo) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = fg) }
            }
        }
        if (!good && f.solution.isNotBlank()) {
            Text(f.solution, color = C.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp).testTag("solution"))
        }
        f.note?.let { Text(it, color = if (good) fg else C.Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        ChunkyButton(
            "Continue", { if (interactive) vm.next() }, Modifier.fillMaxWidth(),
            color = accent, edge = if (good) C.GreenDark else C.RedDark, tag = if (interactive) "continue" else null,
        )
    }
}

/** Common frame: title, scrollable content, and the Check button pinned to the bottom. */
@Composable
fun ExerciseFrame(
    title: String,
    locked: Boolean,
    ready: Boolean,
    onCheck: () -> Unit,
    checkLabel: String = "Check",
    secondary: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp).enterRise(delayMs = 40, distance = 8.dp).testTag("exerciseTitle"))
            content()
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (secondary != null && !locked) { secondary(); Spacer(Modifier.height(10.dp)) }
            ChunkyButton(checkLabel, onCheck, Modifier.fillMaxWidth(), enabled = ready && !locked, tag = "check")
        }
    }
}

@Composable
fun SkipLink(text: String, tag: String, onClick: () -> Unit) {
    Text(
        text, color = C.Muted, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito, fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp).testTag(tag),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun ExerciseHost(vm: AppViewModel, ex: Exercise, locked: Boolean, placement: Boolean) {
    val idk: (@Composable () -> Unit)? = if (placement) { { SkipLink("I don't know", "dontKnow") { vm.submit(false) } } } else null
    when (ex) {
        is Exercise.LetterIntro -> LetterIntroView(vm, ex)
        is Exercise.LetterToSound -> LetterToSoundView(vm, ex, locked, idk)
        is Exercise.SoundToLetter -> SoundToLetterView(vm, ex, locked)
        is Exercise.WordMeaning -> WordMeaningView(vm, ex, locked, idk)
        is Exercise.PickKorean -> PickKoreanView(vm, ex, locked)
        is Exercise.ListenPick -> ListenPickView(vm, ex, locked)
        is Exercise.MatchPairs -> MatchPairsView(vm, ex, locked)
        is Exercise.BuildEnglish -> BuildEnglishView(vm, ex, locked, idk)
        is Exercise.BuildKorean -> BuildKoreanView(vm, ex, locked, idk)
        is Exercise.ListenBuild -> ListenBuildView(vm, ex, locked)
        is Exercise.FillBlank -> FillBlankView(vm, ex, locked, idk)
        is Exercise.Speak -> SpeakView(vm, ex, locked)
    }
}
