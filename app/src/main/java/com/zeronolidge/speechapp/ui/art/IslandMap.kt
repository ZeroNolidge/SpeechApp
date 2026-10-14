package com.zeronolidge.speechapp.ui.art

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The world map as an illustrated island: beaches, a river, a forest, a meadow, a lagoon,
 * a factory, mountains, caves, and a castle on a floating cloud. Locked lands hide under
 * drifting mist. Seagulls fly over, fish swim in the sea, and a little boat bobs on the waves.
 */
@Composable
fun IslandMap(
    open: (Land) -> Boolean,
    suggested: Land,
    onTap: (Land) -> Unit,
    modifier: Modifier = Modifier,
    /** Drawn next to the suggested land, like a "you are here" marker. */
    hero: @Composable (Modifier) -> Unit = {},
    /** Drawn above the suggested land, e.g. a pointing hand. */
    pointer: @Composable (Modifier, Dp) -> Unit = { _, _ -> },
) {
    val t = rememberInfiniteTransition(label = "island")
    val slow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "slow")
    val mid by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "mid")
    val fast by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "fast")
    val flap by t.animateFloat(0f, 1f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "flap")
    val pulse by t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse")

    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            ocean(mid, fast)
            boat(fast)
            fish(mid)
            island()
            river(fast)
            trail()
            forest(Land.FOREST)
            meadow(Land.MEADOW)
            lagoon(Land.LAGOON, fast)
            factory(Land.FACTORY, fast)
            peaks(Land.PEAKS)
            caves(Land.CAVES, fast)
            castle(Land.CLOUDS, mid)
            Land.entries.filter { !open(it) }.forEach { mist(it, slow) }
            birds(slow, flap)
        }

        val node = minOf(w / 6.5f, h / 3.4f, 130.dp)
        Land.entries.forEach { land ->
            val isOpen = open(land)
            val x = w * land.mapX
            val y = h * land.mapY
            // An invisible tap target over each land, with its name below.
            Box(
                Modifier
                    .offset(x = x - node / 2, y = y - node / 2)
                    .size(node)
                    .scale(if (land == suggested) pulse else 1f)
                    .clip(CircleShape)
                    .then(
                        if (land == suggested) Modifier.border(6.dp, Color(0xFFFFD600), CircleShape)
                        else Modifier,
                    )
                    .clickable { onTap(land) }
                    .zIndex(1f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (!isOpen) {
                    Column(
                        Modifier
                            .padding(bottom = node * 0.18f)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xCC455A64))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🔒 ${land.needs?.emoji} ${land.needs?.level}", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                land.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOpen) Color(0xFF3A2E5C) else Color(0xFF607D8B),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = x - node * 0.6f, y = y + node * 0.42f)
                    .width(node * 1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xBBFFFFFF))
                    .padding(horizontal = 4.dp),
            )
        }
        val sx = w * suggested.mapX
        val sy = h * suggested.mapY
        hero(
            Modifier
                .offset(x = sx - node * 1.25f, y = sy - node * 0.35f)
                .size(width = node * 0.7f, height = node * 0.85f)
                .zIndex(2f),
        )
        val hand = node * 0.55f
        pointer(
            Modifier
                .offset(x = sx - hand / 2, y = sy - node / 2 - hand * 1.25f)
                .zIndex(3f),
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
    drawPath(smooth(pts), color)
}

// ---------------------------------------------------------------- sea

private fun DrawScope.ocean(mid: Float, fast: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))))
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
        drawPath(hull, Color(0xFF8D6E63))
        drawLine(Color(0xFF5D4037), Offset(c.x, c.y), Offset(c.x, c.y - s * 1.4f), u * 0.005f)
        val sail = Path().apply {
            moveTo(c.x + s * 0.05f, c.y - s * 1.35f)
            lineTo(c.x + s * 0.8f, c.y - s * 0.15f)
            lineTo(c.x + s * 0.05f, c.y - s * 0.15f)
            close()
        }
        drawPath(sail, Color.White)
        val flag = Path().apply {
            moveTo(c.x, c.y - s * 1.4f)
            lineTo(c.x - s * 0.35f, c.y - s * 1.28f)
            lineTo(c.x, c.y - s * 1.16f)
            close()
        }
        drawPath(flag, Color(0xFFE53935))
    }
}

