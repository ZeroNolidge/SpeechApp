package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.ReadingContent
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** test50: the missed-question review (logic/ReviewQueue.kt). */
class ReviewQueueTest {
    private fun which(word: String, seed: Int = 1): Challenge.WhichWord {
        val w = ReadingContent.cvcWords.first { it.text == word }
        return Challenge.WhichWord(Reading.make(listOf(w), Random(seed)).let { ReadRound(w, choiceOf(w, ReadingContent.cvcWords, 3, Random(seed))) })
    }

    private fun answerAt(c: Challenge): Int = AnswerKey.of(c)!!.let { it.options.indexOf(it.answer) }

    /** Plays rounds [from]..[to] as new, right-first-time questions; returns the round a repeat came. */
    private fun firstRepeat(q: ReviewQueue, from: Int, to: Int): Pair<Int, ReviewQueue.Review>? {
        for (r in from..to) {
            val rv = q.next(r)
            if (rv != null) return r to rv
            q.result(r, which("dog"), missed = false)
        }
        return null
    }

    @Test
    fun aMissComesBackTwoOrThreeRoundsLaterNeverTheNextRound() {
        repeat(40) { seed ->
            val q = ReviewQueue(Random(seed))
            assertNull(q.next(0))
            q.result(0, which("cat"), missed = true)
            val (round, rv) = firstRepeat(q, 1, 10)!!
            assertTrue(round in 2..3, "came back at round $round")
            assertEquals(ReviewQueue.Stage.RETRY, rv.stage)
        }
    }

    @Test
    fun theRetryHasTwoChoicesTheAnswerMovedAndANewWrongChoice() {
        repeat(40) { seed ->
            val q = ReviewQueue(Random(seed))
            val c = which("cat", seed)
            q.result(0, c, missed = true)
            val rv = firstRepeat(q, 1, 5)!!.second
            val k = AnswerKey.of(rv.challenge)!!
            assertEquals(2, k.options.size)
            assertEquals(AnswerKey.of(c)!!.answer, k.answer, "same question")
            assertNotEquals(answerAt(c), answerAt(rv.challenge), "answer in a new spot")
            val oldWrong = AnswerKey.of(c)!!.options.filter { it != k.answer }
            assertTrue(k.options.filter { it != k.answer }.any { it !in oldWrong }, "a new wrong choice")
            assertTrue(rv.sayTwice, "Rory says the question once more")
        }
    }

    @Test
    fun aRightRetryComesBackAtTheEndWithTheUsualChoicesShuffledAgain() {
        repeat(30) { seed ->
            val q = ReviewQueue(Random(seed))
            val c = which("cat", seed)
            q.result(0, c, missed = true)
            val (r, retry) = firstRepeat(q, 1, 5)!!
            q.result(r, retry.challenge, missed = false, review = retry)
            assertTrue(q.hasRecheck())
            // Not straight away...
            assertNull(q.next(r + 1))
            q.result(r + 1, which("dog"), missed = false)
            // ...but as soon as the game wraps up.
            val end = q.next(r + 2, ending = true)
            assertNotNull(end)
            assertEquals(ReviewQueue.Stage.RECHECK, end.stage)
            assertFalse(end.sayTwice)
            val k = AnswerKey.of(end.challenge)!!
            assertEquals(3, k.options.size, "usual number of choices")
            assertEquals(AnswerKey.of(c)!!.answer, k.answer)
            assertNotEquals(answerAt(retry.challenge), answerAt(end.challenge), "moved again")
            val retryWrong = AnswerKey.of(retry.challenge)!!.options.filter { it != k.answer }
            assertTrue(k.options.filter { it != k.answer }.any { it !in retryWrong }, "new wrong choices")
            q.result(r + 2, end.challenge, missed = false, review = end)
            assertTrue(q.waiting.isEmpty())
            // Done: the same question missed again today doesn't come back a third time.
            q.result(r + 3, c, missed = true)
            assertTrue(q.waiting.isEmpty())
        }
    }

    @Test
    fun theEndCheckAlsoComesIfHeKeepsPlaying() {
        val q = ReviewQueue(Random(3))
        q.result(0, which("cat"), missed = true)
        val (r, retry) = firstRepeat(q, 1, 5)!!
        q.result(r, retry.challenge, missed = false, review = retry)
        val (r2, end) = firstRepeat(q, r + 1, r + 20)!!
        assertEquals(r + ReviewQueue.RECHECK_GAP, r2)
        assertEquals(ReviewQueue.Stage.RECHECK, end.stage)
    }

