package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.SetPiece
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the picture for a story scene behind the characters: a river, a locked door,
 * a tree with something stuck in it... When [solved] turns true the picture changes to
 * match (stepping stones appear, the door swings open, the chest pops open).
 * Pictures sit in the upper part so the characters can stand in front at the bottom.
 */
@Composable
fun SetPieceView(piece: SetPiece, prop: String?, solved: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "piece")
    val loop by t.loop(0f, 1f, 4000, mode = RepeatMode.Restart, easing = LinearEasing, label = "loop")
    val bob by t.loop(0f, 1f, 1200, label = "bob")
    val done by animateFloatAsState(if (solved) 1f else 0f, tween(motionMs(MaxMotionMs)), label = "solved")

    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            when (piece) {
                SetPiece.WINDY_SKY -> windy(loop)
                SetPiece.FORK_PATH -> forkPath(done)
                SetPiece.RIVER -> river(loop, done)
                SetPiece.BROKEN_BRIDGE -> bridge(done)
                SetPiece.LOCKED_DOOR -> door(done)
                SetPiece.TALL_TREE -> tree()
                SetPiece.DARK_CAVE -> cave(done, loop)
                SetPiece.BIG_HILL -> hill(loop)
                SetPiece.PARTY -> party(done, bob)
                SetPiece.TREASURE -> chest(done)
                SetPiece.NIGHT_SKY -> night(loop)
                SetPiece.PICNIC -> picnic()
                SetPiece.GARDEN -> garden()
            }
        }
        val fs = (min(w.value, h.value) * 0.16f).coerceIn(22f, 64f)
        // The prop: where it sits depends on the picture, and a few move when solved.
        if (prop != null) {
            val (px, py) = when (piece) {
                SetPiece.WINDY_SKY -> (0.7f + 0.04f * sin(loop * 2 * PI).toFloat()) to (0.08f + 0.05f * bob)
                SetPiece.TALL_TREE -> 0.56f to 0.06f
                SetPiece.NIGHT_SKY -> 0.5f to (0.5f - 0.4f * done)
                SetPiece.PARTY -> 0.48f to 0.33f
                SetPiece.TREASURE -> 0.16f to 0.38f
                SetPiece.LOCKED_DOOR -> 0.18f to 0.36f
                SetPiece.DARK_CAVE -> 0.48f to 0.48f
                SetPiece.PICNIC -> 0.47f to 0.52f
                SetPiece.GARDEN -> 0.7f to (0.3f + 0.04f * bob)
                SetPiece.BIG_HILL -> 0.6f to (0.05f + 0.04f * bob)
                SetPiece.RIVER -> 0.75f to 0.28f
                else -> 0.5f to (0.12f + 0.04f * bob)
            }
            Text(prop, fontSize = fs.sp, modifier = Modifier.offset(x = w * px - (fs / 2).dp, y = h * py))
        }
        // Little sparkles to celebrate a solved obstacle.
        if (done > 0.05f) {
            listOf(0.22f to 0.18f, 0.78f to 0.14f, 0.5f to 0.04f, 0.35f to 0.4f, 0.68f to 0.36f).forEachIndexed { i, (x, y) ->
                val a = ((sin((loop + i * 0.2f) * 2 * PI) + 1) / 2).toFloat()
                Text(
                    "✨",
                    fontSize = (fs * 0.55f).sp,
                    modifier = Modifier.offset(x = w * x, y = h * y).alpha(done * (0.3f + 0.7f * a)),
                )
            }
        }
        if (piece == SetPiece.TREASURE && done > 0.5f) {
            Text("💎", fontSize = (fs * 0.8f).sp, modifier = Modifier.offset(x = w * 0.46f, y = h * (0.26f - 0.04f * bob)).alpha(done))
        }
        if (piece == SetPiece.PICNIC && prop != null) {
            Text("🍓", fontSize = (fs * 0.6f).sp, modifier = Modifier.offset(x = w * 0.36f, y = h * 0.58f).alpha(done))
            Text("🍎", fontSize = (fs * 0.6f).sp, modifier = Modifier.offset(x = w * 0.58f, y = h * 0.58f).alpha(done))
        }
    }
}

