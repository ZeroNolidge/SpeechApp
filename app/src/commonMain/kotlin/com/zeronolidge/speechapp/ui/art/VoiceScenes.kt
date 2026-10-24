package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.zeronolidge.speechapp.logic.KitchenCut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Backdrop
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.KitchenEndStage
import com.zeronolidge.speechapp.logic.KitchenFlow
import com.zeronolidge.speechapp.logic.KitchenLayout
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Light
import com.zeronolidge.speechapp.logic.Move
import com.zeronolidge.speechapp.logic.Prop
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceBeat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Voice stories, drawn from data: any beat is a backdrop, a prop and a move (logic/VoiceStories.kt).
 * The moves are shared and driven by his voice: [f] is how far the move has gone (0 to 1, pushed
 * by loudness and by how long he keeps it up), [strength] how big the flourish is (his loudest
 * moment), and [push] how hard he is pushing right now (a live wobble). New stories reuse all of
 * it; a new prop needs only its paper art in [PropArt].
 */

private fun bump(f: Float) = sin(PI.toFloat() * f.coerceIn(0f, 1f))
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Where a move puts its prop, as parts of the scene's width and height, and how it's turned and sized. */
class Pose(val x: Float, val y: Float, val scale: Float = 1f, val rotation: Float = 0f, val alpha: Float = 1f)

/** Props that show "grow" themselves (the balloon fills, the sail bellies out) instead of just
 * getting bigger: the gap balloon inflates with his pop, in place, rather than the whole prop
 * scaling up (see [Prop.GAP_BALLOON]). */
private fun growsInside(p: Prop) = p == Prop.BALLOON || p == Prop.SAIL || p == Prop.GAP_BALLOON

/** The shared moves. [wobble] is a 0 to 1 clock for the shake and the live wobble. */
fun pose(move: Move, prop: Prop, f: Float, strength: Float, push: Float, wobble: Float): Pose {
    val live = push * 7f * sin(wobble * 2f * PI.toFloat() * 6f)
    return when (move) {
        Move.RISE -> Pose(0.5f, lerp(0.74f, 0.3f, f) - 0.04f * bump(f) * strength, rotation = live)
        Move.SINK -> Pose(0.5f, lerp(0.3f, 0.64f, f), rotation = live + 10f * bump(f) * strength)
        Move.SLIDE -> Pose(lerp(0.22f, 0.66f, f), 0.6f - 0.06f * bump(f) * strength, rotation = live - 6f * bump(f))
        Move.POP -> Pose(0.5f, 0.45f, scale = if (prop == Prop.BUBBLE) 1f else 1f + 0.3f * f, alpha = if (prop == Prop.BUBBLE) 1f else 1f - f, rotation = live)
        Move.GROW -> Pose(0.5f, 0.52f, scale = if (growsInside(prop)) 1f else 0.62f + (0.38f + 0.2f * strength) * f, rotation = live)
        Move.SHAKE -> {
            // A wobble that's biggest while he's pushing, settling as the move finishes, then a little hop.
            val amp = (8f + 16f * push) * (1f - 0.6f * f) * (0.6f + 0.4f * strength)
            Pose(0.5f, 0.5f - 0.06f * bump(f) * strength, rotation = amp * sin(wobble * 2f * PI.toFloat() * 4f))
        }
    }
}

/** Where the thing his word moves is right now: it glows while he listens, and the wind strips aim at it. */
fun voiceTarget(beat: VoiceBeat, f: Float, strength: Float = 1f): Offset {
    val p = pose(beat.move, beat.prop, f, strength, 0f, 0f)
    return Offset(p.x, p.y)
}

/** Where a still prop stands: under a falling thing to catch it, otherwise off to the side. */
private fun stillSpot(move: Move) = if (move == Move.SINK) Offset(0.5f, 0.8f) else Offset(0.8f, 0.74f)

/** How big each prop is, as a part of the scene's size. */
private fun propSize(p: Prop) = when (p) {
    Prop.BOAT -> 0.32f
    Prop.SAIL -> 0.38f
    Prop.BUBBLE -> 0.3f
    Prop.DUCK, Prop.BIRD -> 0.15f
    Prop.SUN -> 0.26f
    Prop.OWL, Prop.SLEEPY_OWL -> 0.26f
    Prop.LEAVES -> 0.28f
    Prop.BERRIES -> 0.2f
    Prop.BALLOON -> 0.44f
    Prop.CUPCAKE -> 0.13f
    Prop.MOON -> 0.2f
    Prop.GAP_BALLOON -> 0.22f
    // K3c: the bowl and pan each got their own narrower station on the counter (clear of Rory's
    // lane, see KitchenLayout), so these five had to shrink to fit inside them without the two
    // stations' props reaching into each other or into Rory -- the old sizes (up to 0.34 for the
    // pan) were sized for the wider, Rory-straddling band K3b used, not K3c's two slimmer ones.
    // K6: bowl/batter/milk shrank further still, to make room for milk's own ingredient station
    // (KitchenLayout.ingredientBox) beside the bowl's -- a real screenshot showed the milk carton
    // drawn right on top of the bowl, hiding it completely.
    Prop.BOWL -> 0.14f
    Prop.MILK -> 0.12f
    Prop.BATTER -> 0.14f
    Prop.PAN -> 0.16f
    Prop.PANCAKE -> 0.15f
    Prop.STEAM -> 0.14f
    // K6: the butter-cutting mini-sequence's own plate, on the same ingredient station milk uses
    // (by the time it plays, milk's long since poured).
    Prop.BUTTER -> 0.14f
}

/** [Prop.BUBBLE]'s own [propSize] (0.3) is shared with the River story's bubble-pop beat, where
 * it floats free in open air -- too big for Rory's Kitchen's pan station ([KitchenLayout], K3c:
 * 0.2 of the scene's width), where it's the pancake's own bubbles popping on the counter. Kitchen
 * beats use this instead of [propSize] directly; every other backdrop is unaffected.
 *
 * K7: Timothy's ask -- the bowl, its batter and the butter plate read about 1.6x bigger in
 * landscape now that the counter's own framing no longer shrinks/jumps between steps (see
 * VoiceStoryScreen's own `isKitchen`), with the pan a little smaller to leave them the room (its
 * own station, [KitchenLayout.panBox], narrows to match in landscape). Portrait was unaffected
 * then: [propSize]'s own baseline, same as before.
 *
 * K8: Timothy's own measured screenshots still showed the bowl small next to Rory's now-bigger
 * size in both orientations -- BOWL/BATTER grow again (landscape further still; portrait, which
 * K7 never touched, gets its own real boost now that [KitchenLayout]'s own bowl/ing/pan stations
 * have room for it, see `bowlRight`/`ingLeft`/`panLeft` there). These numbers are the exact
 * fractions [KitchenLayout]'s own bowl/ingredient stations were widened to fit (see that file's
 * own K8 notes on `bowlRight`/`ingLeft`/`ingRight`) -- a bigger one here without a matching
 * station there would spill the natural (unclamped) box into the next station over, the K3c bug in
 * a new place.
 *
 * K9: a real landscape screenshot of the butter-cutting step showed the butter plate tiny (about
 * 40px on an iPad-size stage) next to a now much bigger bowl/Rory/counter -- BUTTER now matches
 * BOWL/BATTER's own size instead of its old, much smaller one (~0.2 of the scene's width, Ruby's
 * own ask: "about the bowl's size, so the cut line reads clearly"), in both orientations. Its
 * station ([KitchenLayout.ingredientBox]) is narrower than the bowl's own, so the plate's natural
 * (unclamped) width now reaches a little past its own station into the gap before the pan's --
 * harmless, since nothing else ever shares the counter with the butter plate (by the time it
 * shows, milk and the berries are long since poured/counted).
 *
 * K9: portrait's own share bumped too (0.17 to 0.19), alongside the same step's wider counter
 * stations ([KitchenLayout]'s own `bowlLeft`/`bowlRight` etc.): Ruby's "bowl/berries/butter
 * proportionally bigger" portrait ask. */
private fun kitchenPropSize(p: Prop, landscape: Boolean) = when (p) {
    Prop.BUBBLE -> 0.18f
    Prop.BOWL, Prop.BATTER -> if (landscape) 0.2533f else 0.19f
    Prop.BUTTER -> if (landscape) 0.27f else 0.27f
    Prop.PAN -> if (landscape) 0.144f else propSize(p)
    else -> propSize(p)
}

/** [kitchenPropSize], exposed for Rory's Kitchen's own "KITCHEN props" log (`VoiceStory.kt`,
 * K3b): the exact size fraction a kitchen prop renders at, so the logged/tested box
 * ([com.zeronolidge.speechapp.logic.KitchenLayout.propBox]) matches what's actually drawn. */
fun kitchenPropSizeFraction(prop: Prop, landscape: Boolean): Float = kitchenPropSize(prop, landscape)

/** A balloon's colour, shared by the puzzle (see `BalloonPatternPuzzle` in VoiceStory.kt) and the
 * one that joins the scene once it's solved ([Prop.GAP_BALLOON]). */
fun balloonColor(word: String): Color = when (word) {
    "blue" -> Color(0xFF42A5F5)
    "pink" -> Color(0xFFEC6FA0)
    "brown" -> Color(0xFF8D6E63)
    "purple" -> Color(0xFF9575CD)
    else -> Color(0xFFBDBDBD)
}

/** One paper balloon: a round body on a thin string, filling the drawing area. */
fun DrawScope.paperBalloon(color: Color) {
    val r = size.minDimension * 0.36f
    val c = Offset(size.width / 2f, size.height * 0.42f)
    paperOval(color, Offset(c.x - r, c.y - r * 1.15f), Size(r * 2f, r * 2.3f))
    drawLine(Color(0xFF6D4C41), Offset(c.x, c.y + r * 1.1f), Offset(c.x, size.height * 0.94f), 1.5.dp.toPx())
}

/** One paper blueberry, radius [r] about [center]: a dark round body, a lighter highlight dot,
 * and a tiny five-point star calyx pressed into its top (no stem, no leaf). Shared by Rory's
 * Kitchen's berries beat and its "count the berries" tap tiles (see [BlueberryArt]). */
fun DrawScope.paperBlueberry(center: Offset, r: Float) {
    paperCircle(Color(0xFF2E3F8F), r, center)
    paperCircle(Color(0xFF6E82D6), r * 0.22f, Offset(center.x - r * 0.32f, center.y - r * 0.34f))
    val calyxCenter = Offset(center.x, center.y - r * 0.7f)
    val calyxR = r * 0.34f
    val star = Path().apply {
        for (i in 0 until 5) {
            val outerA = -PI.toFloat() / 2f + i * (2f * PI.toFloat() / 5f)
            val ox = calyxCenter.x + cos(outerA) * calyxR
            val oy = calyxCenter.y + sin(outerA) * calyxR
            if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
            val innerA = outerA + PI.toFloat() / 5f
            lineTo(calyxCenter.x + cos(innerA) * calyxR * 0.42f, calyxCenter.y + sin(innerA) * calyxR * 0.42f)
        }
        close()
    }
    paperPath(star, Color(0xFF241B52))
}

/** A single paper blueberry, sized for Rory's Kitchen's "count the berries" tap tiles (see
 * `KitchenTapRow` in VoiceStory.kt): blueberries, not strawberries or cherries (Ruby, K2a). */
@Composable
fun BlueberryArt(boxSize: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(boxSize)) { paperBlueberry(center, this.size.minDimension * 0.4f) }
}

/** Something placed by its centre at ([x], [y]) (parts of the scene), moved while drawing.
 * [onRect] (K3e), when given, reports this Box's own real drawn bounds (window coordinates) every
 * time it's placed -- for Rory's Kitchen's "KITCHEN props" log (VoiceStory.kt), which used to print
 * a hand-recomputed box ([com.zeronolidge.speechapp.logic.KitchenLayout.propBox]) that could drift
 * from what this function actually draws; reading it straight from the real node closes that gap
 * for good, whatever either formula does later. */
