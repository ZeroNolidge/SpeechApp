package com.zeronolidge.speechapp.logic

import kotlin.math.min
import kotlin.math.sqrt

/** What a tap on the map lands on: an area (its picture, its label or the gap between), the next-stop button, or open sea. */
sealed interface MapTarget {
    data class Area(val land: Land) : MapTarget
    data object Stop : MapTarget
    /** The crystal cave stop beside the Forest (only there once Pip is found). */
    data object Cave : MapTarget
    /** A voice story's stop (logic/VoiceStories.kt). */
    data class Story(val id: String) : MapTarget
    /** A finished stop's ⭐ on the trail: tapping it replays a story there. */
    data class Done(val stop: Int) : MapTarget
    data object Sea : MapTarget
}

/**
 * A round thing on the map (the ▶, a voice story, the cave, a star): [target] at ([x], [y]), drawn
 * with radius [r], in pixels. [tap] is its invisible tap circle's radius (test54: at least 32 dp
 * on a phone, 40 on a tablet), never smaller than [r]. Keepouts and spacing use [r].
 */
data class MapSpot(val target: MapTarget, val x: Float, val y: Float, val r: Float, val tap: Float = r)

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

    /** The corner buttons' row (dp from the top): 10 dp padding and the 80 dp buttons. */
    const val TOP_BAR = 90f

    /** How far up (dp from the bottom) the Adventure bar reaches: on a phone it has its own row above the buttons. */
    fun adventureTop(narrow: Boolean) = if (narrow) 188f else 94f

    /** The island's side margins and its gap to the bars above and below it (dp). */
    const val MAP_MARGIN = 16f
    const val MAP_GAP = 6f

    /** A phone's party bunting hangs from here (dp from the top), just under the corner buttons. */
    const val PHONE_BUNTING_Y = 88f
    const val PHONE_BUNTING_H = 26f

    /** The party bunting's whole drop (flags, sag and cord) for flags [flagH] dp tall. */
    fun buntingDrop(flagH: Float) = flagH * 1.22f + 4f

    /**
     * Where the map goes on a [screenW] by [screenH] screen (dp): the whole island picture, one
     * piece, from the corner buttons down to the Adventure bar and across the screen with
     * [MAP_MARGIN] at the sides (Ruby, test54). On a tablet the party bunting hangs in the corner
     * buttons' row; on a phone it hangs over the top of the picture, so nothing on the map goes
     * in its top [MapFrame.keepTop] dp.
     */
    data class MapFrame(val left: Float, val top: Float, val w: Float, val h: Float, val keepTop: Float, val narrow: Boolean)

    fun mapFrame(screenW: Float, screenH: Float): MapFrame {
        val narrow = screenW < 600f
        val top = TOP_BAR + MAP_GAP
        val bottom = adventureTop(narrow) + MAP_GAP
        val w = (screenW - 2 * MAP_MARGIN).coerceAtLeast(1f)
        val h = (screenH - top - bottom).coerceAtLeast(1f)
        val keep = if (narrow) (PHONE_BUNTING_Y + buntingDrop(PHONE_BUNTING_H) - top).coerceAtLeast(0f) else 0f
        return MapFrame(MAP_MARGIN, top, w, h, keep, narrow)
    }

    /** Every round stop's and star's invisible tap circle (dp across) on a map [mapWDp] wide: 64 on a phone, 80 on a tablet. */
    fun tapNode(mapWDp: Float) = if (mapWDp < 520f) 64f else 80f

    /** The place labels' text size (sp) on a map whose shorter side is [unitDp]. */
    fun labelSp(unitDp: Float) = (unitDp * 0.055f).coerceIn(20f, 30f)

    /** A label chip's height (dp) for labels of [labelSp]. */
    fun labelH(labelSp: Float) = labelSp * 1.05f + 7f

    /**
     * Half the widest label chip's width (dp) for labels of [labelSp] (picture, gap and up to
     * seven letters of the chunky font, plus padding): every stop keeps clear of a box this wide.
     */
    fun labelHalfW(labelSp: Float) = labelSp * 2.6f

    /** How big the next stop's ring pulses (IslandMap's pulse). */
    const val STOP_PULSE = 1.1f

    /** A voice story's stop (dp across) on a map [mapWDp] wide: smaller on a phone, never under 44. */
    fun storyNode(mapWDp: Float) = if (mapWDp < 520f) 52f else 67.2f

    /** A finished stop's star: its tap circle (dp across) is never under 44, however small it's drawn. */
    fun starTap(unitDp: Float) = maxOf(unitDp * 0.085f, 44f)

    /** The next stop's middle keeps this far (dp) from the map's edges: its pulsing ring and 16 dp more. */
    fun stopEdge(stopNodeDp: Float) = stopNodeDp / 2f * STOP_PULSE + 16f + 4f

    /** Rory's height in the fix moment on a map whose shorter side is [unit] (smaller on a phone). */
    fun roryH(unit: Float) = unit * (if (unit < 520f) 0.18f else 0.22f)

    /**
     * Where Rory stands (top-left corner, px) for [land]'s fix moment, [rw] by [rh]: right next to
     * the place's picture, never on any label or another place's picture, inside the map; failing
     * that, out in the open sea at the bottom-left corner. (Ruby: not in the Lagoon pond for the
     * Meadow.)
     */
    fun roryBeside(
        land: Land, w: Float, h: Float, rw: Float, rh: Float, labelHalfW: Float, labelH: Float,
        /** Round things he must not stand on (the next stop's ring, as it fades back in: test53). */
        avoid: List<MapSpot> = emptyList(),
        /** Keep below this (px from the map's top: a phone's bunting). */
        top: Float = 0f,
    ): Pair<Float, Float> {
        val a = of(land)
        val mx = a.markX * w
        val my = a.markY * h
        val pr = min(w, h) * LANDMARK_RADIUS
        val labels = all.map { labelBox(it, w, h, labelHalfW, labelH) }
        fun clear(l: Float, t: Float): Boolean {
            if (l < 0f || t < top || l + rw > w || t + rh > h) return false
            if (labels.any { b -> l < b[2] && l + rw > b[0] && t < b[3] && t + rh > b[1] }) return false
            if (avoid.any { sp -> boxTouchesCircle(l, t, l + rw, t + rh, sp.x, sp.y, sp.r) }) return false
            // Off every picture's middle (his own too: he stands beside it, so it shows).
            return all.none { o ->
                val cx = (o.markX * w).coerceIn(l, l + rw)
                val cy = (o.markY * h).coerceIn(t, t + rh)
                sqrt(sq(cx - o.markX * w) + sq(cy - o.markY * h)) < pr * (if (o.land == land) 0.35f else 0.45f)
            }
        }
        // Beside it first (right or left of the picture, feet level with it), then below it, then
        // the corners; then a little further out each time.
        for (k in listOf(0.5f, 0.7f, 0.9f, 1.1f, 1.35f, 1.6f, 1.9f)) {
            val g = pr * k
            val spots = listOf(
                mx + g to my - rh * 0.75f, mx - g - rw to my - rh * 0.75f,
                mx - rw / 2 to my + g * 0.8f,
                mx + g * 0.7f to my + g * 0.3f, mx - g * 0.7f - rw to my + g * 0.3f,
                mx + g to my - rh * 0.3f, mx - g - rw to my - rh * 0.3f,
                mx + g to my - rh, mx - g - rw to my - rh,
                mx - rw / 2 to my - g * 0.8f - rh, mx + g * 0.5f to my + g * 0.6f, mx - g * 0.5f - rw to my + g * 0.6f,
            )
            for ((l, t) in spots) if (clear(l, t)) return l to t
        }
        // Crowded (the castle in its top corner on a phone): the nearest clear spot, as near as fits.
        var best: Pair<Float, Float>? = null
        var bestD = Float.MAX_VALUE
        val step = rw / 6f
        var t = top
        while (t + rh <= h) {
            var l = 0f
            while (l + rw <= w) {
                val nx = mx.coerceIn(l, l + rw)
                val ny = my.coerceIn(t, t + rh)
                val d = sq(nx - mx) + sq(ny - my)
                if (d < bestD && clear(l, t)) { bestD = d; best = l to t }
                l += step
            }
            t += step
        }
        return best ?: (w * 0.02f to h - rh)
    }

    /** True when the box ([l], [t])-([r], [b]) touches the circle at ([cx], [cy]) of radius [cr]. */
    fun boxTouchesCircle(l: Float, t: Float, r: Float, b: Float, cx: Float, cy: Float, cr: Float): Boolean {
        val nx = cx.coerceIn(l, r)
        val ny = cy.coerceIn(t, b)
        return sq(nx - cx) + sq(ny - cy) < sq(cr)
    }

    /** Which way the pointing hand points (at the next stop). */
    enum class HandDir { RIGHT, LEFT, DOWN, UP }

    /** Where the pointing hand goes: its box's top-left corner (px), its size [hand], and its way. */
    data class HandSpot(val x: Float, val y: Float, val dir: HandDir) {
        /** The box it may cover as it bobs (a quarter of its size towards the stop). */
        fun box(hand: Float): FloatArray = when (dir) {
            HandDir.RIGHT -> floatArrayOf(x, y, x + hand * 1.25f, y + hand)
            HandDir.LEFT -> floatArrayOf(x - hand * 0.25f, y, x + hand, y + hand)
            HandDir.DOWN -> floatArrayOf(x, y, x + hand, y + hand * 1.25f)
            HandDir.UP -> floatArrayOf(x, y - hand * 0.25f, x + hand, y + hand)
        }
    }

    /**
     * Where the pointing hand goes beside the next stop at ([sx], [sy]) whose pulsing ring reaches
     * [ringR]: on the stop's outer side ([outerLeft]: its left), else above it pointing down, else
     * below pointing up, else on its inner side; the first spot wholly inside the map and on no
     * place's label (Ruby, test53: never on "Market" by an edge). [hero] is the buddy's box beside
     * the stop, which it keeps off too.
     */
    fun handSpot(
        sx: Float, sy: Float, ringR: Float, hand: Float, outerLeft: Boolean, w: Float, h: Float,
        labelHalfW: Float, labelH: Float, hero: FloatArray? = null,
        /** Keep below this (px from the map's top: a phone's bunting). */
        top: Float = 0f,
    ): HandSpot {
        val labels = all.map { labelBox(it, w, h, labelHalfW, labelH) }
        fun overlaps(a: FloatArray, b: FloatArray) = a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1]
        fun ok(sp: HandSpot): Boolean {
            val b = sp.box(hand)
            if (b[0] < 2f || b[1] < top + 2f || b[2] > w - 2f || b[3] > h - 2f) return false
            if (labels.any { overlaps(b, it) }) return false
            return hero == null || !overlaps(b, hero)
        }
        val left = HandSpot(sx - ringR - hand * 1.3f, sy - hand / 2, HandDir.RIGHT)
        val right = HandSpot(sx + ringR + hand * 0.05f, sy - hand / 2, HandDir.LEFT)
        val above = HandSpot(sx - hand / 2, sy - ringR - hand * 1.3f, HandDir.DOWN)
        val below = HandSpot(sx - hand / 2, sy + ringR + hand * 0.3f, HandDir.UP)
        // Corners, for a stop tucked by an edge with a label above or below it.
        val d = ringR * 0.72f
        val aboveL = HandSpot(sx - d - hand * 0.5f, sy - d - hand * 1.25f, HandDir.DOWN)
        val aboveR = HandSpot(sx + d - hand * 0.5f, sy - d - hand * 1.25f, HandDir.DOWN)
        val belowL = HandSpot(sx - d - hand * 0.5f, sy + d + hand * 0.25f, HandDir.UP)
        val belowR = HandSpot(sx + d - hand * 0.5f, sy + d + hand * 0.25f, HandDir.UP)
        val order = if (outerLeft) listOf(left, above, below, aboveL, belowL, aboveR, belowR, right)
        else listOf(right, above, below, aboveR, belowR, aboveL, belowL, left)
        fun inMapOffLabels(sp: HandSpot) = sp.box(hand).let { b -> b[0] >= 2f && b[1] >= top + 2f && b[2] <= w - 2f && b[3] <= h - 2f && labels.none { overlaps(b, it) } }
        // No spot clear of the buddy too: the one that covers the least of him.
        fun onHero(sp: HandSpot): Float {
            if (hero == null) return 0f
            val b = sp.box(hand)
            return maxOf(0f, minOf(b[2], hero[2]) - maxOf(b[0], hero[0])) * maxOf(0f, minOf(b[3], hero[3]) - maxOf(b[1], hero[1]))
        }
        return order.firstOrNull(::ok) ?: order.filter(::inMapOffLabels).minByOrNull(::onHero)
            ?: order.firstOrNull { sp -> sp.box(hand).let { b -> labels.none { overlaps(b, it) } } } ?: order.first()
    }

    /**
     * Where the trail buddy stands beside the next stop (node = the stop's full size): on its
     * inner side, and slid further out when the pointing hand had to go on that side too, so the
     * hand never covers him (test53). The same box with the hand shown or hidden, so he never jumps.
     */
    fun heroBox(sx: Float, sy: Float, node: Float, heroOnRight: Boolean, spot: HandSpot?, hand: Float): FloatArray {
        val hw = node * 0.7f
        var l = if (heroOnRight) sx + node * 0.55f else sx - node * 1.2f
        val t = sy - node * 0.6f
        if (spot != null) {
            val b = spot.box(hand)
            if (b[0] < l + hw && b[2] > l && b[1] < t + node * 0.85f && b[3] > t) l = if (heroOnRight) b[2] + 2f else b[0] - 2f - hw
        }
        return floatArrayOf(l, t, l + hw, t + node * 0.85f)
    }

    /** Every round thing on the map besides the areas: where each one goes. */
    data class Layout(val stop: MapSpot, val cave: MapSpot?, val stories: List<MapSpot>, val done: List<MapSpot>) {
        val all: List<MapSpot> get() = listOf(stop) + listOfNotNull(cave) + stories + done
    }

    /**
     * Places the next-stop button (radius [stopR]) for chapter stop [current] on lap [lap], the
     * crystal cave ([caveR], or null when it isn't open), the voice stories' stops ([storyR],
     * each by its land, in order) and the finished stops' stars ([starR]). The ONE layout rule,
     * used by the map and by its tests, so neither can drift: nothing touches a place's label.
     */
    fun layout(
        current: Int, lap: Int, w: Float, h: Float, stopR: Float, caveR: Float?, storyR: Float,
        stories: List<Pair<String, Land>>, starR: Float, labelHalfW: Float, labelH: Float,
        /** How far the next stop's middle keeps from the map's edges (its pulsing ring and a margin). */
        stopEdge: Float = 0f,
        /** The invisible tap circles' radius for the cave, story stops and stars (never under their drawn size). */
        tapR: Float = 0f,
        /** Nothing goes in the map's top [keepTop] px (a phone's party bunting hangs there). */
        keepTop: Float = 0f,
    ): Layout {
        val (fx, fy) = Trail.position(current)
        // Its ring pulses to 1.1x: that keeps off the labels too (Ruby: "Market" on iPad).
        val (sx, sy) = stopSpot(Trail.landAt(current), fx, fy, w, h, stopR, labelHalfW, labelH, edge = stopEdge, labelR = stopR * STOP_PULSE, top = keepTop)
        val stop = MapSpot(MapTarget.Stop, sx, sy, stopR)
        val cave = caveR?.let { r -> spotBy(Land.FOREST, w, h, r, listOf(stop), labelHalfW, labelH, tap = maxOf(r, tapR), top = keepTop).let { (x, y) -> MapSpot(MapTarget.Cave, x, y, r, maxOf(r, tapR)) } }
        val placed = mutableListOf(stop)
        cave?.let { placed += it }
        val storySpots = stories.map { (id, land) ->
            val (x, y) = spotBy(land, w, h, storyR, placed, labelHalfW, labelH, tap = maxOf(storyR, tapR), top = keepTop)
            MapSpot(MapTarget.Story(id), x, y, storyR, maxOf(storyR, tapR)).also { placed += it }
        }
        // At most one star per place (its latest finished stop): a fixed island, not a sheet of
        // stars (Ruby, test52). The place itself replays its stories.
        val done = (0 until Trail.size).filter { s -> (lap > 0 || s < current) && s != current }
            .groupBy { Trail.landAt(it) }.values.map { it.max() }.sorted()
        return Layout(stop, cave, storySpots, doneSpots(done, w, h, starR, placed, labelHalfW, labelH, tap = maxOf(starR, tapR), top = keepTop))
    }

    /** The label chip of [area] on a [w] by [h] map, in pixels (left, top, right, bottom). */
    fun labelBox(area: MapArea, w: Float, h: Float, labelHalfW: Float, labelH: Float): FloatArray {
        // As drawn: centred on its spot, but kept inside the map's width; as wide as its own name
        // (measured on phone and iPad, test51, with a little to spare), the widest at labelHalfW.
        val half = labelHalfW * labelWidth(area.land)
        val left = (area.labelX * w - half).coerceIn(0f, (w - 2 * half).coerceAtLeast(0f))
        return floatArrayOf(left, area.labelY * h, left + 2 * half, area.labelY * h + labelH)
    }

    /** Each place's label width as a share of the widest (Meadow), from the drawn chips. */
    fun labelWidth(land: Land): Float = when (land) {
        Land.CAVES -> 0.86f; Land.PEAKS -> 0.87f; Land.CLOUDS -> 0.9f; Land.FOREST -> 0.9f
        Land.LAGOON -> 0.93f; Land.MARKET -> 0.96f; Land.FACTORY -> 0.99f; Land.MEADOW -> 1f
    }

    /** True when a round stop of radius [r] at ([x], [y]) touches any place's label. */
    fun touchesLabel(x: Float, y: Float, r: Float, w: Float, h: Float, labelHalfW: Float, labelH: Float): Boolean =
        all.any { Shape.Box(labelBox(it, w, h, labelHalfW, labelH)).touches(x, y, r) }

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
     * The ONE tap rule for the whole map (test54). In order: a place's label opens it; the ▶ (a
     * circle of radius [stopR] around [stopX], [stopY]) always takes its own circle; then the
     * round stops (the cave, the voice stories), each by its invisible tap circle, the nearest
     * (distance over tap radius) winning; then the stars, only outside every stop's tap circle;
     * then a place's picture middle and the rest of its zone open it; anything else is sea.
     */
    fun target(
        x: Float, y: Float, w: Float, h: Float, labelHalfW: Float, labelH: Float,
        stopX: Float, stopY: Float, stopR: Float,
        /** The crystal cave stop, when there is one ([caveR] 0 when not), and its tap circle. */
        caveX: Float = 0f, caveY: Float = 0f, caveR: Float = 0f,
        /** Other round things: the voice stories' stops and the stars ([MapSpot.tap] each). */
        spots: List<MapSpot> = emptyList(),
        caveTap: Float = caveR,
    ): MapTarget {
        val area = all.firstOrNull { it.land == hit(x, y, w, h, labelHalfW, labelH) }
        if (area != null && area.labelCore(w, h, labelHalfW, labelH).contains(x, y)) return MapTarget.Area(area.land)
        if (sq(x - stopX) + sq(y - stopY) <= sq(stopR)) return MapTarget.Stop
        val stops = (if (caveR > 0f) listOf(MapSpot(MapTarget.Cave, caveX, caveY, caveR, maxOf(caveR, caveTap))) else emptyList()) +
            spots.filter { it.target !is MapTarget.Done }
        fun reach(sp: MapSpot) = sqrt(sq(x - sp.x) + sq(y - sp.y)) / sp.tap
        stops.filter { reach(it) <= 1f }.minByOrNull(::reach)?.let { return it.target }
        spots.filter { it.target is MapTarget.Done && reach(it) <= 1f }.minByOrNull(::reach)?.let { return it.target }
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
    fun stopSpot(
        land: Land, fx: Float, fy: Float, w: Float, h: Float, stopR: Float, labelHalfW: Float, labelH: Float,
        edge: Float = 0f, labelR: Float = stopR, top: Float = 0f,
    ): Pair<Float, Float> = place(land, fx, fy, w, h, stopR, labelHalfW, labelH, edge = edge, labelR = labelR, top = top) { _, _ -> true }

    /**
     * Where the crystal cave stop goes: just past the Forest (where Pip lives), by the same rules
     * as the next-stop button (clear of every area's zone, clearly nearest the Forest), and never
     * touching the next-stop button at ([stopX], [stopY], radius [stopR]).
     */
    fun caveSpot(
        w: Float, h: Float, caveR: Float, stopX: Float, stopY: Float, stopR: Float, labelHalfW: Float, labelH: Float,
        others: List<MapSpot> = emptyList(),
        tap: Float = caveR,
    ): Pair<Float, Float> = spotBy(Land.FOREST, w, h, caveR, others + MapSpot(MapTarget.Stop, stopX, stopY, stopR), labelHalfW, labelH, tap = tap)

    /**
     * Where an extra round stop of radius [r] for [land] goes (a voice story by its land): just in
     * front of the land's picture, by the next-stop button's rules (clear of every area's zone,
     * clearly nearest [land]), and never touching any of [avoid] (the next stop, other stops).
     */
    fun spotBy(
        land: Land, w: Float, h: Float, r: Float, avoid: List<MapSpot>, labelHalfW: Float, labelH: Float,
        /** Its invisible tap circle: it never reaches into the ▶'s circle (the ▶ keeps its own taps). */
        tap: Float = r,
        top: Float = 0f,
    ): Pair<Float, Float> {
        val a = of(land)
        val gap = r * 0.4f
        fun spaced(x: Float, y: Float) = avoid.all {
            val d = sqrt(sq(x - it.x) + sq(y - it.y))
            d >= it.r + r + gap && (it.target != MapTarget.Stop || d >= it.r + tap)
        }
        return place(land, a.markX + 0.02f, a.markY + 0.21f, w, h, r, labelHalfW, labelH, looseOk = ::spaced, top = top, tapR = tap) { x, y ->
            onIsland(x, y, w, h, r) && spaced(x, y)
        }
    }

    /**
     * Where the ⭐ of each finished stop in [stops] goes (radius [r]): on its trail spot when that's
     * clear, otherwise the nearest clear spot by its land (clear of every picture and label, on
     * the island, never touching [avoid] or another star), so each star takes its own taps.
     */
    fun doneSpots(
        stops: List<Int>, w: Float, h: Float, r: Float, avoid: List<MapSpot>, labelHalfW: Float, labelH: Float,
        tap: Float = r, top: Float = 0f,
    ): List<MapSpot> {
        val placed = avoid.toMutableList()
        val gap = r * 0.2f
        return stops.map { s ->
            val (fx, fy) = Trail.position(s)
            // Clear of every drawn thing, and (test54) the star's middle and most of it outside
            // every stop's tap circle, so the stop wins where they overlap and the star still has its own.
            fun spaced(x: Float, y: Float) = placed.all {
                val d = sqrt(sq(x - it.x) + sq(y - it.y))
                d >= it.r + r + gap && (it.target is MapTarget.Done || d >= it.tap + r * 0.6f)
            }
            val (x, y) = place(Trail.landAt(s), fx, fy, w, h, r, labelHalfW, labelH, step = r * 0.75f, looseOk = ::spaced, top = top, tapR = tap) { x, y ->
                onIsland(x, y, w, h, r) && spaced(x, y)
            }
            // On a crowded small phone a star may have to sit near another place's picture: its tap
            // circle then stops short of that picture's middle (never smaller than the star itself).
            val pr = min(w, h) * LANDMARK_RADIUS
            val nearMark = all.minOf { sqrt(sq(x - it.markX * w) + sq(y - it.markY * h)) }
            MapSpot(MapTarget.Done(s), x, y, r, maxOf(r, minOf(tap, nearMark - pr * 0.15f))).also { placed += it }
        }
    }

    /**
     * On a crowded map a stop may hang over the beach's edge: this much of its radius must still be
     * on the island ([onIsland] with radius r * this), rather than it sitting on a place's label.
     */
    const val LOOSE_ISLAND = 0.3f

    /**
     * The island's shape on the map (fractions of its width and height; IslandMap draws it).
     * test54: centred so its wobbly coast spans the whole map box (about 1% in from each edge).
     */
    const val ISLAND_CX = 0.5096f
    const val ISLAND_CY = 0.5164f
    const val ISLAND_RX = 0.4804f
    const val ISLAND_RY = 0.5039f

    /**
     * True when a stop of radius [r] at ([x], [y]) sits wholly on the island (grass or beach) (never out
     * in the sea, where the cave and owl stops once ended up when the Forest's zone pushed them).
     */
    fun onIsland(x: Float, y: Float, w: Float, h: Float, r: Float): Boolean {
        // The beach edge is the coast at 1.0 (grass at 0.93), and the coast wobbles down to 0.92.
        val k = 0.99f * 0.92f
        val rx = ISLAND_RX * k * w - r
        val ry = ISLAND_RY * k * h - r
        if (rx <= 0f || ry <= 0f) return false
        return sq((x - ISLAND_CX * w) / rx) + sq((y - ISLAND_CY * h) / ry) <= 1f
    }

    private fun place(
        land: Land, fx: Float, fy: Float, w: Float, h: Float, stopR: Float, labelHalfW: Float, labelH: Float,
        step: Float = stopR / 4f,
        /** When even the last tier is full: the spacing from other stops that must still hold. */
        looseOk: ((Float, Float) -> Boolean)? = null,
        /** How far its middle keeps from the map's edges (at least [stopR]). */
        edge: Float = 0f,
        /** How far its middle keeps from every label (at least [stopR]). */
        labelR: Float = stopR,
        /** Nothing above this (px from the map's top): its drawn circle stays below. */
        top: Float = 0f,
        /** Its tap circle's radius: it never reaches any place's picture middle (those open their place). */
        tapR: Float = stopR,
        alsoOk: (Float, Float) -> Boolean,
    ): Pair<Float, Float> {
        val m = maxOf(stopR, edge)
        val zones = all.map { Shape.Box(it.hitBox(w, h, labelHalfW, labelH)) }
        val cores = all.flatMap { it.core(w, h, labelHalfW, labelH) }
        val yMin = maxOf(m, top + stopR)
        val sx = (fx * w).coerceIn(m, (w - m).coerceAtLeast(m))
        val sy = (fy * h).coerceIn(yMin, (h - m).coerceAtLeast(yMin))
        // Clear of every zone; else of every picture and label; else at least the button's middle
        // half clear of them (a squarer map, where the Lagoon is crowded), always by its own land.
        // On a tiny phone map, just its middle off every picture and label, as near its land as fits.
        // First try right beside its own land (well over twice as near it as any other land).
        val tiers = listOf(
            Triple(zones, stopR, 5f), Triple(zones, stopR, 1.4f), Triple(cores, stopR, 1.4f),
            Triple(cores, stopR * 0.5f, 1.4f), Triple(cores, 1f, 1.4f), Triple(cores, 1f, 1f),
        )
        // Whatever the tier, a stop never covers a place's label (Ruby: the ▶ on "Meadow").
        // test54: ...and its tap circle never takes a place's picture middle (a picture always opens its place).
        val marks = all.map { (it.markX * w) to (it.markY * h) }
        val markR = tapR + min(w, h) * LANDMARK_RADIUS * 0.15f
        fun clearOfLabels(cx: Float, cy: Float) = !touchesLabel(cx, cy, maxOf(stopR, labelR), w, h, labelHalfW, labelH) &&
            marks.none { (mx, my) -> sq(cx - mx) + sq(cy - my) < sq(markR) }
        for ((avoid, r, margin) in tiers) {
            fun ok(cx: Float, cy: Float) = clearOfLabels(cx, cy) && avoid.none { it.touches(cx, cy, r) } && (margin == 0f || belongsTo(land, cx, cy, w, h, margin)) && alsoOk(cx, cy)
            if (ok(sx, sy)) return sx to sy
            nearest(sx, sy, w, h, m, step, yMin) { x, y -> ok(x, y) }?.let { return it }
        }
        // Crowded: still off every label and with its middle off every picture (so its taps stay
        // its own), nearer its own land than any other if it can be, hanging over the beach's edge
        // if need be. Only a map too tiny for any of that (no real screen) gives way: its own taps
        // first, then just off the labels.
        fun centreClear(cx: Float, cy: Float) = cores.none { it.touches(cx, cy, 1f) }
        fun byLand(cx: Float, cy: Float) = belongsTo(land, cx, cy, w, h, 1f)
        val spaced = looseOk ?: { _, _ -> true }
        fun loose(cx: Float, cy: Float) = onIsland(cx, cy, w, h, stopR * LOOSE_ISLAND) && spaced(cx, cy)
        fun label(cx: Float, cy: Float) = clearOfLabels(cx, cy)
        fun labelOnly(cx: Float, cy: Float) = !touchesLabel(cx, cy, maxOf(stopR, labelR), w, h, labelHalfW, labelH)
        val middles = all.flatMap { it.core(w, h, labelHalfW, labelH, pictureShare = 0.5f) }
        fun mid(cx: Float, cy: Float) = middles.none { it.touches(cx, cy, 1f) }
        val fallbacks = listOf<(Float, Float) -> Boolean>(
            { x, y -> label(x, y) && centreClear(x, y) && alsoOk(x, y) && byLand(x, y) },
            { x, y -> label(x, y) && centreClear(x, y) && loose(x, y) && byLand(x, y) },
            { x, y -> label(x, y) && mid(x, y) && alsoOk(x, y) && byLand(x, y) },
            { x, y -> label(x, y) && mid(x, y) && loose(x, y) && byLand(x, y) },
            { x, y -> label(x, y) && centreClear(x, y) && alsoOk(x, y) },
            { x, y -> label(x, y) && centreClear(x, y) && loose(x, y) },
            // (Its middle off the pictures' middles: still its own tap, by the tap rule.)
            { x, y -> label(x, y) && mid(x, y) && alsoOk(x, y) },
            { x, y -> label(x, y) && mid(x, y) && loose(x, y) },
            { x, y -> label(x, y) && mid(x, y) && spaced(x, y) },
            { x, y -> label(x, y) && alsoOk(x, y) },
            { x, y -> label(x, y) && loose(x, y) },
            { x, y -> label(x, y) && spaced(x, y) },
            // (Crowded, a small phone: its tap circle may reach a picture's middle; never a label.)
            { x, y -> labelOnly(x, y) && mid(x, y) && loose(x, y) && byLand(x, y) },
            { x, y -> labelOnly(x, y) && mid(x, y) && loose(x, y) },
            { x, y -> labelOnly(x, y) && centreClear(x, y) && spaced(x, y) },
            { x, y -> centreClear(x, y) && loose(x, y) },
            { x, y -> label(x, y) },
            { x, y -> labelOnly(x, y) },
        )
        for (ok in fallbacks) nearest(sx, sy, w, h, m, step, yMin, ok)?.let { return it }
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

    private fun nearest(sx: Float, sy: Float, w: Float, h: Float, r: Float, step: Float, yMin: Float, ok: (Float, Float) -> Boolean): Pair<Float, Float>? {
        var best: Pair<Float, Float>? = null
        var bestD = Float.MAX_VALUE
        var y = maxOf(r, yMin)
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

    /** The label chip's part of an area's core (what a tap on the label means). */
    /** True when ([x], [y]) is on a place's label chip (as the tap rule sees it). */
    fun labelChipAt(x: Float, y: Float, w: Float, h: Float, labelHalfW: Float, labelH: Float): Boolean =
        all.any { it.labelCore(w, h, labelHalfW, labelH).contains(x, y) }

    private fun MapArea.labelCore(w: Float, h: Float, labelHalfW: Float, labelH: Float) =
        Shape.Box(floatArrayOf(labelX * w - labelHalfW, labelY * h, labelX * w + labelHalfW, labelY * h + labelH))

    /** The parts of an area that are always its own: the picture (a circle) and the label chip. */
    private fun MapArea.core(w: Float, h: Float, labelHalfW: Float, labelH: Float, pictureShare: Float = 1f): List<Shape> = listOf(
        Shape.Circle(markX * w, markY * h, min(w, h) * LANDMARK_RADIUS * pictureShare),
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