/** Little fish swimming back and forth in open water. */
private fun DrawScope.fish(mid: Float) {
    data class Lane(val x0: Float, val x1: Float, val y: Float, val color: Long, val speed: Float, val phase: Float)
    val lanes = listOf(
        Lane(0.02f, 0.13f, 0.14f, 0xFFFF8A65, 1f, 0f),
        Lane(0.01f, 0.08f, 0.32f, 0xFFFFD54F, 1.4f, 0.4f),
        Lane(0.14f, 0.3f, 0.97f, 0xFFFF7043, 1.2f, 0.7f),
        Lane(0.86f, 0.98f, 0.12f, 0xFFFFB74D, 0.9f, 0.2f),
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
        drawOval(col, Offset(c.x - s, c.y - s * 0.55f), Size(s * 2, s * 1.1f))
        val tail = Path().apply {
            moveTo(c.x - dir * s * 0.8f, c.y)
            lineTo(c.x - dir * s * 1.6f, c.y - s * 0.6f)
            lineTo(c.x - dir * s * 1.6f, c.y + s * 0.6f)
            close()
        }
        drawPath(tail, col)
        drawCircle(Color(0xFF263238), s * 0.18f, Offset(c.x + dir * s * 0.5f, c.y - s * 0.1f))
    }
}

// ---------------------------------------------------------------- land

private fun DrawScope.island() {
    // Shallow water, then the beach, then the grass.
    drawPath(coast(1.07f), Color(0xFF81D4FA))
    drawPath(coast(1.0f), Color(0xFFFFE0B2))
    drawPath(coast(0.93f), Color(0xFF9CCC65))
    // Soft lighter patches so the grass isn't flat.
    blob(p(0.30f, 0.50f), u * 0.16f, u * 0.10f, Color(0xFFAED581), 1)
    blob(p(0.55f, 0.70f), u * 0.14f, u * 0.08f, Color(0xFFAED581), 2)
    blob(p(0.48f, 0.25f), u * 0.12f, u * 0.07f, Color(0xFFAED581), 3)
    // A few bushes dotted around.
    listOf(0.24f to 0.75f, 0.62f to 0.24f, 0.36f to 0.18f, 0.58f to 0.82f, 0.20f to 0.40f).forEach { (x, y) ->
        drawCircle(Color(0xFF7CB342), u * 0.018f, p(x, y))
        drawCircle(Color(0xFF8BC34A), u * 0.013f, p(x + 0.012f, y - 0.01f))
    }
}

private fun DrawScope.river(fast: Float) {
    val path = Path().apply {
        val a = p(Land.PEAKS.mapX - 0.02f, Land.PEAKS.mapY + 0.05f)
        moveTo(a.x, a.y)
        val c1 = p(0.60f, 0.78f)
        val c2 = p(0.50f, 0.62f)
        val b = p(Land.LAGOON.mapX + 0.04f, Land.LAGOON.mapY - 0.02f)
        cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
    drawPath(path, Color(0xFF4FC3F7), style = Stroke(u * 0.028f, cap = StrokeCap.Round))
    drawPath(
        path, Color.White.copy(alpha = 0.5f),
        style = Stroke(u * 0.006f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.02f, u * 0.05f), -fast * u * 0.07f)),
    )
}

private fun DrawScope.trail() {
    val pts = Land.entries.map { at(it) }
    val path = Path()
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
    drawPath(
        path, Color(0xFFFFF3E0),
        style = Stroke(u * 0.012f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * 0.015f, u * 0.02f))),
    )
}

private fun DrawScope.tree(c: Offset, s: Float, dark: Boolean) {
    drawRect(Color(0xFF6D4C41), Offset(c.x - s * 0.12f, c.y), Size(s * 0.24f, s * 0.5f))
    drawCircle(if (dark) Color(0xFF388E3C) else Color(0xFF43A047), s * 0.5f, Offset(c.x, c.y - s * 0.15f))
    drawCircle(if (dark) Color(0xFF43A047) else Color(0xFF66BB6A), s * 0.28f, Offset(c.x - s * 0.15f, c.y - s * 0.3f))
}

