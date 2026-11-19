package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.audio.AudioHub
import com.doranalley.core.Engine
import com.doranalley.core.Episode
import com.doranalley.core.Help
import com.doranalley.core.MatchResult
import com.doranalley.core.Node
import com.doranalley.core.Progress
import com.doranalley.core.TranscriptEntry
import com.doranalley.core.TurnMemory
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.TurnUi
import com.doranalley.ui.s
import com.doranalley.ui.scene.CampusScene
import com.doranalley.ui.theme.Palette
import com.doranalley.ui.tr
import kotlinx.coroutines.delay

@Composable
fun PlayScreen(vm: AppViewModel, requestMic: () -> Unit) {
    val p = vm.progress ?: return
    val e = vm.engine ?: return
    val ep = e.episode
    val lastPartner = p.transcript.lastOrNull { it.kind == "partner" }
    val pose = (lastPartner?.nodeId?.let { ep.nodes[it] } as? Node.Line)?.pose ?: "idle"
    val turn = e.turn(p)
    var sheet by remember { mutableStateOf<TranscriptEntry?>(null) }
    val here = vm.content.campus.location(p.positions["learner"])

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = ep.title.tr() + (ep.variantLabel.takeIf { it.isNotEmpty() }?.let { " · " + it.tr() } ?: ""),
            subtitle = if (ep.reviewStatus == "draft") s("draft_dialogue") else ep.objective.tr(),
            onBack = vm::back, lang = vm.lang, onLang = vm::setLang,
        )
        CampusScene(
            campus = vm.content.campus, characters = vm.content.characters, positions = p.positions,
            speaker = if (vm.audio.playing?.let { k -> lastPartner?.nodeId?.let { k == it || k == "t:$it" } } == true || !e.isAuto(p)) lastPartner?.speaker else null,
            speakerPose = pose,
            learnerTalking = vm.turnUi == TurnUi.Listening || vm.turnUi == TurnUi.Recording,
            reducedMotion = vm.reducedMotion,
            description = here?.name?.tr() ?: "",
            modifier = Modifier.fillMaxWidth().weight(if (p.finished) 0.7f else 0.95f),
        )
        if (p.finished) {
            ReflectionPanel(vm, e, p, Modifier.weight(1.6f))
        } else {
            Box(Modifier.fillMaxWidth().weight(1.15f)) {
                Transcript(vm, p, onTap = { sheet = it })
                if (vm.awaitingTap) NarrationCard(e, p, vm::continueNarration, Modifier.align(Alignment.BottomCenter))
            }
            if (turn != null) TurnPanel(vm, turn, e.memory(p), requestMic)
            else Spacer(Modifier.navigationBarsPadding().padding(8.dp))
        }
    }
    sheet?.let { entry -> WordSheet(vm, entry) { sheet = null } }
}

@Composable
private fun NarrationCard(e: Engine, p: Progress, onContinue: () -> Unit, modifier: Modifier) {
    val n = e.current(p) as? Node.Narration ?: return
    Surface(
        modifier.fillMaxWidth().padding(12.dp), shape = MaterialTheme.shapes.medium,
        color = Palette.Paper, shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(n.text.tr(), style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            Button(onClick = onContinue, modifier = Modifier.align(Alignment.End)) { Text(s("continue")) }
        }
    }
}

@Composable
private fun Transcript(vm: AppViewModel, p: Progress, onTap: (TranscriptEntry) -> Unit) {
    val list = rememberLazyListState()
    LaunchedEffect(p.transcript.size) {
        if (p.transcript.isNotEmpty()) list.animateScrollToItem(p.transcript.size - 1)
    }
    LazyColumn(
        state = list, modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(p.transcript, key = { i, _ -> i }) { _, entry ->
            when (entry.kind) {
                "narration" -> Text(
                    entry.text.tr(), Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                    color = Palette.Muted, textAlign = TextAlign.Center,
                )
                "partner" -> PartnerBubble(vm, entry, onTap)
                else -> LearnerBubble(entry)
            }
        }
    }
}

