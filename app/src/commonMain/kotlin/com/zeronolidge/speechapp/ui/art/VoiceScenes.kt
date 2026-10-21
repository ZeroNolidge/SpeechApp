package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Backdrop
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.KitchenEndStage
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
    Prop.BOWL -> 0.26f
    Prop.MILK -> 0.2f
    Prop.BATTER -> 0.26f
    Prop.PAN -> 0.34f
    Prop.PANCAKE -> 0.24f
    Prop.STEAM -> 0.2f
}

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

/** Something placed by its centre at ([x], [y]) (parts of the scene), moved while drawing. */
@Composable
private fun BoxWithConstraintsScope.Item(
    size: Dp,
    x: () -> Float,
    y: () -> Float,
    alpha: () -> Float = { 1f },
    rotation: () -> Float = { 0f },
    scale: () -> Float = { 1f },
    content: @Composable () -> Unit,
) {
    val wPx = constraints.maxWidth
    val hPx = constraints.maxHeight
    Box(
        Modifier
            .offset { IntOffset((x() * wPx - size.toPx() / 2).roundToInt(), (y() * hPx - size.toPx() / 2).roundToInt()) }
            .size(size)
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
    /** Rory's Kitchen only: reports the scene's own on-screen box, for the KITCHEN rects log. */
    onSceneRect: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
) {
    val still = LocalReduceMotion.current
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "beat")
    val wobble by t.loop(0f, 1f, 4000, mode = androidx.compose.animation.core.RepeatMode.Restart, easing = androidx.compose.animation.core.LinearEasing, label = "wobble")
    Scenery(beat.backdrop.land, modifier.fillMaxSize(), seed = 30 + beat.backdrop.ordinal * 7) {
        BoxWithConstraints(
            Modifier.fillMaxSize().let { m -> if (onSceneRect == null) m else m.onGloballyPositioned(onSceneRect) },
        ) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            BackdropExtras(beat.backdrop, landscape)
            if (beat.backdrop == Backdrop.KITCHEN) KitchenCharacters(landscape, kitchenTalking, kitchenRoryVisible)
            beat.still?.let { sp ->
                val at = stillSpot(beat.move)
                Item(unit * propSize(sp) * 0.8f, { at.x }, { at.y }) { PropArt(sp, { 1f }, null, beat) }
            }
            fun now() = pose(beat.move, beat.prop, f(), strength, push(), if (still) 0f else wobble)
            // While the pattern puzzle is up, the beat's own prop is tucked into a safe corner,
            // clear of the pattern row above the choice strip (it used to sit left-of-centre and
            // cover the row's first balloon); it stays small but visible, Bear and all.
            Item(
                unit * propSize(beat.prop) * (if (puzzleSafe) 0.7f else 1f),
                x = { if (puzzleSafe) 0.82f else now().x }, y = { if (puzzleSafe) 0.8f else now().y },
                alpha = { now().alpha }, rotation = { now().rotation }, scale = { now().scale },
            ) { PropArt(beat.prop, f, beat.propEmoji, beat) }
            // The light changes with the move: the sun comes up (dawn) or night falls (dusk).
            if (beat.light != Light.SAME) {
                Canvas(Modifier.fillMaxSize()) {
                    val a = if (beat.light == Light.DAWN) 0.55f * (1f - f()) else 0.4f * f()
                    drawRect(Color(0xFF1A2350).copy(alpha = a))
                }
            }
        }
    }
}

/**
 * Rory's Kitchen's own ending (Ruby's K2c): once Bear's had his bite, this replaces the usual
 * beat scene (no more moving prop to drive, no score or summary) with the stack of pancakes he's
 * eating ([PancakeStackArt]) in place on the counter, Bear leaning in for his bite from
 * [KitchenEndStage.BITE] on, same backdrop and Rory as every other kitchen beat.
 */
@Composable
fun KitchenEndingScene(stage: KitchenEndStage, landscape: Boolean = false, modifier: Modifier = Modifier) {
    val bitten = stage != KitchenEndStage.STACK
    val leaning = stage == KitchenEndStage.BITE || bitten
    Scenery(Land.MARKET, modifier.fillMaxSize(), seed = 30 + Backdrop.KITCHEN.ordinal * 7) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            BackdropExtras(Backdrop.KITCHEN, landscape)
            KitchenCharacters(landscape, talking = false, roryVisible = true, bearLeaning = leaning)
            val ct = KitchenLayout.counterTop(landscape)
            Item(unit * 0.4f, { 0.56f }, { ct + (1f - ct) * 0.42f }) { PancakeStackArt(bitten) }
        }
    }
}

/** The little extras that turn a land's scenery into a backdrop: water, an island, a path, sky
 * clouds. [landscape] only changes [Backdrop.KITCHEN]'s wall/counter split (see [KitchenLayout]). */