@Composable
private fun BoxWithConstraintsScope.Item(
    size: Dp,
    x: () -> Float,
    y: () -> Float,
    alpha: () -> Float = { 1f },
    rotation: () -> Float = { 0f },
    scale: () -> Float = { 1f },
    onRect: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val wPx = constraints.maxWidth
    val hPx = constraints.maxHeight
    Box(
        Modifier
            .offset { IntOffset((x() * wPx - size.toPx() / 2).roundToInt(), (y() * hPx - size.toPx() / 2).roundToInt()) }
            .size(size)
            .let { m -> if (onRect == null) m else m.onGloballyPositioned(onRect) }
            .graphicsLayer {
                this.alpha = alpha().coerceIn(0f, 1f)
                rotationZ = rotation()
                scaleX = scale()
                scaleY = scale()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Emoji(e: String, size: Dp) = Text(e, fontSize = (size.value * 0.72f).sp)

/**
 * One beat of a voice story: its backdrop (with the light changing as the move happens), a still
 * prop for company, and the prop his voice moves. [f] and [push] are read only while drawing.
 */
@Composable
fun VoiceBeatScene(
    beat: VoiceBeat,
    f: () -> Float,
    strength: Float,
    push: () -> Float,
    modifier: Modifier = Modifier,
    puzzleSafe: Boolean = false,
    landscape: Boolean = false,
    /** Rory's Kitchen only: his mouth follows this while he models a word (Ruby, K2b). */
    kitchenTalking: Boolean = false,
    /** Rory's Kitchen only: he hides while the big listening Rory takes over (`listenLookGone`),
     * so the two Rorys are never both shown. */
    kitchenRoryVisible: Boolean = true,
    /** Rory's Kitchen only (K3a): a small hand near his mouth while he says "Say it out loud!",
     * so the cue to talk is seen as well as heard. */
    kitchenPointing: Boolean = false,
    /** Rory's Kitchen only (K3a): a paper speech-bubble beside him for the whole of a talk-type
     * beat (never a tap step), the same visual anchor the hint hand already is for a tap step. */
    kitchenSpeechBubble: Boolean = false,
    /** Rory's Kitchen only (K-f): his "f" mouth pose, held for about a second while he models a
     * word starting with the "f" sound -- see [KitchenRory]'s own `fPose`. */
    kitchenFPose: Boolean = false,
    /** Rory's Kitchen only (K6): [Prop.BUTTER] draws as two slid-apart pieces once the
     * butter-cutting step's swipe has fired (or, in the beat right after it, already cut), one
     * pat before that. */
    butterCut: Boolean = false,
    /** Rory's Kitchen only (K6): the counting beat's berries already hopped into the bowl, and
     * the tap handler for the three waiting on the counter (null on every other beat). */
    countHopped: Set<Int> = emptySet(),
    onBerryTap: ((Int) -> Unit)? = null,
    /** Rory's Kitchen only (K6): the swipe-to-cut step is live (dotted line, hint, knife). */
    onCut: (() -> Unit)? = null,
    /** Rory's Kitchen only (K6): a big numeral popped up beside the bowl/butter ("2"). */
    bigNumeral: String? = null,
    /** Rory's Kitchen only (K6): each on-counter tap target's window centre, for the KITCHEN log. */
    onKitchenTarget: ((Int, Offset) -> Unit)? = null,
    /** Rory's Kitchen only: reports the scene's own on-screen box, for the KITCHEN rects log. */
    onSceneRect: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    /** Rory's Kitchen only (K3e): reports each kitchen prop's own real drawn box (window
     * coordinates), straight from the node `Item` actually places -- see [Item]'s own `onRect`. */
    onKitchenPropRect: ((Prop, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
) {
    val still = LocalReduceMotion.current
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "beat")
    val wobble by t.loop(0f, 1f, 4000, mode = androidx.compose.animation.core.RepeatMode.Restart, easing = androidx.compose.animation.core.LinearEasing, label = "wobble")
    Scenery(beat.backdrop.land, modifier.fillMaxSize(), seed = 30 + beat.backdrop.ordinal * 7) {
        BoxWithConstraints(
            Modifier.fillMaxSize().let { m -> if (onSceneRect == null) m else m.onGloballyPositioned(onSceneRect) },
        ) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            val isKitchen = beat.backdrop == Backdrop.KITCHEN
            // K3e: a kitchen prop stands up from the counter, which (in landscape) leaves only a
            // short "wall" band actually visible above the counter line -- [unit] above is tuned
            // for open-air floating props and, in a wide landscape stage, is dominated by the
            // scene's own WIDTH (its *1.4 boost makes height lose that min() almost every time a
            // screen is wider than it is tall), so sizing a counter prop off it cross-multiplies by
            // the stage's own aspect ratio and inflates its height well past the counter (a real
            // iPad screenshot showed the berries reaching almost to the screen's own top, over the
            // wall and the caption). Sizing kitchen props off the plain smaller side instead keeps
            // a prop's on-screen height a direct, un-amplified fraction of the scene's own height
            // in both orientations (portrait is unaffected: [maxWidth] was already the smaller side
            // there either way).
            val kitchenUnit = minOf(maxWidth, maxHeight)
            // Rory's Kitchen draws its own backdrop in three layers, Rory sandwiched behind the
            // counter (K2f; see KitchenRory/KitchenCounterFront): every other backdrop is one
            // flat layer (BackdropExtras).
            if (isKitchen) {
                KitchenWall(landscape)
                KitchenRory(landscape, kitchenTalking, kitchenRoryVisible, kitchenPointing, kitchenSpeechBubble, kitchenFPose)
                KitchenCounterFront(landscape)
            } else {
                BackdropExtras(beat.backdrop)
            }
            // Rory's Kitchen's own props always sit on the counter (K3b): the generic open-air
            // `stillSpot`/`pose` positions below (built for the boat/owl/balloon scenes) put the
            // bowl up over the wall, near Bear, on a real iPad, because they know nothing about
            // where the counter actually is. KitchenLayout.propBox does, from the counter's own
            // geometry, so kitchen props use that instead, for both the still prop and the moving
            // one; every other backdrop is unchanged.
            fun kitchenCenter(prop: Prop): Pair<Float, Float> {
                val box = KitchenLayout.propBox(prop, landscape, kitchenPropSize(prop, landscape), maxWidth.value, maxHeight.value)
                return (box.l + box.r) / 2f to (box.t + box.b) / 2f
            }
            beat.still?.let { sp ->
                val (sx, sy) = if (isKitchen) kitchenCenter(sp) else stillSpot(beat.move).let { it.x to it.y }
                Item(
                    if (isKitchen) kitchenUnit * kitchenPropSize(sp, landscape) else unit * propSize(sp) * 0.8f, { sx }, { sy },
                    onRect = if (!isKitchen) null else { c -> onKitchenPropRect?.invoke(sp, c) },
                ) { PropArt(sp, { 1f }, null, beat, butterCut) }
            }
            fun now() = pose(beat.move, beat.prop, f(), strength, push(), if (still) 0f else wobble)
            val kitchenPropCenter = if (isKitchen) kitchenCenter(beat.prop) else null
            // K6: the counting beat draws its own three waiting berries right on the counter
            // (`KitchenCountRow` in VoiceStory.kt), beside the bowl -- the generic single-pile
            // render below would draw a 4th, separate pile centred on the bowl itself instead.
            val suppressed = isKitchen && beat.kitchen == KitchenFlow.COUNT
            // While the pattern puzzle is up, the beat's own prop is tucked into a safe corner,
            // clear of the pattern row above the choice strip (it used to sit left-of-centre and
            // cover the row's first balloon); it stays small but visible, Bear and all. Rory's
            // Kitchen's own choice strip never needs this: its counter position is already clear
            // of it (K3b).
            if (!suppressed) Item(
                if (isKitchen) kitchenUnit * kitchenPropSize(beat.prop, landscape) else unit * propSize(beat.prop) * (if (puzzleSafe) 0.7f else 1f),
                x = { kitchenPropCenter?.first ?: if (puzzleSafe) 0.82f else now().x },
                y = { kitchenPropCenter?.second ?: if (puzzleSafe) 0.8f else now().y },
                alpha = { now().alpha }, rotation = { now().rotation }, scale = { now().scale },
                onRect = if (!isKitchen) null else { c -> onKitchenPropRect?.invoke(beat.prop, c) },
            ) { PropArt(beat.prop, f, beat.propEmoji, beat, butterCut) }
            if (isKitchen && beat.kitchen == KitchenFlow.COUNT) KitchenCountRow(landscape, countHopped, onBerryTap, onKitchenTarget)
            // the "how many berries?" ask keeps the three berries in the bowl, with no numeral
            // (it would give the answer away)
            if (isKitchen && beat.kitchen == KitchenFlow.ASK && beat.prop == Prop.BATTER) KitchenCountRow(landscape, setOf(0, 1, 2), null, null, showCount = false)
            if (isKitchen && onCut != null) KitchenCutOverlay(landscape, onCut, onKitchenTarget)
            if (isKitchen && bigNumeral != null) KitchenNumeral(bigNumeral, if (beat.prop == Prop.BUTTER) KitchenLayout.ingredientBox(landscape) else KitchenLayout.bowlBox(landscape), landscape)
            // The light changes with the move: the sun comes up (dawn) or night falls (dusk).
            if (beat.light != Light.SAME) {
                Canvas(Modifier.fillMaxSize()) {
                    val a = if (beat.light == Light.DAWN) 0.55f * (1f - f()) else 0.4f * f()
                    drawRect(Color(0xFF1A2350).copy(alpha = a))
                }
            }
            // Bear sits in front of the counter and its props (K2f), drawn last so his lean
            // never disappears behind anything on it.
            if (beat.backdrop == Backdrop.KITCHEN) KitchenBear(landscape)
        }
    }
}

/**
 * Rory's Kitchen's own ending (Ruby's K2c): once Bear's had his bite, this replaces the usual
 * beat scene (no more moving prop to drive, no score or summary) with the stack of pancakes he's
 * eating ([PancakeStackArt]) in place on the counter, same backdrop and Rory as every other
 * kitchen beat. [leanAmount] (K2e) is how far into his lean-in-for-the-bite Bear currently is (0
 * to 1; animated in VoiceStory.kt during [KitchenEndStage.BITE] so he eases in then back), kept
 * separate from [bitten] (the pancake's own notch, which stays once it appears).
 */
@Composable
fun KitchenEndingScene(
    stage: KitchenEndStage,
    leanAmount: Float = 0f,
    landscape: Boolean = false,
    modifier: Modifier = Modifier,
    /** K2j's own KITCHEN bite measurement log: a formatted line, reported once bear-head,
     * bear-seat, stool and the counter's left edge are all known at full lean (see
     * [KitchenBear]'s own `onBiteRects`); the caller (VoiceStory.kt) decides whether/when to
     * actually print it. */
    onBiteRects: ((String) -> Unit)? = null,
) {
    val bitten = stage != KitchenEndStage.STACK
    Scenery(Land.MARKET, modifier.fillMaxSize(), seed = 30 + Backdrop.KITCHEN.ordinal * 7) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            KitchenWall(landscape)
            KitchenRory(landscape, talking = false, roryVisible = true)
            KitchenCounterFront(landscape)
            // K6: a real plate on the counter, next to Rory, about half his own height and as
            // wide as his body (Ruby's old version floated free, taller than Rory, no plate at
            // all) -- sized straight off his own real fitted box ([KitchenLayout.roryFittedBox]),
            // so it always reads as sitting beside him, whatever his own size ends up being.
            val rory = KitchenLayout.roryFittedBox(maxWidth.value, maxHeight.value, landscape)
            val ct = KitchenLayout.counterTop(landscape)
            val counterTopPx = maxHeight.value * ct
            val stackHeight = (rory.b - rory.t) * 0.5f
            val stackWidth = (rory.r - rory.l).coerceAtMost(maxWidth.value * 0.97f - rory.r)
            val stackLeft = (rory.r + maxWidth.value * 0.015f).coerceAtMost(maxWidth.value * 0.97f - stackWidth)
            Box(
                Modifier.offset(x = stackLeft.dp, y = (counterTopPx - stackHeight).dp)
                    .size(stackWidth.dp, stackHeight.dp),
            ) { PancakeStackArt(bitten) }
            KitchenBear(landscape, leanAmount, onBiteRects)
        }
    }
}

/** The little extras that turn a land's scenery into a backdrop: water, an island, a path, sky
 * clouds. [Backdrop.KITCHEN] draws its own wall/counter split elsewhere (K2f: [KitchenWall],
 * [KitchenRory], [KitchenCounterFront], see [KitchenLayout]), with Rory sandwiched behind the
 * counter, so there's nothing for this function to do for it. */
@Composable
private fun BoxWithConstraintsScope.BackdropExtras(b: Backdrop) {
    val unit = minOf(maxWidth, maxHeight * 1.4f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (b) {
            Backdrop.RIVER, Backdrop.ISLAND -> {
                paperRect(Color(0xFF4FC3F7), Offset(0f, h * 0.62f), Size(w, h * 0.38f))
                listOf(0.7f, 0.8f, 0.9f).forEachIndexed { i, y ->
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(w * (0.1f + i * 0.22f), h * y), Offset(w * (0.3f + i * 0.22f), h * y), 3.dp.toPx())
                }
                if (b == Backdrop.ISLAND) paperOval(Color(0xFFFFE0B2), Offset(w * 0.6f, h * 0.6f), Size(w * 0.36f, h * 0.13f))
            }
            Backdrop.PATH -> paperRect(Color(0xFFD7CCC8), Offset(0f, h * 0.64f), Size(w, h * 0.08f))
            Backdrop.SKY -> {
                paperOval(Color.White.copy(alpha = 0.85f), Offset(w * 0.08f, h * 0.2f), Size(w * 0.18f, h * 0.08f))
                paperOval(Color.White.copy(alpha = 0.85f), Offset(w * 0.68f, h * 0.12f), Size(w * 0.2f, h * 0.09f))
            }
            // Rory's Kitchen draws its own wall/counter split as two separate layers with Rory
            // sandwiched between them (K2f; see KitchenWall/KitchenCounterFront below), so Rory
            // never needs this function at all.
            Backdrop.KITCHEN -> {}
            Backdrop.FOREST, Backdrop.MEADOW -> {}
        }
    }
    if (b == Backdrop.ISLAND) Item(unit * 0.16f, { 0.84f }, { 0.5f }) { Emoji("🌴", unit * 0.16f) }
}

