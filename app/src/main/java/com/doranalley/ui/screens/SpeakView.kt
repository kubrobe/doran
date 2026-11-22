package com.doranalley.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.doranalley.audio.KoreanAsr
import com.doranalley.core.Exercise
import com.doranalley.core.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.SpeakState
import com.doranalley.ui.components.AudioButton
import com.doranalley.ui.components.BubbleBox
import com.doranalley.ui.components.ButtonVariant
import com.doranalley.ui.components.DoranButton
import com.doranalley.ui.components.GlossText
import com.doranalley.ui.components.GlowBurst
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.ProgressRing
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.pressScale
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Space
import kotlin.math.roundToInt

fun hasMic(ctx: android.content.Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Keeps track of the microphone permission and re-checks it when the app comes back from Settings. */
@Composable
fun rememberMicPermission(): Pair<Boolean, () -> Unit> {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasMic(ctx)) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        askedOnce = true
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = hasMic(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val request: () -> Unit = {
        val act = ctx as? Activity
        val blocked = askedOnce && act != null && !act.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (blocked) {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return granted to request
}

@Composable
fun SpeakView(vm: AppViewModel, ex: Exercise.Speak, locked: Boolean) {
    val state by vm.speak.collectAsState()
    val level by vm.recorder.level.collectAsState()
    val asrState by vm.asr.state.collectAsState()
    val playing by vm.audio.playing.collectAsState()
    val (granted, request) = rememberMicPermission()

    // Passing automatically shows the green panel.
    LaunchedEffect(state) {
        val s = state
        if (s is SpeakState.Scored && s.result.passed && !locked) vm.submit(true, note = "Pronunciation: ${s.result.score}%")
    }
    DisposableEffect(Unit) { onDispose { vm.stopSpeaking() } }

    val canGiveUp = when (val s = state) {
        is SpeakState.Scored -> !s.result.passed && s.attempt >= 2
        is SpeakState.NotHeard -> s.attempt >= 2
        is SpeakState.Error -> true
        else -> false
    }
    val mood = when (val s = state) {
        is SpeakState.Scored -> if (s.result.score >= 85) Mood.CHEER else if (s.result.passed) Mood.HAPPY else Mood.THINK
        is SpeakState.NotHeard, is SpeakState.Error -> Mood.THINK
        else -> Mood.HAPPY
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
            Text(
                "Speak this sentence", style = DoranType.h2, color = C.Text,
                modifier = Modifier.padding(top = Space.s, bottom = Space.xl).testTag("exerciseTitle").semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(size = 72.dp, mood = mood)
                Spacer(Modifier.width(Space.xs))
                BubbleBox {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AudioButton(playing == "ko:${ex.ko}", { vm.audio.play(ex.ko) }, tag = "play")
                            Spacer(Modifier.width(Space.s))
                            AudioButton(playing == "ko-slow:${ex.ko}", { vm.audio.play(ex.ko, slow = true) }, slow = true, tag = "playSlow")
                        }
                        Spacer(Modifier.height(Space.s))
                        val s = state
                        if (s is SpeakState.Scored) ColoredSentence(ex.ko, s.result.heard)
                        else GlossText(ex.ko, vm.course.gloss::korean, fontSize = DoranType.koTile.fontSize, bold = true)
                        Text(ex.en, style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = Space.xs))
                    }
                }
            }
            Spacer(Modifier.height(Space.xxl))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!granted) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(C.SurfaceRaised), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MicOff, null, tint = C.Muted, modifier = Modifier.size(36.dp))
                    }
                    Text(
                        "Speaking practice needs the microphone. Your voice stays on this phone.",
                        style = DoranType.body, textAlign = TextAlign.Center, color = C.Text, modifier = Modifier.padding(vertical = Space.m),
                    )
                    DoranButton("Allow microphone", request, Modifier.fillMaxWidth(), tone = C.lavender, icon = Icons.Filled.Mic, tag = "allowMic")
                } else {
                    when (val s = state) {
                        SpeakState.Idle -> { MicButton(false, level) { vm.startSpeaking(ex.ko) }; Hint("Tap the microphone and read the sentence") }
                        SpeakState.Recording -> {
                            MicButton(true, level) { vm.stopSpeaking() }
                            LevelMeter(level)
                            Hint("Listening\u2026 tap to stop")
                        }
                        SpeakState.Checking -> {
                            CheckingDots(Modifier.testTag("checking"))
                            Hint(if (asrState is KoreanAsr.State.Loading) "Getting the speech checker ready\u2026" else "Checking your pronunciation\u2026")
                        }
                        is SpeakState.Scored -> ScoreView(vm, s, ex)
                        is SpeakState.NotHeard -> {
                            Text("I couldn't hear you clearly.", style = DoranType.title, color = C.Text, modifier = Modifier.testTag("notHeard"))
                            Hint("Hold the phone closer and speak a little louder.")
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.fillMaxWidth()) {
                                SmallAction("Hear yourself", Icons.Filled.PlayArrow, "hearSelf", Modifier.weight(1f)) { vm.playTake() }
                                SmallAction("Try again", Icons.Filled.Refresh, "tryAgain", Modifier.weight(1f)) { vm.startSpeaking(ex.ko) }
                            }
                        }
                        is SpeakState.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.m)) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = C.HeartInk, modifier = Modifier.size(22.dp))
                                Text(s.message, style = DoranType.body, color = C.HeartInk, modifier = Modifier.padding(start = Space.s).testTag("speakError"))
                            }
                            SmallAction("Try again", Icons.Filled.Refresh, "tryAgain") { vm.resetSpeak() }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m)) {
            if (!locked) SkipLink("Can't speak now", "cantSpeak") { vm.skipExercise(Exercise.Kind.SPEAK) }
            if (canGiveUp && !locked) {
                DoranButton("Continue", { vm.submit(false, note = "We'll practise this one again later.", penalty = false) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, tag = "giveUp")
            }
        }
    }
}

