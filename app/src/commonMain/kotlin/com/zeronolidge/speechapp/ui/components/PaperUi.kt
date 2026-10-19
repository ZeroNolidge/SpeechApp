package com.zeronolidge.speechapp.ui.components

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.zeronolidge.speechapp.ui.art.loop
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.MaxMotionMs
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import com.zeronolidge.speechapp.ui.art.paperPath
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.ui.art.PaperGrain
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.art.raisedPaperBase
import com.zeronolidge.speechapp.ui.art.raisedPaperFace
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.delay

/**
 * Shared kid-facing building blocks, so every screen looks and behaves the same:
 * raised paper buttons, plain paper chips for words, the home and "hear it again" buttons
 * in the same corners, and short paper-like animations (pop up, unfold, flip).
 */

/** The size of the corner buttons (home, hear it again). Thumb-sized and then some. */
val CornerButtonSize = 80.dp

/**
 * Anything the child can tap: a thick, raised cut-out with a drop shadow that presses down
 * onto the table under a finger. Disabled buttons lie flat and faded.
 */
@Composable
fun PaperButton(
    color: Color,
    modifier: Modifier = Modifier,
    corner: Dp = PaperStyle.corner,
    circle: Boolean = false,
    enabled: Boolean = true,
    /**
     * For a button whose taps are handled by a parent (the map's next stop, which the map's one
     * tap handler owns): it draws pressed while this is true and takes no taps itself.
     */
    pressedBy: Boolean? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ownPress by interaction.collectIsPressedAsState()
    val pressed = pressedBy ?: ownPress
    // Every press lands with a soft paper crinkle: at the touch itself (kidTap), or for a button
    // a parent handles (the map's stop), when it shows pressed.
    val sfx = LocalApp.current.sfx
    LaunchedEffect(pressed) { if (pressed && enabled && pressedBy != null) sfx.crinkle() }
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "press")
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            // Pressing squashes the whole cut-out down a touch, so every tap visibly lands.
            .graphicsLayer {
                scaleX = 1f - 0.04f * press
                scaleY = 1f - 0.04f * press
            }
            .then(if (enabled) Modifier.raisedPaperBase(color, corner, circle) { press } else Modifier)
            .then(if (pressedBy == null) Modifier.kidTap(interaction, enabled, onClick, onPress = { sfx.crinkle() }) else Modifier),
        propagateMinConstraints = true,
    ) {
        Box(
            Modifier
                .graphicsLayer { translationY = press * PaperStyle.edge.toPx() }
                .raisedPaperFace(color, corner, circle),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** A word, letter or number on a plain paper chip: crisp dark ink, no texture over it. */
@Composable
fun PaperChip(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    color: Color = PaperStyle.chip,
    textColor: Color = PaperStyle.ink,
    maxLines: Int = Int.MAX_VALUE,
    horizontalPadding: Dp = 14.dp,
) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        modifier = modifier
            .paperCard(color, 999.dp)
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
    )
}

/**
 * A text size that lets [label] fit in [width] without breaking a word across lines
 * (at most two lines, split at spaces), from [max] down to 12.
 */
fun fittingFontSize(label: String, width: Dp, max: Float = 20f): TextUnit {
    val longestWord = label.split(' ').maxOfOrNull { it.length } ?: 1
    // Sniglet's letters are roughly 0.62 of the font size wide.
    val fits = width.value / (longestWord.coerceAtLeast(3) * 0.62f)
    return fits.coerceIn(12f, max).sp
}

/** An emoji icon on a raised paper button. */
@Composable
fun PaperIconButton(
    emoji: String,
    color: Color,
    size: Dp = CornerButtonSize,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PaperButton(color, modifier.size(size), circle = true, enabled = enabled, onClick = onClick) {
        Text(emoji, fontSize = (size.value * 0.46f).sp)
    }
}

/**
 * A play / forward arrow (or back, with [pointLeft]) drawn as a paper triangle. Browsers show the
 * ▶ character as a little blue emoji tile, so arrows are always drawn instead.
 */
@Composable
fun ArrowIcon(size: Dp, modifier: Modifier = Modifier, color: Color = Color.White, pointLeft: Boolean = false) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Nudged towards the point so it looks centred in a round button.
        val tri = Path().apply {
            if (pointLeft) {
                moveTo(w * 0.22f, h * 0.5f); lineTo(w * 0.86f, h * 0.12f); lineTo(w * 0.86f, h * 0.88f)
            } else {
                moveTo(w * 0.78f, h * 0.5f); lineTo(w * 0.14f, h * 0.12f); lineTo(w * 0.14f, h * 0.88f)
            }
            close()
        }
        paperPath(tri, color)
    }
}

