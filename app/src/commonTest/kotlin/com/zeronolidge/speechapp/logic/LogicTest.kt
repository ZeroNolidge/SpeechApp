package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.SpeechContent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
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
    fun trailVisitsEveryLandInOrderAndLoops() {
        assertEquals(16, Trail.size)
        assertEquals(Land.FOREST, Trail.landOf(0))
        assertEquals(Land.FOREST, Trail.landOf(1))
        assertEquals(Land.MEADOW, Trail.landOf(2))
        assertEquals(Land.MARKET, Trail.landOf(6))
        assertEquals(Land.CLOUDS, Trail.landOf(15))
        // After the last stop, a new lap begins; the chapter number keeps going up.
        assertEquals(Land.FOREST, Trail.landOf(16))
        assertEquals(Trail.lands.toSet(), Land.entries.toSet())
        assertTrue(Trail.opensLand(0))
        assertFalse(Trail.opensLand(1))
    }

    @Test
    fun reachedLandsOnlyGrow() {
        var before = 0
        (0..40).forEach { ch ->
            val n = Trail.reached(ch).size
            assertTrue(n >= before)
            assertTrue(Trail.landOf(ch) in Trail.reached(ch))
            before = n
        }
        assertEquals(listOf(Land.FOREST), Trail.reached(0))
        assertEquals(Trail.lands, Trail.reached(16))
    }

    @Test
    fun stopsSitOnTheMap() {
        (0 until Trail.size).forEach {
            val (x, y) = Trail.position(it)
            assertTrue(x in 0.03f..0.97f && y in 0.05f..0.97f, "stop $it at $x,$y")
        }
    }

    @Test
    fun piecesAreGivenInOrderAndNeverRepeat() {
        assertEquals(Catalog.awardOrder.size, Catalog.awardOrder.toSet().size)
        Catalog.awardOrder.forEach { assertNotNull(Catalog.byId(it), it) }
        val owned = Catalog.starters.toMutableSet()
        val given = mutableListOf<String>()
        while (true) {
            val c = Catalog.nextPiece(owned) ?: break
            given += c.id
            owned += c.id
        }
        assertEquals(Catalog.items.size, owned.size)
        assertEquals("hat_party", given.first())
        assertTrue(given.indexOf("wings_fairy") < 3)
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
                val p = StoryParams("Sam", chapter = i, robotSolved = i, reachedLands = Land.entries.toList(), speechStep = i % 3)
                val q = StoryGen.make(land, p, rnd, mixed = i % 5 == 0)
                assertTrue(q.beats.size in 3..5)
                listOf(q.intro, q.twist, q.ending).forEach { assertFalse(it.text.contains("{"), it.text) }
                q.beats.forEach { b ->
                    assertFalse(b.narration.contains("{"), b.narration)
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
            q.beats.map { it.challenge as Challenge.Say }.filter { it.kind == SayKind.PRACTICE }
                .forEach { assertTrue(it.sound!!.id in setOf("s", "r")) }
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

class SpeechPlanTest {
    @Test
    fun wordsMatchTheDifficultyStep() {
        repeat(30) {
            val one = StoryGen.practiceWord(StoryParams("Sam", speechStep = 0), emptySet(), Random(it)).second
            assertEquals(1, one.syllableCount, one.text)
            val two = StoryGen.practiceWord(StoryParams("Sam", speechStep = 1), emptySet(), Random(it)).second
            assertEquals(2, two.syllableCount, two.text)
            val phrase = StoryGen.practiceWord(StoryParams("Sam", speechStep = 2), emptySet(), Random(it)).second
            assertTrue(" " in phrase.text, phrase.text)
        }
    }

    @Test
    fun storyWordsGetHarderStepByStep() {
        StoryWords.all.forEach { sw ->
            assertEquals(3, sw.words.size)
            assertTrue(" " !in sw.at(0), sw.at(0))
            assertTrue(" " in sw.at(2), sw.at(2))
        }
        SetPiece.entries.forEach { StoryWords.forPiece(it) }
    }

    @Test
    fun forestChaptersStartWithAStoryWord() {
        repeat(10) {
            val q = StoryGen.make(Land.FOREST, StoryParams("Sam", chapter = it), Random(it))
            val first = q.beats.first().challenge as Challenge.Say
            assertEquals(SayKind.STORY, first.kind)
            assertEquals(StoryWords.forPiece(q.beats.first().scene.piece).at(0), first.word.text)
        }
    }

    @Test
    fun wordsFromEarlierDaysComeBack() {
        val book = WordBook()
            .record("pig", "🐷", today = 10, clear = false, heard = true)
            .record("pig", "🐷", today = 10, clear = false, heard = true)
            .record("sun", "☀️", today = 11, clear = true, heard = true)
        assertTrue(book.dueForReview(10, 3).isEmpty())
        assertEquals(listOf("pig", "sun"), book.dueForReview(12, 3).map { it.text })
        val p = StoryParams("Sam", chapter = 0, today = 12, words = book)
        val q = StoryGen.make(Land.FOREST, p, Random(1))
        val says = q.beats.map { it.challenge as Challenge.Say }
        assertTrue(says.any { it.kind == SayKind.REVIEW && it.word.text == "pig" })
    }

    @Test
    fun ownWordsShowUp() {
        val p = StoryParams("Sam", chapter = 0, customWords = listOf("Grandma" to "👵"))
        val q = StoryGen.make(Land.FOREST, p, Random(3))
        assertTrue(q.beats.any { (it.challenge as Challenge.Say).word.text == "Grandma" })
    }

    @Test
    fun wordBookTracksStickersAndTrickyWords() {
        var b = WordBook()
        b = b.record("Pig", "🐷", 1, clear = false, heard = true)
        b = b.record("cat", "🐱", 1, clear = true, heard = true)
        b = b.record("pig", "🐷", 2, clear = false, heard = true)
        b = b.record("pig", "🐷", 2, clear = false, heard = true)
        assertEquals(listOf("pig", "cat"), b.stickers.map { it.text })
        assertTrue(b["cat"]!!.shiny)
        assertEquals(WordStatus.STILL_TRICKY, b.status(b["pig"]!!))
        assertEquals(WordStatus.GOING_WELL, b.status(b["cat"]!!))
        assertEquals(2, b["pig"]!!.days)
        assertEquals(b, WordBook.decode(b.encode()))
    }

    @Test
    fun coinRoundsAreFair() {
        repeat(50) {
            val r = Coins.make(it, Random(it))
            assertTrue(r.price >= 1)
            assertTrue(r.onTable > r.price)
            assertTrue(r.price in r.purses.options)
            assertEquals(r.purses.options.size, r.purses.options.toSet().size)
        }
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

class VoiceMeterTest {
    private fun run(levels: List<Float>): VoiceMeter {
        val m = VoiceMeter()
        for (l in levels) if (m.feed(l, 50)) break
        return m
    }

    @Test
    fun quietRoomIsNotAVoice() {
        assertFalse(run(List(80) { 0.004f }).heard)
    }

    @Test
    fun aShortWordIsHeard() {
        val levels = List(10) { 0.005f } + List(8) { 0.08f } + List(62) { 0.005f }
        assertTrue(run(levels).heard)
    }

    @Test
    fun aWhisperOrPartOfAWordCounts() {
        // A soft, short sound: about 0.2 s, a bit louder than a quiet room.
        val levels = List(10) { 0.004f } + List(4) { 0.018f } + List(66) { 0.004f }
        assertTrue(run(levels).heard)
    }

    @Test
    fun aClickIsNotAVoice() {
        val levels = List(10) { 0.005f } + listOf(0.3f, 0.3f) + List(68) { 0.005f }
        assertFalse(run(levels).heard)
    }

    @Test
    fun aNoisyRoomNeedsALouderVoice() {
        val m = VoiceMeter()
        repeat(6) { m.feed(0.03f, 50) }
        assertTrue(m.threshold > 0.06f)
        assertFalse(run(List(80) { 0.03f }).heard)
    }

    @Test
    fun listeningEndsEarlyAfterTheChildStops() {
        val m = VoiceMeter()
        var steps = 0
        val levels = List(10) { 0.005f } + List(10) { 0.1f } + List(70) { 0.005f }
        for (l in levels) {
            steps++
            if (m.feed(l, 50)) break
        }
        assertTrue(m.heard)
        assertTrue(steps < 40, "stopped after $steps steps")
    }
}

class BackupTest {
    @Test
    fun codesRoundTrip() {
        val values = mapOf("chapter" to "i:7", "childName" to "s:Sam = the\\ best\nkid", "words" to "S:a\u001Fb", "speechRate" to "f:0.85")
        val code = Backup.encode(values)
        assertTrue(code.startsWith("TALK1-"))
        assertEquals(values, Backup.decode(code))
        // Spaces and line breaks from copying are fine.
        assertEquals(values, Backup.decode(code.chunked(20).joinToString("\n ")))
    }

    @Test
    fun brokenCodesAreRejected() {
        val code = Backup.encode(mapOf("chapter" to "i:3"))
        assertEquals(null, Backup.decode(code.dropLast(5)))
        assertEquals(null, Backup.decode("hello"))
        val tampered = code.replaceRange(8, 9, if (code[8] == 'A') "B" else "A")
        assertEquals(null, Backup.decode(tampered))
    }

    @Test
    fun base64MatchesTheStandard() {
        assertEquals("aGk=", Base64Lite.encode("hi".encodeToByteArray()))
        assertEquals("aGVsbG8gd29ybGQ=", Base64Lite.encode("hello world".encodeToByteArray()))
        assertEquals("hello world", Base64Lite.decode("aGVsbG8gd29ybGQ=")!!.decodeToString())
    }
}

class TrailOrderTest {
    @Test
    fun adventureReachesEveryLandInOrder() {
        // Two chapters in the forest, then Number Meadow, then the rest, one land at a time.
        assertEquals(Land.FOREST, Trail.landOf(0))
        assertEquals(Land.FOREST, Trail.landOf(1))
        assertEquals(Land.MEADOW, Trail.landOf(2))
        assertFalse(Land.MEADOW in Trail.reached(1))
        assertTrue(Land.MEADOW in Trail.reached(2))
        assertTrue(Trail.opensLand(2))
        var seen = listOf<Land>()
        for (c in 0 until Trail.size) {
            val l = Trail.landOf(c)
            if (seen.lastOrNull() != l) seen = seen + l
            // Every land on the way is reached (open) when its chapter comes up.
            assertTrue(l in Trail.reached(c))
        }
        assertEquals(Trail.lands, seen)
    }
}