private val Brown = Color(0xFF8D6E63)
private val DarkBrown = Color(0xFF5D4037)
private val Gold = Color(0xFFFFC107)
private val Leaf = Color(0xFF66BB6A)
private val DarkLeaf = Color(0xFF43A047)

private fun DrawScope.px(fx: Float, fy: Float) = Offset(size.width * fx, size.height * fy)
private fun DrawScope.sz(fw: Float, fh: Float) = Size(size.width * fw, size.height * fh)
private val DrawScope.m get() = size.minDimension

private fun DrawScope.poly(color: Color, vararg pts: Pair<Float, Float>) {
    val p = Path()
    pts.forEachIndexed { i, (x, y) ->
        val q = px(x, y)
        if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y)
    }
    p.close()
    paperPath(p, color)
}

private fun DrawScope.windy(loop: Float) {
    // Swooshing wind lines drifting across the sky.
    listOf(0.12f, 0.24f, 0.36f).forEachIndexed { i, y ->
        val shift = ((loop + i * 0.33f) % 1f) * 1.4f - 0.4f
        val p = Path().apply {
            val a = px(shift, y)
            moveTo(a.x, a.y)
            val c1 = px(shift + 0.12f, y - 0.06f)
            val c2 = px(shift + 0.22f, y + 0.06f)
            val b = px(shift + 0.34f, y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
        }
        drawPath(p, Color.White.copy(alpha = 0.85f), style = Stroke(m * 0.018f, cap = StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = 0.85f), m * 0.025f, px(shift + 0.34f, y - 0.03f), style = Stroke(m * 0.014f))
    }
}

private fun DrawScope.forkPath(done: Float) {
    val sand = Color(0xFFE6C79C)
    listOf(0.15f, 0.5f, 0.85f).forEach { x ->
        poly(sand, 0.42f to 1f, 0.58f to 1f, (x + 0.04f) to 0.5f, (x - 0.04f) to 0.5f)
    }
    // The right path glows once the puzzle is solved.
    if (done > 0f) poly(Color(0xFFFFF59D).copy(alpha = 0.8f * done), 0.46f to 1f, 0.54f to 1f, 0.52f to 0.5f, 0.48f to 0.5f)
    // Signpost.
    paperRect(DarkBrown, px(0.485f, 0.18f), sz(0.03f, 0.42f))
    poly(Brown, 0.5f to 0.2f, 0.66f to 0.2f, 0.7f to 0.25f, 0.66f to 0.3f, 0.5f to 0.3f)
    poly(Brown, 0.5f to 0.32f, 0.34f to 0.32f, 0.3f to 0.37f, 0.34f to 0.42f, 0.5f to 0.42f)
    paperCircle(Color.White, m * 0.012f, px(0.6f, 0.25f))
    paperCircle(Color.White, m * 0.012f, px(0.4f, 0.37f))
}

private fun DrawScope.river(loop: Float, done: Float) {
    paperRect(Color(0xFF4FC3F7), px(0f, 0.42f), sz(1f, 0.24f))
    paperRect(Color(0xFF81D4FA), px(0f, 0.42f), sz(1f, 0.03f))
    // Little moving waves.
    for (row in 0..2) {
        val y = 0.48f + row * 0.06f
        for (k in 0..5) {
            val x = ((k / 6f + loop * (0.3f + row * 0.1f)) % 1f)
            drawArc(Color.White.copy(alpha = 0.7f), 200f, 140f, false, px(x, y), sz(0.06f, 0.04f), style = Stroke(m * 0.01f, cap = StrokeCap.Round))
        }
    }
    // Stepping stones rise up when solved; one is always there.
    listOf(0.15f, 0.35f, 0.55f, 0.75f).forEachIndexed { i, x ->
        val a = if (i == 0) 1f else done
        if (a > 0f) {
            paperOval(Color(0xFF9E9E9E).copy(alpha = a), px(x, 0.53f), sz(0.13f, 0.08f))
            paperOval(Color(0xFFBDBDBD).copy(alpha = a), px(x + 0.015f, 0.535f), sz(0.09f, 0.04f))
        }
    }
}

private fun DrawScope.bridge(done: Float) {
    // A deep gap between two cliffs.
    paperRect(Color(0xFF90A4AE), px(0.25f, 0.45f), sz(0.5f, 0.55f))
    poly(Color(0xFFA1887F), 0f to 0.42f, 0.3f to 0.42f, 0.26f to 1f, 0f to 1f)
    poly(Color(0xFFA1887F), 1f to 0.42f, 0.7f to 0.42f, 0.74f to 1f, 1f to 1f)
    drawLine(DarkBrown, px(0.3f, 0.36f), px(0.7f, 0.36f), m * 0.012f)
    drawLine(DarkBrown, px(0.3f, 0.36f), px(0.3f, 0.43f), m * 0.015f)
    drawLine(DarkBrown, px(0.7f, 0.36f), px(0.7f, 0.43f), m * 0.015f)
    val missing = setOf(2, 4, 5)
    for (i in 0 until 8) {
        val a = if (i in missing) done else 1f
        if (a > 0f) {
            val x = 0.3f + i * 0.05f
            paperRect(Brown.copy(alpha = a), px(x + 0.004f, 0.42f), sz(0.042f, 0.035f))
        }
    }
}

private fun DrawScope.door(done: Float) {
    // A little house wall with a big arched door.
    paperRect(Color(0xFFFFE0B2), px(0.25f, 0.1f), sz(0.5f, 0.6f))
    poly(Color(0xFFE57373), 0.2f to 0.12f, 0.5f to -0.08f, 0.8f to 0.12f)
    val doorTop = 0.24f
    // Warm light inside once open.
    paperRect(Color(0xFFFFF176), px(0.38f, doorTop), sz(0.24f, 0.46f))
    val openW = 0.24f * (1f - 0.7f * done)
    paperRoundRect(Brown, px(0.38f, doorTop), sz(openW, 0.46f), CornerRadius(m * 0.03f))
    for (k in 1..2) {
        val x = 0.38f + openW * k / 3f
        drawLine(DarkBrown, px(x, doorTop + 0.02f), px(x, doorTop + 0.44f), m * 0.006f)
    }
    paperCircle(Gold, m * 0.018f, px(0.38f + openW * 0.85f, 0.48f))
    // The padlock disappears when solved.
    if (done < 1f) {
        val a = 1f - done
        drawArc(Color(0xFF9E9E9E).copy(alpha = a), 180f, 180f, false, px(0.505f, 0.38f), sz(0.07f, 0.09f), style = Stroke(m * 0.018f))
        paperRoundRect(Gold.copy(alpha = a), px(0.5f, 0.42f), sz(0.08f, 0.08f), CornerRadius(m * 0.012f))
        paperCircle(DarkBrown.copy(alpha = a), m * 0.01f, px(0.54f, 0.455f))
    }
}

private fun DrawScope.tree() {
    paperRect(Brown, px(0.47f, 0.22f), sz(0.08f, 0.5f))
    drawLine(Brown, px(0.51f, 0.4f), px(0.66f, 0.3f), m * 0.03f, cap = StrokeCap.Round)
    listOf(0.51f to 0.12f, 0.38f to 0.2f, 0.64f to 0.2f, 0.45f to 0.05f, 0.58f to 0.04f, 0.51f to 0.25f).forEachIndexed { i, (x, y) ->
        paperCircle(if (i % 2 == 0) Leaf else DarkLeaf, m * 0.13f, px(x, y))
    }
    paperCircle(Color(0xFFE84A7F), m * 0.015f, px(0.4f, 0.22f))
    paperCircle(Color(0xFFE84A7F), m * 0.015f, px(0.62f, 0.15f))
}

private fun DrawScope.cave(done: Float, loop: Float) {
    paperOval(Color(0xFF8D8D8D), px(0.12f, 0.1f), sz(0.76f, 0.8f))
    paperOval(Color(0xFF757575), px(0.2f, 0.18f), sz(0.6f, 0.64f))
    // The dark opening glows warm once solved.
    paperArc(lerp(Color(0xFF1A1A2E), Color(0xFFFFB74D), done * 0.7f), 180f, 180f, true, px(0.34f, 0.34f), sz(0.32f, 0.36f))
    paperRect(lerp(Color(0xFF1A1A2E), Color(0xFFFFB74D), done * 0.7f), px(0.34f, 0.52f), sz(0.32f, 0.2f))
    listOf(0.38f, 0.45f, 0.52f, 0.59f).forEach { x ->
        poly(Color(0xFF616161), x to 0.4f, (x + 0.04f) to 0.4f, (x + 0.02f) to 0.47f)
    }
    listOf(0.25f to 0.3f, 0.72f to 0.26f, 0.68f to 0.6f).forEachIndexed { i, (x, y) ->
        val a = ((sin((loop + i * 0.3f) * 2 * PI) + 1) / 2).toFloat()
        star(px(x, y), m * 0.03f, Color(0xFFB3E5FC).copy(alpha = 0.4f + 0.6f * a))
    }
}

private fun DrawScope.hill(loop: Float) {
    paperOval(Color(0xFF7CB342), px(-0.1f, 0.25f), sz(1.2f, 1.2f))
    // A winding path up to the top.
    val p = Path().apply {
        val a = px(0.45f, 1f)
        moveTo(a.x, a.y)
        val c1 = px(0.75f, 0.75f)
        val c2 = px(0.25f, 0.5f)
        val b = px(0.5f, 0.27f)
        cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
    drawPath(p, Color(0xFFE6C79C), style = Stroke(m * 0.05f, cap = StrokeCap.Round))
    drawLine(DarkBrown, px(0.5f, 0.27f), px(0.5f, 0.08f), m * 0.012f)
    val wave = 0.02f * sin(loop * 2 * PI * 2).toFloat()
    poly(Color(0xFFE84A7F), 0.5f to 0.08f, 0.62f to (0.12f + wave), 0.5f to 0.16f)
}

private fun DrawScope.party(done: Float, bob: Float) {
    // Table.
    paperRect(Color(0xFFFF8A80), px(0.3f, 0.48f), sz(0.4f, 0.04f))
    paperRect(Color(0xFFFFCDD2), px(0.3f, 0.52f), sz(0.4f, 0.08f))
    paperRect(DarkBrown, px(0.33f, 0.6f), sz(0.02f, 0.14f))
    paperRect(DarkBrown, px(0.65f, 0.6f), sz(0.02f, 0.14f))
    // Bunting and balloons arrive once the party is ready.
    if (done > 0f) {
        val colors = listOf(0xFFE84A7F, 0xFFFFEB3B, 0xFF43A047, 0xFF1E88E5, 0xFFAB47BC)
        drawLine(DarkBrown.copy(alpha = done), px(0.02f, 0.04f), px(0.98f, 0.04f), m * 0.006f)
        for (i in 0 until 10) {
            val x = 0.04f + i * 0.095f
            poly(Color(colors[i % colors.size]).copy(alpha = done), x to 0.04f, (x + 0.07f) to 0.04f, (x + 0.035f) to 0.12f)
        }
        listOf(0.12f to 0xFFE84A7F, 0.2f to 0xFF1E88E5, 0.8f to 0xFFFFEB3B, 0.88f to 0xFF43A047).forEachIndexed { i, (x, c) ->
            val y = 0.16f + (if (i % 2 == 0) bob else 1 - bob) * 0.03f
            drawLine(Color.Gray.copy(alpha = done), px(x, y + 0.12f), px(x, 0.6f), m * 0.004f)
            paperOval(Color(c).copy(alpha = done), px(x - 0.04f, y), sz(0.08f, 0.13f))
        }
    }
}

private fun DrawScope.chest(done: Float) {
    val wood = Color(0xFFA1662F)
    paperRoundRect(wood, px(0.36f, 0.46f), sz(0.28f, 0.2f), CornerRadius(m * 0.02f))
    paperRect(Gold, px(0.36f, 0.52f), sz(0.28f, 0.025f))
    paperRect(Gold, px(0.48f, 0.46f), sz(0.04f, 0.2f))
    // The lid lifts open; gold glows from inside.
    if (done > 0f) paperOval(Gold.copy(alpha = 0.7f * done), px(0.38f, 0.4f), sz(0.24f, 0.1f))
    val lidY = 0.38f - 0.12f * done
    paperRoundRect(lerp(wood, Color(0xFF8D5524), done), px(0.36f, lidY), sz(0.28f, 0.09f), CornerRadius(m * 0.03f))
    paperRect(Gold, px(0.48f, lidY), sz(0.04f, 0.09f))
    if (done < 0.5f) paperCircle(Color(0xFF424242), m * 0.012f, px(0.5f, 0.5f))
}

private fun DrawScope.night(loop: Float) {
    // The sky itself goes dark behind the whole screen (Scenery(night = true)); here the moon
    // and stars. The crescent is cut out of the moon, so no dark disc shows against the sky.
    val c = px(0.82f, 0.14f)
    val bite = px(0.86f, 0.12f)
    val moon = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, m * 0.09f)) }
    val cut = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(bite, m * 0.08f)) }
    paperPath(androidx.compose.ui.graphics.Path().apply { op(moon, cut, androidx.compose.ui.graphics.PathOperation.Difference) }, Color(0xFFFFF59D))
    listOf(0.1f to 0.1f, 0.25f to 0.22f, 0.4f to 0.08f, 0.58f to 0.2f, 0.68f to 0.06f, 0.15f to 0.34f, 0.92f to 0.34f).forEachIndexed { i, (x, y) ->
        val a = ((sin((loop * 2 + i * 0.37f) * 2 * PI) + 1) / 2).toFloat()
        star(px(x, y), m * (0.018f + 0.012f * a), Color.White.copy(alpha = 0.5f + 0.5f * a))
    }
}

