package com.doranalley.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.score.Acoustic
import com.doranalley.score.Hangul
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.Rating
import com.doranalley.ui.theme.Palette

private fun isSyllable(c: Char) = c in '가'..'힣'

fun scoreColor(score: Int): Color = when {
    score >= 75 -> Palette.Teal
    score >= 50 -> Palette.Gold
    else -> Palette.Persimmon
}

/** Live input level, so he can see the microphone is actually hearing him. */
@Composable
fun LevelMeter(level: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { level.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp),
        color = if (level > 0.08f) Palette.Teal else Palette.Muted,
        trackColor = Palette.Line,
    )
}

/** Pronunciation feedback for a capture key: sound match and/or what the recognizer understood. */
@Composable
fun RatingPanel(vm: AppViewModel, key: String) {
    val r: Rating = vm.ratings[key] ?: return
    val st = LocalStrings.current
    Surface(color = Palette.Paper, shape = MaterialTheme.shapes.small, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                r.soundBusy -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(st["rating_busy"], fontSize = 14.sp, color = Palette.Muted)
                }
                r.noModel -> Text(st["rating_no_model"], fontSize = 13.sp, color = Palette.Muted)
                r.sound != null -> SoundResult(r.target.ko, r.sound)
            }
            r.text?.let { TextResult(it) }
        }
    }
}

@Composable
private fun SoundResult(ko: String, res: Acoustic.Result) {
    val st = LocalStrings.current
    if (res.noSpeech()) {
        Text(st["rating_no_speech"], fontSize = 14.sp, color = Palette.Persimmon)
        return
    }
    ScoreRow(st["sound_match"], res.score)
    // Colour each syllable of the phrase by how closely that part matched (approximate).
    val syl = res.syllables
    if (syl != null && syl.isNotEmpty()) {
        val marks = syl.map { if (it >= 0.7) 2 else if (it >= 0.45) 1 else 0 }
        MarkedPhrase(ko, marks)
    }
    val tips = buildList {
        if (res.tooQuiet()) add(st["tip_too_quiet"])
        if (res.tooFast()) add(st["tip_too_fast"])
        if (res.tooSlow()) add(st["tip_too_slow"])
        val w = res.weakest()
        if (w >= 0) {
            val chars = ko.filter(::isSyllable)
            chars.getOrNull(w)?.let { add(st.f("tip_weakest", it)) }
        }
        if (isEmpty() && res.score >= 80) add(st["tip_great"])
        if (isEmpty()) add(st["tip_again"])
    }
    tips.forEach { Text("• $it", fontSize = 14.sp) }
    Text(st["sound_note"], fontSize = 11.sp, color = Palette.Muted, lineHeight = 14.sp)
}

@Composable
private fun TextResult(res: Hangul.Result) {
    val st = LocalStrings.current
    ScoreRow(st["understood_score"], res.score)
    MarkedPhrase(res.target, res.marks.toList(), keepAll = true)
    Text(st.f("heard_as", res.heard), fontSize = 13.sp, color = Palette.Muted)
    Text(st["understood_note"], fontSize = 11.sp, color = Palette.Muted, lineHeight = 14.sp)
}

@Composable
private fun ScoreRow(label: String, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("$score", color = scoreColor(score), fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text(" / 100", color = Palette.Muted, fontSize = 13.sp)
    }
    LinearProgressIndicator(
        progress = { score / 100f },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = scoreColor(score),
        trackColor = Palette.Line,
    )
}

/**
 * Shows the phrase with each syllable coloured: matched (teal), close (gold), missed (persimmon, underlined).
 * [marks] has one entry per Hangul syllable (or per character with [keepAll]).
 */
@Composable
fun MarkedPhrase(text: String, marks: List<Int>, keepAll: Boolean = false) {
    val st = LocalStrings.current
    val annotated = buildAnnotatedString {
        var k = 0
        for (c in text) {
            val counted = keepAll || isSyllable(c)
            val m = if (counted) marks.getOrNull(k++) else null
            when (m) {
                2 -> withStyle(SpanStyle(color = Palette.Teal)) { append(c) }
                1 -> withStyle(SpanStyle(color = Palette.Gold)) { append(c) }
                0 -> withStyle(SpanStyle(color = Palette.Persimmon, textDecoration = TextDecoration.Underline)) { append(c) }
                else -> append(c)
            }
        }
    }
    Text(annotated, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Legend(Palette.Teal, st["mark_good"])
        Legend(Palette.Gold, st["mark_close"])
        Legend(Palette.Persimmon, st["mark_missed"])
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Text(" $label", fontSize = 11.sp, color = Palette.Muted)
        androidx.compose.foundation.layout.Spacer(Modifier.width(2.dp))
    }
}
