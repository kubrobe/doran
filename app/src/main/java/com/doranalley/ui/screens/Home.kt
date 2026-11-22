package com.doranalley.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doranalley.core.CourseUnit
import com.doranalley.core.Progress
import com.doranalley.ui.AppViewModel
import com.doranalley.ui.Screen
import com.doranalley.ui.Tab
import com.doranalley.ui.components.AudioButton
import com.doranalley.ui.components.ButtonVariant
import com.doranalley.ui.components.DancheongBand
import com.doranalley.ui.components.Diamond
import com.doranalley.ui.components.DoranButton
import com.doranalley.ui.components.DoranCard
import com.doranalley.ui.components.DoranDialog
import com.doranalley.ui.components.DoranIconButton
import com.doranalley.ui.components.DoranSheet
import com.doranalley.ui.components.EmptyState
import com.doranalley.ui.components.HairlineDivider
import com.doranalley.ui.components.IconStyle
import com.doranalley.ui.components.Level
import com.doranalley.ui.components.ListRow
import com.doranalley.ui.components.Mood
import com.doranalley.ui.components.Motion
import com.doranalley.ui.components.ProgressBar
import com.doranalley.ui.components.RewardChip
import com.doranalley.ui.components.SealMark
import com.doranalley.ui.components.SectionHeader
import com.doranalley.ui.components.StatPill
import com.doranalley.ui.components.TopBar
import com.doranalley.ui.components.enterPop
import com.doranalley.ui.components.enterRise
import com.doranalley.ui.components.surface
import com.doranalley.ui.theme.C
import com.doranalley.ui.theme.DoranType
import com.doranalley.ui.theme.LocalReduceMotion
import com.doranalley.ui.theme.Radius
import com.doranalley.ui.theme.Space
import com.doranalley.ui.theme.Tone
import com.doranalley.ui.theme.darken
import com.doranalley.ui.theme.fixedSp
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
private val NodeSize = 64.dp

/** Horizontal offset of node [idx]: the path winds left and right like an alley. */
private fun nodeX(idx: Int): Float = (sin(idx * 0.9) * 70).toFloat()

private enum class NodeState { DONE, CURRENT, OPEN, LOCKED }

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
                is PathRow.Header -> UnitHeader(r.unit, r.sectionStart, p.lessonsDone(r.unit), cur?.first?.id == r.unit.id) { vm.go(Screen.Guidebook(r.unit)) }
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
            EmptyState(
                if (cur == null) Mood.CHEER else Mood.THINK,
                if (cur == null) "Course complete!" else "More units are on the way",
                if (cur == null) "You finished every unit. 정말 잘했어요!" else "Horangi is preparing the next part of the alley.",
            )
        }
    }

    open?.let { node ->
        val done = p.lessonsDone(node.unit) > node.lesson
        val unlocked = p.isUnlocked(course, node.unit, node.lesson)
        LessonSheet(node.unit, node.lesson, done, unlocked, onStart = { open = null; vm.startLesson(node.unit, node.lesson) }, onDismiss = { open = null })
    }
}

/** A big character from the unit (a letter or the first syllable of its first word) used as a watermark. */
private fun watermark(u: CourseUnit): String =
    u.letters.firstOrNull()?.ch ?: u.words.firstOrNull()?.ko?.firstOrNull()?.toString() ?: "한"

/**
 * A unit header: a flat card with a stripe in the section colour, the unit number, title and blurb,
 * a progress bar and the guidebook button. A section's first unit gets the section title and a
 * short dancheong trim above it.
 */