private fun DrawScope.pine(c: Offset, s: Float) {
    drawRect(Color(0xFF6D4C41), Offset(c.x - s * 0.08f, c.y + s * 0.2f), Size(s * 0.16f, s * 0.3f))
    for (k in 0..2) {
        val top = c.y - s * 0.6f + k * s * 0.25f
        val half = s * (0.25f + k * 0.1f)
        val tri = Path().apply {
            moveTo(c.x, top)
            lineTo(c.x + half, top + s * 0.4f)
            lineTo(c.x - half, top + s * 0.4f)
            close()
        }
        drawPath(tri, Color(0xFF2E7D32))
    }
}

private fun DrawScope.forest(land: Land) {
    val c = at(land)
    val s = u * 0.06f
    listOf(-1.1f to 0.2f, 0.9f to 0.3f, 0f to -0.5f, -0.5f to 0.8f, 0.6f to -0.9f, 1.4f to -0.3f, -1.3f to -0.7f).forEachIndexed { i, (dx, dy) ->
        val q = Offset(c.x + dx * s, c.y + dy * s)
        if (i % 3 == 2) pine(q, s * 0.9f) else tree(q, s, i % 2 == 0)
    }
    drawCircle(Color(0xFFE53935), s * 0.12f, Offset(c.x + s * 0.3f, c.y + s * 0.9f))
    drawCircle(Color.White, s * 0.04f, Offset(c.x + s * 0.28f, c.y + s * 0.86f))
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
        drawCircle(Color(petals[i % petals.size]), s * 0.08f, q)
    }
    // A big sunflower in the middle.
    drawLine(Color(0xFF558B2F), c, Offset(c.x, c.y - s * 0.9f), s * 0.08f)
    val head = Offset(c.x, c.y - s * 0.9f)
    for (k in 0 until 10) {
        val a = k * 2 * PI / 10
        drawCircle(Color(0xFFFFCA28), s * 0.16f, Offset(head.x + (s * 0.28f * cos(a)).toFloat(), head.y + (s * 0.28f * sin(a)).toFloat()))
    }
    drawCircle(Color(0xFF6D4C41), s * 0.2f, head)
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
    drawCircle(Color(0xFF6D4C41), s * 0.07f, Offset(top.x + s * 0.05f, top.y + s * 0.06f))
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
        drawCircle(
            Color.White.copy(alpha = 0.7f * (1 - t)), s * (0.15f + 0.2f * t),
            Offset(c.x + s * 0.55f + t * s * 0.4f, c.y - s * 1.0f - t * s * 0.9f),
        )
    }
    drawRect(Color(0xFF78909C), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.7f))
    drawRect(Color(0xFFE53935), Offset(c.x + s * 0.4f, c.y - s * 1.0f), Size(s * 0.3f, s * 0.1f))
    drawRect(Color(0xFF90A4AE), Offset(c.x - s * 0.9f, c.y - s * 0.4f), Size(s * 1.8f, s * 0.8f))
    // Saw-tooth roof.
    for (k in 0..2) {
        val x0 = c.x - s * 0.9f + k * s * 0.6f
        val roof = Path().apply {
            moveTo(x0, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.4f)
            lineTo(x0 + s * 0.6f, c.y - s * 0.75f)
            close()
        }
        drawPath(roof, Color(0xFF607D8B))
    }
    for (k in 0..2) drawRect(Color(0xFFFFF59D), Offset(c.x - s * 0.7f + k * s * 0.55f, c.y - s * 0.15f), Size(s * 0.3f, s * 0.25f))
    drawCircle(Color(0xFFFFB300), s * 0.2f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
    drawCircle(Color(0xFF90A4AE), s * 0.08f, Offset(c.x - s * 0.95f, c.y + s * 0.4f))
}

