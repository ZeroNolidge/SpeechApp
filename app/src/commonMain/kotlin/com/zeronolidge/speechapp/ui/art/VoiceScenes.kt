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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceStory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * How far each beat of a voice story has gone. [act] is the current beat's action, 0 to 1 (read
 * only while drawing, so the animation doesn't recompose the scene). A beat's action always
 * reaches its end ([a]: the boat gets to the island, the duck climbs aboard); a soft try only makes
 * the flourish smaller ([s]: fewer confetti, a smaller balloon, a tighter loop).
 */
class BeatProgress(val beat: Int, private val strengths: Map<Int, Float>, private val act: () -> Float) {
    /** 0 to 1: how far beat [i]'s action has moved things. */
    fun a(i: Int): Float = when {
        i < beat -> 1f
        i == beat && strengths.containsKey(i) -> act()
        else -> 0f
    }

    /** How big beat [i]'s flourish is: 1 for a clear voice, about half for a soft try. */
    fun s(i: Int): Float = strengths[i] ?: 1f

    /** Beat [i]'s action has started (his word or the button). */
    fun triggered(i: Int): Boolean = i < beat || strengths.containsKey(i)
}

private fun bump(f: Float) = sin(PI.toFloat() * f.coerceIn(0f, 1f))
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Something placed at ([x], [y]) as parts of the scene's width and height (its centre), moved while drawing. */
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

/** The picture for one voice story, drawn in paper, moving as his words make things happen. */
@Composable
fun VoiceStoryScene(story: VoiceStory, progress: BeatProgress, modifier: Modifier = Modifier) {
    when (story.id) {
        "boat" -> BoatScene(story, progress, modifier)
        "owl" -> OwlScene(story, progress, modifier)
        else -> BalloonScene(story, progress, modifier)
    }
}

