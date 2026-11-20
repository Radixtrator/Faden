package io.github.radixtrator.resync.ui.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/** How far media can be pinched in. */
private const val MAX_ZOOM = 20f

/** Where a double tap zooms to from 1x. */
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * How zoomed media glides on after a pan is let go, on Android's own fling curve (the spline [android.widget.OverScroller]
 * and Compose's lists decelerate on). [carryX] and [carryY] are the shares of the release speed it keeps sideways and
 * up and down; [friction] brakes it, and Android's lists use 0.015 ([android.view.ViewConfiguration.getScrollFriction]),
 * which glides for about half a second. Media brakes harder, so a pan settles quickly instead of drifting off. Tuned by
 * hand on a phone held upright, where zoomed media has less room sideways, so it carries a little more that way.
 */
internal object ZoomFling {
    const val carryX = 1.3f
    const val carryY = 1f
    const val friction = 0.0375f
}

/** Pinch/double-tap zoom state shared by the image viewer and the video player. */
@Stable
internal class ZoomState(private val onZoomChange: (Boolean) -> Unit = {}) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    val zoomed: Boolean get() = scale > 1f

    /** The box the media fills, which bounds how far it can be panned. */
    internal var bounds = Size.Zero
    private var fling: Job? = null

    /** A touch that stopped a glide is only a stop, not a tap. */
    internal var stoppedFling = false

    fun zoomTo(value: Float) {
        val wasZoomed = zoomed
        scale = value.coerceIn(1f, MAX_ZOOM)
        offset = if (scale <= 1f) Offset.Zero else clamp(offset)
        if (zoomed != wasZoomed) onZoomChange(zoomed)
    }

    /** Pans by [by], kept so the media still covers the box's middle; returns how far it actually moved. */
    fun pan(by: Offset): Offset {
        if (!zoomed) return Offset.Zero
        val before = offset
        offset = clamp(offset + by)
        return offset - before
    }

    /** Double tap: back to 1x when zoomed, otherwise in to [DOUBLE_TAP_ZOOM]. */
    fun toggle() {
        stopFling()
        zoomTo(if (zoomed) 1f else DOUBLE_TAP_ZOOM)
    }

    /** Stops a glide; true if one was running. */
    internal fun stopFling(): Boolean {
        val running = fling?.isActive == true
        fling?.cancel()
        fling = null
        return running
    }

    internal fun startFling(job: Job) {
        stopFling()
        fling = job
    }

    // Scaled about its middle, the media reaches (scale - 1) / 2 of the box past each edge.
    private fun clamp(o: Offset): Offset {
        if (bounds == Size.Zero) return o
        val maxX = (scale - 1f) * bounds.width / 2f
        val maxY = (scale - 1f) * bounds.height / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }
}

/**
 * Taps, double taps, long presses and pinches for [state]. Single-finger drags are left alone at 1x (for a pager or
 * swipe-to-dismiss) and pan once zoomed; let go of a pan and the media glides on and slows to a stop ([ZoomFling]).
 * A tap that stops a glide doesn't count as a tap.
 */
internal fun Modifier.zoomGestures(state: ZoomState, onTap: () -> Unit, onLongPress: (() -> Unit)? = null): Modifier = this
    .onSizeChanged { state.bounds = it.toSize() }
    .pointerInput(state, onLongPress != null) {
        detectTapGestures(
            onTap = { if (state.stoppedFling) state.stoppedFling = false else onTap() },
            onDoubleTap = { state.toggle() },
            onLongPress = onLongPress?.let { press -> { press() } },
        )
    }
    .pointerInput(state) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                state.stoppedFling = state.stopFling()
                val tracker = VelocityTracker()
                var panned = false
                var pinched = false
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.size > 1) pinched = true
                    if (event.changes.size > 1 || state.zoomed) {
                        state.zoomTo(state.scale * event.calculateZoom())
                        val pan = event.calculatePan()
                        state.pan(pan)
                        if (pan != Offset.Zero) panned = true
                        event.changes.firstOrNull { it.id == down.id }?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                // A pinch's lift-off isn't a flick, so only one-finger pans glide on.
                if (panned && !pinched && state.zoomed) {
                    val v = tracker.calculateVelocity()
                    val velocity = Offset(v.x * ZoomFling.carryX, v.y * ZoomFling.carryY)
                    state.startFling(launch { glide(state, velocity, density) })
                }
            }
        }
    }

/**
 * Moves [state] along [velocity] (px/s) the way [android.widget.OverScroller.fling] would: a distance and duration
 * from the speed and [ZoomFling.friction], then eased along Android's fling spline. Stops early at the edges.
 */
private suspend fun glide(state: ZoomState, velocity: Offset, density: Float) {
    val speed = velocity.getDistance()
    if (speed < MIN_FLING_SPEED * density) return
    val direction = velocity / speed
    val coefficient = PHYSICAL_COEFFICIENT * density * 160f
    val friction = ZoomFling.friction
    val l = ln(INFLEXION * speed / (friction * coefficient))
    val duration = exp(l / (DECELERATION_RATE - 1f)) * 1_000_000_000f
    val distance = friction * coefficient * exp(DECELERATION_RATE / (DECELERATION_RATE - 1f) * l)
    var travelled = 0f
    val start = withFrameNanos { it }
    while (true) {
        val elapsed = withFrameNanos { it } - start
        val t = (elapsed / duration).coerceAtMost(1f)
        val target = distance * FlingSpline.position(t)
        val moved = state.pan(direction * (target - travelled))
        travelled = target
        if (t >= 1f || (moved.x == 0f && moved.y == 0f && target > 0f)) return
    }
}

/** Slower than this (dp/s) and a pan just stops where it was let go. */
private const val MIN_FLING_SPEED = 50f

// OverScroller's constants: the inflexion point of the spline, the deceleration exponent, and gravity × inches per
// meter × a fudge factor, which times the screen's pixels per inch turns friction into px/s².
private const val INFLEXION = 0.35f
private val DECELERATION_RATE = (ln(0.78) / ln(0.9)).toFloat()
private const val PHYSICAL_COEFFICIENT = 9.80665f * 39.37f * 0.84f

/** OverScroller's fling spline: the share of the distance covered after a share of the time. */
internal object FlingSpline {
    private const val SAMPLES = 100
    private val positions = FloatArray(SAMPLES + 1).also { table ->
        val p1 = 0.5f * INFLEXION
        val p2 = 1f - 1f * (1f - INFLEXION)
        var xMin = 0f
        for (i in 0 until SAMPLES) {
            val alpha = i.toFloat() / SAMPLES
            var xMax = 1f
            var x: Float
            var coef: Float
            while (true) {
                x = xMin + (xMax - xMin) / 2f
                coef = 3f * x * (1f - x)
                val tx = coef * ((1f - x) * p1 + x * p2) + x * x * x
                if (abs(tx - alpha) < 1e-5f) break
                if (tx > alpha) xMax = x else xMin = x
            }
            table[i] = coef * ((1f - x) * 0.5f + x) + x * x * x
        }
        table[SAMPLES] = 1f
    }

    fun position(t: Float): Float {
        val index = (SAMPLES * t).toInt().coerceIn(0, SAMPLES - 1)
        val t0 = index.toFloat() / SAMPLES
        val t1 = (index + 1).toFloat() / SAMPLES
        return positions[index] + (t - t0) / (t1 - t0) * (positions[index + 1] - positions[index])
    }
}

/** Draws the content scaled and panned by [state]. */
internal fun Modifier.zoomTransform(state: ZoomState): Modifier = graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offset.x
    translationY = state.offset.y
}
