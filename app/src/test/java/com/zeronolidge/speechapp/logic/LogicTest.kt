package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.SpeechContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RobotTest {
    @Test
    fun forwardAndTurnsFollowTheRobotsHeading() {
        val lvl = RobotLevel(3, 3, Cell(2, 0), Dir.UP, Cell(0, 2), relative = true)
        val program = listOf(Cmd.FORWARD, Cmd.FORWARD, Cmd.TURN_RIGHT, Cmd.FORWARD, Cmd.FORWARD)
        val (_, won) = RobotSim.run(lvl, program)
        assertTrue(won)
    }

    @Test
    fun bumpingAWallStopsTheProgram() {
        val lvl = RobotLevel(3, 3, Cell(0, 0), Dir.UP, Cell(2, 2))
        val (steps, won) = RobotSim.run(lvl, listOf(Cmd.UP, Cmd.RIGHT))
        assertFalse(won)
        assertEquals(1, steps.size)
        assertTrue(steps[0] is StepResult.Bumped)
    }

    @Test
    fun treesBlockTheWay() {
        val lvl = RobotLevel(3, 3, Cell(0, 0), Dir.RIGHT, Cell(0, 2), setOf(Cell(0, 1)))
        val (steps, _) = RobotSim.run(lvl, listOf(Cmd.RIGHT))
        assertTrue(steps.single() is StepResult.Bumped)
    }

    @Test
    fun turnsWrapAround() {
        assertEquals(Dir.LEFT, Dir.UP.turnLeft())
        assertEquals(Dir.UP, Dir.LEFT.turnRight())
    }

    @Test
    fun everyLevelIsSolvable() {
        (RobotLevels.easy + RobotLevels.robot).forEach { assertNotNull(RobotSim.shortestArrowPath(it)) }
        repeat(50) {
            val lvl = RobotLevels.random(it % 2 == 0, Random(it))
            assertNotNull(RobotSim.shortestArrowPath(lvl))
            assertFalse(lvl.start in lvl.blocks || lvl.goal in lvl.blocks)
        }
    }
}

class GamesTest {
    @Test
    fun patternAnswerContinuesTheSequence() {
        repeat(200) { i ->
            val r = Patterns.make(i % (Patterns.maxLevel() + 1), Random(i))
            val expected = r.unit[r.shown.size % r.unit.size]
            assertEquals(expected, r.choice.answer)
            assertEquals(3, r.choice.options.size)
            assertEquals(3, r.choice.options.toSet().size)
            assertTrue(r.choice.answer in r.choice.options)
        }
    }

    @Test
    fun mathStaysInRange() {
        repeat(300) { i ->
            val rnd = Random(i)
            val m = Numbers.math(i % 2 == 0, 10, rnd)
            val result = if (m.plus) m.a + m.b else m.a - m.b
            assertEquals(result, m.answer)
            assertTrue(result in 0..10)
            assertEquals(3, m.choice.options.toSet().size)
            assertTrue(m.choice.options.all { it >= 0 })
        }
    }

    @Test
    fun countingChoicesAreDistinctAndIncludeAnswer() {
        repeat(200) { i ->
            val c = Numbers.count(10, Random(i))
            assertTrue(c.count in 1..10)
            assertEquals(c.count, c.choice.answer)
            assertEquals(3, c.choice.options.toSet().size)
        }
    }

    @Test
    fun oddOneOutHasExactlyOneOdd() {
        repeat(100) { i ->
            val r = OddOneOut.make(i % 2, Random(i))
            assertEquals(4, r.items.size)
            assertEquals(1, r.items.count { it == r.odd })
        }
    }

    @Test
    fun letterRoundsAreWellFormed() {
        repeat(100) { i ->
            val r = Letters.make(LetterMode.entries[i % 3], Random(i), tricky = i % 2 == 0)
            assertEquals(3, r.choice.options.toSet().size)
            assertEquals(r.target, r.choice.answer)
        }
    }

    @Test
    fun readingWordsCanBeSoundedOut() {
        assertEquals(listOf("kuh", "ah", "tuh"), Reading.sounds("cat"))
        ReadingContent.cvcWords.forEach { assertEquals(3, Reading.sounds(it.text).size) }
        assertEquals(26, LetterContent.letters.size)
    }

    @Test
    fun speechContentHasWords() {
        SpeechContent.sounds.forEach { assertTrue(it.words.size >= 4) }
    }
}

class SpeechMatchTest {
    @Test
    fun exactWordInPhraseMatches() {
        assertTrue(SpeechMatch.isGood("cat", listOf("a cat")))
        assertTrue(SpeechMatch.isGood("Banana", listOf("banana!")))
    }

    @Test
    fun closeAttemptsAreAccepted() {
        assertTrue(SpeechMatch.isGood("rabbit", listOf("wabbit")))
        assertTrue(SpeechMatch.isGood("spoon", listOf("poon")))
    }

    @Test
    fun unrelatedWordsAreNot() {
        assertFalse(SpeechMatch.isGood("rabbit", listOf("dog")))
        assertFalse(SpeechMatch.isGood("cat", emptyList()))
    }
}

