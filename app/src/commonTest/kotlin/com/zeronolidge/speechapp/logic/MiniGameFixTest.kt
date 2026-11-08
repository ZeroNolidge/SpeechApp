package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.logic.VoiceStories.FruitPicker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tap quizzes share one praise list. It must cheer the trying, and must not claim he talked. */
class SharedPraiseTest {
    @Test
    fun tapPraiseCheersTheTryingAndDoesNotClaimSpeech() {
        assertTrue(Praise.great.isNotEmpty())
        Praise.great.forEach { line ->
            val n = ClipText.normalize(line)
            assertFalse("talk" in n || "saying" in n || "heard you" in n, line)
            assertTrue(listOf("try", "trying", "work", "kept").any { it in n }, line)
        }
    }

    @Test
    fun aStoryFixClaimsTalkingOnlyAfterAVoiceAttempt() {
        val success = "Four! Good talking! One, two, three, four banana slices!"
        assertTrue("talk" in ClipText.normalize(StoryPraise.line(success, voice = true)))
        val quiet = StoryPraise.line(success, voice = false)
        assertFalse("talk" in ClipText.normalize(quiet), quiet)
        assertTrue(quiet.startsWith("Four!"))
        assertEquals("Good trying!", StoryPraise.line("Good talking!", voice = false))
    }

    @Test
    fun aTapWithNoVoiceDoesNotGetSpeechPraise() {
        TurnPraise.linesFor(heard = false, micUsed = false).forEach { line ->
            val n = ClipText.normalize(line)
            assertFalse("talk" in n || "saying" in n, line)
        }
        val tried = ClipText.normalize(TurnPraise.lineFor(heard = false, micUsed = true))
        assertFalse("saying it together" in tried || "your talking" in tried)
    }
}

/**
 * Letter R is said the same way as a working letter: the name and the sound are both in the
 * clip plan, and a device-voice fallback does not hand the browser a bare "r" / "rrr"
 * (those often come out silent).
 */
class LetterRNarrationTest {
    @Test
    fun letterRIsPlannedLikeAWorkingLetter() {
        val r = LetterContent.of('r')
        val b = LetterContent.of('b')
        val keys = setOf("r", "rrr", "b", "buh", "says", "rabbit", "ball")
        fun planKeys(text: String) = ClipText.plan(text) { it in keys }.filterIsInstance<ClipText.Part.Clips>().flatMap { it.keys }
        val rPlan = planKeys("${r.upper}. ${r.upper} says ${r.sound}.")
        val bPlan = planKeys("${b.upper}. ${b.upper} says ${b.sound}.")
        assertEquals(listOf("b", "b", "says", "buh"), bPlan.filter { it != ClipText.PAUSE })
        assertEquals(listOf("r", "r", "says", "rrr"), rPlan.filter { it != ClipText.PAUSE })
    }

    @Test
    fun deviceVoiceFallbackSaysTheRInsteadOfABareRun() {
        assertEquals("ar", ClipText.deviceFallback("r"))
        assertEquals("errr", ClipText.deviceFallback("rrr"))
        assertEquals("errr", ClipText.deviceFallback("rrrrr"))
        assertEquals("buh", ClipText.deviceFallback("buh"))
        assertEquals("b", ClipText.deviceFallback("b"))
        assertEquals("ar says errr.", ClipText.deviceFallbackPhrase("R says rrr."))
    }
}

/** A hop into the bowl counts only for the visit that scheduled it. */
class BowlLandingTest {
    @Test
    fun aDelayedHopFromThePreviousVisitDoesNotFillTheBowl() {
        assertEquals(null, BowlLanding.countIfCurrent(scheduledVisit = 1, currentVisit = 2, count = 5))
        assertEquals(3, BowlLanding.countIfCurrent(scheduledVisit = 2, currentVisit = 2, count = 3))
    }
}

/** Picking a fruit queues its prep. Pieces are in the bowl only after that prep, including a new play. */
class FruitPickReplayTest {
    @Test
    fun aNewPlayRunsPrepBeforePiecesGoInTheBowl() {
        fun openAndPick(fruit: FruitPicker.Fruit): FruitPick.Result {
            val fresh = FruitPick.pick(listOf(FruitPicker.picker), index = 0, picked = emptyList(), talks = 0, fruit = fruit)
            assertNotNull(fresh)
            return fresh
        }
        for (fruit in listOf(FruitPicker.Fruit.ORANGE, FruitPicker.Fruit.APPLE, FruitPicker.Fruit.GRAPE, FruitPicker.Fruit.BANANA)) {
            val first = openAndPick(fruit)
            val prep = first.beats[1]
            assertEquals(fruit, prep.fruitTag, fruit.name)
            assertTrue(prep.kitchen != null, "${fruit.name} prep was skipped")
            assertFalse(FruitPick.inBowl(first.beats, index = 0, phaseAct = false, fruit))
            // Prep finished (the fix is showing), then he leaves and opens a new play.
            assertTrue(FruitPick.inBowl(first.beats, index = 1, phaseAct = true, fruit))
            val again = openAndPick(fruit)
            assertTrue(again.beats[1].kitchen != null)
            assertFalse(FruitPick.inBowl(again.beats, index = 0, phaseAct = false, fruit))
        }
    }

    @Test
    fun partialAndFinishedPlaysBothStartClean() {
        val partial = FruitPick.pick(listOf(FruitPicker.picker), 0, emptyList(), 0, FruitPicker.Fruit.BANANA)!!
        assertEquals(listOf(KitchenFlow.SWIPE, KitchenFlow.DRAG, null), partial.beats.drop(1).dropLast(1).map { it.kitchen })
        val done = FruitPick.pick(
            listOf(FruitPicker.picker), 0,
            picked = listOf(FruitPicker.Fruit.BANANA, FruitPicker.Fruit.GRAPE),
            talks = 2,
            fruit = FruitPicker.Fruit.ORANGE,
        )!!
        assertEquals(KitchenFlow.COUNT, done.beats[1].kitchen)
        assertFalse(FruitPick.inBowl(done.beats, index = 0, phaseAct = true, FruitPicker.Fruit.ORANGE))
    }
}
