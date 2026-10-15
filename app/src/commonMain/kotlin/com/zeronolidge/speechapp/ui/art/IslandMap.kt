package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.ui.components.ArrowIcon
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.unfold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The world map as an illustrated paper island: a forest, a meadow, a lagoon, a market town,
 * mountains with a rope bridge, a factory, caves, and a castle on a floating cloud. A paper
 * trail winds between them. Finished stops wear a star sticker; the next stop is a big raised
 * paper button. Places the trail hasn't reached yet hide under drifting mist. The scenery is
 * muted so the one thing to tap stands out.
 */
@Composable
fun IslandMap(
    chapter: Int,
    reached: (Land) -> Boolean,
    /** Play the unfold animation on the current stop (it was just earned). */
    unfoldNew: Boolean,
    onUnfolded: () -> Unit,
    onStop: () -> Unit,
    onElsewhere: (Land) -> Unit,
    /** A misted land that was just tapped: it wiggles and sparkles run from it to the next stop. */
    hintFrom: Land? = null,
    hintKey: Int = 0,
    modifier: Modifier = Modifier,
    /** Drawn next to the current stop, like a "you are here" marker. */
    hero: @Composable (Modifier) -> Unit = {},
    /** Drawn above the current stop, e.g. a pointing hand. */
    pointer: @Composable (Modifier, Dp) -> Unit = { _, _ -> },
) {
    val t = rememberInfiniteTransition(label = "island")
    val slow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "slow")
    val mid by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "mid")
    val fast by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "fast")
    val flap by t.animateFloat(0f, 1f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "flap")
    val pulse by t.animateFloat(1f, 1.1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse")

    val current = Trail.stopOf(chapter)
    val lap = chapter / Trail.size
    // A land the trail has just reached: its mist clears as the new stop unfolds.
    val newLand = if (unfoldNew && Trail.opensLand(chapter)) Trail.landOf(chapter) else null
    val clearing = remember(newLand) { Animatable(if (newLand != null) 0f else 1f) }
    LaunchedEffect(newLand) { if (newLand != null) clearing.animateTo(1f, tween(1400)) }

    val hint = remember(hintKey) { Animatable(0f) }
    LaunchedEffect(hintKey) {
        if (hintKey > 0) {
            hint.snapTo(0f)
            hint.animateTo(1f, tween(3600, easing = LinearEasing))
        }
    }

    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            ocean(mid, fast)
            boat(fast)
            fish(mid)
            island()
            river(fast)
            trail(current, lap)
            forest(Land.FOREST)
            meadow(Land.MEADOW)
            lagoon(Land.LAGOON, fast)
            market(Land.MARKET, fast)
            factory(Land.FACTORY, fast)
            peaks(Land.PEAKS)
            caves(Land.CAVES, fast)
            castle(Land.CLOUDS, mid)
        }
        // Scenery sits back; the trail stops below stand out. The wash reaches past the map's edges
        // so the sea around it matches and there's no visible rectangle.
        SceneryWash(overflow = 3000f)
        Canvas(Modifier.fillMaxSize()) {
            Land.entries.forEach { land ->
                // The tapped misty land gives a quick wiggle.
                val wiggle = if (land == hintFrom && hint.value in 0.001f..0.25f) sin(hint.value * 90f) * u * 0.03f * (1f - hint.value / 0.25f) else 0f
                withTransform({ translate(wiggle, 0f) }) {
                    when {
                        land == newLand -> mist(land, slow, 1f - clearing.value)
                        !reached(land) -> mist(land, slow, 1f)
                    }
                }
            }
            if (hintFrom != null && hint.value in 0.001f..0.999f) sparklePath(at(hintFrom), Trail.position(current).let { (x, y) -> p(x, y) }, hint.value)
            birds(slow, flap)
        }

        val unit = minOf(w, h)
        // Tapping anywhere near a land says something helpful.
        Land.entries.forEach { land ->
            val r = unit * 0.13f
            Box(
                Modifier
                    .offset(x = w * land.mapX - r, y = h * land.mapY - r)
                    .size(r * 2)
                    .clip(CircleShape)
                    .clickable { onElsewhere(land) },
            )
        }

        // Finished stops: flat star stickers on the trail.
        val sticker = unit * 0.075f
        (0 until Trail.size).forEach { s ->
            val done = lap > 0 || s < current
            if (done && s != current) {
                val (fx, fy) = Trail.position(s)
                Box(
                    Modifier
                        .offset(x = w * fx - sticker / 2, y = h * fy - sticker / 2)
                        .size(sticker)
                        .rotate(((s * 37) % 21 - 10).toFloat())
                        .paperCard(Color(0xFFFFF8E1), circle = true),
                    contentAlignment = Alignment.Center,
                ) { Text("⭐", fontSize = (sticker.value * 0.55f).sp) }
            }
        }

        // The next stop: a big raised paper button that unfolds when it is new.
        // It keeps the same physical size on a phone and a tablet.
        val (cx, cy) = Trail.position(current)
        val node = 96.dp
        Box(
            Modifier
                .offset(x = w * cx - node / 2, y = h * cy - node / 2)
                .size(node)
                .zIndex(2f)
                .unfold(unfoldNew, onUnfolded),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(node)
                    .scale(pulse)
                    .border(6.dp, Color(0xFFFFD600), CircleShape),
            )
            PaperButton(Color(0xFFFFD54F), Modifier.size(node * 0.78f), circle = true, onClick = onStop) {
                ArrowIcon(node * 0.36f, color = Color(0xFF2E7D32))
            }
        }
        val heroOnRight = cx < 0.5f
        hero(
            Modifier
                .offset(x = if (heroOnRight) w * cx + node * 0.55f else w * cx - node * 1.2f, y = h * cy - node * 0.6f)
                .size(width = node * 0.7f, height = node * 0.85f)
                .zIndex(3f),
        )
        val hand = node * 0.55f
        pointer(
            Modifier
                .offset(x = w * cx - hand / 2, y = h * cy - node / 2 - hand * 1.3f)
                .zIndex(4f),
            hand,
        )
    }
}

