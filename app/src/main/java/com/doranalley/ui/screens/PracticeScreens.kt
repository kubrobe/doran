package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.audio.AudioHub
import com.doranalley.core.Evidence
import com.doranalley.core.Help
import com.doranalley.core.KoText
import com.doranalley.core.MirrorPhrase
import com.doranalley.core.ReviewItem
import com.doranalley.core.Scheduler
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.Screen
import com.doranalley.ui.theme.Palette
import com.doranalley.ui.tr
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ recall

@Composable
fun RecallScreen(vm: AppViewModel, requestMic: () -> Unit) {
    val st = LocalStrings.current
    // Snapshot the due list on entry so it doesn't shift while items are rescheduled.
    val ids = remember { vm.dueItems().map { it.id } }
    var index by remember { mutableIntStateOf(0) }
    var practiceAnyway by remember { mutableStateOf<List<String>?>(null) }
    val list = practiceAnyway ?: ids
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(st["recall_title"], vm::back, vm.lang, vm::setLang, subtitle = st["recall_sub"])
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val item = list.getOrNull(index)?.let { vm.review[it] }
            if (item != null) {
                Text(st.f("card_n_of", index + 1, list.size), color = Palette.Muted)
                RecallCard(vm, item, "recall", requestMic) { index++ }
            } else {
                SectionCard {
                    Text(if (list.isEmpty()) st["nothing_due"] else st["recall_done"], fontWeight = FontWeight.SemiBold)
                    vm.nextDue()?.let { Text(st["next_review"] + " " + whenText(it.nextReview, vm.now()), color = Palette.Muted) }
                    if (vm.review.isNotEmpty()) OutlinedButton(onClick = {
                        practiceAnyway = vm.questItems().map { it.id }; index = 0
                    }) { Text(st["practice_anyway"]) }
                }
                val twist = vm.twistEpisode()
                SectionCard(color = Palette.PersimmonSoft) {
                    Text(st["twist_title"], fontWeight = FontWeight.SemiBold)
                    if (twist != null) {
                        Text(twist.objective.tr(), fontSize = 14.sp)
                        Pill(twist.variantLabel.tr(), Palette.Paper)
                        Button(onClick = { vm.openEpisode(twist.id, restart = true) }) { Text(st["start"]) }
                    } else Text(st["twist_locked"], fontSize = 14.sp, color = Palette.Muted)
                }
            }
        }
    }
}

