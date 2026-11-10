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

    /** The gap (dp) the card keeps from the Adventure bar (Ruby, test53b). */
    const val BAR_GAP = 16f

    /**
     * The big Adventure bar on the map, in dp on screen, worked out as the map's corner buttons lay
     * it out (14 dp side and 10 dp bottom padding; on a phone a full-width row of its own, 84 dp
     * tall, above the 80 dp row of round buttons; on a tablet in the middle of that row, up to 460 dp
     * wide, between the 64 dp gear and the two 80 dp buttons).
     */
    fun adventureBar(screenW: Float, screenH: Float, narrow: Boolean): FloatArray =
        if (narrow) floatArrayOf(14f, screenH - 10f - 80f - 14f - 84f, screenW - 14f, screenH - 10f - 80f - 14f)
        else {
            val l = 14f + 64f + 24f
            val r = screenW - 14f - 24f - 80f - 18f - 80f
            val bw = minOf(460f, r - l)
            val bl = l + (r - l - bw) / 2
            floatArrayOf(bl, screenH - 10f - 84f, bl + bw, screenH - 10f)
        }

    private fun boxHitsCircle(b: FloatArray, cx: Float, cy: Float, r: Float): Boolean {
        val dx = maxOf(b[0] - cx, 0f, cx - b[2])
        val dy = maxOf(b[1] - cy, 0f, cy - b[3])
        return dx * dx + dy * dy < r * r
    }

    /**
     * Lays out the card for [n] tiles for a place whose picture is at ([px], [py]) on a [screenW]
     * by [screenH] screen (dp), always wholly on screen, [BAR_GAP] clear of the Adventure bar and
     * clear of the next stop's pulsing ring [ring] (x, y, r on screen). On a phone: centred, just
     * above the Adventure bar (moved up only as far as it must to clear the ring). On a tablet: just
     * above the tapped place, else below or beside it, else the nearest clear spot. The ✕ sits in
     * the card's own top strip, clear of the tiles.
     */
    fun card(n: Int, px: Float, py: Float, pictureR: Float, screenW: Float, screenH: Float, narrow: Boolean, ring: FloatArray? = null): Card {
        val tile = tileSide(narrow)
        val pad = 16f
        val gap = 16f
        val w = n * tile + (n - 1) * gap + 2 * pad
        val h = CLOSE + 4f + tile + pad
        val margin = 12f
        val bar = adventureBar(screenW, screenH, narrow)
        val barZone = floatArrayOf(bar[0] - BAR_GAP, bar[1] - BAR_GAP, bar[2] + BAR_GAP, bar[3] + BAR_GAP)
        fun overlaps(a: FloatArray, b: FloatArray) = a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]
        fun clear(l: Float, t: Float): Boolean {
            val b = floatArrayOf(l, t, l + w, t + h)
            if (l < margin - 0.01f || t < margin - 0.01f || l + w > screenW - margin + 0.01f || t + h > screenH - margin + 0.01f) return false
            if (overlaps(b, barZone)) return false
            return ring == null || !boxHitsCircle(b, ring[0], ring[1], ring[2] + 8f)
        }
        fun clampL(l: Float) = l.coerceIn(margin, (screenW - w - margin).coerceAtLeast(margin))
        fun clampT(t: Float) = t.coerceIn(margin, (screenH - h - margin).coerceAtLeast(margin))
        var spot: Pair<Float, Float>? = null
        if (narrow) {
            val l = (screenW - w) / 2
            var t = bar[1] - BAR_GAP - h
            while (t >= margin && !clear(l, t)) t -= 4f
            if (t >= margin) spot = l to t
        } else {
            val off = pictureR * 0.6f + 8f
            val tries = listOf(
                clampL(px - w / 2) to py - off - h, // just above the place
                clampL(px - w / 2) to py + off, // just below it
                px - off - w to clampT(py - h / 2), // to its left
                px + off to clampT(py - h / 2), // to its right
            )
            spot = tries.firstOrNull { (l, t) -> clear(l, t) }
            if (spot == null) {
                // The nearest clear spot to just above the place, on an 8 dp grid.
                val (ax, ay) = tries[0]
                var best: Pair<Float, Float>? = null
                var bestD = Float.MAX_VALUE
                var t = margin
                while (t + h <= screenH - margin) {
                    var l = margin
                    while (l + w <= screenW - margin) {
                        val d = (l - ax) * (l - ax) + (t - ay) * (t - ay)
                        if (d < bestD && clear(l, t)) { bestD = d; best = l to t }
                        l += 8f
                    }
                    t += 8f
                }
                spot = best
            }
        }
        val (left, top) = spot ?: (clampL((screenW - w) / 2) to clampT(bar[1] - BAR_GAP - h))
        val tiles = List(n) { i ->
            val l = left + pad + i * (tile + gap)
            floatArrayOf(l, top + CLOSE + 4f, l + tile, top + CLOSE + 4f + tile)
        }
        val close = floatArrayOf(left + w - CLOSE - 6f, top + 4f, left + w - 6f, top + 4f + CLOSE)
        return Card(floatArrayOf(left, top, left + w, top + h), tiles, close)
    }
}