// ---------------------------------------------------------------- helpers

private fun DrawScope.p(fx: Float, fy: Float) = Offset(size.width * fx, size.height * fy)
private val DrawScope.u get() = size.minDimension
private fun DrawScope.at(land: Land) = p(land.mapX, land.mapY)

/** A smooth closed curve through [pts] (Catmull-Rom turned into Bezier curves). */
private fun smooth(pts: List<Offset>): Path {
    val path = Path()
    val n = pts.size
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until n) {
        val p0 = pts[(i - 1 + n) % n]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % n]
        val p3 = pts[(i + 2) % n]
        val c1 = p1 + (p2 - p0) / 6f
        val c2 = p2 - (p3 - p1) / 6f
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    path.close()
    return path
}

private const val CX = 0.45f
private const val CY = 0.53f
private const val RX = 0.40f
private const val RY = 0.43f
private val wobble = listOf(1.0f, 0.95f, 1.05f, 0.98f, 0.92f, 1.04f, 1.0f, 0.94f, 1.06f, 0.97f, 1.02f, 0.93f, 1.05f, 0.99f, 0.96f)

private fun DrawScope.coast(scale: Float): Path = smooth(
    wobble.mapIndexed { i, f ->
        val a = i * 2 * PI / wobble.size
        p(CX + (RX * f * scale * cos(a)).toFloat(), CY + (RY * f * scale * sin(a)).toFloat())
    },
)

private fun DrawScope.blob(c: Offset, rx: Float, ry: Float, color: Color, seed: Int = 0) {
    val n = 9
    val pts = (0 until n).map { i ->
        val a = i * 2 * PI / n
        val f = 0.85f + 0.3f * (((i * 7 + seed * 13) % 10) / 10f)
        Offset(c.x + (rx * f * cos(a)).toFloat(), c.y + (ry * f * sin(a)).toFloat())
    }
    paperPath(smooth(pts), color)
}

// ---------------------------------------------------------------- sea

