@file:OptIn(ExperimentalLayoutApi::class)

package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doranalley.core.Checker
import com.doranalley.core.Exercise
import com.doranalley.core.Tile
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.components.AudioButton
import com.doranalley.ui.components.BrushCircle
import com.doranalley.ui.components.BubbleBox
import com.doranalley.ui.components.ChoiceState
import com.doranalley.ui.components.ChoiceTile
import com.doranalley.ui.components.GlossText
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.RewardChip
import com.doranalley.ui.components.StateBadge
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Space
import kotlinx.coroutines.delay

// Every view keeps its own selection, checks the answer with the same rules as before and calls
// vm.submit(); [result] (the feedback's verdict, null until checked) only colours the tiles.

// ------------------------------------------------------------------ shared pieces

@Composable
private fun Speakers(vm: AppViewModel, text: String, slow: Boolean = true) {
    val playing by vm.audio.playing.collectAsState()
    Row(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        AudioButton(playing == "ko:$text", { vm.audio.play(text) }, size = 88.dp, tag = "play")
        if (slow) {
            Spacer(Modifier.width(Space.xl))
            AudioButton(playing == "ko-slow:$text", { vm.audio.play(text, slow = true) }, size = 56.dp, slow = true, tag = "playSlow")
        }
    }
}

/** Horangi with a speech bubble holding a speaker and the prompt (words tap for hints). He reacts to the answer. */
@Composable
private fun MascotPrompt(text: String, vm: AppViewModel, audioText: String?, korean: Boolean, result: Boolean?) {
    val playing = vm.audio.playing.collectAsState().value
    val mood = when (result) { true -> Mood.CHEER; false -> Mood.SAD; null -> Mood.HAPPY }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = Space.l)) {
        Mascot(size = 72.dp, mood = mood)
        Spacer(Modifier.width(Space.xs))
        BubbleBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (audioText != null) {
                    AudioButton(playing == "ko:$audioText", { vm.audio.play(audioText) }, tag = "play")
                    Spacer(Modifier.width(Space.m))
                }
                val gloss = vm.course.gloss
                GlossText(text, if (korean) gloss::korean else gloss::english, fontSize = DoranType.koBody.fontSize, bold = korean)
            }
        }
    }
}

/** The tile state for [option] once an answer is checked: the right answer is marked, and a wrong pick too. */
private fun optionState(option: String, selected: String?, answer: String, result: Boolean?): ChoiceState = when {
    result == null -> if (selected == option) ChoiceState.Selected else ChoiceState.Normal
    option == answer -> ChoiceState.Correct
    option == selected -> ChoiceState.Wrong
    else -> ChoiceState.Normal
}

@Composable
private fun OptionList(options: List<String>, selected: String?, answer: String, locked: Boolean, result: Boolean?, onPick: (String) -> Unit, korean: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        options.forEach { o ->
            ChoiceTile(
                optionState(o, selected, answer, result), { if (!locked) onPick(o) }, Modifier.fillMaxWidth(),
                enabled = !locked, tag = "option:$o", role = Role.RadioButton,
                contentPadding = PaddingValues(start = Space.l, end = 40.dp, top = Space.m, bottom = Space.m),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(o, style = if (korean) DoranType.koTile else DoranType.option)
            }
        }
    }
}

@Composable
private fun OptionGrid(options: List<String>, selected: String?, answer: String, locked: Boolean, result: Boolean?, onPick: (String) -> Unit, style: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.fillMaxWidth()) {
                row.forEach { o ->
                    ChoiceTile(
                        optionState(o, selected, answer, result), { if (!locked) onPick(o) }, Modifier.weight(1f),
                        enabled = !locked, tag = "option:$o", role = Role.RadioButton, minHeight = 96.dp,
                    ) {
                        Text(o, style = style, textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A single large letter on a card (the letter questions). */
@Composable
private fun LetterCard(letter: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.sheet)
    Box(
        modifier.size(156.dp).clip(shape).background(C.LavenderSoft).border(1.5.dp, C.Lavender.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = DoranType.koLetter, color = C.LavenderInk, modifier = Modifier.testTag("prompt"))
    }
}

// ------------------------------------------------------------------ Hangul

@Composable
fun LetterIntroView(vm: AppViewModel, ex: Exercise.LetterIntro) {
    val playing by vm.audio.playing.collectAsState()
    ExerciseFrame("New letter", locked = false, ready = true, onCheck = { vm.next() }, checkLabel = "Continue") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(212.dp), contentAlignment = Alignment.Center) {
                val shape = RoundedCornerShape(36.dp)
                Box(
                    Modifier.size(184.dp).enterPop(from = 0.8f, durationMs = 380).clip(shape).background(C.LavenderSoft)
                        .border(1.5.dp, C.Lavender.copy(alpha = 0.5f), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    BrushCircle(Modifier.size(150.dp), color = C.Lavender.copy(alpha = 0.55f), delayMs = 250, durationMs = 900, width = 6.dp)
                    Text(ex.letter.ch, style = DoranType.koLetter, color = C.LavenderInk)
                }
            }
            Text("“${ex.letter.rom}”", style = DoranType.h1, color = C.LavenderInk, modifier = Modifier.padding(top = Space.s).enterRise(delayMs = 160, distance = 6.dp))
            Text(ex.letter.hint, style = DoranType.body, color = C.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.xs))
            Spacer(Modifier.height(Space.xl))
            AudioButton(playing == "ko:${ex.letter.say}", { vm.audio.play(ex.letter.say) }, size = 72.dp, tag = "play")
            Text("In a syllable: ${ex.letter.say}", style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = Space.s))
        }
    }
}