/** Pip's Boat Trip: a river, a dock, a paper boat with Pip, then a bubble, a duck, a wave and an island. */
@Composable
private fun BoatScene(story: VoiceStory, p: BeatProgress, modifier: Modifier) {
    Scenery(Land.LAGOON, modifier.fillMaxSize(), seed = 41) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            fun boatX() = 0.16f + 0.15f * p.a(0) + 0.13f * p.a(2) + 0.08f * p.a(4) + 0.18f * p.a(5)
            fun boatY() = 0.62f - 0.16f * bump(p.a(4)) * p.s(4) - (if (p.beat == 0) 0.02f * bump(p.a(0)) else 0f)
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // The river, with a few light ripples.
                paperRect(Color(0xFF4FC3F7), Offset(0f, h * 0.6f), Size(w, h * 0.4f))
                listOf(0.68f, 0.78f, 0.9f).forEachIndexed { i, y ->
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(w * (0.1f + i * 0.2f), h * y), Offset(w * (0.3f + i * 0.2f), h * y), 3.dp.toPx())
                }
                // The dock.
                paperRect(Color(0xFF8D6E63), Offset(0f, h * 0.55f), Size(w * 0.14f, h * 0.06f))
                paperRect(Color(0xFF6D4C41), Offset(w * 0.03f, h * 0.6f), Size(w * 0.02f, h * 0.12f))
                paperRect(Color(0xFF6D4C41), Offset(w * 0.1f, h * 0.6f), Size(w * 0.02f, h * 0.12f))
                // The rock the duck sits on, and later the island.
                if (p.beat >= 3) paperOval(Color(0xFF90A4AE), Offset(w * 0.5f, h * 0.6f), Size(w * 0.11f, h * 0.07f))
                if (p.beat >= 5) {
                    paperOval(Color(0xFFFFE0B2), Offset(w * 0.72f, h * 0.6f), Size(w * 0.28f, h * 0.12f))
                }
                // The wave: a big paper curl that the boat rides over.
                if (p.beat == 4) {
                    val k = 1f - 0.6f * p.a(4)
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.66f)
                        quadraticTo(w * 0.62f, h * (0.66f - 0.2f * k), w * 0.74f, h * 0.66f)
                        close()
                    }
                    paperPath(path, Color(0xFF29B6F6))
                }
                // Night for the goodbye.
                if (p.beat == 6) drawRect(Color(0xFF1A2350).copy(alpha = 0.35f * p.a(6)))
            }
            if (p.beat >= 5) Item(unit * 0.16f, { 0.86f }, { 0.52f }) { Emoji("🌴", unit * 0.16f) }
            // The bubble: it bursts into confetti.
            if (p.beat == 1) {
                Item(unit * 0.26f, { 0.5f }, { 0.46f }, alpha = { 1f - p.a(1) }, scale = { 1f + 0.5f * p.a(1) }) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFFE1F5FE).copy(alpha = 0.55f), size.minDimension / 2)
                        drawCircle(Color.White, size.minDimension / 2, style = Stroke(4.dp.toPx()))
                        drawCircle(Color.White, size.minDimension * 0.08f, Offset(size.width * 0.32f, size.height * 0.3f))
                    }
                }
                Confetti({ p.a(1) }, count = if (p.s(1) < 1f) 8 else 18, cx = 0.5f, cy = 0.46f)
            }
            // The duck (or his own word): on the rock, then into the boat.
            if (p.beat >= 3) {
                Item(
                    unit * 0.12f,
                    x = { lerp(0.555f, boatX() + 0.06f, p.a(3)) },
                    y = { lerp(0.56f, boatY() - 0.07f, p.a(3)) - 0.08f * bump(p.a(3)) },
                    rotation = { if (p.beat == 6) 15f * sin(p.a(6) * 6f * PI.toFloat()) else 0f },
                ) { Emoji(story.prop.emoji, unit * 0.12f) }
            }
            // The boat, with Pip in it.
            Item(unit * 0.3f, { boatX() }, { boatY() - 0.08f }) {
                val sail = { if (p.beat < 2) 1f else lerp(0.3f, 1f, p.a(2)) }
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Mast and sail (the sail droops until his "blow").
                    drawLine(Color(0xFF5D4037), Offset(w * 0.55f, h * 0.12f), Offset(w * 0.55f, h * 0.72f), 4.dp.toPx())
                    val f = sail()
                    val sailPath = Path().apply {
                        moveTo(w * 0.57f, h * 0.14f)
                        lineTo(w * (0.57f + 0.33f * f), h * (0.68f - 0.2f * (1f - f)))
                        lineTo(w * 0.57f, h * 0.68f)
                        close()
                    }
                    paperPath(sailPath, Color(0xFFFFF8E1))
                    // The hull.
                    val hull = Path().apply {
                        moveTo(w * 0.05f, h * 0.7f)
                        lineTo(w * 0.95f, h * 0.7f)
                        lineTo(w * 0.8f, h * 0.92f)
                        lineTo(w * 0.2f, h * 0.92f)
                        close()
                    }
                    paperPath(hull, Color(0xFF26A69A))
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                    Critter(
                        Species.PUPPY,
                        Modifier.size(unit * 0.12f, unit * 0.15f).offset(x = unit * 0.05f, y = -unit * 0.07f),
                        mood = if (p.beat == 6 && p.triggered(6)) Mood.SLEEP else Mood.HAPPY,
                    )
                }
            }
        }
    }
}

