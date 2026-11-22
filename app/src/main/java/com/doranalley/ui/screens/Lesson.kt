package com.doranalley.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doranalley.core.Exercise
import com.doranalley.core.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Feedback
import com.doranalley.ui.SessionType
import com.doranalley.ui.components.AudioButton
import com.doranalley.ui.components.ButtonVariant
import com.doranalley.ui.components.DoranButton
import com.doranalley.ui.components.DoranDialog
import com.doranalley.ui.components.DoranIconButton
import com.doranalley.ui.components.IconStyle
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.ProgressBar
import com.doranalley.ui.components.RewardChip
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.components.enterSlide
import com.doranalley.ui.components.pop
import com.doranalley.ui.components.shake
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Space
import com.doranalley.ui.theme.Tone
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
    val reduce = LocalReduceMotion.current

    // Keep the last feedback so the panel can slide away with its content.
    val lastFeedback = remember { mutableStateOf<Feedback?>(null) }
    SideEffect { if (s.feedback != null) { lastFeedback.value = s.feedback } }
    val haptics = LocalHapticFeedback.current
    // A soft wash of colour from the bottom edge: jade for right, red for wrong.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(s.feedback) {
        val f = s.feedback ?: return@LaunchedEffect
        if (f.correct) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        else haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reduce) launch { flash.snapTo(1f); flash.animateTo(0f, tween(800, easing = Motion.EaseOut)) }
    }
    val flashColor = if (s.feedback?.correct == false) C.Heart else C.Accent

    Box(
        Modifier.fillMaxSize().drawBehind {
            val a = flash.value
            if (a > 0f) drawRect(Brush.verticalGradient(listOf(Color.Transparent, flashColor.copy(alpha = 0.18f * a)), startY = size.height * 0.4f))
        }.statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Space.s), verticalAlignment = Alignment.CenterVertically) {
                DoranIconButton(Icons.Filled.Close, "Quit lesson", { askQuit = true }, style = IconStyle.Plain, tint = C.Text2, tag = "quit")
                ProgressBar(
                    s.fraction, Modifier.weight(1f).padding(horizontal = Space.s),
                    color = if (s.combo >= 5) C.Streak else C.Accent, height = 14.dp, description = "Lesson progress",
                )
                when (s.type) {
                    SessionType.LESSON -> if (!progress.settings.unlimitedHearts) HeartCounter(progress.heartsNow(now)) else Spacer(Modifier.width(Space.s))
                    SessionType.PLACEMENT -> DoranButton("Stop here", { vm.stopPlacementHere() }, variant = ButtonVariant.Tertiary, tone = C.select, tag = "stopPlacement")
                    else -> Spacer(Modifier.width(Space.s))
                }
            }
            val ex = s.current
            if (ex != null) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    key(s.serial) {
                        Box(Modifier.fillMaxSize().enterSlide()) {
                            ExerciseHost(vm, ex, locked = s.feedback != null, placement = s.type == SessionType.PLACEMENT, result = s.feedback?.correct)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = s.feedback != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reduce) fadeIn(tween(0)) else slideInVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) { it } + fadeIn(tween(120)),
            exit = if (reduce) fadeOut(tween(0)) else slideOutVertically(tween(Motion.STATE)) { it } + fadeOut(tween(Motion.STATE)),
        ) {
            val f = s.feedback ?: lastFeedback.value
            // While sliding away the panel is shown but can't be tapped again.
            if (f != null) FeedbackPanel(f, vm, interactive = s.feedback != null, combo = s.combo)
        }
    }

    if (askQuit) {
        val placement = s.type == SessionType.PLACEMENT
        DoranDialog({ askQuit = false }) {
            Text(if (placement) "Finish the placement?" else "Quit this lesson?", style = DoranType.h2, color = C.Text, modifier = Modifier.semantics { heading() })
            Text(
                if (placement) "We'll place you based on what you've answered so far." else "You'll lose your progress in this lesson.",
                style = DoranType.body, color = C.Text2, modifier = Modifier.padding(top = Space.s, bottom = Space.xl),
            )
            DoranButton("Keep learning", { askQuit = false }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.s))
            DoranButton(
                if (placement) "Finish" else "Quit", { askQuit = false; if (placement) vm.stopPlacementHere() else vm.quitSession() },
                Modifier.fillMaxWidth(), variant = ButtonVariant.Tertiary, tone = C.heart, tag = "confirmQuit",
            )
        }
    }
}