@Composable
fun LetterToSoundView(vm: AppViewModel, ex: Exercise.LetterToSound, locked: Boolean, result: Boolean?, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("What sound does this make?", locked, sel != null, { vm.submit(sel == ex.letter.rom) }, secondary = secondary) {
        Box(Modifier.fillMaxWidth().padding(bottom = Space.xxl), contentAlignment = Alignment.Center) { LetterCard(ex.letter.ch) }
        OptionGrid(ex.options, sel, ex.letter.rom, locked, result, { sel = it }, DoranType.h1)
    }
}

@Composable
fun SoundToLetterView(vm: AppViewModel, ex: Exercise.SoundToLetter, locked: Boolean, result: Boolean?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Which letter do you hear?", locked, sel != null, { vm.submit(sel == ex.letter.ch) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.letter.say, slow = false)
        Spacer(Modifier.height(Space.xxl))
        OptionGrid(ex.options.map { it.ch }, sel, ex.letter.ch, locked, result, { sel = it }, DoranType.koPrompt)
    }
}

// ------------------------------------------------------------------ words

@Composable
fun WordMeaningView(vm: AppViewModel, ex: Exercise.WordMeaning, locked: Boolean, result: Boolean?, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    val playing by vm.audio.playing.collectAsState()
    ExerciseFrame(if (ex.isNew) "New word" else "What does this mean?", locked, sel != null, { vm.submit(sel == ex.word.en) }, secondary = secondary) {
        if (ex.isNew) {
            RewardChip("New word", C.lavender, Modifier.enterPop(from = 0.8f), icon = Icons.Filled.AutoAwesome)
            Spacer(Modifier.height(Space.m))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.l)) {
            AudioButton(playing == "ko:${ex.word.ko}", { vm.audio.play(ex.word.ko) }, size = 56.dp, tag = "play")
            Text(ex.word.ko, style = DoranType.koPrompt, color = C.Text, modifier = Modifier.padding(start = Space.l).testTag("prompt"))
        }
        if (ex.isNew) {
            Text("means “${ex.word.en}”", style = DoranType.body, color = C.Text2, modifier = Modifier.padding(bottom = Space.l).testTag("newMeaning"))
        }
        OptionList(ex.options, sel, ex.word.en, locked, result, { sel = it })
    }
}

@Composable
fun PickKoreanView(vm: AppViewModel, ex: Exercise.PickKorean, locked: Boolean, result: Boolean?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Which one is “${ex.word.en}”?", locked, sel != null, { vm.submit(sel == ex.word.ko) }) {
        OptionList(ex.options.map { it.ko }, sel, ex.word.ko, locked, result, { sel = it; vm.audio.play(it) }, korean = true)
    }
}

