package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.animation.core.Animatable
import com.zeronolidge.speechapp.logic.LipSync
import kotlinx.coroutines.delay
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Ears
import com.zeronolidge.speechapp.logic.Mouth
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.components.LocalApp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** LISTEN: a hand cupped by the ear, for when it's his turn to talk. */
enum class Mood { HAPPY, CHEER, THINK, SAD, SLEEP, LISTEN }

/** What a character is wearing. Any part can be null. */
data class Outfit(
    val hat: Cosmetic? = null,
    val cape: Cosmetic? = null,
    val glasses: Cosmetic? = null,
    val wings: Cosmetic? = null,
)

private val Ink = Color(0xFF3A2E5C)
private val Blush = Color(0xFFFF8A9A)
private val Brass = Color(0xFFD9A93A)
private val BrassDark = Color(0xFF9C6F1C)

/** Drawing area is 100 units wide and 140 tall; the critter's head centre sits at (50, 63). */
private const val W = 100f
private const val H = 140f
private const val OY = 15f

/**
 * A character as a paper puppet: body, head and two arms are separate cut-outs joined with
 * brass split pins (at the shoulders and the neck), each with a wobbly hand-cut edge. The pieces
 * are baked once; moods move the arms on their pins, and while [talking] (and something is being
 * said) the head and an arm give a small hinge wiggle. The hero wears a big paper [scarf]; the
 * robot (Rory) has a stitched antenna.
 */
@Composable
fun Critter(
    species: Species,
    modifier: Modifier = Modifier,
    color: Color = Color(species.defaultColor),
    outfit: Outfit = Outfit(),
    mood: Mood = Mood.HAPPY,
    mirrored: Boolean = false,
    animate: Boolean = true,
    scarf: Boolean = false,
    talking: Boolean = false,
    /** A real wave: the right hand comes up and goes side to side twice, then down. */
    waving: Boolean = false,
) {
    val still = LocalReduceMotion.current || !animate
    val t = rememberInfiniteTransition(label = "critter")
    val bob = t.loop(0f, 1f, if (mood == Mood.CHEER) 450 else 1400, easing = LinearEasing, label = "bob")
    val blink = t.animateFloat(
        0f, 0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                0f at 0
                0f at 3300
                1f at 3420
                0f at 3550
            },
        ),
        label = "blink",
    )
    val wiggle = t.loop(-1f, 1f, 170, label = "wiggle")
    val app = LocalApp.current
    val speaking = talking && !still && app.speaker.speaking
    val talk = animateFloatAsState(if (speaking) 1f else 0f, tween(150), label = "talk")
    // Rory's mouth follows the loudness of the clip playing right now (logic/LipSync.kt):
    // -1 = his usual baked smile (not talking), -3 = lips pressed (a p, b or m word starting),
    // else how open, 0 to 1. With the device's own voice (no clip) it chatters gently.
    val mouthOpen = remember { mutableFloatStateOf(-1f) }
    val liveMouth = species.boxy && talking && !still
    LaunchedEffect(liveMouth) {
        if (!liveMouth) {
            mouthOpen.floatValue = -1f
            return@LaunchedEffect
        }
        val lip = LipSync()
        var last = 0L
        while (true) {
            withFrameMillis { now ->
                val dt = if (last == 0L) 16 else (now - last).toInt().coerceIn(1, 100)
                last = now
                mouthOpen.floatValue = if (!app.speaker.speaking) -1f else {
                    val m = app.speaker.mouth()
                    if (m == null) 0.15f + 0.55f * (0.5f + 0.5f * sin(now / 1000.0 * 2 * PI * 3.5).toFloat())
                    else lip.step(m.level, m.text, dt).let { if (lip.closed) -3f else it }
                }
            }
        }
    }
    val waveAnim = remember { Animatable(0f) }
    LaunchedEffect(waving, still) {
        waveAnim.snapTo(0f)
        if (waving && !still) {
            delay(300)
            waveAnim.animateTo(1f, tween(1700, easing = LinearEasing))
            waveAnim.snapTo(0f)
        }
    }
    val waveState = waveAnim.asState()
    val motion = remember(bob, blink, wiggle, talk) { PuppetMotion(bob, blink, wiggle, talk, mouthOpen, waveState) }
    Spacer(
        modifier
            .aspectRatio(W / H)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
            .then(PuppetElement(PuppetSpec(species, color, outfit, mood, scarf, still), motion)),
    )
}

