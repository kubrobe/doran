package com.doranalley.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.doranalley.audio.KoreanAsr
import com.doranalley.core.Exercise
import com.doranalley.core.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.SpeakState
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.GhostButton
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Nunito
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f, fill = false).padding(horizontal = 20.dp)) {
            Text("Speak this sentence", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp).testTag("exerciseTitle"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(size = 72.dp)
                Spacer(Modifier.width(2.dp))
                com.doranalley.ui.components.BubbleBox { Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.doranalley.ui.components.SpeakerButton(playing == "ko:${ex.ko}", { vm.audio.play(ex.ko) }, size = 40.dp, tag = "play")
                        Spacer(Modifier.width(6.dp))
                        com.doranalley.ui.components.SpeakerButton(playing == "ko-slow:${ex.ko}", { vm.audio.play(ex.ko, slow = true) }, size = 36.dp, slow = true, tag = "playSlow")
                    }
                    Spacer(Modifier.height(8.dp))
                    val s = state
                    if (s is SpeakState.Scored) ColoredSentence(ex.ko, s.result.heard) else com.doranalley.ui.components.GlossText(ex.ko, vm.course.gloss::korean, fontSize = 22.sp, bold = true)
                    Text(ex.en, fontSize = 15.sp, color = C.Muted, modifier = Modifier.padding(top = 4.dp))
                } }
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!granted) {
                Icon(Icons.Filled.MicOff, null, tint = C.Muted, modifier = Modifier.size(48.dp))
                Text("Speaking practice needs the microphone. Your voice stays on this phone.", textAlign = TextAlign.Center, color = C.Ink, modifier = Modifier.padding(vertical = 10.dp))
                ChunkyButton("Allow microphone", request, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark, tag = "allowMic")
            } else {
                when (val s = state) {
                    SpeakState.Idle -> { MicButton(false, level) { vm.startSpeaking(ex.ko) }; Hint("Tap the microphone and read the sentence") }
                    SpeakState.Recording -> { MicButton(true, level) { vm.stopSpeaking() }; Hint("Listening… tap to stop") }
                    SpeakState.Checking -> {
                        CheckingDots(Modifier.testTag("checking"))
                        Hint(if (asrState is KoreanAsr.State.Loading) "Getting the speech checker ready…" else "Checking your pronunciation…")
                    }
                    is SpeakState.Scored -> ScoreView(vm, s, ex)
                    is SpeakState.NotHeard -> {
                        Text("I couldn't hear you clearly.", fontWeight = FontWeight.Bold, color = C.Ink, modifier = Modifier.testTag("notHeard"))
                        Hint("Hold the phone closer and speak a little louder.")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmallAction("Hear yourself", Icons.Filled.PlayArrow, "hearSelf") { vm.playTake() }
                            SmallAction("Try again", Icons.Filled.Refresh, "tryAgain") { vm.startSpeaking(ex.ko) }
                        }
                    }
                    is SpeakState.Error -> {
                        Text(s.message, color = C.RedDark, textAlign = TextAlign.Center, modifier = Modifier.testTag("speakError"))
                        Spacer(Modifier.height(8.dp))
                        SmallAction("Try again", Icons.Filled.Refresh, "tryAgain") { vm.resetSpeak() }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (!locked) SkipLink("Can't speak now", "cantSpeak") { vm.skipExercise(Exercise.Kind.SPEAK) }
            if (canGiveUp && !locked) {
                Spacer(Modifier.height(8.dp))
                GhostButton("Continue", { vm.submit(false, note = "We'll practise this one again later.", penalty = false) }, Modifier.fillMaxWidth(), tag = "giveUp")
            }
        }
    }
}

/** Three dots hopping in turn while the speech checker works. */
@Composable
private fun CheckingDots(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dots")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")
    Row(modifier.height(64.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier.size(16.dp).graphicsLayer {
                    val p = ((phase - i * 0.18f) % 1f + 1f) % 1f
                    val hop = if (p < 0.5f) kotlin.math.sin(p * 2.0 * Math.PI).toFloat() else 0f
                    translationY = -hop * 14.dp.toPx()
                }.clip(CircleShape).background(listOf(C.Purple, C.Blue, C.Green)[i]),
            )
        }
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))