@Composable
private fun BoxWithConstraintsScope.BackdropExtras(b: Backdrop, landscape: Boolean = false) {
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
            Backdrop.KITCHEN -> {
                // A soft wall above, and a warm low wooden counter below it, side view. Landscape
                // draws a shorter wall (taller counter), so the counter keeps most of the scene
                // once VoiceStory's landscape lift clips the top clear of the choice strip
                // (Ruby, K2a: it used to clip into the counter on a landscape phone).
                val ct = KitchenLayout.counterTop(landscape)
                paperRect(Color(0xFFFFF3E3), Offset(0f, 0f), Size(w, h * (ct + 0.06f)))
                paperRect(Color(0xFFC9773A), Offset(0f, h * ct), Size(w, h * (1f - ct)))
                paperRect(Color(0xFFE6C38E), Offset(0f, h * ct), Size(w, h * 0.045f))
                listOf(0.28f, 0.52f, 0.76f).forEach { rel ->
                    val py = ct + rel * (1f - ct)
                    drawLine(Color(0xFF8C5A3A).copy(alpha = 0.55f), Offset(0f, h * py), Offset(w, h * py), 2.dp.toPx())
                }
            }
            Backdrop.FOREST, Backdrop.MEADOW -> {}
        }
    }
    if (b == Backdrop.ISLAND) Item(unit * 0.16f, { 0.84f }, { 0.5f }) { Emoji("🌴", unit * 0.16f) }
}

/** Rory's Kitchen's own white paper toque (Ruby, K2b): a fixed prop, not an unlockable piece
 * (see Critter.kt's "hat_chef" case), so it never goes through the buddy's [Catalog]. */
private val ChefHat = Cosmetic("hat_chef", Slot.HAT, "Chef hat", 0xFFFFFFFF)

/** A simple paper stool for Bear to sit on: a round seat and three warm-red legs. */
private fun DrawScope.paperStool() {
    val w = size.width
    val h = size.height
    val legColor = Color(0xFFB23A2E)
    val seatY = h * 0.2f
    listOf(w * 0.2f, w * 0.5f, w * 0.8f).forEach { x ->
        drawLine(legColor, Offset(x, seatY), Offset(x, h), w * 0.1f, cap = StrokeCap.Round)
    }
    paperOval(Color(0xFFD1492F), Offset(w * 0.08f, 0f), Size(w * 0.84f, seatY * 1.3f))
}

/**
 * Rory's Kitchen's own characters (Ruby, K2b), kept in explicit lanes (see [KitchenLayout]) so
 * neither ever reaches the counter's props or the choice strip below: Bear, on a simple paper
 * stool, at the far end of the counter from the pan; Rory, standing behind the counter in his
 * chef's hat, his lower body hidden by the counter front (redrawn over his legs, the same trick
 * as Bear riding in the balloon's basket above). [talking] follows Rory's mouth while he models
 * a word; [roryVisible] hides him while the big listening Rory takes over (`listenLookGone()` at
 * the call site), so the two Rorys are never both shown. [bearLeaning] is the ending's own
 * (Ruby, K2c): Bear tips in toward the counter's props for his bite, cheering instead of just
 * standing happy.
 */
@Composable
private fun BoxWithConstraintsScope.KitchenCharacters(landscape: Boolean, talking: Boolean, roryVisible: Boolean, bearLeaning: Boolean = false) {
    val w = maxWidth
    val h = maxHeight
    val bear = KitchenLayout.bearBox()
    Box(
        Modifier.offset(x = w * bear.l, y = h * bear.t).size(w * (bear.r - bear.l), h * (bear.b - bear.t))
            .graphicsLayer {
                if (bearLeaning) {
                    rotationZ = -8f
                    translationX = size.width * 0.18f
                    scaleX = 1.06f
                    scaleY = 1.06f
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.55f).align(Alignment.BottomCenter)) { paperStool() }
        Critter(Species.BEAR, Modifier.fillMaxHeight(0.85f), mood = if (bearLeaning) Mood.CHEER else Mood.HAPPY)
    }
    val rory = KitchenLayout.roryBox(landscape)
    Box(
        Modifier.offset(x = w * rory.l, y = h * rory.t).size(w * (rory.r - rory.l), h * (rory.b - rory.t)),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (roryVisible) {
            Critter(
                Species.ROBOT, Modifier.fillMaxWidth(), Color(0xFFFFCC80),
                outfit = Outfit(hat = ChefHat), mood = Mood.HAPPY, talking = talking,
            )
        }
    }
    // The counter front, redrawn over Rory's lower body: behind the counter, only his top half
    // shows (nothing to hide while he's not drawn at all). Scoped to Rory's own lane width only
    // (K2d fix): drawn full-width, this used to repaint straight over Bear's lane too, after his
    // Critter had already been drawn, so he never showed at all.
    if (roryVisible) {
        val ct = KitchenLayout.counterTop(landscape)
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * rory.l
            val width = size.width * (rory.r - rory.l)
            paperRect(Color(0xFFC9773A), Offset(left, size.height * ct), Size(width, size.height * (1f - ct)))
            paperRect(Color(0xFFE6C38E), Offset(left, size.height * ct), Size(width, size.height * 0.045f))
        }
    }
}

/**
 * The paper art for each prop. [f] (read while drawing) lets a prop show its own paper motion as
 * the move goes: the sail bellies out, the balloon fills, the bubble's layers burst apart, the
 * leaves flip to their darker side, Owl wakes up. [emoji] shows his own word instead.
 */