@Composable
private fun UnitHeader(u: CourseUnit, sectionStart: Boolean, done: Int, isCurrentUnit: Boolean, onGuide: () -> Unit) {
    val tone = C.section(u.section)
    val complete = done >= u.lessonCount
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(top = if (sectionStart) 24.dp else 28.dp, bottom = 8.dp)) {
        if (sectionStart) {
            SectionHeader(u.section, color = tone.ink, ornament = tone.fill)
            DancheongBand(Modifier.padding(top = 4.dp, bottom = 14.dp).width(72.dp).height(6.dp).clip(RoundedCornerShape(2.dp)), alpha = 0.85f)
        }
        val shape = RoundedCornerShape(Radius.card)
        val stripe = tone.fill
        Box(
            Modifier.fillMaxWidth()
                .then(if (isCurrentUnit) Modifier.clip(shape).background(tone.soft).border(1.5.dp, tone.fill.copy(alpha = 0.6f), shape) else Modifier.surface(Level.Raised, shape))
                .drawBehind { drawRect(stripe, size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height)) },
        ) {
            Text(
                watermark(u), style = DoranType.koLetter.copy(fontSize = 88.dp.fixedSp(), lineHeight = 100.dp.fixedSp()), color = tone.ink.copy(alpha = 0.08f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 64.dp).clearAndSetSemantics { },
            )
            Row(Modifier.padding(start = 21.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UNIT ${u.number}", style = DoranType.caption.copy(letterSpacing = DoranType.caption.letterSpacing * 5), color = tone.ink)
                        if (complete) {
                            Icon(Icons.Filled.CheckCircle, null, tint = tone.ink, modifier = Modifier.padding(start = 6.dp).size(14.dp))
                            Text("Completed", style = DoranType.caption, color = tone.ink, modifier = Modifier.padding(start = 3.dp))
                        }
                    }
                    Text(u.title, style = DoranType.h2, color = C.Text, modifier = Modifier.padding(top = 2.dp).semantics { heading() })
                    Text(u.blurb, style = DoranType.bodySmall, color = C.Text2, modifier = Modifier.padding(top = 2.dp))
                    Row(Modifier.padding(top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProgressBar(done.toFloat() / u.lessonCount, Modifier.weight(1f), color = tone.fill, height = 6.dp, track = if (isCurrentUnit) C.Surface else C.SurfaceRaised)
                        Text("$done/${u.lessonCount}", style = DoranType.label, color = C.Text2, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                DoranIconButton(
                    Icons.AutoMirrored.Filled.MenuBook, "Unit ${u.number} guidebook", onGuide,
                    style = IconStyle.Tonal, tone = tone, tag = "guide:${u.id}",
                )
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
    val tone = C.section(u.section)
    val isReview = r.lesson == u.lessonCount - 1
    val x = nodeX(r.idx)
    val xp = nodeX(r.idx - 1)
    val xn = nodeX(r.idx + 1)
    val walked = tone.fill.copy(alpha = 0.6f)
    val unwalked = C.OutlineStrong.copy(alpha = 0.55f)
    val state = when { current -> NodeState.CURRENT; done -> NodeState.DONE; unlocked -> NodeState.OPEN; else -> NodeState.LOCKED }
    val name = if (isReview) "Unit ${u.number} review" else "Unit ${u.number}, lesson ${r.lesson + 1} of ${u.lessonCount - 1}"
    val label = name + when (state) { NodeState.DONE -> ", completed"; NodeState.CURRENT -> ", next lesson"; NodeState.OPEN -> ", open"; NodeState.LOCKED -> ", locked" }
    Box(
        Modifier.fillMaxWidth().height(top + RowH).drawBehind {
            // Dotted stepping stones between nodes. Each row draws both neighbouring curves clipped to
            // itself, so the trail is continuous and never covers another row's node.
            val cx = size.width / 2f
            val half = RowH.toPx() / 2f
            val cy = size.height - half
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 16.dp.toPx())))
            clipRect {
                if (hasPrev) drawPath(trail(cx + xp.dp.toPx(), -half, cx + x.dp.toPx(), cy), if (prevDone) walked else unwalked, style = stroke)
                if (hasNext) drawPath(trail(cx + x.dp.toPx(), cy, cx + xn.dp.toPx(), size.height + nextTop.toPx() + half), if (done) walked else unwalked, style = stroke)
            }
        },
    ) {
        Box(Modifier.align(Alignment.BottomCenter).offset(x = x.dp).padding(bottom = (RowH - 88.dp) / 2), contentAlignment = Alignment.Center) {
            val icon = when {
                state == NodeState.LOCKED -> Icons.Filled.Lock
                state == NodeState.DONE -> Icons.Filled.Check
                isReview -> Icons.Filled.EmojiEvents
                u.isHangul -> Icons.Filled.Translate
                else -> Icons.Filled.Star
            }
            // Hangul lessons show one of the unit's letters instead of a generic icon.
            val glyph = if (u.isHangul && !isReview) (u.letters.getOrNull(r.lesson * 2)?.ch ?: watermark(u)) else null
            PathNode(state, icon, tone, label, "node:${u.id}:${r.lesson}", glyph, onClick)
            if (current) StartLabel(if (isReview) "Review" else "Start", tone, Modifier.align(Alignment.TopCenter))
        }
    }
}

/**
 * A lesson node. Done: a soft disc with a check. Locked: an outlined disc with a lock. The current
 * lesson is the strongest element on the screen: solid section colour, a ring, a raised lip that
 * presses down, and a "Start" label above it.
 */
@Composable
private fun PathNode(state: NodeState, icon: ImageVector, tone: Tone, label: String, tag: String, glyph: String?, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val push by animateFloatAsState(if (pressed && !reduce) 1f else 0f, tween(Motion.PRESS), label = "push")
    val lipH = 5.dp
    val ringColor = tone.fill.copy(alpha = 0.35f)
    val lip = darken(tone.fill, 0.28f)
    Box(
        Modifier.size(88.dp)
            .clip(CircleShape)
            .clickable(source, null, role = Role.Button, onClickLabel = "Open lesson", onClick = onClick)
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (state == NodeState.CURRENT) {
            Canvas(Modifier.size(88.dp)) {
                drawCircle(ringColor, size.minDimension / 2f - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
            }
        }
        val fill = when (state) { NodeState.DONE, NodeState.OPEN -> tone.soft; NodeState.CURRENT -> tone.fill; NodeState.LOCKED -> C.Surface }
        val border = when (state) { NodeState.DONE, NodeState.OPEN -> tone.fill; NodeState.CURRENT -> Color.Transparent; NodeState.LOCKED -> C.OutlineStrong }
        val tint = when (state) { NodeState.DONE, NodeState.OPEN -> tone.ink; NodeState.CURRENT -> tone.on; NodeState.LOCKED -> C.Muted }
        Box(
            Modifier.size(NodeSize, NodeSize + if (state == NodeState.CURRENT) lipH else 0.dp)
                .drawBehind {
                    if (state == NodeState.CURRENT) {
                        // the lip: a darker disc under the face
                        drawCircle(lip, NodeSize.toPx() / 2f, Offset(size.width / 2f, size.height - NodeSize.toPx() / 2f))
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier.graphicsLayer { translationY = push * lipH.toPx() }
                    .size(NodeSize).clip(CircleShape).background(fill)
                    .then(if (border != Color.Transparent) Modifier.border(2.dp, border, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (glyph != null && (state == NodeState.CURRENT || state == NodeState.OPEN)) {
                    Text(glyph, style = DoranType.h1.copy(fontFamily = com.doranalley.ui.theme.Hangul, fontSize = 28.dp.fixedSp(), lineHeight = 34.dp.fixedSp()), color = tint)
                }
                else Icon(icon, null, tint = tint, modifier = Modifier.size(if (state == NodeState.CURRENT) 30.dp else 26.dp))
            }
        }
    }
}

private val TagArrow = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
}

/** The "Start" label above the current lesson. Still (no bobbing); it rises in once. */
@Composable
private fun StartLabel(text: String, tone: Tone, modifier: Modifier) {
    Column(
        modifier.offset(y = (-40).dp).enterRise(distance = 6.dp, durationMs = Motion.SCREEN).clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text.uppercase(), style = DoranType.label.copy(letterSpacing = DoranType.label.letterSpacing * 4), color = tone.on,
            modifier = Modifier.clip(RoundedCornerShape(Radius.badge + 2.dp)).background(tone.fill).padding(horizontal = 14.dp, vertical = 6.dp),
        )
        Box(Modifier.offset(y = (-1).dp).size(width = 12.dp, height = 7.dp).background(tone.fill, TagArrow))
    }
}

/** The sheet that opens from a path node: what the lesson is, what it's worth, and one button. */
@Composable
private fun LessonSheet(u: CourseUnit, lesson: Int, done: Boolean, unlocked: Boolean, onStart: () -> Unit, onDismiss: () -> Unit) {
    val tone = C.section(u.section)
    val isReview = lesson == u.lessonCount - 1
    DoranSheet(onDismiss, tag = "lessonPopup") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RewardChip("Unit ${u.number} · ${u.section}", tone)
            if (done) RewardChip("Completed", C.accent, Modifier.padding(start = 6.dp), icon = Icons.Filled.Check)
        }
        Text(u.title, style = DoranType.h1, color = C.Text, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
        Text(
            if (isReview) "Unit review · every word and phrase from this unit" else "Lesson ${lesson + 1} of ${u.lessonCount - 1}",
            style = DoranType.body, color = C.Text2, modifier = Modifier.padding(top = 4.dp),
        )
        // One bead per node in the unit.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 14.dp, bottom = 20.dp).clearAndSetSemantics { }) {
            repeat(u.lessonCount) { i ->
                val on = i == lesson
                Box(
                    Modifier.size(width = if (on) 24.dp else 8.dp, height = 8.dp).clip(CircleShape)
                        .background(if (on) tone.fill else if (i < lesson || done) tone.fill.copy(alpha = 0.4f) else C.Outline),
                )
            }
        }
        if (unlocked) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                RewardChip(if (isReview) "+15 XP" else "+10 XP", C.xp, icon = Icons.Filled.Bolt)
                Text("+5 XP for no mistakes", style = DoranType.bodySmall, color = C.Muted, modifier = Modifier.padding(start = 10.dp))
            }
            DoranButton(
                if (done) "Practice again" else if (isReview) "Start review" else "Start lesson",
                onStart, Modifier.fillMaxWidth(), tag = "startLesson",
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Filled.Lock, null, tint = C.Muted, modifier = Modifier.size(20.dp))
                Text("Finish the lessons above to open this one.", style = DoranType.body, color = C.Text2, modifier = Modifier.padding(start = 8.dp))
            }
            DoranButton("Close", onDismiss, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
        }
    }
}