/** Three dots hopping in turn while the speech checker works (still with reduced motion). */
@Composable
private fun CheckingDots(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val phase = if (reduce) null else {
        val t = rememberInfiniteTransition(label = "dots")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")
    }
    Row(
        modifier.height(72.dp).semantics { contentDescription = "Checking" },
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            Box(
                Modifier.size(14.dp).graphicsLayer {
                    val ph = phase?.value ?: return@graphicsLayer
                    val p = ((ph - i * 0.18f) % 1f + 1f) % 1f
                    val hop = if (p < 0.5f) kotlin.math.sin(p * 2.0 * Math.PI).toFloat() else 0f
                    translationY = -hop * 12.dp.toPx()
                }.clip(CircleShape).background(C.Lavender.copy(alpha = 0.5f + 0.25f * i)),
            )
        }
    }
}

@Composable
private fun Hint(text: String) = Text(text, style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.m, bottom = Space.s))

/**
 * The microphone: lavender and still when idle; red with a stop icon while recording, its ring
 * following the voice level, and ripples while (and only while) it records.
 */
@Composable
private fun MicButton(recording: Boolean, level: Float, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val source = remember { MutableInteractionSource() }
    val lvl by animateFloatAsState(if (recording) level.coerceIn(0f, 1f) else 0f, if (reduce) snap() else spring(stiffness = 300f), label = "lvl")
    val ripple = if (recording && !reduce) {
        rememberInfiniteTransition(label = "mic").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "r")
    } else null
    val face by animateColorAsState(if (recording) C.Heart else C.Lavender, tween(Motion.STATE), label = "face")
    val on = if (recording) C.OnHeart else C.OnLavender
    Box(
        Modifier.size(160.dp).drawBehind {
            val base = 48.dp.toPx()
            if (recording) drawCircle(face.copy(alpha = 0.18f + 0.30f * lvl), base * (1.25f + lvl * 0.45f))
            val r = ripple?.value ?: return@drawBehind
            for (k in 0..1) {
                val p = (r + k / 2f) % 1f
                drawCircle(face.copy(alpha = 0.35f * (1f - p)), base * (1f + p * 0.6f), style = Stroke(2.dp.toPx()))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(96.dp).pressScale(source, scale = 0.94f).clip(CircleShape).background(face)
                .clickable(source, null, role = Role.Button, onClickLabel = if (recording) "Stop recording" else "Start recording", onClick = onClick)
                .semantics { contentDescription = if (recording) "Stop recording" else "Record your voice" }
                .testTag(if (recording) "stopMic" else "mic"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, null, tint = on, modifier = Modifier.size(44.dp))
        }
    }
}

/** Five bars showing how loud the microphone hears you (drawn from the live level, no animation of its own). */
@Composable
private fun LevelMeter(level: Float) {
    val l = level.coerceIn(0f, 1f)
    Row(
        Modifier.padding(top = Space.xs).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom,
    ) {
        repeat(5) { i ->
            val lit = l > i * 0.18f + 0.02f
            Box(Modifier.width(6.dp).height((8 + i * 4).dp).clip(RoundedCornerShape(3.dp)).background(if (lit) C.Heart else C.Outline))
        }
    }
}

@Composable
private fun SmallAction(text: String, icon: ImageVector, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    DoranButton(text, onClick, modifier, variant = ButtonVariant.Secondary, icon = icon, tag = tag)
}