/**
 * Rory's Kitchen's back-most layer (K2f): one flat cream wall/floor, covering the whole scene.
 * [KitchenCounterFront] paints over the counter's own share of it afterwards; because this is one
 * uninterrupted rect rather than two separately-positioned pieces meeting at the counter top line
 * (K2a to K2e's approach), there's no seam to line up at all. Drawn before [KitchenRory] so he
 * stands in front of the wall but behind the counter that follows him.
 *
 * K6: Timothy's landscape screenshot of a talk step (no choice strip -- `VoiceBeatScene` only
 * ever passes `landscape = true` here alongside the strip lift, see its own doc) showed a wide,
 * empty cream band above the counter. A small paper-art window (a pale sky patch with a plain
 * cross mullion) and a shelf with a jar or two fill that space without competing with anything
 * that moves: both sit well to the right of Rory and Bear's lanes (`WALL_LEFT`, clear of
 * [KitchenLayout.bearRight]/[KitchenLayout.roryLeft] at any width, even the widened `wide` ones)
 * and well above the counter top line, scaled to whatever share of the wall is actually on screen
 * ([ct]) so neither ever dips down into the counter itself.
 */
private const val WALL_LEFT = 0.56f
private const val WALL_RIGHT = 0.92f

@Composable
private fun KitchenWall(landscape: Boolean = false) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFFFFF3E3))
        val ct = KitchenLayout.counterTop(landscape)
        val windowTop = ct * 0.08f
        val windowBottom = ct * 0.50f
        val wx0 = w * WALL_LEFT
        val wx1 = w * WALL_RIGHT
        // K7: a soft wallpaper band behind the window (a light tint, plus a few faint vertical
        // stripes), so the wall reads as a papered kitchen wall, not a bare colour fill, now that
        // it's clearly visible on every step (not just a lift-clipped sliver).
        val bandTop = windowTop * 0.5f
        val bandBottom = ct * 0.96f
        paperRect(Color(0xFFFFEFD1), Offset(wx0 - w * 0.02f, h * bandTop), Size((wx1 - wx0) + w * 0.04f, h * (bandBottom - bandTop)))
        val stripeCount = 5
        for (s in 0..stripeCount) {
            val sx = wx0 - w * 0.02f + ((wx1 - wx0) + w * 0.04f) * (s / stripeCount.toFloat())
            drawLine(Color(0xFFF5DDAE).copy(alpha = 0.6f), Offset(sx, h * bandTop), Offset(sx, h * bandBottom), 2.dp.toPx())
        }
        paperRoundRect(
            Color(0xFFBEE3F5), Offset(wx0, h * windowTop), Size(wx1 - wx0, h * (windowBottom - windowTop)),
            CornerRadius(w * 0.02f),
        )
        drawLine(Color(0xFFFFFFFF), Offset((wx0 + wx1) / 2f, h * windowTop), Offset((wx0 + wx1) / 2f, h * windowBottom), 3.dp.toPx())
        drawLine(Color(0xFFFFFFFF), Offset(wx0, h * (windowTop + windowBottom) / 2f), Offset(wx1, h * (windowTop + windowBottom) / 2f), 3.dp.toPx())
        drawRect(Color(0xFF9E7B52), Offset(wx0, h * windowTop), Size(wx1 - wx0, h * (windowBottom - windowTop)), style = Stroke(3.dp.toPx()))
        // K7: a sill just under the window (a thin ledge, a touch wider than the frame) and a
        // pair of soft paper curtains gathered at each side -- Timothy's ask for a "clearly drawn
        // wall", not just a plain window rect.
        val sillY = windowBottom + (ct * 0.02f)
        paperRect(Color(0xFFC9A36B), Offset(wx0 - w * 0.015f, h * sillY), Size((wx1 - wx0) + w * 0.03f, h * 0.012f))
        val curtainColor = Color(0xFFD98C7B)
        val curtainW = (wx1 - wx0) * 0.14f
        val curtainTop = windowTop * 0.4f
        listOf(wx0 - curtainW * 0.35f, wx1 - curtainW * 0.65f).forEach { cxLeft ->
            val curtain = Path().apply {
                moveTo(cxLeft, h * curtainTop)
                quadraticTo(cxLeft + curtainW * 0.3f, h * (curtainTop + windowBottom) / 2f, cxLeft + curtainW * 0.15f, h * sillY)
                lineTo(cxLeft + curtainW * 0.85f, h * sillY)
                quadraticTo(cxLeft + curtainW * 0.7f, h * (curtainTop + windowBottom) / 2f, cxLeft + curtainW, h * curtainTop)
                close()
            }
            paperPath(curtain, curtainColor)
        }
        // A little paper shelf below the sill, with jars and a small plant sitting on it.
        val shelfY = ct * 0.74f
        paperRect(Color(0xFF9E7B52), Offset(wx0, h * shelfY), Size(wx1 - wx0, h * 0.02f))
        listOf(0.14f, 0.45f).forEach { jx ->
            val jarW = (wx1 - wx0) * 0.16f
            val jarH = h * ct * 0.14f
            val jl = wx0 + (wx1 - wx0) * jx
            paperRoundRect(Color(0xFFDCEEDC), Offset(jl, h * shelfY - jarH), Size(jarW, jarH), CornerRadius(jarW * 0.2f))
            paperRect(Color(0xFFC9773A), Offset(jl + jarW * 0.2f, h * shelfY - jarH - jarH * 0.18f), Size(jarW * 0.6f, jarH * 0.2f))
        }
        // K7: a small potted plant, third item on the shelf (Timothy's ask for "2-3 jars/plants").
        run {
            val potW = (wx1 - wx0) * 0.16f
            val potH = h * ct * 0.09f
            val pl = wx0 + (wx1 - wx0) * 0.76f
            val potTop = h * shelfY - potH
            paperRoundRect(Color(0xFFC9773A), Offset(pl, potTop), Size(potW, potH), CornerRadius(potW * 0.1f))
            val leafColor = Color(0xFF6FA85C)
            listOf(-1f, 0f, 1f).forEach { k ->
                val leaf = Path().apply {
                    val bx = pl + potW * 0.5f
                    val by = potTop
                    moveTo(bx, by)
                    quadraticTo(bx + k * potW * 0.55f, by - potH * 1.3f, bx + k * potW * 0.15f, by - potH * 1.9f)
                    quadraticTo(bx + k * potW * 0.05f, by - potH * 1.1f, bx, by)
                    close()
                }
                paperPath(leaf, leafColor)
            }
        }
    }
}

/** Rory's Kitchen's counter, its front face only (K2f): drawn after [KitchenRory] so it paints
 * over his lower body, the way a real counter hides someone's legs, instead of the counter-shaped
 * redraw-over-Rory this needed before Rory moved behind it. Only runs from just right of Bear's
 * stool (K2e): left of that stays the plain wall colour from [KitchenWall], so the counter reads
 * as a real piece of furniture with its own left end beside him. Landscape draws a taller share of
 * the scene as counter (shorter wall), so it keeps most of the scene once VoiceStory's landscape
 * lift clips the top clear of the choice strip (Ruby, K2a). */
@Composable
private fun KitchenCounterFront(landscape: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val ct = KitchenLayout.counterTop(landscape)
        val counterLeft = w * KitchenLayout.bearBox(landscape).r
        paperRect(KitchenCounterColor, Offset(counterLeft, h * ct), Size(w - counterLeft, h * (1f - ct)))
        paperRect(Color(0xFFE6C38E), Offset(counterLeft, h * ct), Size(w - counterLeft, h * 0.045f))
        drawLine(Color(0xFF8C5A3A), Offset(counterLeft, h * ct), Offset(counterLeft, h), 3.dp.toPx())
        listOf(0.28f, 0.52f, 0.76f).forEach { rel ->
            val py = ct + rel * (1f - ct)
            drawLine(Color(0xFF8C5A3A).copy(alpha = 0.55f), Offset(counterLeft, h * py), Offset(w, h * py), 2.dp.toPx())
        }
    }
}

/** Rory's Kitchen's counter-front colour ([KitchenCounterFront]'s own body colour). K8:
 * [KitchenStripFloor] below used to share this colour on purpose, so the choice strip's own band
 * read as a continuation of the counter -- Timothy's screenshot showed that read as a second
 * counter stacked under the first instead, so the strip now draws its own, different floor colour
 * ([KitchenFloorColor]) on purpose. */
private val KitchenCounterColor = Color(0xFFC9773A)

/** K7: the choice strip's own reserved band (VoiceStoryScreen's own `gameStrip`), kept the same
 * size on every kitchen step now, tap or talk alike (see that file's own doc), so the band reads
 * as something underfoot on a talk step, with no tiles of its own, instead of an empty gap under
 * it. [KitchenTapRow]'s wash-and-tiles strip draws right on top of this on a tap step, covering it
 * completely.
 *
 * K8 (Ruby's scene checklist): this used to be a plain stretch of [KitchenCounterColor] itself, so
 * it read as a second counter stacked under the first, not the floor the counter stands on --
 * [KitchenFloorColor] is a paler, cooler wood/tile tone instead, with a skirting board right under
 * the counter's own front (where the two surfaces actually meet) and a few faint plank seams. */
@Composable
fun KitchenStripFloor(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(KitchenFloorColor)
        drawRect(KitchenSkirtingColor, Offset.Zero, Size(w, h * 0.12f))
        listOf(0.32f, 0.54f, 0.76f, 0.94f).forEach { rel ->
            drawLine(KitchenFloorSeamColor, Offset(0f, h * rel), Offset(w, h * rel), 1.5.dp.toPx())
        }
    }
}

/** [KitchenStripFloor]'s own floor colour: paler and cooler than [KitchenCounterColor], so the
 * strip reads as a different surface (the floor underfoot), not a continuation of the counter. */
private val KitchenFloorColor = Color(0xFFE3C9A0)

/** The skirting board where the counter front meets the floor (K8). */
private val KitchenSkirtingColor = Color(0xFF8C5A3A)

/** Faint plank/tile seams on the floor (K8). */
private val KitchenFloorSeamColor = Color(0x33704830)

/** Rory's Kitchen's own white paper toque (Ruby, K2b): a fixed prop, not an unlockable piece
 * (see Critter.kt's "hat_chef" case), so it never goes through the buddy's [Catalog]. */
private val ChefHat = Cosmetic("hat_chef", Slot.HAT, "Chef hat", 0xFFFFFFFF)

/** How far [KitchenRory] zooms in while its "f" pose shows (K-f): a close-up, Ruby's own
 * 1.6-1.8x ask. */
private const val FPoseZoom = 1.7f
private const val FPoseZoomLandscape = 1.45f

/**
 * A simple paper stool for Bear to sit on: a round padded seat on three warm-red legs, joined by a
 * rung partway down.
 *
 * K9: real screenshots showed this as a "red blob with three thin wires hanging off it" -- the
 * legs were pencil-thin ([w] * 0.1) and never visually joined to anything, which read badly once
 * the lane they're fitted into is tall and narrow (Bear's own portrait lane especially). Thicker
 * legs ([w] * 0.17) and a rung joining them about two thirds of the way down (a real stool's own
 * cross-brace) make it read as one sturdy piece of furniture standing on the floor, whatever the
 * canvas's own aspect ratio; a thin darker rim on the seat gives it a padded-disc look instead of a
 * flat colour patch.
 */