// ------------------------------------------------------------------ guidebook

@Composable
fun GuidebookScreen(vm: AppViewModel, u: CourseUnit) {
    val playing by vm.audio.playing.collectAsState()
    val tone = C.section(u.section)
    androidx.activity.compose.BackHandler { vm.back() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar("Guidebook", onBack = { vm.back() })
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 32.dp)) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
                    Text(
                        watermark(u), style = DoranType.koLetter.copy(fontSize = 88.dp.fixedSp(), lineHeight = 100.dp.fixedSp()), color = tone.ink.copy(alpha = 0.10f),
                        modifier = Modifier.align(Alignment.TopEnd).clearAndSetSemantics { },
                    )
                    Column {
                        RewardChip("Unit ${u.number} · ${u.section}", tone)
                        Text(u.title, style = DoranType.h1, color = C.Text, modifier = Modifier.padding(top = 10.dp).semantics { heading() })
                        Text(u.blurb, style = DoranType.body, color = C.Text2, modifier = Modifier.padding(top = 4.dp, end = 64.dp))
                    }
                }
            }
            item { SectionHeader("Key points", Modifier.padding(top = 20.dp, bottom = 10.dp), ornament = tone.fill) }
            item {
                DoranCard(Modifier.fillMaxWidth()) {
                    u.tips.forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Diamond(tone.fill, Modifier.padding(top = 8.dp), size = 7.dp)
                            Text(t, style = DoranType.body, color = C.Text, modifier = Modifier.padding(start = 12.dp))
                        }
                        if (i < u.tips.lastIndex) HairlineDivider(Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            if (u.letters.isNotEmpty()) {
                item { SectionHeader("Letters", Modifier.padding(top = 24.dp, bottom = 10.dp), ornament = tone.fill) }
                item {
                    GuideList(u.letters.map { Triple(it.ch, "${it.rom} · ${it.hint}", it.say) }, playing) { vm.audio.play(it) }
                }
            }
            item { SectionHeader("Words", Modifier.padding(top = 24.dp, bottom = 10.dp), ornament = tone.fill) }
            item { GuideList(u.words.map { Triple(it.ko, it.en, it.ko) }, playing) { vm.audio.play(it) } }
            if (u.sentences.isNotEmpty()) {
                item { SectionHeader("Phrases", Modifier.padding(top = 24.dp, bottom = 10.dp), ornament = tone.fill) }
                item { GuideList(u.sentences.map { Triple(it.ko, it.en, it.ko) }, playing) { vm.audio.play(it) } }
            }
        }
    }
}

/** A card of guide rows (Korean, meaning, audio) separated by hairlines. Each triple is (Korean, meaning, audio text). */
@Composable
private fun GuideList(items: List<Triple<String, String, String>>, playing: String?, onPlay: (String) -> Unit) {
    DoranCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        items.forEachIndexed { i, (ko, en, say) ->
            GuideRow(ko, en, playing == "ko:$say") { onPlay(say) }
            if (i < items.lastIndex) HairlineDivider(Modifier.padding(start = 76.dp))
        }
    }
}

/** A Korean item with its meaning and a play button; the whole row plays the audio. */
@Composable
fun GuideRow(ko: String, en: String, playing: Boolean, trailing: (@Composable RowScope.() -> Unit)? = null, onPlay: () -> Unit) {
    ListRow(
        ko, subtitle = en, titleStyle = DoranType.koBody,
        leading = { AudioButton(playing, onPlay) },
        trailing = trailing,
        onClick = onPlay,
    )
}
