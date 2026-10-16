package com.zeronolidge.speechapp.logic

import kotlin.math.min
import kotlin.math.sqrt

/** What a tap on the map lands on: an area (its picture, its label or the gap between), the next-stop button, or open sea. */
sealed interface MapTarget {
    data class Area(val land: Land) : MapTarget
    data object Stop : MapTarget
    data object Sea : MapTarget
}

/**
 * One area of the world map: where its picture label sits and where its landmark is drawn, as
 * fractions of the map's width and height. This single list is used both to DRAW each label and
 * to work out which area a tap lands on, so the two can never disagree.
 */
data class MapArea(val land: Land, val labelX: Float, val labelY: Float, val markX: Float, val markY: Float) {
    /**
     * The area's one tap target on a [w] by [h] map, in pixels: from just above the label down
     * past the landmark, wide enough for both. [labelHalfW] and [labelH] are the label chip's
     * half-width and height in pixels.
     */
    fun hitBox(w: Float, h: Float, labelHalfW: Float, labelH: Float): FloatArray {
        val r = min(w, h) * MapAreas.LANDMARK_RADIUS
        val left = min(markX * w - r, labelX * w - labelHalfW)
        val right = maxOf(markX * w + r, labelX * w + labelHalfW)
        val top = labelY * h - labelH * 0.25f
        val bottom = maxOf(markY * h + r, labelY * h + labelH * 1.25f)
        return floatArrayOf(left, top, right, bottom)
    }
}

object MapAreas {
    /** How far around a landmark counts as tapping it, as a fraction of the map's shorter side. */
    const val LANDMARK_RADIUS = 0.12f

    /** Every land's area. The castle's label sits to its left, clear of the top-right 🔊 button. */
    val all: List<MapArea> = Land.entries.map { land ->
        if (land == Land.CLOUDS) MapArea(land, land.mapX - 0.17f, land.mapY - 0.06f, land.mapX, land.mapY)
        else MapArea(land, land.mapX, (land.mapY - 0.155f).coerceAtLeast(0.005f), land.mapX, land.mapY)
    }

    fun of(land: Land): MapArea = all.first { it.land == land }

    /**
     * Which area a tap at ([x], [y]) pixels on a [w] by [h] map belongs to. Where two areas
     * overlap, the one whose label or landmark centre is nearest wins. Null for open sea.
     */
    fun hit(x: Float, y: Float, w: Float, h: Float, labelHalfW: Float, labelH: Float): Land? =
        all.filter { a ->
            val b = a.hitBox(w, h, labelHalfW, labelH)
            x in b[0]..b[2] && y in b[1]..b[3]
        }.minByOrNull { a ->
            val dl = sq(x - a.labelX * w) + sq(y - (a.labelY * h + labelH / 2))
            val dm = sq(x - a.markX * w) + sq(y - a.markY * h)
            min(dl, dm)
        }?.land

    /**
     * The ONE tap rule for the whole map. Each area has one zone: its picture, its label and the
     * gap between them. A tap on a picture or a label always opens that area; then the next-stop
     * button (a circle of radius [stopR] around [stopX], [stopY]) starts the story; then the rest
     * of an area's zone opens the area; anything else is sea. [stopSpot] keeps the button clear
     * of every zone whenever the map has room (tablets always do), so they never share a tap.
     */
    fun target(
        x: Float, y: Float, w: Float, h: Float, labelHalfW: Float, labelH: Float,
        stopX: Float, stopY: Float, stopR: Float,
    ): MapTarget {
        val area = all.firstOrNull { it.land == hit(x, y, w, h, labelHalfW, labelH) }
        if (area != null && area.core(w, h, labelHalfW, labelH).any { it.contains(x, y) }) return MapTarget.Area(area.land)
        if (sq(x - stopX) + sq(y - stopY) <= sq(stopR)) return MapTarget.Stop
        if (area != null) return MapTarget.Area(area.land)
        return MapTarget.Sea
    }

