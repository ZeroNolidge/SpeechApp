package com.zeronolidge.speechapp.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The browser has no DiscretePathEffect, so the web version builds the rough paper edge
 * itself: it walks around the outline and nudges each point in or out by a small amount.
 * The nudges depend only on the point's index and the shape's size (not its position),
 * so a shape that moves keeps the same torn edge instead of flickering.
 */
internal actual fun DrawScope.drawPaper(color: Color, extent: Float, shape: PaperShape, shadow: Boolean) {
    val rough = extent >= 14f
    val seg = (extent * 0.16f).coerceIn(5f, 26f)
    val outline: Path = when (shape) {
        is PaperShape.Shape -> shape.path
        else -> OutlineCache.get(shape, extent) { roughOutline(outline(shape, seg), if (rough) seg * 0.16f else 0f, extent) }
    }
    // Only solid paper casts a shadow; see-through things (mist, glows) don't.
    if (shadow && PaperPen.shapeShadows && color.alpha > 0.9f) {
        val d = paperShadowOffset(extent)
        translate(d * 0.6f, d) { drawPath(outline, PaperShadowColor) }
    }
    drawPath(outline, color)
    // The thin white hand-cut edge, while baking a layer.
    if (PaperPen.edge > 0f && color.alpha > 0.9f && extent >= PaperPen.edgeMin) {
        drawPath(outline, EdgeColor, style = Stroke(PaperPen.edge, join = StrokeJoin.Round))
    }
}

private val EdgeColor = Color(0xE1FFFFFF)

/** Rough outlines are worked out once per shape and reused, so redrawing is cheap. */
private object OutlineCache {
    private val paths = HashMap<Pair<PaperShape, Float>, Path>()

    fun get(shape: PaperShape, extent: Float, make: () -> Path): Path {
        val key = shape to extent
        paths[key]?.let { return it }
        if (paths.size > 800) paths.clear()
        return make().also { paths[key] = it }
    }
}

/** Points around the shape's edge, roughly [seg] apart. */
private fun outline(shape: PaperShape, seg: Float): List<Offset> = when (shape) {
    is PaperShape.Circle -> ellipse(shape.center, shape.radius, shape.radius, 0f, 360f, seg)
    is PaperShape.Oval -> ellipse(
        Offset(shape.topLeft.x + shape.size.width / 2, shape.topLeft.y + shape.size.height / 2),
        shape.size.width / 2, shape.size.height / 2, 0f, 360f, seg,
    )
    is PaperShape.Rect -> roundRect(shape.topLeft.x, shape.topLeft.y, shape.size.width, shape.size.height, 0f, 0f, seg)
    is PaperShape.RoundRect -> roundRect(
        shape.topLeft.x, shape.topLeft.y, shape.size.width, shape.size.height, shape.corner.x, shape.corner.y, seg,
    )
    is PaperShape.Arc -> {
        val c = Offset(shape.topLeft.x + shape.size.width / 2, shape.topLeft.y + shape.size.height / 2)
        val pts = ellipse(c, shape.size.width / 2, shape.size.height / 2, shape.start, shape.sweep, seg)
        if (shape.useCenter) pts + c else pts
    }
    is PaperShape.Shape -> emptyList()
}

private fun ellipse(c: Offset, rx: Float, ry: Float, startDeg: Float, sweepDeg: Float, seg: Float): List<Offset> {
    val arcLen = (abs(sweepDeg) / 360f) * 2f * PI.toFloat() * sqrt((rx * rx + ry * ry) / 2f)
    val n = (arcLen / seg).toInt().coerceIn(8, 96)
    return (0..n).map { i ->
        val a = (startDeg + sweepDeg * i / n) * PI / 180.0
        Offset(c.x + (rx * cos(a)).toFloat(), c.y + (ry * sin(a)).toFloat())
    }
}

private fun roundRect(x: Float, y: Float, w: Float, h: Float, rxIn: Float, ryIn: Float, seg: Float): List<Offset> {
    val rx = rxIn.coerceIn(0f, w / 2)
    val ry = ryIn.coerceIn(0f, h / 2)
    val pts = mutableListOf<Offset>()
    fun line(a: Offset, b: Offset) {
        val len = sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
        val n = (len / seg).toInt().coerceAtLeast(1)
        for (i in 0 until n) pts += Offset(a.x + (b.x - a.x) * i / n, a.y + (b.y - a.y) * i / n)
    }
    fun corner(cx: Float, cy: Float, start: Float) {
        if (rx <= 0f || ry <= 0f) { pts += Offset(cx, cy); return }
        pts += ellipse(Offset(cx, cy), rx, ry, start, 90f, seg).dropLast(1)
    }
    line(Offset(x + rx, y), Offset(x + w - rx, y))
    corner(x + w - rx, y + ry, 270f)
    line(Offset(x + w, y + ry), Offset(x + w, y + h - ry))
    corner(x + w - rx, y + h - ry, 0f)
    line(Offset(x + w - rx, y + h), Offset(x + rx, y + h))
    corner(x + rx, y + h - ry, 90f)
    line(Offset(x, y + h - ry), Offset(x, y + ry))
    corner(x + rx, y + ry, 180f)
    return pts
}

/** Joins the points into a closed path, nudging each one by up to [amount] pixels. */
private fun roughOutline(pts: List<Offset>, amount: Float, extent: Float): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    val seed = (extent / 4f).toInt()
    pts.forEachIndexed { i, p ->
        val jx = if (amount > 0f) noise(i, seed) * amount else 0f
        val jy = if (amount > 0f) noise(i + 7919, seed) * amount else 0f
        if (i == 0) path.moveTo(p.x + jx, p.y + jy) else path.lineTo(p.x + jx, p.y + jy)
    }
    path.close()
    return path
}

/** A repeatable pseudo-random number between -1 and 1. */
private fun noise(i: Int, seed: Int): Float {
    var h = i * 374761393 + seed * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return ((h and 0xFFFF) / 32767.5f) - 1f
}
