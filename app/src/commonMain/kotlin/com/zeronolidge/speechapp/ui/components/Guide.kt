package com.zeronolidge.speechapp.ui.components

import com.zeronolidge.speechapp.ui.art.loop
import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Children who can't read yet need to *see* what to tap. These helpers make the
 * next thing to press pulse, point at it with a bouncing hand, and say so out loud.
 */

/** A big green "go" arrow on raised paper that gently pulses, with a hand pointing at it. */
@Composable
fun GoButton(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    enabled: Boolean = true,
    /** Pulse and point at it. Off when it's only a "skip", so it doesn't pull attention. */
    hint: Boolean = true,
    onClick: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "go")
    val pulse by t.loop(1f, 1.1f, 550, label = "pulse")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PaperButton(
            PaperColorsUi.go,
            Modifier.size(size).scale(if (enabled && hint) pulse else 1f),
            circle = true,
            enabled = enabled,
            onClick = onClick,
        ) {
            ArrowIcon(size * 0.44f)
        }
        if (enabled && hint) PointingHand(Modifier.offset(y = 4.dp), size = size * 0.5f)
    }
}

/** A hand that bobs up and down, pointing at whatever is above it. */
@Composable
fun PointingHand(modifier: Modifier = Modifier, size: Dp = 48.dp, pointDown: Boolean = false, pointRight: Boolean = false, pointLeft: Boolean = false) {
    val t = rememberInfiniteTransition(label = "hand")
    val bob by t.loop(0f, 1f, 450, label = "bob")
    val shift = size * 0.25f * bob
    // test48: Rory's own paper-robot hand (tan cardboard, three boxy fingers, cyan cuff + rivet).
    val turn = when { pointRight -> 90f; pointLeft -> -90f; pointDown -> 180f; else -> 0f }
    androidx.compose.foundation.Canvas(
        when {
            pointRight -> modifier.offset(x = shift)
            pointLeft -> modifier.offset(x = -shift)
            pointDown -> modifier.offset(y = shift)
            else -> modifier.offset(y = -shift)
        }.size(size).graphicsLayer { rotationZ = turn },
    ) { drawRoryHand() }
}

/** Rory's pointing hand, finger up, filling this canvas (see [PointingHand]). */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoryHand() = drawRoryHand(androidx.compose.ui.geometry.Offset.Zero, size.width, size.height, 1f)