/** Heart count that jolts and rolls down when a heart is lost. */
@Composable
private fun HeartCounter(n: Int) {
    val reduce = LocalReduceMotion.current
    val shakeX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val prev = remember { mutableIntStateOf(n) }
    LaunchedEffect(n) {
        if (n < prev.intValue && !reduce) {
            launch { shakeX.shake(6f) }
            scale.pop(1.35f)
        }
        prev.intValue = n
    }
    Row(
        Modifier.heightIn(min = 48.dp).padding(horizontal = Space.s).testTag("hearts")
            .semantics(mergeDescendants = true) { contentDescription = "Hearts: $n" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Favorite, null, tint = C.Heart,
            modifier = Modifier.size(24.dp).graphicsLayer {
                translationX = shakeX.value.dp.toPx(); scaleX = scale.value; scaleY = scale.value
            },
        )
        AnimatedContent(
            n,
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None
                else (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "hearts", modifier = Modifier.clearAndSetSemantics { },
        ) { v ->
            Text("$v", style = DoranType.title, color = C.HeartInk, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * The answer sheet: a solid panel (jade tint when right, red tint when wrong) with an icon badge,
 * a title, the correct answer when wrong, an optional note, a replay button and Continue.
 */
@Composable
private fun FeedbackPanel(f: Feedback, vm: AppViewModel, interactive: Boolean, combo: Int) {
    val good = f.correct
    val tone = if (good) C.accent else C.heart
    val title = remember(f) { if (good) praise.random() else "Not quite" }
    val playing by vm.audio.playing.collectAsState()
    val shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
    val line = tone.fill
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(C.Bg)
            .background(tone.soft.copy(alpha = if (C.isDark) 1f else 0.9f))
            .drawBehind { drawRect(line, size = Size(size.width, 3.dp.toPx())) }
            .padding(start = Space.gutter, end = Space.gutter, top = Space.xl, bottom = Space.l)
            .testTag(if (good) "feedbackRight" else "feedbackWrong")
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).enterPop(delayMs = 40, from = 0.4f, durationMs = 320).clip(CircleShape).background(tone.fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (good) Icons.Filled.Check else Icons.Filled.Close, if (good) "Correct" else "Wrong", tint = tone.on, modifier = Modifier.size(24.dp))
            }
            Text(
                title, style = DoranType.h2, color = tone.ink,
                modifier = Modifier.padding(start = Space.m).weight(1f).enterRise(delayMs = 60, distance = 4.dp, durationMs = Motion.STATE + 60),
            )
            val sol = f.solution
            if (sol.isNotBlank() && Hangul.syllables(sol).isNotEmpty() && vm.audio.hasAudio(sol)) {
                AudioButton(playing == "ko:$sol", { if (interactive) vm.audio.play(sol) })
            }
        }
        // Five or more right answers in a row: a small streak chip, shown with the answer it belongs to.
        if (good && combo >= 5) {
            RewardChip("$combo in a row!", C.streak, Modifier.padding(top = Space.s).enterPop(delayMs = 120, from = 0.6f), icon = Icons.Filled.LocalFireDepartment)
        }
        if (!good && f.solution.isNotBlank()) {
            Text("Correct answer", style = DoranType.label, color = C.Text2, modifier = Modifier.padding(top = Space.m))
            Text(f.solution, style = DoranType.koBody, color = C.Text, modifier = Modifier.padding(top = 2.dp).testTag("solution"))
        }
        f.note?.let { Text(it, style = DoranType.body, color = if (good) tone.ink else C.Text2, modifier = Modifier.padding(top = Space.s)) }
        Spacer(Modifier.height(Space.l))
        DoranButton("Continue", { if (interactive) vm.next() }, Modifier.fillMaxWidth(), tone = tone, tag = if (interactive) "continue" else null)
    }
}

/**
 * The scaffold every exercise uses: a title, scrolling content, and a pinned action area with
 * optional secondary links above the Check button.
 */
@Composable
fun ExerciseFrame(
    title: String,
    locked: Boolean,
    ready: Boolean,
    onCheck: () -> Unit,
    checkLabel: String = "Check",
    secondary: (@Composable () -> Unit)? = null,
    showCheck: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
            Text(
                title, style = DoranType.h2, color = C.Text,
                modifier = Modifier.padding(top = Space.s, bottom = Space.xl).testTag("exerciseTitle").semantics { heading() },
            )
            content()
            Spacer(Modifier.height(Space.xxl))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m)) {
            if (secondary != null && !locked) secondary()
            if (showCheck) DoranButton(checkLabel, onCheck, Modifier.fillMaxWidth(), enabled = ready && !locked, tag = "check")
        }
    }
}

/** A low-emphasis action above the main button ("Can't listen now", "I don't know"). */
@Composable
fun SkipLink(text: String, tag: String, onClick: () -> Unit) {
    DoranButton(text, onClick, Modifier.fillMaxWidth(), variant = ButtonVariant.Tertiary, tone = Tone(C.Text2, C.Bg, C.Bg2, C.Text2), tag = tag)
}

@Composable
private fun ExerciseHost(vm: AppViewModel, ex: Exercise, locked: Boolean, placement: Boolean, result: Boolean?) {
    val idk: (@Composable () -> Unit)? = if (placement) { { SkipLink("I don't know", "dontKnow") { vm.submit(false) } } } else null
    when (ex) {
        is Exercise.LetterIntro -> LetterIntroView(vm, ex)
        is Exercise.LetterToSound -> LetterToSoundView(vm, ex, locked, result, idk)
        is Exercise.SoundToLetter -> SoundToLetterView(vm, ex, locked, result)
        is Exercise.WordMeaning -> WordMeaningView(vm, ex, locked, result, idk)
        is Exercise.PickKorean -> PickKoreanView(vm, ex, locked, result)
        is Exercise.ListenPick -> ListenPickView(vm, ex, locked, result)
        is Exercise.MatchPairs -> MatchPairsView(vm, ex, locked)
        is Exercise.BuildEnglish -> BuildEnglishView(vm, ex, locked, result, idk)
        is Exercise.BuildKorean -> BuildKoreanView(vm, ex, locked, result, idk)
        is Exercise.ListenBuild -> ListenBuildView(vm, ex, locked, result)
        is Exercise.FillBlank -> FillBlankView(vm, ex, locked, result, idk)
        is Exercise.Speak -> SpeakView(vm, ex, locked)
    }
}
