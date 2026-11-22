package com.doranalley.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.doranalley.ui.components.DancheongBand
import com.doranalley.ui.components.darken
import com.doranalley.ui.components.drawOrb
import com.doranalley.ui.components.glass
import com.doranalley.ui.components.jewel
import com.doranalley.ui.components.lighten
import com.doranalley.ui.theme.HangulSerif
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.doranalley.ui.components.ButtonVariant
import com.doranalley.ui.components.DoranButton
import com.doranalley.ui.components.DoranDialog
import com.doranalley.ui.components.HairlineDivider
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.SealMark
import com.doranalley.ui.components.StatPill
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Space
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
    // The tab bar is docked below the content (never floating over it), so nothing scrolls under it.
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
            HomeHeader(vm, p, now)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(tab) {
                    Box(Modifier.fillMaxSize().enterRise(distance = 6.dp, durationMs = Motion.STATE + 60)) {
                        when (tab) {
                            Tab.LEARN -> PathView(vm, p)
                            Tab.PRACTICE -> PracticeTab(vm)
                            Tab.WORDS -> WordsTab(vm, p)
                            Tab.PROFILE -> ProfileTab(vm, p)
                        }
                    }
                }
            }
        }
        TabBar(tab) { vm.go(Screen.Home(it)) }
    }
}

/** Extra room at the end of scrolling tab content (the tab bar is docked, so this is breathing room only). */
val TabBarSpace = 24.dp

private data class TabItem(val tab: Tab, val icon: ImageVector, val iconOff: ImageVector, val label: String, val tag: String)

private val tabItems = listOf(
    TabItem(Tab.LEARN, Icons.Filled.Home, Icons.Outlined.Home, "Learn", "tabLearn"),
    TabItem(Tab.PRACTICE, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "Practice", "tabPractice"),
    TabItem(Tab.WORDS, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "Words", "tabWords"),
    TabItem(Tab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person, "Profile", "tabProfile"),
)

/**
 * The bottom navigation: a solid bar docked under the content. The selected tab shows a filled
 * icon on a jade pill and jade label; the others an outlined icon. Each item is a tab for TalkBack.
 */
@Composable
private fun TabBar(tab: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Bg2)) {
        HairlineDivider()
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp).selectableGroup()) {
            tabItems.forEach { item -> TabButton(item, item.tab == tab, Modifier.weight(1f)) { onSelect(item.tab) } }
        }
    }
}