@Composable
private fun ScoreView(vm: AppViewModel, s: SpeakState.Scored, ex: Exercise.Speak) {
    val score = s.result.score
    val reduce = LocalReduceMotion.current
    val (tone, icon, verdict) = when {
        score >= 85 -> Triple(C.accent, Icons.Filled.CheckCircle, "Great pronunciation!")
        s.result.passed -> Triple(C.xp, Icons.Filled.ThumbUp, "Good! Listen again and compare.")
        else -> Triple(C.heart, Icons.Filled.Replay, "Not quite. Listen and try again.")
    }
    // The ring sweeps to the score while the number counts up.
    val sweep = remember(s) { Animatable(if (reduce) score / 100f else 0f) }
    LaunchedEffect(s) { if (!reduce) sweep.animateTo(score / 100f, tween(900, easing = Motion.EaseOut)) }
    Box(
        Modifier.size(120.dp).enterPop(from = 0.8f).testTag("score").semantics { contentDescription = "Pronunciation score $score percent" },
        contentAlignment = Alignment.Center,
    ) {
        if (score >= 85) GlowBurst(Modifier.requiredSize(200.dp), color = tone.fill, key = s, durationMs = 1000)
        ProgressRing(sweep.value, Modifier.size(120.dp), color = tone.fill, stroke = 10.dp)
        Text("${(sweep.value * 100).roundToInt()}%", style = DoranType.h1, color = C.Text, modifier = Modifier.clearAndSetSemantics { })
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.m)) {
        Icon(icon, null, tint = tone.ink, modifier = Modifier.size(20.dp))
        Text(verdict, style = DoranType.title, color = C.Text, modifier = Modifier.padding(start = Space.s))
    }
    Text("I heard: ${s.result.heard}", style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.xs).testTag("heard"))
    MarkLegend(Modifier.padding(top = Space.m, bottom = Space.m))
    Row(horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.fillMaxWidth()) {
        SmallAction("Hear yourself", Icons.Filled.PlayArrow, "hearSelf", Modifier.weight(1f)) { vm.playTake() }
        if (!s.result.passed) SmallAction("Try again", Icons.Filled.Refresh, "tryAgain", Modifier.weight(1f)) { vm.startSpeaking(ex.ko) }
    }
}

private fun markStyle(m: Hangul.Mark): SpanStyle = when (m) {
    Hangul.Mark.GOOD -> SpanStyle(color = C.AccentInk)
    Hangul.Mark.CLOSE -> SpanStyle(color = C.XpInk, textDecoration = TextDecoration.Underline)
    Hangul.Mark.MISSED -> SpanStyle(color = C.HeartInk, textDecoration = TextDecoration.LineThrough)
}

/** How to read the marked sentence: colour plus underline / strike-through, never colour alone. */
@Composable
private fun MarkLegend(modifier: Modifier = Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "Plain syllables were said well, underlined ones were close, struck-through ones were missed." },
        horizontalArrangement = Arrangement.spacedBy(Space.l), verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Hangul.Mark.GOOD to "Good", Hangul.Mark.CLOSE to "Close", Hangul.Mark.MISSED to "Missed").forEach { (m, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { }) {
                Text(buildAnnotatedString { withStyle(markStyle(m)) { append("가") } }, style = DoranType.koBody)
                Text(label, style = DoranType.caption, color = C.Text2, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

/**
 * The target sentence with each syllable marked by how well it was heard: plain (good), underlined
 * (close) or struck through (missed), each also coloured. TalkBack reads which syllables were missed.
 * Word joiners (U+2060) between the syllables of a word keep Android from breaking a line inside a word.
 */
@Composable
private fun ColoredSentence(target: String, heard: String) {
    val marks = remember(target, heard) { Hangul.markSyllables(target, heard) }
    val missed = marks.filter { it.second == Hangul.Mark.MISSED && Hangul.isSyllable(it.first) }.joinToString("") { "${it.first}" }
    val close = marks.filter { it.second == Hangul.Mark.CLOSE && Hangul.isSyllable(it.first) }.joinToString("") { "${it.first}" }
    val spoken = target + when {
        missed.isEmpty() && close.isEmpty() -> ". Every syllable was clear."
        else -> (if (close.isNotEmpty()) ". Close: $close" else "") + (if (missed.isNotEmpty()) ". Missed: $missed" else "")
    }
    Text(buildAnnotatedString {
        var prev = ' '
        marks.forEach { (c, m) ->
            if (c != ' ' && prev != ' ') append('\u2060')
            if (Hangul.isSyllable(c)) withStyle(markStyle(m)) { append(c) } else append(c)
            prev = c
        }
    }, style = DoranType.koTile, color = C.Text, modifier = Modifier.testTag("prompt").semantics { contentDescription = spoken })
}
