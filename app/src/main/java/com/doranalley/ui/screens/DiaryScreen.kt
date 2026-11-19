package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.core.DiaryEntry
import com.doranalley.core.DiaryPrompt
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.theme.Palette
import com.doranalley.ui.tr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val WEEK = 7 * 86_400_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen(vm: AppViewModel, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val prompts = vm.content.config.diaryPrompts
    val baseline = vm.diary.filter { it.kind == "baseline" }.maxByOrNull { it.createdAt }
    val weeklies = vm.diary.filter { it.kind == "weekly" }.sortedBy { it.createdAt }
    val lastStory = (listOfNotNull(baseline) + weeklies).maxByOrNull { it.createdAt }
    val weeklyPrompts = prompts.filter { it.kind == "weekly" }
    val weeklyPrompt = weeklyPrompts.getOrNull(weeklies.size % weeklyPrompts.size.coerceAtLeast(1))
    val weeklyDueAt = lastStory?.createdAt?.plus(WEEK)
    var toDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(st["diary_title"], vm::back, vm.lang, vm::setLang, subtitle = st["diary_private"])
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Evidence next to the clips: what he now says on his own and how he recovers.
            SectionCard(color = Palette.TealSoft) {
                val independent = vm.independentExpressions()
                Text(st.f("independent_count", independent.size), fontWeight = FontWeight.SemiBold)
                if (independent.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    independent.forEach { Pill("${it.ko} ×${it.independentDays.size}", Palette.Paper) }
                }
                Text(st.f("clarification_count", vm.clarificationCount()))
                Text(st.f("goal_count", vm.goalsReached()))
            }

            if (baseline == null) {
                prompts.firstOrNull { it.kind == "baseline" }?.let { StoryRecorder(vm, it, "baseline", requestMic) }
            } else if (weeklyPrompt != null) {
                if (weeklyDueAt != null && weeklyDueAt <= vm.now()) StoryRecorder(vm, weeklyPrompt, "weekly", requestMic)
                else SectionCard {
                    Text(st["weekly_next"] + " " + (weeklyDueAt?.let { whenText(it, vm.now()) } ?: "—"), color = Palette.Muted)
                    var early by remember { mutableStateOf(false) }
                    if (early) StoryRecorder(vm, weeklyPrompt, "weekly", requestMic)
                    else TextButton(onClick = { early = true }) { Text(st["record_now_anyway"]) }
                }
            }

            val latest = weeklies.lastOrNull()
            if (baseline != null && latest != null) SectionCard(color = Palette.GoldSoft) {
                Text(st["side_by_side"], fontWeight = FontWeight.SemiBold)
                Text("${st["baseline"]}: ${date(baseline.createdAt)}  →  ${st["weekly"]}: ${date(latest.createdAt)}", fontSize = 13.sp)
                Button(onClick = { vm.playCompare(baseline, latest) }) { Icon(Icons.Filled.PlayArrow, null); Text(" " + st["play_both"]) }
            }

            Text(st["all_recordings"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (vm.diary.isEmpty()) Text(st["no_recordings"], color = Palette.Muted)
            vm.diary.sortedByDescending { it.createdAt }.forEach { d ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(st["kind_${d.kind}"] + " · " + date(d.createdAt), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            d.phraseKo?.let { Korean(it, size = 16, weight = FontWeight.Normal) }
                            d.promptId?.let { id -> prompts.firstOrNull { it.id == id } }?.let { Text(it.text.tr(), fontSize = 12.sp, color = Palette.Muted, maxLines = 2) }
                            Text("${d.durationMs / 1000}s", fontSize = 12.sp, color = Palette.Muted)
                        }
                        IconButton(onClick = { vm.playDiary(d) }) { Icon(Icons.Filled.PlayArrow, st["listen"], tint = Palette.Teal) }
                        IconButton(onClick = { toDelete = d }) { Icon(Icons.Filled.Delete, st["delete"], tint = Palette.Danger) }
                    }
                }
            }
        }
    }
    toDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(st["delete_title"]) },
            text = { Text(st["delete_body"]) },
            confirmButton = { TextButton(onClick = { vm.deleteDiary(d); toDelete = null }) { Text(st["delete"]) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(st["cancel"]) } },
        )
    }
}

@Composable
private fun StoryRecorder(vm: AppViewModel, prompt: DiaryPrompt, kind: String, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val key = "diary:$kind"
    val cap = vm.captured?.takeIf { it.key == key }
    SectionCard(color = Palette.PersimmonSoft) {
        Text(st[if (kind == "baseline") "baseline_title" else "weekly_title"], fontWeight = FontWeight.SemiBold)
        Text(prompt.text.tr(), fontSize = 14.sp)
        if (prompt.koStarter.isNotBlank()) Text(st["starter"] + " " + prompt.koStarter, fontSize = 14.sp, color = Palette.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (vm.capturing == key) {
                Button(onClick = vm::stopCapture) { Icon(Icons.Filled.Stop, null); Text(" " + st["stop"]) }
                Text(st["recording"], color = Palette.Persimmon)
            } else {
                Button(onClick = { vm.startCapture(key, recognize = false, requestPermission = requestMic) }) {
                    Icon(Icons.Filled.Mic, null); Text(" " + st["record"])
                }
            }
        }
        val take = cap?.file
        if (take != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.playTake(take) }) { Text(st["play_mine"]) }
            Button(onClick = { vm.saveCaptureToDiary(kind, prompt.id, null) }) { Text(st["save_take"]) }
            TextButton(onClick = { vm.clearCapture() }) { Text(st["discard"]) }
        }
    }
}

private fun date(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
