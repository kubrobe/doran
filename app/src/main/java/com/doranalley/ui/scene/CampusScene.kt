package com.doranalley.ui.scene

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.doranalley.core.Campus
import com.doranalley.core.CharacterSpec
import com.doranalley.core.Location
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * The hand-drawn 2D campus: a street of buildings with readable Korean signboards, and indoor
 * corridors with a readable room sign and a wall map. Characters walk between places when the
 * conversation moves them; with reduced motion they appear in place.
 */
@Composable
fun CampusScene(
    campus: Campus,
    characters: Map<String, CharacterSpec>,
    positions: Map<String, String>,
    speaker: String?,
    speakerPose: String,
    learnerTalking: Boolean,
    reducedMotion: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val here = campus.location(positions["learner"]) ?: campus.locations.values.first()
    // On the street everyone shares one backdrop and walks along it; each indoor place is its own backdrop.
    val sceneKey = if (here.backdrop == "street") "street" else here.id
    Box(modifier.clipToBounds().semantics { contentDescription = description }) {
        Crossfade(targetState = sceneKey, animationSpec = tween(if (reducedMotion) 0 else 450), label = "scene") { key ->
            val loc = if (key == "street") (if (here.backdrop == "street") here else campus.locations.values.first { it.backdrop == "street" })
            else campus.location(key) ?: here
            SceneLayer(campus, characters, loc, positions, speaker, speakerPose, learnerTalking, reducedMotion)
        }
    }
}

private data class Actor(val id: String, val x: Float, val alpha: Float, val walking: Boolean)

@Composable
private fun SceneLayer(
    campus: Campus,
    characters: Map<String, CharacterSpec>,
    loc: Location,
    positions: Map<String, String>,
    speaker: String?,
    speakerPose: String,
    learnerTalking: Boolean,
    reducedMotion: Boolean,
) {
    val street = loc.backdrop == "street"
    val present = positions.filter { (_, where) ->
        val l = campus.location(where)
        if (street) l?.backdrop == "street" else where == loc.id
    }.keys.sortedWith(compareBy({ it != "learner" }, { it }))

    // Target x for everyone present: grouped around their building on the street, in slots indoors.
    val targets = HashMap<String, Float>()
    if (street) {
        val groups = present.groupBy { positions[it] }
        for ((where, ids) in groups) {
            val bx = campus.building(campus.location(where)?.building)?.x ?: 0.5f
            ids.forEachIndexed { i, id -> targets[id] = (bx - 0.05f + i * 0.09f).coerceIn(0.06f, 0.94f) }
        }
    } else {
        val slots = listOf(0.24f, 0.5f, 0.74f, 0.88f)
        present.forEachIndexed { i, id -> targets[id] = slots.getOrElse(i) { 0.9f } }
    }

    // Everyone seen in this scene keeps an animated x and alpha, so people can walk in and walk away.
    val seen = remember { LinkedHashSet<String>() }
    val xs = remember { HashMap<String, Animatable<Float, AnimationVector1D>>() }
    val alphas = remember { HashMap<String, Animatable<Float, AnimationVector1D>>() }
    val exitX = if (street) campus.building(loc.building)?.x ?: 1.1f else 1.15f
    seen.addAll(present)

    for (id in seen.toList()) key(id) {
        val isHere = id in present
        val tx = targets[id] ?: exitX
        val anim = remember { Animatable(if (reducedMotion || id == "learner") tx else if (tx > 0.5f) 1.12f else -0.12f) }
        val al = remember { Animatable(if (reducedMotion || id == "learner") 1f else 0f) }
        xs[id] = anim
        alphas[id] = al
        LaunchedEffect(tx, isHere, reducedMotion) {
            if (reducedMotion) { anim.snapTo(tx); al.snapTo(if (isHere) 1f else 0f); return@LaunchedEffect }
            launch { al.animateTo(if (isHere) 1f else 0f, tween(if (isHere) 300 else 900, delayMillis = if (isHere) 0 else 500)) }
            val dist = kotlin.math.abs(anim.value - tx)
            anim.animateTo(tx, tween((dist * 2600).toInt().coerceIn(250, 1300), easing = LinearEasing))
        }
    }

    val bob = if (reducedMotion) null else rememberInfiniteTransition(label = "bob")
    val phase = bob?.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val breathe = bob?.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "breathe")
    val measurer = rememberTextMeasurer()

    Canvas(Modifier.fillMaxSize()) {
        if (street) drawStreet(campus, loc, measurer) else drawCorridor(campus, loc, measurer)
        val ground = if (street) size.height * 0.93f else size.height * 0.95f
        val actors = xs.keys.mapNotNull { id ->
            val x = xs[id]?.value ?: return@mapNotNull null
            val a = alphas[id]?.value ?: 1f
            if (a <= 0.01f) return@mapNotNull null
            Actor(id, x, a, kotlin.math.abs(x - (targets[id] ?: exitX)) > 0.004f)
        }.sortedBy { it.x }
        val centre = actors.map { it.x }.average().toFloat().takeIf { !it.isNaN() } ?: 0.5f
        for (a in actors) {
            val spec = characters[a.id]
            val pose = when {
                a.walking -> "walk"
                a.id == speaker -> speakerPose
                else -> "idle"
            }
            val talking = (a.id == speaker && !a.walking) || (a.id == "learner" && learnerTalking)
            drawPerson(
                x = a.x * size.width, ground = ground,
                height = size.height * (if (street) 0.36f else 0.5f),
                body = spec?.let { Color(it.color) } ?: Color(0xFF3F7F7A),
                hair = spec?.let { Color(it.hair) } ?: Color(0xFF2B2420),
                pose = pose, facingRight = a.x < centre || (a.walking && (targets[a.id] ?: exitX) > a.x),
                phase = phase?.value ?: 0f, breathe = breathe?.value ?: 0f,
                talking = talking, isLearner = a.id == "learner", alpha = a.alpha,
                name = if (a.id == "learner") "나" else spec?.nameKo ?: a.id,
                measurer = measurer,
                highlight = a.id == speaker,
            )
        }
    }
}

