package com.zeronolidge.speechapp.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** test59: the skill trees' rules (logic/SkillTree.kt). */
class SkillTreeTest {
    private val count = SkillTree.byId("game:COUNT")!!
    private val f = SkillTree.byId("talk:f")!!

    private fun answers(s: SkillState, node: SkillNode, day: Int, right: Int, wrong: Int): SkillState {
        var x = s
        repeat(right) { x = SkillTree.record(x, node, day, true) }
        repeat(wrong) { x = SkillTree.record(x, node, day, false) }
        return x
    }

    @Test fun talkingOrderIsTheTeachingOrder() {
        assertEquals(listOf("p", "b", "m", "t", "d", "k", "g", "f", "sh", "s", "l", "v", "ch", "sp"), SkillTree.of(SkillBranch.TALKING).map { it.fruit })
        assertEquals(SkillTier.START, SkillTree.byId("talk:g")!!.tier)
        assertEquals(SkillTier.NEXT, SkillTree.byId("talk:ch")!!.tier)
        assertEquals(SkillTier.STRETCH, SkillTree.byId("talk:blends")!!.tier)
        assertEquals(6, SkillTree.LEVELS.size)
        assertEquals(listOf("ABC", "A to a", "a to A", "First sound", "Which word?", "Sound it out"), SkillTree.of(SkillBranch.LETTERS).map { it.label })
        assertEquals(listOf("1 2 3", "Count", "More?", "Add", "Take away", "Market"), SkillTree.of(SkillBranch.NUMBERS).map { it.label })
        assertEquals(listOf("Odd one out", "What's next?", "Robot arrows", "Robot turns"), SkillTree.of(SkillBranch.THINKING).map { it.label })
        assertEquals(VoiceStories.all.size, SkillTree.of(SkillBranch.STORIES).size)
    }

    @Test fun everyTapGameHasANodeAndEveryNodeIdIsUnique() {
        val ids = SkillTree.nodes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        listOf("BIG_SMALL", "SMALL_BIG", "FIRST_SOUND", "SOUND_OUT", "WHICH_WORD", "COUNT", "MORE", "ADD", "TAKE_AWAY", "NEXT", "ODD", "ROBOT_ARROWS", "ROBOT_TURNS", "COINS")
            .forEach { assertTrue(SkillTree.byId("game:$it") != null, it) }
    }

    @Test fun tapGameMastersAtEightyPercentOnTwoDays() {
        var s = answers(SkillState(), count, 100, 8, 2)
        assertFalse(s.mastered, "one good day is not enough")
        s = answers(s, count, 101, 3, 2) // 60%
        assertFalse(s.mastered)
        s = answers(s, count, 102, 4, 1) // 80% on a second day
        assertTrue(s.mastered)
        assertEquals(2, SkillTree.goodDays(s))
    }

    @Test fun tooFewAnswersInADayDoNotCount() {
        var s = answers(SkillState(), count, 100, 4, 0)
        s = answers(s, count, 101, 4, 0)
        assertFalse(s.mastered)
    }

    @Test fun triesNeverMasterASound() {
        var s = SkillState()
        repeat(500) { s = SkillTree.record(s, f, 100 + it / 50, null) }
        assertFalse(s.mastered)
        assertEquals(SkillTree.GROW_CAP, SkillTree.fruitGrowth(s))
        assertEquals(1f, SkillTree.fruitGrowth(s.copy(mastered = true)))
    }

    @Test fun fruitGrowsWithTriesAndNeverShrinks() {
        var s = SkillState()
        var last = 0f
        repeat(40) {
            s = SkillTree.record(s, count, 100 + it * 3, it % 3 == 0)
            val g = SkillTree.fruitGrowth(s)
            assertTrue(g >= last)
            last = g
        }
        // Days away change nothing: leaves are tries, and tries are never taken back.
        val states = mapOf(count.id to s)
        assertEquals(40, SkillTree.leaves(SkillBranch.NUMBERS, states))
    }