/** A round raised paper button with a drawn arrow, for "go", "more" and "back a page". */
@Composable
fun PaperArrowButton(
    color: Color,
    size: Dp = CornerButtonSize,
    pointLeft: Boolean = false,
    arrowColor: Color = PaperStyle.ink,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PaperButton(color, modifier.size(size), circle = true, enabled = enabled, onClick = onClick) {
        ArrowIcon(size * 0.42f, color = arrowColor, pointLeft = pointLeft)
    }
}

/** Home: always the top-left corner, the same size and colour on every screen. */
@Composable
fun HomeButton(onHome: () -> Unit) {
    val app = LocalApp.current
    PaperIconButton("🏠", PaperColorsUi.home) { app.speaker.stop(); onHome() }
}

/** Back one screen: sits right next to 🏠, only where "back" goes somewhere other than the map. */
@Composable
fun BackButton(onBack: () -> Unit) {
    val app = LocalApp.current
    PaperIconButton("⬅️", PaperColorsUi.back, size = 72.dp) { app.speaker.stop(); onBack() }
}

/**
 * "Hear it again": always the top-right corner. Says the screen's instruction again. If speech
 * didn't start or finish in time, it pulses in its own spot (so it never covers anything) until
 * it's tapped.
 */
@Composable
fun HearAgainButton() {
    val app = LocalApp.current
    val trouble = app.audioTrouble
    val t = rememberInfiniteTransition(label = "hear")
    val pulse by t.loop(1f, 1.12f, 500, label = "hearPulse")
    PaperIconButton(
        "🔊",
        if (trouble) PaperColorsUi.home else PaperColorsUi.hear,
        modifier = Modifier.scale(if (trouble) pulse else 1f),
    ) {
        app.audioTrouble = false
        app.repeatInstruction()
    }
}

/** The colours of the shared controls, picked from the paper palette. */
object PaperColorsUi {
    /** Tomato: the one warm accent, only on things he can tap. */
    val home = Color(0xFFF2643D)
    val back = Color(0xFFFFE0B2)
    val hear = Color(0xFFFFD54F)
    val go = Color(0xFF43A047)
    val choice = Color(0xFFFFFAF0)
}

/** Says [text] when the screen opens and makes it what "hear it again" repeats. */
@Composable
fun SpeakOnOpen(text: String, key: Any = text) {
    val app = LocalApp.current
    LaunchedEffect(key) { app.instruct(text) }
}

/**
 * The frame every screen sits in: 🏠 top-left (or [left] on the map, which is home),
 * 🔊 top-right, an optional strip in between, and the screen below.
 */
@Composable
fun ScreenFrame(
    onHome: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Shows a ⬅️ next to 🏠 that goes back one screen. */
    onBack: (() -> Unit)? = null,
    background: @Composable BoxScope.() -> Unit = { PlainBackdrop() },
    left: (@Composable () -> Unit)? = null,
    top: @Composable RowScope.() -> Unit = {},
    /** The grown-up check has nothing to hear: its numbers are for reading, never spoken. */
    showHear: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        background()
        // While it's his turn to talk, the scenery dims slightly (never the buttons or the word).
        val dim by animateFloatAsState(if (LocalApp.current.listening) 0.22f else 0f, tween(250), label = "dim")
        Box(Modifier.fillMaxSize().drawBehind { if (dim > 0f) drawRect(Color.Black.copy(alpha = dim)) })
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth().height(CornerButtonSize + 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    left != null -> left()
                    onHome != null -> HomeButton(onHome)
                    else -> Spacer(Modifier.width(CornerButtonSize))
                }
                if (onBack != null) {
                    Spacer(Modifier.width(12.dp))
                    BackButton(onBack)
                }
                Spacer(Modifier.width(14.dp))
                Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, content = top)
                Spacer(Modifier.width(14.dp))
                if (showHear) HearAgainButton() else Spacer(Modifier.width(CornerButtonSize))
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}

/** Plain kraft paper, for screens without scenery. */
@Composable
fun PlainBackdrop() {
    Box(Modifier.fillMaxSize().paperCard(Kid.background, 0.dp))
    PaperGrain()
}

// ---------------------------------------------------------------- paper animations

/**
 * A picture that isn't a button still answers a tap: a quick paper wiggle (0.3 s). It only
 * watches, so a button underneath still gets the tap, and an unclaimed tap still gets its puff
 * and pop. Nothing moves with reduce motion on.
 */
fun Modifier.wiggleOnTap(): Modifier = composed {
    val still = LocalReduceMotion.current
    val turn = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    this
        .pointerInput(still) {
            if (still) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    if (e.changes.any { it.changedToUpIgnoreConsumed() }) {
                        scope.launch { for (a in listOf(8f, -7f, 5f, -3f, 0f)) turn.animateTo(a, tween(60)) }
                    }
                }
            }
        }
        .graphicsLayer { rotationZ = turn.value }
}