/** Sleepy Owl's Morning: a dark forest, a branch with Owl asleep; the sun, warm beams, leaves, berries, a loop. */
@Composable
private fun OwlScene(story: VoiceStory, p: BeatProgress, modifier: Modifier) {
    Scenery(Land.FOREST, modifier.fillMaxSize(), seed = 52) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            // The sun comes up from behind the hill.
            Item(unit * 0.24f, { 0.8f }, { lerp(0.78f, 0.24f, p.a(0)) }) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * 0.32f
                    for (k in 0 until 8) {
                        val a = k * PI.toFloat() / 4f
                        drawLine(Color(0xFFFFCA28), center + Offset(cos(a) * r * 1.2f, sin(a) * r * 1.2f), center + Offset(cos(a) * r * 1.55f, sin(a) * r * 1.55f), 5.dp.toPx())
                    }
                    paperCircle(Color(0xFFFFD54F), r, center)
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // A hill in front of the sun, the branch, and the path.
                paperOval(Color(0xFF558B2F), Offset(w * 0.6f, h * 0.68f), Size(w * 0.5f, h * 0.4f))
                paperRect(Color(0xFF795548), Offset(w * 0.22f, h * 0.5f), Size(w * 0.48f, h * 0.035f))
                paperRect(Color(0xFFD7CCC8), Offset(0f, h * 0.86f), Size(w, h * 0.06f))
                // It's dark until the sun is up.
                drawRect(Color(0xFF1A2350).copy(alpha = 0.55f * (1f - p.a(0))))
            }
            // Warm sunbeams wrap round Owl.
            if (p.beat >= 2) {
                Item(unit * 0.36f, { 0.47f }, { 0.36f }, alpha = { (0.7f * p.a(2) * p.s(2)).coerceAtLeast(if (p.beat > 2) 0.25f else 0f) }, rotation = { 20f * p.a(2) }) {
                    Canvas(Modifier.fillMaxSize()) {
                        for (k in 0 until 6) {
                            val a = k * PI.toFloat() / 3f
                            paperRect(Color(0xFFFFEE58).copy(alpha = 0.7f), center + Offset(cos(a) * size.width * 0.3f, sin(a) * size.height * 0.3f) - Offset(size.width * 0.05f, size.height * 0.05f), Size(size.width * 0.1f, size.height * 0.1f))
                        }
                    }
                }
            }
            // Owl: on the branch, asleep until his word; later a big loop.
            fun loopAngle() = if (p.beat == 5) 2f * PI.toFloat() * p.a(5) else 0f
            val loopR = 0.16f * p.s(5)
            Item(
                unit * 0.22f,
                x = { 0.47f + loopR * sin(loopAngle()) },
                y = { 0.41f - loopR * (1f - cos(loopAngle())) },
            ) {
                Critter(Species.OWL, Modifier.fillMaxSize(), mood = if (p.triggered(1)) Mood.HAPPY else Mood.SLEEP)
            }
            if (!p.triggered(1)) Item(unit * 0.1f, { 0.6f }, { 0.28f }) { Emoji("💤", unit * 0.1f) }
            // Leaves on the path, blown aside by his "sss".
            if (p.beat <= 3) {
                listOf(0.32f, 0.44f, 0.56f).forEachIndexed { i, x ->
                    val dir = if (i == 0) -1f else 1f
                    Item(unit * 0.09f, { x + dir * 0.4f * p.a(3) }, { 0.86f - 0.12f * bump(p.a(3)) * p.s(3) }, rotation = { 200f * p.a(3) }) { Emoji("🍂", unit * 0.09f) }
                }
            }
            // Berries (or his own word) tumble down to Owl.
            if (p.beat == 4) {
                val n = if (p.s(4) < 1f) 2 else 3
                for (i in 0 until n) {
                    Item(unit * 0.08f, { 0.4f + 0.07f * i }, { lerp(-0.05f, 0.45f, (p.a(4) * 1.3f - i * 0.15f).coerceIn(0f, 1f)) }) { Emoji(story.prop.emoji, unit * 0.08f) }
                }
            }
            // A feather drifts down at the goodbye.
            if (p.beat == 6) {
                Item(unit * 0.1f, { 0.5f + 0.06f * sin(p.a(6) * 4f * PI.toFloat()) }, { lerp(0.42f, 0.82f, p.a(6)) }, alpha = { p.a(6) * 3f }, rotation = { 30f * sin(p.a(6) * 4f * PI.toFloat()) }) {
                    Canvas(Modifier.fillMaxSize()) {
                        paperOval(Color(0xFFFFF3E0), Offset(size.width * 0.35f, 0f), Size(size.width * 0.3f, size.height))
                        drawLine(Color(0xFFA1887F), Offset(size.width / 2, size.height * 0.05f), Offset(size.width / 2, size.height), 2.dp.toPx())
                    }
                }
            }
        }
    }
}