    @Test fun currentIsTheLatestUnmasteredOne() {
        assertEquals("talk:p", SkillTree.current(SkillBranch.TALKING, emptyMap())?.id)
        var st = mapOf("talk:p" to SkillState(mastered = true), "talk:b" to SkillState(mastered = true))
        assertEquals("talk:m", SkillTree.current(SkillBranch.TALKING, st)?.id)
        st = st + ("talk:f" to SkillTree.record(SkillState(), f, 200, null))
        assertEquals("talk:f", SkillTree.current(SkillBranch.TALKING, st)?.id)
        assertEquals(SkillStatus.MASTERED, SkillTree.status(SkillTree.byId("talk:p")!!, st))
        assertEquals(SkillStatus.CURRENT, SkillTree.status(f, st))
        assertEquals(SkillStatus.UPCOMING, SkillTree.status(SkillTree.byId("talk:m")!!, st))
        val all = SkillTree.of(SkillBranch.THINKING).associate { it.id to SkillState(mastered = true) }
        assertNull(SkillTree.current(SkillBranch.THINKING, all))
    }

    @Test fun weekLine() {
        var s = SkillState(levels = setOf(0))
        for (d in 0 until 4) repeat(10) { s = SkillTree.record(s, f, 300 + d, null) }
        s = SkillTree.record(s, f, 300 + 3, null).let { it } // one more on the last day
        val st = (listOf("p", "b", "m", "t", "d", "k", "g").associate { "talk:$it" to SkillState(mastered = true) }) + (f.id to s)
        assertEquals("This week: 41 tries on /f/ over 4 days. Next up: /f/ at the start of words.", SkillTree.weekSummary(SkillBranch.TALKING, st, 303))
        assertEquals("No tries on /f/ this week yet. Next up: /f/ at the start of words.", SkillTree.weekSummary(SkillBranch.TALKING, st, 320))
        assertEquals("This week: 1 try on Count over 1 day. Next up: More?.".replace("?.", "?."), SkillTree.weekSummary(SkillBranch.NUMBERS, mapOf(count.id to SkillTree.record(SkillState(), count, 5, true)), 5))
    }

    @Test fun savesAndLoadsWithNoNamesOrWords() {
        var s = answers(SkillState(levels = setOf(0, 3)), count, 100, 3, 1)
        s = answers(s, count, 101, 2, 0)
        val st = mapOf(count.id to s, "story:boat" to SkillState(mastered = true), "nonsense" to SkillState(tries = 3))
        val code = SkillTree.encodeAll(st)
        assertFalse("nonsense" in code)
        assertEquals(st - "nonsense", SkillTree.decodeAll(code))
        assertTrue(SkillTree.decodeAll("garbage\nx=1;2").isEmpty())
        // Only ids, counts and day numbers are kept.
        assertTrue(code.all { it.isLetterOrDigit() || it in ":;,=_\n" }, code)
    }

    @Test fun saidWordsFindTheirSound() {
        assertEquals("talk:f", SkillTree.talkNodeFor("fish")?.id)
        assertEquals("talk:sh", SkillTree.talkNodeFor("shoe")?.id)
        assertEquals("talk:blends", SkillTree.talkNodeFor("spoon")?.id)
        assertEquals("talk:m", SkillTree.talkNodeFor("More cake, please!", "m")?.id)
        assertEquals("talk:b", SkillTree.talkNodeFor("bubble")?.id)
        assertNull(SkillTree.talkNodeFor("rain")) // r isn't on the tree yet
    }