// ------------------------------------------------------------------ backdrops

private val signStyle = TextStyle(color = Color(0xFF2F2A26), fontSize = 13.sp, fontWeight = FontWeight.Bold)

private fun DrawScope.label(m: TextMeasurer, text: String, center: Offset, style: TextStyle, bg: Color?, pad: Float = 6f) {
    val r = m.measure(text, style)
    val tl = Offset(center.x - r.size.width / 2f, center.y - r.size.height / 2f)
    if (bg != null) {
        drawRoundRect(bg, Offset(tl.x - pad * 1.6f, tl.y - pad), Size(r.size.width + pad * 3.2f, r.size.height + pad * 2), CornerRadius(pad, pad))
        drawRoundRect(Color(0x33000000), Offset(tl.x - pad * 1.6f, tl.y - pad), Size(r.size.width + pad * 3.2f, r.size.height + pad * 2), CornerRadius(pad, pad), style = Stroke(1.5f))
    }
    drawText(r, topLeft = tl)
}

private fun DrawScope.drawStreet(campus: Campus, here: Location, m: TextMeasurer) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.70f
    // Sky and late-morning light.
    drawRect(Brush.verticalGradient(listOf(Color(0xFFFFEBD3), Color(0xFFF8D9B5)), 0f, horizon), size = Size(w, horizon))
    drawCircle(Color(0x55FFF4D6), radius = h * 0.16f, center = Offset(w * 0.82f, h * 0.16f))
    // Distant hills and a generic hilltop tower.
    val hills = Path().apply {
        moveTo(0f, horizon * 0.62f)
        cubicTo(w * 0.2f, horizon * 0.42f, w * 0.35f, horizon * 0.58f, w * 0.52f, horizon * 0.48f)
        cubicTo(w * 0.7f, horizon * 0.36f, w * 0.85f, horizon * 0.55f, w, horizon * 0.5f)
        lineTo(w, horizon); lineTo(0f, horizon); close()
    }
    drawPath(hills, Color(0xFFE7C9A4))
    drawLine(Color(0xFFC9A884), Offset(w * 0.58f, horizon * 0.43f), Offset(w * 0.58f, horizon * 0.26f), strokeWidth = 3f)
    drawOval(Color(0xFFC9A884), Offset(w * 0.565f, horizon * 0.28f), Size(w * 0.03f, horizon * 0.035f))
    // Clouds.
    for ((cx, cy) in listOf(0.18f to 0.14f, 0.46f to 0.09f)) {
        drawOval(Color(0xAAFFFFFF), Offset(w * cx, h * cy), Size(w * 0.12f, h * 0.05f))
        drawOval(Color(0xAAFFFFFF), Offset(w * (cx + 0.04f), h * (cy - 0.025f)), Size(w * 0.08f, h * 0.05f))
    }
    // Buildings.
    for (b in campus.buildings) {
        val isCafe = b.floors <= 1
        val bw = w * (if (isCafe) 0.15f else 0.2f)
        val bh = h * (0.16f + b.floors * 0.11f)
        val left = b.x * w - bw / 2
        val top = horizon - bh
        val body = Color(b.color)
        drawRect(body, Offset(left, top), Size(bw, bh))
        drawRect(Color(0x22000000), Offset(left + bw * 0.86f, top), Size(bw * 0.14f, bh)) // side shade
        drawRect(body.darker(0.75f), Offset(left - 4f, top - 6f), Size(bw + 8f, 8f)) // roof line
        // Windows.
        val rows = b.floors
        for (r in 0 until rows) for (c in 0 until 3) {
            val ww = bw * 0.18f
            val wx = left + bw * 0.12f + c * bw * 0.27f
            val wy = top + bh * 0.1f + r * (bh * 0.72f / rows.coerceAtLeast(1))
            if (isCafe) continue
            drawRoundRect(Color(0xFFFFF6E3), Offset(wx, wy), Size(ww, bh * 0.1f), CornerRadius(3f, 3f))
            drawLine(body.darker(0.8f), Offset(wx + ww / 2, wy), Offset(wx + ww / 2, wy + bh * 0.1f), 1.5f)
        }
        // Door.
        val dw = bw * 0.24f
        val dh = h * 0.11f
        drawRoundRect(Color(0xFF7A5238), Offset(b.x * w - dw / 2, horizon - dh), Size(dw, dh), CornerRadius(4f, 4f))
        drawRect(Color(0x55FFFFFF), Offset(b.x * w - dw / 2 + 4f, horizon - dh + 6f), Size(dw - 8f, dh * 0.35f))
        if (isCafe) {
            // Striped awning and a large window.
            val aw = bw * 1.08f
            val stripes = 7
            for (i in 0 until stripes) {
                drawRect(if (i % 2 == 0) Color(0xFFD9673E) else Color(0xFFFFF3E2),
                    Offset(left - bw * 0.04f + i * aw / stripes, top + bh * 0.28f), Size(aw / stripes, bh * 0.12f))
            }
            drawRoundRect(Color(0xFFFFF6E3), Offset(left + bw * 0.08f, top + bh * 0.48f), Size(bw * 0.3f, bh * 0.3f), CornerRadius(4f, 4f))
            drawCircle(Color(0xFF6E9B62), radius = bw * 0.07f, center = Offset(left + bw * 0.9f, horizon - bw * 0.08f))
            drawRect(Color(0xFFB0714A), Offset(left + bw * 0.85f, horizon - bw * 0.06f), Size(bw * 0.1f, bw * 0.06f))
        }
        // Readable Korean signboard over the door.
        val signY = if (isCafe) top + bh * 0.14f else horizon - dh - h * 0.05f
        val focus = b.id == here.building
        label(m, b.ko, Offset(b.x * w, signY), signStyle.copy(fontSize = if (focus) 14.sp else 12.sp),
            if (focus) Color(0xFFFFF0C2) else Color(0xFFFFFAF0))
    }
    // Trees and lamps between buildings.
    for (tx in listOf(0.265f, 0.53f, 0.775f)) {
        val x = tx * w
        drawLine(Color(0xFF7A5A40), Offset(x, horizon), Offset(x, horizon - h * 0.12f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF7FA66E), radius = h * 0.07f, center = Offset(x, horizon - h * 0.17f))
        drawCircle(Color(0xFF93B982), radius = h * 0.05f, center = Offset(x - h * 0.04f, horizon - h * 0.13f))
        drawCircle(Color(0xFF6E9760), radius = h * 0.045f, center = Offset(x + h * 0.045f, horizon - h * 0.12f))
    }
    for (lx in listOf(0.03f, 0.97f)) {
        drawLine(Color(0xFF4F4A45), Offset(lx * w, horizon + h * 0.04f), Offset(lx * w, horizon - h * 0.2f), 4f)
        drawCircle(Color(0xFFFFE6A8), 7f, Offset(lx * w, horizon - h * 0.2f))
    }
    // Pavement.
    drawRect(Color(0xFFEADBC4), Offset(0f, horizon), Size(w, h - horizon))
    drawRect(Color(0xFFD9C6AA), Offset(0f, horizon), Size(w, 5f))
    var px = -20f
    while (px < w) { drawLine(Color(0x22000000), Offset(px, horizon + 5f), Offset(px + 30f, h), 1.2f); px += 46f }
    // Small street sign with the neighbourhood name.
    label(m, "${campus.neighborhoodKo} · ${campus.nameKo}", Offset(w * 0.5f, h * 0.05f),
        signStyle.copy(fontSize = 11.sp, color = Color(0xFFFFFFFF)), Color(0xFF3F7F7A))
}

