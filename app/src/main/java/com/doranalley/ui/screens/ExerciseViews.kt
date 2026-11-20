@file:OptIn(ExperimentalLayoutApi::class)

package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.requiredSize
import com.doranalley.ui.components.BrushCircle
import com.doranalley.ui.components.DancheongBand
import com.doranalley.ui.components.GlowBurst
import com.doranalley.ui.components.glass
import com.doranalley.ui.components.jewel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.core.Checker
import com.doranalley.core.Exercise
import com.doranalley.core.Tile
import com.doranalley.ui.AppViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import com.doranalley.ui.components.BoxState
import com.doranalley.ui.components.BubbleBox
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.theme.Nunito
import com.doranalley.ui.components.Bubble
import com.doranalley.ui.components.GlossText
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.SelectBox
import com.doranalley.ui.components.SpeakerButton
import com.doranalley.ui.theme.C
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ shared pieces

@Composable
private fun Speakers(vm: AppViewModel, text: String, slow: Boolean = true, big: Boolean = true) {
    val playing by vm.audio.playing.collectAsState()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        SpeakerButton(playing == "ko:$text", { vm.audio.play(text) }, size = if (big) 96.dp else 64.dp, tag = "play")
        if (slow) {
            Spacer(Modifier.width(16.dp))
            SpeakerButton(playing == "ko-slow:$text", { vm.audio.play(text, slow = true) }, size = if (big) 64.dp else 48.dp, slow = true, tag = "playSlow")
        }
    }
}

/** Mascot with a speech bubble; the bubble can hold a speaker and the prompt (words tap for hints). */
@Composable
private fun MascotPrompt(text: String, vm: AppViewModel, audioText: String?, korean: Boolean) {
    val playing = vm.audio.playing.collectAsState().value
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Mascot(size = 80.dp)
        Spacer(Modifier.width(4.dp))
        BubbleBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (audioText != null) {
                    SpeakerButton(playing == "ko:$audioText", { vm.audio.play(audioText) }, size = 40.dp, tag = "play")
                    Spacer(Modifier.width(10.dp))
                }
                val gloss = vm.course.gloss
                GlossText(text, if (korean) gloss::korean else gloss::english)
            }
        }
    }
}

@Composable
private fun OptionList(options: List<String>, selected: String?, locked: Boolean, onPick: (String) -> Unit, big: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, o ->
            SelectBox(selected == o, { if (!locked) onPick(o) }, Modifier.fillMaxWidth(), tag = "option:$o", enabled = !locked) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val on = selected == o
                    val badge by animateColorAsState(if (on) C.Blue else C.Line, tween(160), label = "badge")
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (on) C.Blue else Color.Transparent)
                            .border(2.dp, badge, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = if (on) Color.White else C.Muted, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito, fontSize = 14.sp)
                    }
                    Text(o, fontSize = if (big) 24.sp else 18.sp, color = C.Ink, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }
}

