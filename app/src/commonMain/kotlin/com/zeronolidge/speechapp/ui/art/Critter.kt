package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Ears
import com.zeronolidge.speechapp.logic.Mouth
import com.zeronolidge.speechapp.logic.Species
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Mood { HAPPY, CHEER, THINK, SAD }

/** What a character is wearing. Any part can be null. */
data class Outfit(
    val hat: Cosmetic? = null,
    val cape: Cosmetic? = null,
    val glasses: Cosmetic? = null,
)

private val Ink = Color(0xFF3A2E5C)
private val Blush = Color(0xFFFF8A9A)

/** Drawing area is 100 units wide and 140 tall; the critter's head centre sits at (50, 63). */
private const val W = 100f
private const val H = 140f
private const val OY = 15f

/**
 * A cartoon character drawn from simple shapes. All heroes, friends and pets use this.
 * It gently bobs and blinks so it feels alive.
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
) {
    val t = rememberInfiniteTransition(label = "critter")
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (mood == Mood.CHEER) 450 else 1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val blink by t.animateFloat(
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
    val lift = if (!animate) 0f else if (mood == Mood.CHEER) -10f * bob else -2.5f * bob
    Canvas(
        modifier
            .aspectRatio(W / H)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f },
    ) {
        val u = size.width / W
        drawCritter(u, species, color, outfit, mood, lift, if (animate) blink else 0f)
    }
}

/** A hero with an optional pet sidekick sitting next to them. */
@Composable
fun HeroWithPet(
    species: Species,
    color: Color,
    outfit: Outfit,
    pet: Species?,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.HAPPY,
) {
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Critter(species, Modifier.fillMaxHeight(), color, outfit, mood)
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

private fun darker(c: Color, f: Float = 0.25f) = lerp(c, Color.Black, f)
private fun lighter(c: Color, f: Float = 0.5f) = lerp(c, Color.White, f)

private fun DrawScope.drawCritter(
    u: Float,
    sp: Species,
    color: Color,
    outfit: Outfit,
    mood: Mood,
    lift: Float,
    blink: Float,
) {
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

    // Ground shadow does not bob.
    paperOval(Color(0x22000000), Offset(25 * u, (H - 8) * u), Size(50 * u, 7 * u))

    // ---- Behind the body ----
    outfit.cape?.let { cape ->
        val c = Color(cape.color)
        poly(c, 34f to 74f, 66f to 74f, 80f to 122f, 20f to 122f)
        if (cape.id == "cape_rainbow") {
            listOf(0xFFFF5252, 0xFFFFD740, 0xFF69F0AE, 0xFF40C4FF).forEachIndexed { i, col ->
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

    // ---- Body, feet and arms ----
    if (sp.boxy) {
        paperRoundRect(body, o(28f, 76f), Size(44f * u, 40f * u), CornerRadius(8f * u))
        paperRoundRect(belly, o(36f, 84f), Size(28f * u, 22f * u), CornerRadius(4f * u))
        circle(44f, 92f, 3f, Color(0xFFFF5252)); circle(56f, 92f, 3f, Color(0xFFFFEB3B))
        paperRoundRect(dark, o(42f, 99f), Size(16f * u, 3f * u), CornerRadius(2f * u))
    } else {
        oval(50f, 97f, 46f, 44f, body)
        oval(50f, 100f, 28f, 28f, belly)
        if (sp == Species.PENGUIN) oval(50f, 100f, 32f, 34f, belly)
    }
    val foot = if (sp == Species.PENGUIN || sp == Species.CHICK || sp == Species.OWL) Color(0xFFFFA726) else darker(body, 0.15f)
    oval(39f, 119f, 17f, 9f, foot); oval(61f, 119f, 17f, 9f, foot)
    val armColor = if (kid) skin else body
    if (mood == Mood.CHEER) {
        oval(24f, 80f, 10f, 20f, armColor); oval(76f, 80f, 10f, 20f, armColor)
    } else if (mood == Mood.THINK) {
        oval(25f, 96f, 10f, 18f, armColor)
        oval(62f, 80f, 10f, 18f, armColor)
    } else {
        oval(25f, 96f, 10f, 18f, armColor); oval(75f, 96f, 10f, 18f, armColor)
    }

    // ---- Head ----
    if (sp.boxy) {
        paperRoundRect(body, o(22f, 34f), Size(56f * u, 54f * u), CornerRadius(14f * u))
        paperRoundRect(Color(0xFF263238), o(28f, 42f), Size(44f * u, 36f * u), CornerRadius(9f * u))
    } else {
        circle(50f, 63f, 30f, skin)
    }
    if (sp.antenna) {
        drawLine(dark, o(50f, 34f), o(50f, 22f), 3f * u)
        circle(50f, 20f, 5f, Color(0xFFFF5252))
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
        mouth(o(50f, 65f), 8f * u, glow, mood)
    } else {
        val eyeH = 15f * (1f - blink * 0.9f)
        oval(39f, eyeY, 13f, eyeH, Color.White); oval(61f, eyeY, 13f, eyeH, Color.White)
        if (blink < 0.5f) {
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
                poly(Color(0xFFFFA726), 43f to 70f, 57f to 70f, 50f to 80f)
                if (mood == Mood.CHEER) poly(Color(0xFFFB8C00), 45f to 76f, 55f to 76f, 50f to 82f)
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
                circle(50f, top - 3f, 3f, Color(0xFFE53935)); circle(41f, top - 1f, 2.2f, Color(0xFF1E88E5)); circle(59f, top - 1f, 2.2f, Color(0xFF43A047))
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
                paperRoundRect(Color(0xFFE53935), o(37f, top - 6f), Size(26f * u, 5f * u))
                oval(50f, top + 2f, 44f, 7f, c)
            }
        }
    }
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
