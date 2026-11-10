package com.zeronolidge.speechapp.logic

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** test53 (Aida, Ruby): every finished trail stop can be played again from its place's one ⭐. */
class ReplayPickerTest {
    @Test
    fun everyFinishedStopIsOnItsPlacesCardAndEachPlaceHasOneStar() {
        for (chapter in 0..Trail.size * 2) {
            val current = Trail.stopOf(chapter)
            val lap = chapter / Trail.size
            val finished = (0 until Trail.size).filter { (lap > 0 || it < current) && it != current }.toSet()
            val onCards = Land.entries.flatMap { ReplayPicker.finishedAt(it, chapter) }
            assertEquals(finished, onCards.toSet(), "chapter $chapter")
            assertEquals(onCards.size, onCards.toSet().size, "a stop on two cards, chapter $chapter")
            Land.entries.forEach { l -> assertTrue(ReplayPicker.finishedAt(l, chapter).all { Trail.landAt(it) == l }) }
            // The map's stars: exactly one at every place with a finished stop, and the star is one of them.
            val lay = MapAreas.layout(current, lap, 1366f, 784f, 48f, null, 33.6f, emptyList(), 22f, 78f, 38.5f)
            val starLands = lay.done.map { Trail.landAt((it.target as MapTarget.Done).stop) }
            assertEquals(starLands.size, starLands.toSet().size, "two stars at one place, chapter $chapter")
            assertEquals(Land.entries.filter { ReplayPicker.finishedAt(it, chapter).isNotEmpty() }.toSet(), starLands.toSet(), "chapter $chapter")
        }
        // A place's second finished stop is on the card too (Meadow, once both are done).
        assertEquals(listOf(2, 3), ReplayPicker.finishedAt(Land.MEADOW, 4))
        assertEquals(listOf(2, 3), ReplayPicker.finishedAt(Land.MEADOW, Trail.size + 1))
    }

    @Test
    fun eachTileIsNamedByTheStoryItReallyPlays() {
        Plots.all.forEach { p ->
            assertTrue(ReplayPicker.hasName(p), "no name for ${p.id}")
            val n = ReplayPicker.name(p)
            assertEquals("Or " + n.first().lowercase() + n.drop(1), ReplayPicker.orName(p))
            assertTrue(n.endsWith("!") && n.split(" ").size <= 4)
        }
        assertEquals(Plots.all.size, Plots.all.map { ReplayPicker.name(it) }.toSet().size)
        // A saved plot is that stop's story; a stop finished before test53 gets a stand-in that
        // differs from the other stop at its place.
        assertEquals("teddy", ReplayPicker.plotFor(3, "teddy").id)
        for (s in 0 until Trail.size) assertNotEquals(ReplayPicker.plotFor(s, null).id, ReplayPicker.plotFor(ReplayPicker.sibling(s), null).id)
        val two = listOf(Plots.all.first { it.id == "balloon" }, Plots.all.first { it.id == "teddy" })
        assertEquals("The red balloon! Or the lost teddy!", ReplayPicker.line(two))
        // A new trail stop never repeats the other stop's story at its place.
        repeat(50) { seed ->
            val q = StoryGen.make(Land.MEADOW, StoryParams("Sam"), kotlin.random.Random(seed), avoid = setOf("balloon"))
            assertNotEquals("balloon", q.plot.id)
        }
        val replay = StoryGen.make(Land.MEADOW, StoryParams("Sam"), kotlin.random.Random(1), plot = two[1])
        assertEquals("teddy", replay.plot.id)
    }

    @Test
    fun theCardFitsBesideEveryPlaceWithBigTilesAndAClearClose() {
        val screens = listOf(390f to 844f, 360f to 740f, 430f to 932f, 1366f to 1024f, 1024f to 768f, 820f to 1180f, 768f to 1024f)
        for ((sw, sh) in screens) {
            val narrow = sw < 600f
            val top = 96f + (if (narrow) 26f else 32f) + 22f
            val bottom = if (narrow) 176f else 90f
            val (mw, mh) = MapAreas.mapSize(sw, sh - top - bottom, narrow)
            val mapLeft = (sw - mw) / 2
            val mapTop = (sh - mh) / 2 + (top - bottom) / 2
            for (a in MapAreas.all) for (n in 1..Trail.STOPS_PER_LAND) {
                val c = ReplayPicker.card(n, mapLeft + a.markX * mw, mapTop + a.markY * mh, min(mw, mh) * MapAreas.LANDMARK_RADIUS, sw, sh, narrow)
                val where = "${a.land} n=$n on ${sw.toInt()}x${sh.toInt()}"
                assertTrue(c.box[0] >= 0f && c.box[1] >= 0f && c.box[2] <= sw && c.box[3] <= sh, "card off screen: $where")
                assertEquals(n, c.tiles.size)
                c.tiles.forEach { t ->
                    assertTrue(t[3] - t[1] >= (if (narrow) 100f else 64f) && t[2] - t[0] >= 64f, "small tile: $where")
                    assertTrue(t[0] >= c.box[0] && t[2] <= c.box[2] && t[1] >= c.box[1] && t[3] <= c.box[3], "tile outside the card: $where")
                    assertTrue(t[2] <= c.close[0] || t[0] >= c.close[2] || t[1] >= c.close[3] || t[3] <= c.close[1], "tile under the ✕: $where")
                }
                for (i in c.tiles.indices) for (j in i + 1 until c.tiles.size) assertTrue(c.tiles[i][2] <= c.tiles[j][0], "tiles overlap: $where")
                assertTrue(c.close[2] - c.close[0] >= 48f && c.close[3] - c.close[1] >= 48f, "small ✕: $where")
                // Right beside the place: the card's nearest edge within a picture's width of it.
                val px = mapLeft + a.markX * mw
                val py = mapTop + a.markY * mh
                val dx = maxOf(c.box[0] - px, 0f, px - c.box[2])
                val dy = maxOf(c.box[1] - py, 0f, py - c.box[3])
                assertTrue(kotlin.math.sqrt(dx * dx + dy * dy) <= min(mw, mh) * MapAreas.LANDMARK_RADIUS * 1.2f, "card far from the place: $where")
            }
        }
    }
}