private fun DrawScope.mountain(base: Offset, wide: Float, tall: Float, color: Color) {
    val m = Path().apply {
        moveTo(base.x - wide, base.y)
        lineTo(base.x, base.y - tall)
        lineTo(base.x + wide, base.y)
        close()
    }
    drawPath(m, color)
    val cap = Path().apply {
        moveTo(base.x, base.y - tall)
        lineTo(base.x + wide * 0.32f, base.y - tall * 0.68f)
        lineTo(base.x + wide * 0.12f, base.y - tall * 0.74f)
        lineTo(base.x - wide * 0.05f, base.y - tall * 0.66f)
        lineTo(base.x - wide * 0.32f, base.y - tall * 0.68f)
        close()
    }
    drawPath(cap, Color.White)
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
    drawArc(Color(0xFF263238), 180f, 180f, true, Offset(c.x - s * 0.5f, c.y - s * 0.2f), Size(s, s * 0.9f))
    drawRect(Color(0xFF263238), Offset(c.x - s * 0.5f, c.y + s * 0.25f), Size(s, s * 0.3f))
    listOf(-0.9f to -0.4f, 0.8f to -0.5f, 0.95f to 0.3f).forEachIndexed { i, (dx, dy) ->
        val a = ((sin((fast + i * 0.3f) * 2 * PI) + 1) / 2).toFloat()
        star(Offset(c.x + dx * s, c.y + dy * s), s * 0.18f, Color(0xFF80DEEA).copy(alpha = 0.5f + 0.5f * a))
    }
}

private fun DrawScope.castle(land: Land, mid: Float) {
    val c = at(land)
    val s = u * 0.065f
    val float = sin(mid * 2 * PI).toFloat() * s * 0.12f
    withTransform({ translate(0f, float) }) {
        // A fluffy cloud island.
        listOf(-1.0f to 0.55f, -0.4f to 0.75f, 0.3f to 0.75f, 0.95f to 0.55f, 0f to 0.45f).forEach { (dx, dy) ->
            drawCircle(Color.White, s * 0.55f, Offset(c.x + dx * s, c.y + dy * s))
        }
        // Towers with pointy roofs and flags.
        drawRect(Color(0xFFF8BBD0), Offset(c.x - s * 0.6f, c.y - s * 0.5f), Size(s * 1.2f, s * 0.9f))
        listOf(-0.75f, 0.75f, 0f).forEachIndexed { i, dx ->
            val tall = if (i == 2) 1.3f else 1.0f
            val x = c.x + dx * s
            drawRect(Color(0xFFF48FB1), Offset(x - s * 0.22f, c.y - s * tall), Size(s * 0.44f, s * tall + s * 0.4f))
            val roof = Path().apply {
                moveTo(x - s * 0.3f, c.y - s * tall)
                lineTo(x, c.y - s * tall - s * 0.5f)
                lineTo(x + s * 0.3f, c.y - s * tall)
                close()
            }
            drawPath(roof, Color(0xFF7E57C2))
            drawLine(Color(0xFF5D4037), Offset(x, c.y - s * tall - s * 0.5f), Offset(x, c.y - s * tall - s * 0.8f), s * 0.05f)
            val wave = sin(mid * 2 * PI * 6 + i).toFloat() * s * 0.04f
            val flag = Path().apply {
                moveTo(x, c.y - s * tall - s * 0.8f)
                lineTo(x + s * 0.3f, c.y - s * tall - s * 0.72f + wave)
                lineTo(x, c.y - s * tall - s * 0.62f)
                close()
            }
            drawPath(flag, Color(0xFFFFD54F))
        }
        drawArc(Color(0xFF7E57C2), 180f, 180f, true, Offset(c.x - s * 0.2f, c.y - s * 0.1f), Size(s * 0.4f, s * 0.4f))
        drawRect(Color(0xFF7E57C2), Offset(c.x - s * 0.2f, c.y + s * 0.1f), Size(s * 0.4f, s * 0.3f))
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

/** Soft drifting mist over a land that isn't open yet. */
private fun DrawScope.mist(land: Land, slow: Float) {
    val c = at(land)
    val s = u * 0.07f
    val drift = sin(slow * 2 * PI + land.ordinal).toFloat() * s * 0.25f
    listOf(-1.0f to 0.1f, -0.3f to -0.4f, 0.5f to -0.2f, 1.0f to 0.25f, 0f to 0.45f, -0.6f to 0.6f, 0.6f to 0.7f, 0f to -0.9f).forEachIndexed { i, (dx, dy) ->
        val r = s * (0.75f + 0.15f * (i % 3))
        drawCircle(Color(0xFFECEFF1).copy(alpha = 0.62f), r, Offset(c.x + dx * s + drift * (if (i % 2 == 0) 1 else -1), c.y + dy * s))
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
