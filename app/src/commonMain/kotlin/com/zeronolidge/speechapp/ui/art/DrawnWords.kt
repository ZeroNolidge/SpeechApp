package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * test49: words with no good emoji get a drawn paper picture instead. "oven" is the same red paper
 * oven he sees in Bruno's Birthday Cake (dark window, handle, knobs, dial; no flames), and "fan" is
 * a little electric fan with a round cage on a stand. Keyed on the word's text, so the 🔥 of "hot"
 * stays a flame.
 */
enum class DrawnWord { OVEN, FAN }

fun drawnWordFor(text: String): DrawnWord? = when (text.lowercase().trim().substringAfterLast(' ')) {
    "oven" -> DrawnWord.OVEN
    "fan" -> DrawnWord.FAN
    else -> null
}

/** A word's picture: the drawn one when it has one, else its emoji at [fontSize]. */
@Composable
fun WordGlyph(text: String, emoji: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val d = drawnWordFor(text)
    if (d == null) { Text(emoji, fontSize = fontSize, modifier = modifier); return }
    // An emoji glyph is about 1.2x its font size tall; match that so layouts don't jump.
    val side = with(LocalDensity.current) { (fontSize.toPx() * 1.15f).toDp() }
    Box(modifier.size(side), contentAlignment = Alignment.Center) { DrawnWordPic(d, Modifier.size(side)) }
}

@Composable
fun DrawnWordPic(d: DrawnWord, modifier: Modifier) {
    Canvas(modifier) {
        when (d) {
            DrawnWord.OVEN -> paperOvenPic()
            DrawnWord.FAN -> paperFanPic()
        }
    }
}

/** The Birthday Cake oven, cold: red box, control panel with two knobs and a dial, handle, dark window. */
fun DrawScope.paperOvenPic() {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun z(x: Float, y: Float) = Size(x * s, y * s)
    val line = maxOf(1.5f, s * 0.012f)
    // feet
    drawRect(Color(0xFF5D4037), o(0.16f, 0.88f), z(0.1f, 0.05f))
    drawRect(Color(0xFF5D4037), o(0.74f, 0.88f), z(0.1f, 0.05f))
    // body
    paperRoundRect(Color(0xFFD9534F), o(0.08f, 0.10f), z(0.84f, 0.79f), CornerRadius(s * 0.06f))
    drawRoundRect(Color(0xFF8E2B28), o(0.08f, 0.10f), z(0.84f, 0.79f), CornerRadius(s * 0.06f), style = Stroke(line * 1.3f))
    // control panel: two knobs and the timer dial
    paperRoundRect(Color(0xFFB83B37), o(0.13f, 0.15f), z(0.74f, 0.14f), CornerRadius(s * 0.03f))
    for (x in listOf(0.24f, 0.37f)) {
        drawCircle(Color(0xFFFFE0B2), s * 0.04f, o(x, 0.22f))
        drawCircle(Color(0xFF8E2B28), s * 0.04f, o(x, 0.22f), style = Stroke(line))
        drawLine(Color(0xFF8E2B28), o(x, 0.195f), o(x, 0.225f), line, cap = StrokeCap.Round)
    }
    val dc = o(0.74f, 0.22f)
    drawCircle(Color.White, s * 0.05f, dc)
    drawCircle(Color(0xFF8E2B28), s * 0.05f, dc, style = Stroke(line))
    drawLine(Color(0xFF8E2B28), dc, dc + Offset(0f, -s * 0.032f), line, cap = StrokeCap.Round)
    // door handle
    drawLine(Color(0xFF8E2B28), o(0.27f, 0.355f), o(0.73f, 0.355f), s * 0.045f, cap = StrokeCap.Round)
    drawLine(Color(0xFFEEEEEE), o(0.27f, 0.35f), o(0.73f, 0.35f), s * 0.03f, cap = StrokeCap.Round)
    // the window in the door, with the oven rack (no flames)
    paperRoundRect(Color(0xFF3B2A2A), o(0.18f, 0.42f), z(0.64f, 0.38f), CornerRadius(s * 0.04f))
    drawRoundRect(Color(0xFFFFFFFF).copy(alpha = 0.12f), o(0.22f, 0.45f), z(0.2f, 0.12f), CornerRadius(s * 0.03f))
    drawLine(Color(0xFF9E9E9E), o(0.22f, 0.68f), o(0.78f, 0.68f), line * 1.4f)
    drawRoundRect(Color(0xFF8E2B28), o(0.18f, 0.42f), z(0.64f, 0.38f), CornerRadius(s * 0.04f), style = Stroke(line * 1.3f))
}

/** A little electric fan: round wire cage with three blue blades, on a pole and a round foot. */
fun DrawScope.paperFanPic() {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val line = maxOf(1.5f, s * 0.012f)
    val ink = Color(0xFF455A64)
    // foot and pole
    paperOval(Color(0xFF78909C), o(0.24f, 0.84f), Size(s * 0.52f, s * 0.12f))
    drawOval(ink, o(0.24f, 0.84f), Size(s * 0.52f, s * 0.12f), style = Stroke(line))
    paperRoundRect(Color(0xFF90A4AE), o(0.455f, 0.6f), Size(s * 0.09f, s * 0.27f), CornerRadius(s * 0.03f))
    drawRoundRect(ink, o(0.455f, 0.6f), Size(s * 0.09f, s * 0.27f), CornerRadius(s * 0.03f), style = Stroke(line))
    // cage back
    val c = o(0.5f, 0.38f)
    val r = s * 0.33f
    paperCircle(Color(0xFFE3F2FD), r, c)
    // three blades
    for (k in 0 until 3) {
        rotate(k * 120f - 20f, c) {
            val p = Path().apply {
                moveTo(c.x, c.y)
                cubicTo(c.x + r * 0.15f, c.y - r * 0.35f, c.x + r * 0.55f, c.y - r * 0.95f, c.x + r * 0.05f, c.y - r * 0.9f)
                cubicTo(c.x - r * 0.35f, c.y - r * 0.85f, c.x - r * 0.25f, c.y - r * 0.3f, c.x, c.y)
                close()
            }
            paperPath(p, Color(0xFF42A5F5))
            drawPath(p, Color(0xFF1565C0), style = Stroke(line))
        }
    }
    // the wire cage over the blades: rim, spokes, an inner ring
    for (k in 0 until 12) {
        val a = k * PI.toFloat() / 6f
        drawLine(Color(0xFF90A4AE), c + Offset(cos(a) * r * 0.2f, sin(a) * r * 0.2f), c + Offset(cos(a) * r, sin(a) * r), line * 0.8f)
    }
    drawCircle(Color(0xFF90A4AE), r * 0.6f, c, style = Stroke(line * 0.8f))
    drawCircle(Color.White, r, c, style = Stroke(line * 3.2f))
    drawCircle(ink, r + line * 1.6f, c, style = Stroke(line))
    // hub
    drawCircle(Color(0xFF78909C), r * 0.18f, c)
    drawCircle(ink, r * 0.18f, c, style = Stroke(line))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.06f, c + Offset(-r * 0.05f, -r * 0.05f))
}
