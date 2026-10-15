package com.zeronolidge.speechapp.logic

import kotlin.math.min

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

    private fun sq(v: Float) = v * v
}
