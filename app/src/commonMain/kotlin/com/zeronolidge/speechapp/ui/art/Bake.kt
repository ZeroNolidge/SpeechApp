package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Paper art is drawn once into an image ("baked") with its soft shadow, thin white hand-cut edge
 * and paper grain, and after that only the image is copied to the screen. Older iPads stay
 * smooth because nothing rough-edged is redrawn while things move.
 */

/** Switches the paper helpers read while a layer is being baked. */
internal object PaperPen {
    /** Width of the white hand-cut edge around each shape, in pixels (0: none). */
    var edge = 0f
    /** Shapes smaller than this (in pixels) get no white edge, e.g. pupils. */
    var edgeMin = 0f
    /** Whether each shape casts its own small shadow (off inside a layer, which has one shadow). */
    var shapeShadows = true
}

/** One shared paper-fibre texture: specks plus short fibres, tiled over large coloured shapes. */
internal object PaperTexture {
    private var cached: ShaderBrush? = null

    fun brush(): ShaderBrush = cached ?: make().also { cached = it }

    private fun make(): ShaderBrush {
        val n = 192
        val rnd = Random(7)
        val bmp = ImageBitmap(n, n)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for (y in 0 until n) {
            for (x in 0 until n) {
                val v = rnd.nextInt(256)
                paint.color = when {
                    v < 14 -> Color(60, 40, 20, 24)
                    v > 240 -> Color(255, 255, 255, 34)
                    v % 3 == 0 -> Color(90, 70, 40, rnd.nextInt(6))
                    else -> continue
                }
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
        // Paper fibres: short, faint, slightly curved strands.
        paint.strokeWidth = 1f
        paint.strokeCap = StrokeCap.Round
        repeat(90) { i ->
            val x = rnd.nextFloat() * n
            val y = rnd.nextFloat() * n
            val a = rnd.nextFloat() * 6.28f
            val len = 5f + rnd.nextFloat() * 10f
            paint.color = if (i % 3 == 0) Color(255, 255, 255, 40) else Color(80, 60, 30, 18)
            val mx = x + cos(a) * len / 2 + sin(a) * 1.5f
            val my = y + sin(a) * len / 2 - cos(a) * 1.5f
            canvas.drawLine(Offset(x, y), Offset(mx, my), paint)
            canvas.drawLine(Offset(mx, my), Offset(x + cos(a) * len, y + sin(a) * len), paint)
        }
        return ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
    }
}

/** A baked piece of paper art: the image covers the area plus [pad] on every side. */
class Baked internal constructor(val image: ImageBitmap, val pad: Float, val w: Float, val h: Float)

private val shadowTint = ColorFilter.tint(Color.Black, BlendMode.SrcIn)

/**
 * Draws [draw] (which sees a [w] by [h] area) into an image. [depth] adds a soft shadow lit from
 * the top left, [edge] a white cut edge, [grain] the paper fibres; [maxPx] caps the image size
 * for big backgrounds (they're soft anyway, and it keeps memory low).
 */
internal fun DrawScope.bakePaper(
    w: Float,
    h: Float,
    pad: Float,
    depth: Float,
    edge: Float,
    edgeMin: Float,
    grain: Boolean,
    maxPx: Float,
    shapeShadows: Boolean,
    draw: DrawScope.() -> Unit,
): Baked {
    val full = maxOf(w, h) + 2 * pad
    val scale = if (maxPx > 0f) minOf(1f, maxPx / full) else 1f
    val bw = ceil((w + 2 * pad) * scale).toInt().coerceAtLeast(1)
    val bh = ceil((h + 2 * pad) * scale).toInt().coerceAtLeast(1)
    val content = ImageBitmap(bw, bh)
    val canvas = Canvas(content)
    canvas.save()
    canvas.scale(scale, scale)
    canvas.translate(pad, pad)
    val old = Triple(PaperPen.edge, PaperPen.edgeMin, PaperPen.shapeShadows)
    PaperPen.edge = edge
    PaperPen.edgeMin = edgeMin
    PaperPen.shapeShadows = shapeShadows
    try {
        CanvasDrawScope().draw(this, layoutDirection, canvas, Size(w, h)) { draw() }
    } finally {
        PaperPen.edge = old.first
        PaperPen.edgeMin = old.second
        PaperPen.shapeShadows = old.third
    }
    canvas.restore()
    if (grain) {
        // Fibres only where there is paper (SrcAtop keeps the see-through parts clear).
        CanvasDrawScope().draw(this, layoutDirection, canvas, Size(bw.toFloat(), bh.toFloat())) {
            drawRect(PaperTexture.brush(), blendMode = BlendMode.SrcAtop)
        }
    }
    val image = if (depth > 0f) {
        val out = ImageBitmap(bw, bh)
        CanvasDrawScope().draw(this, layoutDirection, Canvas(out), Size(bw.toFloat(), bh.toFloat())) {
            val d = depth * scale
            // A soft shadow lit from the top left: a few faint copies, further and fainter.
            listOf(1.2f to 0.12f, 0.7f to 0.14f).forEach { (k, a) ->
                drawImage(content, Offset(d * 0.6f * k, d * k), alpha = a, colorFilter = shadowTint)
            }
            drawImage(content)
        }
        out
    } else {
        content
    }
    return Baked(image, pad, w, h)
}

/** Copies a baked image to the screen at ([dx], [dy]), optionally as a flat [tint] (for shadows and edges). */
fun DrawScope.drawBaked(b: Baked, dx: Float = 0f, dy: Float = 0f, alpha: Float = 1f, tint: Color? = null) {
    drawImage(
        b.image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(b.image.width, b.image.height),
        dstOffset = IntOffset((dx - b.pad).roundToInt(), (dy - b.pad).roundToInt()),
        dstSize = IntSize((b.w + 2 * b.pad).roundToInt(), (b.h + 2 * b.pad).roundToInt()),
        alpha = alpha,
        colorFilter = when (tint) {
            null -> null
            Color.Black -> shadowTint
            else -> ColorFilter.tint(tint, BlendMode.SrcIn)
        },
    )
}

/**
 * Bakes [draw] behind the content and redraws it only when [spec] or the size changes.
 * [live], if given, decides how the baked image is put on screen each frame (e.g. a button's
 * shadow moving as it's pressed); it may read animated state.
 */
internal class BakedPaperElement(
    val spec: Any,
    val depth: Dp = 0.dp,
    val edge: Boolean = true,
    val edgeMin: Dp = 10.dp,
    val grain: Boolean = false,
    val maxPx: Float = 0f,
    val shapeShadows: Boolean = false,
    val live: (DrawScope.(Baked) -> Unit)? = null,
    val draw: DrawScope.() -> Unit,
) : ModifierNodeElement<BakedPaperNode>() {
    override fun create() = BakedPaperNode(this)

    override fun update(node: BakedPaperNode) {
        val changed = node.element.spec != spec
        node.element = this
        if (changed) {
            node.cache = null
            node.invalidateDraw()
        }
    }

    override fun equals(other: Any?) = other is BakedPaperElement && other.spec == spec
    override fun hashCode() = spec.hashCode()
}

internal class BakedPaperNode(var element: BakedPaperElement) : Modifier.Node(), DrawModifierNode {
    var cache: Baked? = null
    private var cacheSize = Size.Zero

    override fun ContentDrawScope.draw() {
        if (size.width >= 1f && size.height >= 1f) {
            val e = element
            if (cache == null || cacheSize != size) {
                val depth = e.depth.toPx()
                val pad = maxOf(6.dp.toPx(), depth * 2.5f)
                cache = bakePaper(
                    size.width, size.height, pad, depth,
                    if (e.edge) 1.4.dp.toPx() else 0f, e.edgeMin.toPx(),
                    e.grain, e.maxPx, e.shapeShadows, e.draw,
                )
                cacheSize = size
            }
            cache?.let { b -> e.live?.invoke(this, b) ?: drawBaked(b) }
        }
        drawContent()
    }

    override fun onDetach() {
        cache = null
    }
}

/**
 * One stacked layer of a paper scene (sky, far hills, near hills...), baked with its own soft
 * shadow ([depth]: further layers get shallower shadows), white cut edges and paper fibres.
 */
@Composable
fun BakedLayer(
    key: Any,
    modifier: Modifier = Modifier,
    depth: Dp = 3.dp,
    grain: Boolean = true,
    edge: Boolean = true,
    maxPx: Float = 1000f,
    /** How to put the baked image on screen each frame, if not simply as it is. */
    live: (DrawScope.(Baked) -> Unit)? = null,
    draw: DrawScope.() -> Unit,
) {
    Spacer(
        modifier.fillMaxSize() then BakedPaperElement(
            spec = key, depth = depth, edge = edge, grain = grain, maxPx = maxPx, live = live, draw = draw,
        ),
    )
}