@Composable
private fun PartnerBubble(vm: AppViewModel, entry: TranscriptEntry, onTap: (TranscriptEntry) -> Unit) {
    val c = entry.speaker?.let { vm.content.characters[it] }
    val color = c?.let { Color(it.color) } ?: Palette.Teal
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text(c?.nameKo?.take(1) ?: "?", color = color, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.size(8.dp))
        Column(Modifier.widthIn(max = 320.dp)) {
            Text("${c?.nameKo ?: ""} · ${c?.name?.tr() ?: ""}", fontSize = 12.sp, color = Palette.Muted)
            Surface(
                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp), color = Palette.Paper, shadowElevation = 1.dp,
                modifier = Modifier.clickable(enabled = entry.unlocked) { onTap(entry) },
            ) {
                Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f, fill = false)) {
                        Korean(entry.ko ?: "", size = 19)
                        if (entry.unlocked) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TouchApp, null, Modifier.size(14.dp), tint = Palette.Teal)
                            Text(" " + s("tap_meaning"), fontSize = 12.sp, color = Palette.Teal)
                        } else Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(12.dp), tint = Palette.Muted)
                            Text(" " + s("meaning_after"), fontSize = 11.sp, color = Palette.Muted)
                        }
                    }
                    IconButton(onClick = { vm.replayEntry(entry) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, s("replay"), tint = if (vm.audio.playing == "t:${entry.nodeId}" || vm.audio.playing == entry.nodeId) Palette.Persimmon else Palette.Muted)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearnerBubble(entry: TranscriptEntry) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp), color = Palette.TealSoft, modifier = Modifier.widthIn(max = 320.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                when {
                    entry.ko != null -> Korean(entry.ko, size = 18)
                    entry.evidence == "listening_only" -> Text(s("you_chose"), fontStyle = FontStyle.Italic, color = Palette.Muted)
                    else -> Text(s("you_recorded"), fontStyle = FontStyle.Italic, color = Palette.Muted)
                }
                val meaning = entry.text.tr()
                if (meaning.isNotBlank()) Text(meaning, fontSize = 13.sp, color = Palette.Muted, textAlign = TextAlign.End)
                else Text(s("partner_unclear"), fontSize = 13.sp, color = Palette.Muted)
            }
        }
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            EvidencePill(entry.evidence)
            HelpPill(entry.help)
        }
    }
}

// ------------------------------------------------------------------ the learner's turn

@Composable
private fun TurnPanel(vm: AppViewModel, turn: Node.Turn, mem: TurnMemory, requestMic: () -> Unit) {
    Surface(color = Palette.Paper, shadowElevation = 10.dp, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(s("your_turn"), Palette.PersimmonSoft)
                Spacer(Modifier.size(8.dp))
                Text(turn.task.tr(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            HintBox(vm, turn, mem.hint)
            if (vm.micDenied && vm.turnUi == TurnUi.Idle) {
                Surface(color = Palette.GoldSoft, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(10.dp)) {
                        Text(s("mic_denied_route"), fontSize = 13.sp)
                        Row {
                            TextButton(onClick = vm::chooseWithoutSpeaking) { Text(s("choose_reply")) }
                            TextButton(onClick = requestMic) { Text(s("allow_mic")) }
                        }
                    }
                }
            }
            when (val u = vm.turnUi) {
                TurnUi.Idle -> Controls(vm, mem, requestMic)
                TurnUi.Listening -> ListeningBar(vm, s("listening"), vm.recognizer.partial, vm.recognizer.level)
                TurnUi.Recording -> ListeningBar(vm, s("recording"), null, vm.recorder.level)
                is TurnUi.Heard -> HeardPanel(vm, u)
                is TurnUi.Choose -> ChoosePanel(vm, turn, u)
            }
        }
    }
}

