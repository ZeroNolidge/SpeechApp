package com.zeronolidge.speechapp.logic

/**
 * The replay card (Ruby and Aida, test53). Each place shows at most one ⭐ on the map, but every
 * finished trail stop there can be played again: tapping the star opens a small paper card right
 * beside the place with one big picture tile per finished stop (the picture of the story that stop
 * really played), and Rory names each one ("The red balloon! Or the lost teddy!") so he can pick
 * without reading. With only one finished stop there, the star plays it straight away.
 *
 * Each trail stop remembers the plot it played (Progress.stopPlot), so its tile, its name and its
 * replay are always the same story. A stop finished before test53 gets a fixed stand-in plot
 * ([plotFor]), different from the other stop at its place.
 */
object ReplayPicker {
    /** Rory's name for each plot's story: the first tile, then "Or ..." for the next. */
    private val names: Map<String, Pair<String, String>> = mapOf(
        "kite" to ("The lost kite!" to "Or the lost kite!"),
        "party" to ("The birthday party!" to "Or the birthday party!"),
        "treasure" to ("The treasure map!" to "Or the treasure map!"),
        "teddy" to ("The lost teddy!" to "Or the lost teddy!"),
        "star" to ("The little star!" to "Or the little star!"),
        "key" to ("The golden key!" to "Or the golden key!"),
        "picnic" to ("The sunny picnic!" to "Or the sunny picnic!"),
        "balloon" to ("The red balloon!" to "Or the red balloon!"),
    )

    fun name(plot: Plot): String = names.getValue(plot.id).first
    fun orName(plot: Plot): String = names.getValue(plot.id).second
    fun hasName(plot: Plot) = plot.id in names

    /** What Rory says as the card opens: each tile's name in turn. */
    fun line(plots: List<Plot>): String = plots.mapIndexed { i, p -> if (i == 0) name(p) else orName(p) }.joinToString(" ")

    /** The plot trail stop [stop] played ([stored] its saved plot id), or its fixed stand-in. */
    fun plotFor(stop: Int, stored: String?): Plot =
        Plots.all.firstOrNull { it.id == stored } ?: Plots.all[(stop * 3) % Plots.all.size]

    /** The other trail stop at the same place as [stop]. */
    fun sibling(stop: Int): Int = if (stop % Trail.STOPS_PER_LAND == 0) stop + 1 else stop - 1

    /**
     * The finished trail stops at [land] when the trail is at [chapter] (as the map draws them:
     * every stop behind the next one, all of them after a full lap, never the next stop itself).
     */
    fun finishedAt(land: Land, chapter: Int): List<Int> {
        val current = Trail.stopOf(chapter)
        val lap = chapter / Trail.size
        return (0 until Trail.size).filter { s -> Trail.landAt(s) == land && (lap > 0 || s < current) && s != current }
    }

    /** The card's parts, in dp on screen: the card, its tiles, and its big close ✕. */
    data class Card(val box: FloatArray, val tiles: List<FloatArray>, val close: FloatArray)

    /** A tile's side (dp): big enough for a small finger and a clear picture. */
    fun tileSide(narrow: Boolean) = if (narrow) 112f else 136f

    /** The close ✕ (dp across). */
    const val CLOSE = 56f

    /**
     * Lays out the card for [n] tiles beside a place whose picture is at ([px], [py]) on a
     * [screenW] by [screenH] screen (dp): just below the picture, or above it when there's no room
     * below, always wholly on screen. The ✕ sits in the card's own top strip, clear of the tiles.
     */
    fun card(n: Int, px: Float, py: Float, pictureR: Float, screenW: Float, screenH: Float, narrow: Boolean): Card {
        val tile = tileSide(narrow)
        val pad = 16f
        val gap = 16f
        val w = n * tile + (n - 1) * gap + 2 * pad
        val h = CLOSE + 4f + tile + pad
        val margin = 12f
        val left = (px - w / 2).coerceIn(margin, (screenW - w - margin).coerceAtLeast(margin))
        val below = py + pictureR * 0.6f
        val top = if (below + h <= screenH - margin) below else (py - pictureR * 0.6f - h).coerceIn(margin, (screenH - h - margin).coerceAtLeast(margin))
        val tiles = List(n) { i ->
            val l = left + pad + i * (tile + gap)
            floatArrayOf(l, top + CLOSE + 4f, l + tile, top + CLOSE + 4f + tile)
        }
        val close = floatArrayOf(left + w - CLOSE - 6f, top + 4f, left + w - 6f, top + 4f + CLOSE)
        return Card(floatArrayOf(left, top, left + w, top + h), tiles, close)
    }
}