@Composable
fun ListenPickView(vm: AppViewModel, ex: Exercise.ListenPick, locked: Boolean, result: Boolean?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("What do you hear?", locked, sel != null, { vm.submit(sel == ex.ko) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.ko, slow = true)
        Spacer(Modifier.height(Space.xxl))
        OptionList(ex.options, sel, ex.ko, locked, result, { sel = it }, korean = true)
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

    // Matching submits by itself when the last pair is found, so there is no Check button.
    ExerciseFrame("Tap the matching pairs", locked, ready = false, onCheck = {}, showCheck = false) {
        Text("${matched.size} of ${answer.size} matched", style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(bottom = Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                left.forEach { l ->
                    val done = l in matched
                    val st = when {
                        rightPair?.first == l -> ChoiceState.Correct
                        done -> ChoiceState.Done
                        wrongPair?.first == l -> ChoiceState.Wrong
                        selL == l -> ChoiceState.Selected
                        else -> ChoiceState.Normal
                    }
                    ChoiceTile(st, {
                        if (!done && !locked) { selL = l; if (ex.leftAudio) vm.audio.play(l); tryMatch() }
                    }, Modifier.fillMaxWidth(), enabled = !done && !locked, tag = "matchL:$l", minHeight = 64.dp, badge = false,
                        contentPadding = PaddingValues(horizontal = Space.s, vertical = Space.m)) {
                        Text(l, style = if (l.length == 1) DoranType.koPrompt else DoranType.koBody, textAlign = TextAlign.Center)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                right.forEach { r ->
                    val done = answer.any { (k, v) -> v == r && k in matched }
                    val st = when {
                        rightPair?.second == r -> ChoiceState.Correct
                        done -> ChoiceState.Done
                        wrongPair?.second == r -> ChoiceState.Wrong
                        selR == r -> ChoiceState.Selected
                        else -> ChoiceState.Normal
                    }
                    ChoiceTile(st, { if (!done && !locked) { selR = r; tryMatch() } },
                        Modifier.fillMaxWidth(), enabled = !done && !locked, tag = "matchR:$r", minHeight = 64.dp, badge = false,
                        contentPadding = PaddingValues(horizontal = Space.s, vertical = Space.m)) {
                        Text(r, style = DoranType.body, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ word bank

/**
 * The answer area grows with what is in it (no fixed lines), and shows the verdict with a
 * coloured outline plus a check or cross once the answer is checked.
 */
@Composable
private fun WordBank(tiles: List<Tile>, chosen: MutableList<Tile>, locked: Boolean, korean: Boolean, result: Boolean?, onTap: (Tile) -> Unit = {}) {
    val style = if (korean) DoranType.koBody else DoranType.option
    val shape = RoundedCornerShape(Radius.card)
    val border = when (result) { true -> C.Accent; false -> C.Heart; null -> C.OutlineStrong.copy(alpha = 0.6f) }
    val dashed = result == null
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp).clip(shape).background(C.Bg2)
            .drawBehind {
                val st = (if (dashed) 1.5.dp else 2.5.dp).toPx()
                drawRoundRect(
                    border, cornerRadius = CornerRadius(Radius.card.toPx()),
                    style = Stroke(st, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null),
                )
            }
            .testTag("answerArea")
            .semantics {
                contentDescription = if (chosen.isEmpty()) "Your answer: empty" else "Your answer: " + chosen.joinToString(" ") { it.text }
                when (result) { true -> stateDescription = "Correct"; false -> stateDescription = "Wrong"; null -> {} }
            },
    ) {
        if (chosen.isEmpty()) {
            Text(
                "Tap the words below", style = DoranType.bodySmall, color = C.Muted,
                modifier = Modifier.align(Alignment.Center).clearAndSetSemantics { },
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s),
            modifier = Modifier.padding(Space.m).padding(end = if (result != null) 24.dp else 0.dp),
        ) {
            chosen.forEach { t ->
                key(t.id) {
                    ChoiceTile(ChoiceState.Normal, { if (!locked) chosen.remove(t) }, Modifier.enterPop(from = 0.85f, durationMs = 200),
                        enabled = !locked, tag = "chosen:${t.text}", badge = false, minHeight = 48.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = Space.s)) {
                        Text(t.text, style = style)
                    }
                }
            }
        }
        if (result != null) StateBadge(if (result) ChoiceState.Correct else ChoiceState.Wrong, Modifier.align(Alignment.TopEnd).padding(Space.s), size = 22.dp)
    }
    Spacer(Modifier.height(Space.xl))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(Space.s),
        modifier = Modifier.fillMaxWidth(),
    ) {
        tiles.forEach { t ->
            val used = chosen.any { it.id == t.id }
            if (used) {
                // A dashed slot keeps the bank from reflowing while the word is in the answer.
                val dash = C.OutlineStrong.copy(alpha = 0.6f)
                Box(
                    Modifier.heightIn(min = 48.dp).drawBehind {
                        drawRoundRect(dash, cornerRadius = CornerRadius(Radius.tile.toPx()),
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
                    }.padding(horizontal = 14.dp + 1.5.dp, vertical = Space.s + 1.5.dp).clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) { Text(t.text, style = style, color = Color.Transparent) }
            } else {
                ChoiceTile(ChoiceState.Normal, { if (!locked) { chosen.add(t); onTap(t) } }, enabled = !locked, tag = "bank:${t.text}",
                    badge = false, minHeight = 48.dp, contentPadding = PaddingValues(horizontal = 14.dp, vertical = Space.s)) {
                    Text(t.text, style = style)
                }
            }
        }
    }
}

@Composable
fun BuildEnglishView(vm: AppViewModel, ex: Exercise.BuildEnglish, locked: Boolean, result: Boolean?, secondary: (@Composable () -> Unit)?) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Translate this sentence", locked, chosen.isNotEmpty(), { vm.submit(Checker.englishCorrect(ex.sentence, chosen.map { it.text })) }, secondary = secondary) {
        MascotPrompt(ex.sentence.ko, vm, ex.sentence.ko, korean = true, result = result)
        WordBank(ex.tiles, chosen, locked, korean = false, result = result)
    }
}

@Composable
fun BuildKoreanView(vm: AppViewModel, ex: Exercise.BuildKorean, locked: Boolean, result: Boolean?, secondary: (@Composable () -> Unit)?) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Write this in Korean", locked, chosen.isNotEmpty(), { vm.submit(Checker.koreanCorrect(ex.sentence, chosen.map { it.text })) }, secondary = secondary) {
        MascotPrompt(ex.sentence.en, vm, null, korean = false, result = result)
        WordBank(ex.tiles, chosen, locked, korean = true, result = result)
    }
}

@Composable
fun ListenBuildView(vm: AppViewModel, ex: Exercise.ListenBuild, locked: Boolean, result: Boolean?) {
    val chosen = remember { mutableStateListOf<Tile>() }
    ExerciseFrame("Tap what you hear", locked, chosen.isNotEmpty(), { vm.submit(Checker.koreanCorrect(ex.sentence, chosen.map { it.text }), note = ex.sentence.en) },
        secondary = { SkipLink("Can't listen now", "cantListen") { vm.skipExercise(Exercise.Kind.LISTEN) } }) {
        Speakers(vm, ex.sentence.ko)
        Spacer(Modifier.height(Space.xl))
        WordBank(ex.tiles, chosen, locked, korean = true, result = result)
    }
}

// ------------------------------------------------------------------ fill in the blank

@Composable
fun FillBlankView(vm: AppViewModel, ex: Exercise.FillBlank, locked: Boolean, result: Boolean?, secondary: (@Composable () -> Unit)?) {
    var sel by remember { mutableStateOf<String?>(null) }
    ExerciseFrame("Fill in the blank", locked, sel != null, { vm.submit(sel == ex.fill.answer, note = ex.fill.en) }, secondary = secondary) {
        Text(ex.fill.en, style = DoranType.body, color = C.Text2, modifier = Modifier.padding(bottom = Space.l))
        FlowRow(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = Space.xxl)) {
            if (ex.fill.before.isNotBlank()) Text(ex.fill.before, style = DoranType.koTile, color = C.Text, modifier = Modifier.align(Alignment.CenterVertically))
            val (fill, line) = when {
                result == true -> C.AccentSoft to C.Accent
                result == false -> C.HeartSoft to C.Heart
                sel != null -> C.SelectSoft to C.Select
                else -> Color.Transparent to C.OutlineStrong
            }
            Box(
                Modifier.padding(horizontal = 6.dp).align(Alignment.CenterVertically)
                    .defaultMinSize(minWidth = 72.dp, minHeight = 44.dp)
                    .clip(RoundedCornerShape(Radius.badge + 2.dp)).background(fill)
                    .drawBehind {
                        val y = size.height - 1.5.dp.toPx()
                        drawLine(line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                    }
                    .padding(horizontal = Space.m, vertical = Space.xs)
                    .testTag("blank")
                    .semantics { contentDescription = sel?.let { "Blank: $it" } ?: "Blank" },
                contentAlignment = Alignment.Center,
            ) {
                key(sel) {
                    Text(sel ?: " ", style = DoranType.koTile, color = if (sel != null) C.SelectInk else C.Muted, modifier = if (sel != null) Modifier.enterPop(from = 0.7f, durationMs = 220) else Modifier)
                }
            }
            if (ex.fill.after.isNotBlank()) Text(ex.fill.after, style = DoranType.koTile, color = C.Text, modifier = Modifier.align(Alignment.CenterVertically))
        }
        OptionList(ex.options, sel, ex.fill.answer, locked, result, { sel = it }, korean = true)
    }
}