private fun DrawScope.paperStool() {
    val w = size.width
    val h = size.height
    val legColor = Color(0xFFB23A2E)
    val legW = w * 0.17f
    val seatY = h * 0.22f
    val legXs = listOf(w * 0.18f, w * 0.5f, w * 0.82f)
    legXs.forEach { x -> drawLine(legColor, Offset(x, seatY), Offset(x, h), legW, cap = StrokeCap.Round) }
    val rungY = seatY + (h - seatY) * 0.62f
    drawLine(legColor, Offset(legXs.first(), rungY), Offset(legXs.last(), rungY), legW * 0.55f, cap = StrokeCap.Round)
    // K6: the seat's own side margins (equal on both sides) are KitchenLayout's shared constant,
    // not a bare literal, so [KitchenLayout.stoolSeatCenterXFraction]'s own promise that the seat
    // is centred within this canvas stays true for however this margin is tuned later.
    val margin = KitchenLayout.STOOL_SEAT_SIDE_MARGIN
    val seatSize = Size(w * (1f - 2f * margin), seatY * 1.3f)
    val seatTopLeft = Offset(w * margin, 0f)
    paperOval(Color(0xFFD1492F), seatTopLeft, seatSize)
    drawOval(Color(0xFF8C3322), seatTopLeft, seatSize, style = Stroke(w * 0.025f))
}

/** Bear's constant "turned toward the pan" stance (K2e, widened at K2f): a clockwise lean and a
 * pseudo-3D turn (`rotationY`, [BearFacingTurnDeg]) toward the counter's props to his right,
 * rather than sitting square to the screen; his pupils shift the same way ([BearPanLookX]), in
 * every beat and the ending, since the paper puppet's own face is otherwise symmetric front-on. */
private const val BearFacingTiltDeg = 8f
private const val BearFacingTurnDeg = 20f
private const val BearPanLookX = 6f

/** How far [BearFacingTurnDeg]'s perspective turn reads before it starts to look flattened or
 * stretched instead of rotated: a bigger camera distance keeps the puppet's own paper cut-outs
 * looking flat-but-turned rather than warped. */
private const val BearTurnCameraDistance = 12f

/**
 * Bear, on a simple paper stool beside the counter's own left end (its left end starts just past
 * him; see [KitchenCounterFront]), turned toward the pan (K2e/K2f). [leanAmount] is the ending's
 * own (Ruby, K2c/K2e/K2g): 0 to 1, how far into his bite-lean Bear currently is, cheering (mouth
 * open for the bite) instead of just standing happy while it's above zero.
 *
 * K2j: Bear's resting graphicsLayer below keeps only his constant facing tilt/turn (never the
 * bite lean itself: his stool-seated lower body — legs, bottom, belly, the stool under him —
 * never moves at all, resting or leaning). The bite lean itself is [Critter]'s own `lean`/
 * `leanForward` (K2j), which rotates and shifts only his head+arms, about his neck: K2i rotated
 * and shifted the *whole* Critter about a bottom-centre pivot instead, which is how his whole
 * body (not just his head) ended up swinging past the counter and off the stool in real
 * screenshots, even though a unit test of that same rotation's math passed. [KitchenLayout.bearLean]
 * works out how far his head can lean before it would pass the counter's own left edge, the one
 * thing left to cap now that his body itself never moves. [onBiteRects], if given, reports a
 * formatted KITCHEN bite log line once his head, his own on-screen box ("seat"), the stool and
 * the counter's left edge are all known at full lean (VoiceStory.kt decides whether to print it).
 *
 * Bear's own height here used to be handed to [Critter] unconditionally at 85%: in a lane
 * narrower than Critter's own 100-wide/140-tall art ratio needs for that height (K2e's portrait
 * ending), [Modifier.fillMaxHeight] and the aspect ratio inside [Critter] fight over the lane's
 * width, and [Critter] is measured taller than its own art can fill at that width — its puppet
 * only draws the top slice of that extra-tall canvas, leaving empty space below his feet and
 * reading as Bear floating above the stool (K2g). K8: this fitted box (capped by the lane's width,
 * not just its height) is now [KitchenLayout.bearFittedBox] (shared with the tests, see its own
 * doc), not re-derived here, so his measured box and his drawn art stay the same size, his feet
 * always land on the box's own bottom edge, right where the stool's seat is, and that same box (in
 * the scene's own px) is what's handed to [KitchenLayout.bearLean].
 *
 * K2k: fitted to a slice of the lane narrower than the lane itself
 * ([KitchenLayout.BEAR_SHIFT_FRACTION]) and pinned to the lane's own left edge (not centred), so
 * he and his stool sit further left, with a real gap before the counter his lean can't eat back
 * up (see [KitchenLayout.BEAR_SHIFT_FRACTION]'s own doc for why: screenshots at full lean still
 * showed his head about 25px past the counter on an iPad-size stage). [onHeadBounds]'s real
 * measured position still gets a runtime check of its own below (`runtimeLeanScale`): his lean
 * backs off and remeasures if his head crosses the counter's real measured left edge regardless,
 * since that drift is between the modelled head circle and Critter's real drawn art, not
 * something either box's own numbers alone can promise away.
 */
@Composable
private fun BoxWithConstraintsScope.KitchenBear(
    landscape: Boolean,
    leanAmount: Float = 0f,
    onBiteRects: ((String) -> Unit)? = null,
) {
    val w = maxWidth.value
    val h = maxHeight.value
    val bear = KitchenLayout.bearBox(landscape)
    // K8: his real drawn box is now KitchenLayout's own shared `bearFittedBox` (the fit, the
    // K2k left-shift and the left-pin all live there now, so `KitchenLayoutTest`'s own bite-lean
    // checks and this draw can never quietly drift apart again) -- see its own doc for why this
    // is a slice of the lane (`bear`) above, not the lane itself.
    val fitted = KitchenLayout.bearFittedBox(w, h, landscape)
    val laneLeft = fitted.l
    val bearWidth = fitted.r - fitted.l
    val bearHeight = fitted.b - fitted.t
    val seatY = fitted.b
    val floorY = h * KitchenLayout.BEAR_FLOOR
    val bearBox = fitted
    val counterLeftX = w * bear.r
    val lean = KitchenLayout.bearLean(bearBox, counterLeftX)
    // K2j's own measurement log: real on-screen boxes (device px), not a second model of them,
    // collected once leanAmount reaches its held maximum (see KitchenEnding's own animateTo(1f)
    // then hold, VoiceStory.kt) and reported together once every piece is in (see the
    // LaunchedEffect below).
    val wantRects = onBiteRects != null && leanAmount >= 1f
    var seatRect by remember { mutableStateOf<Rect?>(null) }
    var stoolRect by remember { mutableStateOf<Rect?>(null) }
    var localHeadRect by remember { mutableStateOf<Rect?>(null) }
    var critterOrigin by remember { mutableStateOf<Offset?>(null) }
    var counterLeftPx by remember { mutableStateOf<Float?>(null) }
    // K2k's own runtime safety net, on top of the static shift above: if his real measured head
    // still ends up past the counter's real measured left edge at full lean, back the lean off a
    // little and remeasure (the LaunchedEffect below re-fires on its own since this is one of its
    // keys), rather than trusting the model alone. Normal sizes never need this.
    var runtimeLeanScale by remember { mutableStateOf(1f) }
    if (wantRects) {
        // A zero-size marker at the counter's own left edge (same dp-value coordinate space as
        // [counterLeftX]), measured the same way as everything else here (`positionInWindow`),
        // so the final log line never mixes dp-value numbers with device px (which would be
        // wrong whenever density != 1).
        Box(Modifier.offset(x = counterLeftX.dp, y = (h * bear.t).dp).onGloballyPositioned { c -> counterLeftPx = c.positionInWindow().x })
    }
    // K4: a tall stool, its own box from Bear's seat down to the real floor (KitchenLayout.BEAR_FLOOR),
    // separate from his body box below -- so his seat reads at counter height while his stool's
    // legs still reach the ground, instead of the two being the same point (K2e to K3's own
    // low-stool look, where his seat sat far below the counter).
    // K4b: its top starts a few px ABOVE seatY (KitchenLayout.stoolTop), not at seatY itself, so
    // Bear's own bottom (drawn below, at seatY, and on top of this Canvas in draw order) always
    // overlaps the stool's own drawn seat by that much instead of merely touching it (a real
    // screenshot showed a hairline gap between the two that read as him floating).
    val stoolTopY = h * KitchenLayout.stoolTop(landscape)
    Box(
        Modifier.offset(x = laneLeft.dp, y = stoolTopY.dp).size(bearWidth.dp, (floorY - stoolTopY).dp),
        // K6: Timothy's screenshot showed the stool's own seat sitting well left of Bear's own
        // body -- this canvas used to default to `Alignment.TopStart` (a plain `Box`'s own
        // default), pinning it to this box's LEFT edge instead of centring it the way Bear's own
        // puppet below fills the whole box; `Alignment.TopCenter` keeps
        // [KitchenLayout.stoolSeatCenterXFraction]'s own promise (always 0.5, Bear's own centre)
        // true for any width.
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            Modifier.fillMaxWidth(KitchenLayout.STOOL_CANVAS_WIDTH_FRACTION).fillMaxHeight()
                .graphicsLayer {
                    rotationZ = BearFacingTiltDeg
                    rotationY = BearFacingTurnDeg
                    cameraDistance = BearTurnCameraDistance * density
                }
                .let { m -> if (!wantRects) m else m.onGloballyPositioned { c -> stoolRect = c.windowRect() } },
        ) { paperStool() }
    }
    Box(Modifier.offset(x = laneLeft.dp, y = (seatY - bearHeight).dp).size(bearWidth.dp, bearHeight.dp)) {
        Critter(
            Species.BEAR,
            Modifier.fillMaxSize()
                .graphicsLayer {
                    // Bear's constant "turned toward the pan" stance (K2e/K2f) only: the bite
                    // lean itself is Critter's own `lean`/`leanForward` below, which moves only
                    // his head+arms, never this whole-puppet layer (K2i's bug).
                    rotationZ = BearFacingTiltDeg
                    rotationY = BearFacingTurnDeg
                    cameraDistance = BearTurnCameraDistance * density
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .let { m -> if (!wantRects) m else m.onGloballyPositioned { c -> critterOrigin = c.positionInWindow(); seatRect = c.windowRect() } },
            mood = if (leanAmount > 0.1f) Mood.CHEER else Mood.HAPPY,
            animate = false, lookX = BearPanLookX,
            lean = lean.angleDeg * leanAmount * runtimeLeanScale,
            leanForward = lean.forwardUnits * leanAmount * runtimeLeanScale,
            onHeadBounds = if (!wantRects) null else { r -> localHeadRect = r },
        )
    }
    if (wantRects) {
        LaunchedEffect(seatRect, stoolRect, localHeadRect, critterOrigin, counterLeftPx, runtimeLeanScale) {
            val seat = seatRect
            val stool = stoolRect
            val headLocal = localHeadRect
            val origin = critterOrigin
            val counterLeft = counterLeftPx
            if (seat != null && stool != null && headLocal != null && origin != null && counterLeft != null) {
                val head = Rect(origin.x + headLocal.left, origin.y + headLocal.top, origin.x + headLocal.right, origin.y + headLocal.bottom)
                if (head.right > counterLeft + 0.5f && runtimeLeanScale > 0.05f) {
                    runtimeLeanScale *= 0.85f
                } else {
                    fun fmt(r: Rect) = "${r.left.roundToInt()},${r.top.roundToInt()},${r.right.roundToInt()},${r.bottom.roundToInt()}"
                    onBiteRects?.invoke("KITCHEN bite bear-head=${fmt(head)} bear-seat=${fmt(seat)} stool=${fmt(stool)} counterLeft=${counterLeft.roundToInt()}")
                }
            }
        }
    }
}

/**
 * A small paper speech-bubble (K3a): a rounded cream shape with a little tail and three dots, the
 * same "someone is talking" shorthand a comic uses, drawn with the shared paper helpers (never a
 * raw `drawRoundRect`/`drawPath`) so it keeps the torn-edge paper look everything else here has.
 * Shown beside Rory for the whole of a kitchen talk turn (see [KitchenRory]'s own `speechBubble`),
 * so a child who can't read the caption still sees that this step is him talking, not a tap.
 */
@Composable
private fun KitchenSpeechBubble(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val bodyH = size.height * 0.74f
        paperRoundRect(Color.White, Offset.Zero, Size(w, bodyH), CornerRadius(w * 0.32f, w * 0.32f))
        val tail = Path().apply {
            moveTo(w * 0.16f, bodyH * 0.9f)
            lineTo(w * 0.06f, size.height)
            lineTo(w * 0.42f, bodyH * 0.9f)
            close()
        }
        paperPath(tail, Color.White)
        val dot = w * 0.09f
        listOf(0.3f, 0.5f, 0.7f).forEach { fx -> paperCircle(PaperStyle.ink, dot, Offset(w * fx, bodyH * 0.46f)) }
    }
}

