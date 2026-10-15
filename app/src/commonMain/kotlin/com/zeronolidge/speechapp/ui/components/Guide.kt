package com.zeronolidge.speechapp.ui.components

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
    val pulse by t.animateFloat(1f, 1.1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse")
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
fun PointingHand(modifier: Modifier = Modifier, size: Dp = 48.dp, pointDown: Boolean = false, pointRight: Boolean = false) {
    val t = rememberInfiniteTransition(label = "hand")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "bob")
    val shift = size * 0.25f * bob
    Text(
        when {
            pointRight -> "👉"
            pointDown -> "👇"
            else -> "👆"
        },
        fontSize = size.value.sp,
        modifier = when {
            pointRight -> modifier.offset(x = shift)
            pointDown -> modifier.offset(y = shift)
            else -> modifier.offset(y = -shift)
        },
    )
}

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
}

val LocalContinue = staticCompositionLocalOf<ContinueSlot?> { null }

/**
 * Holds the steps of a chapter (or a practice word) and draws the one continue button: always
 * the same size, pinned to the same bottom-right spot inside the screen edges. Steps say what it
 * does with [ContinueAction].
 */
@Composable
fun ContinueHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val slot = remember { ContinueSlot() }
    val action = slot.onClick
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
                val label = slot.label
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
    SideEffect {
        if (shown && enabled) {
            if (slot.owner !== token) slot.touched = false
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