/**
 * One spoken-recall card: he tries to produce the expression before any model is revealed.
 * Peeking is recorded as assisted recall even if he then says it correctly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecallCard(vm: AppViewModel, item: ReviewItem, setting: String, requestMic: () -> Unit, onDone: () -> Unit) {
    val st = LocalStrings.current
    val key = "recall:${item.id}"
    var hintShown by remember(item.id) { mutableStateOf(false) }
    var revealed by remember(item.id) { mutableStateOf(false) }
    var saidAloud by remember(item.id) { mutableStateOf(false) }
    val cap = vm.captured?.takeIf { it.key == key }
    val attempted = cap != null || saidAloud
    val ctx = remember(item.id) { Scheduler.contextFor(item) }
    val vocab = vm.content.vocab[item.id.removePrefix("saved:")]
    val heard = cap?.alternatives?.firstOrNull()
    val heardIt = cap != null && heard != null && vocab != null &&
        KoText.used(cap.alternatives.joinToString(" "), listOf(vocab)).isNotEmpty()

    fun finish(help: Int, produced: Boolean) {
        val ev = if (heard != null) Evidence.RECOGNIZED else Evidence.SELF_CONFIRMED
        vm.recordRecall(item.id, help, produced, ev, setting)
        vm.clearCapture()
        onDone()
    }

    SectionCard {
        Pill(if (item.source == "saved") st["saved_word"] else st["lesson_expression"], Palette.TealSoft)
        Text(st["how_do_you_say"], color = Palette.Muted)
        Text(item.meaning.tr(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ctx?.let { Text(st["situation"] + ": " + it.gloss.tr(), fontSize = 14.sp, color = Palette.Muted) }
        if (hintShown && !revealed) Korean(item.ko.take(1) + "…", size = 22, color = Palette.Persimmon)

        if (!revealed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.capturing == key) {
                    Button(onClick = vm::stopCapture) { Icon(Icons.Filled.Stop, null); Text(" " + st["stop"]) }
                    if (vm.recognizer.partial.isNotBlank()) Korean(vm.recognizer.partial, size = 16, weight = FontWeight.Normal)
                } else {
                    Button(onClick = { vm.startCapture(key, recognize = true, requestPermission = requestMic) }) {
                        Icon(Icons.Filled.Mic, null); Text(" " + st["say_it"])
                    }
                    if (!attempted) TextButton(onClick = { saidAloud = true }) { Text(st["said_aloud"]) }
                }
            }
        }
        if (heard != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(st["app_heard"] + " «$heard»  ", fontSize = 14.sp)
            if (heardIt) Pill("✓", Palette.TealSoft)
        }
        cap?.file?.let { f -> OutlinedButton(onClick = { vm.playTake(f) }) { Icon(Icons.Filled.PlayArrow, null); Text(" " + st["play_my_reply"]) } }

        if (!revealed) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!hintShown) OutlinedButton(onClick = { hintShown = true }) { Text(st["first_sound"]) }
            FilledTonalButton(onClick = { revealed = true }) { Text(st["reveal"]) }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Korean(item.ko, size = 24, color = Palette.Persimmon)
                IconButton(onClick = { vm.playKo(key, item.ko, item.audio) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, st["listen"]) }
                if (vm.audio.sourceFor(item.audio, vm.settings.allowSynthetic) == AudioHub.Source.SYNTHETIC) SyntheticBadge()
            }
            ctx?.let { Korean(it.ko, size = 16, weight = FontWeight.Normal, color = Palette.Muted) }
            if (!attempted) {
                Text(st["peeked_note"], fontSize = 13.sp, color = Palette.Muted)
                Button(onClick = { finish(Help.MODEL, false) }) { Text(st["next"]) }
            } else {
                Text(st["how_did_it_go"], fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!hintShown) Button(onClick = { finish(Help.NONE, true) }) { Text(st["rate_own"]) }
                    OutlinedButton(onClick = { finish(Help.START, true) }) { Text(st["rate_hint"]) }
                    OutlinedButton(onClick = { finish(Help.MODEL, false) }) { Text(st["rate_not_yet"]) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ five-minute quest

@Composable
fun QuestScreen(vm: AppViewModel, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val items = remember { vm.questItems().map { it.id } }
    val phrase = remember {
        val first = items.firstOrNull()?.removePrefix("saved:")
        val phrases = vm.content.mainEpisodes().flatMap { it.mirrorPhrases }
        phrases.firstOrNull { it.vocab == first } ?: phrases.firstOrNull()
    }
    var step by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(vm.content.config.sideQuestMinutes * 60) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(st["quest_title"], vm::back, vm.lang, vm::setLang, subtitle = "%d:%02d".format(left / 60, left % 60))
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                items.isEmpty() -> SectionCard {
                    Text(st["nothing_learned_yet"])
                    Button(onClick = { vm.content.mainEpisodes().firstOrNull()?.let { vm.openEpisode(it.id) } }) { Text(st["play"]) }
                }
                step < items.size -> {
                    Text(st.f("card_n_of", step + 1, items.size + 1), color = Palette.Muted)
                    val item = vm.review[items[step]]
                    if (item != null) RecallCard(vm, item, "quest", requestMic) { step++ }
                    else LaunchedEffect(step) { step++ }
                }
                step == items.size && phrase != null -> {
                    Text(st.f("card_n_of", step + 1, items.size + 1), color = Palette.Muted)
                    MirrorPhraseCard(vm, phrase, requestMic)
                    Button(onClick = { vm.clearCapture(); step++ }, modifier = Modifier.fillMaxWidth()) { Text(st["next"]) }
                }
                else -> {
                    val take = items.firstOrNull()?.let { vm.review[it] }
                    SectionCard(color = Palette.GoldSoft) {
                        Text(st["quest_done"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(st["quest_stop_note"])
                        take?.let {
                            Text(st["take_away"], fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Korean(it.ko, size = 26)
                                IconButton(onClick = { vm.playKo("take", it.ko, it.audio) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, st["listen"]) }
                            }
                            Text(it.meaning.tr())
                        }
                    }
                    Button(onClick = { vm.go(Screen.Home) }, modifier = Modifier.fillMaxWidth()) { Text(st["stop_here"]) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ pronunciation mirror

@Composable
fun MirrorScreen(vm: AppViewModel, episodeId: String, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val ep = vm.content.episodes[episodeId] ?: return
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(st["mirror_title"], vm::back, vm.lang, vm::setLang, subtitle = ep.title.tr())
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(color = Palette.TealSoft) {
                Text(st["mirror_intro"], fontSize = 14.sp)
                Text(st["ladder_explain"], fontSize = 13.sp, color = Palette.Muted)
            }
            ep.mirrorPhrases.forEach { MirrorPhraseCard(vm, it, requestMic) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Reference audio, record, immediate A/B, and save-only-if-he-chooses. No automatic sound judgments. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MirrorPhraseCard(vm: AppViewModel, phrase: MirrorPhrase, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val key = "mirror:${phrase.id}"
    var fromMeaning by remember(phrase.id) { mutableStateOf(false) }
    var revealed by remember(phrase.id, fromMeaning) { mutableStateOf(!fromMeaning) }
    val cap = vm.captured?.takeIf { it.key == key }
    val step = vm.ladder(phrase.id)
    val synthetic = vm.audio.sourceFor(phrase.audio, vm.settings.allowSynthetic) == AudioHub.Source.SYNTHETIC
    SectionCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Speaking ladder: hear → imitate → say from meaning → use in a new scene (the next-day twist).
            val usedInScene = vm.meta.recallVariantsDone.isNotEmpty()
            listOf("ladder_hear", "ladder_imitate", "ladder_meaning", "ladder_scene").forEachIndexed { i, k ->
                val done = if (i == 3) usedInScene else step > i
                Pill((if (done) "✓ " else "") + st[k], if (done) Palette.TealSoft else MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        if (!revealed) {
            Text(phrase.gloss.tr(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(st["say_from_meaning"], fontSize = 13.sp, color = Palette.Muted)
        } else {
            Korean(phrase.ko, size = 22)
            Text(phrase.gloss.tr(), color = Palette.Muted)
        }
        Text(st["listen_for"] + " " + phrase.listenFor.tr(), fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = { vm.playKo(key, phrase.ko, phrase.audio); vm.markLadder(phrase.id, 1) }, enabled = revealed) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(18.dp)); Text(" " + st["model"])
            }
            if (vm.capturing == key) {
                Button(onClick = vm::stopCapture) { Icon(Icons.Filled.Stop, null, Modifier.size(18.dp)); Text(" " + st["stop"]) }
            } else {
                Button(onClick = {
                    vm.clearCapture()
                    vm.startCapture(key, recognize = false, requestPermission = requestMic)
                    vm.markLadder(phrase.id, if (fromMeaning) 3 else 2)
                }) { Icon(Icons.Filled.Mic, null, Modifier.size(18.dp)); Text(" " + st["record"]) }
            }
            if (synthetic) SyntheticBadge()
        }
        val take = cap?.file
        if (take != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { vm.playTake(take) }) { Text(st["play_mine"]) }
                OutlinedButton(onClick = { vm.playAB(key, phrase.ko, phrase.audio, take) }, enabled = revealed) { Text(st["ab_compare"]) }
                Button(onClick = { vm.saveCaptureToDiary("mirror", null, phrase.ko) }) { Text(st["save_take"]) }
                TextButton(onClick = { vm.clearCapture() }) { Text(st["discard"]) }
            }
        }
        if (!revealed) TextButton(onClick = { revealed = true }) { Text(st["reveal"]) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(st["from_meaning_toggle"], Modifier.weight(1f), fontSize = 13.sp)
            Switch(checked = fromMeaning, onCheckedChange = { fromMeaning = it })
        }
    }
}