@Composable
private fun MicButton(recording: Boolean, level: Float, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 5.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
    // The voice ring follows the microphone level smoothly.
    val lvl by animateFloatAsState(if (recording) level.coerceIn(0f, 1f) else 0f, spring(stiffness = 300f), label = "lvl")
    val ripple = if (recording) {
        val t = rememberInfiniteTransition(label = "mic")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "r").value
    } else -1f
    val face by animateColorAsState(if (recording) C.Red else C.Blue, tween(200), label = "face")
    val edge by animateColorAsState(if (recording) C.RedDark else C.BlueDark, tween(200), label = "edge")
    Box(
        Modifier.size(170.dp).drawBehind {
            val base = 52.dp.toPx()
            if (ripple >= 0f) {
                for (k in 0..2) {
                    val p = (ripple + k / 3f) % 1f
                    drawCircle(C.Red.copy(alpha = 0.22f * (1f - p)), base * (1f + p * 0.6f), style = Stroke(3.dp.toPx()))
                }
                drawCircle(C.Red.copy(alpha = 0.16f), base * (1.02f + lvl * 0.55f))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(104.dp).clip(CircleShape)
                .clickable(source, null, onClick = onClick).testTag(if (recording) "stopMic" else "mic")
                .padding(top = sink).background(edge, CircleShape)
                .padding(bottom = (6.dp - sink).coerceAtLeast(0.dp)).background(face, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, if (recording) "Stop" else "Speak", tint = Color.White, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(C.Card).border(2.dp, C.Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = C.Blue, modifier = Modifier.size(22.dp))
        Text(text, color = C.Blue, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ScoreView(vm: AppViewModel, s: SpeakState.Scored, ex: Exercise.Speak) {
    val score = s.result.score
    val color = when { score >= 85 -> C.Green; s.result.passed -> C.Gold; else -> C.Red }
    // The ring sweeps to the score while the number counts up.
    val sweep = remember(s) { Animatable(0f) }
    LaunchedEffect(s) { sweep.animateTo(score / 100f, tween(900, easing = Motion.EaseOut)) }
    Box(Modifier.size(104.dp).enterPop(from = 0.7f).testTag("score"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(104.dp)) {
            val st = 9.dp.toPx()
            drawCircle(color.copy(alpha = 0.12f), size.minDimension / 2f - st / 2f)
            drawArc(C.Line, 0f, 360f, false, topLeft = Offset(st / 2, st / 2), size = Size(size.width - st, size.height - st), style = Stroke(st))
            drawArc(color, -90f, 360f * sweep.value, false, topLeft = Offset(st / 2, st / 2), size = Size(size.width - st, size.height - st), style = Stroke(st, cap = StrokeCap.Round))
        }
        Text("${(sweep.value * 100).roundToInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = Nunito, color = color)
    }
    Text(
        when { score >= 85 -> "Great pronunciation!"; s.result.passed -> "Good! Listen again and compare."; else -> "Not quite. Listen and try again." },
        fontWeight = FontWeight.ExtraBold, fontFamily = Nunito, fontSize = 17.sp, color = C.Ink, modifier = Modifier.padding(top = 10.dp).enterRise(delayMs = 200),
    )
    Text("I heard: ${s.result.heard}", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp).testTag("heard"))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SmallAction("Hear yourself", Icons.Filled.PlayArrow, "hearSelf") { vm.playTake() }
        if (!s.result.passed) SmallAction("Try again", Icons.Filled.Refresh, "tryAgain") { vm.startSpeaking(ex.ko) }
    }
}

/** The target sentence with each syllable coloured by how well it was heard. */
@Composable
private fun ColoredSentence(target: String, heard: String) {
    val marks = remember(target, heard) { Hangul.markSyllables(target, heard) }
    Text(buildAnnotatedString {
        marks.forEach { (c, m) ->
            val col = when (m) { Hangul.Mark.GOOD -> C.GreenDark; Hangul.Mark.CLOSE -> C.GoldDark; Hangul.Mark.MISSED -> C.Red }
            withStyle(SpanStyle(color = if (Hangul.isSyllable(c)) col else C.Ink)) { append(c) }
        }
    }, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("prompt"))
}
