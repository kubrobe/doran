package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.doranalley.ui.theme.C

private val praise = listOf("Excellent!", "Great job!", "Nice!", "Correct!", "Awesome!", "You got it!", "잘했어요!")

@Composable
fun LessonScreen(vm: AppViewModel) {
    val session by vm.session.collectAsState()
    val progress by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    val s = session ?: return
    var askQuit by remember { mutableStateOf(false) }
    BackHandler { askQuit = true }

    Box(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { askQuit = true }, modifier = Modifier.testTag("quit")) {
                    Icon(Icons.Filled.Close, "Quit", tint = C.Muted, modifier = Modifier.size(28.dp))
                }
                ProgressBar(s.fraction, Modifier.weight(1f).padding(horizontal = 6.dp))
                when (s.type) {
                    SessionType.LESSON -> if (!progress.settings.unlimitedHearts) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, end = 6.dp).testTag("hearts")) {
                            Icon(Icons.Filled.Favorite, null, tint = C.Red, modifier = Modifier.size(26.dp))
                            Text("${progress.heartsNow(now)}", color = C.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    SessionType.PLACEMENT -> TextButton(onClick = { vm.stopPlacementHere() }, modifier = Modifier.testTag("stopPlacement")) {
                        Text("Stop here", color = C.Blue, fontWeight = FontWeight.Bold)
                    }
                    else -> Spacer(Modifier.width(8.dp))
                }
            }
            if (s.combo >= 5 && s.feedback?.correct == true) {
                Text("🔥 ${s.combo} in a row!", color = C.Orange, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 24.dp))
            }
            val ex = s.current
            if (ex != null) {
                key(s.serial) {
                    ExerciseHost(vm, ex, locked = s.feedback != null, placement = s.type == SessionType.PLACEMENT)
                }
            }
        }
        AnimatedVisibility(
            visible = s.feedback != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it }, exit = slideOutVertically { it },
        ) {
            s.feedback?.let { FeedbackPanel(it, vm, s.current) }
        }
    }

    if (askQuit) {
        AlertDialog(
            onDismissRequest = { askQuit = false },
            title = { Text("Quit now?") },
            text = { Text(if (s.type == SessionType.PLACEMENT) "We'll place you based on what you've answered so far." else "You'll lose your progress in this lesson.") },
            confirmButton = {
                TextButton(onClick = { askQuit = false; if (s.type == SessionType.PLACEMENT) vm.stopPlacementHere() else vm.quitSession() }, modifier = Modifier.testTag("confirmQuit")) {
                    Text(if (s.type == SessionType.PLACEMENT) "Finish" else "Quit", color = C.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { askQuit = false }) { Text("Keep learning", fontWeight = FontWeight.Bold) } },
        )
    }
}

@Composable
private fun FeedbackPanel(f: Feedback, vm: AppViewModel, ex: Exercise?) {
    val good = f.correct
    val bg = if (good) C.GreenLight else C.RedLight
    val fg = if (good) C.GreenDark else C.RedDark
    val title = remember(f) { if (good) praise.random() else "Correct solution:" }
    Column(Modifier.fillMaxWidth().background(bg).padding(20.dp).testTag(if (good) "feedbackRight" else "feedbackWrong")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (good) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null, tint = fg, modifier = Modifier.size(30.dp))
            Text(title, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
            val solKo = f.solution
            if (solKo.isNotBlank() && Hangul.syllables(solKo).isNotEmpty() && vm.audio.hasAudio(solKo)) {
                IconButton(onClick = { vm.audio.play(solKo) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = fg) }
            }
        }
        if (!good && f.solution.isNotBlank()) {
            Text(f.solution, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp).testTag("solution"))
        }
        f.note?.let { Text(it, color = fg, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(14.dp))
        ChunkyButton(
            "Continue", { vm.next() }, Modifier.fillMaxWidth(),
            color = if (good) C.Green else C.Red, edge = if (good) C.GreenDark else C.RedDark, tag = "continue",
        )
    }
}

/** Common frame: title, scrollable content, and the CHECK button pinned to the bottom. */
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
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp).testTag("exerciseTitle"))
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
        text.uppercase(), color = C.Muted, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.8.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp).testTag(tag),
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