@Composable
private fun TabButton(item: TabItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg by animateColorAsState(if (selected) C.AccentInk else C.Muted, tween(Motion.STATE), label = "tabFg")
    val pill by animateColorAsState(if (selected) C.AccentSoft else Color.Transparent, tween(Motion.STATE), label = "tabPill")
    Column(
        modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(Radius.tile))
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .testTag(item.tag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(width = 56.dp, height = 30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
            Icon(if (selected) item.icon else item.iconOff, null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Text(item.label, style = DoranType.caption, color = fg, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The compact status row on every tab: the Doran mark on the left, streak, today's XP and hearts on the right. */
@Composable
private fun HomeHeader(vm: AppViewModel, p: Progress, now: Long) {
    val today = vm.today()
    var heartsInfo by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = Space.gutter, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SealMark(size = 32.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp).semantics(mergeDescendants = true) { }) {
            Text("도란", style = DoranType.koBody, color = C.Text, maxLines = 1)
            Text("DORAN KOREAN", style = DoranType.caption.copy(letterSpacing = DoranType.caption.letterSpacing * 6), color = C.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val streak = p.visibleStreak(today)
        val extended = p.extendedToday(today)
        StatPill(
            Icons.Filled.LocalFireDepartment, "$streak", if (extended) C.Streak else C.Muted,
            "Day streak: $streak" + if (extended) ", done today" else "", tag = "streak",
        )
        Spacer(Modifier.width(6.dp))
        val xp = p.xpOn(today)
        val goal = p.dailyGoal.coerceAtLeast(1)
        StatPill(
            Icons.Filled.Bolt, "$xp", C.Xp, "Today: $xp of $goal XP", tag = "dailyXp",
            progress = xp.toFloat() / goal,
        )
        Spacer(Modifier.width(2.dp))
        val hearts = p.heartsNow(now)
        StatPill(
            Icons.Filled.Favorite, if (p.settings.unlimitedHearts) "∞" else "$hearts", C.Heart,
            if (p.settings.unlimitedHearts) "Unlimited hearts" else "Hearts: $hearts of ${Progress.MAX_HEARTS}",
            onClick = { heartsInfo = true }, tag = "heartsChip",
        )
    }
    if (heartsInfo) HeartsDialog(vm, p, now) { heartsInfo = false }
}

@Composable
fun HeartsDialog(vm: AppViewModel, p: Progress, now: Long, onDismiss: () -> Unit) {
    val hearts = p.heartsNow(now)
    val next = p.nextHeartIn(now)
    DoranDialog(onDismiss) {
        Text("Hearts", style = DoranType.h2, color = C.Text, modifier = Modifier.semantics { heading() })
        Row(
            Modifier.padding(vertical = 14.dp).semantics(mergeDescendants = true) { contentDescription = "$hearts of ${Progress.MAX_HEARTS} hearts" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(Progress.MAX_HEARTS) { i ->
                val full = p.settings.unlimitedHearts || i < hearts
                Icon(
                    if (full) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                    tint = if (full) C.Heart else C.OutlineStrong,
                    modifier = Modifier.size(32.dp).enterPop(delayMs = i * 50, from = 0.5f),
                )
            }
        }
        Text(
            when {
                p.settings.unlimitedHearts -> "Unlimited hearts are on (Settings)."
                next == 0L -> "Your hearts are full. A mistake in a lesson costs one heart."
                else -> "Next heart in ${(next / 60000) + 1} min. Finish a practice session to earn one now."
            },
            style = DoranType.body, color = C.Text2,
        )
        Spacer(Modifier.height(20.dp))
        // Practising for hearts only makes sense when some are missing.
        if (!p.settings.unlimitedHearts && next != 0L) {
            DoranButton("Practice to earn hearts", { onDismiss(); vm.startPractice(com.doranalley.core.LessonBuilder.PracticeMode.MIXED) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        DoranButton("Close", onDismiss, Modifier.fillMaxWidth(), variant = ButtonVariant.Tertiary, tone = C.select)
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

    LazyColumn(state = state, modifier = Modifier.fillMaxSize().testTag("path"), contentPadding = PaddingValues(bottom = TabBarSpace)) {
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
                Text(if (cur == null) "You finished the whole course! 정말 잘했어요!" else "More units are on the way.", color = C.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
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
        if (sectionStart) SectionTitle(u.section, Modifier.padding(bottom = 12.dp, start = 4.dp), color = sectionInk(u.section))
        val shape = RoundedCornerShape(26.dp)
        Box(Modifier.fillMaxWidth().jewel(col, edge, shape).clip(shape)) {
            DancheongBand(Modifier.fillMaxWidth().height(9.dp).align(Alignment.TopCenter), alpha = 0.75f)
            Text(
                watermark(u), fontSize = 124.sp, fontWeight = FontWeight.Black, fontFamily = HangulSerif, color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-54).dp, y = 16.dp).graphicsLayer { rotationZ = -8f },
            )
            Row(Modifier.padding(start = 18.dp, end = 16.dp, top = 22.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Unit ${u.number}", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = Nunito, letterSpacing = 0.8.sp)
                    Text(u.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, fontFamily = Nunito, lineHeight = 26.sp)
                    Text(u.blurb, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontFamily = Nunito, lineHeight = 18.sp)
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val f by animateFloatAsState(done.toFloat() / u.lessonCount, tween(700, easing = com.doranalley.ui.components.Motion.EaseOut), label = "unitProgress")
                        Box(Modifier.width(118.dp).height(7.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f))) {
                            if (f > 0.001f) Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White))))
                        }
                        Text("$done/${u.lessonCount}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = Nunito, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
                        .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.15f))), CircleShape)
                        .clickable(onClick = onGuide).testTag("guide:${u.id}"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.MenuBook, "Guidebook", tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}

/** Readable text colour for a section on the backdrop. */
private fun sectionInk(section: String): Color = when (section) {
    "Hangul" -> C.PurpleInk
    "Basics" -> C.GreenInk
    "Everyday" -> if (C.isDark) lighten(C.Plum, 0.35f) else C.PlumDark
    else -> C.OrangeInk
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
    val walked = col.copy(alpha = 0.75f)
    val unwalked = C.LineDark
    Box(
        Modifier.fillMaxWidth().height(top + RowH).drawBehind {
            // Dotted stepping stones between nodes. Each row draws both neighbouring curves clipped to
            // itself, so the trail is continuous and never covers another row's node.
            val cx = size.width / 2f
            val half = RowH.toPx() / 2f
            val cy = size.height - half
            val dash = PathEffect.dashPathEffect(floatArrayOf(0.1f, 18.dp.toPx()))
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
            val halo = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
            clipRect {
                if (hasPrev) {
                    val path = trail(cx + xp.dp.toPx(), -half, cx + x.dp.toPx(), cy)
                    if (prevDone) drawPath(path, col.copy(alpha = 0.18f), style = halo)
                    drawPath(path, if (prevDone) walked else unwalked, style = stroke)
                }
                if (hasNext) {
                    val path = trail(cx + x.dp.toPx(), cy, cx + xn.dp.toPx(), size.height + nextTop.toPx() + half)
                    if (done) drawPath(path, col.copy(alpha = 0.18f), style = halo)
                    drawPath(path, if (done) walked else unwalked, style = stroke)
                }
            }
        },
    ) {
        Box(Modifier.align(Alignment.BottomCenter).offset(x = x.dp).padding(bottom = (RowH - 92.dp) / 2)) {
            val icon = when { done -> Icons.Filled.Check; !unlocked -> Icons.Filled.Lock; isReview -> Icons.Filled.EmojiEvents; u.isHangul -> Icons.Filled.Translate; else -> Icons.Filled.Star }
            val fill = when { done -> C.Gold; unlocked -> col; else -> Color.Unspecified }
            val rim = when { done -> C.GoldDark; unlocked -> edge; else -> Color.Unspecified }
            NodeButton(icon, fill, rim, if (unlocked || done) Color.White else C.Muted, current, col, "node:${u.id}:${r.lesson}", onClick)
            if (current) StartTag(col, Modifier.align(Alignment.TopCenter))
        }
    }
}

/**
 * A lesson node: a glossy glass orb (gold when done, the section colour when open, frosted when
 * locked). The current lesson glows like a lantern, with a pulsing halo and a progress ring.
 */
@Composable
private fun NodeButton(icon: ImageVector, fill: Color, rim: Color, tint: Color, current: Boolean, col: Color, tag: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "squeeze")
    val halo = if (current) {
        val t = rememberInfiniteTransition(label = "halo")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "h")
    } else null
    val locked = fill == Color.Unspecified
    val ring = C.Line
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val rr = size.minDimension / 2f
            if (halo != null) {
                val h = halo.value
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.45f), col.copy(alpha = 0f))), rr * 1.25f)
                drawCircle(col.copy(alpha = 0.5f * (1f - h)), rr * (0.78f + 0.3f * h), style = Stroke(2.5.dp.toPx()))
                drawCircle(ring, rr - 4.dp.toPx(), style = Stroke(5.dp.toPx()))
                drawArc(col, -90f, 360f, false, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            } else if (!locked) {
                drawCircle(Brush.radialGradient(listOf(fill.copy(alpha = 0.30f * (0.5f + 0.5f * C.night)), fill.copy(alpha = 0f))), rr * 1.05f)
            }
        }
        Box(
            Modifier.size(NodeSize).graphicsLayer { scaleX = squeeze; scaleY = squeeze }
                .then(if (locked) Modifier.glass(CircleShape, elevation = 6.dp) else Modifier.shadow(10.dp, CircleShape, clip = false, ambientColor = fill, spotColor = fill))
                .clip(CircleShape)
                .drawBehind { if (!locked) drawOrb(fill, rim, center, size.minDimension / 2f) }
                .clickable(source, null, onClick = onClick).testTag(tag),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
        }
    }
}

