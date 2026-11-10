package com.zeronolidge.speechapp.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandStoryTest {
    @Test
    fun everyTrailPlaceHasOneLineOneCostumeAndOneFlag() {
        assertEquals(Trail.lands, IslandStory.places.map { it.land })
        assertEquals(8, IslandStory.places.map { it.costume.id }.toSet().size)
        assertEquals(8, IslandStory.places.map { it.flag }.toSet().size)
        IslandStory.places.forEach {
            assertTrue(it.costume.id.startsWith("costume_"))
            // Short lines (well under ten seconds spoken), no reading needed.
            assertTrue(it.before.split(" ").size <= 12, it.before)
            assertTrue(it.after.split(" ").size <= 12, it.after)
        }
    }

    @Test
    fun partyOnlyOnTheFixThatFillsTheLastFlag() {
        val seven = Trail.lands.take(7).toSet()
        assertTrue(IslandStory.partyNow(seven, Trail.lands.toSet()))
        assertFalse(IslandStory.partyNow(Trail.lands.toSet(), Trail.lands.toSet()))
        assertFalse(IslandStory.partyNow(emptySet(), setOf(Land.FOREST)))
    }
}