/** A hero with an optional pet sidekick sitting next to them. The hero wears the paper scarf. */
@Composable
fun HeroWithPet(
    species: Species,
    color: Color,
    outfit: Outfit,
    pet: Species?,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.HAPPY,
    talking: Boolean = false,
) {
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Critter(species, Modifier.fillMaxHeight(), color, outfit, mood, scarf = true, talking = talking)
        if (pet != null) {
            Critter(
                pet,
                Modifier
                    .fillMaxHeight(0.42f)
                    .align(Alignment.BottomEnd),
                mood = mood,
                mirrored = true,
            )
        }
    }
}

private data class PuppetSpec(
    val species: Species,
    val color: Color,
    val outfit: Outfit,
    val mood: Mood,
    val scarf: Boolean,
    val still: Boolean,
)

private class PuppetMotion(
    val bob: State<Float>,
    val blink: State<Float>,
    val wiggle: State<Float>,
    val talk: State<Float>,
    val mouth: State<Float>,
    val wave: State<Float>,
)

// HEAD_TALK(_BLINK): the head without its mouth, which is drawn live while Rory talks.
private enum class Part { BODY, HEAD, HEAD_BLINK, HEAD_TALK, HEAD_TALK_BLINK, FRONT, ARM_L, ARM_R }

private class PuppetElement(val spec: PuppetSpec, val motion: PuppetMotion) : ModifierNodeElement<PuppetNode>() {
    override fun create() = PuppetNode(spec, motion)

    override fun update(node: PuppetNode) {
        node.motion = motion
        if (node.spec != spec) {
            node.spec = spec
            node.parts.clear()
            node.invalidateDraw()
        }
    }

    override fun equals(other: Any?) = other is PuppetElement && other.spec == spec && other.motion === motion
    override fun hashCode() = spec.hashCode()
}

private class PuppetNode(var spec: PuppetSpec, var motion: PuppetMotion) : Modifier.Node(), DrawModifierNode {
    val parts = HashMap<Part, Baked>()
    private var bakedSize = Size.Zero

    private fun DrawScope.part(p: Part): Baked {
        if (bakedSize != size) {
            parts.clear()
            bakedSize = size
        }
        return parts.getOrPut(p) {
            val s = spec
            val u = size.width / W
            bakePaper(
                size.width, size.height, 6.dp.toPx(), 3.dp.toPx(), 1.2.dp.toPx(), 9.dp.toPx(),
                grain = true, maxPx = 520f, shapeShadows = true,
            ) { drawPuppetPart(p, u, s.species, s.color, s.outfit, s.mood, s.scarf) }
        }
    }

    override fun ContentDrawScope.draw() {
        if (size.minDimension < 2f) return
        val s = spec
        val m = motion
        val u = size.width / W
        val still = s.still
        val bob = if (still) 0f else m.bob.value
        val lift = if (s.mood == Mood.CHEER) -10f * bob else -2.5f * bob
        val wig = if (still) 0f else m.wiggle.value * m.talk.value
        val blinking = !still && s.mood != Mood.SLEEP && m.blink.value > 0.5f
        fun o(x: Float, y: Float) = Offset(x * u, (y + OY) * u)
        val boxy = s.species.boxy

        // The ground shadow stays put while the puppet bobs.
        drawOval(Color(0x22000000), Offset(25 * u, (H - 8) * u), Size(50 * u, 7 * u))
        translate(0f, lift * u) {
            drawBaked(part(Part.BODY))
            val tilt = when (s.mood) {
                Mood.THINK -> -8f
                Mood.SAD -> 5f
                Mood.SLEEP -> 10f
                Mood.LISTEN -> 6f
                else -> 0f
            }
            val mouth = if (boxy) m.mouth.value else -1f
            rotate(tilt + wig * 4f, o(50f, if (boxy) 86f else 90f)) {
                if (mouth == -1f) {
                    drawBaked(part(if (blinking) Part.HEAD_BLINK else Part.HEAD))
                } else {
                    drawBaked(part(if (blinking) Part.HEAD_TALK_BLINK else Part.HEAD_TALK))
                    robotMouth(o(50f, 65f), u, mouth)
                }
            }
            drawBaked(part(Part.FRONT))
            val (l, r) = armAngles(s.mood)
            val wave = if (s.mood == Mood.CHEER) (bob - 0.5f) * 16f else 0f
            // A real wave: up, side to side twice, down.
            val w = if (still) 0f else m.wave.value
            val up = if (w <= 0f || w >= 1f) 0f else minOf(w / 0.15f, (1f - w) / 0.12f, 1f)
            val swing = if (w in 0.15f..0.85f) sin((w - 0.15f) / 0.7f * 2f * PI.toFloat() * 2f) * 26f else 0f
            val rWave = (r - wave + wig * 14f) * (1f - up) + (-150f + swing) * up
            rotate(l + wave - wig * 4f, shoulder(u, boxy, false)) { drawBaked(part(Part.ARM_L)) }
            rotate(rWave, shoulder(u, boxy, true)) { drawBaked(part(Part.ARM_R)) }
        }
        drawContent()
    }