private fun DrawScope.ocean(mid: Float, fast: Float) {
    // Flat paper sea, with a slightly darker band of deep water around the edges.
    drawRect(Color(0xFF3FA9E0))
    paperPath(coast(1.32f), Color(0xFF58BCEB))
    // Shimmering wave marks drifting slowly.
    for (i in 0 until 26) {
        val fx = ((i * 0.137f + mid * (0.04f + (i % 3) * 0.02f)) % 1f)
        val fy = ((i * 0.293f) % 1f)
        val a = ((sin((fast + i * 0.17f) * 2 * PI) + 1) / 2).toFloat()
        drawArc(
            Color.White.copy(alpha = 0.15f + 0.35f * a), 200f, 140f, false,
            p(fx, fy), Size(u * 0.05f, u * 0.025f),
            style = Stroke(u * 0.006f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.boat(fast: Float) {
    val c = p(0.06f, 0.88f)
    val bob = sin(fast * 2 * PI).toFloat() * u * 0.008f
    val s = u * 0.05f
    withTransform({ translate(0f, bob); rotate(sin(fast * 2 * PI).toFloat() * 4f, c) }) {
        val hull = Path().apply {
            moveTo(c.x - s, c.y)
            lineTo(c.x + s, c.y)
            lineTo(c.x + s * 0.7f, c.y + s * 0.4f)
            lineTo(c.x - s * 0.7f, c.y + s * 0.4f)
            close()
        }
        paperPath(hull, Color(0xFF8D6E63))
        drawLine(Color(0xFF5D4037), Offset(c.x, c.y), Offset(c.x, c.y - s * 1.4f), u * 0.005f)
        val sail = Path().apply {
            moveTo(c.x + s * 0.05f, c.y - s * 1.35f)
            lineTo(c.x + s * 0.8f, c.y - s * 0.15f)
            lineTo(c.x + s * 0.05f, c.y - s * 0.15f)
            close()
        }
        paperPath(sail, Color.White)
        val flag = Path().apply {
            moveTo(c.x, c.y - s * 1.4f)
            lineTo(c.x - s * 0.35f, c.y - s * 1.28f)
            lineTo(c.x, c.y - s * 1.16f)
            close()
        }
        paperPath(flag, Color(0xFFE53935))
    }
}

/** Little fish swimming back and forth in open water. */
private fun DrawScope.fish(mid: Float) {
    data class Lane(val x0: Float, val x1: Float, val y: Float, val color: Long, val speed: Float, val phase: Float)
    val lanes = listOf(
        Lane(0.02f, 0.13f, 0.14f, 0xFFFF8A65, 1f, 0f),
        Lane(0.01f, 0.08f, 0.32f, 0xFFFFD54F, 1.4f, 0.4f),
        Lane(0.14f, 0.3f, 0.97f, 0xFFFF7043, 1.2f, 0.7f),
        Lane(0.88f, 0.98f, 0.44f, 0xFFFFB74D, 0.9f, 0.2f),
        Lane(0.82f, 0.97f, 0.92f, 0xFFF06292, 1.1f, 0.55f),
        Lane(0.55f, 0.72f, 0.985f, 0xFFFFD54F, 1.3f, 0.1f),
    )
    lanes.forEach { l ->
        // Swims one way, turns around, swims back.
        val t = (mid * l.speed + l.phase) % 1f
        val goingRight = t < 0.5f
        val k = if (goingRight) t * 2 else (1 - t) * 2
        val eased = (1 - cos(k * PI).toFloat()) / 2
        val c = p(l.x0 + (l.x1 - l.x0) * eased, l.y + 0.01f * sin(mid * 12 * PI + l.phase * 10).toFloat())
        val s = u * 0.016f
        val dir = if (goingRight) 1f else -1f
        val col = Color(l.color)
        paperOval(col, Offset(c.x - s, c.y - s * 0.55f), Size(s * 2, s * 1.1f))
        val tail = Path().apply {
            moveTo(c.x - dir * s * 0.8f, c.y)
            lineTo(c.x - dir * s * 1.6f, c.y - s * 0.6f)
            lineTo(c.x - dir * s * 1.6f, c.y + s * 0.6f)
            close()
        }
        paperPath(tail, col)
        paperCircle(Color(0xFF263238), s * 0.18f, Offset(c.x + dir * s * 0.5f, c.y - s * 0.1f))
    }
}

// ---------------------------------------------------------------- land

private fun DrawScope.island() {
    // Shallow water, then the beach, then the grass.
    paperPath(coast(1.07f), Color(0xFF81D4FA))
    paperPath(coast(1.0f), Color(0xFFFFE0B2))
    paperPath(coast(0.93f), Color(0xFF9CCC65))
    // Soft lighter patches so the grass isn't flat.
    blob(p(0.30f, 0.50f), u * 0.16f, u * 0.10f, Color(0xFFAED581), 1)
    blob(p(0.55f, 0.70f), u * 0.14f, u * 0.08f, Color(0xFFAED581), 2)
    blob(p(0.48f, 0.25f), u * 0.12f, u * 0.07f, Color(0xFFAED581), 3)
    // A few bushes dotted around.
    listOf(0.22f to 0.36f, 0.48f to 0.18f, 0.62f to 0.86f, 0.42f to 0.68f, 0.82f to 0.48f).forEach { (x, y) ->
        paperCircle(Color(0xFF7CB342), u * 0.018f, p(x, y))
        paperCircle(Color(0xFF8BC34A), u * 0.013f, p(x + 0.012f, y - 0.01f))
    }
}

private fun DrawScope.river(fast: Float) {
    val a = p(Land.PEAKS.mapX - 0.02f, Land.PEAKS.mapY + 0.05f)
    val c1 = p(0.64f, 0.52f)
    val c2 = p(0.58f, 0.68f)
    val b = p(Land.LAGOON.mapX + 0.03f, Land.LAGOON.mapY - 0.03f)
    val path = Path().apply {
        moveTo(a.x, a.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
    drawPath(path, Color(0xFF4FC3F7), style = Stroke(u * 0.028f, cap = StrokeCap.Round))
    drawPath(
        path, Color.White.copy(alpha = 0.5f),
        style = Stroke(u * 0.006f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.02f, u * 0.05f), -fast * u * 0.07f)),
    )
    // A little rope bridge where the patterns live.
    fun at(t: Float): Offset {
        val m = 1 - t
        return a * (m * m * m) + c1 * (3 * m * m * t) + c2 * (3 * m * t * t) + b * (t * t * t)
    }
    val q = at(0.22f)
    val dir = at(0.24f) - at(0.2f)
    val len = sqrt(dir.x * dir.x + dir.y * dir.y).coerceAtLeast(0.001f)
    val across = Offset(-dir.y / len, dir.x / len) * (u * 0.035f)
    val along = Offset(dir.x / len, dir.y / len) * (u * 0.011f)
    val planks = listOf(0xFFA1887F, 0xFF8D6E63)
    for (k in -2..2) {
        val c = q + along * (k * 1.25f)
        val plank = Path().apply {
            val p1 = c - across - along * 0.45f
            val p2 = c + across - along * 0.45f
            val p3 = c + across + along * 0.45f
            val p4 = c - across + along * 0.45f
            moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
        }
        paperPath(plank, Color(planks[(k + 2) % 2]))
    }
    listOf(-1f, 1f).forEach { side ->
        val r0 = q + across * side * 1.05f - along * 3f
        val r1 = q + across * side * 1.05f + along * 3f
        drawLine(Color(0xFF5D4037), r0, r1, u * 0.004f, cap = StrokeCap.Round)
    }
}

/** Two market stalls with striped paper awnings and fruit on the counters. */
private fun DrawScope.market(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-0.75f to 0xFFE53935, 0.75f to 0xFF1E88E5).forEachIndexed { k, (dx, stripe) ->
        val x = c.x + dx * s
        val y = c.y + (if (k == 0) 0.1f else -0.15f) * s
        // Posts and counter.
        paperRect(Color(0xFF8D6E63), Offset(x - s * 0.62f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFF8D6E63), Offset(x + s * 0.52f, y - s * 0.5f), Size(s * 0.1f, s * 0.95f))
        paperRect(Color(0xFFD7A26B), Offset(x - s * 0.7f, y + s * 0.15f), Size(s * 1.4f, s * 0.35f))
        // Striped awning with a scalloped edge.
        for (i in 0 until 5) {
            val col = if (i % 2 == 0) Color(stripe) else Color.White
            val left = x - s * 0.75f + i * s * 0.3f
            paperRect(col, Offset(left, y - s * 0.78f), Size(s * 0.3f, s * 0.32f))
            paperArc(col, 0f, 180f, true, Offset(left, y - s * 0.6f), Size(s * 0.3f, s * 0.24f))
        }
        // Fruit on the counter, gently bobbing as if being arranged.
        val bob = sin((fast + k * 0.5f) * 2 * PI).toFloat() * s * 0.02f
        listOf(0xFFFF5252, 0xFFFFCA28, 0xFF66BB6A).forEachIndexed { i, col ->
            paperCircle(Color(col), s * 0.12f, Offset(x - s * 0.35f + i * s * 0.35f, y + s * 0.08f + bob))
        }
    }
}

/** A smooth open curve through [pts]. */
private fun openCurve(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val p0 = pts[maxOf(i - 1, 0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[minOf(i + 2, pts.size - 1)]
        val c1 = p1 + (p2 - p0) / 6f
        val c2 = p2 - (p3 - p1) / 6f
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** The paper trail through every stop: the part already walked is bold, the rest is faint. */
private fun DrawScope.trail(current: Int, lap: Int) {
    val pts = (0 until Trail.size).map { Trail.position(it).let { (x, y) -> p(x, y) } }
    val walkedTo = if (lap > 0) pts.size - 1 else current
    val ahead = openCurve(pts.drop(walkedTo))
    drawPath(
        ahead, Color(0x88FFF3E0),
        style = Stroke(u * 0.008f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.012f, u * 0.022f))),
    )
    if (walkedTo > 0) {
        val walked = openCurve(pts.take(walkedTo + 1))
        drawPath(walked, Color(0xFFFFF3E0), style = Stroke(u * 0.016f, cap = StrokeCap.Round))
        drawPath(
            walked, Color(0xFFD7B98E),
            style = Stroke(u * 0.006f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.012f, u * 0.02f))),
        )
    }
}

private fun DrawScope.tree(c: Offset, s: Float, dark: Boolean) {
    paperRect(Color(0xFF6D4C41), Offset(c.x - s * 0.12f, c.y), Size(s * 0.24f, s * 0.5f))
    paperCircle(if (dark) Color(0xFF388E3C) else Color(0xFF43A047), s * 0.5f, Offset(c.x, c.y - s * 0.15f))
    paperCircle(if (dark) Color(0xFF43A047) else Color(0xFF66BB6A), s * 0.28f, Offset(c.x - s * 0.15f, c.y - s * 0.3f))
}

private fun DrawScope.pine(c: Offset, s: Float) {
    paperRect(Color(0xFF6D4C41), Offset(c.x - s * 0.08f, c.y + s * 0.2f), Size(s * 0.16f, s * 0.3f))
    for (k in 0..2) {
        val top = c.y - s * 0.6f + k * s * 0.25f
        val half = s * (0.25f + k * 0.1f)
        val tri = Path().apply {
            moveTo(c.x, top)
            lineTo(c.x + half, top + s * 0.4f)
            lineTo(c.x - half, top + s * 0.4f)
            close()
        }
        paperPath(tri, Color(0xFF2E7D32))
    }
}

private fun DrawScope.forest(land: Land) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-1.1f to 0.2f, 0.9f to 0.3f, 0f to -0.5f, -0.5f to 0.8f, 0.6f to -0.9f, 1.4f to -0.3f, -1.3f to -0.7f).forEachIndexed { i, (dx, dy) ->
        val q = Offset(c.x + dx * s, c.y + dy * s)
        if (i % 3 == 2) pine(q, s * 0.9f) else tree(q, s, i % 2 == 0)
    }
    paperCircle(Color(0xFFE53935), s * 0.12f, Offset(c.x + s * 0.3f, c.y + s * 0.9f))
    paperCircle(Color.White, s * 0.04f, Offset(c.x + s * 0.28f, c.y + s * 0.86f))
}