/** The Balloon Picnic: a flat balloon and Bear in a basket; up, past a bird, down to the cupcakes, bubbles, the moon. */
@Composable
private fun BalloonScene(story: VoiceStory, p: BeatProgress, modifier: Modifier) {
    Scenery(Land.MEADOW, modifier.fillMaxSize(), seed = 63) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val unit = minOf(maxWidth, maxHeight * 1.4f)
            fun alt() = 0.3f * p.a(1) - 0.26f * p.a(3)
            fun basketY() = 0.8f - alt()
            // The picnic blanket and cupcakes below.
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (p.beat >= 3) {
                    paperRect(Color(0xFFFFCDD2), Offset(w * 0.6f, h * 0.84f), Size(w * 0.3f, h * 0.08f))
                    for (k in 0 until 5) paperRect(Color.White.copy(alpha = 0.6f), Offset(w * (0.6f + k * 0.06f), h * 0.84f), Size(w * 0.03f, h * 0.08f))
                }
                if (p.beat == 6) drawRect(Color(0xFF1A2350).copy(alpha = 0.4f * p.a(6)))
            }
            if (p.beat >= 3) {
                Item(unit * 0.09f, { 0.68f }, { 0.82f }) { Emoji("🧁", unit * 0.09f) }
                Item(unit * 0.09f, { 0.8f }, { 0.82f }) { Emoji("🧁", unit * 0.09f) }
            }
            if (p.beat == 6) Item(unit * 0.16f, { 0.8f }, { lerp(0.5f, 0.18f, p.a(6)) }) {
                Canvas(Modifier.fillMaxSize()) { paperCircle(Color(0xFFFFF9C4), size.minDimension * 0.4f, center) }
            }
            // The balloon: flat at first, bigger with his "blow" (a soft try: a smaller balloon).
            val grown = 0.6f + 0.4f * p.s(0)
            Item(unit * 0.5f, { 0.42f }, { basketY() - 0.28f }) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val r = w * (0.08f + 0.26f * p.a(0) * grown)
                    val c = Offset(w / 2, h * 0.86f - r)
                    // Ropes to the basket.
                    drawLine(Color(0xFF6D4C41), Offset(c.x - r * 0.5f, c.y + r * 0.8f), Offset(w * 0.42f, h), 2.dp.toPx())
                    drawLine(Color(0xFF6D4C41), Offset(c.x + r * 0.5f, c.y + r * 0.8f), Offset(w * 0.58f, h), 2.dp.toPx())
                    paperCircle(Color(0xFFAB47BC), r, c)
                    paperOval(Color(0xFFFFD54F), Offset(c.x - r * 0.25f, c.y - r), Size(r * 0.5f, r * 2f))
                }
            }
            // Bear in the basket (holding the cupcake after "more").
            Item(unit * 0.2f, { 0.42f }, { basketY() }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Critter(Species.BEAR, Modifier.fillMaxWidth(0.75f).fillMaxHeight(0.8f), mood = if (p.beat == 6 && p.triggered(6)) Mood.SLEEP else Mood.HAPPY)
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.42f)) {
                        paperRect(Color(0xFFA1887F), Offset(size.width * 0.15f, 0f), Size(size.width * 0.7f, size.height))
                    }
                }
            }
            // The bird (or his own word) in the way: it moves aside.
            if (p.beat == 2) {
                Item(unit * 0.12f, { 0.42f + 0.32f * p.a(2) }, { 0.22f - 0.08f * bump(p.a(2)) * p.s(2) }) { Emoji(story.prop.emoji, unit * 0.12f) }
            }
            // A cupcake drops into Bear's paw.
            if (p.beat == 4) {
                Item(unit * 0.08f, { 0.47f }, { lerp(0.02f, basketY() - 0.06f, p.a(4)) }) { Emoji("🧁", unit * 0.08f) }
            }
            // Bubbles that pop one by one (a soft try pops a few).
            if (p.beat == 5) {
                val spots = listOf(0.2f to 0.3f, 0.32f to 0.18f, 0.62f to 0.24f, 0.74f to 0.4f, 0.86f to 0.2f)
                val pops = if (p.s(5) < 1f) 3 else spots.size
                spots.forEachIndexed { i, (x, y) ->
                    val popped = { i < pops && p.a(5) > (i + 1f) / (pops + 1f) }
                    Item(unit * 0.1f, { x }, { y }, alpha = { if (popped()) 0f else 1f }, scale = { if (popped()) 1.5f else 1f }) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Color(0xFFE1F5FE).copy(alpha = 0.5f), size.minDimension / 2)
                            drawCircle(Color.White, size.minDimension / 2, style = Stroke(3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

/** A quick burst of paper confetti from ([cx], [cy]). */
@Composable
private fun BoxWithConstraintsScope.Confetti(f: () -> Float, count: Int, cx: Float, cy: Float) {
    val colors = listOf(Color(0xFFFFD54F), Color(0xFF4FC3F7), Color(0xFFAB47BC), Color(0xFF81C784), Color(0xFFFFB74D))
    Canvas(Modifier.fillMaxSize()) {
        val t = f()
        if (t <= 0f || t >= 1f) return@Canvas
        for (k in 0 until count) {
            val a = k * 2f * PI.toFloat() / count
            val d = size.minDimension * 0.3f * t
            val c = Offset(size.width * cx + cos(a) * d, size.height * cy + sin(a) * d + size.height * 0.1f * t * t)
            paperRect(colors[k % colors.size], c, Size(size.minDimension * 0.02f, size.minDimension * 0.012f))
        }
    }
}
