@file:OptIn(ExperimentalLayoutApi::class)

package com.doranalley.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.doranalley.ui.theme.C

/** A sentence whose words show their meaning when tapped (words with a hint get a dotted underline). */
@Composable
fun GlossText(
    text: String,
    hints: (String) -> List<String>?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 19.sp,
    color: Color = C.Ink,
    bold: Boolean = false,
    onWordTap: ((String) -> Unit)? = null,
) {
    val words = remember(text) { text.split(" ").filter { it.isNotEmpty() } }
    var open by remember(text) { mutableStateOf<Int?>(null) }
    // When a tap outside closes the hint, the same tap must not reopen it.
    var closedAt by remember { mutableStateOf(0L to -1) }
    FlowRow(modifier.testTag("prompt"), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
        words.forEachIndexed { i, w ->
            val h = remember(w) { hints(w) }
            var height by remember { mutableIntStateOf(0) }
            Box {
                Text(
                    w + if (i < words.lastIndex) " " else "",
                    fontSize = fontSize, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .onSizeChanged { height = it.height }
                        .then(
                            if (h != null) Modifier
                                .drawBehind {
                                    val y = size.height - 1.dp.toPx()
                                    val end = if (i < words.lastIndex) size.width - 6.dp.toPx() else size.width
                                    drawLine(C.OutlineStrong, Offset(0f, y), Offset(end, y), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                                }
                                .clickable {
                                    val justClosed = closedAt.second == i && System.currentTimeMillis() - closedAt.first < 400
                                    open = if (open == i || justClosed) null else i
                                    onWordTap?.invoke(w)
                                }
                                .testTag("hint:$w")
                            else Modifier
                        ),
                )
                if (open == i && h != null) {
                    Popup(alignment = Alignment.TopStart, offset = IntOffset(0, height + 8), onDismissRequest = { closedAt = System.currentTimeMillis() to i; open = null },
                        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)) {
                        Column(
                            Modifier.enterPop(from = 0.9f, durationMs = 180).surface(Level.Overlay, RoundedCornerShape(com.doranalley.ui.theme.Radius.tile))
                                .clickable { open = null }.padding(horizontal = 14.dp, vertical = 10.dp).testTag("hintPopup"),
                        ) {
                            h.forEach { line -> Text(line, style = com.doranalley.ui.theme.DoranType.body, color = C.Text) }
                        }
                    }
                }
            }
        }
    }
}