@Composable
private fun OptionGrid(options: List<String>, selected: String?, locked: Boolean, onPick: (String) -> Unit, fontSize: Int = 40) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { o ->
                    SelectBox(selected == o, { if (!locked) onPick(o) }, Modifier.weight(1f).height(110.dp), tag = "option:$o", enabled = !locked) {
                        Text(o, fontSize = fontSize.sp, color = C.Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ------------------------------------------------------------------ Hangul

@Composable
fun LetterIntroView(vm: AppViewModel, ex: Exercise.LetterIntro) {
    val playing by vm.audio.playing.collectAsState()
    ExerciseFrame("New letter", locked = false, ready = true, onCheck = { vm.next() }, checkLabel = "Continue") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val card = RoundedCornerShape(40.dp)
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                GlowBurst(Modifier.requiredSize(300.dp), color = C.Purple, durationMs = 1100)
                Box(
                    Modifier.size(188.dp).enterPop(from = 0.7f, durationMs = 480).jewel(C.Purple, C.PurpleDark, card, elevation = 22.dp).clip(card),
                    contentAlignment = Alignment.Center,
                ) {
                    DancheongBand(Modifier.fillMaxWidth().height(8.dp).align(Alignment.TopCenter), alpha = 0.7f)
                    BrushCircle(Modifier.size(150.dp), color = Color.White.copy(alpha = 0.55f), delayMs = 350, durationMs = 950, width = 6.dp)
                    Text(ex.letter.ch, fontSize = 108.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.enterPop(delayMs = 120, from = 0.4f, durationMs = 520))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("“${ex.letter.rom}”", fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = Nunito, color = C.PurpleInk, modifier = Modifier.enterRise(delayMs = 260))
            Text(ex.letter.hint, fontSize = 17.sp, color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(20.dp))
            SpeakerButton(playing == "ko:${ex.letter.say}", { vm.audio.play(ex.letter.say) }, size = 80.dp, tag = "play")
            Text("in a syllable: ${ex.letter.say}", color = C.Muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun LetterToSoundView(vm: AppViewModel, ex: Exercise.LetterToSound, locked: Boolean, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("What sound does this make?", locked, sel != null, { vm.submit(sel == ex.letter.rom) }, secondary = secondary) {
        Box(Modifier.fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
            Text(ex.letter.ch, fontSize = 96.sp, fontWeight = FontWeight.Bold, color = C.Ink, modifier = Modifier.enterPop(from = 0.6f).testTag("prompt"))
        }
        OptionGrid(ex.options, sel, locked, { sel = it }, fontSize = 26)
    }
}

@Composable
fun SoundToLetterView(vm: AppViewModel, ex: Exercise.SoundToLetter, locked: Boolean) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Which letter do you hear?", locked, sel != null, { vm.submit(sel == ex.letter.ch) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.letter.say, slow = false)
        Spacer(Modifier.height(24.dp))
        OptionGrid(ex.options.map { it.ch }, sel, locked, { sel = it })
    }
}

// ------------------------------------------------------------------ words

@Composable
fun WordMeaningView(vm: AppViewModel, ex: Exercise.WordMeaning, locked: Boolean, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    val playing by vm.audio.playing.collectAsState()
    ExerciseFrame(if (ex.isNew) "New word" else "What does this mean?", locked, sel != null, { vm.submit(sel == ex.word.en) }, secondary = secondary) {
        if (ex.isNew) {
            Text("✨ New word", color = C.PurpleInk, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito, fontSize = 14.sp,
                modifier = Modifier.enterPop(from = 0.7f).glass(CircleShape, tint = C.Purple, tintAlpha = 0.2f, elevation = 6.dp, glow = C.Purple).padding(horizontal = 12.dp, vertical = 4.dp))
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
            SpeakerButton(playing == "ko:${ex.word.ko}", { vm.audio.play(ex.word.ko) }, size = 56.dp, tag = "play")
            Text(ex.word.ko, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = C.Ink, modifier = Modifier.padding(start = 16.dp).testTag("prompt"))
        }
        if (ex.isNew) {
            Text("means “${ex.word.en}”", color = C.Muted, fontSize = 17.sp, modifier = Modifier.padding(bottom = 16.dp).testTag("newMeaning"))
        }
        OptionList(ex.options, sel, locked, { sel = it })
    }
}

@Composable
fun PickKoreanView(vm: AppViewModel, ex: Exercise.PickKorean, locked: Boolean) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Which one is “${ex.word.en}”?", locked, sel != null, { vm.submit(sel == ex.word.ko) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ex.options.forEach { w ->
                SelectBox(sel == w.ko, { if (!locked) { sel = w.ko; vm.audio.play(w.ko) } }, Modifier.fillMaxWidth().height(84.dp), tag = "option:${w.ko}", enabled = !locked) {
                    Text(w.ko, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                }
            }
        }
    }
}

@Composable
fun ListenPickView(vm: AppViewModel, ex: Exercise.ListenPick, locked: Boolean) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("What do you hear?", locked, sel != null, { vm.submit(sel == ex.ko) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.ko, slow = true)
        Spacer(Modifier.height(24.dp))
        OptionList(ex.options, sel, locked, { sel = it }, big = true)
    }
}

// ------------------------------------------------------------------ match pairs

