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
        val clippedStars = mutableListOf<String>()
        run {
            val narrow = sc.w < 600f
            // As in MapScreen (test54): the island fills the room between the corner buttons and the
            // Adventure bar; a phone's bunting hangs over the top [keep] dp of it.
            val fr = MapAreas.mapFrame(sc.w, sc.h)
            val mw = fr.w
            val mh = fr.h
            val keep = fr.keepTop
            val tapR = MapAreas.tapNode(mw) / 2f
            val label = MapAreas.labelSp(min(mw, mh))
            val halfW = MapAreas.labelHalfW(label)
            val labelH = MapAreas.labelH(label)
            for (chapter in 0..Trail.size) for (cave in listOf(false, true)) {
                val edge = MapAreas.stopEdge(96f)
                val lay = MapAreas.layout(
                    Trail.stopOf(chapter), chapter / Trail.size, mw, mh, 48f, if (cave) 38.4f else null, MapAreas.storyNode(mw) / 2f,
                    stories, MapAreas.starTap(min(mw, mh)) / 2f, halfW, labelH, stopEdge = edge, tapR = tapR, keepTop = keep,
                )
                // test52 (Ruby): the ▶'s pulsing ring keeps off every label and 16 dp inside the screen,
                // every tap circle is at least 44 dp across, and at most one star per place.
                val st = lay.stop
                if (MapAreas.touchesLabel(st.x, st.y, st.r * MapAreas.STOP_PULSE, mw, mh, halfW, labelH)) problems += "${sc.name} chapter $chapter cave=$cave: the ▶ ring on a label"
                val ring = st.r * MapAreas.STOP_PULSE
                if (st.x - ring < 0f || st.x + ring > mw || st.y - ring < keep) problems += "${sc.name} chapter $chapter: the ▶ ring at ${st.x.toInt()},${st.y.toInt()} is within 16 dp of the edge"
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
                        hero = floatArrayOf(heroL, heroT, heroL + 96f * 0.7f, heroT + 96f * 0.85f), top = keep)
                    val hb = hs.box(hand)
                    val hero = MapAreas.heroBox(st.x, st.y, 96f, heroOnRight, hs, hand)
                    val onHero = maxOf(0f, minOf(hb[2], hero[2]) - maxOf(hb[0], hero[0])) * maxOf(0f, minOf(hb[3], hero[3]) - maxOf(hb[1], hero[1]))
                    if (onHero > 0f) problems += "${sc.name} chapter $chapter: the hand on the buddy"
                    if (hero[0] < 0f || hero[2] > mw) problems += "${sc.name} chapter $chapter: the buddy off the map"
                    if (hb[0] < 0f || hb[1] < keep || hb[2] > mw || hb[3] > mh) problems += "${sc.name} chapter $chapter: the hand off the map"
                    MapAreas.all.forEach { a ->
                        val lb = MapAreas.labelBox(a, mw, mh, halfW, labelH)
                        if (hb[0] < lb[2] && hb[2] > lb[0] && hb[1] < lb[3] && hb[3] > lb[1]) problems += "${sc.name} chapter $chapter: the hand on the ${a.land} label"
                    }
                }
                // ...and Rory, in any place's fix moment, never stands on the next stop's ring.
                val rh = MapAreas.roryH(min(mw, mh))
                val stopRing = st.copy(r = st.r * MapAreas.STOP_PULSE)
                Land.entries.forEach { l ->
                    val (x, y) = MapAreas.roryBeside(l, mw, mh, rh * 0.72f, rh, halfW, labelH, avoid = listOf(stopRing), top = keep)
                    if (MapAreas.boxTouchesCircle(x, y, x + rh * 0.72f, y + rh, stopRing.x, stopRing.y, stopRing.r)) problems += "${sc.name} chapter $chapter: Rory on the ▶ for $l"
                }
                val spots = lay.stories + lay.done
                val c = lay.cave
                fun tapAt(x: Float, y: Float) = MapAreas.target(x, y, mw, mh, halfW, labelH, lay.stop.x, lay.stop.y, lay.stop.r, c?.x ?: 0f, c?.y ?: 0f, c?.r ?: 0f, spots, caveTap = c?.tap ?: 0f)
                lay.all.forEach { sp ->
                    if (MapAreas.touchesLabel(sp.x, sp.y, sp.r, mw, mh, halfW, labelH)) {
                        problems += "${sc.name} chapter $chapter cave=$cave: ${sp.target} at ${sp.x.toInt()},${sp.y.toInt()}"
                    }
                    // test54: nothing under a phone's bunting.
                    if (sp.y - sp.r < keep) problems += "${sc.name} chapter $chapter: ${sp.target} under the bunting"
                    // test54: every stop's and star's invisible tap circle is 64 dp on a phone, 80 on a tablet (or its drawn size, if bigger).
                    // (A star crowded beside another place's picture stops short of that picture's middle: counted, at most one.)
                    val clipped = sp.target is MapTarget.Done && MapAreas.all.any { m ->
                        kotlin.math.hypot(sp.x - m.markX * mw, sp.y - m.markY * mh) < tapR + min(mw, mh) * MapAreas.LANDMARK_RADIUS * 0.15f
                    }
                    if (clipped && sp.tap < tapR) clippedStars += "${sc.name} chapter $chapter cave=$cave: ${sp.target} ${(sp.tap * 2).toInt()} dp"
                    if (sp.target != MapTarget.Stop && sp.tap < maxOf(sp.r, tapR) && !clipped) problems += "${sc.name}: ${sp.target} taps only ${(sp.tap * 2).toInt()} dp"
                    // ...and a tap on its middle is its own.
                    val got = tapAt(sp.x, sp.y)
                    if (got != sp.target) taps += "${sc.name} chapter $chapter cave=$cave: ${sp.target} tap gives $got"
                }
                // test54: on every real screen each place's picture middle and label still open it
                // (Land pictures keep opening the land's games).
                MapAreas.all.forEach { a ->
                    val g1 = tapAt(a.markX * mw, a.markY * mh)
                    if (g1 != MapTarget.Area(a.land)) taps += "${sc.name} chapter $chapter cave=$cave: ${a.land}'s picture middle gives $g1"
                    val lb = MapAreas.labelBox(a, mw, mh, halfW, labelH)
                    val g2 = tapAt((lb[0] + lb[2]) / 2, (lb[1] + lb[3]) / 2)
                    if (g2 != MapTarget.Area(a.land)) taps += "${sc.name} chapter $chapter cave=$cave: ${a.land}'s label gives $g2"
                }
                // test54 (Ruby): the ▶'s whole circle is the ▶'s, and no stop's tap circle reaches into it.
                for (k in 0 until 24) for (f in listOf(0f, 0.5f, 0.98f)) {
                    val a = k * 2f * kotlin.math.PI.toFloat() / 24f
                    val got = tapAt(lay.stop.x + kotlin.math.cos(a) * lay.stop.r * f, lay.stop.y + kotlin.math.sin(a) * lay.stop.r * f)
                    if (got != MapTarget.Stop) taps += "${sc.name} chapter $chapter cave=$cave: the ▶ circle gives $got"
                }
                (listOfNotNull(c) + lay.stories).forEach { sp ->
                    val d = kotlin.math.sqrt((sp.x - lay.stop.x) * (sp.x - lay.stop.x) + (sp.y - lay.stop.y) * (sp.y - lay.stop.y))
                    if (d < sp.tap + lay.stop.r) problems += "${sc.name} chapter $chapter: ${sp.target}'s tap circle reaches into the ▶"
                }
                // test54 (Ruby): every point in a stop's tap circle goes to that stop or a nearer one
                // (distance over tap radius), never to a star. (A place's label and the ▶ come first.)
                val stopsHere = listOfNotNull(c) + lay.stories
                stopsHere.forEach { sp ->
                    for (k in 0 until 16) for (f in listOf(0.3f, 0.6f, 0.85f, 0.99f)) {
                        val a = k * 2f * kotlin.math.PI.toFloat() / 16f
                        val x = sp.x + kotlin.math.cos(a) * sp.tap * f
                        val y = sp.y + kotlin.math.sin(a) * sp.tap * f
                        val got = tapAt(x, y)
                        val ok = when (got) {
                            sp.target, MapTarget.Stop, is MapTarget.Area -> true
                            is MapTarget.Done, MapTarget.Sea -> false
                            else -> {
                                val o = stopsHere.first { it.target == got }
                                kotlin.math.hypot(x - o.x, y - o.y) / o.tap <= kotlin.math.hypot(x - sp.x, y - sp.y) / sp.tap
                            }
                        }
                        if (got is MapTarget.Area && !MapAreas.labelChipAt(x, y, mw, mh, halfW, labelH)) taps += "${sc.name} chapter $chapter: ${sp.target}'s tap circle gives $got off its label"
                        else if (!ok) taps += "${sc.name} chapter $chapter cave=$cave: ${sp.target}'s tap circle gives $got"
                    }
                }
            }
        }
        // Rory's spot in each place's fix moment: inside the map, on no label.
        run {
            val narrow = sc.w < 600f
            val fr = MapAreas.mapFrame(sc.w, sc.h)
            val mw = fr.w
            val mh = fr.h
            val label = MapAreas.labelSp(min(mw, mh))
            val halfW = MapAreas.labelHalfW(label)
            val labelH = MapAreas.labelH(label)
            val rh = MapAreas.roryH(min(mw, mh))
            val rw = rh * 0.72f
            Land.entries.forEach { l ->
                val (x, y) = MapAreas.roryBeside(l, mw, mh, rw, rh, halfW, labelH, top = fr.keepTop)
                if (x < 0f || y < fr.keepTop || x + rw > mw || y + rh > mh) problems += "${sc.name}: Rory off the map for $l"
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
        if (clippedStars.isNotEmpty()) println("MapLayoutTest ${sc.name}: stars with a clipped tap circle: " + clippedStars.joinToString("; "))
        assertTrue(clippedStars.size <= 4, "Too many stars with a clipped tap circle: $clippedStars")
        assertTrue(problems.isEmpty(), "Stops on labels (${problems.size}):\n" + problems.take(25).joinToString("\n"))
        assertTrue(taps.isEmpty(), "Stops whose middle isn't theirs (${taps.size}):\n" + taps.take(25).joinToString("\n"))
    }
}
