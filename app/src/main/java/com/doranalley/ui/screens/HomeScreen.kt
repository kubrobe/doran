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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.Screen
import com.doranalley.ui.scene.CampusScene
import com.doranalley.ui.theme.Palette
import com.doranalley.ui.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val st = LocalStrings.current
    val c = vm.content
    val now = vm.now()
    val due = vm.dueItems()
    val unlockedCharacters = c.characters.values.filter { it.unlockedByDefault || "character:${it.id}" in vm.meta.unlocks }
    val streetCast = buildMap {
        put("learner", "street_inmun")
        put("minji", "street_inmun")
        if ("character:seoyeon" in vm.meta.unlocks) put("seoyeon", "cafe")
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = st["app_name"], subtitle = "${c.campus.nameKo} · ${c.campus.neighborhood.tr()}",
            onBack = null, lang = vm.lang, onLang = vm::setLang,
        )
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CampusScene(
                campus = c.campus, characters = c.characters, positions = streetCast, speaker = null, speakerPose = "idle",
                learnerTalking = false, reducedMotion = vm.reducedMotion, description = c.campus.name.tr(),
                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)),
            )

            // Resume where he left off.
            vm.meta.activeEpisode?.let { id -> c.episodes[id] }?.let { ep ->
                SectionCard(color = Palette.PersimmonSoft) {
                    Text(st["continue_where"], fontWeight = FontWeight.SemiBold)
                    Text(ep.title.tr() + (ep.variantLabel.takeIf { it.isNotEmpty() }?.let { " · " + it.tr() } ?: ""))
                    Button(onClick = { vm.openEpisode(ep.id) }) { Text(st["continue"]) }
                }
            }

            if (vm.diary.none { it.kind == "baseline" }) {
                SectionCard(color = Palette.GoldSoft) {
                    Text(st["start_here"], fontWeight = FontWeight.SemiBold)
                    Text(st["baseline_invite"], fontSize = 14.sp)
                    OutlinedButton(onClick = { vm.go(Screen.Diary) }) { Text(st["record_baseline"]) }
                }
            }

            Text(st["stories"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            c.index.forEach { entry ->
                val playable = entry.status == "playable" && c.episodes.containsKey(entry.id)
                val done = entry.id in vm.meta.completed
                SectionCard(color = if (playable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(entry.setting.tr(), if (playable) Palette.TealSoft else Color(0x11000000))
                        Spacer(Modifier.width(8.dp))
                        if (done) Pill("✓ " + st["completed"], Palette.GoldSoft)
                        if (!playable) Pill(st["coming_soon"], Color(0x11000000))
                    }
                    Text(entry.title.tr(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = if (playable) Palette.Ink else Palette.Muted)
                    Text(entry.summary.tr(), fontSize = 14.sp, color = Palette.Muted)
                    if (playable) {
                        val inProgress = vm.meta.activeEpisode == entry.id
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.openEpisode(entry.id, restart = !inProgress) }) {
                                Text(if (inProgress) st["continue"] else if (done) st["play_again"] else st["play"])
                            }
                            if (done) OutlinedButton(onClick = { vm.go(Screen.Mirror(entry.id)) }) { Text(st["mirror_short"]) }
                        }
                    }
                }
            }

            Text(st["practice"], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SectionCard(color = if (due.isNotEmpty()) Palette.TealSoft else MaterialTheme.colorScheme.surface) {
                Text(st["recall_title"], fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        due.isNotEmpty() -> st.f("due_count", due.size)
                        vm.review.isEmpty() -> st["nothing_learned_yet"]
                        else -> st["next_review"] + " " + (vm.nextDue()?.let { whenText(it.nextReview, now) } ?: "—")
                    }, fontSize = 14.sp,
                )
                Button(onClick = { vm.go(Screen.Recall) }, enabled = vm.review.isNotEmpty() || vm.meta.completed.isNotEmpty()) { Text(st["open"]) }
            }
            SectionCard {
                Text(st["quest_title"], fontWeight = FontWeight.SemiBold)
                Text(st.f("quest_desc", c.config.sideQuestMinutes), fontSize = 14.sp)
                Button(onClick = { vm.go(Screen.Quest) }, enabled = vm.review.isNotEmpty()) { Text(st["start"]) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCard(Modifier.weight(1f)) {
                    Text(st["mirror_title"], fontWeight = FontWeight.SemiBold)
                    Text(st["mirror_desc"], fontSize = 13.sp, color = Palette.Muted)
                    OutlinedButton(onClick = { c.mainEpisodes().firstOrNull()?.let { vm.go(Screen.Mirror(it.id)) } }) { Text(st["open"]) }
                }
                SectionCard(Modifier.weight(1f)) {
                    Text(st["diary_title"], fontWeight = FontWeight.SemiBold)
                    Text(st["diary_desc"], fontSize = 13.sp, color = Palette.Muted)
                    OutlinedButton(onClick = { vm.go(Screen.Diary) }) { Text(st["open"]) }
                }
            }

            SectionCard {
                Text(st["daily_rhythm"], fontWeight = FontWeight.SemiBold)
                Text(st["daily_rhythm_note"], fontSize = 13.sp, color = Palette.Muted)
                c.config.daily.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${b.minutes}′", Modifier.width(44.dp), fontWeight = FontWeight.Bold, color = Palette.Persimmon)
                        Text(b.label.tr(), Modifier.weight(1f))
                        val target: Screen? = when (b.action) {
                            "recall" -> Screen.Recall
                            "episode" -> c.mainEpisodes().firstOrNull()?.let { Screen.Play(it.id) }
                            "mirror" -> c.mainEpisodes().firstOrNull()?.let { Screen.Mirror(it.id) }
                            "diary" -> Screen.Diary
                            else -> null
                        }
                        if (target != null) TextButton(onClick = {
                            if (target is Screen.Play) vm.openEpisode(target.episodeId) else vm.go(target)
                        }) { Text(st["go"]) } else Text(st["coming_soon"], fontSize = 12.sp, color = Palette.Muted)
                    }
                }
                Text(st["more_today"], fontSize = 13.sp, color = Palette.Muted)
            }

            SectionCard {
                Text(st["cast"], fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.characters.values.forEach { ch ->
                        val open = ch in unlockedCharacters
                        Pill(if (open) "${ch.nameKo} · ${ch.role.tr()}" else "? · ${ch.role.tr()}",
                            if (open) Color(ch.color).copy(alpha = 0.2f) else Color(0x11000000))
                    }
                }
                vm.meta.unlocks.filter { it.startsWith("location:") }.forEach { Text("✦ " + unlockName(vm, it), fontSize = 13.sp) }
            }

            TextButton(onClick = { vm.go(Screen.Settings) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Filled.Settings, null); Text(" " + st["settings"])
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