@Composable
private fun HintBox(vm: AppViewModel, turn: Node.Turn, level: Int) {
    if (level <= Help.NONE) return
    Surface(color = Palette.GoldSoft, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1 · " + turn.hints.cue.tr(), fontSize = 14.sp)
            if (level >= Help.START) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("2 · ", fontSize = 14.sp); Korean(turn.hints.start, size = 17)
            }
            if (level >= Help.MODEL) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("3 · ", fontSize = 14.sp)
                Korean(turn.hints.model, size = 17, color = Palette.Persimmon)
                IconButton(onClick = vm::playModel) { Icon(Icons.AutoMirrored.Filled.VolumeUp, s("play_model"), tint = Palette.Persimmon) }
                if (vm.audio.sourceFor(turn.hints.modelAudio, vm.settings.allowSynthetic) == AudioHub.Source.SYNTHETIC) SyntheticBadge()
            }
        }
    }
}

/** Hint · microphone · replay — all within thumb reach at the bottom of the screen. */
@Composable
private fun Controls(vm: AppViewModel, mem: TurnMemory, requestMic: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        OutlinedButton(onClick = vm::hint, enabled = mem.hint < Help.MODEL) {
            Icon(Icons.Filled.Lightbulb, null, Modifier.size(18.dp))
            Text(" " + LocalStrings.current.f("hint_n", mem.hint, Help.MODEL))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LargeFloatingActionButton(
                onClick = { vm.speak(requestMic) }, shape = CircleShape,
                containerColor = Palette.Persimmon, contentColor = Color.White,
            ) { Icon(Icons.Filled.Mic, s("speak"), Modifier.size(36.dp)) }
            Text(
                when {
                    vm.settings.inputMode == com.doranalley.core.InputMode.LISTEN -> s("choose_reply")
                    vm.canRecognize() -> s("speak")
                    else -> s("record_reply")
                }, fontSize = 12.sp, color = Palette.Muted,
            )
        }
        IconButton(onClick = vm::replayLastLine, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.Replay, s("replay"), Modifier.size(30.dp), tint = Palette.Teal)
        }
    }
    if (!vm.micDenied && vm.settings.inputMode != com.doranalley.core.InputMode.LISTEN) {
        TextButton(onClick = vm::chooseWithoutSpeaking, modifier = Modifier.fillMaxWidth()) {
            Text(s("cant_speak_now"), fontSize = 12.sp, color = Palette.Muted)
        }
    }
}

@Composable
private fun ListeningBar(vm: AppViewModel, label: String, partial: String?, level: Float) {
    var secs by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); secs++ } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("$label  ${secs}s", color = Palette.Persimmon, fontWeight = FontWeight.SemiBold)
            LevelMeter(level)
            if (!partial.isNullOrBlank()) Korean(partial, size = 17, weight = FontWeight.Normal)
            Text(s("finish_when_ready"), fontSize = 12.sp, color = Palette.Muted)
        }
        LargeFloatingActionButton(onClick = vm::finishSpeaking, shape = CircleShape, containerColor = Palette.Ink, contentColor = Color.White) {
            Icon(Icons.Filled.Stop, s("stop"), Modifier.size(34.dp))
        }
    }
}

