package com.doranalley.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motion rules for Doran:
 * - Motion answers the learner (press, pick, right / wrong, hearts, score). Idle motion is limited to Horangi.
 * - Everything here is either finite or an infinite transition, so UI tests can still reach "idle",
 *   and Compose scales all of it by the phone's animation setting (off = no motion).
 * - Entrances only: a screen never animates out while the next one is on screen, so a tap can't
 *   land on a screen that is leaving.
 */
object Motion {
    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseInOut = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
}

/** Fades content in while lifting it [distance] once, when it first appears. */
fun Modifier.enterRise(delayMs: Int = 0, distance: Dp = 14.dp, durationMs: Int = 340): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, delayMs, Motion.EaseOut)) }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * distance.toPx()
    }
}

/** Slides content in from the side once (used for each new exercise). */
fun Modifier.enterSlide(distance: Dp = 36.dp, durationMs: Int = 300): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, 0, Motion.EaseOut)) }
    graphicsLayer {
        alpha = a.value
        translationX = (1f - a.value) * distance.toPx()
    }
}

/** Grows content from [from] with a small overshoot once, when it first appears. */
fun Modifier.enterPop(delayMs: Int = 0, from: Float = 0.6f, durationMs: Int = 420): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, delayMs, Motion.EaseOutBack)) }
    graphicsLayer {
        alpha = a.value.coerceIn(0f, 1f)
        val sc = from + (1f - from) * a.value
        scaleX = sc; scaleY = sc
    }
}

/** A horizontal "no" shake, in dp. Use with Modifier.graphicsLayer { translationX = anim.value.dp.toPx() }. */
suspend fun Animatable<Float, AnimationVector1D>.shake(strength: Float = 12f) {
    snapTo(0f)
    animateTo(0f, keyframes {
        durationMillis = 420
        -strength at 50
        strength * 0.85f at 120
        -strength * 0.6f at 190
        strength * 0.4f at 260
        -strength * 0.2f at 330
    })
}

/** A quick "yes" pop: grows a little and springs back. */
suspend fun Animatable<Float, AnimationVector1D>.pop(peak: Float = 1.08f) {
    snapTo(1f)
    animateTo(peak, tween(90, easing = Motion.EaseOut))
    animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
}

/** Counts up from 0 to [target] once. */
@Composable
fun CountUpText(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMs: Int = 900,
    delayMs: Int = 0,
    format: (Int) -> String = { "$it" },
) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target.toFloat(), tween(durationMs, delayMs, Motion.EaseOut)) }
    Text(format(a.value.roundToInt()), modifier, style = style, color = color)
}

private class Bit(
    val vx: Float, val vy: Float, val color: Color, val w: Float, val h: Float,
    val spin: Float, val flip: Float, val delay: Float, val round: Boolean,
)

/**
 * A one-off burst of paper confetti in the traditional five colours, falling with gravity.
 * Decorative only; it draws nothing once finished.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, colors: List<Color>, count: Int = 80, seed: Int = 7, originY: Float = 0.3f, durationMs: Int = 2800) {
    val t = remember { Animatable(0f) }
    val done = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(durationMs, easing = LinearEasing)); done.value = true }
    val bits = remember(seed, count) {
        val r = Random(seed)
        List(count) {
            val angle = (-PI / 2 + (r.nextFloat() - 0.5f) * PI * 0.95).toFloat()
            val speed = 0.55f + r.nextFloat() * 0.75f
            Bit(
                vx = cos(angle) * speed, vy = sin(angle) * speed * 1.25f,
                color = colors[r.nextInt(colors.size)],
                w = 7f + r.nextFloat() * 7f, h = 10f + r.nextFloat() * 9f,
                spin = (r.nextFloat() - 0.5f) * 900f, flip = 4f + r.nextFloat() * 8f,
                delay = r.nextFloat() * 0.12f, round = r.nextInt(5) == 0,
            )
        }
    }
    if (done.value) return
    Canvas(modifier) {
        val total = durationMs / 1000f
        val w = size.width
        val origin = Offset(w / 2f, size.height * originY)
        val dp = density
        bits.forEach { b ->
            val time = t.value * total - b.delay
            if (time <= 0f) return@forEach
            val drag = 1f / (1f + time * 0.9f)
            val x = origin.x + b.vx * w * time * drag
            val y = origin.y + b.vy * w * time * drag + 0.55f * w * time * time
            val fade = ((total - time) / (total * 0.3f)).coerceIn(0f, 1f)
            if (fade <= 0f || y > size.height + 40f) return@forEach
            val c = b.color.copy(alpha = fade)
            val squash = abs(cos(time * b.flip))
            rotate(b.spin * time, Offset(x, y)) {
                if (b.round) drawCircle(c, b.w * dp * 0.45f, Offset(x, y))
                else drawRect(c, Offset(x - b.w * dp / 2, y - b.h * dp * squash / 2), Size(b.w * dp, (b.h * dp * squash).coerceAtLeast(1f)))
            }
        }
    }
}