class RpgTest {
    @Test
    fun levelCurveIsIncreasing() {
        assertEquals(1, Leveling.levelFor(0))
        assertEquals(2, Leveling.levelFor(Leveling.xpFor(2)))
        assertEquals(1, Leveling.levelFor(Leveling.xpFor(2) - 1))
        (1..20).forEach { assertTrue(Leveling.xpFor(it + 1) > Leveling.xpFor(it)) }
        assertTrue(Leveling.progress(Leveling.xpFor(3) + 1) in 0f..1f)
    }

    @Test
    fun everyLockedLandHasAReachablePower() {
        Land.entries.forEach { land ->
            assertTrue(land.unlocked(10))
            land.needs?.let { assertFalse(it.helper) }
        }
        assertTrue(Land.FOREST.unlocked(1))
        assertFalse(Land.CLOUDS.unlocked(1))
        assertTrue(Land.CLOUDS.unlocked(1, openAll = true))
    }

    @Test
    fun catalogIdsAreUnique() {
        assertEquals(Catalog.items.size, Catalog.items.map { it.id }.toSet().size)
        Catalog.inSlot(Slot.PET).forEach { assertNotNull(it.pet) }
    }

    @Test
    fun robotSolverFindsWorkingPrograms() {
        (RobotLevels.easy + RobotLevels.robot).forEach { lvl ->
            val program = RobotSim.solve(lvl)
            assertNotNull(program)
            assertTrue(RobotSim.run(lvl, program!!).second)
        }
        repeat(30) {
            val lvl = RobotLevels.random(it % 2 == 0, Random(it))
            assertTrue(RobotSim.run(lvl, RobotSim.solve(lvl)!!).second)
        }
    }
}

class StoryTest {
    @Test
    fun questsAreWellFormedForEveryLand() {
        repeat(40) { i ->
            val rnd = Random(i)
            Land.entries.forEach { land ->
                val p = StoryParams("Sam", heroLevel = 1 + i % 8, robotSolved = i, unlockedLands = Land.entries.toList())
                val q = StoryGen.make(land, p, rnd, mixed = i % 5 == 0)
                assertTrue(q.beats.size in 3..5)
                listOf(q.intro, q.twist, q.ending).forEach { assertFalse(it.text, it.text.contains("{")) }
                q.beats.forEach { b ->
                    assertFalse(b.narration, b.narration.contains("{"))
                    assertFalse(b.scene.text.contains("{"))
                }
                // The story always ends on the plot's big finish.
                assertEquals(q.plot.obstacles.last().piece, q.beats.last().scene.piece)
                assertTrue(q.twistBefore in 1 until q.beats.size)
                assertTrue(q.visitor.species != q.friend.species || q.plot.visitor != null)
                if (land == Land.FOREST && i % 5 != 0) assertTrue(q.beats.all { it.challenge is Challenge.Say })
            }
        }
    }

    @Test
    fun focusSoundsAreRespected() {
        val p = StoryParams("Sam", 1, focusSounds = setOf("s", "r"))
        repeat(20) {
            val q = StoryGen.make(Land.FOREST, p, Random(it))
            q.beats.forEach { b -> assertTrue((b.challenge as Challenge.Say).sound.id in setOf("s", "r")) }
        }
    }

    @Test
    fun speechWordsDontRepeatWithinAQuest() {
        repeat(20) {
            val q = StoryGen.make(Land.FOREST, StoryParams("Sam", 1), Random(it))
            val words = q.beats.map { (it.challenge as Challenge.Say).word.text }
            assertEquals(words.size, words.toSet().size)
        }
    }
}

class MapSuggestionTest {
    @Test
    fun suggestsUnvisitedLandsFirstThenRotates() {
        val open = listOf(Land.FOREST, Land.MEADOW, Land.LAGOON)
        assertEquals(Land.FOREST, Land.suggest(open, emptySet(), null))
        assertEquals(Land.MEADOW, Land.suggest(open, setOf(Land.FOREST), Land.FOREST))
        assertEquals(Land.LAGOON, Land.suggest(open, setOf(Land.FOREST, Land.MEADOW), Land.MEADOW))
        val all = open.toSet()
        assertEquals(Land.MEADOW, Land.suggest(open, all, Land.FOREST))
        assertEquals(Land.FOREST, Land.suggest(open, all, Land.LAGOON))
    }
}

class PlotTest {
    @Test
    fun plotsAreComplete() {
        assertTrue(Plots.all.size >= 8)
        assertEquals(Plots.all.size, Plots.all.map { it.id }.toSet().size)
        Plots.all.forEach { p ->
            assertEquals(4, p.obstacles.size)
            assertTrue(p.twist.text.contains("{v}"))
            assertEquals(Feeling.CHEER, p.ending.feeling)
        }
    }

    @Test
    fun obstacleCountsFitEveryQuestLength() {
        Plots.all.forEach { p ->
            (1..6).forEach { n ->
                val obs = StoryGen.obstaclesFor(p, n)
                assertEquals(n, obs.size)
                assertEquals(p.obstacles.last(), obs.last())
            }
        }
    }
}
