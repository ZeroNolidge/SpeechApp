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
 * Makes a button, card or panel look like a piece of paper: rough edges, a thin white cut edge
 * and a soft shadow, baked into an image once (not redrawn every frame).
 * Put it before `clip(...)` so the shadow isn't cut off.
 */
fun Modifier.paperCard(color: Color, corner: Dp = 22.dp, circle: Boolean = false): Modifier =
    this then BakedPaperElement(
        spec = listOf("card", color, corner, circle),
        depth = 3.dp,
    ) { faceShape(color, corner.toPx(), circle) }

/**
 * One look for the whole app. Anything the child can tap is a thick, raised cut-out with a clear
 * drop shadow; scenery sits flat and a little muted behind it, so it is obvious what to tap.
 */
object PaperStyle {
    /** How far a tappable cut-out's drop shadow falls: deeper than any scenery layer. */
    val lift = 8.dp
    /** How thick the card stock looks along a tappable cut-out's bottom edge. */
    val edge = 5.dp
    val corner = 26.dp
    val shadow = Color(0x45000000)
    /** Laid over scenery so it sits back and tappable things stand out. */
    val sceneryWash = Color(0x33F5E6C8)
    /** Text and icons: dark ink on plain cream paper, with no texture over them. */
    val chip = Color(0xFFFFFAF0)
    val ink = Color(0xFF2E2140)
}

/** The flat shape of a cut-out, filling the drawing area. */
private fun DrawScope.faceShape(color: Color, corner: Float, circle: Boolean) {
    if (circle) drawPaper(color, size.minDimension, PaperShape.Circle(center, size.minDimension / 2), shadow = false)
    else drawPaper(color, minOf(size.width, size.height), PaperShape.RoundRect(Offset.Zero, size, CornerRadius(corner, corner)), shadow = false)
}

/**
 * The drop shadow and card-stock edge under a raised cut-out: the deepest shadow in the app, so
 * buttons always sit on top of the scenery. [press] (0 to 1) squashes the cut-out down onto the
 * table while a finger is on it. The shape is baked once; only its position changes.
 */
fun Modifier.raisedPaperBase(color: Color, corner: Dp = PaperStyle.corner, circle: Boolean = false, press: () -> Float = { 0f }): Modifier =
    this then BakedPaperElement(
        spec = listOf("base", corner, circle),
        edge = false,
        live = { baked ->
            val lift = PaperStyle.lift.toPx()
            val edge = PaperStyle.edge.toPx()
            val p = press()
            // A soft shadow lit from the top left: a few faint copies, further and fainter.
            val dx = lift * 0.5f * (1f - p)
            val dy = edge + lift * (1f - 0.7f * p)
            listOf(1.25f to 0.10f, 1f to 0.16f, 0.75f to 0.16f).forEach { (k, a) ->
                drawBaked(baked, dx * k, dy * k, tint = Color.Black, alpha = a)
            }
            drawBaked(baked, 0f, edge, tint = lerp(color, Color.Black, 0.28f))
        },
    ) { faceShape(Color.White, corner.toPx(), circle) }

/** The top face of a raised cut-out, with its thin white hand-cut edge. */
fun Modifier.raisedPaperFace(color: Color, corner: Dp = PaperStyle.corner, circle: Boolean = false): Modifier =
    this then BakedPaperElement(spec = listOf("face", color, corner, circle)) { faceShape(color, corner.toPx(), circle) }

/** Scenery sits back: a soft wash and paper grain over the background, but never over text or buttons. */
@Composable
fun SceneryWash(modifier: Modifier = Modifier.fillMaxSize(), overflow: Float = 0f) {
    Canvas(modifier) {
        drawRect(PaperStyle.sceneryWash, Offset(-overflow, -overflow), Size(size.width + overflow * 2, size.height + overflow * 2))
    }
    PaperGrain(modifier, overflow)
}

/** A fine paper-grain texture for backgrounds, so the app feels like paper. One shared texture. */
@Composable
fun PaperGrain(modifier: Modifier = Modifier.fillMaxSize(), overflow: Float = 0f) {
    val brush = remember { PaperTexture.brush() }
    Canvas(modifier) { drawRect(brush, Offset(-overflow, -overflow), Size(size.width + overflow * 2, size.height + overflow * 2)) }
}

/** The warm, bright paper colours used across the app. */
object PaperColors {
    val kraft = Color(0xFFF3E3C3)
    val cream = Color(0xFFFFF7E6)
    /** The one warm accent, kept for things he can tap. Never used in scenery. */
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
