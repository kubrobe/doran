package com.doranalley.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.audio.AudioHub
import com.doranalley.core.InputMode
import com.doranalley.core.Lang
import com.doranalley.core.Node
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.theme.Palette

@Composable
fun SettingsScreen(vm: AppViewModel, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(st["settings"], vm::back, vm.lang, vm::setLang)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Text(st["interface_language"], fontWeight = FontWeight.SemiBold)
                Lang.entries.forEach { l -> Choice(l.label, vm.lang == l.code) { vm.setLang(l.code) } }
                Text(st["language_note"], fontSize = 12.sp, color = Palette.Muted)
            }
            SectionCard {
                Text(st["how_you_answer"], fontWeight = FontWeight.SemiBold)
                Choice(st["mode_auto"], vm.settings.inputMode == InputMode.AUTO) { vm.updateSettings { it.copy(inputMode = InputMode.AUTO) } }
                Choice(st["mode_record"], vm.settings.inputMode == InputMode.RECORD) { vm.updateSettings { it.copy(inputMode = InputMode.RECORD) } }
                Choice(st["mode_listen"], vm.settings.inputMode == InputMode.LISTEN) { vm.updateSettings { it.copy(inputMode = InputMode.LISTEN) } }
            }
            SectionCard {
                Toggle(st["reduced_motion"], vm.reducedMotion) { on -> vm.updateSettings { it.copy(reducedMotion = on) } }
                Toggle(st["allow_synthetic"], vm.settings.allowSynthetic) { on -> vm.updateSettings { it.copy(allowSynthetic = on) } }
                Text(st["synthetic_note"], fontSize = 12.sp, color = Palette.Muted)
            }
            SectionCard {
                Text(st["review_timing"], fontWeight = FontWeight.SemiBold)
                Text(st["review_timing_note"], fontSize = 12.sp, color = Palette.Muted)
                val current = vm.reviewConfig.intervalsHours
                val labels = listOf("timing_1", "timing_2", "timing_3", "timing_4")
                labels.forEachIndexed { i, k ->
                    var text by remember(current) { mutableStateOf(current.getOrNull(i)?.let { trim(it) } ?: "") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st[k], Modifier.weight(1f), fontSize = 14.sp)
                        OutlinedTextField(
                            value = text, singleLine = true, modifier = Modifier.width(110.dp),
                            suffix = { Text(st["hours_short"]) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            onValueChange = { v ->
                                text = v
                                v.toDoubleOrNull()?.takeIf { it > 0 }?.let { h ->
                                    val list = current.toMutableList()
                                    while (list.size <= i) list.add(h)
                                    list[i] = h
                                    vm.updateSettings { it.copy(intervalsHours = list) }
                                }
                            },
                        )
                    }
                }
                TextButton(onClick = { vm.updateSettings { it.copy(intervalsHours = null) } }) { Text(st["reset_default"]) }
            }
            SectionCard {
                Text(st["device_check"], fontWeight = FontWeight.SemiBold)
                Status(st["korean_voice"], when (vm.audio.ttsState) {
                    AudioHub.TtsState.READY -> st["ok"] + (vm.audio.ttsEngine?.let { " · $it" } ?: "")
                    AudioHub.TtsState.LOADING -> st["checking"]
                    AudioHub.TtsState.MISSING -> st["missing"]
                }, vm.audio.ttsState == AudioHub.TtsState.READY)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.playKo("test_voice", "안녕하세요. 강의실이 바뀌었어요.") }) { Text(st["test_voice"]) }
                    if (vm.audio.ttsState == AudioHub.TtsState.MISSING) TextButton(onClick = vm.audio::retryTts) { Text(st["check_again"]) }
                }
                if (vm.audio.ttsState == AudioHub.TtsState.MISSING) {
                    Text(st["voice_missing_note"], fontSize = 12.sp, color = Palette.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runCatching { ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .onFailure { openTtsSettings(ctx) }
                        }) { Text(st["install_voice"]) }
                        TextButton(onClick = { openTtsSettings(ctx) }) { Text(st["tts_settings"]) }
                    }
                }
                Status(st["microphone"], if (vm.micGranted) st["ok"] else st["off_listening"], vm.micGranted)
                if (!vm.micGranted) OutlinedButton(onClick = requestMic) { Text(st["allow_mic"]) }
                else DeviceTest(vm, "mictest", st["test_mic"], recognize = false, requestMic = requestMic)
                val recOk = (vm.recognizer.isAvailable() || vm.canRecognize()) && !vm.recognitionBroken
                Status(st["recognition"], if (recOk) st["ok"] + " · " + vm.recognizer.serviceName() else st["unavailable_manual"], recOk)
                if (vm.micGranted) DeviceTest(vm, "rectest", st["test_recognition"], recognize = true, requestMic = requestMic)
                if (vm.recognitionBroken) TextButton(onClick = vm::resetRecognition) { Text(st["try_recognition_again"]) }
                val lines = vm.content.episodes.values.flatMap { it.nodes.values }.filterIsInstance<Node.Line>().filter { it.audio != null }
                val bundled = lines.count { vm.audio.hasBundled(it.audio) }
                Status(st["recordings"], "$bundled / ${lines.size}", bundled == lines.size && lines.isNotEmpty())
                Text(st["review_status_note"], fontSize = 12.sp, color = Palette.Muted)
            }
            SectionCard(color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(st["test_tools"], fontWeight = FontWeight.SemiBold)
                Text(st.f("clock_offset", vm.settings.timeOffsetHours), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.shiftTime(24) }) { Text(st["plus_day"]) }
                    OutlinedButton(onClick = { vm.shiftTime(4) }) { Text(st["plus_4h"]) }
                    TextButton(onClick = vm::resetTime) { Text(st["reset_clock"]) }
                }
                Button(onClick = { confirmReset = true }) { Text(st["reset_progress"]) }
            }
            Text(st["privacy_note"], fontSize = 12.sp, color = Palette.Muted)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(st["reset_progress"]) },
        text = { Text(st["reset_body"]) },
        confirmButton = { TextButton(onClick = { vm.resetProgress(keepDiary = true); confirmReset = false }) { Text(st["reset_keep_diary"]) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(st["cancel"]) } },
    )
}

private fun openTtsSettings(ctx: android.content.Context) {
    runCatching { ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

/** Record a few seconds and play it back, or say something and see what the recognizer heard. */
@Composable
private fun DeviceTest(vm: AppViewModel, key: String, label: String, recognize: Boolean, requestMic: () -> Unit) {
    val st = LocalStrings.current
    val cap = vm.captured?.takeIf { it.key == key }
    if (vm.capturing == key) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::stopCapture) { Text(st["stop"]) }
            Column(Modifier.weight(1f)) {
                LevelMeter(if (vm.recognizer.listening) vm.recognizer.level else vm.recorder.level)
                if (vm.recognizer.partial.isNotBlank()) Korean(vm.recognizer.partial, size = 15, weight = FontWeight.Normal)
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.clearCapture(); vm.startCapture(key, recognize, requestMic) }) { Text(label) }
            cap?.file?.let { f -> TextButton(onClick = { vm.playTake(f) }) { Text(st["play_mine"]) } }
        }
        cap?.alternatives?.firstOrNull()?.let { Text(st["app_heard"] + " «$it»", fontSize = 13.sp) }
    }
}

private fun trim(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else d.toString()

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun Status(label: String, value: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(if (ok) Palette.Teal else Palette.Gold)
        Text("  $label", Modifier.weight(1f), fontSize = 14.sp)
        Text(value, fontSize = 13.sp, color = Palette.Muted)
    }
}