@Composable
private fun HeardPanel(vm: AppViewModel, h: TurnUi.Heard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s("app_heard") + "  ", color = Palette.Muted, fontSize = 13.sp)
            EvidencePill("recognized")
        }
        Korean("«${h.heard}»", size = 19)
        // How clearly it came through, against the closest model reply (a paraphrase can still be fine).
        // Below ~40 he most likely used different (still valid) wording, so no number is shown.
        vm.clarityFor(h.heard)?.takeIf { it.score >= 40 }?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s("clarity") + "  ", fontSize = 13.sp, color = Palette.Muted)
                Text("${c.score}", color = scoreColor(c.score), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(" / 100 · " + s("clarity_vs") + " ", fontSize = 12.sp, color = Palette.Muted)
                Korean(c.target, size = 13, weight = FontWeight.Normal, color = Palette.Muted)
            }
        }
        when (val r = h.result) {
            is MatchResult.Matched -> {
                Text(s("meaning_is") + " " + r.intent.meaning.tr(), fontSize = 15.sp)
                Button(onClick = { vm.confirmHeard(r.intent.id) }, modifier = Modifier.fillMaxWidth()) { Text(s("yes_meant")) }
            }
            is MatchResult.Ambiguous -> {
                Text(s("which_meant"), fontSize = 15.sp)
                r.candidates.forEach { c ->
                    OutlinedButton(onClick = { vm.confirmHeard(c.id) }, modifier = Modifier.fillMaxWidth()) { Text(c.meaning.tr()) }
                }
            }
            is MatchResult.NoMatch -> {
                Text(s("heard_unclear"), fontSize = 14.sp)
                Button(onClick = vm::sendUnclear, modifier = Modifier.fillMaxWidth()) { Text(s("send_anyway")) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::retry, modifier = Modifier.weight(1f)) { Text(s("say_again")) }
            OutlinedButton(onClick = vm::correctHeard, modifier = Modifier.weight(1f)) { Text(s("not_what_i_said")) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoosePanel(vm: AppViewModel, turn: Node.Turn, c: TurnUi.Choose) {
    var picked by remember(c) { mutableStateOf<String?>(null) }
    var used by remember(c) { mutableStateOf(setOf<String>()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (c.listeningOnly) {
            EvidencePill("listening_only")
            Text(s("choose_listening_hint"), fontSize = 13.sp, color = Palette.Muted)
        } else {
            EvidencePill("self_confirmed")
            Text(s("choose_self_hint"), fontSize = 13.sp, color = Palette.Muted)
            c.take?.let { path ->
                OutlinedButton(onClick = { vm.playTake(path) }) {
                    Icon(Icons.Filled.PlayArrow, null); Text(" " + s("play_my_reply"))
                }
            }
            c.heard?.let { Text(s("app_heard") + " «$it»", fontSize = 13.sp, color = Palette.Muted) }
        }
        Text(s("what_did_you_mean"), fontWeight = FontWeight.SemiBold)
        val options = turn.intents.filter { it.manual }
        options.forEach { o ->
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                    .selectable(selected = picked == o.id, role = Role.RadioButton) { picked = o.id }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = picked == o.id, onClick = null)
                Text(o.meaning.tr(), Modifier.padding(start = 6.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                .selectable(selected = picked == OTHER, role = Role.RadioButton) { picked = OTHER }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = picked == OTHER, onClick = null)
            Text(s("something_else"), Modifier.padding(start = 6.dp), color = Palette.Muted)
        }
        if (!c.listeningOnly && turn.targets.isNotEmpty() && picked != null && picked != OTHER) {
            Text(s("which_did_you_say"), fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                turn.targets.mapNotNull { vm.content.vocab[it] }.forEach { v ->
                    FilterChip(
                        selected = v.id in used,
                        onClick = { used = if (v.id in used) used - v.id else used + v.id },
                        label = { Text(v.ko, fontSize = 15.sp) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::retry, modifier = Modifier.weight(1f)) { Text(s("back_to_mic")) }
            Button(
                onClick = { val id = picked ?: return@Button; if (id == OTHER) vm.sendUnclear() else vm.confirmChoice(id, used) },
                enabled = picked != null, modifier = Modifier.weight(1f),
            ) { Text(s("confirm")) }
        }
    }
}

private const val OTHER = "__other"

// ------------------------------------------------------------------ words after the exchange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WordSheet(vm: AppViewModel, entry: TranscriptEntry, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.Paper) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Korean(entry.ko ?: "", size = 21)
                    Text(entry.text.tr(), color = Palette.Muted)
                }
                IconButton(onClick = { vm.replayEntry(entry) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, s("replay"), tint = Palette.Teal) }
            }
            if (vm.audio.lastSource == AudioHub.Source.SYNTHETIC) SyntheticBadge()
            val words = entry.words.mapNotNull { vm.content.vocab[it] }
            if (words.isEmpty()) Text(s("no_new_words"), color = Palette.Muted)
            words.forEach { v ->
                SectionCard(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Korean(v.ko, size = 19)
                            Text(v.meaning.tr())
                            v.note.tr().takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Palette.Muted) }
                        }
                        IconButton(onClick = { vm.playKo("w:${v.id}", v.ko, v.audio) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, s("listen")) }
                    }
                    val saved = vm.isSaved(v.id)
                    if (saved) Pill(s("saved_for_review"), Palette.TealSoft)
                    else OutlinedButton(onClick = { vm.saveWord(v.id, entry) }) { Text(s("save_with_sentence")) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ reflection

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReflectionPanel(vm: AppViewModel, e: Engine, p: Progress, modifier: Modifier) {
    val r = remember(p) { e.reflection(p) }
    val st = LocalStrings.current
    val vocab = vm.content.vocab
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(st["episode_done"], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(st.f("goals_clarifications", r.goals, r.clarifications), color = Palette.Muted)

        @Composable
        fun group(title: String, ids: List<String>, bg: Color) {
            SectionCard {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (ids.isEmpty()) Text(st["none_yet"], color = Palette.Muted)
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ids.mapNotNull { vocab[it] }.forEach { Pill("${it.ko} · ${it.meaning.tr()}", bg) }
                }
            }
        }
        group(st["said_on_your_own"], r.independent, Palette.TealSoft)
        group(st["said_with_help"], r.assisted, Palette.GoldSoft)
        group(st["not_yet_produced"], r.notYet, MaterialTheme.colorScheme.surfaceVariant)

        SectionCard {
            Text(st["coming_back"], fontWeight = FontWeight.SemiBold)
            e.episode.targets.forEach { id ->
                val item = vm.review[id]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Korean(vocab[id]?.ko ?: id, size = 16)
                    Spacer(Modifier.weight(1f))
                    Text(item?.let { whenText(it.nextReview, vm.now()) } ?: "—", color = Palette.Muted, fontSize = 13.sp)
                }
            }
        }
        SectionCard(color = Palette.PersimmonSoft) {
            Text(st["practice_point"], fontWeight = FontWeight.SemiBold)
            Text(r.practicePoint.tr())
        }
        r.takeaway?.let { vocab[it] }?.let { v ->
            SectionCard(color = Palette.GoldSoft) {
                Text(st["take_away"], fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Korean(v.ko, size = 26); Text(v.meaning.tr()) }
                    IconButton(onClick = { vm.playKo("w:${v.id}", v.ko, v.audio) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, st["listen"]) }
                }
            }
        }
        if (r.unlocks.isNotEmpty()) SectionCard(color = Palette.TealSoft) {
            Text(st["unlocked"], fontWeight = FontWeight.SemiBold)
            r.unlocks.forEach { Text("✦ " + unlockName(vm, it)) }
        }
        val mirrorId = e.episode.parent ?: e.episode.id
        Button(onClick = { vm.go(com.doranalley.ui.Screen.Mirror(mirrorId)) }, modifier = Modifier.fillMaxWidth()) { Text(st["open_mirror"]) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.openEpisode(e.episode.id, restart = true) }, modifier = Modifier.weight(1f)) { Text(st["play_again"]) }
            OutlinedButton(onClick = { vm.go(com.doranalley.ui.Screen.Home) }, modifier = Modifier.weight(1f)) { Text(st["stop_here"]) }
        }
    }
}

@Composable
fun unlockName(vm: AppViewModel, key: String): String {
    val (kind, id) = key.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return when (kind) {
        "character" -> vm.content.characters[id]?.let { "${it.nameKo} · ${it.name.tr()} — ${it.role.tr()}" } ?: id
        "location" -> vm.content.campus.location(id)?.let { "${it.ko} · ${it.name.tr()}" } ?: id
        else -> key
    }
}

@Suppress("unused")
private fun Episode.isRecall() = parent != null