/**
 * Rory's hand, finger up, in the [w] x [h] box at [o], at [alpha]. test48 (Ruby, Timothy): his own
 * paper-robot hand, cut from the same tan cardboard as his body (never orange or skin): a boxy
 * palm with exactly three square-cut cardboard fingers (the middle one raised to point), a thick
 * white paper outline, and a cyan wrist cuff with one small cyan rivet. Cyan only on the cuff.
 * Every hint and demo hand (the bouncing PointingHand, the map, the kitchen's peel/wash/slice/
 * stir demos) draws through here, so it is the same hand everywhere.
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoryHand(o: androidx.compose.ui.geometry.Offset, w: Float, h: Float, alpha: Float) = translate(o.x, o.y) {
    if (alpha <= 0f) return@translate
    val body = RoryCardboard
    val shade = androidx.compose.ui.graphics.Color(0xFFC9955A)
    val cyan = androidx.compose.ui.graphics.Color(0xFF4DD0E1)
    val teal = androidx.compose.ui.graphics.Color(0xFF00838F)
    val white = androidx.compose.ui.graphics.Color.White
    fun box(x0: Float, y0: Float, x1: Float, y1: Float, grow: Float, fill: androidx.compose.ui.graphics.Color, r: Float = 0.035f) =
        drawRoundRect(
            fill, androidx.compose.ui.geometry.Offset(w * x0 - grow, h * y0 - grow),
            androidx.compose.ui.geometry.Size(w * (x1 - x0) + 2 * grow, h * (y1 - y0) + 2 * grow),
            androidx.compose.ui.geometry.CornerRadius(w * r + grow * 0.6f),
        )
    val left = floatArrayOf(0.17f, 0.28f, 0.37f, 0.6f)
    val right = floatArrayOf(0.63f, 0.31f, 0.83f, 0.6f)
    val middle = floatArrayOf(0.39f, 0.04f, 0.61f, 0.6f) // raised: it points
    val fingers = listOf(left, right, middle)
    // Drawn whole into one layer, then faded together, so a fading hand never shows its
    // white outline through the cardboard.
    val bounds = androidx.compose.ui.geometry.Rect(-w * 0.1f, -h * 0.1f, w * 1.1f, h * 1.1f)
    drawContext.canvas.saveLayer(bounds, androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha })
    // One thick white paper outline round the whole hand...
    val thick = w * 0.075f
    for (f in fingers) box(f[0], f[1], f[2], f[3], thick, white)
    box(0.17f, 0.5f, 0.83f, 0.78f, thick, white, 0.06f)
    box(0.24f, 0.75f, 0.76f, 0.95f, thick, white, 0.04f)
    // ...then the palm and cuff, and each of the three boxy fingers as its own cut-out with a thin
    // white edge, so they read as three separate cardboard fingers.
    box(0.17f, 0.5f, 0.83f, 0.78f, 0f, body, 0.06f)
    box(0.24f, 0.75f, 0.76f, 0.95f, 0f, cyan, 0.04f)
    val thin = w * 0.03f
    for (f in fingers) {
        box(f[0], f[1], f[2], f[3], thin, white)
        box(f[0], f[1], f[2], f[3], 0f, body)
    }
    val cap = androidx.compose.ui.graphics.StrokeCap.Round
    // two knuckle creases on the pointing finger
    for (y in listOf(0.2f, 0.36f)) drawLine(shade, androidx.compose.ui.geometry.Offset(w * 0.45f, h * y), androidx.compose.ui.geometry.Offset(w * 0.55f, h * y), w * 0.024f, cap = cap)
    // The small cyan rivet at the wrist (a teal rim and a white glint, so it shows on the cyan cuff).
    val rc = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.85f)
    drawCircle(teal, w * 0.075f, rc)
    drawCircle(cyan, w * 0.055f, rc)
    drawCircle(white, w * 0.018f, rc + androidx.compose.ui.geometry.Offset(-w * 0.018f, -w * 0.018f))
    drawContext.canvas.restore()
}

/** Rory's tan cardboard: his body's own paper colour, shared by his hand and his antenna stalk. */
val RoryCardboard = androidx.compose.ui.graphics.Color(0xFFFFCC80)

/**
 * Says [text] again every [everyMs] while the screen is showing, if nothing else is being said,
 * up to [times] times. Gives a gentle reminder when a child isn't sure what to do.
 */
@Composable
fun Nudge(key: Any, text: String, everyMs: Long = 9000, times: Int = 3) {
    val app = LocalApp.current
    LaunchedEffect(key) {
        repeat(times) {
            delay(everyMs)
            if (!app.speaker.speaking) app.speaker.say(text)
        }
    }
}

/** The continue button's size, the same on every step. */
val ContinueSize = 96.dp

/** Room a step leaves at its bottom-right for the continue button and the hand beside it. */
val ContinueReserve = ContinueSize + 76.dp

/** Where the current step's continue action lives, so the button can always be drawn in one spot. */
class ContinueSlot {
    internal var onClick by mutableStateOf<(() -> Unit)?>(null)
    internal var label by mutableStateOf<String?>(null)
    internal var hint by mutableStateOf(true)
    /** Narration steps: a tap anywhere on the step (not on a button) also continues. */
    internal var tapAnywhere by mutableStateOf(false)
    /** The child has tapped something since this step started: the hand hides. */
    internal var touched by mutableStateOf(false)
    internal var owner: Any? = null
    /** When the last continue tap was taken: a second tap in the moment after is ignored. */
    internal var lastTap: kotlin.time.TimeMark? = null
    /** When the labelled button ("I said it!") appeared: taps in its first half second are ignored. */
    internal var labelSince: kotlin.time.TimeMark? = null
}

val LocalContinue = staticCompositionLocalOf<ContinueSlot?> { null }

/**
 * Holds the steps of a chapter (or a practice word) and draws the one continue button: always
 * the same size, pinned to the same bottom-right spot inside the screen edges. Steps say what it
 * does with [ContinueAction].
 */
/**
 * How long after a continue tap the next one is ignored (a double tap mustn't skip two steps).
 * Measured from the last tap taken, not from when the button appeared: a first tap on a button
 * that just popped up always counts.
 */
private const val SettleMs = 700L