    /**
     * Where the next-stop button for [land] is drawn, in pixels: its spot on the trail ([fx], [fy]
     * as fractions) if a button of radius [stopR] there stays out of every area's zone, otherwise
     * the nearest spot that does, beside its own land: always nearer [land]'s picture than any
     * other land's (by a clear margin), so a fresh start's button sits at the Forest, never by
     * the misty Meadow.
     */
    fun stopSpot(land: Land, fx: Float, fy: Float, w: Float, h: Float, stopR: Float, labelHalfW: Float, labelH: Float): Pair<Float, Float> {
        val zones = all.map { Shape.Box(it.hitBox(w, h, labelHalfW, labelH)) }
        val cores = all.flatMap { it.core(w, h, labelHalfW, labelH) }
        val sx = (fx * w).coerceIn(stopR, w - stopR)
        val sy = (fy * h).coerceIn(stopR, h - stopR)
        // Clear of every zone; else of every picture and label; else at least the button's middle
        // half clear of them (a squarer map, where the Lagoon is crowded), always by its own land.
        // On a tiny phone map, just its middle off every picture and label, as near its land as fits.
        // First try right beside its own land (well over twice as near it as any other land).
        val tiers = listOf(
            Triple(zones, stopR, 5f), Triple(zones, stopR, 1.4f), Triple(cores, stopR, 1.4f),
            Triple(cores, stopR * 0.5f, 1.4f), Triple(cores, 1f, 1.4f), Triple(cores, 1f, 0f),
        )
        for ((avoid, r, margin) in tiers) {
            fun ok(cx: Float, cy: Float) = avoid.none { it.touches(cx, cy, r) } && (margin == 0f || belongsTo(land, cx, cy, w, h, margin))
            if (ok(sx, sy)) return sx to sy
            nearest(sx, sy, w, h, stopR) { x, y -> ok(x, y) }?.let { return it }
        }
        return sx to sy
    }

    /** True when ([x], [y]) is clearly nearer [land]'s picture than any other land's. */
    fun belongsTo(land: Land, x: Float, y: Float, w: Float, h: Float, margin: Float = 1.4f): Boolean {
        fun d(a: MapArea) = sq(x - a.markX * w) + sq(y - a.markY * h)
        val own = d(of(land))
        return all.filter { it.land != land }.all { own * margin < d(it) }
    }

    /** True when a button of radius [stopR] at ([x], [y]) is clear of every area's whole zone. */
    fun clearOfZones(x: Float, y: Float, w: Float, h: Float, stopR: Float, labelHalfW: Float, labelH: Float) =
        all.none { Shape.Box(it.hitBox(w, h, labelHalfW, labelH)).touches(x, y, stopR) }

    /** True when a button of radius [stopR] at ([x], [y]) covers no area's picture or label. */
    fun clearOfCores(x: Float, y: Float, w: Float, h: Float, stopR: Float, labelHalfW: Float, labelH: Float) =
        all.flatMap { it.core(w, h, labelHalfW, labelH) }.none { it.touches(x, y, stopR) }

    private fun nearest(sx: Float, sy: Float, w: Float, h: Float, r: Float, ok: (Float, Float) -> Boolean): Pair<Float, Float>? {
        val step = r / 4f
        var best: Pair<Float, Float>? = null
        var bestD = Float.MAX_VALUE
        var y = r
        while (y <= h - r) {
            var x = r
            while (x <= w - r) {
                val d = sq(x - sx) + sq(y - sy)
                if (d < bestD && ok(x, y)) {
                    bestD = d
                    best = x to y
                }
                x += step
            }
            y += step
        }
        return best
    }

    /** The parts of an area that are always its own: the picture (a circle) and the label chip. */
    private fun MapArea.core(w: Float, h: Float, labelHalfW: Float, labelH: Float): List<Shape> = listOf(
        Shape.Circle(markX * w, markY * h, min(w, h) * LANDMARK_RADIUS),
        Shape.Box(floatArrayOf(labelX * w - labelHalfW, labelY * h, labelX * w + labelHalfW, labelY * h + labelH)),
    )

    private sealed interface Shape {
        fun contains(x: Float, y: Float): Boolean
        fun touches(cx: Float, cy: Float, r: Float): Boolean

        class Box(val b: FloatArray) : Shape {
            override fun contains(x: Float, y: Float) = x in b[0]..b[2] && y in b[1]..b[3]
            override fun touches(cx: Float, cy: Float, r: Float): Boolean {
                val nx = cx.coerceIn(b[0], b[2])
                val ny = cy.coerceIn(b[1], b[3])
                return sqrt(sq(cx - nx) + sq(cy - ny)) < r
            }
        }

        class Circle(val x: Float, val y: Float, val r: Float) : Shape {
            override fun contains(x: Float, y: Float) = sq(x - this.x) + sq(y - this.y) <= sq(r)
            override fun touches(cx: Float, cy: Float, r: Float) = sqrt(sq(cx - x) + sq(cy - y)) < r + this.r
        }
    }

}

private fun sq(v: Float) = v * v