/** A finger can wobble this far and still tap (young fingers wobble); further is a drag. */
val TapSlop = 24.dp

/** A touch held longer than this is a resting hand (a palm), not a tap. */
const val TapMaxMs = 1200L

/**
 * After a touch went [down]: waits for the finger to lift and says whether it was a tap. Not a
 * tap: it moved more than [TapSlop] (dragging the map), it rested longer than [TapMaxMs] (a palm
 * on the glass), or another finger or the palm touched too.
 */
suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitKidTapUp(
    down: androidx.compose.ui.input.pointer.PointerInputChange,
): Boolean {
    val slop = TapSlop.toPx()
    var ok = true
    while (true) {
        val e = awaitPointerEvent()
        if (e.changes.count { it.pressed } > 1) ok = false
        val c = e.changes.firstOrNull { it.id == down.id } ?: return false
        if ((c.position - down.position).getDistance() > slop) ok = false
        if (!c.pressed) {
            c.consume()
            return ok && c.uptimeMillis - down.uptimeMillis <= TapMaxMs
        }
    }
}

/**
 * A tap for young fingers: the feedback starts the moment the finger touches ([onPress], the
 * crinkle, and the press-down), so it feels instant; the tap itself counts when the finger
 * lifts ([onClick]), and only if it was a tap (see [awaitKidTapUp]: not a drag, not a resting
 * palm).
 */
fun Modifier.kidTap(
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onPress: (() -> Unit)? = null,
): Modifier = composed {
    val latest = androidx.compose.runtime.rememberUpdatedState(onClick)
    val pressLatest = androidx.compose.runtime.rememberUpdatedState(onPress)
    val scope = rememberCoroutineScope()
    if (!enabled) return@composed this
    pointerInput(interaction) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val press = androidx.compose.foundation.interaction.PressInteraction.Press(down.position)
            scope.launch { interaction.emit(press) }
            pressLatest.value?.invoke()
            val tapped = awaitKidTapUp(down)
            scope.launch { interaction.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press)) }
            if (tapped) latest.value()
        }
    }
}

/** Pops up like a paper cut-out being stood up: a quick springy grow with a little twist. */
fun Modifier.popIn(key: Any? = Unit, delayMillis: Int = 0): Modifier = composed {
    val still = LocalReduceMotion.current
    val anim = remember(key) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(key) {
        if (still) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        // A quick springy settle, well under half a second.
        anim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
    }
    graphicsLayer {
        val v = anim.value
        scaleX = 0.4f + 0.6f * v
        scaleY = 0.4f + 0.6f * v
        rotationZ = (1f - v) * -8f
        alpha = v.coerceIn(0f, 1f)
    }
}

/** A new screen settles in like a sheet of paper laid down: a short fade and grow. */
fun Modifier.paperEnter(key: Any?): Modifier = composed {
    val still = LocalReduceMotion.current
    val anim = remember(key) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(key) { if (!still) anim.animateTo(1f, tween(220)) }
    graphicsLayer {
        val v = anim.value
        scaleX = 0.97f + 0.03f * v
        scaleY = 0.97f + 0.03f * v
        alpha = v
    }
}

/**
 * Unfolds from the top edge like a folded paper map opening out. [play] false shows it
 * already open. [onDone] runs when the fold has finished.
 */
fun Modifier.unfold(play: Boolean, onDone: () -> Unit = {}): Modifier = composed {
    val still = LocalReduceMotion.current
    val anim = remember { Animatable(if (play && !still) 0f else 1f) }
    LaunchedEffect(play) {
        if (play) {
            if (!still) {
                delay(300)
                anim.animateTo(1f, tween(MaxMotionMs))
            }
            onDone()
        }
    }
    graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 0f)
        cameraDistance = 12f * density
        rotationX = (1f - anim.value) * -90f
        alpha = if (anim.value < 0.02f) 0f else 1f
    }
}

/**
 * Stands up like a pop-up book page when [key] changes: the layer swings up from its bottom
 * edge, after [delayMillis] (so layers behind pop first). Under half a second, still with
 * reduce motion.
 */
fun Modifier.popUp(key: Any?, delayMillis: Int = 0): Modifier = composed {
    val still = LocalReduceMotion.current
    val anim = remember(key) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(key) {
        if (still) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        anim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        val v = anim.value
        transformOrigin = TransformOrigin(0.5f, 1f)
        cameraDistance = 10f * density
        rotationX = (1f - v) * 75f
        alpha = if (v < 0.02f) 0f else 1f
    }
}