@Composable
private fun PropArt(prop: Prop, f: () -> Float, emoji: String?, beat: VoiceBeat) {
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
        Prop.BUBBLE -> Canvas(Modifier.fillMaxSize()) {
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
            // berries, Backdrop.FOREST, keep their cherry look below).
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension * 0.17f
                listOf(Offset(0.32f, 0.5f), Offset(0.58f, 0.36f), Offset(0.68f, 0.62f)).forEach { spot ->
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
        // Rory's Kitchen: a mixing bowl, side view (a wide rim over a rounded body).
        Prop.BOWL -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.1f, h * 0.34f)
                quadraticTo(w * 0.5f, h * 0.9f, w * 0.9f, h * 0.34f)
                quadraticTo(w * 0.5f, h * 0.48f, w * 0.1f, h * 0.34f)
                close()
            }
            paperPath(body, Color(0xFFFFF3E0))
            paperOval(Color(0xFFFFFDF7), Offset(w * 0.06f, h * 0.22f), Size(w * 0.88f, h * 0.2f))
        }
        // A little milk jug: a straight-sided body with a pouring lip.
        Prop.MILK -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Path().apply {
                moveTo(w * 0.28f, h * 0.16f)
                lineTo(w * 0.7f, h * 0.16f)
                lineTo(w * 0.82f, h * 0.3f)
                lineTo(w * 0.7f, h * 0.9f)
                lineTo(w * 0.3f, h * 0.9f)
                lineTo(w * 0.18f, h * 0.3f)
                close()
            }
            paperPath(body, Color(0xFFFFFFFF))
            drawLine(Color(0xFFE0E0E0), Offset(w * 0.8f, h * 0.38f), Offset(w * 0.92f, h * 0.58f), 4.dp.toPx())
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
        // A flat frying pan (side view, no dials or flame): a dark oval with one handle.
        Prop.PAN -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            paperOval(Color(0xFF424242), Offset(w * 0.06f, h * 0.56f), Size(w * 0.7f, h * 0.22f))
            paperRect(Color(0xFF5D4037), Offset(w * 0.72f, h * 0.6f), Size(w * 0.26f, h * 0.06f))
        }
        // A golden pancake, two shades for a browned edge.
        Prop.PANCAKE -> Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            paperOval(Color(0xFFD08A3E), Offset(w * 0.14f, h * 0.3f), Size(w * 0.72f, h * 0.4f))
            paperOval(Color(0xFFF2C06B), Offset(w * 0.22f, h * 0.34f), Size(w * 0.56f, h * 0.26f))
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
        listOf(0.28f, 0.5f, 0.72f).forEachIndexed { i, x ->
            val rise = (p + i / 3f) % 1f
            val wisp = Path().apply {
                moveTo(w * x, h * (0.96f - rise * 0.1f))
                quadraticTo(w * (x - 0.09f), h * (0.56f - rise * 0.12f), w * x, h * (0.3f - rise * 0.1f))
                quadraticTo(w * (x + 0.09f), h * (0.12f - rise * 0.1f), w * x, h * (-0.04f - rise * 0.08f))
            }
            drawPath(wisp, Color.White.copy(alpha = 0.6f * (1f - rise)), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/**
 * The stack of pancakes Bear's about to eat (Rory's Kitchen's ending, Ruby's K2c): three golden
 * paper pancakes, each a touch smaller than the one below, a pat of butter on top, and
 * [PancakeSteam] rising behind them. Once [bitten], the top pancake carries a bite-shaped notch
 * (a plain circle cut from its edge, `PathOperation.Difference`) where Bear's had his bite.
 */
@Composable
fun PancakeStackArt(bitten: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        PancakeSteam(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val layers = listOf(0.86f to 0.62f, 0.76f to 0.46f, 0.66f to 0.3f)
            layers.forEachIndexed { i, (pw, py) ->
                val top = Path().apply { addOval(Rect(Offset(w * (0.5f - pw / 2f), h * py), Size(w * pw, h * 0.22f))) }
                val shape = if (bitten && i == layers.lastIndex) {
                    // Bear sits at the far left of the counter: the bite comes out of the top
                    // pancake's left edge, nearest him (K2d fix; it was cut from the right edge,
                    // the side away from Bear).
                    val bite = Path().apply {
                        addOval(Rect(Offset(w * (0.5f - pw * 0.4f), h * (py - 0.01f)), Size(w * pw * 0.26f, h * 0.2f)))
                    }
                    Path().apply { op(top, bite, PathOperation.Difference) }
                } else {
                    top
                }
                paperPath(shape, Color(0xFFD08A3E))
                paperOval(Color(0xFFF2C06B), Offset(w * (0.5f - pw * 0.39f), h * (py + 0.02f)), Size(w * pw * 0.78f, h * 0.15f))
            }
            // A pat of butter, melting on top.
            paperRoundRect(Color(0xFFFFE066), Offset(w * 0.44f, h * 0.24f), Size(w * 0.12f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(h * 0.02f))
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
