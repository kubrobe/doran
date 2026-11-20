package com.doranalley.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.doranalley.core.CourseUnit
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.Tab
import com.doranalley.ui.components.CardBox
import com.doranalley.ui.components.ChunkyButton
import com.doranalley.ui.components.Mascot
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.SectionTitle
import com.doranalley.ui.components.SpeakerButton
import com.doranalley.ui.components.StatChip
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.Nunito
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen(vm: AppViewModel, tab: Tab) {
    val p by vm.progress.collectAsState()
    val now by vm.now.collectAsState()
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        TopStats(vm, p, now)
        Box(Modifier.weight(1f)) {
            key(tab) {
                Box(Modifier.fillMaxSize().enterRise(distance = 10.dp, durationMs = 260)) {
                    when (tab) {
                        Tab.LEARN -> PathView(vm, p)
                        Tab.PRACTICE -> PracticeTab(vm)
                        Tab.WORDS -> WordsTab(vm, p)
                        Tab.PROFILE -> ProfileTab(vm, p)
                    }
                }
            }
        }
        Column(Modifier.background(C.Card)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(C.Line))
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                NavItem(Icons.Filled.Home, "Learn", tab == Tab.LEARN, "tabLearn") { vm.go(Screen.Home(Tab.LEARN)) }
                NavItem(Icons.Filled.FitnessCenter, "Practice", tab == Tab.PRACTICE, "tabPractice") { vm.go(Screen.Home(Tab.PRACTICE)) }
                NavItem(Icons.AutoMirrored.Filled.MenuBook, "Words", tab == Tab.WORDS, "tabWords") { vm.go(Screen.Home(Tab.WORDS)) }
                NavItem(Icons.Filled.Person, "Profile", tab == Tab.PROFILE, "tabProfile") { vm.go(Screen.Home(Tab.PROFILE)) }
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.GreenLight else Color.Transparent, tween(200), label = "navBg")
    val fg by animateColorAsState(if (selected) C.GreenDark else C.Muted, tween(200), label = "navFg")
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "navScale")
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(26.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        Text(label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito)
    }
}

@Composable
private fun TopStats(vm: AppViewModel, p: Progress, now: Long) {
    val today = vm.today()
    var heartsInfo by remember { mutableStateOf(false) }
    Column(Modifier.background(C.Card)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("도란", fontSize = 22.sp, fontWeight = FontWeight.Black, color = C.Ink, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.weight(1f))
            val streak = p.visibleStreak(today)
            StatChip(Icons.Filled.LocalFireDepartment, "$streak", if (p.extendedToday(today)) C.Orange else C.Muted, tag = "streak")
            Spacer(Modifier.width(6.dp))
            StatChip(Icons.Filled.Star, "${p.xpOn(today)}/${p.dailyGoal}", C.Gold, tag = "dailyXp")
            Spacer(Modifier.width(6.dp))
            StatChip(Icons.Filled.Favorite, if (p.settings.unlimitedHearts) "∞" else "${p.heartsNow(now)}", C.Red, onClick = { heartsInfo = true }, tag = "heartsChip")
        }
        // Daily goal as a thin bar under the stats.
        ProgressLine(p.xpOn(today).toFloat() / p.dailyGoal.coerceAtLeast(1))
    }
    if (heartsInfo) HeartsDialog(vm, p, now) { heartsInfo = false }
}

@Composable
private fun ProgressLine(fraction: Float) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 120f), label = "goal")
    Box(Modifier.fillMaxWidth().height(3.dp).background(C.Line)) {
        if (f > 0.001f) Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(3.dp).background(C.Gold))
    }
}

@Composable
fun HeartsDialog(vm: AppViewModel, p: Progress, now: Long, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.enterPop(from = 0.85f, durationMs = 320).clip(RoundedCornerShape(24.dp)).background(C.Card).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hearts", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.padding(vertical = 12.dp)) {
                repeat(Progress.MAX_HEARTS) { i ->
                    Icon(
                        Icons.Filled.Favorite, null, tint = if (i < p.heartsNow(now)) C.Red else C.Line,
                        modifier = Modifier.size(34.dp).enterPop(delayMs = i * 60, from = 0.3f),
                    )
                }
            }
            val next = p.nextHeartIn(now)
            Text(
                when {
                    p.settings.unlimitedHearts -> "Unlimited hearts are on (Settings)."
                    next == 0L -> "You have full hearts. You lose one for each mistake in a lesson."
                    else -> "Next heart in ${(next / 60000) + 1} min. Finish a practice session to earn one now."
                },
                textAlign = TextAlign.Center, color = C.Ink,
            )
            Spacer(Modifier.height(16.dp))
            ChunkyButton("Practice to earn hearts", { onDismiss(); vm.startPractice(com.doranalley.core.LessonBuilder.PracticeMode.MIXED) }, Modifier.fillMaxWidth(), color = C.Blue, edge = C.BlueDark)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.ExtraBold, color = C.Muted) }
        }
    }
}

