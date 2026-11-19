package com.doranalley.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doranalley.core.Evidence
import com.doranalley.core.Help
import com.doranalley.core.SUPPORTED_LANGS
import com.doranalley.ui.LocalStrings
import com.doranalley.ui.s
import com.doranalley.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, lang: String, onLang: (String) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s("back")) }
        } else Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LangSwitch(lang, onLang)
    }
}

/** UZ · RU · EN — always one tap away; switching keeps the learner's place. */
@Composable
fun LangSwitch(lang: String, onLang: (String) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(2.dp),
    ) {
        for (code in SUPPORTED_LANGS) {
            val sel = code == lang
            Surface(
                onClick = { onLang(code) },
                shape = RoundedCornerShape(50),
                color = if (sel) Palette.Teal else Color.Transparent,
                modifier = Modifier.padding(1.dp),
            ) {
                Text(
                    code.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (sel) Color.White else Palette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color = Palette.Ink) {
    Text(
        text, Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
    )
}

/** Honest label for how the app knows what he said. */
@Composable
fun EvidencePill(code: String?) {
    when (Evidence.of(code)) {
        Evidence.RECOGNIZED -> Pill(s("ev_recognized"), Palette.TealSoft)
        Evidence.SELF_CONFIRMED -> Pill(s("ev_self"), Palette.GoldSoft)
        Evidence.LISTENING_ONLY -> Pill(s("ev_listening"), MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun HelpPill(help: Int) {
    when {
        help >= Help.MODEL -> Pill(s("help_model"), Palette.PersimmonSoft)
        help > Help.NONE -> Pill(s("help_hint"), Palette.GoldSoft)
        else -> {}
    }
}

@Composable
fun SyntheticBadge() = Pill(s("synthetic_preview"), Palette.GoldSoft)

@Composable
fun Korean(text: String, size: Int = 20, weight: FontWeight = FontWeight.SemiBold, color: Color = Palette.Ink) =
    Text(text, fontSize = size.sp, fontWeight = weight, color = color, lineHeight = (size * 1.35f).sp)

@Composable
fun Gap(h: Int = 8) = Spacer(Modifier.height(h.dp))

@Composable
fun Dot(color: Color) = Spacer(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))

/** "today 18:30", "tomorrow 09:10", "in 3 days". */
@Composable
fun whenText(at: Long, now: Long): String {
    val st = LocalStrings.current
    val zone = ZoneId.systemDefault()
    val d0 = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val d1 = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
    val time = Instant.ofEpochMilli(at).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    val days = ChronoUnit.DAYS.between(d0, d1)
    return when {
        at <= now -> st["due_now"]
        days == 0L -> st.f("today_at", time)
        days == 1L -> st.f("tomorrow_at", time)
        else -> st.f("in_days", days)
    }
}