private fun DrawScope.meadow(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    blob(c, s * 1.5f, s * 0.9f, Color(0xFFDCE775), 5)
    val petals = listOf(0xFFFF80AB, 0xFFFFFFFF, 0xFFB388FF, 0xFFFF8A65)
    for (i in 0 until 14) {
        val a = i * 2.4
        val r = s * (0.3f + 0.08f * (i % 7))
        val q = Offset(c.x + (r * cos(a)).toFloat() * 1.6f, c.y + (r * sin(a)).toFloat())
        paperCircle(Color(petals[i % petals.size]), s * 0.08f, q)
    }
    // A big sunflower in the middle.
    drawLine(Color(0xFF558B2F), c, Offset(c.x, c.y - s * 0.9f), s * 0.08f)
    val head = Offset(c.x, c.y - s * 0.9f)
    for (k in 0 until 10) {
        val a = k * 2 * PI / 10
        paperCircle(Color(0xFFFFCA28), s * 0.16f, Offset(head.x + (s * 0.28f * cos(a)).toFloat(), head.y + (s * 0.28f * sin(a)).toFloat()))
    }
    paperCircle(Color(0xFF6D4C41), s * 0.2f, head)
}

private fun DrawScope.palm(c: Offset, s: Float, lean: Float) {
    val trunk = Path().apply {
        moveTo(c.x, c.y)
        quadraticTo(c.x + lean * s * 0.3f, c.y - s * 0.5f, c.x + lean * s * 0.2f, c.y - s)
    }
    drawPath(trunk, Color(0xFF8D6E63), style = Stroke(s * 0.12f, cap = StrokeCap.Round))
    val top = Offset(c.x + lean * s * 0.2f, c.y - s)
    listOf(-150f, -110f, -70f, -30f, 10f).forEach { deg ->
        val a = deg * PI / 180
        val end = Offset(top.x + (s * 0.55f * cos(a)).toFloat(), top.y + (s * 0.55f * sin(a)).toFloat() + s * 0.15f)
        val leaf = Path().apply {
            moveTo(top.x, top.y)
            quadraticTo((top.x + end.x) / 2, top.y - s * 0.25f, end.x, end.y)
        }
        drawPath(leaf, Color(0xFF43A047), style = Stroke(s * 0.1f, cap = StrokeCap.Round))
    }
    paperCircle(Color(0xFF6D4C41), s * 0.07f, Offset(top.x + s * 0.05f, top.y + s * 0.06f))
}

