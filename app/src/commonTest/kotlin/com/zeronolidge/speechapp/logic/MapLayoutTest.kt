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
                val lay = MapAreas.layout(
                    Trail.stopOf(chapter), chapter / Trail.size, mw, mh, 48f, if (cave) 38.4f else null, 33.6f,
                    stories, min(mw, mh) * 0.085f / 2f, halfW, labelH,
                )
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
        assertTrue(problems.isEmpty(), "Stops on labels (${problems.size}):\n" + problems.take(25).joinToString("\n"))
        assertTrue(taps.isEmpty(), "Stops whose middle isn't theirs (${taps.size}):\n" + taps.take(25).joinToString("\n"))
    }
}