private fun DrawScope.picnic() {
    // A red and white checked blanket, in a little perspective.
    val rows = 4
    val cols = 6
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val t0 = r / rows.toFloat()
            val t1 = (r + 1) / rows.toFloat()
            val y0 = 0.52f + 0.22f * t0
            val y1 = 0.52f + 0.22f * t1
            val l0 = 0.3f - 0.08f * t0
            val r0 = 0.7f + 0.08f * t0
            val l1 = 0.3f - 0.08f * t1
            val r1 = 0.7f + 0.08f * t1
            val x00 = l0 + (r0 - l0) * c / cols
            val x01 = l0 + (r0 - l0) * (c + 1) / cols
            val x10 = l1 + (r1 - l1) * c / cols
            val x11 = l1 + (r1 - l1) * (c + 1) / cols
            poly(if ((r + c) % 2 == 0) Color(0xFFE84A7F) else Color.White, x00 to y0, x01 to y0, x11 to y1, x10 to y1)
        }
    }
}

private fun DrawScope.garden() {
    // A white picket fence and rows of flowers.
    paperRect(Color.White, px(0f, 0.36f), sz(1f, 0.02f))
    paperRect(Color.White, px(0f, 0.46f), sz(1f, 0.02f))
    for (i in 0..11) {
        val x = i / 11f - 0.02f
        poly(Color.White, x to 0.52f, (x + 0.04f) to 0.52f, (x + 0.04f) to 0.32f, (x + 0.02f) to 0.28f, x to 0.32f)
    }
    val petals = listOf(0xFFFF80AB, 0xFFFFEB3B, 0xFFB388FF, 0xFFFF8A65, 0xFF80D8FF)
    for (i in 0..8) {
        val x = 0.06f + i * 0.11f
        val y = 0.56f + (i % 2) * 0.05f
        drawLine(DarkLeaf, px(x, y), px(x, y + 0.1f), m * 0.01f)
        val c = Color(petals[i % petals.size])
        listOf(-1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f).forEach { (dx, dy) ->
            paperCircle(c, m * 0.022f, Offset(size.width * x + dx * m * 0.025f, size.height * y + dy * m * 0.025f))
        }
        paperCircle(Gold, m * 0.016f, px(x, y))
    }
}