/** How long a newly shown "I said it!" ignores taps. */
private const val LabelSettleMs = 500L

@Composable
fun ContinueHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val slot = remember { ContinueSlot() }
    val app = LocalApp.current
    // A quick second tap on the same spot must not skip the step that just opened.
    val action = slot.onClick?.let { a ->
        tap@{
            // The first second of a listening turn: taps don't count yet.
            if (app.tapsSettling) return@tap
            // A tap still landing from a start tap mustn't count as "I said it!" (it appears where
            // the green arrow was): its first half second is ignored.
            val fresh = slot.label != null && (slot.labelSince?.elapsedNow()?.inWholeMilliseconds ?: LabelSettleMs) < LabelSettleMs
            if (!fresh && (slot.lastTap?.elapsedNow()?.inWholeMilliseconds ?: SettleMs) >= SettleMs) {
                slot.lastTap = kotlin.time.TimeSource.Monotonic.markNow()
                a()
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            // Notice any tap (without taking it away from what was tapped), so the hand hides.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.changes.any { it.pressed }) slot.touched = true
                    }
                }
            },
    ) {
        // Behind the step: on narration, a tap on any empty part of the screen continues.
        if (action != null && slot.tapAnywhere) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { action() })
        }
        CompositionLocalProvider(LocalContinue provides slot) { content() }
        if (action != null) {
            // The hand sits beside the button (in the step's reserved corner), never over anything else.
            Row(Modifier.align(Alignment.BottomEnd).zIndex(5f), verticalAlignment = Alignment.CenterVertically) {
                if (slot.hint && !slot.touched) PointingHand(Modifier.padding(end = 6.dp), size = 52.dp, pointRight = true)
                // A labelled button ("I said it!") shows only on his turn, with the listening look;
                // otherwise (Rory talking) it's the plain green arrow, doing the same.
                val label = slot.label?.takeIf { app.listening }
                if (label == null) {
                    GoButton(Modifier.popIn("go"), size = ContinueSize, hint = false, onClick = action)
                } else {
                    // A wide green "I said it!" button in the same spot.
                    PaperButton(PaperColorsUi.go, Modifier.height(ContinueSize).popIn(label), corner = 999.dp, onClick = action) {
                        Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                            ArrowIcon(34.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(label, fontSize = 26.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The current step's continue action. After [delayMs] (so the child can listen first) the big
 * continue button appears bottom-right, whatever the audio is doing. [label] turns it into a
 * wide labelled button (e.g. "I said it!"). Outside a [ContinueHost] it draws its own button.
 */
@Composable
fun ContinueAction(
    enabled: Boolean = true,
    hint: Boolean = true,
    label: String? = null,
    delayMs: Long = 0,
    /** Narration steps: a tap anywhere on the screen (not on a button) also continues. */
    tapAnywhere: Boolean = false,
    onClick: () -> Unit,
) {
    val slot = LocalContinue.current
    var shown by remember { mutableStateOf(delayMs <= 0L) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) {
            delay(delayMs)
            shown = true
        }
    }
    val latest by rememberUpdatedState(onClick)
    // One stable lambda, so registering it doesn't make the host redraw over and over.
    val stable = remember { { latest() } }
    val token = remember { Any() }
    if (slot == null) {
        if (shown && enabled) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                GoButton(size = ContinueSize, hint = hint, onClick = stable)
            }
        }
        return
    }
    // Read here, during composition, so the button registers the moment the delay is over.
    val visible = shown && enabled
    SideEffect {
        if (visible) {
            if (slot.owner !== token) slot.touched = false
            if (label != null && (slot.owner !== token || slot.label != label)) slot.labelSince = kotlin.time.TimeSource.Monotonic.markNow()
            slot.owner = token
            slot.onClick = stable
            slot.label = label
            slot.hint = hint
            slot.tapAnywhere = tapAnywhere
        } else if (slot.owner === token) {
            slot.onClick = null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (slot.owner === token) {
                slot.owner = null
                slot.onClick = null
                slot.label = null
            }
        }
    }
}

/** For other files' DrawScope extensions: [drawRoryHand] on [scope]. */
fun drawRoryHandOn(scope: androidx.compose.ui.graphics.drawscope.DrawScope, o: androidx.compose.ui.geometry.Offset, w: Float, h: Float, alpha: Float) = scope.drawRoryHand(o, w, h, alpha)