@Composable
fun MatchPairsView(vm: AppViewModel, ex: Exercise.MatchPairs, locked: Boolean) {
    val left = remember { ex.pairs.map { it.first }.shuffled() }
    val right = remember { ex.pairs.map { it.second }.shuffled() }
    val answer = remember { ex.pairs.toMap() }
    val matched = remember { mutableStateListOf<String>() }
    var selL by remember { mutableStateOf<String?>(null) }
    var selR by remember { mutableStateOf<String?>(null) }
    var wrongPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var rightPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mistakes by remember { mutableStateOf(0) }

    fun tryMatch() {
        val l = selL ?: return
        val r = selR ?: return
        if (answer[l] == r) {
            matched += l
            rightPair = l to r
            selL = null; selR = null
            if (vm.progress.value.settings.sounds) vm.audio.effect(com.doranalley.audio.AudioPlayer.Sfx.CORRECT)
            if (matched.size == answer.size) vm.submit(true, if (mistakes == 0) "All pairs matched!" else null)
        } else {
            mistakes++
            wrongPair = l to r
            selL = null; selR = null
            if (vm.progress.value.settings.sounds) vm.audio.effect(com.doranalley.audio.AudioPlayer.Sfx.WRONG)
        }
    }
    LaunchedEffect(wrongPair) { if (wrongPair != null) { delay(600); wrongPair = null } }
    LaunchedEffect(rightPair) { if (rightPair != null) { delay(450); rightPair = null } }

    ExerciseFrame("Tap the matching pairs", locked, ready = false, onCheck = {}) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                left.forEach { l ->
                    val done = l in matched
                    val st = when {
                        rightPair?.first == l -> BoxState.RIGHT
                        done -> BoxState.DONE
                        wrongPair?.first == l -> BoxState.WRONG
                        selL == l -> BoxState.SELECTED
                        else -> BoxState.NORMAL
                    }
                    SelectBox(selL == l, {
                        if (!done && !locked) { selL = l; if (ex.leftAudio) vm.audio.play(l); tryMatch() }
                    }, Modifier.fillMaxWidth().height(64.dp).alpha(fadeDone(done && st != BoxState.RIGHT)), state = st, enabled = !done && !locked, tag = "matchL:$l") {
                        Text(l, fontSize = if (l.length > 6) 17.sp else 22.sp, fontWeight = FontWeight.Bold, color = C.Ink, textAlign = TextAlign.Center)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                right.forEach { r ->
                    val done = answer.any { (k, v) -> v == r && k in matched }
                    val st = when {
                        rightPair?.second == r -> BoxState.RIGHT
                        done -> BoxState.DONE
                        wrongPair?.second == r -> BoxState.WRONG
                        selR == r -> BoxState.SELECTED
                        else -> BoxState.NORMAL
                    }
                    SelectBox(selR == r, { if (!done && !locked) { selR = r; tryMatch() } },
                        Modifier.fillMaxWidth().height(64.dp).alpha(fadeDone(done && st != BoxState.RIGHT)), state = st, enabled = !done && !locked, tag = "matchR:$r") {
                        Text(r, fontSize = if (r.length > 14) 13.sp else 16.sp, color = C.Ink, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ word bank

@Composable
private fun WordBank(tiles: List<Tile>, chosen: MutableList<Tile>, locked: Boolean, korean: Boolean, onTap: (Tile) -> Unit = {}) {
    val fs = if (korean) 20.sp else 18.sp
    // Answer lines
    Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 128.dp).testTag("answerArea")) {
        Box(Modifier.fillMaxWidth().padding(top = 63.dp).height(1.5.dp).background(C.LineDark))
        Box(Modifier.fillMaxWidth().padding(top = 126.dp).height(1.5.dp).background(C.LineDark))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            chosen.forEach { t ->
                key(t.id) {
                    SelectBox(false, { if (!locked) chosen.remove(t) }, Modifier.enterPop(from = 0.7f, durationMs = 260), tag = "chosen:${t.text}", enabled = !locked) {
                        Text(t.text, fontSize = fs, color = C.Ink)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        tiles.forEach { t ->
            val used = chosen.any { it.id == t.id }
            if (used) {
                val dash = C.LineDark
                Box(Modifier.drawBehind {
                    drawRoundRect(dash.copy(alpha = 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                    drawRoundRect(dash, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))))
                }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(t.text, fontSize = fs, color = Color.Transparent)
                }
            } else {
                SelectBox(false, { if (!locked) { chosen.add(t); onTap(t) } }, tag = "bank:${t.text}", enabled = !locked) {
                    Text(t.text, fontSize = fs, color = C.Ink)
                }
            }
        }
    }
}

@Composable
fun BuildEnglishView(vm: AppViewModel, ex: Exercise.BuildEnglish, locked: Boolean, secondary: (@Composable () -> Unit)?) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Translate this sentence", locked, chosen.isNotEmpty(), { vm.submit(Checker.englishCorrect(ex.sentence, chosen.map { it.text })) }, secondary = secondary) {
        MascotPrompt(ex.sentence.ko, vm, ex.sentence.ko, korean = true)
        WordBank(ex.tiles, chosen, locked, korean = false)
    }
}

@Composable
fun BuildKoreanView(vm: AppViewModel, ex: Exercise.BuildKorean, locked: Boolean, secondary: (@Composable () -> Unit)?) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Write this in Korean", locked, chosen.isNotEmpty(), { vm.submit(Checker.koreanCorrect(ex.sentence, chosen.map { it.text })) }, secondary = secondary) {
        MascotPrompt(ex.sentence.en, vm, null, korean = false)
        WordBank(ex.tiles, chosen, locked, korean = true)
    }
}

@Composable
fun ListenBuildView(vm: AppViewModel, ex: Exercise.ListenBuild, locked: Boolean) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Tap what you hear", locked, chosen.isNotEmpty(), { vm.submit(Checker.koreanCorrect(ex.sentence, chosen.map { it.text }), note = ex.sentence.en) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.sentence.ko)
        Spacer(Modifier.height(20.dp))
        WordBank(ex.tiles, chosen, locked, korean = true)
    }
}