private val TagArrow = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
}

/** The bobbing glass "Start" tag above the current lesson. */
@Composable
private fun StartTag(col: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "tag")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Column(
        modifier.offset { IntOffset(0, (-(48.dp.toPx()) - bob * 5.dp.toPx()).roundToInt()) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Start", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = Nunito, letterSpacing = 0.5.sp,
            modifier = Modifier.jewel(col, darken(col, 0.25f), RoundedCornerShape(14.dp), elevation = 8.dp)
                .padding(horizontal = 16.dp, vertical = 5.dp),
        )
        Box(Modifier.offset(y = (-1).dp).size(width = 14.dp, height = 8.dp).background(darken(col, 0.1f), TagArrow))
    }
}

@Composable
private fun LessonPopup(u: CourseUnit, lesson: Int, done: Boolean, unlocked: Boolean, onStart: () -> Unit, onDismiss: () -> Unit) {
    val (col, edge) = C.section(u.section)
    val isReview = lesson == u.lessonCount - 1
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(30.dp)
        Box(
            Modifier.enterPop(from = 0.85f, durationMs = 340)
                .then(if (unlocked) Modifier.jewel(col, edge, shape, elevation = 24.dp) else Modifier.glass(shape, elevation = 24.dp, opaque = 0.92f))
                .clip(shape).testTag("lessonPopup"),
        ) {
            if (unlocked) DancheongBand(Modifier.fillMaxWidth().height(9.dp).align(Alignment.TopCenter), alpha = 0.8f)
            Text(
                watermark(u), fontSize = 104.sp, fontWeight = FontWeight.Black, fontFamily = HangulSerif,
                color = (if (unlocked) Color.White else C.Ink).copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-2).dp),
            )
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp)) {
                val fg = if (unlocked) Color.White else C.Ink
                Text(u.title, color = fg, fontWeight = FontWeight.Black, fontSize = 23.sp, fontFamily = Nunito)
                Text(
                    if (isReview) "Unit review" else "Lesson ${lesson + 1} of ${u.lessonCount - 1}",
                    color = fg.copy(alpha = 0.85f), fontFamily = Nunito, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )
                // One bead per node in the unit.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                    repeat(u.lessonCount) { i ->
                        val on = i == lesson
                        Box(Modifier.enterPop(delayMs = 80 + i * 35, from = 0.2f).size(width = if (on) 24.dp else 8.dp, height = 8.dp).clip(CircleShape)
                            .background(if (unlocked) Color.White.copy(alpha = if (on) 1f else 0.35f) else if (on) C.Muted else C.Line))
                    }
                }
                if (unlocked) {
                    ChunkyButton(
                        if (done) "Practice again  +10 XP" else if (isReview) "Start review  +15 XP" else "Start  +10 XP",
                        onStart, Modifier.fillMaxWidth(), color = Color.White, edge = Color(0xFFE9ECF5), textColor = darken(edge, 0.1f), tag = "startLesson",
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
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        val headShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
        Box(Modifier.fillMaxWidth().jewel(col, edge, headShape, elevation = 18.dp).clip(headShape)) {
            Text(
                watermark(u), fontSize = 160.sp, fontWeight = FontWeight.Black, fontFamily = HangulSerif, color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = 34.dp),
            )
            DancheongBand(Modifier.fillMaxWidth().height(9.dp).align(Alignment.BottomCenter), alpha = 0.7f)
            Column(Modifier.statusBarsPadding().padding(start = 8.dp, end = 20.dp, bottom = 26.dp)) {
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
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item { SectionTitle("Key points", Modifier.padding(top = 22.dp, bottom = 10.dp)) }
            items(u.tips) { t ->
                CardBox(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(Modifier.padding(14.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(8.dp).graphicsLayer { rotationZ = 45f }.clip(RoundedCornerShape(2.dp)).background(col))
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
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(RoundedCornerShape(18.dp), elevation = 4.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onPlay).padding(vertical = 8.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SpeakerButton(playing, onPlay, size = 42.dp)
        Column(Modifier.padding(start = 14.dp, end = 34.dp)) {
            Text(ko, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink, lineHeight = 24.sp)
            Text(en, fontSize = 14.sp, color = C.Muted)
        }
    }
}
