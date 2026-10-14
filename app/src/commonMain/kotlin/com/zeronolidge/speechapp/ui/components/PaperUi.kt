package com.zeronolidge.speechapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(70), label = "press")
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (enabled) Modifier.raisedPaperBase(color, corner, circle) { press } else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
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
) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .paperCard(color, 999.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
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

/** Home: always the top-left corner, the same size and colour on every screen. */
@Composable
fun HomeButton(onHome: () -> Unit) {
    val app = LocalApp.current
    PaperIconButton("🏠", PaperColorsUi.home) { app.speaker.stop(); onHome() }
}

/** "Hear it again": always the top-right corner. Says the screen's instruction again. */
@Composable
fun HearAgainButton() {
    val app = LocalApp.current
    PaperIconButton("🔊", PaperColorsUi.hear) { app.repeatInstruction() }
}

/** The colours of the shared controls, picked from the paper palette. */
object PaperColorsUi {
    val home = Color(0xFFFF8A65)
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
    background: @Composable BoxScope.() -> Unit = { PlainBackdrop() },
    left: (@Composable () -> Unit)? = null,
    top: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        background()
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
                Spacer(Modifier.width(14.dp))
                Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, content = top)
                Spacer(Modifier.width(14.dp))
                HearAgainButton()
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

/** Pops up like a paper cut-out being stood up: a quick springy grow with a little twist. */
fun Modifier.popIn(key: Any? = Unit, delayMillis: Int = 0): Modifier = composed {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        anim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
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
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { anim.animateTo(1f, tween(220)) }
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
    val anim = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(play) {
        if (play) {
            delay(350)
            anim.animateTo(1f, tween(650))
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
    val pages = ((items.size + perPage - 1) / perPage).coerceAtLeast(1)
    if (page >= pages) page = pages - 1
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(CornerButtonSize), contentAlignment = Alignment.Center) {
            if (page > 0) PaperIconButton("◀", PaperColorsUi.choice, size = 72.dp) { page--; app.speaker.say("Back!") }
        }
        Spacer(Modifier.width(spacing))
        Row(
            Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shown = items.drop(page * perPage).take(perPage)
            shown.forEachIndexed { i, it ->
                Box(Modifier.weight(1f).popIn(page to i, i * 60), contentAlignment = Alignment.Center) {
                    // Big, but not blown up on a tablet: buttons stay the same physical size.
                    item(it, Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp).widthIn(max = 240.dp).fillMaxHeight())
                }
            }
            // Keep tiles the same size on a short last page.
            repeat(perPage - shown.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.width(spacing))
        Box(Modifier.width(CornerButtonSize), contentAlignment = Alignment.Center) {
            if (page < pages - 1) PaperIconButton("▶", PaperColorsUi.choice, size = 72.dp) { page++; app.speaker.say("More!") }
        }
    }
}

/** A big choice tile: a picture with an optional word chip underneath. */
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
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (picture != null) picture() else Text(emoji, fontSize = 64.sp)
            }
            if (label != null) PaperChip(label, fontSize = 20.sp)
        }
    }
}