    override fun onDetach() {
        parts.clear()
    }
}

/** How each mood holds the arms on their pins (degrees; arms hang straight down at 0). */
private fun armAngles(mood: Mood): Pair<Float, Float> = when (mood) {
    Mood.CHEER -> 150f to -150f
    Mood.THINK -> 12f to -120f
    Mood.SAD, Mood.SLEEP -> 4f to -4f
    // The right hand comes up beside the head, cupped by the ear.
    Mood.LISTEN -> 14f to -158f
    Mood.HAPPY -> 14f to -14f
}

private fun shoulderUnits(boxy: Boolean, right: Boolean) = Offset(if (right) (if (boxy) 72f else 70f) else (if (boxy) 28f else 30f), if (boxy) 80f else 82f)

private fun shoulder(u: Float, boxy: Boolean, right: Boolean): Offset {
    val p = shoulderUnits(boxy, right)
    return Offset(p.x * u, (p.y + OY) * u)
}

private fun darker(c: Color, f: Float = 0.25f) = lerp(c, Color.Black, f)
private fun lighter(c: Color, f: Float = 0.5f) = lerp(c, Color.White, f)

/** A brass split pin, the joint of a paper puppet. */
private fun DrawScope.splitPin(c: Offset, r: Float) {
    paperCircle(BrassDark, r, c)
    paperCircle(Brass, r * 0.78f, Offset(c.x - r * 0.08f, c.y - r * 0.08f))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.22f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
}