private fun DrawScope.lagoon(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    // A bay that opens out to the sea at the bottom of the island.
    blob(Offset(c.x, c.y + s * 0.3f), s * 1.5f, s * 0.9f, Color(0xFFFFE0B2), 7)
    blob(Offset(c.x, c.y + s * 0.4f), s * 1.25f, s * 0.7f, Color(0xFF26C6DA), 8)
    blob(Offset(c.x, c.y + s * 0.45f), s * 0.8f, s * 0.42f, Color(0xFF80DEEA), 9)
    val glint = ((sin(fast * 2 * PI) + 1) / 2).toFloat()
    drawArc(Color.White.copy(alpha = 0.4f + 0.4f * glint), 200f, 140f, false, Offset(c.x - s * 0.3f, c.y + s * 0.3f), Size(s * 0.4f, s * 0.2f), style = Stroke(s * 0.05f))
    palm(Offset(c.x - s * 1.2f, c.y + s * 0.1f), s, 1f)
    palm(Offset(c.x + s * 1.25f, c.y), s * 0.85f, -1f)
}

private fun DrawScope.factory(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    // Puffs of smoke drifting up from the chimney.
    for (k in 0..2) {
        val t = (fast + k / 3f) % 1f
        paperCircle(
            Color.White.copy(alpha = 0.7f * (1 - t)), s * (0.15f + 0.2f * t),
            Offset(c.x + s * 0.55f + t * s * 0.4f, c.y - s * 1.0f - t * s * 0.9f),
        )
    }
    paperRect(Color(0xFF78909C), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.7f))
    paperRect(Color(0xFFE53935), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.1f))
    paperRect(Color(0xFF90A4AE), Offset(c.x - s * 0.9f, c.y - s * 0.4f), Size(s * 1.8f, s * 0.8f))
    // Saw-tooth roof.
    for (k in 0..2) {
        val x0 = c.x - s * 0.9f + k * s * 0.6f
        val roof = Path().apply {
            moveTo(x0, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.75f)
            close()
        }
        paperPath(roof, Color(0xFF607D8B))
    }
    for (k in 0..2) paperRect(Color(0xFFFFF59D), Offset(c.x - s * 0.7f + k * s * 0.55f, c.y - s * 0.15f), Size(s * 0.3f, s * 0.25f))
    paperCircle(Color(0xFFFFB300), s * 0.2f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
    paperCircle(Color(0xFF90A4AE), s * 0.08f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
}

private fun DrawScope.mountain(base: Offset, wide: Float, tall: Float, color: Color) {
    val m = Path().apply {
        moveTo(base.x - wide, base.y)
        lineTo(base.x, base.y - tall)
        lineTo(base.x + wide, base.y)
        close()
    }
    paperPath(m, color)
    val cap = Path().apply {
        moveTo(base.x, base.y - tall)
        lineTo(base.x + wide * 0.32f, base.y - tall * 0.68f)
        lineTo(base.x + wide * 0.12f, base.y - tall * 0.74f)
        lineTo(base.x - wide * 0.05f, base.y - tall * 0.66f)
        lineTo(base.x - wide * 0.32f, base.y - tall * 0.68f)
        close()
    }
    paperPath(cap, Color.White)
}

private fun DrawScope.peaks(land: Land) {
    val c = at(land)
    val s = u * 0.07f
    mountain(Offset(c.x - s * 0.8f, c.y + s * 0.5f), s * 0.9f, s * 1.4f, Color(0xFF9575CD))
    mountain(Offset(c.x + s * 0.9f, c.y + s * 0.55f), s * 0.8f, s * 1.2f, Color(0xFF7E57C2))
    mountain(Offset(c.x, c.y + s * 0.6f), s * 1.1f, s * 1.9f, Color(0xFF8E7CC3))
}

private fun DrawScope.caves(land: Land, fast: Float) {
    val c = at(land)
    val s = u * 0.07f
    blob(Offset(c.x, c.y), s * 1.4f, s * 1.0f, Color(0xFF8D8D8D), 11)
    blob(Offset(c.x - s * 0.2f, c.y - s * 0.2f), s * 0.9f, s * 0.6f, Color(0xFFA1A1A1), 12)
    paperArc(Color(0xFF263238), 180f, 180f, true, Offset(c.x - s * 0.5f, c.y - s * 0.2f), Size(s, s * 0.9f))
    paperRect(Color(0xFF263238), Offset(c.x - s * 0.5f, c.y + s * 0.25f), Size(s, s * 0.3f))
    listOf(-0.9f to -0.4f, 0.8f to -0.5f, 0.95f to 0.3f).forEachIndexed { i, (dx, dy) ->
        val a = ((sin((fast + i * 0.3f) * 2 * PI) + 1) / 2).toFloat()
        star(Offset(c.x + dx * s, c.y + dy * s), s * 0.18f, Color(0xFF80DEEA).copy(alpha = 0.5f + 0.5f * a))
    }
}

private fun DrawScope.castle(land: Land, mid: Float) {
    val c = at(land)
    val s = u * 0.065f
    val float = sin(mid * 2 * PI).toFloat() * s * 0.12f
    // Its shadow on the sea far below shows the castle is up in the sky.
    val shadowW = s * (2.6f - float / s)
    drawOval(
        Color(0x22003050),
        Offset(c.x - shadowW / 2, c.y + s * 3.4f),
        Size(shadowW, s * 0.55f),
    )
    withTransform({ translate(0f, float) }) {
        // A fluffy cloud island.
        listOf(-1.0f to 0.55f, -0.4f to 0.75f, 0.3f to 0.75f, 0.95f to 0.55f, 0f to 0.45f).forEach { (dx, dy) ->
            paperCircle(Color.White, s * 0.55f, Offset(c.x + dx * s, c.y + dy * s))
        }
        // Towers with pointy roofs and flags.
        paperRect(Color(0xFFF8BBD0), Offset(c.x - s * 0.6f, c.y - s * 0.5f), Size(s * 1.2f, s * 0.9f))
        listOf(-0.75f, 0.75f, 0f).forEachIndexed { i, dx ->
            val tall = if (i == 2) 1.3f else 1.0f
            val x = c.x + dx * s
            paperRect(Color(0xFFF48FB1), Offset(x - s * 0.22f, c.y - s * tall), Size(s * 0.44f, s * tall + s * 0.4f))
            val roof = Path().apply {
                moveTo(x - s * 0.3f, c.y - s * tall)
                lineTo(x, c.y - s * tall - s * 0.5f)
                lineTo(x + s * 0.3f, c.y - s * tall)
                close()
            }
            paperPath(roof, Color(0xFF7E57C2))
            drawLine(Color(0xFF5D4037), Offset(x, c.y - s * tall - s * 0.5f), Offset(x, c.y - s * tall - s * 0.8f), s * 0.05f)
            val wave = sin(mid * 2 * PI * 6 + i).toFloat() * s * 0.04f
            val flag = Path().apply {
                moveTo(x, c.y - s * tall - s * 0.8f)
                lineTo(x + s * 0.3f, c.y - s * tall - s * 0.72f + wave)
                lineTo(x, c.y - s * tall - s * 0.62f)
                close()
            }
            paperPath(flag, Color(0xFFFFD54F))
        }
        paperArc(Color(0xFF7E57C2), 180f, 180f, true, Offset(c.x - s * 0.2f, c.y - s * 0.1f), Size(s * 0.4f, s * 0.4f))
        paperRect(Color(0xFF7E57C2), Offset(c.x - s * 0.2f, c.y + s * 0.1f), Size(s * 0.4f, s * 0.3f))
        // A little rainbow behind.
        listOf(0xFFFF8A80, 0xFFFFE082, 0xFFA5D6A7, 0xFF90CAF9).forEachIndexed { i, col ->
            drawArc(
                Color(col).copy(alpha = 0.6f), 180f, 180f, false,
                Offset(c.x - s * (1.9f - i * 0.12f), c.y - s * (1.4f - i * 0.12f)), Size(s * (1.2f - i * 0.24f), s * (1.2f - i * 0.24f)),
                style = Stroke(s * 0.1f),
            )
        }
    }
}

/** Sparkles that travel from [from] to [to] (twice), showing the way to the next stop. */
private fun DrawScope.sparklePath(from: Offset, to: Offset, t: Float) {
    // A faint dotted guide line, then a trail of big sparkles running along it.
    val fadeAll = if (t > 0.85f) (1f - t) / 0.15f else 1f
    drawLine(
        Color.White.copy(alpha = 0.6f * fadeAll), from, to, u * 0.008f, cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.01f, u * 0.02f)),
    )
    val head = (t * 2.2f) % 1.1f
    for (k in 0 until 8) {
        val f = head - k * 0.06f
        if (f < 0f || f > 1f) continue
        val q = from + (to - from) * f
        val lift = sin(f * PI * 3).toFloat() * u * 0.02f
        val fade = (1f - k / 8f) * fadeAll
        star(Offset(q.x, q.y - lift), u * (0.04f - k * 0.003f), Color(0xFFFFD54F).copy(alpha = fade.coerceIn(0f, 1f)))
    }
}