private fun androidx.compose.ui.layout.LayoutCoordinates.windowRect(): Rect {
    val p = positionInWindow()
    return Rect(p.x, p.y, p.x + size.width, p.y + size.height)
}

/**
 * Rory, standing behind the counter in his chef's hat (Ruby, K2b; K2f draws him behind it, not
 * merely above it): his box (see [KitchenLayout.roryBox]) now reaches down past the counter top
 * line on purpose, since [KitchenCounterFront] is drawn after him and paints over his lower body,
 * the way a real counter hides someone's legs. [talking] follows his mouth while he models a
 * word; [roryVisible] hides him while the big listening Rory takes over (`listenLookGone()` at
 * the call site), so the two Rorys are never both shown. [pointing] and [speechBubble] (K3a) are
 * only ever true alongside [roryVisible] (a talk-type beat's own MODEL/LISTEN phases): a small
 * hand near his mouth for the "Say it out loud!" cue, and a paper speech-bubble beside him for the
 * whole talk turn, so a child who can't read the caption still sees, not just hears, that this is
 * his turn to talk. K3d: [pointing] now poses his own arm (`Critter(pointMouth = true)`, a paper
 * hand raised to his mouth) instead of a separate floating hand icon beside him.
 */
@Composable
private fun BoxWithConstraintsScope.KitchenRory(
    landscape: Boolean,
    talking: Boolean,
    roryVisible: Boolean,
    pointing: Boolean = false,
    speechBubble: Boolean = false,
    /** True while Rory models a word starting with the "f" sound (K-f): his face shows the "f"
     * mouth pose (see `Critter`'s own `fPose`) and his lane zooms in close, scaled from near its
     * own top so his head barely moves while the rest grows down into the counter (which already
     * hides his lower body), keeping his face fully on screen. */
    fPose: Boolean = false,
) {
    val w = maxWidth.value
    val h = maxHeight.value
    val rory = KitchenLayout.roryBox(landscape)
    val laneLeft = w * rory.l
    val laneTop = h * rory.t
    val laneWidth = w * (rory.r - rory.l)
    val laneHeight = h * (rory.b - rory.t)
    val still = LocalReduceMotion.current
    val zoom by animateFloatAsState(if (fPose) (if (landscape) FPoseZoomLandscape else FPoseZoom) else 1f, tween(if (still) 0 else 350), label = "fPoseZoom")
    // K7d: zoom about the bottom of his face screen, so the screen grows UP and the body grows
    // down behind the counter (the scene box clips anything below it, so no feet). Landscape has
    // little headroom above him (its top is clipped by the lift), so it zooms less and stays put;
    // portrait has a tall wall above, so it zooms more and lifts the face to ~40% of the scene.
    val fit0 = KitchenLayout.roryFittedBox(w, h, landscape)
    val faceY = fit0.t + (fit0.b - fit0.t) * 0.40f
    val faceX = (fit0.l + fit0.r) / 2f
    val zf = ((zoom - 1f) / (FPoseZoom - 1f)).coerceIn(0f, 1f)
    val liftPx = if (landscape) 0f else (h * 0.40f - faceY) * zf
    val shiftPx = (w * (if (landscape) 0.30f else 0.36f) - faceX).coerceAtLeast(0f) * zf
    Box(
        Modifier.offset(x = laneLeft.dp, y = laneTop.dp).size(laneWidth.dp, laneHeight.dp)
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                transformOrigin = TransformOrigin(
                    ((faceX - laneLeft) / laneWidth).coerceIn(0f, 1f),
                    ((faceY - laneTop) / laneHeight).coerceIn(0f, 1f),
                )
                translationY = liftPx * density
                translationX = shiftPx * density
            },
    ) {
        if (roryVisible) {
            // K4b: fitted to his lane's own width (KitchenLayout.roryFittedBox), not drawn with
            // Modifier.fillMaxHeight(): in a lane narrower than his art needs for the lane's
            // nominal height (portrait's tall, narrow lane especially), fillMaxHeight() plus
            // Critter's own aspectRatio modifier measured him shorter than the lane and left him
            // top-anchored there, his real feet well short of the counter top line -- a real
            // portrait screenshot (768x1024) showed his whole body standing above the counter
            // instead of behind it. [KitchenLayout.roryFittedBox] anchors his feet to the counter
            // top line directly, by his own fitted height, so he always emerges from behind it by
            // the same proportion however much the lane narrows him.
            val fitted = KitchenLayout.roryFittedBox(w, h, landscape)
            Critter(
                Species.ROBOT,
                Modifier.offset(x = (fitted.l - laneLeft).dp, y = (fitted.t - laneTop).dp)
                    .size((fitted.r - fitted.l).dp, (fitted.b - fitted.t).dp),
                Color(0xFFFFCC80),
                outfit = Outfit(hat = ChefHat), mood = Mood.HAPPY, talking = talking, pointMouth = pointing,
                fPose = fPose,
            )
            if (speechBubble && !fPose) KitchenSpeechBubble(
                Modifier.align(Alignment.TopEnd).offset(x = (laneWidth * 0.1f).dp, y = -(laneHeight * 0.15f).dp)
                    .fillMaxWidth(0.55f).aspectRatio(1.1f),
            )
        }
    }
}

/**
 * The paper art for each prop. [f] (read while drawing) lets a prop show its own paper motion as
 * the move goes: the sail bellies out, the balloon fills, the bubble's layers burst apart, the
 * leaves flip to their darker side, Owl wakes up. [emoji] shows his own word instead.
 */