private fun DrawScope.drawPuppetPart(
    part: Part,
    u: Float,
    sp: Species,
    color: Color,
    outfit: Outfit,
    mood: Mood,
    scarf: Boolean,
) {
    val lift = 0f
    fun o(x: Float, y: Float) = Offset(x * u, (y + OY + lift) * u)
    fun oval(cx: Float, cy: Float, w: Float, h: Float, c: Color) =
        paperOval(c, o(cx - w / 2, cy - h / 2), Size(w * u, h * u))
    fun circle(cx: Float, cy: Float, r: Float, c: Color) = paperCircle(c, r * u, o(cx, cy))
    fun poly(c: Color, vararg pts: Pair<Float, Float>) {
        val p = Path()
        pts.forEachIndexed { i, (x, y) -> val q = o(x, y); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
        p.close()
        paperPath(p, c)
    }

    val kid = sp == Species.KID
    val skin = if (kid) Color(sp.defaultColor) else color
    val body = color
    val belly = if (kid) lighter(color, 0.35f) else Color(sp.bellyColor)
    val dark = darker(body)
    val blink = if (part == Part.HEAD_BLINK || part == Part.HEAD_TALK_BLINK) 1f else 0f
    val noMouth = part == Part.HEAD_TALK || part == Part.HEAD_TALK_BLINK

    when (part) {
        Part.BODY -> {
            // ---- Behind the body ----
            outfit.wings?.let { wings ->
                val c = Color(wings.color)
                val flutter = 0f
                when (wings.id) {
                    "wings_fairy", "wings_butterfly" -> listOf(-1f, 1f).forEach { side ->
                        fun x(v: Float) = 50f + side * v
                        // Two rounded paper lobes each side, like a butterfly.
                        val top = Path().apply {
                            val a = o(x(14f), 84f); moveTo(a.x, a.y)
                            val c1 = o(x(30f), 56f + flutter); val c2 = o(x(52f), 62f + flutter); val e = o(x(44f), 86f)
                            cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
                            close()
                        }
                        paperPath(top, c)
                        val bottom = Path().apply {
                            val a = o(x(14f), 90f); moveTo(a.x, a.y)
                            val c1 = o(x(40f), 92f); val c2 = o(x(42f), 112f); val e = o(x(24f), 108f)
                            cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
                            close()
                        }
                        paperPath(bottom, if (wings.id == "wings_butterfly") Color(0xFFFFB347) else lighter(c, 0.3f))
                        circle(x(34f), 74f + flutter * 0.5f, 3.5f, Color.White.copy(alpha = 0.95f))
                    }
                    else -> listOf(-1f, 1f).forEach { side ->
                        fun x(v: Float) = 50f + side * v
                        // Feathered wings: three overlapping paper feathers each side.
                        listOf(0f, 7f, 14f).forEachIndexed { i, dy ->
                            val f = Path().apply {
                                val a = o(x(16f), 80f + dy * 0.4f); moveTo(a.x, a.y)
                                val c1 = o(x(36f), 60f + dy + flutter); val e = o(x(48f - i * 4f), 70f + dy * 1.4f + flutter)
                                quadraticTo(c1.x, c1.y, e.x, e.y)
                                val c2 = o(x(34f), 86f + dy)
                                quadraticTo(c2.x, c2.y, a.x, a.y)
                                close()
                            }
                            paperPath(f, if (i == 1) darker(c, 0.08f) else c)
                        }
                    }
                }
            }
            outfit.cape?.let { cape ->
                val c = Color(cape.color)
                poly(c, 34f to 74f, 66f to 74f, 80f to 122f, 20f to 122f)
                if (cape.id == "cape_rainbow") {
                    listOf(0xFFFF6F91, 0xFFFFD740, 0xFF69F0AE, 0xFF40C4FF).forEachIndexed { i, col ->
                        val y = 88f + i * 8f
                        poly(Color(col), (30f - i * 2.5f) to y, (70f + i * 2.5f) to y, (71f + i * 2.5f) to y + 4f, (29f - i * 2.5f) to y + 4f)
                    }
                }
                if (cape.id == "cape_purple") listOf(30f to 108f, 66f to 112f, 48f to 116f).forEach { (x, y) -> star(o(x, y), 4f * u, Color(0xFFFFEB3B)) }
            }
            if (sp.tail) {
                when (sp) {
                    Species.BUNNY -> circle(74f, 108f, 7f, Color.White)
                    Species.DINO -> poly(body, 66f to 98f, 92f to 116f, 68f to 114f)
                    Species.UNICORN -> {
                        oval(78f, 104f, 12f, 24f, Color(0xFFF48FB1))
                        oval(82f, 108f, 9f, 18f, Color(0xFFB39DDB))
                    }
                    Species.MOUSE -> {
                        val p = Path().apply {
                            val a = o(70f, 110f); moveTo(a.x, a.y)
                            val b = o(92f, 96f); val c = o(88f, 120f); quadraticTo(b.x, b.y, c.x, c.y)
                        }
                        drawPath(p, Color(0xFFFF8A9A), style = Stroke(3f * u, cap = StrokeCap.Round))
                    }
                    Species.FOX -> {
                        oval(80f, 104f, 16f, 28f, body)
                        oval(83f, 116f, 9f, 10f, Color.White)
                    }
                    else -> oval(76f, 104f, 12f, 18f, dark)
                }
            }


            // ---- Body, feet and arms ----
            if (sp.boxy) {
                paperRoundRect(body, o(28f, 76f), Size(44f * u, 40f * u), CornerRadius(8f * u))
                paperRoundRect(belly, o(36f, 84f), Size(28f * u, 22f * u), CornerRadius(4f * u))
                circle(44f, 92f, 3f, Color(0xFF22A699)); circle(56f, 92f, 3f, Color(0xFFFFEB3B))
                paperRoundRect(dark, o(42f, 99f), Size(16f * u, 3f * u), CornerRadius(2f * u))
            } else {
                oval(50f, 97f, 46f, 44f, body)
                oval(50f, 100f, 28f, 28f, belly)
                if (sp == Species.PENGUIN) oval(50f, 100f, 32f, 34f, belly)
            }
            val foot = if (sp == Species.PENGUIN || sp == Species.CHICK || sp == Species.OWL) Color(0xFFFFA726) else darker(body, 0.15f)
            oval(39f, 119f, 17f, 9f, foot); oval(61f, 119f, 17f, 9f, foot)

        }
        Part.HEAD, Part.HEAD_BLINK, Part.HEAD_TALK, Part.HEAD_TALK_BLINK -> {
            // ---- Ears behind the head ----
            when (sp.ears) {
                Ears.ROUND -> {
                    circle(29f, 39f, 10f, body); circle(71f, 39f, 10f, body)
                    circle(29f, 39f, 5f, belly); circle(71f, 39f, 5f, belly)
                }
                Ears.BIG_ROUND -> {
                    circle(24f, 40f, 15f, body); circle(76f, 40f, 15f, body)
                    circle(24f, 40f, 9f, Color(0xFFFFC1CC)); circle(76f, 40f, 9f, Color(0xFFFFC1CC))
                }
                Ears.LONG -> {
                    oval(38f, 22f, 13f, 38f, body); oval(62f, 22f, 13f, 38f, body)
                    oval(38f, 23f, 6f, 28f, Color(0xFFFFC1CC)); oval(62f, 23f, 6f, 28f, Color(0xFFFFC1CC))
                }
                Ears.POINTY -> {
                    if (sp == Species.OWL) {
                        poly(dark, 26f to 46f, 28f to 30f, 40f to 40f); poly(dark, 74f to 46f, 72f to 30f, 60f to 40f)
                    } else {
                        poly(body, 24f to 50f, 28f to 24f, 46f to 38f); poly(body, 76f to 50f, 72f to 24f, 54f to 38f)
                        poly(Color(0xFFFFC1CC), 29f to 44f, 31f to 31f, 40f to 39f); poly(Color(0xFFFFC1CC), 71f to 44f, 69f to 31f, 60f to 39f)
                    }
                }
                else -> {}
            }
            if (sp == Species.UNICORN) {
                // A colourful mane down the back of the head.
                listOf(0xFFF48FB1, 0xFFCE93D8, 0xFF90CAF9, 0xFFA5D6A7).forEachIndexed { i, col ->
                    circle(70f + (i % 2) * 4f, 40f + i * 9f, 9f, Color(col))
                }
            }


            // ---- Head ----
            if (sp.boxy) {
                paperRoundRect(body, o(22f, 34f), Size(56f * u, 54f * u), CornerRadius(14f * u))
                paperRoundRect(Color(0xFF263238), o(28f, 42f), Size(44f * u, 36f * u), CornerRadius(9f * u))
            } else {
                circle(50f, 63f, 30f, skin)
            }
            if (sp.antenna) {
                // Rory's signature: a stitched paper antenna with a gold star tip.
                paperRoundRect(darker(body, 0.12f), o(47.5f, 12f), Size(5f * u, 24f * u), CornerRadius(2f * u))
                drawLine(
                    Color.White.copy(alpha = 0.9f), o(50f, 14f), o(50f, 34f), 1f * u,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.2f * u, 1.6f * u)),
                )
                listOf(18f, 25f, 32f).forEach { y -> drawLine(Color.White.copy(alpha = 0.9f), o(48f, y - 1.2f), o(52f, y + 1.2f), 0.9f * u) }
                circle(50f, 11f, 5.5f, Color(0xFFF2B630))
                star(o(50f, 11f), 3.6f * u, Color(0xFFFFF3C4))
            }
            if (sp.spikes) {
                poly(dark, 38f to 36f, 43f to 24f, 48f to 34f)
                poly(dark, 46f to 34f, 52f to 20f, 58f to 34f)
                poly(dark, 56f to 35f, 62f to 25f, 65f to 39f)
            }
            if (sp.horn) {
                poly(Color(0xFFFFD54F), 45f to 37f, 50f to 12f, 55f to 37f)
                drawLine(Color(0xFFFFB300), o(46.5f, 30f), o(53.5f, 27f), 1.5f * u)
                drawLine(Color(0xFFFFB300), o(47.5f, 23f), o(52.5f, 21f), 1.5f * u)
            }
            if (sp.hair) {
                val hair = Color(0xFF6D4C41)
                paperArc(hair, 180f, 180f, true, o(20f, 33f), Size(60f * u, 50f * u))
                circle(37f, 44f, 9f, hair); circle(50f, 41f, 9f, hair); circle(63f, 44f, 9f, hair)
            }
            if (sp.ears == Ears.FLOPPY) {
                rotate(15f, o(22f, 60f)) { oval(22f, 62f, 15f, 32f, dark) }
                rotate(-15f, o(78f, 60f)) { oval(78f, 62f, 15f, 32f, dark) }
            }
            if (sp == Species.OWL) {
                circle(39f, 60f, 12f, belly); circle(61f, 60f, 12f, belly)
            }
            if (sp == Species.FOX) {
                poly(Color.White, 24f to 66f, 50f to 70f, 50f to 90f, 34f to 84f)
                poly(Color.White, 76f to 66f, 50f to 70f, 50f to 90f, 66f to 84f)
            }

            // ---- Face ----
            val eyeY = if (sp.frogEyes) 42f else 60f
            if (sp.frogEyes) {
                circle(37f, 42f, 12f, body); circle(63f, 42f, 12f, body)
            }
            if (sp.boxy) {
                // Robot screen face.
                val glow = Color(0xFF4DD0E1)
                if (blink > 0.5f) {
                    drawLine(glow, o(34f, 56f), o(44f, 56f), 3f * u); drawLine(glow, o(56f, 56f), o(66f, 56f), 3f * u)
                } else {
                    circle(39f, 56f, 6f, glow); circle(61f, 56f, 6f, glow)
                    circle(41f, 54f, 2f, Color.White); circle(63f, 54f, 2f, Color.White)
                }
                if (!noMouth) mouth(o(50f, 65f), 8f * u, glow, mood)
            } else {
                val eyeH = 15f * (1f - blink * 0.9f)
                if (mood == Mood.SLEEP) {
                    // Closed, sleepy eyes and a little "z z".
                    listOf(39f, 61f).forEach { ex ->
                        val lid = Path().apply {
                            val a = o(ex - 6f, eyeY); moveTo(a.x, a.y)
                            val c = o(ex, eyeY + 5f); val b = o(ex + 6f, eyeY)
                            quadraticTo(c.x, c.y, b.x, b.y)
                        }
                        drawPath(lid, Ink, style = Stroke(2.2f * u, cap = StrokeCap.Round))
                    }
                    listOf(Triple(76f, 40f, 5f), Triple(84f, 28f, 7f)).forEach { (zx, zy, zs) ->
                        val z = Path().apply {
                            val a = o(zx, zy); moveTo(a.x, a.y)
                            val b = o(zx + zs, zy); lineTo(b.x, b.y)
                            val c = o(zx, zy + zs); lineTo(c.x, c.y)
                            val d = o(zx + zs, zy + zs); lineTo(d.x, d.y)
                        }
                        drawPath(z, Ink.copy(alpha = 0.7f), style = Stroke(1.6f * u, cap = StrokeCap.Round))
                    }
                } else {
                    oval(39f, eyeY, 13f, eyeH, Color.White); oval(61f, eyeY, 13f, eyeH, Color.White)
                }
                if (blink < 0.5f && mood != Mood.SLEEP) {
                    val look = if (mood == Mood.THINK) -2f else 1f
                    val ly = if (mood == Mood.THINK) -2f else 1f
                    circle(39f + look, eyeY + ly, 4.6f, Ink); circle(61f + look, eyeY + ly, 4.6f, Ink)
                    circle(40.5f + look, eyeY + ly - 2f, 1.7f, Color.White); circle(62.5f + look, eyeY + ly - 2f, 1.7f, Color.White)
                }
                if (mood == Mood.SAD) {
                    // A little tear.
                    val tear = Path().apply {
                        val top = o(33f, eyeY + 6f)
                        moveTo(top.x, top.y)
                        val l = o(30.5f, eyeY + 12f)
                        val r = o(35.5f, eyeY + 12f)
                        lineTo(r.x, r.y)
                        lineTo(l.x, l.y)
                        close()
                    }
                    paperPath(tear, Color(0xFF64B5F6))
                    paperCircle(Color(0xFF64B5F6), 2.6f * u, o(33f, eyeY + 12.5f))
                }
                // Cheeks.
                paperCircle(Blush.copy(alpha = 0.55f), 5f * u, o(30f, 72f))
                paperCircle(Blush.copy(alpha = 0.55f), 5f * u, o(70f, 72f))
                when (sp.mouth) {
                    Mouth.SMILE -> mouth(o(50f, 73f), 7f * u, Ink, mood)
                    Mouth.SNOUT -> {
                        oval(50f, 76f, 26f, 18f, belly)
                        oval(50f, 71f, 10f, 7f, Ink)
                        mouth(o(50f, 78f), 5f * u, Ink, mood)
                    }
                    Mouth.BEAK -> {
                        // One beak, the same in every mood.
                        poly(Color(0xFFFFA726), 43f to 70f, 57f to 70f, 50f to 80f)
                    }
                }
                if (sp.whiskers) {
                    listOf(-3f, 2f).forEach { dy ->
                        drawLine(Ink.copy(alpha = 0.5f), o(32f, 74f + dy), o(18f, 72f + dy * 2), 1.2f * u)
                        drawLine(Ink.copy(alpha = 0.5f), o(68f, 74f + dy), o(82f, 72f + dy * 2), 1.2f * u)
                    }
                }
            }

            // ---- Glasses ----
            outfit.glasses?.let { g ->
                val c = Color(g.color)
                val ly = if (sp.boxy) 56f else eyeY
                when (g.id) {
                    "glasses_sun" -> {
                        paperRoundRect(c, o(29f, ly - 7f), Size(19f * u, 13f * u), CornerRadius(5f * u))
                        paperRoundRect(c, o(52f, ly - 7f), Size(19f * u, 13f * u), CornerRadius(5f * u))
                        drawLine(c, o(47f, ly - 3f), o(53f, ly - 3f), 2.5f * u)
                        drawLine(Color.White.copy(alpha = 0.6f), o(32f, ly - 4f), o(37f, ly - 4f), 1.5f * u)
                    }
                    "glasses_star" -> {
                        star(o(39f, ly), 11f * u, c.copy(alpha = 0.85f)); star(o(61f, ly), 11f * u, c.copy(alpha = 0.85f))
                        drawLine(c, o(47f, ly), o(53f, ly), 2f * u)
                    }
                    "glasses_heart" -> {
                        heart(o(39f, ly), 10f * u, c.copy(alpha = 0.8f)); heart(o(61f, ly), 10f * u, c.copy(alpha = 0.8f))
                        drawLine(c, o(47f, ly - 2f), o(53f, ly - 2f), 2f * u)
                    }
                    else -> {
                        drawCircle(c, 9.5f * u, o(39f, ly), style = Stroke(2.2f * u))
                        drawCircle(c, 9.5f * u, o(61f, ly), style = Stroke(2.2f * u))
                        drawLine(c, o(48f, ly - 1f), o(52f, ly - 1f), 2.2f * u)
                    }
                }
            }

            // ---- Hat ----
            outfit.hat?.let { hat ->
                val c = Color(hat.color)
                val top = if (sp.boxy) 34f else 36f
                when (hat.id) {
                    "hat_cap" -> {
                        paperArc(c, 180f, 180f, true, o(30f, top - 4f), Size(40f * u, 26f * u))
                        oval(64f, top + 9f, 26f, 6f, darker(c))
                        circle(50f, top - 4f, 2.5f, darker(c))
                    }
                    "hat_party" -> {
                        poly(c, 38f to top + 4f, 50f to top - 26f, 62f to top + 4f)
                        poly(Color(0xFFFFEB3B), 41f to top - 4f, 59f to top - 4f, 57f to top - 9f, 43f to top - 9f)
                        circle(50f, top - 27f, 4f, Color(0xFFFF4081))
                    }
                    "hat_wizard" -> {
                        poly(c, 32f to top + 4f, 54f to top - 32f, 68f to top + 4f)
                        oval(50f, top + 4f, 50f, 9f, darker(c))
                        star(o(50f, top - 8f), 5f * u, Color(0xFFFFEB3B))
                        star(o(57f, top - 20f), 3f * u, Color(0xFFFFEB3B))
                    }
                    "hat_crown" -> {
                        poly(c, 34f to top + 4f, 34f to top - 14f, 42f to top - 5f, 50f to top - 18f, 58f to top - 5f, 66f to top - 14f, 66f to top + 4f)
                        circle(50f, top - 3f, 3f, Color(0xFFE84A7F)); circle(41f, top - 1f, 2.2f, Color(0xFF1E88E5)); circle(59f, top - 1f, 2.2f, Color(0xFF43A047))
                    }
                    "hat_pirate" -> {
                        paperArc(c, 180f, 180f, true, o(24f, top - 16f), Size(52f * u, 36f * u))
                        paperRoundRect(c, o(22f, top), Size(56f * u, 5f * u), CornerRadius(2f * u))
                        circle(50f, top - 7f, 4f, Color.White)
                        circle(48.5f, top - 7.5f, 1f, c); circle(51.5f, top - 7.5f, 1f, c)
                    }
                    "hat_flower" -> {
                        listOf(30f, 40f, 50f, 60f, 70f).forEachIndexed { i, x ->
                            val y = top + 2f - if (i in 1..3) 4f else 0f
                            val petal = listOf(c, Color(0xFFFFEB3B), Color(0xFFB388FF), Color(0xFF80D8FF), c)[i]
                            (0 until 5).forEach { k ->
                                val a = k * 2 * PI / 5
                                circle(x + 3.5f * cos(a).toFloat(), y + 3.5f * sin(a).toFloat(), 3.2f, petal)
                            }
                            circle(x, y, 2.4f, Color(0xFFFFA000))
                        }
                    }
                    "hat_top" -> {
                        paperRoundRect(c, o(37f, top - 24f), Size(26f * u, 26f * u), CornerRadius(2f * u))
                        paperRoundRect(Color(0xFF7D4C96), o(37f, top - 6f), Size(26f * u, 5f * u))
                        oval(50f, top + 2f, 44f, 7f, c)
                    }
                    "hat_chef" -> {
                        // Rory's Kitchen: a toque, a white band below a puffy top made of three
                        // overlapping rounded puffs (Ruby, K2d: one plain circle read as a big
                        // white ball), wider than tall, thin grey outline all round.
                        val grey = Color(0xFFBDBDBD)
                        val bandTL = o(32f, top + 2f)
                        val bandSize = Size(36f * u, 11f * u)
                        paperRoundRect(c, bandTL, bandSize, CornerRadius(4f * u))
                        drawRoundRect(grey, bandTL, bandSize, CornerRadius(4f * u, 4f * u), style = Stroke(1.2f * u))
                        listOf(Triple(37f, top - 6f, 14f), Triple(50f, top - 13f, 15f), Triple(63f, top - 6f, 14f)).forEach { (x, y, r) ->
                            circle(x, y, r, c)
                            drawCircle(grey, r * u, o(x, y), style = Stroke(1.2f * u))
                        }
                    }
                }
            }

        }
        Part.FRONT -> {
            if (scarf) {
                // The hero's signature: a big paper scarf, wrapped at the neck with a long tail.
                val wool = Color(0xFF1FA39A)
                val stripe = Color(0xFFF2B630)
                val neckY = if (sp.boxy) 82f else 88f
                paperRoundRect(wool, o(29f, neckY - 4f), Size(42f * u, 11f * u), CornerRadius(5f * u))
                paperRoundRect(stripe, o(31f, neckY), Size(38f * u, 2.6f * u), CornerRadius(1.3f * u))
                poly(wool, 55f to neckY + 3f, 66f to neckY + 3f, 71f to neckY + 30f, 59f to neckY + 32f)
                poly(stripe, 57.5f to neckY + 13f, 67.5f to neckY + 12f, 68.5f to neckY + 16f, 58.5f to neckY + 17f)
                listOf(60f, 63.5f, 67f).forEach { x ->
                    drawLine(darker(wool, 0.2f), o(x, neckY + 31.5f), o(x + 0.6f, neckY + 35f), 1.3f * u, cap = StrokeCap.Round)
                }
            }
            splitPin(o(50f, if (sp.boxy) 86f else 90f), 3.2f * u)
        }
        Part.ARM_L, Part.ARM_R -> {
            val right = part == Part.ARM_R
            val pin = shoulderUnits(sp.boxy, right)
            val armColor = body
            val hand = if (kid) skin else darker(body, 0.1f)
            if (sp.boxy) {
                paperRoundRect(armColor, o(pin.x - 4.5f, pin.y - 3f), Size(9f * u, 24f * u), CornerRadius(3f * u))
                circle(pin.x, pin.y + 22f, 5f, darker(body, 0.15f))
            } else {
                oval(pin.x, pin.y + 9f, 11f, 24f, armColor)
                circle(pin.x, pin.y + 20f, 5.5f, hand)
            }
            splitPin(o(pin.x, pin.y), 2.6f * u)
        }
    }
}