/** Soft drifting mist over a land that isn't open yet. */
private fun DrawScope.mist(land: Land, slow: Float, strength: Float) {
    if (strength <= 0.01f) return
    val c = at(land)
    val s = u * 0.07f
    val drift = sin(slow * 2 * PI + land.ordinal).toFloat() * s * 0.25f
    listOf(-1.0f to 0.1f, -0.3f to -0.4f, 0.5f to -0.2f, 1.0f to 0.25f, 0f to 0.45f, -0.6f to 0.6f, 0.6f to 0.7f, 0f to -0.9f).forEachIndexed { i, (dx, dy) ->
        val r = s * (0.75f + 0.15f * (i % 3))
        paperCircle(Color(0xFFECEFF1).copy(alpha = 0.62f * strength), r, Offset(c.x + dx * s + drift * (if (i % 2 == 0) 1 else -1), c.y + dy * s))
    }
}

/** Seagulls flapping across the sky. */
private fun DrawScope.birds(slow: Float, flap: Float) {
    listOf(Triple(0f, 0.12f, 1f), Triple(0.45f, 0.42f, 0.8f)).forEach { (phase, y, scale) ->
        val t = (slow * 1.6f + phase) % 1f
        val c = p(-0.05f + 1.1f * t, y + 0.03f * sin(t * 2 * PI * 3).toFloat())
        val s = u * 0.025f * scale
        val lift = s * (0.2f + 0.6f * flap)
        listOf(-1f, 1f).forEach { side ->
            val wing = Path().apply {
                moveTo(c.x, c.y)
                quadraticTo(c.x + side * s * 0.5f, c.y - lift, c.x + side * s, c.y - lift * 0.3f)
            }
            drawPath(wing, Color(0xFF37474F), style = Stroke(s * 0.22f, cap = StrokeCap.Round))
        }
    }
}
