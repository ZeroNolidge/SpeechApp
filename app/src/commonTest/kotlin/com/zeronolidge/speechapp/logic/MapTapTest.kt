package com.zeronolidge.speechapp.logic

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test54 (Ruby): the map's tap order with the bigger invisible tap circles. A label first, then
 * the ▶ (always its own whole circle), then the nearest round stop (distance over its tap
 * radius), then a star only outside every stop's tap circle, then the place itself.
 */
class MapTapTest {
    private val w = 358f
    private val h = 554f
    private val label = MapAreas.labelSp(min(w, h))
    private val half = MapAreas.labelHalfW(label)
    private val lh = MapAreas.labelH(label)

    // Out in the open sea corner, away from every place, so only the round things matter.
    private val stop = MapSpot(MapTarget.Stop, 60f, 520f, 48f)
    private val story = MapSpot(MapTarget.Story("boat"), 60f + 48f + 32f, 520f, 26f, 32f)
    private val story2 = MapSpot(MapTarget.Story("owl"), 60f + 48f + 32f + 50f, 520f, 26f, 32f)
    private val star = MapSpot(MapTarget.Done(3), 60f + 48f + 32f + 24f, 520f - 40f, 22f, 32f)

    private fun tap(x: Float, y: Float) =
        MapAreas.target(x, y, w, h, half, lh, stop.x, stop.y, stop.r, spots = listOf(story, story2, star))

    @Test fun thePlayButtonAlwaysWinsItsWholeCircle() {
        for (k in 0 until 36) for (f in listOf(0f, 0.4f, 0.8f, 0.99f)) {
            val a = k * 10f * kotlin.math.PI.toFloat() / 180f
            assertEquals(MapTarget.Stop, tap(stop.x + cos(a) * stop.r * f, stop.y + sin(a) * stop.r * f), "▶ at angle ${k * 10}, ${f}r")
        }
    }

    @Test fun aStopsTapCircleGoesToItOrANearerStopNeverAStar() {
        for (sp in listOf(story, story2)) for (k in 0 until 36) for (f in listOf(0.2f, 0.5f, 0.8f, 0.99f)) {
            val a = k * 10f * kotlin.math.PI.toFloat() / 180f
            val x = sp.x + cos(a) * sp.tap * f
            val y = sp.y + sin(a) * sp.tap * f
            val got = tap(x, y)
            assertTrue(got !is MapTarget.Done, "${sp.target} tap circle gave a star at $x,$y")
            if (got is MapTarget.Story && got != sp.target) {
                val o = listOf(story, story2).first { it.target == got }
                assertTrue(kotlin.math.hypot(x - o.x, y - o.y) / o.tap <= kotlin.math.hypot(x - sp.x, y - sp.y) / sp.tap, "a farther stop won at $x,$y")
            }
            if (got == MapTarget.Stop) assertTrue(kotlin.math.hypot(x - stop.x, y - stop.y) <= stop.r)
        }
    }

    @Test fun aStarTakesTheRestOfItsCircleAndBiggerTapsThanItsDrawing() {
        // Its own middle is outside the stop's tap circle here, so it's the star's...
        assertEquals(MapTarget.Done(3), tap(star.x, star.y - 10f))
        // ...a point beyond its drawn star but inside its tap circle is still the star's...
        assertEquals(MapTarget.Done(3), tap(star.x, star.y - star.r - 6f))
        // ...and where the star's and the story's circles overlap, the story wins.
        assertEquals(MapTarget.Story("boat"), tap(story.x + 6f, story.y - 20f))
    }

    @Test fun phoneAndTabletTapSizes() {
        assertEquals(64f, MapAreas.tapNode(358f))
        assertEquals(80f, MapAreas.tapNode(992f))
        // The drawn sizes stay as they were.
        assertEquals(52f, MapAreas.storyNode(358f))
        assertEquals(67.2f, MapAreas.storyNode(992f))
        assertEquals(44f, MapAreas.starTap(358f))
    }

    @Test fun theIslandFillsTheRoomBetweenTheBars() {
        val phone = MapAreas.mapFrame(390f, 844f)
        assertEquals(16f, phone.left); assertEquals(358f, phone.w)
        assertEquals(MapAreas.TOP_BAR + MapAreas.MAP_GAP, phone.top)
        assertEquals(844f - phone.top - MapAreas.adventureTop(true) - MapAreas.MAP_GAP, phone.h)
        assertTrue(phone.keepTop > 0f, "a phone's bunting hangs over the top of the map")
        val ipad = MapAreas.mapFrame(1024f, 768f)
        assertEquals(992f, ipad.w)
        assertEquals(0f, ipad.keepTop)
        assertEquals(768f - ipad.top - MapAreas.adventureTop(false) - MapAreas.MAP_GAP, ipad.h)
    }
}