// ------------------------------------------------------------------ path

private sealed interface PathRow {
    data class Header(val unit: CourseUnit, val sectionStart: Boolean) : PathRow
    data class Node(val unit: CourseUnit, val lesson: Int, val idx: Int) : PathRow
}

/** Height of the band each lesson node sits in; the node's centre is always half of this above the row's bottom. */
private val RowH = 96.dp
private val NodeSize = 72.dp

/** Horizontal offset of node [idx]: the path winds left and right like an alley. */
private fun nodeX(idx: Int): Float = (sin(idx * 0.9) * 70).toFloat()

@Composable
private fun PathView(vm: AppViewModel, p: Progress) {
    val course = vm.course
    val rows = remember(course) {
        buildList {
            var lastSection = ""
            var idx = 0
            course.units.forEach { u ->
                add(PathRow.Header(u, u.section != lastSection)); lastSection = u.section
                repeat(u.lessonCount) { add(PathRow.Node(u, it, idx++)) }
            }
        }
    }
    val cur = p.current(course)
    fun isCurrent(r: PathRow?) = r is PathRow.Node && cur != null && r.unit.id == cur.first.id && r.lesson == cur.second
    fun topPad(i: Int): Dp {
        val r = rows.getOrNull(i) as? PathRow.Node ?: return 0.dp
        val first = rows.getOrNull(i - 1) !is PathRow.Node
        return (if (first) 18.dp else 0.dp) + (if (isCurrent(r)) 44.dp else 0.dp)
    }
    val currentRow = rows.indexOfFirst { isCurrent(it) }
    val state = rememberLazyListState()
    LaunchedEffect(currentRow) { if (currentRow > 2) state.scrollToItem((currentRow - 2).coerceAtLeast(0)) }
    var open by remember { mutableStateOf<PathRow.Node?>(null) }

    LazyColumn(state = state, modifier = Modifier.fillMaxSize().testTag("path")) {
        items(rows.size) { i ->
            when (val r = rows[i]) {
                is PathRow.Header -> UnitHeader(r.unit, r.sectionStart, p.lessonsDone(r.unit)) { vm.go(Screen.Guidebook(r.unit)) }
                is PathRow.Node -> {
                    val prev = rows.getOrNull(i - 1) as? PathRow.Node
                    val next = rows.getOrNull(i + 1) as? PathRow.Node
                    val done = p.lessonsDone(r.unit) > r.lesson
                    val unlocked = p.isUnlocked(course, r.unit, r.lesson)
                    PathNodeRow(
                        r, top = topPad(i), nextTop = topPad(i + 1),
                        hasPrev = prev != null, hasNext = next != null,
                        prevDone = prev != null && p.lessonsDone(prev.unit) > prev.lesson,
                        done = done, current = isCurrent(r), unlocked = unlocked,
                    ) { open = r }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(mood = if (cur == null) Mood.CHEER else Mood.THINK, size = 110.dp)
                Text(if (cur == null) "You finished the whole course! 정말 잘했어요!" else "More units are on the way.", color = C.Muted, textAlign = TextAlign.Center)
            }
        }
    }

    open?.let { node ->
        val done = p.lessonsDone(node.unit) > node.lesson
        val unlocked = p.isUnlocked(course, node.unit, node.lesson)
        LessonPopup(node.unit, node.lesson, done, unlocked, onStart = { open = null; vm.startLesson(node.unit, node.lesson) }, onDismiss = { open = null })
    }
}

/** A big character from the unit (a letter or the first syllable of its first word) used as a watermark. */
private fun watermark(u: CourseUnit): String =
    u.letters.firstOrNull()?.ch ?: u.words.firstOrNull()?.ko?.firstOrNull()?.toString() ?: "한"

@Composable
private fun UnitHeader(u: CourseUnit, sectionStart: Boolean, done: Int, onGuide: () -> Unit) {
    val (col, edge) = C.section(u.section)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = if (sectionStart) 22.dp else 30.dp, bottom = 10.dp)) {
        if (sectionStart) SectionTitle(u.section, Modifier.padding(bottom = 10.dp, start = 4.dp), color = edge)
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.fillMaxWidth().clip(shape).background(edge).padding(bottom = 5.dp).clip(shape).background(col)) {
            Text(
                watermark(u), fontSize = 118.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-58).dp, y = 14.dp).graphicsLayer { rotationZ = -10f },
            )
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Unit ${u.number}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = Nunito)
                    Text(u.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, fontFamily = Nunito, lineHeight = 26.sp)
                    Text(u.blurb, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, fontFamily = Nunito, lineHeight = 18.sp)
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val f by animateFloatAsState(done.toFloat() / u.lessonCount, tween(600), label = "unitProgress")
                        Box(Modifier.width(110.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                            if (f > 0.001f) Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(Color.White))
                        }
                        Text("$done/${u.lessonCount}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = Nunito, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f))
                        .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onGuide).padding(10.dp).testTag("guide:${u.id}"),
                ) { Icon(Icons.AutoMirrored.Filled.MenuBook, "Guidebook", tint = Color.White) }
            }
        }
    }
}