/**
 * A sticker peeling off its backing and landing: it lifts from the top-left corner with a curl
 * and settles flat. Under half a second, still with reduce motion.
 */
fun Modifier.peelIn(key: Any?, delayMillis: Int = 0): Modifier = composed {
    val still = LocalReduceMotion.current
    val anim = remember(key) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(key) {
        if (still) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        anim.animateTo(1f, tween(MaxMotionMs, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        val v = anim.value
        val k = 1f - v
        transformOrigin = TransformOrigin(0f, 0f)
        cameraDistance = 8f * density
        rotationY = k * -65f
        rotationX = k * 35f
        rotationZ = k * -12f
        translationY = -k * 18f * density
        alpha = if (v < 0.02f) 0f else 1f
    }
}

/** Flips over like a card. 0 shows the front, 1 the back; content should swap at the halfway point. */
fun Modifier.flip(progress: Float): Modifier = graphicsLayer {
    cameraDistance = 12f * density
    rotationY = progress * 180f
    // Keep the back readable instead of mirrored.
    scaleX = if (progress > 0.5f) -1f else 1f
}

// ---------------------------------------------------------------- choices

/**
 * Shows [items] a few at a time ([perPage], 2 to 4) as big, well-spaced choices, with big
 * ◀ ▶ paper arrows to see more. Keeps the number of things to pick from small.
 */
@Composable
fun <T> PagedChoices(
    items: List<T>,
    perPage: Int,
    modifier: Modifier = Modifier,
    key: Any = Unit,
    spacing: Dp = 22.dp,
    item: @Composable (T, Modifier) -> Unit,
) {
    val app = LocalApp.current
    var page by remember(key) { mutableIntStateOf(0) }
    BoxWithConstraints(modifier) {
        // On a narrow phone, two big choices at a time instead of squeezing in more.
        val narrow = maxWidth < 520.dp
        val count = if (narrow) minOf(perPage, 2) else perPage
        val gap = if (narrow) minOf(spacing, 12.dp) else spacing
        val pages = ((items.size + count - 1) / count).coerceAtLeast(1)
        if (page >= pages) page = pages - 1
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(CornerButtonSize), contentAlignment = Alignment.Center) {
                if (page > 0) PaperArrowButton(PaperColorsUi.choice, size = 72.dp, pointLeft = true) { page--; app.speaker.say("Back!") }
            }
            Spacer(Modifier.width(gap))
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shown = items.drop(page * count).take(count)
                shown.forEachIndexed { i, it ->
                    Box(Modifier.weight(1f).popIn(page to i, i * 60), contentAlignment = Alignment.Center) {
                        // Big, but not blown up on a tablet: buttons stay the same physical size.
                        item(it, Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp).widthIn(max = 240.dp).fillMaxHeight())
                    }
                }
                // Keep tiles the same size on a short last page.
                repeat(count - shown.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.width(gap))
            Box(Modifier.width(CornerButtonSize), contentAlignment = Alignment.Center) {
                if (page < pages - 1) PaperArrowButton(PaperColorsUi.choice, size = 72.dp) { page++; app.speaker.say("More!") }
            }
        }
    }
}

/**
 * A big choice tile: a picture with an optional word chip. Tall tiles put the word underneath;
 * short or wide ones (a phone on its side) put it beside the picture, so the word always shows.
 */
@Composable
fun ChoiceTile(
    emoji: String,
    label: String?,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    picture: (@Composable () -> Unit)? = null,
) {
    PaperButton(if (selected) Kid.highlight else color, modifier, onClick = onClick) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
            val w = maxWidth
            val h = maxHeight
            val beside = label != null && (h < 170.dp || w > h * 1.3f)
            if (beside && label != null) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    Box(Modifier.weight(0.42f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (picture != null) picture() else Text(emoji, fontSize = tileEmojiSize(w * 0.42f, h))
                    }
                    Box(Modifier.weight(0.58f), contentAlignment = Alignment.Center) {
                        PaperChip(label, fontSize = fittingFontSize(label, w * 0.58f - 16.dp), maxLines = 2, horizontalPadding = 8.dp)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (picture != null) picture() else Text(emoji, fontSize = tileEmojiSize(w, if (label != null) h - 56.dp else h))
                    }
                    if (label != null) {
                        // Fit the words to the tile instead of wrapping them a letter at a time.
                        PaperChip(label, fontSize = fittingFontSize(label, w - 16.dp), maxLines = 2, horizontalPadding = 8.dp)
                    }
                }
            }
        }
    }
}

/** An emoji as big as the space allows, within reason. */
private fun tileEmojiSize(w: Dp, h: Dp): TextUnit = (minOf(w, h).value * 0.6f).coerceIn(28f, 80f).sp