private fun DrawScope.drawCorridor(campus: Campus, here: Location, m: TextMeasurer) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.8f
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF2EADB), Color(0xFFE6DCC9)), 0f, floorY), size = Size(w, floorY))
    drawRect(Color(0xFFD7C3A5), Offset(0f, floorY), Size(w, h - floorY))
    drawRect(Color(0xFFB79C79), Offset(0f, floorY - 6f), Size(w, 8f))
    var fx = 0f
    while (fx < w) { drawLine(Color(0x22000000), Offset(fx, floorY), Offset(fx - 40f, h), 1.2f); fx += 60f }
    // Ceiling lights.
    for (lx in listOf(0.2f, 0.5f, 0.8f)) drawRoundRect(Color(0xFFFFF8E6), Offset(w * lx - 30f, 0f), Size(60f, 10f), CornerRadius(4f, 4f))
    // Window on the left.
    drawRoundRect(Color(0xFFCFE4EA), Offset(w * 0.03f, h * 0.16f), Size(w * 0.14f, h * 0.3f), CornerRadius(6f, 6f))
    drawLine(Color(0xFFFFFFFF), Offset(w * 0.1f, h * 0.16f), Offset(w * 0.1f, h * 0.46f), 3f)
    drawCircle(Color(0xFF8DB27B), radius = h * 0.05f, center = Offset(w * 0.06f, h * 0.4f))

    if (here.showMap) {
        // Wall map showing two buildings.
        val left = w * 0.36f
        val top = h * 0.1f
        val mw = w * 0.5f
        val mh = h * 0.42f
        drawRoundRect(Color(0xFF7A5238), Offset(left - 6f, top - 6f), Size(mw + 12f, mh + 12f), CornerRadius(8f, 8f))
        drawRoundRect(Color(0xFFFFFBF2), Offset(left, top), Size(mw, mh), CornerRadius(6f, 6f))
        label(m, here.sign ?: "안내도", Offset(left + mw / 2, top + mh * 0.12f), signStyle.copy(fontSize = 12.sp), null)
        val shown = campus.buildings.take(2)
        shown.forEachIndexed { i, b ->
            val bx = left + mw * (0.27f + i * 0.46f)
            val by = top + mh * 0.6f
            drawRoundRect(Color(b.color), Offset(bx - mw * 0.16f, by - mh * 0.2f), Size(mw * 0.32f, mh * 0.38f), CornerRadius(6f, 6f))
            label(m, b.ko, Offset(bx, by), signStyle.copy(fontSize = 13.sp), Color(0xFFFFFFFF), 4f)
            if (b.id == here.building) {
                drawCircle(Color(0xFFD9673E), 7f, Offset(bx, by + mh * 0.12f))
                label(m, "현재 위치", Offset(bx, by + mh * 0.25f), signStyle.copy(fontSize = 9.sp, color = Color(0xFFD9673E)), null)
            }
        }
        if (shown.size == 2) {
            drawLine(Color(0xFFB0A08A), Offset(left + mw * 0.43f, top + mh * 0.6f), Offset(left + mw * 0.57f, top + mh * 0.6f), 4f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
    } else {
        // A closed classroom door with a readable room sign.
        val dx = w * 0.42f
        val dw = w * 0.2f
        val dTop = h * 0.2f
        drawRect(Color(0xFF8E6B4E), Offset(dx - 8f, dTop - 8f), Size(dw + 16f, floorY - dTop + 8f))
        drawRect(Color(0xFFB98A62), Offset(dx, dTop), Size(dw, floorY - dTop))
        drawRect(Color(0xFFD6EBF0), Offset(dx + dw * 0.2f, dTop + h * 0.06f), Size(dw * 0.6f, h * 0.12f))
        drawCircle(Color(0xFF5A4636), 6f, Offset(dx + dw * 0.85f, dTop + (floorY - dTop) * 0.55f))
        here.sign?.let { label(m, it, Offset(dx + dw / 2, dTop - h * 0.07f), signStyle.copy(fontSize = 15.sp), Color(0xFFFFFFFF)) }
        // Notice board.
        drawRoundRect(Color(0xFFCFAE7E), Offset(w * 0.7f, h * 0.18f), Size(w * 0.22f, h * 0.28f), CornerRadius(4f, 4f))
        drawRect(Color(0xFFFFFFFF), Offset(w * 0.72f, h * 0.21f), Size(w * 0.08f, h * 0.1f))
        drawRect(Color(0xFFFFF0B5), Offset(w * 0.82f, h * 0.24f), Size(w * 0.08f, h * 0.12f))
    }
    label(m, here.ko, Offset(w * 0.12f, h * 0.06f), signStyle.copy(fontSize = 11.sp, color = Color.White), Color(0xFF3F7F7A))
}

// ------------------------------------------------------------------ people

private fun Color.darker(f: Float) = Color(red * f, green * f, blue * f, alpha)

private fun DrawScope.drawPerson(
    x: Float, ground: Float, height: Float, body: Color, hair: Color, pose: String, facingRight: Boolean,
    phase: Float, breathe: Float, talking: Boolean, isLearner: Boolean, alpha: Float, name: String,
    measurer: TextMeasurer, highlight: Boolean,
) {
    val u = height / 10f
    val dir = if (facingRight) 1f else -1f
    val walking = pose == "walk"
    val bounce = if (walking) kotlin.math.abs(sin(phase)) * u * 0.25f else breathe * u * 0.08f
    val hip = Offset(x, ground - u * 4f - bounce)
    val shoulder = Offset(x, ground - u * 6.9f - bounce)
    val headC = Offset(x, ground - u * 8.2f - bounce)
    val skin = Color(0xFFF2D2B6).copy(alpha = alpha)
    val bodyC = (if (isLearner) Color(0xFF3F7F7A) else body).copy(alpha = alpha)
    val legC = Color(0xFF3B3632).copy(alpha = alpha)
    val stroke = u * 0.75f

    // Ground shadow.
    drawOval(Color(0x22000000).copy(alpha = 0.13f * alpha), Offset(x - u * 1.6f, ground - u * 0.25f), Size(u * 3.2f, u * 0.5f))
    if (highlight) drawCircle(Color(0x33E9B949), radius = u * 4.6f, center = Offset(x, ground - u * 5f))

    // Legs.
    val swing = if (walking) sin(phase) * u * 1.1f else 0f
    drawLine(legC, hip + Offset(-u * 0.4f, 0f), Offset(x - u * 0.5f + swing, ground - u * 0.2f), stroke, StrokeCap.Round)
    drawLine(legC, hip + Offset(u * 0.4f, 0f), Offset(x + u * 0.5f - swing, ground - u * 0.2f), stroke, StrokeCap.Round)

    // Backpack for the learner.
    if (isLearner) drawRoundRect(Color(0xFFE9B949).copy(alpha = alpha), Offset(x - dir * u * 1.9f - u * 0.6f, shoulder.y + u * 0.3f), Size(u * 1.2f, u * 2.2f), CornerRadius(u * 0.4f, u * 0.4f))

    // Torso.
    drawRoundRect(bodyC, Offset(x - u * 1.25f, shoulder.y - u * 0.2f), Size(u * 2.5f, hip.y - shoulder.y + u * 0.6f), CornerRadius(u * 0.9f, u * 0.9f))

    // Arms by pose.
    val armC = bodyC.darker(0.9f)
    val sL = shoulder + Offset(-u * 1.1f, u * 0.3f)
    val sR = shoulder + Offset(u * 1.1f, u * 0.3f)
    val front = if (facingRight) sR else sL
    val back = if (facingRight) sL else sR
    fun arm(from: Offset, to: Offset) {
        drawLine(armC, from, to, stroke * 0.85f, StrokeCap.Round)
        drawCircle(skin, u * 0.38f, to)
    }
    when (pose) {
        "wave" -> {
            arm(back, back + Offset(0f, u * 2.4f))
            val wobble = sin(phase * 2f) * u * 0.4f
            arm(front, front + Offset(dir * u * 1.2f + wobble, -u * 2.2f))
        }
        "point" -> {
            arm(back, back + Offset(0f, u * 2.4f))
            arm(front, front + Offset(dir * u * 2.8f, -u * 0.5f))
        }
        "think" -> {
            arm(back, back + Offset(0f, u * 2.4f))
            arm(front, headC + Offset(dir * u * 0.3f, u * 1.3f))
        }
        "walk" -> {
            arm(sL, sL + Offset(-sin(phase) * u * 0.9f, u * 2.3f))
            arm(sR, sR + Offset(sin(phase) * u * 0.9f, u * 2.3f))
        }
        else -> {
            arm(sL, sL + Offset(-u * 0.3f, u * 2.4f))
            arm(sR, sR + Offset(u * 0.3f, u * 2.4f))
        }
    }

    // Head, hair, face.
    drawCircle(skin, u * 1.25f, headC)
    val hairC = hair.copy(alpha = alpha)
    drawArc(hairC, 180f, 180f, true, headC - Offset(u * 1.32f, u * 1.4f), Size(u * 2.64f, u * 2.2f))
    if (!isLearner) drawOval(hairC, headC + Offset(-dir * u * 1.35f - u * 0.35f, -u * 0.6f), Size(u * 0.7f, u * 1.7f))
    val eyeY = headC.y + u * 0.05f
    drawCircle(Color(0xFF2F2A26).copy(alpha = alpha), u * 0.13f, Offset(x + dir * u * 0.15f, eyeY))
    drawCircle(Color(0xFF2F2A26).copy(alpha = alpha), u * 0.13f, Offset(x + dir * u * 0.7f, eyeY))
    drawCircle(Color(0x55E07A6A).copy(alpha = 0.33f * alpha), u * 0.22f, Offset(x + dir * u * 0.95f, eyeY + u * 0.4f))
    val mouth = Offset(x + dir * u * 0.45f, headC.y + u * 0.6f)
    if (talking) {
        val open = (0.15f + 0.2f * kotlin.math.abs(sin(phase * 2.5f))) * u
        drawOval(Color(0xFF8C3B2E).copy(alpha = alpha), mouth - Offset(u * 0.22f, open / 2), Size(u * 0.44f, open))
    } else {
        drawArc(Color(0xFF8C3B2E).copy(alpha = alpha), 20f, 140f, false, mouth - Offset(u * 0.25f, u * 0.2f), Size(u * 0.5f, u * 0.3f), style = Stroke(u * 0.12f))
    }
    if (pose == "think") rotate(-12f, headC + Offset(dir * u * 1.6f, -u * 1.6f)) {
        label(measurer, "?", headC + Offset(dir * u * 1.8f, -u * 1.6f), signStyle.copy(fontSize = 14.sp, color = Color(0xFFD9673E).copy(alpha = alpha)), null)
    }
    // Name tag.
    label(measurer, name, Offset(x, headC.y - u * 2.3f),
        signStyle.copy(fontSize = 10.sp, color = Color.White.copy(alpha = alpha)),
        (if (isLearner) Color(0xFF3F7F7A) else body.darker(0.8f)).copy(alpha = 0.92f * alpha), 4f)
}