private fun trail(x1: Float, y1: Float, x2: Float, y2: Float): Path = Path().apply {
    val dy = (y2 - y1) / 2f
    moveTo(x1, y1)
    cubicTo(x1, y1 + dy, x2, y2 - dy, x2, y2)
}

@Composable
private fun PathNodeRow(
    r: PathRow.Node,
    top: Dp,
    nextTop: Dp,
    hasPrev: Boolean,
    hasNext: Boolean,
    prevDone: Boolean,
    done: Boolean,
    current: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    val u = r.unit
    val (col, edge) = C.section(u.section)
    val isReview = r.lesson == u.lessonCount - 1
    val x = nodeX(r.idx)
    val xp = nodeX(r.idx - 1)
    val xn = nodeX(r.idx + 1)
    val walked = col.copy(alpha = 0.55f)
    Box(
        Modifier.fillMaxWidth().height(top + RowH).drawBehind {
            // Dotted stepping stones between nodes. Each row draws both neighbouring curves clipped to
            // itself, so the trail is continuous and never covers another row's node.
            val cx = size.width / 2f
            val half = RowH.toPx() / 2f
            val cy = size.height - half
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 18.dp.toPx())))
            clipRect {
                if (hasPrev) drawPath(trail(cx + xp.dp.toPx(), -half, cx + x.dp.toPx(), cy), if (prevDone) walked else C.LineDark, style = stroke)
                if (hasNext) drawPath(trail(cx + x.dp.toPx(), cy, cx + xn.dp.toPx(), size.height + nextTop.toPx() + half), if (done) walked else C.LineDark, style = stroke)
            }
        },
    ) {
        Box(Modifier.align(Alignment.BottomCenter).offset(x = x.dp).padding(bottom = (RowH - 92.dp) / 2)) {
            val icon = when { done -> Icons.Filled.Check; !unlocked -> Icons.Filled.Lock; isReview -> Icons.Filled.EmojiEvents; u.isHangul -> Icons.Filled.Translate; else -> Icons.Filled.Star }
            val fill = when { done -> C.Gold; unlocked -> col; else -> C.Line }
            val rim = when { done -> C.GoldDark; unlocked -> edge; else -> C.LineDark }
            NodeButton(icon, fill, rim, if (unlocked || done) Color.White else C.Muted, current, col, "node:${u.id}:${r.lesson}", onClick)
            if (current) StartTag(col, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun NodeButton(icon: ImageVector, fill: Color, rim: Color, tint: Color, current: Boolean, col: Color, tag: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) 6.dp else 0.dp, spring(stiffness = Spring.StiffnessHigh), label = "sink")
    val halo = if (current) {
        val t = rememberInfiniteTransition(label = "halo")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "h").value
    } else -1f
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        if (current) {
            Canvas(Modifier.size(92.dp)) {
                val rr = size.minDimension / 2f
                drawCircle(col.copy(alpha = 0.22f * (1f - halo)), rr * (0.8f + 0.2f * halo))
                drawCircle(C.Line, rr - 4.dp.toPx(), style = Stroke(6.dp.toPx()))
                drawArc(col, -90f, 360f, false, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Box(
            Modifier.size(NodeSize).clip(CircleShape)
                .clickable(source, null, onClick = onClick).testTag(tag)
                .padding(top = sink).background(rim, CircleShape)
                .padding(bottom = (7.dp - sink).coerceAtLeast(0.dp)).background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 15.dp, top = 11.dp).size(width = 14.dp, height = 8.dp).graphicsLayer { rotationZ = -35f }.clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
            Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp))
        }
    }
}

private val TagArrow = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
}

/** The bouncing "Start" tag above the current lesson. */
@Composable
private fun StartTag(col: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "tag")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Column(
        modifier.offset { IntOffset(0, (-(46.dp.toPx()) - bob * 5.dp.toPx()).roundToInt()) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Start", color = col, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = Nunito,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.Card).border(2.dp, C.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        )
        Box(Modifier.offset(y = (-2).dp).size(width = 14.dp, height = 8.dp).background(C.Line, TagArrow))
    }
}

