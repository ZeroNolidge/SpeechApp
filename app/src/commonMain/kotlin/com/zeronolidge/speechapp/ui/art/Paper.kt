package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * The app's look: everything is cut from coloured paper. Shapes get slightly rough, torn
 * edges and a soft shadow underneath, as if layers of construction paper were stacked up.
 *
 * These mirror Compose's drawCircle / drawOval / drawRect / drawRoundRect / drawPath so art
 * code reads the same. Each platform draws the rough edges its own way (see [drawPaper]);
 * the roughness is deterministic for the same shape, so edges don't flicker while animating.
 */
sealed interface PaperShape {
    data class Circle(val center: Offset, val radius: Float) : PaperShape
    data class Oval(val topLeft: Offset, val size: Size) : PaperShape
    data class Rect(val topLeft: Offset, val size: Size) : PaperShape
    data class RoundRect(val topLeft: Offset, val size: Size, val corner: CornerRadius) : PaperShape
    data class Arc(val topLeft: Offset, val size: Size, val start: Float, val sweep: Float, val useCenter: Boolean) : PaperShape
    class Shape(val path: Path) : PaperShape
}

/** Draws [shape] as paper: a soft shadow (for solid colours) and then a rough-edged fill. [shadow] false skips the built-in soft shadow (for layered cut-outs). */
internal expect fun DrawScope.drawPaper(color: Color, extent: Float, shape: PaperShape, shadow: Boolean = true)

/** How far a paper layer's shadow falls, for a shape of this size. */
internal fun paperShadowOffset(extent: Float) = (extent * 0.05f).coerceIn(1.5f, 7f)

internal val PaperShadowColor = Color(0x30000000)

fun DrawScope.paperCircle(color: Color, radius: Float, center: Offset) =
    drawPaper(color, radius * 2, PaperShape.Circle(center, radius))

fun DrawScope.paperOval(color: Color, topLeft: Offset, size: Size) =
    drawPaper(color, minOf(size.width, size.height), PaperShape.Oval(topLeft, size))

fun DrawScope.paperRect(color: Color, topLeft: Offset, size: Size) =
    drawPaper(color, minOf(size.width, size.height).coerceAtLeast(20f), PaperShape.Rect(topLeft, size))

fun DrawScope.paperRoundRect(color: Color, topLeft: Offset, size: Size, cornerRadius: CornerRadius = CornerRadius.Zero) =
    drawPaper(color, minOf(size.width, size.height), PaperShape.RoundRect(topLeft, size, cornerRadius))

fun DrawScope.paperArc(color: Color, startAngle: Float, sweepAngle: Float, useCenter: Boolean, topLeft: Offset, size: Size) =
    drawPaper(color, minOf(size.width, size.height), PaperShape.Arc(topLeft, size, startAngle, sweepAngle, useCenter))

fun DrawScope.paperPath(path: Path, color: Color) {
    val b = path.getBounds()
    drawPaper(color, minOf(b.width, b.height).coerceAtLeast(16f), PaperShape.Shape(path))
}

/**
 * Makes a button, card or panel look like a piece of paper: rough edges and a soft shadow.
 * Put it before `clip(...)` so the shadow isn't cut off.
 */
fun Modifier.paperCard(color: Color, corner: Dp = 22.dp, circle: Boolean = false): Modifier = drawBehind {
    if (circle) {
        paperCircle(color, size.minDimension / 2, center)
    } else {
        val r = corner.toPx()
        paperRoundRect(color, Offset.Zero, size, CornerRadius(r, r))
    }
}

/**
 * One look for the whole app. Anything the child can tap is a thick, raised cut-out with a clear
 * drop shadow; scenery sits flat and a little muted behind it, so it is obvious what to tap.
 */
