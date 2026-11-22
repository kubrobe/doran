package com.doranalley.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.doranalley.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motion rules for Doran ("calm when learning, exciting when achieving"):
 * - Everyday motion answers the learner: press (120 ms), state change (200 ms), screen (320 ms).
 * - Rich motion is reserved for wins (lesson / unit complete, streak, a great score). It runs once and ends.
 * - Nothing decorative loops forever. The only loops are tied to a real process (audio playing,
 *   recording, the speech checker working) and stop with it.
 * - Entrances only: a screen never animates out while the next one is on screen, so a tap can't
 *   land on a screen that is leaving.
 * - [LocalReduceMotion] (in-app setting or phone animations off) turns entrances, pops, shakes and
 *   celebrations off; state changes still show, just without movement.
 */
object Motion {
    const val PRESS = 120
    const val STATE = 200
    const val SCREEN = 320

    val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseInOut = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
}

/** Shrinks content slightly while it is pressed. */
fun Modifier.pressScale(source: InteractionSource, enabled: Boolean = true, scale: Float = 0.97f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val target = if (pressed && enabled && !reduce) scale else 1f
    val s by androidx.compose.animation.core.animateFloatAsState(target, tween(Motion.PRESS, easing = Motion.EaseOut), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Fades content in while lifting it [distance] once, when it first appears. */
fun Modifier.enterRise(delayMs: Int = 0, distance: Dp = 12.dp, durationMs: Int = Motion.SCREEN): Modifier = composed {
    if (LocalReduceMotion.current) return@composed Modifier
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, delayMs, Motion.EaseOut)) }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * distance.toPx()
    }
}

/** Slides content in from the side once (used for each new exercise). */
fun Modifier.enterSlide(distance: Dp = 28.dp, durationMs: Int = 280): Modifier = composed {
    if (LocalReduceMotion.current) return@composed Modifier
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, 0, Motion.EaseOut)) }
    graphicsLayer {
        alpha = a.value
        translationX = (1f - a.value) * distance.toPx()
    }
}

/** Grows content from [from] with a small overshoot once, when it first appears. */
fun Modifier.enterPop(delayMs: Int = 0, from: Float = 0.6f, durationMs: Int = 360): Modifier = composed {
    if (LocalReduceMotion.current) return@composed Modifier
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMs, delayMs, Motion.EaseOutBack)) }
    graphicsLayer {
        alpha = a.value.coerceIn(0f, 1f)
        val sc = from + (1f - from) * a.value
        scaleX = sc; scaleY = sc
    }
}

/** A horizontal "no" shake, in dp. Use with Modifier.graphicsLayer { translationX = anim.value.dp.toPx() }. */
suspend fun Animatable<Float, AnimationVector1D>.shake(strength: Float = 10f) {
    snapTo(0f)
    animateTo(0f, keyframes {
        durationMillis = 360
        -strength at 45
        strength * 0.8f at 110
        -strength * 0.5f at 180
        strength * 0.3f at 250
    })
}

/** A quick "yes" pop: grows a little and springs back. */
suspend fun Animatable<Float, AnimationVector1D>.pop(peak: Float = 1.06f) {
    snapTo(1f)
    animateTo(peak, tween(90, easing = Motion.EaseOut))
    animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
}

/** Counts up from 0 to [target] once (shows the final value at once with reduced motion). */
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
    val reduce = LocalReduceMotion.current
    val a = remember { Animatable(if (reduce) target.toFloat() else 0f) }
    LaunchedEffect(target, reduce) {
        if (reduce) a.snapTo(target.toFloat()) else a.animateTo(target.toFloat(), tween(durationMs, delayMs, Motion.EaseOut))
    }
    Text(format(a.value.roundToInt()), modifier, style = style, color = color)
}

private class Bit(
    val vx: Float, val vy: Float, val color: Color, val w: Float, val h: Float,
    val spin: Float, val flip: Float, val delay: Float, val round: Boolean,
)

/**
 * A one-off burst of paper confetti falling with gravity. Decorative only: it draws nothing once
 * finished, and nothing at all with reduced motion.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, colors: List<Color>, count: Int = 70, seed: Int = 7, originY: Float = 0.3f, durationMs: Int = 2400) {
    if (LocalReduceMotion.current) return
    val t = remember { Animatable(0f) }
    val done = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(durationMs, easing = LinearEasing)); done.value = true }
    val bits = remember(seed, count) {
        val r = Random(seed)
        List(count) {
            val angle = (-PI / 2 + (r.nextFloat() - 0.5f) * PI * 0.9).toFloat()
            val speed = 0.5f + r.nextFloat() * 0.7f
            Bit(
                vx = cos(angle) * speed, vy = sin(angle) * speed * 1.2f,
                color = colors[r.nextInt(colors.size)],
                w = 6f + r.nextFloat() * 6f, h = 9f + r.nextFloat() * 8f,
                spin = (r.nextFloat() - 0.5f) * 800f, flip = 4f + r.nextFloat() * 8f,
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