/**
 * Rory's live mouth on his screen face: [open] 0 to 1 (an oval that opens with the sound), or -3
 * for lips pressed together (the start of a p, b or m word).
 */
private fun DrawScope.robotMouth(center: Offset, u: Float, open: Float) {
    val glow = Color(0xFF4DD0E1)
    if (open == -3f || open < 0.06f) {
        val half = if (open == -3f) 6.5f * u else 5.5f * u
        drawLine(glow, Offset(center.x - half, center.y), Offset(center.x + half, center.y), (if (open == -3f) 3.4f else 2.6f) * u, cap = StrokeCap.Round)
        return
    }
    val w = (11f - 2f * open) * u
    val h = (2.5f + 9.5f * open) * u
    drawOval(Color(0xFF0E2A3A), Offset(center.x - w / 2, center.y - h / 2), Size(w, h))
    drawOval(glow, Offset(center.x - w / 2, center.y - h / 2), Size(w, h), style = Stroke(1.8f * u))
}

private fun DrawScope.mouth(center: Offset, r: Float, color: Color, mood: Mood) {
    if (mood == Mood.SAD) {
        // A frown: the top of a circle sitting below the mouth position.
        drawArc(
            color, 200f, 140f, false,
            Offset(center.x - r, center.y + r * 0.2f), Size(2 * r, 2 * r),
            style = Stroke(r * 0.32f, cap = StrokeCap.Round),
        )
    } else {
        smile(center, r, color, mood == Mood.CHEER)
    }
}

private fun DrawScope.smile(center: Offset, r: Float, color: Color, open: Boolean) {
    if (open) {
        paperArc(Color(0xFFC62828), 0f, 180f, true, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r))
        paperArc(Color(0xFFFF8A80), 20f, 140f, true, Offset(center.x - r * 0.6f, center.y), Size(1.2f * r, r))
    } else {
        drawArc(color, 20f, 140f, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(r * 0.32f, cap = StrokeCap.Round))
    }
}

fun DrawScope.star(center: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = center.x + rr * cos(a).toFloat()
        val y = center.y + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    paperPath(p, color)
}

private fun DrawScope.heart(center: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y + r * 0.8f)
        cubicTo(center.x - r * 1.4f, center.y - r * 0.2f, center.x - r * 0.6f, center.y - r * 1.2f, center.x, center.y - r * 0.4f)
        cubicTo(center.x + r * 0.6f, center.y - r * 1.2f, center.x + r * 1.4f, center.y - r * 0.2f, center.x, center.y + r * 0.8f)
        close()
    }
    paperPath(p, color)
}
