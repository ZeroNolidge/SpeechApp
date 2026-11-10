package com.zeronolidge.speechapp.logic

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ruby (test51): the next-stop ▶ and every other round stop on the map (the crystal cave, the
 * voice stories, the finished stops' stars) keep off every place's label, on phones and tablets,
 * at every point of the trail. Sizes in dp, worked out exactly as the map screen does.
 */
class MapLayoutTest {
    private data class Screen(val name: String, val w: Float, val h: Float)

    private val screens = listOf(
        Screen("iPhone 390x844", 390f, 844f), Screen("small phone 360x740", 360f, 740f), Screen("big phone 430x932", 430f, 932f),
        Screen("iPad landscape 1366x1024", 1366f, 1024f), Screen("iPad landscape 1024x768", 1024f, 768f),
        Screen("iPad portrait 820x1180", 820f, 1180f), Screen("iPad portrait 768x1024", 768f, 1024f),
    )

    @Test fun phone390() = check(screens[0])
    @Test fun phone360() = check(screens[1])
    @Test fun phone430() = check(screens[2])
    @Test fun ipad1366() = check(screens[3])
    @Test fun ipad1024() = check(screens[4])
    @Test fun ipadPortrait820() = check(screens[5])
    @Test fun ipadPortrait768() = check(screens[6])

    private fun check(sc: Screen) {
        val stories = VoiceStories.all.map { it.id to it.land }
        val problems = mutableListOf<String>()
        val taps = mutableListOf<String>()
        run {
            val narrow = sc.w < 600f
            // As in MapScreen: the bunting (96 + its height + 22) on top, the buttons below.
            val top = 96f + (if (narrow) 26f else 32f) + 22f
            val bottom = if (narrow) 176f else 90f
            val (mw, mh) = MapAreas.mapSize(sc.w, sc.h - top - bottom, narrow)
            val label = MapAreas.labelSp(min(mw, mh))
            val halfW = MapAreas.labelHalfW(label)
            val labelH = MapAreas.labelH(label)
            for (chapter in 0..Trail.size) for (cave in listOf(false, true)) {
                val edge = MapAreas.stopEdge(96f)
                val lay = MapAreas.layout(
                    Trail.stopOf(chapter), chapter / Trail.size, mw, mh, 48f, if (cave) 38.4f else null, MapAreas.storyNode(mw) / 2f,
                    stories, MapAreas.starTap(min(mw, mh)) / 2f, halfW, labelH, stopEdge = edge,
                )
                // test52 (Ruby): the ▶'s pulsing ring keeps off every label and 16 dp inside the screen,
                // every tap circle is at least 44 dp across, and at most one star per place.
                val st = lay.stop
                if (MapAreas.touchesLabel(st.x, st.y, st.r * MapAreas.STOP_PULSE, mw, mh, halfW, labelH)) problems += "${sc.name} chapter $chapter cave=$cave: the ▶ ring on a label"
                val ring = st.r * MapAreas.STOP_PULSE
                if (st.x - ring < 16f || st.x + ring > mw - 16f || st.y - ring < 0f) problems += "${sc.name} chapter $chapter: the ▶ ring at ${st.x.toInt()},${st.y.toInt()} is within 16 dp of the edge"
                lay.all.forEach { if (it.r * 2f < 44f) problems += "${sc.name}: ${it.target} only ${(it.r * 2).toInt()} dp" }
                val perLand = lay.done.groupBy { Trail.landAt((it.target as MapTarget.Done).stop) }
                perLand.forEach { (l, ss) -> if (ss.size > 1) problems += "${sc.name} chapter $chapter: ${ss.size} stars at $l" }
                // test53 (Ruby): the pointing hand is on screen and on no label (and off the buddy).
                run {
                    val heroOnRight = st.x / mw < 0.5f
                    val heroL = if (heroOnRight) st.x + 96f * 0.55f else st.x - 96f * 1.2f
                    val heroT = st.y - 96f * 0.6f
                    val hand = 96f * 0.55f
                    val hs = MapAreas.handSpot(st.x, st.y, st.r * MapAreas.STOP_PULSE, hand, heroOnRight, mw, mh, halfW, labelH,
                        hero = floatArrayOf(heroL, heroT, heroL + 96f * 0.7f, heroT + 96f * 0.85f))
                    val hb = hs.box(hand)
                    val hero = MapAreas.heroBox(st.x, st.y, 96f, heroOnRight, hs, hand)
                    val onHero = maxOf(0f, minOf(hb[2], hero[2]) - maxOf(hb[0], hero[0])) * maxOf(0f, minOf(hb[3], hero[3]) - maxOf(hb[1], hero[1]))
                    if (onHero > 0f) problems += "${sc.name} chapter $chapter: the hand on the buddy"
                    if (hero[0] < 0f || hero[2] > mw) problems += "${sc.name} chapter $chapter: the buddy off the map"
                    if (hb[0] < 0f || hb[1] < 0f || hb[2] > mw || hb[3] > mh) problems += "${sc.name} chapter $chapter: the hand off the map"
                    MapAreas.all.forEach { a ->
                        val lb = MapAreas.labelBox(a, mw, mh, halfW, labelH)
                        if (hb[0] < lb[2] && hb[2] > lb[0] && hb[1] < lb[3] && hb[3] > lb[1]) problems += "${sc.name} chapter $chapter: the hand on the ${a.land} label"
                    }
                }
                // ...and Rory, in any place's fix moment, never stands on the next stop's ring.
                val rh = MapAreas.roryH(min(mw, mh))
                val stopRing = st.copy(r = st.r * MapAreas.STOP_PULSE)
                Land.entries.forEach { l ->
                    val (x, y) = MapAreas.roryBeside(l, mw, mh, rh * 0.72f, rh, halfW, labelH, avoid = listOf(stopRing))
                    if (MapAreas.boxTouchesCircle(x, y, x + rh * 0.72f, y + rh, stopRing.x, stopRing.y, stopRing.r)) problems += "${sc.name} chapter $chapter: Rory on the ▶ for $l"
                }
                val spots = lay.stories + lay.done
                val c = lay.cave
                lay.all.forEach { sp ->
                    if (MapAreas.touchesLabel(sp.x, sp.y, sp.r, mw, mh, halfW, labelH)) {
                        problems += "${sc.name} chapter $chapter cave=$cave: ${sp.target} at ${sp.x.toInt()},${sp.y.toInt()}"
                    }
                    // ...and a tap on its middle is its own.
                    val got = MapAreas.target(sp.x, sp.y, mw, mh, halfW, labelH, lay.stop.x, lay.stop.y, lay.stop.r, c?.x ?: 0f, c?.y ?: 0f, c?.r ?: 0f, spots)
                    if (got != sp.target) taps += "${sc.name} chapter $chapter cave=$cave: ${sp.target} tap gives $got"
                }
            }
        }
        // Rory's spot in each place's fix moment: inside the map, on no label.
        run {
            val narrow = sc.w < 600f
            val top = 96f + (if (narrow) 26f else 32f) + 22f
            val bottom = if (narrow) 176f else 90f
            val (mw, mh) = MapAreas.mapSize(sc.w, sc.h - top - bottom, narrow)
            val label = MapAreas.labelSp(min(mw, mh))
            val halfW = MapAreas.labelHalfW(label)
            val labelH = MapAreas.labelH(label)
            val rh = MapAreas.roryH(min(mw, mh))
            val rw = rh * 0.72f
            Land.entries.forEach { l ->
                val (x, y) = MapAreas.roryBeside(l, mw, mh, rw, rh, halfW, labelH)
                if (x < 0f || y < 0f || x + rw > mw || y + rh > mh) problems += "${sc.name}: Rory off the map for $l"
                // Right next to the place (as near as fits by the castle in its corner): his box within 3.2 picture radii of its middle.
                val a = MapAreas.of(l)
                val nx = (a.markX * mw).coerceIn(x, x + rw)
                val ny = (a.markY * mh).coerceIn(y, y + rh)
                val d = kotlin.math.sqrt((nx - a.markX * mw) * (nx - a.markX * mw) + (ny - a.markY * mh) * (ny - a.markY * mh))
                if (d > min(mw, mh) * MapAreas.LANDMARK_RADIUS * 3.2f) problems += "${sc.name}: Rory not beside $l (at ${x.toInt()},${y.toInt()})"
                MapAreas.all.forEach { a ->
                    val b = MapAreas.labelBox(a, mw, mh, halfW, labelH)
                    if (x < b[2] && x + rw > b[0] && y < b[3] && y + rh > b[1]) problems += "${sc.name}: Rory on the ${a.land} label for $l"
                }
            }
        }
        assertTrue(problems.isEmpty(), "Stops on labels (${problems.size}):\n" + problems.take(25).joinToString("\n"))
        assertTrue(taps.isEmpty(), "Stops whose middle isn't theirs (${taps.size}):\n" + taps.take(25).joinToString("\n"))
    }
}