    @Test fun roryNamesWhatWasPractisedWithProcessPraiseOnly() {
        val st = mapOf(f.id to SkillTree.record(SkillState(), f, 10, null))
        assertEquals("You practiced your fff sound! Every try grew a new leaf.", SkillTree.openLine(SkillBranch.TALKING, st))
        assertEquals("This is your numbers tree. Every try grows a new leaf!", SkillTree.openLine(SkillBranch.NUMBERS, st))
        val lines = SkillTree.nodes.flatMap { n ->
            listOf(SkillTree.fruitLine(n, emptyMap()), SkillTree.fruitLine(n, mapOf(n.id to SkillState(tries = 1))), SkillTree.fruitLine(n, mapOf(n.id to SkillState(tries = 1, mastered = true))))
        } + SkillBranch.entries.map { SkillTree.openLine(it, emptyMap()) }
        val banned = listOf("good job", "smart", "clever", "perfect", "great", "right", "wrong", "level", "lock", "%")
        lines.forEach { l -> banned.forEach { b -> assertFalse(l.lowercase().contains(b), "$l has $b") } }
        assertFalse(lines.any { l -> l.any { it.isDigit() } }, "no numbers for him")
    }

    @Test fun talkingDotsToggleWithoutMastering() {
        var s = SkillState()
        s = SkillTree.toggleLevel(s, 0)
        s = SkillTree.toggleLevel(s, 2)
        assertEquals(setOf(0, 2), s.levels)
        assertFalse(s.mastered)
        s = SkillTree.toggleLevel(s, 0)
        assertEquals(setOf(2), s.levels)
        s = SkillTree.toggleLevel(s, 2)
        assertTrue(s.levels.isEmpty())
    }

    @Test fun parentRowHasOneControlAndTalkingDots() {
        val p = SkillTree.byId("talk:p")!!
        val fNode = SkillTree.byId("talk:f")!!
        val st = mapOf(
            "talk:p" to SkillState(mastered = true, levels = setOf(0, 1, 2, 3, 4, 5), tries = 20),
            "talk:b" to SkillState(mastered = true, tries = 20),
            "talk:m" to SkillState(mastered = true, tries = 20),
            "talk:f" to SkillState(tries = 25, levels = setOf(0, 1, 2)),
        )
        val mastered = SkillTree.parentRow(p, st)
        assertTrue(mastered.faded)
        assertTrue(mastered.showCheck)
        assertTrue(mastered.showDots)
        assertTrue(mastered.holdToUndo)
        assertFalse(mastered.dotsEnabled)
        assertFalse(mastered.markMastered)
        val open = SkillTree.parentRow(fNode, st)
        assertFalse(open.faded)
        assertFalse(open.showCheck)
        assertFalse(open.holdToUndo)
        assertTrue(open.showDots)
        assertTrue(open.dotsEnabled)
        assertTrue(open.markMastered)
        // The kid tree and the grown-ups tab share this store: ripe /p/, /f/ capped at 80%.
        assertEquals(1f, SkillTree.fruitGrowth(st["talk:p"]))
        assertEquals(SkillTree.GROW_CAP, SkillTree.fruitGrowth(st["talk:f"]))
        assertEquals(0f, SkillTree.fruitGrowth(st["talk:t"]))
        assertEquals(SkillStatus.MASTERED, SkillTree.status(p, st))
        assertEquals(SkillStatus.CURRENT, SkillTree.status(fNode, st))
        assertEquals(6, SkillTree.LEVEL_TAGS.size)
        assertEquals(SkillTree.LEVELS.size, SkillTree.LEVEL_TAGS.size)
        assertEquals(listOf("alone", "start", "mid", "end", "phrase", "sentence"), SkillTree.LEVEL_TAGS)
    }

    @Test fun everyTalkingRowShowsSixDotsAndMarkMasteredUntilMastered() {
        SkillTree.of(SkillBranch.TALKING).forEach { n ->
            val open = SkillTree.parentRow(n, emptyMap())
            assertTrue(open.showDots, n.id)
            assertTrue(open.dotsEnabled, n.id)
            assertTrue(open.markMastered, n.id)
            assertFalse(open.holdToUndo, n.id)
            val done = SkillTree.parentRow(n, mapOf(n.id to SkillState(mastered = true)))
            assertTrue(done.showDots, n.id)
            assertTrue(done.holdToUndo, n.id)
            assertFalse(done.markMastered, n.id)
            assertFalse(done.dotsEnabled, n.id)
        }
    }
}