@Composable
private fun PropArt(prop: Prop, f: () -> Float, emoji: String?, beat: VoiceBeat, butterCut: Boolean = false) {
    if (emoji != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 44.sp) }
        return
    }
    val moving = prop == beat.prop
    when (prop) {
        Prop.BOAT -> Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawLine(Color(0xFF5D4037), Offset(w * 0.55f, h * 0.12f), Offset(w * 0.55f, h * 0.72f), 4.dp.toPx())
                val sail = Path().apply {
                    moveTo(w * 0.57f, h * 0.14f)
                    quadraticTo(w * 0.8f, h * 0.38f, w * 0.9f, h * 0.68f)
                    lineTo(w * 0.57f, h * 0.68f)
                    close()
                }
                paperPath(sail, Color(0xFFFFF8E1))
                val hull = Path().apply {
                    moveTo(w * 0.05f, h * 0.7f)
                    lineTo(w * 0.95f, h * 0.7f)
                    lineTo(w * 0.8f, h * 0.92f)
                    lineTo(w * 0.2f, h * 0.92f)
                    close()
                }
                paperPath(hull, Color(0xFF26A69A))
            }
            val sleepy = beat.light == Light.DUSK
            // Pip sits in the boat: feet on the deck (the hull's top edge), left of the mast.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val bw = maxWidth
                val bh = maxHeight
                Critter(
                    Species.PUPPY,
                    Modifier.offset(x = bw * 0.16f, y = bh * 0.34f).size(bw * 0.34f, bh * 0.4f),
                    mood = if (sleepy) Mood.SLEEP else Mood.HAPPY,
                )
            }
        }
        Prop.SAIL -> Box(Modifier.fillMaxSize()) {
            // The whole boat, with Pip in it (a sail on its own looked odd): the sail is droopy,
            // then fills and bellies out with his breath, and a wake streams out behind.
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val fl = if (moving) f() else 1f
                drawLine(Color(0xFF5D4037), Offset(w * 0.55f, h * 0.06f), Offset(w * 0.55f, h * 0.72f), 5.dp.toPx())
                val tipX = w * (0.6f + 0.33f * fl)
                val tipY = h * (0.68f - 0.04f * (1f - fl))
                val belly = 0.16f * fl
                val sail = Path().apply {
                    moveTo(w * 0.57f, h * 0.08f)
                    quadraticTo(w * (0.6f + 0.3f * fl + belly), h * (0.36f - belly * 0.4f), tipX, tipY)
                    lineTo(w * 0.57f, h * 0.68f)
                    close()
                }
                paperPath(sail, Color(0xFFFFF8E1))
                // The wake: white streaks behind the boat, longer as it speeds up.
                if (fl > 0.05f) repeat(3) { i ->
                    val y = h * (0.8f + i * 0.05f)
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(w * (0.12f - 0.1f * fl * (1 + i % 2)), y), Offset(w * 0.12f, y), 3.dp.toPx())
                }
                val hull = Path().apply {
                    moveTo(w * 0.05f, h * 0.7f)
                    lineTo(w * 0.95f, h * 0.7f)
                    lineTo(w * 0.8f, h * 0.92f)
                    lineTo(w * 0.2f, h * 0.92f)
                    close()
                }
                paperPath(hull, Color(0xFF26A69A))
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val bw = maxWidth
                val bh = maxHeight
                Critter(
                    Species.PUPPY,
                    Modifier.offset(x = bw * 0.16f, y = bh * 0.34f).size(bw * 0.34f, bh * 0.4f),
                    mood = Mood.HAPPY,
                )
            }
        }
        // Rory's Kitchen's "pop the bubbles" beat (K4): the generic open-air bubble below (pale
        // blue paper rings bursting apart) was built for the River story's own floating bubble,
        // not a pancake on a pan -- on Rory's Kitchen's counter it read as a pale plate, not
        // bubbles cooking. [KitchenPancakeBubbles] draws the real picture instead: the same dark
        // pan and golden pancake as [Prop.PAN]/[Prop.PANCAKE] (so this beat's "still" pan, already
        // drawn underneath, is never seen as a mismatched second pan), with a few bubbles that pop
        // on his voice.
        Prop.BUBBLE -> if (beat.backdrop == Backdrop.KITCHEN) {
            KitchenPancakeBubbles(moving, beat, f)
        } else Canvas(Modifier.fillMaxSize()) {
            // Three paper layers: whole, then bursting apart, each flying its own way.
            val burst = if (moving && beat.move == Move.POP) ((f() - 0.55f) / 0.45f).coerceIn(0f, 1f) else 0f
            val swell = if (moving && beat.move == Move.POP) 1f + 0.18f * (f() / 0.55f).coerceIn(0f, 1f) else 1f
            val r0 = size.minDimension / 2 * 0.8f * swell
            listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color.White).forEachIndexed { i, c ->
                val a = i * 2.1f + 0.4f
                val d = size.minDimension * 0.5f * burst * (1f + 0.3f * i)
                val r = r0 * (1f - 0.22f * i) * (1f - 0.4f * burst)
                val at = center + Offset(cos(a) * d, sin(a) * d)
                if (burst < 0.98f) {
                    paperCircle(c.copy(alpha = (0.75f - 0.15f * i) * (1f - burst)), r, at)
                    drawCircle(Color.White.copy(alpha = 1f - burst), r, at, style = Stroke(3.dp.toPx()))
                }
            }
        }
        Prop.DUCK -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🦆", fontSize = 40.sp) }
        Prop.BIRD -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🐦", fontSize = 40.sp) }
        Prop.CUPCAKE -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🧁", fontSize = 40.sp) }
        Prop.BERRIES -> if (beat.backdrop == Backdrop.KITCHEN) {
            // Rory's Kitchen: blueberries tumbling in, not cherries (Ruby, K2a; the owl story's
            // berries, Backdrop.FOREST, keep their cherry look below). K5: the lowest berry's own
            // centre+radius are KitchenLayout's shared constants (summing to 1f, its own bottom
            // on the box's bottom edge) -- a real screenshot showed the whole pile floating well
            // short of the counter; the other two keep their original offset from this one.
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension * KitchenLayout.BERRIES_RADIUS_FRACTION
                val lowest = KitchenLayout.BERRIES_LOWEST_CENTER_Y
                listOf(Offset(0.32f, lowest - 0.12f), Offset(0.58f, lowest - 0.26f), Offset(0.68f, lowest)).forEach { spot ->
                    paperBlueberry(Offset(size.width * spot.x, size.height * spot.y), r)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🍒🍒", fontSize = 34.sp) }
        }
        Prop.SUN -> Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.32f
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4f
                drawLine(Color(0xFFFFCA28), center + Offset(cos(a) * r * 1.2f, sin(a) * r * 1.2f), center + Offset(cos(a) * r * 1.55f, sin(a) * r * 1.55f), 5.dp.toPx())
            }
            paperCircle(Color(0xFFFFD54F), r, center)
        }
        Prop.MOON -> Canvas(Modifier.fillMaxSize()) { paperCircle(Color(0xFFFFF9C4), size.minDimension * 0.4f, center) }
        Prop.OWL, Prop.SLEEPY_OWL -> {
            // A sleepy owl wakes once his voice has moved it halfway.
            val asleep = prop == Prop.SLEEPY_OWL && !(moving && f() > 0.5f)
            Critter(Species.OWL, Modifier.fillMaxSize(), mood = if (asleep) Mood.SLEEP else Mood.HAPPY)
        }
        Prop.LEAVES -> BoxWithConstraints(Modifier.fillMaxSize()) {
            listOf(0.2f, 0.5f, 0.8f).forEachIndexed { i, x ->
                Item(maxWidth * 0.32f, { x }, { 0.55f + 0.1f * (i % 2) }, rotation = { (if (moving) f() else 0f) * 120f * (if (i == 0) -1f else 1f) }) {
                    // A paper leaf that flips over to its darker side as it blows away.
                    Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = cos(PI.toFloat() * (if (moving) f() else 0f)) }) {
                        val flipped = moving && f() > 0.5f
                        val leaf = Path().apply {
                            moveTo(size.width * 0.5f, size.height * 0.05f)
                            quadraticTo(size.width * 0.98f, size.height * 0.45f, size.width * 0.5f, size.height * 0.95f)
                            quadraticTo(size.width * 0.02f, size.height * 0.45f, size.width * 0.5f, size.height * 0.05f)
                            close()
                        }
                        paperPath(leaf, if (flipped) Color(0xFF8D4E1C) else Color(0xFFE59A3C))
                        drawLine(Color(0xFF6D3A12), Offset(size.width * 0.5f, size.height * 0.1f), Offset(size.width * 0.5f, size.height * 0.92f), 2.dp.toPx())
                    }
                }
            }
        }
        Prop.BALLOON -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // The balloon fills with his "blow"; Bear rides in the basket below.
            val fill = { if (moving && beat.move == Move.GROW) f() else 1f }
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val r = w * (0.08f + 0.24f * fill())
                val c = Offset(w / 2, h * 0.62f - r)
                drawLine(Color(0xFF6D4C41), Offset(c.x - r * 0.5f, c.y + r * 0.8f), Offset(w * 0.4f, h * 0.72f), 2.dp.toPx())
                drawLine(Color(0xFF6D4C41), Offset(c.x + r * 0.5f, c.y + r * 0.8f), Offset(w * 0.6f, h * 0.72f), 2.dp.toPx())
                paperCircle(Color(0xFFAB47BC), r, c)
                paperOval(Color(0xFFFFD54F), Offset(c.x - r * 0.25f, c.y - r), Size(r * 0.5f, r * 2f))
            }
            Box(Modifier.fillMaxWidth(0.42f).fillMaxHeight(0.36f), contentAlignment = Alignment.BottomCenter) {
                Critter(Species.BEAR, Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.95f), mood = if (beat.light == Light.DUSK) Mood.SLEEP else Mood.HAPPY)
                Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.28f)) {
                    paperRect(Color(0xFF8D6E63), Offset(size.width * 0.12f, 0f), Size(size.width * 0.76f, size.height))
                }
            }
        }
        // The puzzle's picked colour (see VoiceBeat.gapColor): his colour talk turn slides it
        // into place flat (Move.SLIDE); it only inflates once he says "pop" himself, in the
        // second talk turn (Move.GROW) - never before.
        Prop.GAP_BALLOON -> {
            val inflate = when {
                !moving -> 1f
                beat.move == Move.GROW -> 0.4f + 0.6f * f()
                else -> 0.4f
            }
            Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = inflate; scaleY = inflate }) {
                paperBalloon(balloonColor(beat.gapColor ?: "blue"))
            }
        }
        // Rory's Kitchen: a mixing bowl, side view (a wide rim over a rounded body). K5: the rim
        // and control points are KitchenLayout's own shared constants, not bare literals, so the
        // bowl's own lowest drawn point ([KitchenLayout.symmetricQuadApexY]) stays exactly on the
        // box's bottom edge (1f) -- a real screenshot showed it floating well short of the
        // counter, 38% of the box's own height empty below it.
        Prop.BOWL -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.1f, h * KitchenLayout.BOWL_RIM_Y)
                quadraticTo(w * 0.5f, h * KitchenLayout.BOWL_OUTER_CONTROL_Y, w * 0.9f, h * KitchenLayout.BOWL_RIM_Y)
                quadraticTo(w * 0.5f, h * KitchenLayout.BOWL_INNER_CONTROL_Y, w * 0.1f, h * KitchenLayout.BOWL_RIM_Y)
                close()
            }
            paperPath(body, Color(0xFFFFF3E0))
            paperOval(
                Color(0xFFFFFDF7),
                Offset(w * 0.06f, h * KitchenLayout.BOWL_INNER_RIM_TOP),
                Size(w * 0.88f, h * KitchenLayout.BOWL_INNER_RIM_HEIGHT),
            )
        }
        // K3b: a classic gable-top milk carton, not a plain jug (which read as a grey blob on a
        // real iPad) -- a white body, a blue label band with a milk-drop mark, a blue-ridged
        // gable top and a thin grey outline, so it reads as milk at a glance. K7: while it pours
        // (its own beat's move, Move.SINK, into the bowl) it tilts over from the side, and a
        // white stream arcs toward the bowl's own station, which sits just to its left.
        Prop.MILK -> {
            val pouring = moving && beat.move == Move.SINK
            val tilt = if (pouring) -34f * f().coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxSize()) {
                Canvas(
                    Modifier.fillMaxSize().graphicsLayer {
                        rotationZ = tilt
                        transformOrigin = TransformOrigin(0.85f, 0.95f)
                    },
                ) {
                    val w = size.width
                    val h = size.height
                    val bodyLeft = w * 0.26f
                    val bodyRight = w * 0.74f
                    // K5: shifted down 8% so the carton's own drawn bottom
                    // ([KitchenLayout.MILK_ART_BOTTOM]) lands exactly on the box's bottom edge,
                    // not floating short of the counter.
                    val gableTop = h * 0.14f
                    val shoulder = h * 0.36f
                    val bottom = h * KitchenLayout.MILK_ART_BOTTOM
                    val ridgeX = w * 0.5f
                    val carton = Path().apply {
                        moveTo(bodyLeft, bottom)
                        lineTo(bodyLeft, shoulder)
                        lineTo(ridgeX, gableTop)
                        lineTo(bodyRight, shoulder)
                        lineTo(bodyRight, bottom)
                        close()
                    }
                    paperPath(carton, Color(0xFFFFFFFF))
                    // The gable's own fold lines and a thin grey outline, so the white body still
                    // reads against a pale wall instead of melting into it.
                    drawPath(carton, Color(0xFF9E9E9E), style = Stroke(1.5.dp.toPx()))
                    drawLine(Color(0xFFBDBDBD), Offset(bodyLeft, shoulder), Offset(bodyRight, shoulder), 1.5.dp.toPx())
                    drawLine(Color(0xFFBDBDBD), Offset(ridgeX, gableTop), Offset(ridgeX, shoulder), 1.dp.toPx())
                    // A blue label band around the middle, with a small white milk drop (a
                    // cow-spot would read the same at this size) so it's unmistakably milk, not
                    // juice or water.
                    val labelTop = h * 0.50f
                    val labelBottom = h * 0.76f
                    paperRect(Color(0xFF2E7BD6), Offset(bodyLeft, labelTop), Size(bodyRight - bodyLeft, labelBottom - labelTop))
                    val dropR = (labelBottom - labelTop) * 0.22f
                    val dropTop = labelTop + (labelBottom - labelTop) * 0.22f
                    val drop = Path().apply {
                        moveTo(ridgeX, dropTop)
                        quadraticTo(ridgeX + dropR, dropTop + dropR * 1.3f, ridgeX, dropTop + dropR * 2.1f)
                        quadraticTo(ridgeX - dropR, dropTop + dropR * 1.3f, ridgeX, dropTop)
                        close()
                    }
                    paperPath(drop, Color(0xFFFFFFFF))
                }
                // K7: while it pours, a white stream arcs from the carton's spout toward the
                // bowl's own station -- drawn past this Canvas's own left edge on purpose (a
                // Compose Canvas isn't clipped to its own bounds unless asked to be), since the
                // bowl sits in a separate station just to the milk's left.
                if (pouring && f() > 0.08f) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val alpha = (f() * 1.4f).coerceIn(0f, 1f)
                        val start = Offset(w * 0.3f, h * 0.18f)
                        val end = Offset(-w * 0.95f, h * 0.7f)
                        drawLine(Color.White.copy(alpha = 0.85f * alpha), start, end, w * 0.065f, cap = StrokeCap.Round)
                        drawLine(Color(0xFFEFEFEF).copy(alpha = 0.6f * alpha), start, end, w * 0.03f, cap = StrokeCap.Round)
                    }
                }
            }
        }
        // The bowl, now with pale batter inside (reuses the bowl shape).
        Prop.BATTER -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.1f, h * 0.34f)
                quadraticTo(w * 0.5f, h * 0.9f, w * 0.9f, h * 0.34f)
                quadraticTo(w * 0.5f, h * 0.48f, w * 0.1f, h * 0.34f)
                close()
            }
            paperPath(body, Color(0xFFFFF3E0))
            paperOval(Color(0xFFFFE9A8), Offset(w * 0.08f, h * 0.2f), Size(w * 0.84f, h * 0.22f))
        }
        // A flat frying pan (side view, no dials or flame): a dark oval with one handle. K5: top
        // offset/height are KitchenLayout's shared constants (summing to 1f) so the pan's own
        // drawn bottom sits exactly on the box's bottom edge -- a real screenshot showed it
        // floating about 15px above the counter during the bubbles beat.
        Prop.PAN -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            paperOval(Color(0xFF424242), Offset(w * 0.06f, h * KitchenLayout.PAN_ART_TOP), Size(w * 0.7f, h * KitchenLayout.PAN_ART_HEIGHT))
            paperRect(Color(0xFF5D4037), Offset(w * 0.72f, h * (KitchenLayout.PAN_ART_TOP + 0.04f)), Size(w * 0.26f, h * 0.06f))
        }
        // A golden pancake, two shades for a browned edge. K5: same shared-constant fix as the pan.
        Prop.PANCAKE -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            paperOval(Color(0xFFD08A3E), Offset(w * 0.14f, h * KitchenLayout.PANCAKE_ART_TOP), Size(w * 0.72f, h * KitchenLayout.PANCAKE_ART_HEIGHT))
            paperOval(Color(0xFFF2C06B), Offset(w * 0.22f, h * (KitchenLayout.PANCAKE_ART_TOP + 0.04f)), Size(w * 0.56f, h * 0.26f))
        }
        // Soft paper steam wisps, rising.
        Prop.STEAM -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            listOf(0.3f, 0.5f, 0.7f).forEachIndexed { i, x ->
                val wisp = Path().apply {
                    moveTo(w * x, h * 0.92f)
                    quadraticTo(w * (x - 0.1f), h * 0.6f, w * x, h * 0.4f)
                    quadraticTo(w * (x + 0.1f), h * 0.2f, w * x, h * 0.04f)
                }
                drawPath(wisp, Color.White.copy(alpha = 0.55f - i * 0.1f), style = Stroke(4.dp.toPx()))
            }
        }
        // K6: the butter-cutting mini-sequence's own small plate -- one pat (before the cut) or
        // two pieces, slid a little apart (after it); `butterCut` comes from VoiceStoryScreen (the
        // beat's own phase, not this prop's usual move/f -- the cut is a swipe, not a voice push).
        Prop.BUTTER -> {
            // The halves slide apart once cut (and stay apart on the beat after).
            val sep by animateFloatAsState(if (butterCut) 1f else 0f, tween(450), label = "butterSep")
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                paperOval(Color.White, Offset(w * 0.04f, h * 0.68f), Size(w * 0.92f, h * 0.24f))
                drawOval(Color(0xFFE0E0E0), Offset(w * 0.1f, h * 0.71f), Size(w * 0.8f, h * 0.15f), style = Stroke(1.5.dp.toPx()))
                val pw = w * 0.2f
                val ph = h * 0.26f
                val top = h * 0.48f
                paperRoundRect(Color(0xFFFFE066), Offset(w * 0.3f - w * 0.09f * sep, top), Size(pw, ph), CornerRadius(w * 0.04f))
                paperRoundRect(Color(0xFFFFE066), Offset(w * 0.5f + w * 0.09f * sep, top), Size(pw, ph), CornerRadius(w * 0.04f))
            }
        }
    }
}

