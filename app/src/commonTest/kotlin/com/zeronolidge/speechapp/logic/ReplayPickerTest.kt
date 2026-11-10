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
    fun theCardFitsClearOfTheAdventureBarAndTheNextStop() {
        val screens = listOf(390f to 844f, 360f to 740f, 430f to 932f, 1366f to 1024f, 1024f to 768f, 820f to 1180f, 768f to 1024f)
        val stories = VoiceStories.all.map { it.id to it.land }
        val problems = mutableListOf<String>()
        for ((sw, sh) in screens) {
            val narrow = sw < 600f
            val top = 96f + (if (narrow) 26f else 32f) + 22f
            val bottom = if (narrow) 176f else 90f
            val (mw, mh) = MapAreas.mapSize(sw, sh - top - bottom, narrow)
            val mapLeft = (sw - mw) / 2
            val mapTop = (sh - mh) / 2 + (top - bottom) / 2
            val label = MapAreas.labelSp(min(mw, mh))
            val halfW = MapAreas.labelHalfW(label)
            val labelH = MapAreas.labelH(label)
            val bar = ReplayPicker.adventureBar(sw, sh, narrow)
            val pictureR = min(mw, mh) * MapAreas.LANDMARK_RADIUS
            for (chapter in 0..Trail.size * 2) for (cave in listOf(false, true)) {
                // The next stop exactly as the map places it (the same call and sizes as MapLayoutTest).
                val st = MapAreas.layout(
                    Trail.stopOf(chapter), chapter / Trail.size, mw, mh, 48f, if (cave) 38.4f else null, MapAreas.storyNode(mw) / 2f,
                    stories, MapAreas.starTap(min(mw, mh)) / 2f, halfW, labelH, stopEdge = MapAreas.stopEdge(96f),
                ).stop
                val ring = floatArrayOf(mapLeft + st.x, mapTop + st.y, st.r * MapAreas.STOP_PULSE)
                for (a in MapAreas.all) for (n in 1..Trail.STOPS_PER_LAND) {
                    val px = mapLeft + a.markX * mw
                    val py = mapTop + a.markY * mh
                    val c = ReplayPicker.card(n, px, py, pictureR, sw, sh, narrow, ring)
                    val b = c.box
                    val where = "${a.land} n=$n chapter $chapter on ${sw.toInt()}x${sh.toInt()}"
                    if (b[0] < 0f || b[1] < 0f || b[2] > sw || b[3] > sh) problems += "card off screen: $where"
                    // Ruby (test53b): never on the Adventure bar, 16 dp clear of it all round...
                    if (b[0] < bar[2] + 16f && b[2] > bar[0] - 16f && b[1] < bar[3] + 16f && b[3] > bar[1] - 16f) problems += "card within 16 dp of the Adventure bar: $where"
                    // ...and never on the next stop's ▶ or its pulsing ring.
                    val dx = maxOf(b[0] - ring[0], 0f, ring[0] - b[2])
                    val dy = maxOf(b[1] - ring[1], 0f, ring[1] - b[3])
                    if (dx * dx + dy * dy < ring[2] * ring[2]) problems += "card on the ▶ ring: $where"
                    if (narrow) {
                        // Phone: centred, just above the bar (higher only to clear the ▶).
                        if (kotlin.math.abs((b[0] + b[2]) / 2 - sw / 2) > 0.5f) problems += "card not centred: $where"
                        if (b[3] > bar[1] - 16f + 0.01f) problems += "card not above the bar: $where"
                    } else {
                        // Tablet: beside the tapped place.
                        val ex = maxOf(b[0] - px, 0f, px - b[2])
                        val ey = maxOf(b[1] - py, 0f, py - b[3])
                        if (kotlin.math.sqrt(ex * ex + ey * ey) > pictureR * 2.5f) problems += "card far from the place: $where"
                    }
                    assertEquals(n, c.tiles.size)
                    c.tiles.forEach { t ->
                        if (t[3] - t[1] < (if (narrow) 100f else 64f) || t[2] - t[0] < 64f) problems += "small tile: $where"
                        if (!(t[0] >= b[0] && t[2] <= b[2] && t[1] >= b[1] && t[3] <= b[3])) problems += "tile outside the card: $where"
                        if (!(t[2] <= c.close[0] || t[0] >= c.close[2] || t[1] >= c.close[3] || t[3] <= c.close[1])) problems += "tile under the ✕: $where"
                    }
                    for (i in c.tiles.indices) for (j in i + 1 until c.tiles.size) if (c.tiles[i][2] > c.tiles[j][0]) problems += "tiles overlap: $where"
                    if (c.close[2] - c.close[0] < 48f || c.close[3] - c.close[1] < 48f) problems += "small ✕: $where"
                }
            }
        }
        assertTrue(problems.isEmpty(), "Replay card problems (${problems.size}):\n" + problems.distinct().take(30).joinToString("\n"))
    }
}