    @Test
    fun aMissedRetryIsDroppedForTheDay() {
        val dropped = ReviewQueue.droppedToday(20000)
        val q = ReviewQueue(Random(5), dropped)
        val c = which("cat")
        q.result(0, c, missed = true)
        val (r, retry) = firstRepeat(q, 1, 5)!!
        q.result(r, retry.challenge, missed = true, review = retry)
        assertTrue(q.waiting.isEmpty(), "no looping")
        assertTrue(q.isDropped(Rebuild.keyOf(c)!!))
        q.result(r + 1, c, missed = true)
        assertTrue(q.waiting.isEmpty(), "not queued again today")
        // A new visit to the game the same day remembers it; the next day it can come back.
        val again = ReviewQueue(Random(6), ReviewQueue.droppedToday(20000))
        again.result(0, c, missed = true)
        assertTrue(again.waiting.isEmpty())
        val tomorrow = ReviewQueue(Random(6), ReviewQueue.droppedToday(20001))
        tomorrow.result(0, c, missed = true)
        assertEquals(1, tomorrow.waiting.size)
    }

    @Test
    fun atMostTwoWaitingAndOneRepeatARound() {
        val q = ReviewQueue(Random(7))
        q.result(0, which("cat"), missed = true)
        q.result(0, which("sun"), missed = true)
        q.result(0, which("pig"), missed = true)
        assertEquals(2, q.waiting.size, "a third miss isn't queued")
        // Both are due by round 3, but only one plays a round.
        var served = 0
        for (r in 1..8) {
            val a = q.next(r)
            assertNull(q.next(r), "never two repeats in one round")
            if (a != null) { served++; q.result(r, a.challenge, missed = true, review = a) } else q.result(r, which("dog"), missed = false)
        }
        assertEquals(2, served)
    }

    @Test
    fun everyTapChoiceGameCanBeRebuiltAndTheRobotCannot() {
        val rnd = Random(11)
        val games = listOf(
            Challenge.Count(Numbers.count(10, rnd)),
            Challenge.Math(Numbers.math(true, 10, rnd)),
            Challenge.Math(Numbers.math(false, 10, rnd)),
            Challenge.More(Numbers.compare(8, rnd)),
            Challenge.Letter(Letters.make(LetterMode.BIG_TO_SMALL, rnd, target = LetterContent.of('b'))),
            Challenge.Letter(Letters.make(LetterMode.FIRST_SOUND, rnd)),
            Challenge.SoundOut(Reading.make(ReadingContent.cvcWords, rnd)),
            which("cat"),
            Challenge.Pattern(Patterns.makeStep(2, rnd)),
            Challenge.OddOne(OddOneOut.make(0, rnd)),
            Challenge.OddOne(OddOneOut.make(1, rnd)),
            Challenge.Coins(CoinRound("🍎", 3, CoinMode.PURSE, choiceOf(3, (1..5).toList(), 3, rnd))),
        )
        for (c in games) repeat(20) { s ->
            val r = Rebuild.retry(c, Random(s))
            assertNotNull(r, "$c")
            val k = AnswerKey.of(r)!!
            val old = AnswerKey.of(c)!!
            if (c is Challenge.OddOne) {
                assertEquals(3, k.options.size, "odd one out: the fewest pictures that still make the question")
                assertEquals(c.round.odd, r.let { it as Challenge.OddOne }.round.odd)
                val oldAlike = c.round.items.filter { it != c.round.odd }.toSet()
                assertTrue((r as Challenge.OddOne).round.items.filter { it != c.round.odd }.any { it !in oldAlike }, "new look-alikes: $c -> $r")
            } else {
                assertEquals(2, k.options.size, "$c")
                // Which side has more: the answer is a side, so it moves; the bigger pile stays the same.
                if (c is Challenge.More) assertEquals(maxOf(c.round.left, c.round.right), (r as Challenge.More).round.let { maxOf(it.left, it.right) })
                else assertEquals(old.answer, k.answer, "same answer $c")
            }
            if (c is Challenge.More) assertTrue(c.round.moreIsLeft != (r as Challenge.More).round.moreIsLeft, "bigger pile swaps sides $c $r")
            else assertTrue(old.options.indexOf(old.answer) != k.options.indexOf(k.answer), "answer moved: $c -> $r")
        }
        assertNull(Rebuild.retry(Challenge.Robot(RobotLevels.random(false, rnd), "🍪"), rnd))
        assertNull(Rebuild.retry(Challenge.Coins(CoinRound("🍎", 3, CoinMode.PAY, choiceOf(3, (1..5).toList(), 3, rnd))), rnd))
        val q = ReviewQueue(rnd)
        q.result(0, Challenge.Robot(RobotLevels.random(false, rnd), "🍪"), missed = true)
        assertTrue(q.waiting.isEmpty(), "robot mazes aren't queued")
    }
}