/**
 * Rory's Kitchen's own "pop the bubbles" beat (K4): a real frying pan (dark, seen from a slight
 * side angle, with its handle) holding a golden pancake, with a few bubbles that pop into view on
 * his voice -- reuses [Prop.PAN]'s and [Prop.PANCAKE]'s own shapes so the pan this beat draws
 * always matches the one its "still" prop drew underneath, then adds the bubbles themselves
 * (small raised paper circles, not the generic open-air bubble's pale rings).
 */
@Composable
private fun KitchenPancakeBubbles(moving: Boolean, beat: VoiceBeat, f: () -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // K5: same shared KitchenLayout constants as Prop.PAN/Prop.PANCAKE below, not a second
        // copy of the old (floating) literals, so this beat's own pan/pancake always matches the
        // "still" one drawn underneath it, bottom flush with the box either way.
        paperOval(Color(0xFF424242), Offset(w * 0.06f, h * KitchenLayout.PAN_ART_TOP), Size(w * 0.7f, h * KitchenLayout.PAN_ART_HEIGHT))
        paperRect(Color(0xFF5D4037), Offset(w * 0.72f, h * (KitchenLayout.PAN_ART_TOP + 0.04f)), Size(w * 0.26f, h * 0.06f))
        paperOval(Color(0xFFD08A3E), Offset(w * 0.14f, h * KitchenLayout.PANCAKE_ART_TOP), Size(w * 0.72f, h * KitchenLayout.PANCAKE_ART_HEIGHT))
        paperOval(Color(0xFFF2C06B), Offset(w * 0.22f, h * (KitchenLayout.PANCAKE_ART_TOP + 0.04f)), Size(w * 0.56f, h * 0.26f))
        val popping = moving && beat.move == Move.POP
        // Each bubble pops bigger in turn as his voice pushes the move along, then fades away
        // near the very end (burst) rather than lingering once the pancake's ready to flip. K5:
        // a real screenshot taken before any voice push showed a bare pancake -- [grow] now has a
        // visible floor (0.4) from the moment this beat starts, so the 4 bubbles always read as
        // "cooking" rather than only appearing once he's spoken.
        val along = if (popping) (f() / 0.55f).coerceIn(0f, 1f) else 0f
        val burst = if (popping) ((f() - 0.8f) / 0.2f).coerceIn(0f, 1f) else 0f
        val spots = listOf(Offset(0.32f, 0.74f), Offset(0.46f, 0.82f), Offset(0.58f, 0.72f), Offset(0.7f, 0.8f))
        spots.forEachIndexed { i, spot ->
            val grow = 0.4f + 0.6f * (along * spots.size - i).coerceIn(0f, 1f)
            if (burst < 1f) {
                val r = w * 0.05f * grow * (1f - burst)
                val at = Offset(w * spot.x, h * spot.y)
                drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.95f * (1f - burst)), r, at)
                drawCircle(Color(0xFFB9752E).copy(alpha = 0.6f * (1f - burst)), r, at, style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** Soft paper steam, rising and fading in a gentle loop (Rory's Kitchen's ending, Ruby's K2c):
 * 3 wavy wisps, each a little behind the last, fading as it nears the top. */
@Composable
private fun PancakeSteam(modifier: Modifier = Modifier) {
    val still = LocalReduceMotion.current
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "pancakeSteam")
    val phase by t.loop(0f, 1f, 2200, mode = androidx.compose.animation.core.RepeatMode.Restart, easing = androidx.compose.animation.core.LinearEasing, label = "steamPhase")
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = if (still) 0f else phase
        // K6: 3 SHORT wisps, starting just above the stack's own top (py~0.26, see
        // PancakeStackArt) and fading out well before the top of the box -- the old version ran
        // the full height of the box, from near the counter up past the top, which read as tall
        // lines through the counter once the box itself shrank to a sane, Rory-relative size.
        listOf(0.3f, 0.5f, 0.68f).forEachIndexed { i, x ->
            val rise = (p + i / 3f) % 1f
            val wisp = Path().apply {
                moveTo(w * x, h * (0.24f - rise * 0.03f))
                quadraticTo(w * (x - 0.06f), h * (0.12f - rise * 0.05f), w * x, h * (0.02f - rise * 0.06f))
            }
            drawPath(wisp, Color.White.copy(alpha = 0.55f * (1f - rise)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/**
 * The stack of pancakes Bear's about to eat (Rory's Kitchen's ending, Ruby's K2c): three golden
 * paper pancakes, each a touch smaller than the one below, a pat of butter on top, and
 * [PancakeSteam] rising behind them. Once [bitten], the top pancake carries a bite-shaped notch
 * cut from its left edge (nearest Bear, who sits at the far left of the counter): a scalloped mark
 * (K2f), 3 overlapping circular "scallops" instead of one smooth oval, so it reads as a real bite
 * rather than a clean scoop, spanning about 35% of the pancake's own width and reaching about half
 * of its own height deep. The notch is cut out of BOTH the pancake's base and its lighter top
 * highlight (K2e fix: cutting only the base left an un-notched highlight oval painted straight
 * over it, so the bite never showed); each scallop is centred right on the edge so half falls
 * outside the pancake's own outline and the other half carves into it, same as the single-circle
 * bite K2e started from.
 */
@Composable
fun PancakeStackArt(bitten: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        // K6: 2-3 short wisps, just above the stack's own top (py~0.42 below), never the full
        // height of the box -- a tall line used to run straight down through where the counter
        // sits once the box itself shrank to a sane, Rory-relative size (see KitchenEndingScene).
        PancakeSteam(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // K6: a real plate under the stack (Ruby's old version floated free with no plate at
            // all) -- a wide, flat white oval sitting on the counter, the stack's own lowest
            // pancake resting just inside its own rim.
            paperOval(Color.White, Offset(w * 0.04f, h * 0.74f), Size(w * 0.92f, h * 0.2f))
            paperOval(Color(0xFFE0E0E0), Offset(w * 0.1f, h * 0.78f), Size(w * 0.8f, h * 0.1f))
            // Three puffy pancakes, each a touch smaller and lighter-edged than the one below, a
            // slightly uneven outline (not a perfect oval) so they read as stacked, not stamped.
            val layers = listOf(0.7f to 0.56f, 0.6f to 0.4f, 0.5f to 0.26f)
            layers.forEachIndexed { i, (pw, py) ->
                val top = Path().apply {
                    moveTo(w * (0.5f - pw / 2f), h * (py + 0.11f))
                    quadraticTo(w * (0.5f - pw * 0.3f), h * (py - 0.02f), w * 0.5f, h * (py + 0.01f))
                    quadraticTo(w * (0.5f + pw * 0.32f), h * (py - 0.015f), w * (0.5f + pw / 2f), h * (py + 0.1f))
                    quadraticTo(w * (0.5f + pw * 0.3f), h * (py + 0.21f), w * 0.5f, h * (py + 0.2f))
                    quadraticTo(w * (0.5f - pw * 0.32f), h * (py + 0.215f), w * (0.5f - pw / 2f), h * (py + 0.11f))
                    close()
                }
                val highlight = Path().apply {
                    addOval(Rect(Offset(w * (0.5f - pw * 0.36f), h * (py + 0.02f)), Size(w * pw * 0.72f, h * 0.14f)))
                }
                if (bitten && i == layers.lastIndex) {
                    val biteTotalW = w * pw * 0.4f
                    val scallopD = h * 0.2f * 0.5f
                    val scallopR = scallopD / 2f
                    val edgeX = w * (0.5f - pw / 2f)
                    val centerY = h * (py + 0.11f)
                    val step = (biteTotalW - scallopD) / 2f
                    val scallops = listOf(
                        Offset(edgeX, centerY - scallopR * 0.3f),
                        Offset(edgeX + step, centerY + scallopR * 0.35f),
                        Offset(edgeX + step * 2f, centerY - scallopR * 0.15f),
                    )
                    val bite = Path().apply {
                        scallops.forEach { c -> addOval(Rect(Offset(c.x - scallopR, c.y - scallopR), Size(scallopD, scallopD))) }
                    }
                    paperPath(Path().apply { op(top, bite, PathOperation.Difference) }, Color(0xFFD08A3E))
                    paperPath(Path().apply { op(highlight, bite, PathOperation.Difference) }, Color(0xFFF2C06B))
                } else {
                    paperPath(top, Color(0xFFD08A3E))
                    paperPath(highlight, Color(0xFFF2C06B))
                }
            }
            // The two cut butter pieces (K6: the butter-cutting step's own halves) on top, with a
            // syrup drip down the stack's own side.
            paperRoundRect(Color(0xFFFFE066), Offset(w * 0.37f, h * 0.16f), Size(w * 0.1f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(h * 0.02f))
            paperRoundRect(Color(0xFFFFE066), Offset(w * 0.52f, h * 0.15f), Size(w * 0.1f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(h * 0.02f))
            val drip = Path().apply {
                moveTo(w * 0.62f, h * 0.22f)
                quadraticTo(w * 0.68f, h * 0.4f, w * 0.64f, h * 0.6f)
                quadraticTo(w * 0.6f, h * 0.7f, w * 0.66f, h * 0.76f)
            }
            drawPath(drip, Color(0xFFB9752E), style = Stroke(w * 0.025f, cap = StrokeCap.Round))
        }
    }
}

/**
 * What shows over the dimmed scene while he talks: the thing to move glows gold with paper
 * sparkles turning round it, and his voice is three torn cream paper "wind strips" streaming from
 * Rory ([fromX], [fromY]) toward it, longer and brighter the louder he is ([push], read while drawing).
 */
@Composable
fun VoiceListenExtras(target: () -> Offset, fromX: Float, fromY: Float, glowing: Boolean, push: () -> Float, modifier: Modifier = Modifier) {
    val still = LocalReduceMotion.current
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "extras")
    val spin by t.loop(0f, 1f, 2400, mode = androidx.compose.animation.core.RepeatMode.Restart, easing = androidx.compose.animation.core.LinearEasing, label = "spin")
    val flow by t.loop(0f, 1f, 700, mode = androidx.compose.animation.core.RepeatMode.Restart, easing = androidx.compose.animation.core.LinearEasing, label = "flow")
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tg = target()
        val c = Offset(tg.x * w, tg.y * h)
        val u = size.minDimension
        if (glowing) {
            // The glow and its paper sparkles.
            drawCircle(Color(0xFFFFE082).copy(alpha = 0.45f), u * 0.11f, c)
            for (k in 0 until 3) {
                val a = 2f * PI.toFloat() * ((if (still) 0f else spin) + k / 3f)
                star(c + Offset(cos(a) * u * 0.12f, sin(a) * u * 0.12f), u * 0.022f, Color(0xFFFFF59D))
            }
        }
        val v = push()
        if (v > 0f) {
            // Three torn paper strips, flowing from Rory toward the target.
            val from = Offset(fromX * w, fromY * h)
            val dx = c.x - from.x
            val dy = c.y - from.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val nx = -dy / len
            val ny = dx / len
            for (k in 0 until 3) {
                val lane = (k - 1) * u * 0.035f
                val phase = ((if (still) 0.5f else flow) + k * 0.33f) % 1f
                val along = 0.15f + 0.6f * phase
                val stripLen = len * (0.12f + 0.18f * v)
                val start = from + Offset(dx * along + nx * lane, dy * along + ny * lane)
                val end = start + Offset(dx / len * stripLen, dy / len * stripLen)
                val path = Path().apply {
                    // A slightly ragged paper strip: two wavy long edges.
                    val thick = u * (0.012f + 0.01f * v)
                    moveTo(start.x + nx * thick, start.y + ny * thick)
                    quadraticTo((start.x + end.x) / 2 + nx * thick * 2.2f, (start.y + end.y) / 2 + ny * thick * 2.2f, end.x + nx * thick * 0.5f, end.y + ny * thick * 0.5f)
                    lineTo(end.x - nx * thick * 0.5f, end.y - ny * thick * 0.5f)
                    quadraticTo((start.x + end.x) / 2 - nx * thick * 0.4f, (start.y + end.y) / 2 - ny * thick * 0.4f, start.x - nx * thick, start.y - ny * thick)
                    close()
                }
                paperPath(path, Color(0xFFFFF3D6).copy(alpha = (0.55f + 0.45f * v) * (1f - kotlin.math.abs(phase - 0.5f))))
            }
        }
    }
}

/**
 * Hole-punch paper confetti: little paper circles burst from ([at], parts of the scene) when his
 * word succeeds, fall and fade, and are gone within 1.6 s ([f] 0 to 1 over that time).
 */
@Composable
fun HolePunchConfetti(at: Offset, f: () -> Float, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFFFFD54F), Color(0xFF4FC3F7), Color(0xFFAB47BC), Color(0xFF81C784), Color(0xFFFFB74D), Color.White)
    Canvas(modifier.fillMaxSize()) {
        val t = f()
        if (t <= 0f || t >= 1f) return@Canvas
        val u = size.minDimension
        for (k in 0 until 22) {
            val a = k * 2.4f
            val speed = 0.18f + 0.12f * ((k * 37) % 10) / 10f
            val d = u * speed * kotlin.math.sqrt(t)
            val c = Offset(size.width * at.x + cos(a) * d, size.height * at.y + sin(a) * d * 0.7f + u * 0.25f * t * t)
            paperCircle(colors[k % colors.size].copy(alpha = 1f - t), u * 0.011f, c)
        }
    }
}

/** K6: the counting beat's three big berries, waiting in a row on the counter beside the bowl
 * (the pan isn't out yet, so they use the ingredient and pan stations together, see
 * [KitchenLayout.berrySlot]); a tap hops one into the bowl in an arc, where it stays, sitting IN
 * the batter.
 *
 * K7: Timothy's screenshot showed a hopped berry floating well clear of the batter's own surface
 * (visibly above the bowl, not in it) -- [bowlTop] now settles it just below where
 * [Prop.BATTER]'s own highlight oval sits (that oval's local y is 0.2 to 0.42 of its box, centre
 * ~0.31; [bowlTop] targets the box's own local 0.5, a touch lower still, so it's unmistakably
 * inside, not balanced right on the rim), and the batter's own highlight is redrawn once more,
 * on top of the berries, after they've settled (see the `Canvas` after the loop below) -- the
 * same paper piece [Prop.BATTER] already draws, so the bowl's own near rim always matches it,
 * never a second, separately-tuned shape that could drift out of sync. The three WAITING berries
 * (not yet tapped) now sit at [KitchenLayout.berrySlot]'s own centres instead of a bare
 * `0.66 + i * 0.1` guess that drifted into the pan station once K7 widened it -- same tested
 * layout the counting beat's own tap targets already use. */
@Composable
private fun BoxWithConstraintsScope.KitchenCountRow(landscape: Boolean, hopped: Set<Int>, onTap: ((Int) -> Unit)?, onTarget: ((Int, Offset) -> Unit)?, showCount: Boolean = true) {
    val bowl = KitchenLayout.bowlBox(landscape)
    val counter = KitchenLayout.counterTop(landscape) + KitchenLayout.PROPS_MARGIN
    val size = minOf(maxWidth * 0.085f, maxHeight * 0.11f, 84.dp)
    val sizeFracY = size / maxHeight
    val sizeFracX = size / maxWidth
    val bowlCx = (bowl.l + bowl.r) / 2f
    // Props stand ON the counter line (their bottoms at the station's top), so the bowl's rim
    // is about one bowl-height above it.
    val bowlH = (minOf(maxWidth, maxHeight) * kitchenPropSize(Prop.BATTER, landscape)) / maxHeight
    val bowlTop = bowl.t - bowlH * 0.5f
    val pulse = rememberInfiniteTransition(label = "berryPulse").animateFloat(
        1f, 1.12f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p",
    )
    val next = (0..2).firstOrNull { it !in hopped }
    for (i in 0..2) {
        val slot = KitchenLayout.berrySlot(i, landscape)
        val restX = (slot.l + slot.r) / 2f
        val restY = counter - sizeFracY * 0.5f
        val inX = bowlCx + (i - 1) * sizeFracX * 0.5f
        val inY = bowlTop
        val t by animateFloatAsState(if (i in hopped) 1f else 0f, tween(550), label = "hop$i")
        val x = restX + (inX - restX) * t
        val y = restY + (inY - restY) * t - 0.18f * kotlin.math.sin(kotlin.math.PI.toFloat() * t)
        val s = if (i in hopped) 0.8f + 0.2f * (1f - t) else if (i == next) pulse.value else 1f
        Item(size, { x }, { y }, scale = { s }) {
            Box(
                Modifier.fillMaxSize().onGloballyPositioned { c ->
                    val pos = c.positionInWindow()
                    onTarget?.invoke(i, Offset(pos.x + c.size.width / 2f, pos.y + c.size.height / 2f))
                }.let { m ->
                    if (onTap == null || i in hopped) m
                    else m.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onTap(i) }
                },
            ) { BigBerry(Modifier.fillMaxSize()) }
        }
    }
    // K7: the bowl's own front rim (the batter's highlight oval, [Prop.BATTER]'s own shape),
    // redrawn once more here so it sits in front of (and partly hides) any berry that's hopped in
    // -- without this a settled berry read as floating above the batter, not nestled inside it.
    if (hopped.isNotEmpty()) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = bowlCx * this.size.width
            val rimY = bowlTop * this.size.height
            val rimW = bowlH * this.size.height * 1.5f
            paperOval(Color(0xFFFFE9A8), Offset(cx - rimW / 2f, rimY - rimW * 0.11f), Size(rimW, rimW * 0.22f))
        }
    }
    val count = hopped.size
    if (count > 0 && showCount) KitchenNumeral(count.toString(), bowl, landscape, key = count)
}

