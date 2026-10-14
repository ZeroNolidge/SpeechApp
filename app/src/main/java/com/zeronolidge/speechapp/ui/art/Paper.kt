package com.zeronolidge.speechapp.ui.art

import android.graphics.Bitmap
import android.graphics.DiscretePathEffect
import android.graphics.RectF
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
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * The app's look: everything is cut from coloured paper. Shapes get slightly rough, torn
 * edges and a soft shadow underneath, as if layers of construction paper were stacked up.
 *
 * These mirror Compose's drawCircle / drawOval / drawRect / drawRoundRect / drawPath so art
 * code reads the same. The roughness comes from Android's DiscretePathEffect, which is
 * deterministic for the same shape, so edges don't flicker while things animate.
 */
private object PaperInk {
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    val shadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
        color = 0x30000000
    }
    private val effects = HashMap<Int, android.graphics.PathEffect>()
    val rect = RectF()

    /** Rougher edges on big shapes, smooth edges on tiny ones like pupils. */
    fun effect(extent: Float): android.graphics.PathEffect? {
        if (extent < 14f) return null
        val seg = (extent * 0.16f).coerceIn(5f, 26f)
        val key = seg.toInt()
        return effects.getOrPut(key) { DiscretePathEffect(key.toFloat(), key * 0.16f) }
    }

    fun shadowOffset(extent: Float) = (extent * 0.05f).coerceIn(1.5f, 7f)
}

private inline fun DrawScope.paper(
    color: Color,
    extent: Float,
    crossinline shape: (android.graphics.Canvas, android.graphics.Paint, Float) -> Unit,
) = drawIntoCanvas { c ->
    val canvas = c.nativeCanvas
    val effect = PaperInk.effect(extent)
    // Only solid paper casts a shadow; see-through things (mist, glows) don't.
    if (color.alpha > 0.9f) {
        PaperInk.shadow.pathEffect = effect
        shape(canvas, PaperInk.shadow, PaperInk.shadowOffset(extent))
    }
    PaperInk.fill.pathEffect = effect
    PaperInk.fill.color = color.toArgb()
    shape(canvas, PaperInk.fill, 0f)
}

fun DrawScope.paperCircle(color: Color, radius: Float, center: Offset) =
    paper(color, radius * 2) { c, p, d -> c.drawCircle(center.x + d * 0.6f, center.y + d, radius, p) }

fun DrawScope.paperOval(color: Color, topLeft: Offset, size: Size) =
    paper(color, minOf(size.width, size.height)) { c, p, d ->
        PaperInk.rect.set(topLeft.x + d * 0.6f, topLeft.y + d, topLeft.x + size.width + d * 0.6f, topLeft.y + size.height + d)
        c.drawOval(PaperInk.rect, p)
    }

fun DrawScope.paperRect(color: Color, topLeft: Offset, size: Size) =
    paper(color, minOf(size.width, size.height).coerceAtLeast(20f)) { c, p, d ->
        c.drawRect(topLeft.x + d * 0.6f, topLeft.y + d, topLeft.x + size.width + d * 0.6f, topLeft.y + size.height + d, p)
    }

fun DrawScope.paperRoundRect(color: Color, topLeft: Offset, size: Size, cornerRadius: CornerRadius = CornerRadius.Zero) =
    paper(color, minOf(size.width, size.height)) { c, p, d ->
        PaperInk.rect.set(topLeft.x + d * 0.6f, topLeft.y + d, topLeft.x + size.width + d * 0.6f, topLeft.y + size.height + d)
        c.drawRoundRect(PaperInk.rect, cornerRadius.x, cornerRadius.y, p)
    }

fun DrawScope.paperArc(color: Color, startAngle: Float, sweepAngle: Float, useCenter: Boolean, topLeft: Offset, size: Size) =
    paper(color, minOf(size.width, size.height)) { c, p, d ->
        PaperInk.rect.set(topLeft.x + d * 0.6f, topLeft.y + d, topLeft.x + size.width + d * 0.6f, topLeft.y + size.height + d)
        c.drawArc(PaperInk.rect, startAngle, sweepAngle, useCenter, p)
    }

fun DrawScope.paperPath(path: Path, color: Color) {
    val bounds = path.getBounds()
    paper(color, minOf(bounds.width, bounds.height).coerceAtLeast(16f)) { c, p, d ->
        val ap = path.asAndroidPath()
        if (d == 0f) c.drawPath(ap, p) else {
            c.save()
            c.translate(d * 0.6f, d)
            c.drawPath(ap, p)
            c.restore()
        }
    }
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

/** A fine paper-grain texture laid over everything, so the whole app feels like paper. */
@Composable
fun PaperGrain(modifier: Modifier = Modifier.fillMaxSize()) {
    val brush = remember {
        val n = 160
        val rnd = Random(7)
        val pixels = IntArray(n * n) {
            val v = rnd.nextInt(256)
            when {
                v < 18 -> android.graphics.Color.argb(26, 60, 40, 20) // dark fleck
                v > 236 -> android.graphics.Color.argb(34, 255, 255, 255) // light fleck
                else -> android.graphics.Color.argb(rnd.nextInt(6), 90, 70, 40)
            }
        }
        val bmp = Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    Canvas(modifier) { drawRect(brush) }
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