object PaperStyle {
    /** How far a tappable cut-out's drop shadow falls. */
    val lift = 7.dp
    /** How thick the card stock looks along a tappable cut-out's bottom edge. */
    val edge = 5.dp
    val corner = 26.dp
    val shadow = Color(0x45000000)
    /** Laid over scenery so it sits back and tappable things stand out. */
    val sceneryWash = Color(0x4DF5E6C8)
    /** Text and icons: dark ink on plain cream paper, with no texture over them. */
    val chip = Color(0xFFFFFAF0)
    val ink = Color(0xFF2E2140)
}

private fun DrawScope.paperLayer(color: Color, corner: Float, circle: Boolean, dx: Float, dy: Float) = translate(dx, dy) {
    if (circle) drawPaper(color, size.minDimension, PaperShape.Circle(center, size.minDimension / 2), shadow = false)
    else drawPaper(color, minOf(size.width, size.height), PaperShape.RoundRect(Offset.Zero, size, CornerRadius(corner, corner)), shadow = false)
}

/**
 * The drop shadow and card-stock edge under a raised cut-out. [press] (0 to 1) squashes the
 * cut-out down onto the table while a finger is on it.
 */
fun Modifier.raisedPaperBase(color: Color, corner: Dp = PaperStyle.corner, circle: Boolean = false, press: () -> Float = { 0f }): Modifier = drawBehind {
    val lift = PaperStyle.lift.toPx()
    val edge = PaperStyle.edge.toPx()
    val p = press()
    val r = corner.toPx()
    paperLayer(PaperStyle.shadow, r, circle, lift * 0.45f * (1f - p), edge + lift * (1f - 0.7f * p))
    paperLayer(lerp(color, Color.Black, 0.28f), r, circle, 0f, edge)
}

/** The top face of a raised cut-out. */
fun Modifier.raisedPaperFace(color: Color, corner: Dp = PaperStyle.corner, circle: Boolean = false): Modifier = drawBehind {
    paperLayer(color, corner.toPx(), circle, 0f, 0f)
}

/** Scenery sits back: a soft wash and paper grain over the background, but never over text or buttons. */
@Composable
fun SceneryWash(modifier: Modifier = Modifier.fillMaxSize(), overflow: Float = 0f) {
    Canvas(modifier) {
        drawRect(PaperStyle.sceneryWash, Offset(-overflow, -overflow), Size(size.width + overflow * 2, size.height + overflow * 2))
    }
    PaperGrain(modifier, overflow)
}

/** A fine paper-grain texture for backgrounds, so the app feels like paper. */
@Composable
fun PaperGrain(modifier: Modifier = Modifier.fillMaxSize(), overflow: Float = 0f) {
    val brush = remember {
        val n = 160
        val rnd = Random(7)
        val bmp = ImageBitmap(n, n)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        val paint = Paint()
        for (y in 0 until n) {
            for (x in 0 until n) {
                val v = rnd.nextInt(256)
                paint.color = when {
                    v < 18 -> Color(60, 40, 20, 26) // dark fleck
                    v > 236 -> Color(255, 255, 255, 34) // light fleck
                    v % 3 == 0 -> Color(90, 70, 40, rnd.nextInt(6))
                    else -> continue
                }
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
        ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
    }
    Canvas(modifier) { drawRect(brush, Offset(-overflow, -overflow), Size(size.width + overflow * 2, size.height + overflow * 2)) }
}

/** The warm, bright paper colours used across the app. */
object PaperColors {
    val kraft = Color(0xFFF3E3C3)
    val cream = Color(0xFFFFF7E6)
    val tomato = Color(0xFFF2643D)
    val sunshine = Color(0xFFFFC63A)
    val grass = Color(0xFF7CC242)
    val leaf = Color(0xFF3E9B4F)
    val sky = Color(0xFF5BC0EB)
    val ocean = Color(0xFF2E8FD6)
    val berry = Color(0xFFE84A7F)
    val grape = Color(0xFF8E5CC9)
    val tangerine = Color(0xFFFF9636)
    val ink = Color(0xFF3B2A4A)
}