// ------------------------------------------------------------------ fill in the blank

@Composable
fun FillBlankView(vm: AppViewModel, ex: Exercise.FillBlank, locked: Boolean, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Fill in the blank", locked, sel != null, { vm.submit(sel == ex.fill.answer, note = ex.fill.en) }, secondary = secondary) {
        Text(ex.fill.en, color = C.Muted, fontSize = 17.sp, modifier = Modifier.padding(bottom = 16.dp))
        FlowRow(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            if (ex.fill.before.isNotBlank()) Text(ex.fill.before, fontSize = 26.sp, color = C.Ink, modifier = Modifier.align(Alignment.CenterVertically))
            Box(
                Modifier.padding(horizontal = 4.dp).glass(RoundedCornerShape(12.dp), tint = if (sel != null) C.Blue else Color.Unspecified, elevation = 4.dp)
                    .border(2.dp, if (sel != null) C.Blue else C.LineDark, RoundedCornerShape(12.dp))
                    .defaultMinSize(minWidth = 70.dp).padding(horizontal = 10.dp, vertical = 4.dp).align(Alignment.CenterVertically).testTag("blank"),
                contentAlignment = Alignment.Center,
            ) { key(sel) { Text(sel ?: " ", fontSize = 26.sp, color = C.BlueInk, fontWeight = FontWeight.Bold, modifier = if (sel != null) Modifier.enterPop(from = 0.6f, durationMs = 260) else Modifier) } }
            if (ex.fill.after.isNotBlank()) Text(ex.fill.after, fontSize = 26.sp, color = C.Ink, modifier = Modifier.align(Alignment.CenterVertically))
        }
        OptionList(ex.options, sel, locked, { sel = it }, big = true)
    }
}

@Composable
fun BigBubble(text: String) = Bubble(text, fontSize = 22.sp)

@Suppress("unused")
@Composable
private fun Placeholder() = ChunkyButton("", {})

@Composable
private fun fadeDone(done: Boolean): Float =
    animateFloatAsState(if (done) 0.45f else 1f, tween(250), label = "fadeDone").value
