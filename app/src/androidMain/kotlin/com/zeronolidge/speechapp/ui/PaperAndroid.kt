package com.zeronolidge.speechapp.ui.art

import android.graphics.DiscretePathEffect
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Android draws paper edges with DiscretePathEffect, which roughens any shape and is
 * deterministic for the same shape, so edges don't flicker while things animate.
 */
private object PaperInk {
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    val shadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
        color = PaperShadowColor.toArgb()
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
}

internal actual fun DrawScope.drawPaper(color: Color, extent: Float, shape: PaperShape, shadow: Boolean) = drawIntoCanvas { c ->
    val canvas = c.nativeCanvas
    val effect = PaperInk.effect(extent)

    fun draw(paint: android.graphics.Paint, d: Float) {
        val dx = d * 0.6f
        when (shape) {
            is PaperShape.Circle -> canvas.drawCircle(shape.center.x + dx, shape.center.y + d, shape.radius, paint)
            is PaperShape.Oval -> {
                PaperInk.rect.set(shape.topLeft.x + dx, shape.topLeft.y + d, shape.topLeft.x + shape.size.width + dx, shape.topLeft.y + shape.size.height + d)
                canvas.drawOval(PaperInk.rect, paint)
            }
            is PaperShape.Rect ->
                canvas.drawRect(shape.topLeft.x + dx, shape.topLeft.y + d, shape.topLeft.x + shape.size.width + dx, shape.topLeft.y + shape.size.height + d, paint)
            is PaperShape.RoundRect -> {
                PaperInk.rect.set(shape.topLeft.x + dx, shape.topLeft.y + d, shape.topLeft.x + shape.size.width + dx, shape.topLeft.y + shape.size.height + d)
                canvas.drawRoundRect(PaperInk.rect, shape.corner.x, shape.corner.y, paint)
            }
            is PaperShape.Arc -> {
                PaperInk.rect.set(shape.topLeft.x + dx, shape.topLeft.y + d, shape.topLeft.x + shape.size.width + dx, shape.topLeft.y + shape.size.height + d)
                canvas.drawArc(PaperInk.rect, shape.start, shape.sweep, shape.useCenter, paint)
            }
            is PaperShape.Shape -> {
                val ap = shape.path.asAndroidPath()
                if (d == 0f) canvas.drawPath(ap, paint) else {
                    canvas.save()
                    canvas.translate(dx, d)
                    canvas.drawPath(ap, paint)
                    canvas.restore()
                }
            }
        }
    }

    // Only solid paper casts a shadow; see-through things (mist, glows) don't.
    if (shadow && color.alpha > 0.9f) {
        PaperInk.shadow.pathEffect = effect
        draw(PaperInk.shadow, paperShadowOffset(extent))
    }
    PaperInk.fill.pathEffect = effect
    PaperInk.fill.color = color.toArgb()
    draw(PaperInk.fill, 0f)
}