@Composable
private fun LessonPopup(u: CourseUnit, lesson: Int, done: Boolean, unlocked: Boolean, onStart: () -> Unit, onDismiss: () -> Unit) {
    val (col, edge) = C.section(u.section)
    val isReview = lesson == u.lessonCount - 1
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            Modifier.enterPop(from = 0.85f, durationMs = 340).clip(shape).background(if (unlocked) edge else C.LineDark).padding(bottom = 5.dp)
                .clip(shape).background(if (unlocked) col else C.Card).testTag("lessonPopup"),
        ) {
            Text(
                watermark(u), fontSize = 96.sp, fontWeight = FontWeight.Black,
                color = (if (unlocked) Color.White else C.Ink).copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-8).dp),
            )
            Column(Modifier.padding(20.dp)) {
                val fg = if (unlocked) Color.White else C.Ink
                Text(u.title, color = fg, fontWeight = FontWeight.Black, fontSize = 22.sp, fontFamily = Nunito)
                Text(
                    if (isReview) "Unit review" else "Lesson ${lesson + 1} of ${u.lessonCount - 1}",
                    color = fg.copy(alpha = 0.85f), fontFamily = Nunito, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                // One dot per node in the unit.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    repeat(u.lessonCount) { i ->
                        val on = i == lesson
                        Box(Modifier.size(width = if (on) 22.dp else 8.dp, height = 8.dp).clip(CircleShape)
                            .background(if (unlocked) Color.White.copy(alpha = if (on) 1f else 0.35f) else if (on) C.Muted else C.Line))
                    }
                }
                if (unlocked) {
                    ChunkyButton(
                        if (done) "Practice again  +10 XP" else if (isReview) "Start review  +15 XP" else "Start  +10 XP",
                        onStart, Modifier.fillMaxWidth(), color = C.Card, edge = C.Line, textColor = edge, tag = "startLesson",
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, tint = C.Muted, modifier = Modifier.size(20.dp))
                        Text("Finish the lessons above to open this one.", color = C.Muted, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ guidebook

@Composable
fun GuidebookScreen(vm: AppViewModel, u: CourseUnit) {
    val playing by vm.audio.playing.collectAsState()
    val (col, edge) = C.section(u.section)
    androidx.activity.compose.BackHandler { vm.back() }
    Column(Modifier.fillMaxSize().background(C.Bg).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().background(col)) {
            Text(
                watermark(u), fontSize = 150.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = 30.dp),
            )
            Column(Modifier.statusBarsPadding().padding(start = 8.dp, end = 20.dp, bottom = 20.dp)) {
                androidx.compose.material3.IconButton(onClick = { vm.back() }, modifier = Modifier.testTag("back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Unit ${u.number} guidebook", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, fontFamily = Nunito)
                    Text(u.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = Nunito, lineHeight = 32.sp)
                    Text(u.blurb, color = Color.White.copy(alpha = 0.92f), fontFamily = Nunito)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(5.dp).background(edge))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item { SectionTitle("Key points", Modifier.padding(top = 20.dp, bottom = 10.dp)) }
            items(u.tips) { t ->
                CardBox(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(Modifier.padding(14.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(col))
                        Text(t, fontSize = 16.sp, color = C.Ink, modifier = Modifier.padding(start = 12.dp), lineHeight = 22.sp)
                    }
                }
            }
            if (u.letters.isNotEmpty()) {
                item { SectionTitle("Letters", Modifier.padding(top = 20.dp, bottom = 6.dp)) }
                items(u.letters) { l -> GuideRow(l.ch, "${l.rom} · ${l.hint}", playing == "ko:${l.say}") { vm.audio.play(l.say) } }
            }
            item { SectionTitle("Words", Modifier.padding(top = 20.dp, bottom = 6.dp)) }
            items(u.words) { w -> GuideRow(w.ko, w.en, playing == "ko:${w.ko}") { vm.audio.play(w.ko) } }
            if (u.sentences.isNotEmpty()) {
                item { SectionTitle("Phrases", Modifier.padding(top = 20.dp, bottom = 6.dp)) }
                items(u.sentences) { s -> GuideRow(s.ko, s.en, playing == "ko:${s.ko}") { vm.audio.play(s.ko) } }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun GuideRow(ko: String, en: String, playing: Boolean, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onPlay).padding(vertical = 8.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        SpeakerButton(playing, onPlay, size = 42.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(ko, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            Text(en, fontSize = 14.sp, color = C.Muted)
        }
    }
}