@Composable
private fun BigBerry(modifier: Modifier) = Canvas(modifier) {
    val r = size.minDimension * 0.42f
    val c = Offset(size.width / 2f, size.height * 0.55f)
    drawCircle(Color(0xFF3949AB), r, c)
    drawCircle(Color(0xFF5C6BC0), r * 0.82f, c.copy(y = c.y - r * 0.08f))
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.2f, Offset(c.x - r * 0.38f, c.y - r * 0.38f))
    // the little crown on top
    for (k in -1..1) drawLine(Color(0xFF283593), Offset(c.x, c.y - r * 0.72f), Offset(c.x + k * r * 0.28f, c.y - r * 0.98f), r * 0.12f, cap = StrokeCap.Round)
}

/** K6: a big numeral that pops up above [box] (scene fractions), counting as it changes. */
@Composable
private fun BoxWithConstraintsScope.KitchenNumeral(text: String, box: KitchenLayout.Rect, landscape: Boolean, key: Any = text) {
    val pop = remember(key) { Animatable(0.3f) }
    LaunchedEffect(key) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    val size = minOf(maxWidth * 0.12f, maxHeight * 0.16f, 120.dp)
    val cx = (box.l + box.r) / 2f
    // Above the prop itself (props stand on the counter line, box.t), never over it.
    val propH = (minOf(maxWidth, maxHeight) * kitchenPropSize(Prop.BATTER, landscape)) / maxHeight
    val cy = (box.t - propH * 1.15f - (size / maxHeight) * 0.5f).coerceAtLeast(0.08f)
    Item(size, { cx }, { cy }, scale = { pop.value }) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f), CircleShape), contentAlignment = Alignment.Center) {
            Text(text, fontSize = (size.value * 0.6f).sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
        }
    }
}

/** K7: a small drawn cartoon hand (palm, thumb, one extended index finger pointing down the way
 * it's swiping), [unit]-sized about its own palm, at [alpha] -- the butter-cutting step's own
 * one-time swipe hint ([KitchenCutOverlay]), in place of a bare sweeping dot: a child who can't
 * read the caption beside the line reads "a hand, dragging down" far more clearly than a dot. */
private fun DrawScope.paperPointingHandDown(center: Offset, unit: Float, alpha: Float) {
    if (alpha <= 0f) return
    val skin = Color(0xFFE3A76F).copy(alpha = alpha)
    val outline = Color(0xFF8D5A3B).copy(alpha = alpha)
    val palmW = unit * 1f
    val palmH = unit * 0.8f
    val palmTop = center.y - palmH * 0.7f
    val palmTopLeft = Offset(center.x - palmW / 2f, palmTop)
    drawOval(skin, palmTopLeft, Size(palmW, palmH))
    drawOval(outline, palmTopLeft, Size(palmW, palmH), style = Stroke(unit * 0.07f))
    // a small thumb to the side, so it reads as a hand, not a thumb-less mitt
    val thumbTopLeft = Offset(center.x - palmW * 0.62f, palmTop + palmH * 0.15f)
    val thumbSize = Size(unit * 0.38f, unit * 0.3f)
    drawOval(skin, thumbTopLeft, thumbSize)
    drawOval(outline, thumbTopLeft, thumbSize, style = Stroke(unit * 0.05f))
    // the extended index finger, pointing down -- the swipe's own direction
    val fingerW = unit * 0.3f
    val fingerH = unit * 0.9f
    val fingerTopLeft = Offset(center.x - fingerW / 2f, palmTop + palmH * 0.55f)
    val fingerSize = Size(fingerW, fingerH)
    val corner = CornerRadius(fingerW / 2f)
    drawRoundRect(skin, fingerTopLeft, fingerSize, corner)
    drawRoundRect(outline, fingerTopLeft, fingerSize, corner, style = Stroke(unit * 0.06f))
}

/** K6: the swipe-to-cut step: a glowing dotted line over the butter, a one-time hand-swipe hint,
 * and a rounded wooden toy knife that follows the finger only while cutting. Any drag of at least
 * [KitchenCut.THRESHOLD_DP] counts, any direction; a tap on the line is the backup. */
@Composable
private fun BoxWithConstraintsScope.KitchenCutOverlay(landscape: Boolean, onCut: () -> Unit, onTarget: ((Int, Offset) -> Unit)?) {
    val box = KitchenLayout.ingredientBox(landscape)
    val density = LocalDensity.current
    val wPx = constraints.maxWidth.toFloat()
    val hPx = constraints.maxHeight.toFloat()
    val cx = (box.l + box.r) / 2f * wPx
    val butter = with(density) { (minOf(maxWidth, maxHeight) * kitchenPropSize(Prop.BUTTER, landscape)).toPx() }
    val cy = box.t * hPx - butter * 0.45f
    val zone = butter * 1.7f
    var done by remember { mutableStateOf(false) }
    var knife by remember { mutableStateOf<Offset?>(null) }
    var dragged by remember { mutableStateOf(0f) }
    val glow = rememberInfiniteTransition(label = "cutGlow").animateFloat(0.45f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")
    val hint = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(900); hint.animateTo(1f, tween(1300)) }
    // The knife stays under his finger until he lets go, even once the cut has fired.
    fun fire() { if (!done) { done = true; onCut() } }
    Box(
        Modifier.offset { IntOffset((cx - zone / 2f).roundToInt(), (cy - zone / 2f).roundToInt()) }.size(with(density) { zone.toDp() })
            .onGloballyPositioned { c ->
                val pos = c.positionInWindow()
                onTarget?.invoke(0, Offset(pos.x + c.size.width / 2f, pos.y + c.size.height / 2f))
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { knife = it; dragged = 0f },
                    onDragEnd = { knife = null }, onDragCancel = { knife = null },
                ) { change, amount ->
                    knife = change.position
                    dragged += amount.getDistance()
                    if (KitchenCut.crossed(dragged, density.density)) fire()
                }
            }
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                // tap backup: the knife shows briefly on the line, then the cut
                knife = Offset(zone / 2f, zone * 0.6f); fire()
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (!done) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(size.width * 0.05f, size.width * 0.04f))
                val top = Offset(size.width / 2f, size.height * 0.12f)
                val bot = Offset(size.width / 2f, size.height * 0.88f)
                drawLine(Color(0xFFFFF59D).copy(alpha = 0.5f * glow.value), top, bot, size.width * 0.07f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF8F00).copy(alpha = glow.value), top, bot, size.width * 0.025f, cap = StrokeCap.Round, pathEffect = dash)
            }
            val h = hint.value
            if (!done && knife == null && h > 0f && h < 1f) {
                // K7: the one-time hand-swipe hint was a bare sweeping dot -- a drawn cartoon
                // hand (palm, thumb and one pointing index finger) reads far more clearly as
                // "swipe down here" to a child who can't read the caption beside it.
                val p = Offset(size.width / 2f + size.width * 0.06f, size.height * (0.15f + 0.7f * h))
                val a = kotlin.math.sin(kotlin.math.PI.toFloat() * h)
                paperPointingHandDown(p, size.width * 0.22f, a)
            }
            knife?.let { k ->
                // rounded wooden toy knife: a fat round-ended blade and a handle, never pointy
                val bladeW = size.width * 0.09f
                drawLine(Color(0xFFD7B98E), k.copy(y = k.y - size.height * 0.02f), k.copy(y = k.y - size.height * 0.24f), bladeW, cap = StrokeCap.Round)
                drawLine(Color(0xFF8D6E63), k.copy(y = k.y - size.height * 0.26f), k.copy(y = k.y - size.height * 0.42f), bladeW * 0.8f, cap = StrokeCap.Round)
            }
        }
    }
}
