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

/** The balloon colour-pattern game beat: AB, then AAB, then ABC; every talk word is p or b. */
class BalloonGameTest {
    @Test
    fun everyTalkWordStartsWithPOrB() {
        for (c in BalloonGame.colors) {
            val w = BalloonGame.talkWord(c)
            assertTrue(w.first() == 'p' || w.first() == 'b', w)
        }
    }

    @Test
    fun theThreeStepsAreAbThenAabThenAbc() {
        val rnd = Random(3)
        repeat(50) {
            assertEquals(2, BalloonGame.make(0, rnd).unit.size)
            assertEquals(3, BalloonGame.make(1, rnd).unit.size)
            assertEquals(2, BalloonGame.make(1, rnd).unit.distinct().size)
            assertEquals(3, BalloonGame.make(2, rnd).unit.size)
            assertEquals(3, BalloonGame.make(2, rnd).unit.distinct().size)
        }
    }

    @Test
    fun atMostThreeChoicesAndTheAnswerContinuesTheRow() {
        val rnd = Random(4)
        repeat(60) { i ->
            val round = BalloonGame.make(i % 3, rnd)
            assertTrue(round.choice.options.size <= 3)
            assertEquals(round.choice.options.size, round.choice.options.distinct().size)
            assertTrue(round.choice.answer in round.choice.options)
            assertEquals(round.unit[round.shown.size % round.unit.size], round.choice.answer)
        }
    }

    @Test
    fun theAnswerIsNotAlwaysFirst() {
        val seenIndex = mutableSetOf<Int>()
        repeat(60) { i -> seenIndex += BalloonGame.make(i % 3, Random(i)).let { it.choice.options.indexOf(it.choice.answer) } }
        assertTrue(seenIndex.size > 1, "answer landed at the same index every time: $seenIndex")
    }

    @Test
    fun linesAreOneWordTwoWordsThenASentence() {
        for (c in BalloonGame.colors) {
            val lines = BalloonGame.lines(c)
            assertEquals(3, lines.size)
            assertEquals(1, lines[0].split(' ').size)
            assertEquals(2, lines[1].split(' ').size)
            assertEquals(c.first().toString(), BalloonGame.sound(c))
        }
    }

    @Test
    fun modelPopSaysTheColourOnceThenAsksForThePop() {
        for (c in BalloonGame.colors) {
            val line = BalloonGame.modelPop(c)
            assertTrue(line.startsWith(c.replaceFirstChar { it.uppercase() }), line)
            assertTrue(line.contains("pop", ignoreCase = true), line)
        }
    }

    @Test
    fun popLinesAreOneWordTwoWordsThenASentenceAndAlwaysP() {
        assertEquals(3, BalloonGame.popLines.size)
        assertEquals(1, BalloonGame.popLines[0].split(' ').size)
        assertEquals(2, BalloonGame.popLines[1].split(' ').size)
        for (line in BalloonGame.popLines) assertTrue(line.lowercase().startsWith("pop"), line)
    }

    @Test
    fun successNeverSaysPopForHim() {
        // He's just said "pop" himself (see GameBeatPhase.TALK_POP); Rory never says it for him.
        assertFalse(BalloonGame.success().contains("pop", ignoreCase = true), BalloonGame.success())
    }

    @Test
    fun ladderCeilingMatchesTheThreeSteps() {
        assertEquals(2, Ladder.MAX.getValue(Ladder.GAME_PATTERN))
    }

    @Test
    fun neverBothPinkAndPurpleAndNeverRed() {
        for (step in 0..2) {
            repeat(300) { i ->
                val round = BalloonGame.make(step, Random(step * 10_000 + i))
                val all = round.unit + round.choice.options
                assertFalse(all.contains("pink") && all.contains("purple"), "step $step seed $i: $all")
                assertFalse(all.contains("red"), "step $step seed $i: $all")
            }
        }
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
        assertTrue(m.threshold > 0.04f)
        assertFalse(run(List(80) { 0.03f }).heard)
    }

    @Test
    fun aSoftVoicedShortSoundCounts() {
        // Matches VoiceDetector's real settings for a plain talk turn (needMs 90, see
        // VoiceDetector.listenTry): a true, if very quiet, try just above a quiet room now
        // crosses the bar and counts quickly. With the old floor (MIN_VOICE 0.012, factor 2.2)
        // this same sound never would have.
        val m = VoiceMeter(needMs = 90)
        val levels = List(6) { 0.004f } + List(2) { 0.008f } + List(60) { 0.004f }
        for (l in levels) if (m.feed(l, 50)) break
        assertTrue(m.heard)
    }

    @Test
    fun trueSilenceNeverCounts() {
        val m = VoiceMeter(needMs = 90)
        val levels = List(80) { 0.004f }
        for (l in levels) if (m.feed(l, 50)) break
        assertFalse(m.heard)
        assertFalse(m.soft)
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

class QuizLookTest {
    private val cases = sequence {
        for (isAnswer in listOf(true, false)) for (m in 0..3) for (s in listOf(true, false))
            for (r in listOf(true, false)) for (wd in listOf(true, false)) yield(listOf(isAnswer, m, s, r, wd))
    }

    @Test
    fun onlyTheCorrectAnswerCanEverBeGreenOrGlow() {
        cases.forEach { (isAnswer, m, s, r, wd) ->
            val look = tileLook(isAnswer as Boolean, m as Int, s as Boolean, r as Boolean, wd as Boolean)
            if (!isAnswer) assertTrue(look == TileLook.PLAIN || look == TileLook.GREY, "wrong tile was $look")
        }
    }

    @Test
    fun secondMissHighlightsTheCorrectAnswerOnly() {
        assertEquals(TileLook.GLOW, tileLook(isAnswer = true, misses = 2, solvedByTap = false, revealed = false, wanded = false))
        assertEquals(TileLook.PLAIN, tileLook(isAnswer = false, misses = 2, solvedByTap = false, revealed = false, wanded = false))
        // Shown after the 3rd miss: the correct one glows (not green), the wrong ones grey.
        assertEquals(TileLook.GLOW, tileLook(isAnswer = true, misses = 3, solvedByTap = false, revealed = true, wanded = false))
        assertEquals(TileLook.GREY, tileLook(isAnswer = false, misses = 3, solvedByTap = false, revealed = true, wanded = false))
        assertEquals(TileLook.RIGHT, tileLook(isAnswer = true, misses = 1, solvedByTap = true, revealed = false, wanded = false))
    }
}

class MapAreasTest {
    private val sizes = listOf(1280f to 760f, 1000f to 620f, 820f to 700f, 360f to 300f)

    @Test
    fun tappingAnAreasLabelOrPictureOpensThatSameArea() {
        for ((w, h) in sizes) {
            val half = 70f
            val lh = 40f
            MapAreas.all.forEach { a ->
                // The middle of its label chip...
                assertEquals(a.land, MapAreas.hit(a.labelX * w, a.labelY * h + lh / 2, w, h, half, lh), "label of ${a.land} at ${w}x$h")
                // ...and the middle of its landmark picture.
                assertEquals(a.land, MapAreas.hit(a.markX * w, a.markY * h, w, h, half, lh), "picture of ${a.land} at ${w}x$h")
            }
        }
    }

    @Test
    fun meadowAndForestAreTheirOwnAreas() {
        val m = MapAreas.of(Land.MEADOW)
        assertEquals(Land.MEADOW, MapAreas.hit(m.labelX * 1280f, m.labelY * 760f + 20f, 1280f, 760f, 70f, 40f))
        val f = MapAreas.of(Land.FOREST)
        assertEquals(Land.FOREST, MapAreas.hit(f.labelX * 1280f, f.labelY * 760f + 20f, 1280f, 760f, 70f, 40f))
    }
}

/** Plays every kind of quiz against the answer key, with a right tap and with wrong taps. */
class PlayEveryQuizTest {
    private val rnd = Random(42)

    private fun allQuizzes(): List<Challenge> = buildList {
        repeat(150) {
            add(Challenge.Count(Numbers.count(10, rnd)))
            add(Challenge.Math(Numbers.math(true, 10, rnd)))
            add(Challenge.Math(Numbers.math(false, 10, rnd)))
            add(Challenge.More(Numbers.compare(8, rnd)))
            LetterMode.entries.forEach { add(Challenge.Letter(Letters.make(it, rnd, tricky = it.ordinal % 2 == 0))) }
            add(Challenge.SoundOut(Reading.make(ReadingContent.cvcWords, rnd)))
            add(Challenge.WhichWord(Reading.make(ReadingContent.cvcWords, rnd)))
            add(Challenge.Pattern(Patterns.make(it % (Patterns.maxLevel() + 1), rnd)))
            add(Challenge.OddOne(OddOneOut.make(it % 2, rnd)))
            add(Challenge.Coins(Coins.make(it, rnd)))
        }
    }

    @Test
    fun theStoredAnswerIsReallyTheRightOne() {
        allQuizzes().forEach { c ->
            val k = AnswerKey.of(c) ?: return@forEach
            assertTrue(k.answer in k.options, "answer not among the tiles: $c")
            assertEquals(k.options.size, k.options.distinct().size, "duplicate tiles: $c")
            when (c) {
                is Challenge.Count -> assertEquals(c.round.count, k.answer)
                is Challenge.Math -> assertEquals(if (c.round.plus) c.round.a + c.round.b else c.round.a - c.round.b, k.answer)
                is Challenge.More -> assertEquals(c.round.left > c.round.right, k.answer)
                is Challenge.Letter -> assertEquals(c.round.target, k.answer)
                is Challenge.SoundOut -> assertEquals(c.round.word, k.answer)
                is Challenge.WhichWord -> assertEquals(c.round.word, k.answer)
                is Challenge.Pattern -> assertEquals(c.round.unit[c.round.shown.size % c.round.unit.size], k.answer)
                is Challenge.OddOne -> assertEquals(1, c.round.items.count { it == c.round.items[k.answer as Int] })
                is Challenge.Coins -> assertEquals(c.round.price, k.answer)
                else -> {}
            }
        }
    }

    @Test
    fun aRightTapIsRightAndAWrongTapIsNeverMarkedRight() {
        allQuizzes().forEach { c ->
            val k = AnswerKey.of(c) ?: return@forEach
            // A right tap: solved, and only the correct tile is green.
            val (afterRight, r) = QuizState().tap(true)
            assertEquals(TapResult.RIGHT, r)
            k.options.forEach { o -> assertEquals(o == k.answer, afterRight.look(o == k.answer) == TileLook.RIGHT, "$c") }
            // Wrong taps on every wrong tile in turn: never RIGHT, never green, and the right tile
            // glows on the 2nd miss and is shown after the 3rd.
            val wrong = k.options.filter { it != k.answer }
            var s = QuizState()
            val results = (0 until 3).map { i ->
                val tapped = wrong[i % wrong.size]
                val (next, res) = s.tap(tapped == k.answer)
                s = next
                k.options.forEach { o -> if (o != k.answer) assertTrue(s.look(false) != TileLook.RIGHT, "wrong tile green: $c") }
                assertTrue(res != TapResult.RIGHT, "wrong tap marked right: $c")
                res
            }
            assertEquals(listOf(TapResult.TRY_AGAIN, TapResult.HINT, TapResult.SHOWN), results)
            assertEquals(TileLook.GLOW, s.look(true))
            assertEquals(TapResult.IGNORED, s.tap(true).second)
        }
    }
}

class EchoTest {
    @Test
    fun roryRepeatsTheTargetWordAndAddsOne() {
        assertEquals("Dog! A big dog!", Echo.line("dog", 0))
        assertEquals("Grandma! Hi, Grandma!", Echo.line("Grandma"))
        assertEquals("Big red ball! Yay!", Echo.line("big red ball"))
        assertTrue(Echo.line("cat", 3).startsWith("Cat! A "))
    }
}

class PersonalWordsTest {
    @Test
    fun ownWordsAndTargetSoundsAreUsedMostOfTheTime() {
        val p = StoryParams("Sam", 1, focusSounds = setOf("s"), customWords = listOf("Grandma" to "👵", "pizza" to "🍕", "Rex" to "🐶"))
        var personal = 0
        var total = 0
        repeat(40) {
            val q = StoryGen.make(Land.FOREST, p.copy(chapter = it), Random(it))
            q.beats.map { b -> b.challenge as Challenge.Say }.forEach { s ->
                total++
                if (s.kind == SayKind.OWN || (s.kind == SayKind.PRACTICE && s.sound?.id == "s")) personal++
            }
        }
        assertTrue(personal * 2 > total, "only $personal of $total turns used his words or sounds")
    }

    @Test
    fun firstSoundPuzzlesUseHisLetters() {
        assertEquals(listOf('s', 'g'), Letters.targetsFor(setOf("sh"), listOf("Grandma")).map { it.lower })
    }
}

class PipStoryTest {
    @Test
    fun pipHasAboutTenTurnsHopsOnEveryCallAndEndsAsleep() {
        (0..2).forEach { step ->
            for (lures in listOf(emptyList(), listOf("Grandma", "ball"), listOf("a", "b", "c", "d"))) {
                val t = PipStory.turns(step, lures)
                assertTrue(t.size in 9..11, "${t.size} turns")
                val calls = t.filter { it.spot != null }
                assertEquals(PipStory.HOPS, calls.size)
                // Every call makes Pip hop closer: no rustle-only turns.
                assertTrue(calls.all { it.reaction.startsWith("Hop") }, calls.map { it.reaction }.toString())
                assertEquals(PipSpot.entries.toList(), calls.mapNotNull { it.spot }.distinct())
                assertTrue(calls.last().reaction.contains("jumps out"))
                assertTrue(t.last().reaction.contains("asleep"))
                assertTrue(t.all { it.call.isNotBlank() && it.prompt.contains(it.call) })
            }
        }
        assertTrue(PipStory.turns(0).all { it.call.split(' ').size <= 2 })
    }

    @Test
    fun hisOwnWordsComeFirst() {
        val t = PipStory.turns(1, listOf("Grandma", "ball"))
        assertEquals(listOf("Grandma!", "ball!"), t.filter { it.stage == 1 && it.spot != null }.map { it.call }.take(2))
        assertTrue(t.first { it.call == "Grandma!" }.prompt.contains("Grandma"))
    }
}

/** The quiet difficulty ladders: up after ~4 of 5 right across two sessions, back after 2 misses in a row. */
class LadderTest {
    private fun Ladder.play(vararg r: Pair<Int, Boolean>) = r.fold(this) { l, (s, ok) -> l.record(ok, s, 2) }

    @Test
    fun stepsUpOnlyAcrossTwoSessions() {
        assertEquals(0, Ladder().play(1 to true, 1 to true, 1 to true, 1 to true, 1 to true).step)
        assertEquals(1, Ladder().play(1 to true, 1 to true, 1 to false, 2 to true, 2 to true).step)
        // Never past the top, never below the bottom.
        assertEquals(2, Ladder(2).play(3 to true, 3 to true, 4 to true, 4 to true, 4 to true).step)
        assertEquals(0, Ladder().play(1 to false, 1 to false).step)
    }

    @Test
    fun stepsBackAfterTwoMissesInARow() {
        assertEquals(1, Ladder(2).play(5 to true, 5 to false, 5 to false).step)
        // One miss alone doesn't.
        assertEquals(2, Ladder(2).play(5 to false, 5 to true, 5 to false).step)
    }

    @Test
    fun survivesSaving() {
        val l = Ladder().play(1 to true, 2 to false, 2 to true)
        assertEquals(l, Ladder.decode(l.encode()))
        assertEquals(Ladder(), Ladder.decode(null))
    }

    @Test
    fun stepsChangeThePuzzles() {
        val rnd = Random(9)
        repeat(50) {
            assertTrue((Numbers.forStep(0, rnd) as MathOrCount.C).round.count <= 5)
            assertTrue((Numbers.forStep(1, rnd) as MathOrCount.C).round.count <= 10)
            val add = (Numbers.forStep(2, rnd) as MathOrCount.M).round
            assertEquals(1, add.b)
            assertTrue(add.plus)
            assertEquals(2, Patterns.makeStep(0, rnd).unit.size)
            assertEquals(3, Patterns.makeStep(1, rnd).unit.size)
            assertEquals(3, Patterns.makeStep(2, rnd).unit.distinct().size)
        }
        val s = SpeechContent.sounds.first()
        assertTrue(SpeechSteps.wordsFor(s, 0).first().text.split(' ').size == 1)
        assertTrue(SpeechSteps.wordsFor(s, 1).all { it.text.split(' ').size == 2 })
        assertTrue(SpeechSteps.wordsFor(s, 2).all { it.text.split(' ').size >= 2 })
    }
}

/** An in-memory [com.zeronolidge.speechapp.state.Prefs], so [com.zeronolidge.speechapp.state.Progress] can be tested without Android or the web. */
private class FakePrefs(
    private val ints: MutableMap<String, Int> = mutableMapOf(),
    private val floats: MutableMap<String, Float> = mutableMapOf(),
    private val bools: MutableMap<String, Boolean> = mutableMapOf(),
    private val strings: MutableMap<String, String?> = mutableMapOf(),
    private val sets: MutableMap<String, Set<String>> = mutableMapOf(),
) : com.zeronolidge.speechapp.state.Prefs {
    override fun getInt(key: String, default: Int) = ints[key] ?: default
    override fun getFloat(key: String, default: Float) = floats[key] ?: default
    override fun getBoolean(key: String, default: Boolean) = bools[key] ?: default
    override fun getString(key: String, default: String?) = strings[key] ?: default
    override fun getStringSet(key: String, default: Set<String>?) = sets[key] ?: default
    override fun edit(): com.zeronolidge.speechapp.state.Prefs.Editor = Editor()

    private inner class Editor : com.zeronolidge.speechapp.state.Prefs.Editor {
        override fun putInt(key: String, value: Int): com.zeronolidge.speechapp.state.Prefs.Editor { ints[key] = value; return this }
        override fun putFloat(key: String, value: Float): com.zeronolidge.speechapp.state.Prefs.Editor { floats[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): com.zeronolidge.speechapp.state.Prefs.Editor { bools[key] = value; return this }
        override fun putString(key: String, value: String?): com.zeronolidge.speechapp.state.Prefs.Editor { strings[key] = value; return this }
        override fun putStringSet(key: String, value: Set<String>): com.zeronolidge.speechapp.state.Prefs.Editor { sets[key] = value; return this }
        override fun clear(): com.zeronolidge.speechapp.state.Prefs.Editor {
            ints.clear(); floats.clear(); bools.clear(); strings.clear(); sets.clear()
            return this
        }
        override fun apply() {}
    }
}

/**
 * The balloon game beat's quiet ladder, persisted through [com.zeronolidge.speechapp.state.Progress]
 * exactly like speech, numbers and patterns: up after 4 of the last 5 right across two sessions
 * (a new [com.zeronolidge.speechapp.state.Progress] over the same prefs, as a real app restart
 * would), back one level after 2 misses in a row, never below AB or above ABC.
 */
class GamePatternLadderTest {
    @Test
    fun defaultsToAbForOldSaves() {
        // An old save made before this feature existed has no "ladder_gamePattern" key at all.
        assertEquals(0, com.zeronolidge.speechapp.state.Progress(FakePrefs()).gamePatternStep)
    }

    @Test
    fun stepsUpAfterFourOfFiveAcrossTwoSessions() {
        val prefs = FakePrefs()
        val first = com.zeronolidge.speechapp.state.Progress(prefs)
        repeat(4) { first.gameBeatResult(true) }
        // Still one session: on its own, a lucky run shouldn't be enough.
        assertEquals(0, first.gamePatternStep)
        val second = com.zeronolidge.speechapp.state.Progress(prefs)
        second.gameBeatResult(true)
        assertEquals(1, second.gamePatternStep)
    }

    @Test
    fun stepsBackAfterTwoMissesInARow() {
        val prefs = FakePrefs()
        val up = com.zeronolidge.speechapp.state.Progress(prefs)
        repeat(4) { up.gameBeatResult(true) }
        val toAab = com.zeronolidge.speechapp.state.Progress(prefs)
        toAab.gameBeatResult(true)
        assertEquals(1, toAab.gamePatternStep)
        toAab.gameBeatResult(false)
        toAab.gameBeatResult(false)
        assertEquals(0, toAab.gamePatternStep)
        // One miss alone doesn't step back.
        val prefs2 = FakePrefs()
        val p2 = com.zeronolidge.speechapp.state.Progress(prefs2)
        repeat(4) { p2.gameBeatResult(true) }
        val p2b = com.zeronolidge.speechapp.state.Progress(prefs2)
        p2b.gameBeatResult(true)
        p2b.gameBeatResult(false)
        p2b.gameBeatResult(true)
        assertEquals(1, p2b.gamePatternStep)
    }

    @Test
    fun neverGoesAboveAbcOrBelowAb() {
        val prefs = FakePrefs()
        var last = com.zeronolidge.speechapp.state.Progress(prefs)
        repeat(30) {
            last = com.zeronolidge.speechapp.state.Progress(prefs)
            last.gameBeatResult(true)
        }
        assertEquals(2, last.gamePatternStep)
        repeat(6) { last.gameBeatResult(false) }
        assertTrue(last.gamePatternStep >= 0)
    }

    @Test
    fun survivesSavingAcrossAppRestarts() {
        val prefs = FakePrefs()
        val first = com.zeronolidge.speechapp.state.Progress(prefs)
        repeat(4) { first.gameBeatResult(true) }
        val second = com.zeronolidge.speechapp.state.Progress(prefs)
        second.gameBeatResult(true)
        val step = second.gamePatternStep
        assertEquals(step, com.zeronolidge.speechapp.state.Progress(prefs).gamePatternStep)
    }
}

/** The balloon game beat's flow (see [GameBeatState]): nothing ever fails, and every path reaches [GameBeatPhase.DONE]. */
class GameBeatStateTest {
    @Test
    fun rightFirstTimeSkipsStraightToTalk() {
        val s = GameBeatState().named().tap(true)
        assertEquals(GameBeatPhase.TALK_COLOUR, s.phase)
        assertTrue(s.rightFirstTry)
    }

    @Test
    fun oneWrongTapWigglesThenGlowsAndCanStillSucceed() {
        var s = GameBeatState().named()
        s = s.tap(false)
        assertEquals(GameBeatPhase.WIGGLE, s.phase)
        assertTrue(s.glow)
        s = s.wiggled()
        assertEquals(GameBeatPhase.PUZZLE, s.phase)
        assertTrue(s.glow)
        s = s.tap(true)
        assertEquals(GameBeatPhase.TALK_COLOUR, s.phase)
        assertFalse(s.rightFirstTry)
    }

    @Test
    fun twoWrongTapsShowTheAnswer() {
        val s = GameBeatState().named().tap(false).wiggled().tap(false)
        assertEquals(GameBeatPhase.SHOW_ANSWER, s.phase)
        assertEquals(GameBeatPhase.TALK_COLOUR, s.shown().phase)
    }

    @Test
    fun colourTalkThenModelThenPopTalkThenClose() {
        var s = GameBeatState().named().tap(true)
        assertEquals(GameBeatPhase.TALK_COLOUR, s.phase)
        s = s.talked()
        assertEquals(GameBeatPhase.MODEL_POP, s.phase)
        s = s.modeled()
        assertEquals(GameBeatPhase.TALK_POP, s.phase)
        s = s.popped()
        assertEquals(GameBeatPhase.CLOSE, s.phase)
        s = s.closed()
        assertEquals(GameBeatPhase.DONE, s.phase)
    }

    @Test
    fun everyPathReachesDone() {
        val paths = listOf(
            GameBeatState().named().tap(true),
            GameBeatState().named().tap(false).wiggled().tap(true),
            GameBeatState().named().tap(false).wiggled().tap(false).shown(),
        )
        for (p in paths) assertEquals(GameBeatPhase.DONE, p.talked().modeled().popped().closed().phase)
    }

    @Test
    fun outOfOrderCallsAreIgnored() {
        // modeled() only fires from MODEL_POP, popped() only from TALK_POP: calling either early
        // (before Rory's asked for the pop) is a no-op, so a stray call can never skip a turn.
        val talking = GameBeatState().named().tap(true)
        assertEquals(talking, talking.modeled())
        assertEquals(talking, talking.popped())
    }

    @Test
    fun tapsOutsideThePuzzlePhaseAreIgnored() {
        val start = GameBeatState()
        assertEquals(start, start.tap(true))
        val talking = GameBeatState().named().tap(true)
        assertEquals(talking, talking.tap(false))
    }
}

/** Echo Cave and the grown-ups' words for today. */
class EchoCaveTest {
    @Test
    fun ownWordsThenTargetSoundsFirst() {
        val w = EchoCave.words(listOf("Grandma" to "👵", "Rex" to "🐶"), setOf("s"), 0, Random(1))
        assertEquals(EchoCave.TRIES, w.size)
        assertEquals(listOf("Grandma", "Rex"), w.take(2).map { it.text })
        val s = SpeechContent.sounds.first { it.id == "s" }.words.map { it.text }
        assertTrue(w[2].text in s)
        assertEquals(w.size, w.map { it.text.lowercase() }.distinct().size)
        assertEquals("pig... pig... pig...", EchoCave.echo("pig!"))
    }

    @Test
    fun wordsToSayToday() {
        val book = WordBook().record("dog", "🐶", 5, clear = false, heard = true).record("dog", "🐶", 5, clear = false, heard = true)
            .record("sun", "☀️", 2, clear = true, heard = true)
        val today = TodayWords.pick(book, 6, listOf("sock", "sea"))
        assertEquals(3, today.size)
        assertEquals("dog", today.first())
        assertTrue("sun" in today)
    }

    @Test
    fun roryExpandsTheWord() {
        assertEquals("A big red ball!", Echo.expansion("ball", 0))
        assertEquals("Hi, Grandma!", Echo.expansion("Grandma"))
        assertEquals("Yay!", Echo.expansion("big pig"))
    }
}

/** Every letter round shows only letters, in one case per round, and the right tile matches the question. */
class LetterRoundsTest {
    @Test
    fun onlyLettersOneCasePerRound() {
        val rnd = Random(3)
        LetterMode.entries.forEach { mode ->
            repeat(400) { i ->
                val target = if (i % 3 == 0) LetterContent.letters[i % 26] else null
                val r = Letters.make(mode, rnd, tricky = i % 2 == 0, target = target)
                val tiles = r.choice.options.map { Letters.tileText(mode, it) }
                assertTrue(tiles.all { t -> t.isNotEmpty() && t.all { it.isLetter() } }, "non-letter tile in $mode: $tiles")
                assertEquals(tiles.size, tiles.distinct().size)
                when (mode) {
                    LetterMode.BIG_TO_SMALL -> assertTrue(tiles.all { t -> t.all { it.isLowerCase() } }, "$tiles")
                    LetterMode.SMALL_TO_BIG -> assertTrue(tiles.all { t -> t.all { it.isUpperCase() } }, "$tiles")
                    LetterMode.FIRST_SOUND -> assertTrue(tiles.all { t -> t.length == 2 && t[0].isUpperCase() && t[1].isLowerCase() }, "$tiles")
                }
                // Big A asks for little a (and the other way round): the right tile is the same letter.
                val q = Letters.questionText(mode, r.target)
                val right = Letters.tileText(mode, r.choice.answer)
                if (q != null) assertEquals(q.lowercase(), right.lowercase())
                assertEquals(r.target, r.choice.answer)
            }
        }
    }

    @Test
    fun hisLettersComeFirst() {
        val mine = Letters.targetsFor(setOf("s"), listOf("Grandma"))
        val rnd = Random(5)
        val picks = List(400) { Letters.pickTarget(mine, rnd) }
        assertTrue(picks.count { it != null } > 250)
        assertTrue(picks.filterNotNull().all { it in mine })
    }
}

/** The map's one tap rule: area zones and the next-stop button never share a tap. */
class MapTargetTest {
    // Map sizes from phones to tablets, with the label and button sizes the map uses (density 1 and 2).
    private val cases = listOf(
        floatArrayOf(1100f, 630f, 64f, 44f, 48f), floatArrayOf(1000f, 570f, 64f, 40f, 48f),
        floatArrayOf(760f, 660f, 64f, 40f, 48f), floatArrayOf(400f, 348f, 64f, 35f, 48f),
        floatArrayOf(2200f, 1260f, 128f, 88f, 96f), floatArrayOf(800f, 696f, 128f, 70f, 96f),
    )

    @Test
    fun everyStopButtonIsClearOfEveryAreaAndTapsStartTheStory() {
        for (c in cases) {
            val (w, h, half, lh, r) = c.toList()
            for (stop in 0 until Trail.size) {
                val (fx, fy) = Trail.position(stop)
                val (sx, sy) = MapAreas.stopSpot(Trail.landAt(stop), fx, fy, w, h, r, half, lh)
                assertTrue(sx in 0f..w && sy in 0f..h, "stop $stop off the map at ${w}x$h")
                // It is always clearly at its own land (a fresh start's button at the Forest).
                assertTrue(MapAreas.belongsTo(Trail.landAt(stop), sx, sy, w, h), "stop $stop not by ${Trail.landAt(stop)} at ${w}x$h")
                // Tablet-sized maps: the Forest and Meadow buttons touch no area's zone at all, and no
                // button ever covers a picture or a label (those always open their land).
                if (w / r >= 15f && stop < 4) assertTrue(MapAreas.clearOfZones(sx, sy, w, h, r, half, lh), "stop $stop inside a zone at ${w}x$h")
                // (On a squarer map, an upright iPad, the Lagoon has no such room; there its picture
                // still wins any tap on it, by the tap rule above.)
                if (w / r >= 15f && w / h >= 1.5f) assertTrue(MapAreas.clearOfCores(sx, sy, w, h, r, half, lh), "stop $stop on a picture or label at ${w}x$h")
                // Its middle and its ring start the story...
                assertEquals(MapTarget.Stop, MapAreas.target(sx, sy, w, h, half, lh, sx, sy, r), "stop $stop centre at ${w}x$h")
                if (MapAreas.clearOfCores(sx, sy, w, h, r, half, lh)) {
                    assertEquals(MapTarget.Stop, MapAreas.target(sx + r * 0.9f, sy, w, h, half, lh, sx, sy, r), "stop $stop ring at ${w}x$h")
                }
                // ...and every area's picture and label still open that area, wherever the button is.
                MapAreas.all.forEach { a ->
                    assertEquals(MapTarget.Area(a.land), MapAreas.target(a.markX * w, a.markY * h, w, h, half, lh, sx, sy, r), "picture of ${a.land}, stop $stop at ${w}x$h")
                    assertEquals(MapTarget.Area(a.land), MapAreas.target(a.labelX * w, a.labelY * h + lh / 2, w, h, half, lh, sx, sy, r), "label of ${a.land}, stop $stop at ${w}x$h")
                }
            }
        }
    }

    @Test
    fun theCrystalCaveStopSitsByTheForestClearOfTheNextStopAndOpensTheCave() {
        for (c in cases) {
            val (w, h, half, lh, r) = c.toList()
            for (stop in 0 until Trail.size) {
                val (fx, fy) = Trail.position(stop)
                val (sx, sy) = MapAreas.stopSpot(Trail.landAt(stop), fx, fy, w, h, r, half, lh)
                val cr = r * 0.8f
                val (cx, cy) = MapAreas.caveSpot(w, h, cr, sx, sy, r, half, lh)
                assertTrue(cx in 0f..w && cy in 0f..h, "cave off the map at ${w}x$h")
                // Never touching the next-stop button, so the two never share a tap.
                val d = kotlin.math.sqrt((cx - sx) * (cx - sx) + (cy - sy) * (cy - sy))
                assertTrue(d >= r + cr, "cave on the stop button (stop $stop) at ${w}x$h")
                assertEquals(MapTarget.Cave, MapAreas.target(cx, cy, w, h, half, lh, sx, sy, r, cx, cy, cr), "cave centre, stop $stop at ${w}x$h")
                if (w / r >= 15f) assertTrue(MapAreas.belongsTo(Land.FOREST, cx, cy, w, h), "cave not by the Forest at ${w}x$h")
                if (w / r >= 15f) assertTrue(MapAreas.onIsland(cx, cy, w, h, cr), "cave in the sea (stop $stop) at ${w}x$h")
                // Every area's picture and label still open their area.
                MapAreas.all.forEach { a ->
                    assertEquals(MapTarget.Area(a.land), MapAreas.target(a.markX * w, a.markY * h, w, h, half, lh, sx, sy, r, cx, cy, cr), "picture of ${a.land} with the cave at ${w}x$h")
                }
            }
        }
    }

    @Test
    fun finishedStarsEachTakeTheirOwnTapAndNeverHidePicturesOrLabels() {
        for (c in cases) {
            val (w, h, half, lh, r) = c.toList()
            if (w / r < 15f) continue
            // A second lap: every stop finished, the next stop back at the start.
            val (fx, fy) = Trail.position(0)
            val (sx, sy) = MapAreas.stopSpot(Trail.landAt(0), fx, fy, w, h, r, half, lh)
            val placed = mutableListOf(MapSpot(MapTarget.Stop, sx, sy, r))
            val cr = r * 0.8f
            val (cx, cy) = MapAreas.caveSpot(w, h, cr, sx, sy, r, half, lh)
            placed += MapSpot(MapTarget.Cave, cx, cy, cr)
            val sr = r * 0.7f
            for (story in VoiceStories.all) {
                val (x, y) = MapAreas.spotBy(story.land, w, h, sr, placed, half, lh)
                placed += MapSpot(MapTarget.Story(story.id), x, y, sr)
            }
            val starR = r * 0.45f
            val stars = MapAreas.doneSpots((1 until Trail.size).toList(), w, h, starR, placed, half, lh)
            val spots = placed.filter { it.target is MapTarget.Story } + stars
            for (st in stars) {
                assertEquals(st.target, MapAreas.target(st.x, st.y, w, h, half, lh, sx, sy, r, cx, cy, cr, spots), "${st.target} centre at ${w}x$h")
            }
            MapAreas.all.forEach { a ->
                assertEquals(MapTarget.Area(a.land), MapAreas.target(a.markX * w, a.markY * h, w, h, half, lh, sx, sy, r, cx, cy, cr, spots), "picture of ${a.land} with stars at ${w}x$h")
            }
        }
    }

    @Test
    fun theVoiceStoryStopsSitByTheirLandsAndNeverShareATap() {
        for (c in cases) {
            val (w, h, half, lh, r) = c.toList()
            for (stop in 0 until Trail.size) {
                val (fx, fy) = Trail.position(stop)
                val (sx, sy) = MapAreas.stopSpot(Trail.landAt(stop), fx, fy, w, h, r, half, lh)
                val placed = mutableListOf(MapSpot(MapTarget.Stop, sx, sy, r))
                // The cave first (as the map places it), then the stories.
                val cr = r * 0.8f
                val (cx, cy) = MapAreas.caveSpot(w, h, cr, sx, sy, r, half, lh)
                placed += MapSpot(MapTarget.Cave, cx, cy, cr)
                val sr = r * 0.7f
                for (story in VoiceStories.all) {
                    val (x, y) = MapAreas.spotBy(story.land, w, h, sr, placed, half, lh)
                    placed += MapSpot(MapTarget.Story(story.id), x, y, sr)
                }
                val spots = placed.filter { it.target is MapTarget.Story }
                // Tablet-sized maps: no two stops touch, each story is by its land, and its middle opens it.
                if (w / r >= 15f) {
                    for (i in placed.indices) for (j in i + 1 until placed.size) {
                        val a = placed[i]
                        val b = placed[j]
                        val d = kotlin.math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
                        assertTrue(d >= a.r + b.r, "${a.target} touches ${b.target} (stop $stop) at ${w}x$h")
                    }
                    for (story in VoiceStories.all) {
                        val sp = spots.first { it.target == MapTarget.Story(story.id) }
                        assertTrue(MapAreas.belongsTo(story.land, sp.x, sp.y, w, h), "${story.id} not by ${story.land} (stop $stop) at ${w}x$h")
                        assertTrue(MapAreas.onIsland(sp.x, sp.y, w, h, sr), "${story.id} in the sea (stop $stop) at ${w}x$h")
                        assertEquals(MapTarget.Story(story.id), MapAreas.target(sp.x, sp.y, w, h, half, lh, sx, sy, r, cx, cy, cr, spots), "${story.id} centre at ${w}x$h")
                    }
                }
                // Pictures and labels always open their area, whatever stops there are.
                MapAreas.all.forEach { a ->
                    assertEquals(MapTarget.Area(a.land), MapAreas.target(a.markX * w, a.markY * h, w, h, half, lh, sx, sy, r, cx, cy, cr, spots), "picture of ${a.land} with story stops at ${w}x$h")
                }
            }
        }
    }

    @Test
    fun theSunflowerOpensTheMeadowEvenWhenTheNextStopIsThere() {
        val (w, h, half, lh, r) = listOf(1000f, 570f, 64f, 40f, 48f)
        val m = MapAreas.of(Land.MEADOW)
        val (fx, fy) = Trail.position(2) // Meadow's first stop, just under the sunflower
        val (sx, sy) = MapAreas.stopSpot(Land.MEADOW, fx, fy, w, h, r, half, lh)
        // The lower half of the sunflower, where the button used to sit.
        assertEquals(MapTarget.Area(Land.MEADOW), MapAreas.target(m.markX * w, m.markY * h + 0.1f * h, w, h, half, lh, sx, sy, r))
        // The gap between the label and the picture.
        assertEquals(MapTarget.Area(Land.MEADOW), MapAreas.target(m.markX * w, (m.labelY * h + lh + m.markY * h) / 2, w, h, half, lh, sx, sy, r))
    }
}

/** "Tap the B!" after saying a word: 2 or 3 letters, the sound's own letter among them, played like every quiz. */
class TapLetterTest {
    @Test
    fun theSoundsLetterIsAlwaysThereAndAWrongTapIsNeverRight() {
        val rnd = Random(7)
        for (sound in com.zeronolidge.speechapp.data.SpeechContent.sounds) for (n in 2..3) repeat(20) {
            val k = AnswerKey.tapLetter(sound, n, rnd)
            assertEquals(n, k.options.size)
            assertEquals(sound.label, k.answer)
            assertTrue(k.answer in k.options)
            assertEquals(k.options.size, k.options.distinct().size)
            var s = QuizState()
            val wrong = k.options.first { it != k.answer }
            repeat(3) {
                val (next, res) = s.tap(wrong == k.answer)
                s = next
                assertTrue(res != TapResult.RIGHT)
            }
            assertTrue(s.revealed)
        }
    }
}

/** Pre-made voice clips: sentences are matched to clips the same way the build script makes them. */
class ClipTextTest {
    private val clips = setOf("pip loves", "say", "great job", "a", "big", "red", "ball", "listen", "ah", "apple", "says", "pig")

    @Test
    fun normalizesLikeTheBuildScript() {
        assertEquals("great job saying it together", ClipText.normalize("Great job, saying it together!"))
        assertEquals("millie's turn", ClipText.normalize("Millie’s turn…"))
        assertEquals(listOf("Pip loves Grandma", "Say", "Grandma"), ClipText.phrases("Pip loves Grandma! Say: Grandma!"))
    }

    @Test
    fun clipsBackToBackAndHisWordsByTheDeviceVoice() {
        val plan = ClipText.plan("Pip loves Grandma! Say: Grandma!") { it in clips }
        assertEquals(
            listOf(ClipText.Part.Clips(listOf("pip loves")), ClipText.Part.Speak("grandma"), ClipText.Part.Clips(listOf("say")), ClipText.Part.Speak("grandma")),
            plan,
        )
        assertEquals(listOf(ClipText.Part.Clips(listOf("a", "big", "red", "ball"))), ClipText.plan("A big red ball!") { it in clips })
        assertEquals(listOf(ClipText.Part.Clips(listOf("listen", ClipText.PAUSE, "ah", "ah", "apple", ClipText.PAUSE, "a", "says", "ah"))), ClipText.plan("Listen: ah, ah, apple! A says ah.") { it in clips })
        // A whole sentence with a clip of its own is one clip.
        assertEquals(listOf(ClipText.Part.Clips(listOf("great job"))), ClipText.plan("Great job!") { it in clips })
        assertEquals(listOf(ClipText.Part.Speak("hello sam")), ClipText.plan("Hello Sam") { it in clips })
        // Long story sentences (up to 22 words) still match their own clip.
        val long = "we can see the kite string past a wobbly bridge but some boards are missing and the river is very wide today"
        assertEquals(listOf(ClipText.Part.Clips(listOf(long))), ClipText.plan(long.replaceFirstChar { it.uppercase() } + "!") { it == long })
    }
}

/** A sitting of play survives a reload; a long break starts a new one; Rory wraps up once. */
class PlaySessionTest {
    private val min = 60_000L

    @Test
    fun aReloadCarriesOnAndALongBreakStartsAgain() {
        val start = 1_000_000L
        val s = PlaySession.resume(null, start)
        assertEquals(start, s.start)
        // Reloaded 5 minutes in: the same sitting.
        val reloaded = PlaySession.resume(PlaySession.decode(s.seen(start + 5 * min).encode()), start + 6 * min)
        assertEquals(start, reloaded.start)
        assertTrue(!reloaded.long(start + 11 * min))
        assertTrue(reloaded.long(start + 12 * min))
        // Back after an hour: a new sitting.
        assertEquals(start + 70 * min, PlaySession.resume(reloaded, start + 70 * min).start)
        // Once Rory has wrapped up, not again in the same sitting, even after a reload.
        val rested = reloaded.copy(rested = true)
        assertTrue(!PlaySession.resume(PlaySession.decode(rested.encode()), start + 13 * min).long(start + 20 * min))
        assertEquals(null, PlaySession.decode(""))
        assertEquals(null, PlaySession.decode("junk"))
    }
}

/** The voice stories follow the pattern: short, their sounds up front, a calm goodbye. */
class VoiceStoriesTest {
    @Test
    fun everyStoryFollowsThePattern() {
        for (s in VoiceStories.all) {
            val maxBeats = if (s.id == "kitchen") VoiceStories.KITCHEN_MAX_BEATS else VoiceStories.MAX_BEATS
            assertTrue(s.beats.size in 6..maxBeats, "${s.id}: ${s.beats.size} beats")
            // His sounds start the words of at least 4 beats; each beat's sound is really in its word.
            val starting = s.beats.count { b -> b.sound != null && VoiceStories.startsWith(b.word, b.sound!!) }
            assertTrue(starting >= 4, "${s.id}: only $starting beats start with their sound")
            for (b in s.beats) {
                b.sound?.let { snd ->
                    assertTrue(snd in s.sounds, "${s.id} ${b.word}: $snd isn't one of ${s.sounds}")
                    assertTrue(VoiceStories.startsWith(b.word, snd) || VoiceStories.endsWith(b.word, snd), "${s.id}: '${b.word}' has no $snd")
                }
                // Three versions: one word, two words, a short sentence.
                assertEquals(3, b.lines.size, "${s.id} ${b.word}")
                assertEquals(1, b.lines[0].trim().split(' ').size, "${s.id}: '${b.lines[0]}' is one word")
                assertTrue(ClipText.normalize(b.lines[1]).split(' ').size == 2, "${s.id}: '${b.lines[1]}' is two words")
                assertTrue(ClipText.normalize(b.lines[2]).split(' ').size >= 2, "${s.id}: '${b.lines[2]}' is a sentence")
                // Rory says the word back first, and nothing has a blank to fill. The game beat's
                // close is the exception: whatever colour he said, the balloon settles into the
                // gap the same way (see BalloonGame.success), so its success line doesn't start
                // with the colour; it also never says "pop" for him (he's just said it himself).
                if (!b.game) assertTrue(ClipText.normalize(b.success).startsWith(ClipText.normalize(b.word)), "${s.id}: success '${b.success}'")
                if (b.game) assertFalse(b.success.contains("pop", ignoreCase = true), "${s.id}: success '${b.success}'")
                (listOf(b.problem, b.success) + b.lines).forEach { assertTrue('{' !in it, "${s.id}: blank in '$it'") }
            }
            // It ends calmly, with the goodbye beat last (and only last).
            assertTrue(s.beats.last().goodbye && s.beats.dropLast(1).none { it.goodbye }, s.id)
            assertTrue(s.ending.contains("sticker"), s.id)
            // Later stories use sounds at the ends of words too ("up", "stop").
        }
        assertTrue(VoiceStories.boat.beats.any { b -> b.sound != null && VoiceStories.endsWith(b.word, b.sound!!) && !VoiceStories.startsWith(b.word, b.sound!!) })
    }

    @Test
    fun atMostOneGameBeatPerStory() {
        for (s in VoiceStories.all) assertTrue(s.beats.count { it.game } <= 1, "${s.id} has more than one game beat")
        assertEquals(1, VoiceStories.balloon.beats.count { it.game })
    }

    @Test
    fun theStoriesShareTheMovesAndBackdrops() {
        // A handful of moves carry every beat, and backdrops are reused across beats.
        val beats = VoiceStories.all.flatMap { it.beats }
        assertTrue(beats.map { it.move }.toSet().size <= Move.entries.size)
        assertTrue(beats.map { it.backdrop }.toSet().size * 3 <= beats.size, "backdrops are shared: ${beats.map { it.backdrop }.toSet()}")
    }

    @Test
    fun hisOwnWordStandsInForTheProp() {
        // A name for the boat story's duck.
        val boat = VoiceStories.ready(VoiceStories.boat, listOf("ball" to "⚽", "Grandma" to "👵"))
        val come = boat.beats.first { it.word == "come" }
        assertEquals("come Grandma", come.lines[1])
        assertEquals("Oh no! Grandma is stuck on a rock.", come.problem)
        assertEquals("Come! Come, Grandma! Grandma paddles over to the boat!", come.success)
        assertEquals("👵", come.propEmoji)
        // A thing for the owl's berries.
        val owl = VoiceStories.ready(VoiceStories.owl, listOf("Grandma" to "👵", "ball" to "⚽"))
        assertEquals("More ball, please!", owl.beats.first { it.word == "more" }.lines[2])
        // No own words: the story as written.
        assertEquals(VoiceStories.boat, VoiceStories.ready(VoiceStories.boat, emptyList()))
    }

    @Test
    fun theSpeechStepPicksTheLine() {
        val b = VoiceStories.boat.beats[0]
        assertEquals("go", b.line(0))
        assertEquals("go boat", b.line(1))
        assertEquals("Go, boat, go!", b.line(2))
        assertEquals("Go, boat, go!", b.line(5))
    }

    @Test
    fun aSoftTryIsNotSilence() {
        fun meter(levels: List<Float>) = VoiceMeter().also { m -> levels.forEach { m.feed(it, 50) } }
        val room = List(6) { 0.004f }
        val soft = meter(room + List(10) { 0.006f } + List(70) { 0.004f })
        assertTrue(!soft.heard && soft.soft)
        val quiet = meter(room + List(80) { 0.004f })
        assertTrue(!quiet.heard && !quiet.soft)
        val loud = meter(room + List(10) { 0.05f } + List(20) { 0.004f })
        assertTrue(loud.heard && !loud.soft)
    }

    @Test
    fun isFWordCatchesFAndSilentHPhButNotOtherSounds() {
        assertTrue(VoiceStories.isFWord("fly"))
        assertTrue(VoiceStories.isFWord("Fly up high, owl!"))
        assertTrue(VoiceStories.isFWord("fish"))
        assertTrue(VoiceStories.isFWord("phone"))
        assertTrue(VoiceStories.isFWord("  Fox"))
        assertFalse(VoiceStories.isFWord("van"))
        assertFalse(VoiceStories.isFWord("pancake"))
        assertFalse(VoiceStories.isFWord(""))
    }
}

/** His voice pushes a voice story's action: louder is faster, holding keeps going, quiet is never a miss. */
class VoicePushTest {
    @Test
    fun louderPushesFurtherAndHoldingKeepsItMoving() {
        val soft = VoicePush().apply { repeat(4) { feed(0.15f, 50) } }
        val loud = VoicePush().apply { repeat(4) { feed(0.9f, 50) } }
        assertTrue(loud.progress > soft.progress * 2, "${loud.progress} vs ${soft.progress}")
        assertTrue(loud.strength > soft.strength)
        // Silence doesn't move it; holding the sound does, until the end.
        val p = VoicePush()
        p.feed(0.5f, 50)
        val before = p.progress
        repeat(10) { p.feed(0f, 50) }
        assertEquals(before, p.progress)
        var done = false
        repeat(40) { if (p.feed(0.5f, 50)) done = true }
        assertTrue(done && p.progress == 1f)
        // It starts moving on the very first reading of voice (well within 0.2 s).
        assertTrue(VoicePush().apply { feed(0.1f, 50) }.progress > 0f)
    }

    @Test
    fun aQuietTryNudgesAndAsksAgainThenAlwaysFinishes() {
        val p = VoicePush()
        assertEquals(VoicePush.Next.AGAIN_TOGETHER, p.afterTry(heard = false, soft = true))
        assertTrue(p.progress > 0f && p.progress <= VoicePush.NUDGE_CAP)
        // After two quiet tries it waits (never moves on by itself), and then any sound finishes it.
        assertEquals(VoicePush.Next.WAIT, p.afterTry(heard = false, soft = false))
        assertTrue(p.waiting)
        assertEquals(VoicePush.Next.WAIT, p.afterTry(heard = false, soft = false))
        assertEquals(VoicePush.Next.FINISH_GENTLY, p.afterTry(heard = false, soft = true))
        // Any clear voice finishes (the rest gently), even if it didn't push all the way.
        val q = VoicePush().apply { feed(0.3f, 50) }
        assertEquals(VoicePush.Next.FINISH_GENTLY, q.afterTry(heard = true, soft = false))
        // A quiet try never reaches the end by itself.
        val r = VoicePush()
        r.afterTry(false, true)
        assertTrue(r.progress < 1f)
    }

    @Test
    fun popSoundsCountEachBurst() {
        assertEquals(VoicePush.Mode.BURSTS, VoicePush.modeFor("p"))
        assertEquals(VoicePush.Mode.HOLD, VoicePush.modeFor("sh"))
        assertEquals(VoicePush.Mode.LOUDNESS, VoicePush.modeFor(null))
        // "pop! pop!" = two pushes, whatever the length of each.
        val p = VoicePush(VoicePush.Mode.BURSTS)
        repeat(4) { p.feed(0.6f, 50) }
        repeat(4) { p.feed(0f, 50) }
        repeat(2) { p.feed(0.6f, 50) }
        assertEquals(2, p.bursts)
        // Holding one pop sound doesn't keep pushing.
        val held = VoicePush(VoicePush.Mode.BURSTS).apply { repeat(30) { feed(0.6f, 50) } }
        assertEquals(1, held.bursts)
        assertTrue(held.progress < 0.5f)
        // A one-reading dip inside a word isn't a second pop.
        val dip = VoicePush(VoicePush.Mode.BURSTS).apply { feed(0.6f, 50); feed(0f, 50); feed(0.6f, 50) }
        assertEquals(1, dip.bursts)
        // Three pops get there.
        val three = VoicePush(VoicePush.Mode.BURSTS)
        repeat(3) { three.feed(0.3f, 50); repeat(3) { three.feed(0f, 50) } }
        assertEquals(1f, three.progress)
    }

    @Test
    fun longSoundsGoByHowLongTheyAreHeld() {
        // A quiet "ssss" held for 2 s gets there; a loud short one doesn't.
        val quietLong = VoicePush(VoicePush.Mode.HOLD).apply { repeat(40) { feed(0.15f, 50) } }
        val loudShort = VoicePush(VoicePush.Mode.HOLD).apply { repeat(6) { feed(1f, 50) } }
        assertEquals(1f, quietLong.progress)
        assertTrue(loudShort.progress < 0.5f)
    }

    @Test
    fun iSaidItIsAtLeastMedium() {
        val faint = VoicePush().apply { feed(0.1f, 50) }
        val before = faint.strength
        faint.pressed()
        assertTrue(faint.strength > before)
        assertTrue(faint.strength >= 0.45f + 0.55f * VoicePush.PRESSED)
        // A big voice stays big.
        val loud = VoicePush().apply { feed(0.9f, 50) }
        val s = loud.strength
        loud.pressed()
        assertEquals(s, loud.strength)
    }

    /** Rory's Kitchen's milk beat (Aida): a short or quiet "mmm" must still pour a little milk,
     * never nothing, while a longer one still clearly pours more; only true silence pours none. */
    @Test
    fun aShortOrQuietHoldStillPoursSomethingNeverZero() {
        // A quiet "mmm" right at VoiceMeter's own push floor (0.08), held about 150 ms: short
        // and quiet is exactly the case that used to work out to only a few percent.
        val shortQuiet = VoicePush(VoicePush.Mode.HOLD).apply { repeat(3) { feed(0.08f, 50) } }
        assertTrue(shortQuiet.progress > 0f)
        assertTrue(shortQuiet.progress >= VoicePush.FIRST_VOICE_FLOOR, "${shortQuiet.progress}")
        // The same quiet level held twice as long (about 300 ms) pours more, never less.
        val longerQuiet = VoicePush(VoicePush.Mode.HOLD).apply { repeat(6) { feed(0.08f, 50) } }
        assertTrue(longerQuiet.progress > shortQuiet.progress)
        // The same holds for a word with no named sound (LOUDNESS mode).
        val shortLoudness = VoicePush().apply { repeat(3) { feed(0.08f, 50) } }
        assertTrue(shortLoudness.progress >= VoicePush.FIRST_VOICE_FLOOR)
        // Nothing voiced at all pours nothing.
        val none = VoicePush(VoicePush.Mode.HOLD).apply { repeat(10) { feed(0f, 50) } }
        assertEquals(0f, none.progress)
        // "I said it!" shows some result too, even pressed cold with no voice attempted at all.
        val pressedCold = VoicePush().apply { pressed() }
        assertTrue(pressedCold.progress >= VoicePush.FIRST_VOICE_FLOOR)
        // The floor never shrinks the gap between a quiet short try and a loud one that reaches
        // the end by itself (the existing louder-pushes-further guarantee must still hold).
        val quiet4 = VoicePush().apply { repeat(4) { feed(0.15f, 50) } }
        val loud4 = VoicePush().apply { repeat(4) { feed(0.9f, 50) } }
        assertTrue(loud4.progress > quiet4.progress * 2, "${loud4.progress} vs ${quiet4.progress}")
    }
}

/** The room's loudness is reused between turns, so it's never measured while Rory talks. */
class RoomLevelTest {
    @Test
    fun aKnownRoomNeedsNoSettling() {
        // No 300 ms settle: after a short lead (Rory's last echo), voice counts at once.
        val m = VoiceMeter(knownRoom = 0.004f)
        assertEquals(0.004f, m.room)
        repeat(4) { m.feed(0.05f, 50) }
        assertTrue(m.push(0.05f) > 0f)
        repeat(3) { m.feed(0.05f, 50) }
        assertTrue(m.heard)
        assertEquals(0.004f, m.room)
        // Without it, the first readings only measure the room (here: his voice, setting the bar too high).
        val fresh = VoiceMeter()
        repeat(6) { fresh.feed(0.05f, 50) }
        assertTrue(fresh.threshold > 0.07f)
    }

    @Test
    fun theSavedRoomFollowsTheRoomUpAndDownButNotHisVoice() {
        // His word (short) doesn't raise the saved room.
        val m = VoiceMeter(knownRoom = 0.005f)
        repeat(10) { m.feed(0.005f, 50) }
        repeat(10) { m.feed(0.08f, 50) }
        repeat(30) { m.feed(0.006f, 50) }
        val r = m.roomAfter()!!
        assertTrue(r in 0.004f..0.007f, "room $r")
        // A louder room raises it all the way (no cap), so the bar keeps up.
        val n = VoiceMeter(knownRoom = 0.004f)
        repeat(60) { n.feed(0.02f, 50) }
        assertTrue(n.roomAfter()!! >= 0.019f, "${n.roomAfter()}")
    }

    @Test
    fun backgroundNoiseNeverCountsAsHisTry() {
        // The room got louder before the turn (TV on): the lead sees it, and noise isn't voice.
        val tv = VoiceMeter(knownRoom = 0.004f)
        var done = false
        repeat(80) { if (!done) done = tv.feed(0.03f, 50) }
        assertTrue(!tv.heard, "steady noise counted as voice")
        // Noise that starts mid-turn and never dips: past the longest hold it's the room, not his try.
        val mid = VoiceMeter(knownRoom = 0.004f)
        repeat(6) { mid.feed(0.004f, 50) }
        var midDone = false
        repeat(140) { if (!midDone) midDone = mid.feed(0.05f, 50) }
        assertTrue(midDone && !mid.heard)
        // He still gets heard over the louder room.
        val over = VoiceMeter(knownRoom = 0.004f)
        repeat(4) { over.feed(0.03f, 50) }
        repeat(6) { over.feed(0.03f, 50) }
        repeat(6) { over.feed(0.2f, 50) }
        assertTrue(over.heard)
    }
}

/** Word listening: swaps by sound, judging, records, agreement, gentle corrections, parent cards. */
class WordCheckTest {
    @Test
    fun swapsFollowTheSoundTable() {
        val pig = Swaps.forWord("pig", "p")
        assertTrue("big" in pig, "$pig")
        assertTrue(pig.size <= 3)
        assertEquals(SoundPos.START, Swaps.position("pig", "p"))
        assertEquals(SoundPos.END, Swaps.position("cup", "p"))
        assertEquals(SoundPos.MIDDLE, Swaps.position("happy", "p"))
        // Final sound dropped for p, t, d, k, s at the end.
        assertTrue("cu" in Swaps.forWord("cup", "p") || Swaps.forWord("cup", "p").any { it.length == 2 })
        assertTrue("bat" in Swaps.forWord("mat", "m"))
        assertTrue("dog" in Swaps.forWord("dog", "d").map { it } == false)
        assertTrue("dug" in Swaps.forWord("duck", "k"), "${Swaps.forWord("duck", "k")}")
        assertTrue("cop" in Swaps.forWord("top", "t"), "${Swaps.forWord("top", "t")}")
        assertTrue("sip" in Swaps.forWord("ship", "sh"))
        assertTrue("pan" in Swaps.forWord("fan", "f"))
        // Real words come first.
        assertTrue(Swaps.forWord("pig", "p").first() in RealWords.all)
    }

    @Test
    fun clustersAndDevelopingSoundsAreNeverScored() {
        assertTrue(Swaps.inCluster("stop", "s"))
        assertTrue(Swaps.inCluster("blue", "b"))
        assertTrue(!Swaps.inCluster("pig", "p"))
        assertTrue(!Swaps.inCluster("happy", "p"))
        assertTrue("top" in Swaps.forWord("stop", "s"), "${Swaps.forWord("stop", "s")}")
        for (s in listOf("r", "l", "s", "z", "sh", "ch", "j", "th", "v")) assertTrue(!Swaps.check("${s}ip", s, true)!!.scored, s)
        assertTrue(!Swaps.check("blue", "b", true)!!.scored)
        assertTrue(Swaps.check("pig", "p", true)!!.scored)
    }

    @Test
    fun judgingIsGenerousAndUnsureIsAPass() {
        val c = Swaps.check("big pig", "p", true)!!
        assertEquals("pig", c.target)
        assertTrue("big" in c.others && "big" !in c.swaps)
        assertEquals(Heard.RIGHT, Judge.judge(c, listOf("big" to 0.9f, "pig" to 0.8f)).second.first)
        assertEquals(Heard.UNSURE, Judge.judge(c, listOf("pig" to 0.3f)).second.first)
        assertEquals(Heard.UNSURE, Judge.judge(c, listOf("[unk]" to 1f)).second.first)
        assertEquals(Heard.UNSURE, Judge.judge(c, emptyList()).second.first)
        val swap = c.swaps.first()
        assertEquals(Heard.NEAR_MISS, Judge.judge(c, listOf(swap to 0.9f)).second.first)
        assertEquals(Heard.UNSURE, Judge.judge(c, listOf(swap to 0.6f)).second.first)
        // A lisp-like "th" for s counts as right.
        val sun = Swaps.check("sun", "s", true)!!
        assertEquals(Heard.RIGHT, Judge.judge(sun, listOf("thun" to 0.9f)).second.first)
        assertTrue("[unk]" in c.grammar)
    }

    @Test
    fun recordsRoundTripAndKeepNoText() {
        val r = TryRecord(100, "pig", "p", SoundPos.START, "big", 0.87f, true, Heard.NEAR_MISS, true, null)
        val back = TryRecord.decode(r.encode())!!
        assertEquals(r.copy(conf = 0.87f), back.copy(conf = 0.87f))
        assertTrue(kotlin.math.abs(back.conf - 0.87f) < 0.011f)
        assertEquals(listOf(back.copy(rating = true)), TryRecord.decodeAll(TryRecord.encodeAll(listOf(r.copy(rating = true)))).map { it.copy(conf = back.conf) })
        assertEquals(TryRecord.MAX, TryRecord.decodeAll(TryRecord.encodeAll(List(TryRecord.MAX + 5) { r })).size)
    }

    @Test
    fun modellingUnlocksOnlyAfter30RatedTriesAt90Percent() {
        fun rec(result: Heard, rating: Boolean) = TryRecord(1, "pig", "p", SoundPos.START, "pig", 0.9f, true, result, true, rating)
        val agree = List(29) { rec(Heard.RIGHT, true) }
        assertTrue(!Agreement.modelingAllowed(agree))
        assertTrue(Agreement.modelingAllowed(agree + rec(Heard.NEAR_MISS, false)))
        val some = List(26) { rec(Heard.RIGHT, true) } + List(4) { rec(Heard.RIGHT, false) }
        assertTrue(!Agreement.modelingAllowed(some))
        // Unsure tries don't count either way.
        assertEquals(0, Agreement.called(List(40) { rec(Heard.UNSURE, true) }).size)
    }

    @Test
    fun correctionsAreRareNeverTwiceInARowAndStopWhenHeTires() {
        val c = Corrections()
        c.noteTry("p", 0.5f, 600)
        assertTrue(!c.mayCorrect("p"))
        c.noteTry("p", 0.5f, 600)
        c.noteTry("p", 0.5f, 600)
        assertTrue(c.mayCorrect("p"))
        c.noteReply("p", true)
        c.noteTry("p", 0.5f, 600)
        assertTrue(!c.mayCorrect("p"))
        c.noteReply("p", false)
        c.noteTry("p", 0.5f, 600)
        assertTrue(!c.mayCorrect("p"))
        c.noteReply("p", false)
        c.noteTry("p", 0.5f, 600)
        assertTrue(c.mayCorrect("p"))
        // Quieter, shorter tries: no more corrections this sitting.
        val t = Corrections()
        repeat(3) { t.noteTry("b", 0.6f, 700) }
        repeat(3) { t.noteTry("b", 0.2f, 300) }
        assertTrue(t.stopped && !t.mayCorrect("b"))
        // Replies: a confident near-miss on a scored sound may be modelled; never on developing sounds.
        val check = Swaps.check("pig", "p", true)!!
        val fresh = Corrections().apply { repeat(3) { noteTry("p", 0.5f, 600) } }
        assertEquals(Reply.MODEL_SOUND, Corrections.reply(check, Heard.NEAR_MISS, 0.9f, fresh))
        assertEquals(Reply.ECHO, Corrections.reply(check, Heard.NEAR_MISS, 0.75f, fresh))
        assertEquals(Reply.CELEBRATE, Corrections.reply(check, Heard.RIGHT, 0.9f, fresh))
        assertEquals(Reply.ECHO, Corrections.reply(Swaps.check("sun", "s", true)!!, Heard.NEAR_MISS, 0.95f, Corrections().apply { repeat(3) { noteTry("s", 0.5f, 600) } }))
    }

    @Test
    fun soundCardsNeedAbout20TriesAndSuggestFocusWords() {
        fun rec(day: Int, right: Boolean, pos: SoundPos = SoundPos.START, sound: String = "p") =
            TryRecord(day, "pig", sound, pos, "x", 0.9f, true, if (right) Heard.RIGHT else Heard.NEAR_MISS, true)
        val few = List(19) { rec(100, true) }
        assertTrue(SoundCards.cards(few, 100).isEmpty())
        val many = List(10) { rec(80, false) } + List(10) { rec(100, it % 2 == 0, SoundPos.END) } + List(5) { rec(100, true) }
        val card = SoundCards.cards(many, 100).single()
        assertEquals("p", card.sound)
        assertEquals(4, card.weeks.size)
        assertTrue(card.weeks.first() == null || card.weeks.first()!! < card.weeks.last()!!)
        assertTrue(card.byPos[SoundPos.MIDDLE] == null)
        val focus = SoundCards.focus(listOf(card))
        assertEquals("p", focus.single().first)
        assertEquals(3, focus.single().second.size)
        // Developing sounds never get a card.
        assertTrue(SoundCards.cards(List(30) { rec(100, false).copy(sound = "r", scored = false) }, 100).isEmpty())
    }
}

/** Round 15: long held sounds count, pops must sound like a voice and last a moment. */
class HoldAndPopTest {
    @Test
    fun aLongHeldSssssCountsAndKeepsListening() {
        // 0.5 s quiet, then "sssss" for 4 s: still his, the room isn't raised, listening waits for it.
        val m = VoiceMeter(knownRoom = 0.004f)
        repeat(10) { m.feed(0.004f, 50) }
        var done = false
        var pushes = 0
        repeat(80) { if (!done) { done = m.feed(0.03f, 50); if (m.lastPush > 0f) pushes++ } }
        assertTrue(m.heard, "a 4 s hold must count")
        assertEquals(80, pushes, "every reading of the hold pushes")
        assertTrue(!done, "listening ends only after he stops")
        assertTrue(m.room < 0.01f, "the room was raised during his sound: ${m.room}")
        repeat(30) { if (!done) done = m.feed(0.004f, 50) }
        assertTrue(done && m.heard)
        assertTrue(m.roomAfter()!! < 0.01f, "the saved room took in his sound: ${m.roomAfter()}")
    }

    @Test
    fun noiseFromTheStartIsStillTheRoomAfterTwoSeconds() {
        // Sound with no quiet before it at all (a TV louder than even the lead allows) is room after 2 s.
        val tv = VoiceMeter(knownRoom = 0.004f)
        var done = false
        repeat(80) { if (!done) done = tv.feed(0.06f, 50) }
        assertTrue(!tv.heard)
    }

    @Test
    fun popWordsCountPuffsWhispersAndWordsButNotCoughsOrClaps() {
        fun pops(vararg readings: Triple<Float, Float, Float>): VoiceMeter {
            val m = VoiceMeter(knownRoom = 0.004f, needMs = 50, pops = true)
            repeat(6) { m.feed(0.004f, 50) }
            readings.forEach { (rms, band, per) -> m.feed(rms, 50, band, per) }
            repeat(20) { m.feed(0.004f, 50) }
            return m
        }
        val voice = Triple(0.08f, 0.7f, 0.8f)
        // "Pop!": voiced for about 100 ms.
        assertTrue(pops(voice, voice).heard)
        // A bare "p" puff: one short burst in the voice range.
        assertTrue(pops(Triple(0.06f, 0.5f, 0.1f)).heard)
        // A clap or a tap on the glass: one short click outside the voice range.
        assertTrue(!pops(Triple(0.3f, 0.15f, 0.1f)).heard)
        // A cough: a burst that dies straight away.
        assertTrue(!pops(Triple(0.12f, 0.5f, 0.15f), Triple(0.05f, 0.5f, 0.1f), Triple(0.03f, 0.5f, 0.1f)).heard)
        // A puff split across two readings, or with a quiet reading between, still counts.
        assertTrue(pops(Triple(0.03f, 0.5f, 0.1f), Triple(0.025f, 0.5f, 0.1f)).heard)
        assertTrue(pops(Triple(0.04f, 0.5f, 0.1f), Triple(0.004f, 0.5f, 0.1f), Triple(0.03f, 0.5f, 0.1f)).heard)
        // A loud whispered "Pip!": unvoiced, but it holds its loudness.
        assertTrue(pops(Triple(0.05f, 0.5f, 0.1f), Triple(0.045f, 0.5f, 0.1f), Triple(0.045f, 0.5f, 0.1f), Triple(0.035f, 0.5f, 0.1f)).heard)
        // A puff whose first reading is mostly its faint edge (a low voice-range share) still
        // counts once its louder second half is weighed in.
        assertTrue(pops(Triple(0.03f, 0.2f, 0.1f), Triple(0.05f, 0.55f, 0.1f)).heard)
        // A puff split so neither half alone is loud enough, but together they are, still counts.
        assertTrue(pops(Triple(0.0046f, 0.5f, 0.1f), Triple(0.0046f, 0.5f, 0.1f)).heard)
        // Steady room-level readings, each well under the near-threshold band, never count.
        assertTrue(!pops(Triple(0.004f, 0.5f, 0.1f), Triple(0.004f, 0.5f, 0.1f), Triple(0.004f, 0.5f, 0.1f)).heard)
    }

    @Test
    fun onlyTheSecondReadingOfAPopPushes() {
        val m = VoiceMeter(knownRoom = 0.004f, needMs = 50, pops = true)
        repeat(6) { m.feed(0.004f, 50) }
        m.feed(0.08f, 50, 0.7f, 0.8f)
        assertEquals(0f, m.lastPush)
        m.feed(0.08f, 50, 0.7f, 0.8f)
        assertTrue(m.lastPush > 0f)
    }

    @Test
    fun popsCountsMergesASplitPuffButIsNoStricter() {
        // A bare "p" puff in one reading, or split across two (readings aren't lined up with his
        // puff), both count once merged into a single burst.
        assertTrue(Pops.counts(listOf(0.06f), 0.5f))
        assertTrue(Pops.counts(listOf(0.06f, 0.04f), 0.5f))
        // Still no stricter than before: a click outside the voice range doesn't count either way.
        assertFalse(Pops.counts(listOf(0.3f), 0.15f))
        assertFalse(Pops.counts(listOf(0.3f, 0.3f), 0.15f))
    }
}

/** Rory's mouth follows the clip's loudness; p, b and m words start with closed lips. */
class LipSyncTest {
    @Test
    fun theMouthOpensWithTheSoundAndClosesInQuiet() {
        val lip = LipSync()
        var o = 0f
        repeat(5) { o = lip.step(0.12f, "go boat", 16) }
        assertTrue(o > 0.5f, "open $o")
        repeat(10) { o = lip.step(0.002f, "go boat", 16) }
        assertTrue(o < 0.05f, "closed $o")
    }

    @Test
    fun pbmWordsStartWithClosedLipsForAMoment() {
        val lip = LipSync()
        // "pig": the first 80 ms closed, then open.
        lip.step(0.12f, "pig", 16)
        assertTrue(lip.closed)
        repeat(4) { lip.step(0.12f, "pig", 16) }
        assertTrue(lip.closed)
        repeat(3) { lip.step(0.12f, "pig", 16) }
        assertTrue(!lip.closed)
        // "go, ball": the second word (after a short quiet) closes its lips; "go" didn't.
        val two = LipSync()
        two.step(0.12f, "go ball", 16)
        assertTrue(!two.closed)
        repeat(6) { two.step(0.12f, "go ball", 16) }
        repeat(5) { two.step(0.001f, "go ball", 16) }
        two.step(0.12f, "go ball", 16)
        assertTrue(two.closed)
    }

    @Test
    fun popPopPopClosesTheLipsOnEveryP() {
        val lip = LipSync()
        val text = "pop, pop, pop!"
        fun word(): Boolean {
            // Starts closed (p), opens, then dips (no quiet) into the next word: closed at the end (p).
            val start = lip.step(0.12f, text, 16).let { lip.closed }
            repeat(7) { lip.step(0.12f, text, 16) }
            val opened = !lip.closed
            lip.step(0.03f, text, 16)
            val end = lip.closed
            lip.step(0.03f, text, 16)
            return start && opened && end
        }
        assertTrue(word(), "first pop")
        assertTrue(word(), "second pop")
        assertTrue(word(), "third pop")
    }

    @Test
    fun aRippleInAWordsTailDoesntStealTheNextWordsClosure() {
        // "pop pop duck": a loud "pop" whose burst dips into its vowel and then ripples part way
        // back up, soon after it started, must not be read as the next word ("duck" doesn't
        // close, so if the ripple stole the second "pop"'s slot early, the real second "pop"
        // would open on "duck" instead and show no closure at all).
        val lip = LipSync()
        val text = "pop pop duck"
        lip.step(0.3f, text, 16) // "pop" #1 starts, closed
        assertTrue(lip.closed)
        lip.step(0.3f, text, 16)
        lip.step(0.1f, text, 16) // dips into its vowel
        lip.step(0.25f, text, 16) // a ripple back up, moments after the word started: not a new word
        lip.step(0.2f, text, 16)
        lip.step(0.1f, text, 16) // settles further, towards the real gap
        lip.step(0.05f, text, 16)
        lip.step(0.3f, text, 16) // the real "pop" #2: must close now, not land on "duck"
        assertTrue(lip.closed, "the real second pop didn't close")
    }

    @Test
    fun repeatedClipsOfTheSameWordEachCloseTheLips() {
        // "Pop! Pop! Pop!" plays as three clips with the same text, with a breath between.
        val lip = LipSync()
        repeat(3) { n ->
            lip.step(0.12f, "pop", 16)
            assertTrue(lip.closed, "pop ${n + 1} didn't close")
            repeat(8) { lip.step(0.12f, "pop", 16) }
            repeat(10) { lip.step(null, "pop", 16) }
        }
        // "bye bye duck": both byes close.
        val bye = LipSync()
        bye.step(0.12f, "bye bye duck", 16)
        assertTrue(bye.closed)
        repeat(8) { bye.step(0.12f, "bye bye duck", 16) }
        bye.step(0.03f, "bye bye duck", 16)
        bye.step(0.03f, "bye bye duck", 16)
        bye.step(0.12f, "bye bye duck", 16)
        assertTrue(bye.closed, "second bye didn't close")
    }

    @Test
    fun aHeldMmmKeepsTheLipsClosed() {
        val lip = LipSync()
        repeat(60) {
            lip.step(0.1f, "mmmmm", 16)
            assertTrue(lip.closed, "open at frame $it")
        }
    }

    @Test
    fun realClipLevelsCloseOnEveryPBM() {
        // RMS per 16 ms frame, measured from real played clips. Checks that every p/b/m word's
        // onset gets a lip closure (within a couple of frames either way) and that release
        // puffs and long, gentle dips don't create or hide a word.
        fun closesNear(levels: List<Float>, text: String, onsets: List<Int>) {
            val lip = LipSync()
            val closedAt = BooleanArray(levels.size)
            levels.forEachIndexed { i, lvl -> lip.step(lvl, text, 16); closedAt[i] = lip.closed }
            for (onset in onsets) {
                val lo = maxOf(0, onset - 1)
                val hi = minOf(levels.size - 1, onset + 2)
                assertTrue((lo..hi).any { closedAt[it] }, "\"$text\": no closure near onset $onset")
            }
        }

        closesNear(
            listOf(
                0.0007f, 0.0142f, 0.0612f, 0.073f, 0.0745f, 0.0691f, 0.109f, 0.2344f, 0.2897f, 0.2373f,
                0.1662f, 0.1227f, 0.0779f, 0.0219f, 0.0063f, 0.0031f, 0.002f, 0.0013f, 0.0113f, 0.0133f,
                0.0063f, 0.0048f, 0.0036f, 0.0027f, 0.0012f, 0.0009f, 0.0012f, 0.0013f, 0.0012f, 0.001f,
                0.0008f, 0.001f, 0.0011f, 0.0011f, 0.0014f, 0.0012f, 0.0011f, 0.0012f, 0.0008f, 0.0009f,
                0.0007f, 0.0183f, 0.0303f, 0.0326f, 0.0369f, 0.0923f, 0.2421f, 0.2847f, 0.2343f, 0.2121f,
                0.1818f, 0.1424f, 0.0919f, 0.0582f, 0.0239f, 0.0045f, 0.0034f, 0.0025f, 0.0017f, 0.0012f,
                0.0148f, 0.0128f, 0.007f, 0.0027f, 0.0017f, 0.0011f, 0.001f, 0.0012f, 0.0012f, 0.0013f,
                0.0009f, 0.0011f, 0.0013f, 0.001f, 0.0011f, 0.0012f, 0.001f, 0.0009f, 0.001f, 0.0012f,
                0.0012f, 0.0012f, 0.0012f, 0.0013f, 0.0014f, 0.0013f, 0.001f, 0.0013f, 0.0011f, 0.0169f,
                0.0391f, 0.0395f, 0.0417f, 0.0383f, 0.1709f, 0.2932f, 0.328f, 0.2692f, 0.1904f, 0.115f,
                0.074f, 0.035f, 0.0167f, 0.0061f, 0.0048f, 0.0038f, 0.0016f, 0.0008f, 0.0251f, 0.026f,
                0.008f, 0.0057f, 0.0043f, 0.0033f, 0.0016f, 0.0016f, 0.0014f, 0.0009f, 0.0004f,
            ),
            "pop pop pop", listOf(1, 41, 89),
        )
        closesNear(
            listOf(
                0.0f, 0.0122f, 0.0678f, 0.0752f, 0.0682f, 0.108f, 0.1616f, 0.2172f, 0.2433f, 0.2295f,
                0.1942f, 0.1407f, 0.063f, 0.0091f, 0.0043f, 0.0032f, 0.0081f, 0.0276f, 0.0232f, 0.0068f,
                0.003f, 0.0023f, 0.0023f, 0.0025f, 0.0017f, 0.0013f, 0.0011f, 0.0012f, 0.0012f, 0.0174f,
                0.0299f, 0.0347f, 0.039f, 0.1051f, 0.1617f, 0.1791f, 0.1624f, 0.1143f, 0.0841f, 0.0556f,
                0.0313f, 0.0068f, 0.0054f, 0.0033f, 0.0019f, 0.0019f, 0.0231f, 0.0218f, 0.0093f, 0.0043f,
                0.002f, 0.0013f, 0.0009f, 0.0008f, 0.0008f, 0.0008f, 0.0008f, 0.0008f, 0.001f, 0.0009f,
                0.0008f, 0.0008f, 0.0009f, 0.001f, 0.001f, 0.001f,
            ),
            "pop pop", listOf(1, 29),
        )
        closesNear(
            listOf(
                0.0003f, 0.0071f, 0.0279f, 0.0475f, 0.0533f, 0.0504f, 0.0592f, 0.1688f, 0.2109f, 0.1855f,
                0.1577f, 0.1483f, 0.1408f, 0.1366f, 0.1329f, 0.1173f, 0.1059f, 0.1038f, 0.1041f, 0.1046f,
                0.1032f, 0.1003f, 0.0978f, 0.0937f, 0.0895f, 0.0882f, 0.0839f, 0.077f, 0.0822f, 0.0894f,
                0.0917f, 0.0722f, 0.0487f, 0.0334f, 0.0182f, 0.043f, 0.0967f, 0.1185f, 0.1227f, 0.1223f,
                0.1173f, 0.1196f, 0.1109f, 0.1033f, 0.0958f, 0.0876f, 0.0837f, 0.0817f, 0.0794f, 0.0796f,
                0.0838f, 0.0866f, 0.0886f, 0.0876f, 0.0848f, 0.0866f, 0.0904f, 0.0922f, 0.0915f, 0.0786f,
                0.0598f, 0.0433f, 0.0186f, 0.0199f, 0.0618f, 0.1099f, 0.1332f, 0.1377f, 0.1268f, 0.1148f,
                0.1037f, 0.0801f, 0.0543f, 0.0248f, 0.0033f, 0.0018f, 0.0015f, 0.0008f, 0.0096f, 0.0157f,
                0.0102f, 0.0077f, 0.0052f, 0.0044f, 0.0032f, 0.0019f, 0.0013f, 0.0009f, 0.0005f, 0.0008f,
                0.0006f, 0.0002f, 0.0006f, 0.0008f, 0.0008f, 0.0004f, 0.0001f, 0.0002f, 0.0006f,
            ),
            "bye bye duck", listOf(1, 35),
        )
        closesNear(
            listOf(
                0.0007f, 0.0056f, 0.027f, 0.0438f, 0.0424f, 0.0318f, 0.1054f, 0.206f, 0.2162f, 0.1718f,
                0.1426f, 0.1352f, 0.1317f, 0.1348f, 0.1308f, 0.1197f, 0.1205f, 0.1148f, 0.1165f, 0.1037f,
                0.1026f, 0.0933f, 0.0908f, 0.0833f, 0.071f, 0.061f, 0.0574f, 0.0538f, 0.0468f, 0.0339f,
                0.0184f, 0.0072f, 0.003f, 0.0015f, 0.0008f, 0.0005f, 0.0006f, 0.0006f, 0.0008f, 0.0008f,
                0.0211f, 0.1405f, 0.2046f, 0.1923f, 0.177f, 0.1595f, 0.143f, 0.1341f, 0.1238f, 0.1204f,
                0.1266f, 0.1259f, 0.1211f, 0.1097f, 0.1032f, 0.1018f, 0.0979f, 0.093f, 0.0852f, 0.0773f,
                0.0763f, 0.0733f, 0.0655f, 0.0603f, 0.0557f, 0.0458f, 0.029f, 0.0134f, 0.003f, 0.0016f,
                0.0006f, 0.0005f, 0.0006f, 0.0005f, 0.0006f, 0.0006f, 0.018f, 0.086f, 0.1062f, 0.0637f,
                0.0229f, 0.0052f, 0.0441f, 0.1407f, 0.1781f, 0.1678f, 0.1421f, 0.1152f, 0.0927f, 0.0801f,
                0.0698f, 0.0658f, 0.0595f, 0.0603f, 0.0587f, 0.0546f, 0.0465f, 0.0406f, 0.0338f, 0.0298f,
                0.022f, 0.0088f, 0.0045f, 0.0014f, 0.0008f, 0.0004f, 0.0003f, 0.0002f, 0.0003f, 0.0002f,
                0.0002f, 0.0003f, 0.0002f, 0.0001f, 0.0f, 0.0f,
            ),
            "bye bye bee", listOf(1, 40, 76),
        )
        closesNear(
            listOf(
                0.0001f, 0.011f, 0.0927f, 0.1295f, 0.127f, 0.1227f, 0.124f, 0.1132f, 0.0803f, 0.0608f,
                0.0481f, 0.0374f, 0.023f, 0.0094f, 0.0038f, 0.0021f, 0.0012f, 0.0019f, 0.0558f, 0.1277f,
                0.16f, 0.1586f, 0.1566f, 0.1422f, 0.1345f, 0.1329f, 0.1276f, 0.124f, 0.1246f, 0.1248f,
                0.1241f, 0.1249f, 0.1254f, 0.1231f, 0.1127f, 0.0841f, 0.0505f, 0.0324f, 0.0173f, 0.0256f,
                0.0389f, 0.0282f, 0.0085f, 0.003f, 0.0011f, 0.001f, 0.0008f, 0.0005f, 0.0005f, 0.0139f,
                0.0996f, 0.1771f, 0.1802f, 0.1588f, 0.142f, 0.1357f, 0.1322f, 0.1272f, 0.1214f, 0.113f,
                0.1053f, 0.1007f, 0.0918f, 0.0889f, 0.0773f, 0.0791f, 0.0681f, 0.0652f, 0.0607f, 0.0548f,
                0.0515f, 0.0451f, 0.0412f, 0.0376f, 0.0312f, 0.024f, 0.0142f, 0.0053f, 0.0016f, 0.001f,
                0.0003f, 0.0003f, 0.0004f, 0.0002f, 0.0002f, 0.0007f, 0.0008f, 0.0004f, 0.0004f, 0.0008f,
                0.0007f, 0.0003f, 0.0003f, 0.0003f,
            ),
            "a big ball", listOf(18, 49),
        )

        // "mmm" stays closed the whole time it's sounding, even as its loudness drifts.
        val lip = LipSync()
        val mmm = listOf(
            0.0f, 0.0001f, 0.0143f, 0.0474f, 0.0835f, 0.11f, 0.1183f, 0.1142f, 0.1094f, 0.1039f, 0.0995f,
            0.0966f, 0.0946f, 0.0932f, 0.0911f, 0.0892f, 0.0881f, 0.0937f, 0.099f, 0.1042f, 0.1056f,
            0.1115f, 0.1115f, 0.1094f, 0.1042f, 0.1004f, 0.0981f, 0.0933f, 0.0927f, 0.0868f, 0.0828f,
            0.0788f, 0.0758f, 0.0761f, 0.0747f, 0.0719f, 0.0711f, 0.0696f, 0.0652f, 0.0618f, 0.0592f,
            0.0556f, 0.0514f, 0.0474f, 0.0399f, 0.029f, 0.0132f, 0.0038f, 0.0006f, 0.0005f, 0.0004f,
            0.0004f, 0.0004f, 0.0006f, 0.0005f, 0.0002f, 0.0003f, 0.0003f, 0.0001f, 0.0f, 0.0001f,
            0.0001f, 0.0f, 0.0f,
        )
        mmm.forEach { lvl ->
            lip.step(lvl, "mmm", 16)
            if (lvl >= LipSync.OFF) assertTrue(lip.closed, "mmm open while sounding at $lvl")
        }
    }
}

/** Rory's Kitchen: every target word is p/b/m, or one of the given exceptions (flip, the counting
 * turn's number word), and its tap/talk flow is set up the way [KitchenBeatState] expects. */
class KitchenStoryTest {
    @Test
    fun everyKitchenWordIsPBMOrAnAllowedException() {
        // K6: "three" (the berry count), "cut" (the butter swipe) and "one"/"two" (the butter
        // counts) aren't p/b/m words -- they're never scored as speech targets (every new K6 beat
        // has `sound = null`), just said back and counted.
        val allowed = setOf("flip", "three", "cut", "one", "two")
        for (b in VoiceStories.kitchen.beats) {
            val first = b.word.first().lowercaseChar()
            assertTrue(first in "pbm" || b.word in allowed, "unexpected kitchen target word '${b.word}'")
        }
    }

    @Test
    fun theSpecialBeatsCarryTheRightFlowAndGoal() {
        val byWord = VoiceStories.kitchen.beats.associateBy { it.word to it.kitchen }
        fun beat(word: String, flow: KitchenFlow) = byWord.getValue(word to flow)
        assertEquals(listOf("pan", "spoon"), beat("bowl", KitchenFlow.TAP_CHOICE).kitchenChoices)
        assertEquals(3, beat("mix", KitchenFlow.STIR).kitchenGoal)
        assertEquals(3, beat("three", KitchenFlow.COUNT).kitchenGoal)
        assertEquals(3, beat("pop", KitchenFlow.BUBBLES).kitchenGoal)
        assertEquals(KitchenFlow.FLIP, beat("flip", KitchenFlow.FLIP).kitchen)
        // The "how many?" asks (K6): he answers without Rory saying the number first, and a
        // persistent silence still has a fixed, spoken fallback that counts.
        assertEquals("One, two... three!", beat("three", KitchenFlow.ASK).kitchenFallback)
        assertEquals("One!", beat("one", KitchenFlow.ASK).kitchenFallback)
        assertEquals("One... two!", beat("two", KitchenFlow.ASK).kitchenFallback)
        // The butter swipe (K6): narrated only, never a wrong tap, never a talk turn.
        val cut = beat("cut", KitchenFlow.CUT)
        assertEquals(0, cut.kitchen!!.talkGoal(cut.kitchenGoal))
        assertFalse(cut.kitchen!!.hasWrongTaps())
        // The other three beats (milk, pour, mmm) are the usual single talk turn: no kitchen flow.
        assertEquals(3, VoiceStories.kitchen.beats.count { it.kitchen == null })
    }

    @Test
    fun theBowlRoundAlwaysHasTheBowlAndIsNotAlwaysInTheSameSpot() {
        val seenIndex = mutableSetOf<Int>()
        repeat(60) { i ->
            val round = KitchenGame.tapRound("bowl", 2 + i % 2, Random(i))
            assertTrue(round.options.size in 2..3)
            assertEquals(round.options.size, round.options.distinct().size)
            assertTrue("bowl" in round.options)
            assertEquals("bowl", round.answer)
            seenIndex += round.options.indexOf("bowl")
        }
        assertTrue(seenIndex.size > 1, "the bowl landed at the same index every time: $seenIndex")
    }

    /** K2l: a toddler-clear on-screen cue for every step, at most 4 words so it's a quick glance,
     * not a reading task (he can't read yet anyway). */
    @Test
    fun everyKitchenBeatHasAShortCaption() {
        for (b in VoiceStories.kitchen.beats) {
            val caption = b.caption
            assertTrue(!caption.isNullOrBlank(), "kitchen beat '${b.word}' has no caption")
            val words = caption.trim().split(Regex("\\s+"))
            assertTrue(words.size <= 4, "kitchen beat '${b.word}' caption too long: '$caption'")
        }
    }

    /** K2l: a coffee cup doesn't belong in a toddler pancake kitchen (Ruby's screenshot catch). */
    @Test
    fun theBowlDistractorsNeverIncludeACup() {
        assertTrue("cup" !in KitchenGame.distractors)
        assertTrue("cup" !in VoiceStories.kitchen.beats.first { it.word == "bowl" }.kitchenChoices)
    }
}

/** The kitchen flows' pure state machine (see [KitchenBeatState]): nothing ever fails, and every
 * path reaches [KitchenPhase.DONE]. */
class KitchenBeatStateTest {
    @Test
    fun tapChoiceRightFirstTimeSkipsStraightToClose() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true)
        assertEquals(KitchenPhase.CLOSE, s.phase)
        assertTrue(s.rightFirstTry)
    }

    @Test
    fun tapChoiceOneWrongTapWigglesThenGlowsAndCanStillSucceed() {
        var s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named()
        assertEquals(KitchenPhase.TAP, s.phase)
        s = s.tap(false)
        assertEquals(KitchenPhase.WIGGLE, s.phase)
        assertTrue(s.glow)
        s = s.wiggled()
        assertEquals(KitchenPhase.TAP, s.phase)
        s = s.tap(true)
        assertEquals(KitchenPhase.CLOSE, s.phase)
        assertFalse(s.rightFirstTry)
    }

    @Test
    fun tapChoiceTwoWrongTapsShowTheAnswerThenClose() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(false)
        assertEquals(KitchenPhase.SHOW_ANSWER, s.phase)
        assertEquals(KitchenPhase.CLOSE, s.shown().phase)
    }

    @Test
    fun tapChoiceGoesStraightToCloseWithNoTalkTurnAtAll() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true)
        assertEquals(KitchenPhase.CLOSE, s.phase)
        assertEquals(KitchenPhase.DONE, s.closed().phase)
    }

    @Test
    fun flipIsOneTapWithNoWrongAnswerAndNoTalkTurn() {
        var s = KitchenBeatState(KitchenFlow.FLIP).named()
        assertEquals(KitchenPhase.TAP, s.phase)
        // A stray "wrong" tap is impossible for this flow: it's simply ignored, never fails.
        assertEquals(s, s.tap(false))
        s = s.tap(true)
        assertEquals(KitchenPhase.CLOSE, s.phase)
        assertEquals(KitchenPhase.DONE, s.closed().phase)
    }

    @Test
    fun stirNeedsThreeTalkTriesWithNoTapPhaseAtAll() {
        var s = KitchenBeatState(KitchenFlow.STIR, goal = 3).named()
        assertEquals(KitchenPhase.TALK, s.phase)
        repeat(2) {
            s = s.talked()
            assertEquals(KitchenPhase.TALK, s.phase, "stir ended early")
        }
        s = s.talked()
        assertEquals(KitchenPhase.CLOSE, s.phase)
    }

    @Test
    fun bubblesNeedsThreePopsWithNoTapPhaseAtAll() {
        var s = KitchenBeatState(KitchenFlow.BUBBLES, goal = 3).named()
        assertEquals(KitchenPhase.TALK, s.phase)
        repeat(3) { s = s.talked() }
        assertEquals(KitchenPhase.CLOSE, s.phase)
    }

    @Test
    fun countNeedsThreeTapsAndNoTalkTurnAtAll() {
        var s = KitchenBeatState(KitchenFlow.COUNT, goal = 3).named()
        assertEquals(KitchenPhase.TAP, s.phase)
        repeat(2) {
            s = s.tap(true)
            assertEquals(KitchenPhase.TAP, s.phase, "counting ended early")
        }
        s = s.tap(true)
        // Straight to CLOSE: a counting turn is never a talk turn.
        assertEquals(KitchenPhase.CLOSE, s.phase)
        // A stray "wrong" tap can never happen for a counting beat: ignored, never fails.
        val counted = KitchenBeatState(KitchenFlow.COUNT, goal = 3).named().tap(true)
        assertEquals(counted, counted.tap(false))
    }

    @Test
    fun outOfOrderCallsAreIgnored() {
        val talking = KitchenBeatState(KitchenFlow.STIR, goal = 3).named()
        assertEquals(KitchenPhase.TALK, talking.phase)
        assertEquals(talking, talking.wiggled())
        assertEquals(talking, talking.shown())
        assertEquals(talking, talking.closed())
        val start = KitchenBeatState(KitchenFlow.STIR, goal = 3)
        assertEquals(start, start.talked())
        assertEquals(start, start.tap(true))
    }

    /** K3a (Timothy's iPad notes): a tap step that also slid into a talk/listening turn afterward
     * read, with no audio to explain it, as "the tiles vanished and a strange listening screen
     * appeared". Every tap flow must now go straight from its last tap to CLOSE, never through
     * TALK -- the only phase that ever shows the listening look (see VoiceStoryScreen's `listen`). */
    @Test
    fun tapFlowsNeverEnterTalkPhase() {
        fun drive(flow: KitchenFlow, taps: List<Boolean>): KitchenBeatState {
            var s = KitchenBeatState(flow, goal = taps.count { it }.coerceAtLeast(1)).named()
            for (right in taps) {
                s = s.tap(right)
                if (s.phase == KitchenPhase.WIGGLE) s = s.wiggled()
                if (s.phase == KitchenPhase.SHOW_ANSWER) s = s.shown()
                assertTrue(s.phase != KitchenPhase.TALK, "$flow $taps: reached TALK")
            }
            return s
        }
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.TAP_CHOICE, listOf(true)).phase)
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.TAP_CHOICE, listOf(false, true)).phase)
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.TAP_CHOICE, listOf(false, false)).phase)
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.FLIP, listOf(true)).phase)
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.COUNT, listOf(true, true, true)).phase)
        assertEquals(KitchenPhase.CLOSE, drive(KitchenFlow.CUT, listOf(true)).phase)
    }

    /** K6: the butter swipe is narrated only -- it must never reach TAP's own wrong-answer path
     * ([KitchenFlow.hasWrongTaps] false) or a talk turn either side of it. */
    @Test
    fun cutIsAPlainTapWithNoWrongAnswerAndNoTalkTurn() {
        assertFalse(KitchenFlow.CUT.hasWrongTaps())
        assertEquals(1, KitchenFlow.CUT.tapGoal(1))
        assertEquals(0, KitchenFlow.CUT.talkGoal(1))
        val s = KitchenBeatState(KitchenFlow.CUT).named().tap(true)
        assertEquals(KitchenPhase.CLOSE, s.phase)
    }

    /** K6: [KitchenFlow.ASK] ("how many?") never taps and always listens, straight from NAME. */
    @Test
    fun askIsTalkOnlyAndNeverTaps() {
        assertEquals(0, KitchenFlow.ASK.tapGoal(1))
        assertEquals(1, KitchenFlow.ASK.talkGoal(1))
        val s = KitchenBeatState(KitchenFlow.ASK).named()
        assertEquals(KitchenPhase.TALK, s.phase)
        assertEquals(KitchenPhase.CLOSE, s.talked().phase)
    }

    /** K6: only the bowl choice and the pancake flip ever reserve the bottom choice strip --
     * counting (now tapped right on the counter) and the butter swipe (a drag, not a tile) never
     * do. */
    @Test
    fun onlyTapChoiceAndFlipUseTheChoiceStrip() {
        assertTrue(KitchenFlow.TAP_CHOICE.usesStrip())
        assertTrue(KitchenFlow.FLIP.usesStrip())
        for (flow in listOf(KitchenFlow.COUNT, KitchenFlow.STIR, KitchenFlow.BUBBLES, KitchenFlow.CUT, KitchenFlow.ASK)) {
            assertFalse(flow.usesStrip(), "$flow shouldn't use the choice strip")
        }
    }

    /** The flip side of [tapFlowsNeverEnterTalkPhase]: every flow is tap-only (goes NAME -> TAP,
     * never TALK) or talk-only (goes NAME -> TALK, never TAP), never both. */
    @Test
    fun everyFlowIsEitherTapOnlyOrTalkOnlyNeverBoth() {
        for (flow in KitchenFlow.entries) {
            val hasTap = flow.tapGoal(3) > 0
            val hasTalk = flow.talkGoal(3) > 0
            assertTrue(hasTap != hasTalk, "$flow must be either tap-only or talk-only, not both or neither")
        }
    }

    /** Aida (speech therapist): "mix!" and "pop!" must model once per try, then wait quietly for
     * the child, never nag. The state machine itself must never drop back to NAME or TAP mid-flow
     * (VoiceStoryScreen's startKitchenTalk explicitly starts each new try; see the TALK case in
     * its `LaunchedEffect(kitchenPhase.phase)`): a return to NAME is what would replay the model
     * line from scratch instead of just listening again. */
    @Test
    fun stirAndBubblesStayInTalkAcrossEveryTryNeverReNamingOrRetapping() {
        for (flow in listOf(KitchenFlow.STIR, KitchenFlow.BUBBLES)) {
            var s = KitchenBeatState(flow, goal = 3).named()
            val phases = mutableListOf(s.phase)
            repeat(3) { s = s.talked(); phases += s.phase }
            assertTrue(phases.dropLast(1).all { it == KitchenPhase.TALK }, "$flow: $phases")
            assertEquals(KitchenPhase.CLOSE, phases.last())
        }
    }

    @Test
    fun everyFlowAndEveryPathReachesDone() {
        val paths = listOf(
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true),
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(true),
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(false).shown(),
            KitchenBeatState(KitchenFlow.FLIP).named().tap(true),
            KitchenBeatState(KitchenFlow.STIR, goal = 3).named().talked().talked().talked(),
            KitchenBeatState(KitchenFlow.BUBBLES, goal = 3).named().talked().talked().talked(),
            KitchenBeatState(KitchenFlow.COUNT, goal = 3).named().tap(true).tap(true).tap(true),
            KitchenBeatState(KitchenFlow.CUT).named().tap(true),
            KitchenBeatState(KitchenFlow.ASK).named().talked(),
        )
        for (p in paths) {
            assertEquals(KitchenPhase.CLOSE, p.phase)
            assertEquals(KitchenPhase.DONE, p.closed().phase)
        }
    }
}

/** The butter-cutting step's own swipe (K6, [KitchenFlow.CUT]): forgiving, any direction, scaled
 * to the device's own density. */
class KitchenCutTest {
    @Test
    fun aDragShorterThanTheThresholdDoesNotCount() {
        assertFalse(KitchenCut.crossed(dragDistancePx = 40f, densityScale = 1f))
    }

    @Test
    fun aDragAtOrPastTheThresholdCounts() {
        assertTrue(KitchenCut.crossed(dragDistancePx = 60f, densityScale = 1f))
        assertTrue(KitchenCut.crossed(dragDistancePx = 90f, densityScale = 1f))
    }

    @Test
    fun theThresholdScalesWithDensity() {
        // A 2x-density screen needs twice the px for the same physical 60dp swipe.
        assertFalse(KitchenCut.crossed(dragDistancePx = 90f, densityScale = 2f))
        assertTrue(KitchenCut.crossed(dragDistancePx = 130f, densityScale = 2f))
    }

    @Test
    fun anyDirectionCountsTheSameWay() {
        // The threshold is a plain distance, not a signed one: a drag left crosses it exactly
        // like a drag right, up or down.
        assertEquals(
            KitchenCut.crossed(60f, 1f),
            KitchenCut.crossed(kotlin.math.abs(-60f), 1f),
        )
    }
}

/** Rory's Kitchen's counter (K2a; K4 moved the counter top down a long way, see
 * [KitchenLayout.COUNTER_TOP_LANDSCAPE]'s own doc): keeps at least
 * [KitchenLayout.MIN_VISIBLE_FRACTION] of the scene as counter, and lands its REAL drawn position
 * (after VoiceStory's landscape lift, [KitchenLayout.onStage]) at roughly 60-65% down the visible
 * scene in both orientations -- not the same local fraction (landscape's clips away a share of
 * the scene's own top before anything is visible at all, so matching portrait's real position
 * needs a bigger local number there, the opposite of K2a's old "shorter wall" rule). */
class KitchenLayoutTest {
    @Test
    fun portraitCounterTopLandsInTheTargetVisibleBand() {
        // Portrait never lifts/clips (see onStage), so its local fraction IS its visible one.
        // K9: raised again (0.55 to 0.75, Timothy's own "counter front 22-25% of the screen" ask,
        // a real portrait screenshot having measured it at ~45%): most of the scene is wall/window
        // above the line now, with far more headroom for a bigger Rory and Bear.
        val ct = KitchenLayout.counterTop(landscape = false)
        assertTrue(ct in 0.73f..0.78f, "portrait counter top not in the target band: $ct")
        val counterFront = 1f - ct
        assertTrue(counterFront in 0.22f..0.25f, "portrait counter front not in Timothy's 22-25% ask: $counterFront")
    }

    @Test
    fun landscapeCounterMeetsRubysFloor() {
        val counterFraction = 1f - KitchenLayout.counterTop(landscape = true)
        assertTrue(counterFraction >= KitchenLayout.MIN_VISIBLE_FRACTION)
    }

    /** Timothy's own report (K4): an iPad landscape at 1024x768. [KitchenLayout.onStage] mirrors
     * VoiceStory's real lift/clip math, so dividing by its own value at the visible scene's very
     * bottom (`local = 1f`) turns its stage-fraction result into a visible-scene fraction: 0 at
     * the top of what's actually on screen, 1 at the bottom. */
    @Test
    fun landscapeCounterTopLandsInTheSameVisibleBandAsPortrait() {
        val stripFraction = 0.18f
        val stripMinHeight = 128f
        for (h in listOf(600f, 768f, 1024f)) {
            val ct = KitchenLayout.counterTop(landscape = true)
            val onStage = KitchenLayout.onStage(ct, true, h, stripFraction, stripMinHeight)
            val visibleBottom = KitchenLayout.onStage(1f, true, h, stripFraction, stripMinHeight)
            val visibleFraction = onStage / visibleBottom
            assertTrue(visibleFraction in 0.55f..0.70f, "landscape counter top not in the target visible band at height $h: $visibleFraction")
        }
    }

    /** K4: Timothy's screenshots showed Rory standing behind the counter with only his head and
     * hat visible once the tap choice strip is shown -- the real on-screen slice of him, after the
     * landscape lift/clip ([KitchenLayout.onStage]), must be at least 45% of his own full height
     * at a real device size (an iPad landscape, 1024x768). */
    @Test
    fun roryTapPhaseVisiblePartIsAtLeast45PercentOfHisHeightLandscape1024x768() {
        val stripFraction = 0.18f
        val stripMinHeight = 128f
        val h = 768f
        fun stage(local: Float) = KitchenLayout.onStage(local, true, h, stripFraction, stripMinHeight)
        val box = KitchenLayout.roryBox(true)
        val ct = KitchenLayout.counterTop(true)
        val fullTop = stage(box.t)
        val fullBottom = stage(box.b)
        val visibleTop = maxOf(fullTop, 0f) // the lift clips away anything above the visible scene's own top
        val visibleBottom = stage(ct)
        val fullHeight = fullBottom - fullTop
        val visibleFraction = (visibleBottom - visibleTop).coerceAtLeast(0f) / fullHeight
        assertTrue(visibleFraction >= 0.45f, "Rory's visible part is less than 45% of his height: $visibleFraction")
    }

    /** K4b: a real portrait screenshot (768x1024) showed Rory standing in front of the counter
     * with his whole body, feet included, visible above it, instead of behind it -- his lane is
     * much narrower than [KitchenLayout.roryBox]'s own nominal height needs at that aspect ratio,
     * so trusting the nominal box alone (as `Modifier.fillMaxHeight()` plus Critter's own
     * aspectRatio used to) measures him shorter and top-anchored, leaving his real feet short of
     * the counter top line. [KitchenLayout.roryFittedBox] (what `KitchenRory` in VoiceScenes.kt
     * actually draws) must always put his feet at or below the counter top line instead, so his
     * lower body is always at least started behind it, at the sizes this bug was seen (portrait
     * tablet) and its landscape counterpart. */
    @Test
    fun roryFittedFeetNeverFloatAboveTheCounterTop() {
        val sizes = listOf(768f to 1024f, 1024f to 768f, 390f to 844f, 844f to 390f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val fitted = KitchenLayout.roryFittedBox(w, h, landscape)
            val ct = h * KitchenLayout.counterTop(landscape)
            assertTrue(
                fitted.b >= ct - 0.0001f,
                "Rory's feet float above the counter top at ${w}x$h: feet=${fitted.b} counterTop=$ct",
            )
        }
    }

    /**
     * K8: real screenshots (1024x768 landscape, 768x1024 portrait) measured the drawn Bear at only
     * about a third of his own lane's box, pressed against the screen's own left edge, with a
     * bowl that stayed small next to Rory's now-bigger size -- a box-only check (like
     * [bearsSeatIsWithinTenPercentOfTheCounterTop] below) could never catch any of this, since it
     * never looks at the FITTED size `KitchenBear`/`KitchenRory` actually draw (see
     * [KitchenLayout.bearFittedBox]/[KitchenLayout.roryFittedBox]). Checks the real drawn boxes
     * directly, at the two sizes Timothy reported: Bear reads about half Rory's own real visible
     * height (never a sliver of it, nor as tall as him), the bowl is a real fraction of Rory's own
     * drawn width (not swamped next to him), and Bear never sits closer than 16px to the screen's
     * own left edge.
     */
    @Test
    fun bearAndTheBowlStayProportionateToRorysRealDrawnSize() {
        for ((w, h) in listOf(1024f to 768f, 768f to 1024f)) {
            val landscape = w > h
            val rory = KitchenLayout.roryFittedBox(w, h, landscape)
            val bear = KitchenLayout.bearFittedBox(w, h, landscape)
            val roryVisibleHeight = h * KitchenLayout.counterTop(landscape) - rory.t
            val bearHeight = bear.b - bear.t
            val ratio = bearHeight / roryVisibleHeight
            assertTrue(
                ratio in 0.4f..0.65f,
                "Bear isn't about half Rory's real drawn height at ${w}x$h: bear=$bearHeight rory=$roryVisibleHeight ratio=$ratio",
            )
            assertTrue(bear.l >= 16f, "Bear sits too close to the screen's own left edge at ${w}x$h: ${bear.l}")
            // Mirrors VoiceScenes.kt's own kitchenPropSize for the bowl (landscape 0.2533, portrait
            // 0.17, see that file's own K8 doc): `logic` stays platform-free, so this can't import
            // it directly (same reason kitchenPropsSitOnTheRealDrawnCounterTopInLandscape can't,
            // above).
            val bowlSizeFraction = if (landscape) 0.2533f else 0.215f
            val unit = minOf(w, h)
            val bowlWidth = unit * bowlSizeFraction
            val roryWidth = (rory.r - rory.l) * KitchenLayout.roryBodyWidthFraction(landscape)
            assertTrue(
                bowlWidth >= roryWidth * 0.4f,
                "Bowl isn't at least 0.4x Rory's real drawn width at ${w}x$h: bowl=$bowlWidth rory=$roryWidth",
            )
        }
    }

    /** K4: Timothy's screenshots showed Bear sitting on a low stool far below the counter -- his
     * seat ([KitchenLayout.bearBox]'s own bottom) must now land close to the counter top, within
     * a tenth of the scene's own height, in both orientations. */
    @Test
    fun bearsSeatIsWithinTenPercentOfTheCounterTop() {
        for (landscape in listOf(false, true)) {
            val ct = KitchenLayout.counterTop(landscape)
            val seat = KitchenLayout.bearBox(landscape).b
            assertTrue(
                kotlin.math.abs(seat - ct) <= 0.10f,
                "Bear's seat too far from the counter top in ${if (landscape) "landscape" else "portrait"}: seat=$seat ct=$ct",
            )
        }
    }

    /** K4b: real screenshots (1024x768 landscape, 768x1024 portrait, both the tap and the talk
     * steps) showed a hairline gap between Bear's own bottom ([KitchenLayout.bearBox]'s own `b`)
     * and the stool's drawn seat, reading as him floating just above it. [KitchenLayout.stoolTop]
     * is defined a few px above that bottom on purpose, never as a separately-guessed number, so
     * the two can never drift apart again: his seat always overlaps the stool's own top by a
     * small amount (never a gap), and that overlap stays well under the 2% of the scene's own
     * height Ruby allowed, in both orientations (the step -- tap or talk -- never changes either
     * number: both are pure functions of `landscape` alone). */
    @Test
    fun bearsSeatOverlapsTheStoolSeat() {
        for (landscape in listOf(false, true)) {
            val bearBottom = KitchenLayout.bearBox(landscape).b
            val stoolTop = KitchenLayout.stoolTop(landscape)
            val diff = bearBottom - stoolTop
            assertTrue(diff > 0f, "Bear's seat doesn't overlap the stool at all in ${if (landscape) "landscape" else "portrait"}: diff=$diff")
            assertTrue(
                kotlin.math.abs(diff) <= 0.02f,
                "Bear's seat and the stool's are more than 2% of the scene height apart in ${if (landscape) "landscape" else "portrait"}: diff=$diff",
            )
        }
    }

    /** Ruby's K2b: Bear (on his stool), Rory (behind the counter, chef's hat) and the counter's
     * own props (the bowl choice, the pan) never share any area, in either aspect ratio. K2f:
     * Rory's box now reaches down past the counter top line on purpose (the counter hides his
     * lower body, see [KitchenLayout.roryVisibleBox]), so only his visible part above the line is
     * checked here; it may still sit horizontally over the bowl/pan, since those are below the
     * line where he can't be seen at all. */
    @Test
    fun bearRoryBowlAndPanNeverOverlapEachOther() {
        for (landscape in listOf(false, true)) {
            val boxes = listOf(
                "bear" to KitchenLayout.bearBox(landscape),
                "rory" to KitchenLayout.roryVisibleBox(landscape),
                "bowl" to KitchenLayout.bowlBox(landscape),
                "pan" to KitchenLayout.panBox(landscape),
            )
            for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                val (nameA, a) = boxes[i]
                val (nameB, b) = boxes[j]
                assertFalse(a.overlaps(b), "$nameA overlaps $nameB in ${if (landscape) "landscape" else "portrait"}")
            }
        }
    }

    /**
     * K7: Rory's Kitchen now frames itself by the real screen orientation on every step, not only
     * while a tap step's own tiles are up (see VoiceStoryScreen's own `isKitchen`), so the old K6
     * "wide" flag (a real wide stage, independent of whether the choice strip was reserved) is
     * gone -- [bearBox]/[roryVisibleBox] take `landscape` alone now. Landscape's own Rory and Bear
     * must still read bigger than portrait's (Timothy's K7 ask), without overlapping each other,
     * the bowl or the pan in either orientation.
     */
    @Test
    fun landscapeBearAndRoryAreBiggerThanPortraitWithoutOverlappingAnything() {
        for (landscape in listOf(false, true)) {
            val bear = KitchenLayout.bearBox(landscape)
            val rory = KitchenLayout.roryVisibleBox(landscape)
            val bowl = KitchenLayout.bowlBox(landscape)
            val pan = KitchenLayout.panBox(landscape)
            val boxes = listOf("bear" to bear, "rory" to rory, "bowl" to bowl, "pan" to pan)
            for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                val (nameA, a) = boxes[i]
                val (nameB, b) = boxes[j]
                assertFalse(a.overlaps(b), "$nameA overlaps $nameB in ${if (landscape) "landscape" else "portrait"}")
            }
        }
        val portraitBear = KitchenLayout.bearBox(false)
        val landscapeBear = KitchenLayout.bearBox(true)
        val portraitRory = KitchenLayout.roryVisibleBox(false)
        val landscapeRory = KitchenLayout.roryVisibleBox(true)
        assertTrue(
            (landscapeBear.b - landscapeBear.t) > (portraitBear.b - portraitBear.t),
            "Bear isn't taller in landscape than portrait",
        )
        // K7e: Timothy asked for portrait Rory ~1.5x taller (his lane now holds his drawn body,
        // see KitchenLayout.roryBodyWidthFraction), so the old "landscape slice is taller" rule no
        // longer applies; both orientations just keep a real visible slice.
        assertTrue((portraitRory.b - portraitRory.t) > 0f && (landscapeRory.b - landscapeRory.t) > 0f)
    }

    /** K2f: his visible part (above the counter top) is a strict top slice of his full box, never
     * reaching past the line itself, in both orientations. K7: Rory is no longer the same full
     * height in both (that was K2f's own, now-superseded rule) -- landscape reads bigger than
     * portrait (Timothy's K7 ask; see [landscapeBearAndRoryAreBiggerThanPortraitWithoutOverlappingAnything]
     * for the actual size check). */
    @Test
    fun roryHasAVisibleSliceAboveTheCounterInBothOrientations() {
        for (landscape in listOf(false, true)) {
            val box = KitchenLayout.roryBox(landscape)
            val visible = KitchenLayout.roryVisibleBox(landscape)
            assertEquals(box.l, visible.l)
            assertEquals(box.r, visible.r)
            assertEquals(box.t, visible.t)
            assertEquals(KitchenLayout.counterTop(landscape), visible.b)
            assertTrue(visible.b < box.b, "rory's visible box should stop at the counter top, short of his full box's bottom")
        }
    }

    /** None of the four ever reach down into the choice strip's own reserved band, at every size
     * Ruby asked to check: tablet portrait/landscape and phone portrait/landscape. */
    @Test
    fun bearRoryBowlAndPanNeverReachTheChoiceStrip() {
        val sizes = listOf(768f to 1024f, 1024f to 768f, 390f to 844f, 844f to 390f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val stripFraction = if (landscape) 0.18f else 0.22f
            val sceneFraction = 1f - stripFraction
            val strip = KitchenLayout.stripBox(stripFraction)
            val boxes = listOf(
                "bear" to KitchenLayout.bearBox(landscape),
                "rory" to KitchenLayout.roryBox(landscape),
                "bowl" to KitchenLayout.bowlBox(landscape),
                "pan" to KitchenLayout.panBox(landscape),
            )
            for ((name, box) in boxes) {
                val onStage = KitchenLayout.stageBox(box, sceneFraction)
                assertFalse(onStage.overlaps(strip), "$name reaches the choice strip at ${w}x$h")
            }
        }
    }

    /** K3d: Rory's Kitchen's landscape strip doesn't shrink the scene the way
     * [bearRoryBowlAndPanNeverReachTheChoiceStrip]'s own [KitchenLayout.stageBox] models every
     * other strip (the balloon puzzle included) -- VoiceStory.kt keeps the scene at full size and
     * lifts it clear of the strip instead ([KitchenLayout.onStage]). A screenshot showed what that
     * mismatch looks like on screen: the counter drawn (correctly) at its lifted position while a
     * prop was placed as if the scene had shrunk, leaving it floating above the counter, near the
     * wall. Checked at the sizes Timothy reported it on (a landscape tablet and phone) plus a
     * bigger tablet, so a future change to either the lift or the shrink math can't quietly drift
     * the two apart again: every kitchen prop's own drawn bottom ([KitchenLayout.propBox]) must
     * land within [KitchenLayout.PROPS_MARGIN] of the counter's own drawn top, on the SAME stage,
     * never above it (floating) and never far below it (sunk off the counter). */
    @Test
    fun kitchenPropsSitOnTheRealDrawnCounterTopInLandscape() {
        val stripFraction = 0.18f
        val stripMinHeight = 128f
        val sizes = listOf(1024f to 768f, 1366f to 1024f, 844f to 390f)
        val props = listOf(Prop.BOWL, Prop.MILK, Prop.BERRIES, Prop.PAN, Prop.PANCAKE)
        for ((w, h) in sizes) {
            assertTrue(w > h, "these are the landscape, strip-shown sizes: ${w}x$h")
            val drawnCounterTop = KitchenLayout.onStage(KitchenLayout.counterTop(true), true, h, stripFraction, stripMinHeight)
            for (prop in props) {
                // The size fraction itself doesn't matter here (propBox's own bottom, [propNaturalBox],
                // is independent of it): a representative size stands in for every prop's real one
                // (`kitchenPropSizeFraction` in VoiceScenes.kt), which this test can't reach -- `logic`
                // stays platform-free.
                val box = KitchenLayout.propBox(prop, true, 0.2f, w, h)
                val drawnBottom = KitchenLayout.onStage(box.b, true, h, stripFraction, stripMinHeight)
                assertTrue(
                    drawnBottom >= drawnCounterTop - 0.0001f,
                    "$prop floats above the drawn counter at ${w}x$h: bottom=$drawnBottom counterTop=$drawnCounterTop",
                )
                val margin = KitchenLayout.onStage(KitchenLayout.counterTop(true) + KitchenLayout.PROPS_MARGIN, true, h, stripFraction, stripMinHeight)
                assertTrue(
                    drawnBottom <= margin + 0.0001f,
                    "$prop sits too far below the drawn counter at ${w}x$h: bottom=$drawnBottom expected<=$margin",
                )
            }
        }
    }

    /**
     * K9: a real landscape screenshot (1024x768) showed a ~120px band of empty cream between the
     * counter's own (real, drawn) bottom and the choice strip below it -- VoiceStoryScreen's own
     * landscape lift used a plain `Modifier.height(h)` on the scene box, which Compose silently
     * clamps down to its parent's own smaller incoming max-height instead of forcing the full `h`
     * the lift's own maths ([KitchenLayout.onStage]) assumes; fixed there with `requiredHeight`.
     * [onStage] itself already modelled the gap-free version correctly (that's what the fix now
     * actually draws): the counter's own real bottom (a local fraction of 1f; the counter's own
     * front, `KitchenCounterFront` in VoiceScenes.kt, paints full-height) must land, in on-stage
     * terms, at exactly the same position as the strip's own top -- `1 - strip / h`, the same
     * `strip` (coerced to its own minimum height) VoiceStory reserves for it -- at every stage
     * height this story runs at, with no gap and no overlap.
     */
    @Test
    // K7e: this checks the model (KitchenLayout.onStage). The REAL drawn layout is checked in a
    // browser run: VoiceStory logs the scene node's own onGloballyPositioned bottom
    // ("KITCHEN layout counterBottom=") and the strip's real rect ("strip="); the loop's
    // check_gap.py fails if they differ by more than 1px (test26's 60px gap passed this unit test
    // but not that check).
    fun landscapeCounterBottomMeetsTheStripTopWithNoGap() {
        val stripFraction = 0.18f
        val stripMinHeight = 128f
        for (h in listOf(390f, 600f, 768f, 1024f, 1366f)) {
            val counterBottomOnStage = KitchenLayout.onStage(1f, true, h, stripFraction, stripMinHeight)
            val strip = (h * stripFraction).coerceAtLeast(stripMinHeight)
            val stripTopOnStage = 1f - strip / h
            assertEquals(stripTopOnStage, counterBottomOnStage, 0.0001f, "gap between the counter's own bottom and the strip's own top at height $h")
        }
    }

    /** Mirrors `KitchenBear`'s own geometry in VoiceScenes.kt: Bear's actual on-screen box (not
     * his lane), fitted to a slice of the lane's width the same way (his drawn puppet never
     * floats above the stool, K2g) and pinned to the lane's own left edge, not centred (K2k: his
     * lane is fitted narrower than itself by [KitchenLayout.BEAR_SHIFT_FRACTION] and left-aligned,
     * so his bite lean gets a real gap before the counter that stays open however far he leans),
     * for a scene of [w] by [h]. K8: this is now just [KitchenLayout.bearFittedBox] itself (shared
     * with `KitchenBear`'s own real draw and the [bearAndTheBowlStayProportionateToRorysRealDrawnSize]
     * test below), not a second copy of the same formula. */
    private fun bearOnScreenBox(w: Float, h: Float, landscape: Boolean): KitchenLayout.Rect {
        return KitchenLayout.bearFittedBox(w, h, landscape)
    }

    /** K2j: K2i's whole-body bite lean (rotating and shifting the entire Critter about a
     * bottom-centre pivot) still tipped Bear's body past the counter's left end in real
     * screenshots, even though a unit test of that same rotation passed -- the test modelled a
     * different transform than the one actually drawn. K2j's lean only rotates/shifts his head
     * (his stool-seated lower body never moves at all), so the only thing left to check is that
     * [KitchenLayout.headRightEdgeUnits] -- the exact geometry `KitchenBear` (VoiceScenes.kt)
     * hands to `Critter`'s own `lean`/`leanForward` parameters -- never passes the counter's own
     * left edge for the lean [KitchenLayout.bearLean] actually returns, at every size Ruby asked
     * to check: tablet portrait/landscape and phone portrait/landscape. It should also stay
     * clearly visible (at least [KitchenLayout.MIN_LEAN_DEG]) at all four, since there's enough
     * room in this layout for that; the clamp itself (tested separately below) is allowed to go
     * smaller if a future size were ever tighter than that. */
    @Test
    fun bearLeanNeverCrossesTheCounter() {
        val sizes = listOf(768f to 1024f, 1024f to 768f, 390f to 844f, 844f to 390f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val box = bearOnScreenBox(w, h, landscape)
            val counterLeftX = w * KitchenLayout.bearBox(landscape).r
            val lean = KitchenLayout.bearLean(box, counterLeftX)

            assertTrue(lean.angleDeg >= KitchenLayout.MIN_LEAN_DEG, "lean too small at ${w}x$h: ${lean.angleDeg}")

            val u = (box.b - box.t) / 140f
            val headRightEdge = box.l + KitchenLayout.headRightEdgeUnits(lean.angleDeg, lean.forwardUnits) * u

            assertTrue(headRightEdge <= counterLeftX + 0.5f, "head past the counter at ${w}x$h: $headRightEdge > $counterLeftX")
        }
    }

    /** [KitchenLayout.bearLean]'s own safety net: a counter gap tighter than the full lean needs
     * (but roomier than his resting, no-lean head) comes back with a *smaller* lean that still
     * keeps the head's own right edge at or before the counter -- the clamp (a binary search in
     * [KitchenLayout.bearLean]) actually engaging, not just never being tight enough to matter in
     * the four real sizes [bearLeanNeverCrossesTheCounter] checks. */
    @Test
    fun bearLeanClampsWhenTheGapIsTighterThanFullLean() {
        val box = KitchenLayout.Rect(l = 0f, t = 0f, r = 100f, b = 140f) // u = 1f
        val counterLeftX = 90f // less than a full lean's own reach, more than resting's
        val lean = KitchenLayout.bearLean(box, counterLeftX)
        val headRightEdge = box.l + KitchenLayout.headRightEdgeUnits(lean.angleDeg, lean.forwardUnits)
        assertTrue(headRightEdge <= counterLeftX + 0.5f, "head past the counter: $headRightEdge > $counterLeftX")
        assertTrue(lean.angleDeg < KitchenLayout.MAX_LEAN_DEG, "clamp should have engaged: ${lean.angleDeg}")
    }

    /** Even a gap too tight for his head at rest (an unrealistic input: Bear's own box is never
     * wider than the gap by design, see [bearBox]) must come back with a sensible floor -- no
     * lean at all, not a crash or a negative angle -- rather than making things worse. */
    @Test
    fun bearLeanFloorsAtZeroWhenEvenRestingWouldCrossTheCounter() {
        val box = KitchenLayout.Rect(l = 0f, t = 0f, r = 20f, b = 140f) // u = 1f, 20 units wide
        val counterLeftX = 60f // well inside his resting head's own reach
        val lean = KitchenLayout.bearLean(box, counterLeftX)
        assertEquals(0f, lean.angleDeg)
        assertEquals(0f, lean.forwardUnits)
    }

    /** K2i: Bear's resting (non-lean) stool must sit fully inside the scene, not near the bottom
     * edge, or there's no room left for the lean itself. */
    @Test
    fun bearsRestingPositionStaysClearOfTheBottomEdge() {
        val sizes = listOf(768f to 1024f, 1024f to 768f, 390f to 844f, 844f to 390f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val box = bearOnScreenBox(w, h, landscape)
            assertTrue(box.b <= h * (1f - KitchenLayout.BOTTOM_MARGIN_FRACTION), "Bear's resting box reaches the bottom edge at ${w}x$h")
        }
    }

    /** K2k: shifting Bear (and his stool) left must never push him off the screen's own left
     * edge, on an iPad-size stage (same two sizes the shift itself was measured against, see
     * [bearsRestingBoxLeavesAGapBeforeTheCounter]): his drawn box must keep at least a few px
     * clear of it. A narrow phone's lane already sits this close to the edge before this shift
     * (its own pre-existing tightness, not something this shift changes, since the shift only
     * ever narrows Bear's *fitted width*, never his lane's own left edge), so it's out of scope
     * here. */
    @Test
    fun bearStaysOnScreenAfterTheLeftShift() {
        val sizes = listOf(768f to 1024f, 1024f to 768f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val box = bearOnScreenBox(w, h, landscape)
            assertTrue(box.l >= 4f, "Bear's box sits too close to (or off) the left edge at ${w}x$h: ${box.l}")
        }
    }

    /** K2k's own point: the left shift must open a real gap between Bear's *resting* box and the
     * counter (never the lean itself, see [bearLeanNeverCrossesTheCounter]), on an iPad-size
     * stage, or the shift bought nothing -- a tighter phone size may have less slack to spare, so
     * this only checks the two sizes the shift was measured against. */
    @Test
    fun bearsRestingBoxLeavesAGapBeforeTheCounter() {
        val sizes = listOf(768f to 1024f, 1024f to 768f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val box = bearOnScreenBox(w, h, landscape)
            val counterLeftX = w * KitchenLayout.bearBox(landscape).r
            assertTrue(counterLeftX - box.r >= 10f, "resting gap before the counter too small at ${w}x$h: ${counterLeftX - box.r}")
        }
    }

    /** K3b: on a real iPad the bowl rendered up over the wall, near Bear, because the generic
     * move-based position (`pose` in VoiceScenes.kt, shared with the open-air boat/owl/balloon
     * scenes) never knew where the counter actually was. Every kitchen prop's own resting box
     * ([KitchenLayout.propBox]) must sit on the counter instead: its bottom on the counter's own
     * surface ([KitchenLayout.PROPS_MARGIN] below the counter-top line), horizontally clear of
     * Bear's lane and the counter's own right edge, at every size Timothy's note asked for
     * (tablet portrait/landscape, a wider tablet, phone portrait/landscape) and whether or not the
     * choice strip is reserved below (the scene's own box already excludes it, same as every
     * other KitchenLayout fraction, so a size here stands for both). */
    @Test
    fun kitchenPropsAlwaysSitOnTheCounter() {
        // Mirrors VoiceScenes.kt's own `propSize` for these five (the one thing this test and
        // that must agree on, or a changed size could quietly drift out of sync with where this
        // thinks it ends).
        val sizeFractions = mapOf(
            Prop.BOWL to 0.18f, Prop.MILK to 0.2f, Prop.BERRIES to 0.2f, Prop.PAN to 0.16f, Prop.PANCAKE to 0.15f,
        )
        val sizes = listOf(
            1024f to 768f, 768f to 1024f, 1180f to 820f, 820f to 1180f, 1366f to 1024f, 390f to 844f, 844f to 390f,
        )
        for ((w, h) in sizes) {
            val landscape = w > h
            val bearRight = KitchenLayout.bearBox(landscape).r
            val expectedBottom = KitchenLayout.counterTop(landscape) + KitchenLayout.PROPS_MARGIN
            for ((prop, sizeFraction) in sizeFractions) {
                val box = KitchenLayout.propBox(prop, landscape, sizeFraction, w, h)
                assertTrue(box.l >= bearRight, "$prop reaches Bear's lane at ${w}x$h: ${box.l} < $bearRight")
                assertTrue(box.r <= 1f, "$prop reaches past the counter's right edge at ${w}x$h: ${box.r}")
                assertEquals(expectedBottom, box.b, 0.0001f, "$prop's bottom isn't on the counter's own surface at ${w}x$h")
                assertTrue(box.t < box.b, "$prop has no height at ${w}x$h")
            }
        }
    }

    /** K3c: a screenshot (1024x768 and 768x1024) showed the bowl, milk and berries sitting right
     * over Rory's face -- [kitchenPropsAlwaysSitOnTheCounter] above only ever checked
     * [KitchenLayout.propBox], which clamps a prop's span into its own station, so it could never
     * have caught this (clamping just hides an oversized prop behind a smaller box than the one
     * actually drawn). This checks [KitchenLayout.propNaturalBox] -- the unclamped box
     * `VoiceScenes.kt`'s `Item` really draws -- against [KitchenLayout.roryVisibleBox] (Rory's own
     * visible slice, same reasoning as [bearRoryBowlAndPanNeverOverlapEachOther]) and against every
     * prop in the other station, at every size Timothy's note asked for: the two sizes the bug
     * screenshots were taken at, a wider tablet, a taller one, and both orientations of a phone.
     * Sizes mirror `kitchenPropSize` in VoiceScenes.kt (BOWL/MILK/BATTER/BERRIES/PAN/PANCAKE/STEAM
     * match [propSize] there exactly; BUBBLE is Rory's Kitchen's own smaller override of that
     * function, since [propSize]'s own 0.3 is sized for the River story's open-air bubble, not a
     * pancake's; K7: BOWL/BATTER/BUTTER grow and PAN shrinks a little, in landscape only, see
     * [kitchenSizeMirror]'s own doc). */
    @Test
    fun kitchenPropsNeverOverlapRoryOrTheOtherStation() {
        // K6: milk moved off the bowl's own station onto the ingredient one (see
        // [KitchenLayout.ingredientBox]'s own doc -- Timothy's screenshot showed it drawn flush on
        // top of the bowl, hiding it completely); sizes mirror `kitchenPropSize` in VoiceScenes.kt.
        // Prop.BERRIES isn't checked here any more: the counting beat draws its own three waiting
        // berries directly (see [KitchenLayout.berryBox]/[coOccurringKitchenPropsNeverOverlapEachOther]),
        // and the "how many?" ask beat that follows reuses Prop.BATTER instead of a second,
        // overlapping single-pile render at the very same spot (K6's own milk-on-the-bowl bug, in
        // a new place) -- the generic single-prop Prop.BERRIES render is never reached in Rory's
        // Kitchen any more.
        val bowlStationProps = listOf(Prop.BOWL, Prop.BATTER)
        // K7d: BUTTER has its own station (KitchenLayout.butterBox) and is only ever drawn alone;
        // it's checked against Rory below and by KitchenSceneRulesTest's per-beat co-occurrence check.
        val ingredientStationProps = listOf(Prop.MILK)
        val panStationProps = listOf(Prop.PAN, Prop.PANCAKE, Prop.BUBBLE, Prop.STEAM)
        val sizes = listOf(
            1024f to 768f, 768f to 1024f, 1180f to 820f, 820f to 1180f, 1366f to 1024f, 390f to 844f, 844f to 390f,
        )
        for ((w, h) in sizes) {
            val landscape = w > h
            val rory = KitchenLayout.roryVisibleBox(landscape)
            val bowlBoxes = bowlStationProps.map { p -> p to KitchenLayout.propNaturalBox(p, landscape, kitchenSizeMirror(p, landscape), w, h) }
            val ingBoxes = ingredientStationProps.map { p -> p to KitchenLayout.propNaturalBox(p, landscape, kitchenSizeMirror(p, landscape), w, h) }
            val panBoxes = panStationProps.map { p -> p to KitchenLayout.propNaturalBox(p, landscape, kitchenSizeMirror(p, landscape), w, h) }
            val butterBox = KitchenLayout.propNaturalBox(Prop.BUTTER, landscape, kitchenSizeMirror(Prop.BUTTER, landscape), w, h)
            assertFalse(rory.overlaps(butterBox), "rory overlaps BUTTER at ${w}x$h")
            assertTrue(butterBox.r <= 1f, "BUTTER off the right edge at ${w}x$h")
            for ((prop, box) in bowlBoxes + ingBoxes + panBoxes) {
                assertFalse(rory.overlaps(box), "rory overlaps $prop at ${w}x$h")
            }
            for ((bowlProp, bowlBox) in bowlBoxes) for ((panProp, panBox) in panBoxes) {
                assertFalse(bowlBox.overlaps(panBox), "$bowlProp overlaps $panProp at ${w}x$h")
            }
            for ((bowlProp, bowlBox) in bowlBoxes) for ((ingProp, ingBox) in ingBoxes) {
                assertFalse(bowlBox.overlaps(ingBox), "$bowlProp overlaps $ingProp at ${w}x$h")
            }
            // K7e: portrait's pan station reuses the ingredient station's counter space -- milk (beat
            // 2) and the pan (from "Say pour!" on) never share a beat, which KitchenSceneRulesTest's
            // per-beat co-occurrence check enforces -- so the pan can be a real size on a 768px-wide
            // counter next to a 1.5x Rory. Landscape keeps separate stations.
            if (landscape) for ((ingProp, ingBox) in ingBoxes) for ((panProp, panBox) in panBoxes) {
                assertFalse(ingBox.overlaps(panBox), "$ingProp overlaps $panProp at ${w}x$h")
            }
        }
    }

    /**
     * K6: Timothy's screenshot showed the milk carton drawn exactly on top of the bowl (both
     * mapped to the same station, same centre) -- the fix gives milk its own ingredient station
     * ([KitchenLayout.ingredientBox]), but nothing before this checked the specific PAIRS of props
     * that actually appear together in one beat, only "this station's props" vs "that station's
     * props" as two whole groups ([kitchenPropsNeverOverlapRoryOrTheOtherStation] above). Checks
     * every beat's own real combination directly, at the sizes Timothy reported (a landscape
     * tablet and its portrait twin): bowl+milk (beat 2, milk pours in) and bowl/batter+the three
     * waiting berries (the counting beat, [KitchenLayout.berrySlot]). Local-fraction overlap is
     * the same whether or not the choice strip is reserved below (the strip only rescales the
     * WHOLE stage afterward, see [KitchenLayout.onStage] -- a monotonic rescale can't make two
     * boxes that were clear of each other start overlapping), so checking both orientations here
     * covers every combination of strip-shown-or-not Timothy asked about too.
     */
    @Test
    fun coOccurringKitchenPropsNeverOverlapEachOther() {
        val sizes = listOf(1024f to 768f, 768f to 1024f)
        for ((w, h) in sizes) {
            val landscape = w > h
            val bowl = KitchenLayout.propNaturalBox(Prop.BOWL, landscape, kitchenSizeMirror(Prop.BOWL, landscape), w, h)
            val milk = KitchenLayout.propNaturalBox(Prop.MILK, landscape, kitchenSizeMirror(Prop.MILK, landscape), w, h)
            assertFalse(bowl.overlaps(milk), "bowl overlaps milk at ${w}x$h")
            val batter = KitchenLayout.propNaturalBox(Prop.BATTER, landscape, kitchenSizeMirror(Prop.BATTER, landscape), w, h)
            val berries = (0..2).map { i -> KitchenLayout.berryBox(i, landscape, w, h) }
            for (b in berries) assertFalse(batter.overlaps(b), "batter overlaps a waiting berry at ${w}x$h")
            for (i in berries.indices) for (j in i + 1 until berries.size) {
                assertFalse(berries[i].overlaps(berries[j]), "berry $i overlaps berry $j at ${w}x$h")
            }
        }
    }

    /** K7/K8: a duplicate, platform-free mirror of `kitchenPropSize` in VoiceScenes.kt (the one
     * thing this and that must agree on, same spirit as [KitchenLayout.symmetricQuadApexY]'s own
     * shared-constant fix) -- the bowl and its batter read bigger again in both orientations (K8:
     * Timothy's own "bowl still looks small" ask), the butter plate splitting off its own, smaller
     * size (it has its own target, not the bowl's), and the pan a little smaller in landscape to
     * leave them the room. */
    private fun kitchenSizeMirror(prop: Prop, landscape: Boolean): Float = when (prop) {
        Prop.BUBBLE -> if (landscape) 0.18f else 0.17f
        Prop.BOWL, Prop.BATTER -> if (landscape) 0.2533f else 0.215f
        Prop.BUTTER -> 0.40f
        Prop.PAN -> if (landscape) 0.144f else 0.17f
        Prop.PANCAKE -> if (landscape) 0.15f else 0.17f
        Prop.MILK -> 0.12f
        Prop.PANCAKE -> 0.15f
        Prop.STEAM -> if (landscape) 0.14f else 0.15f
        else -> 0.14f
    }

    /** The bowl/batter/berries beats, the ingredient (milk/butter) beats and the
     * pan/pancake/bubble/steam beats never share a station (K3b; K6 split the old bowl/milk
     * station in two): each kitchen prop goes to exactly one of the three counter slots. */
    @Test
    fun everyKitchenPropHasExactlyOneStation() {
        val bowlProps = setOf(Prop.BOWL, Prop.BATTER, Prop.BERRIES)
        val ingredientProps = setOf(Prop.MILK)
        val panProps = setOf(Prop.PAN, Prop.PANCAKE, Prop.BUBBLE, Prop.STEAM)
        for (landscape in listOf(false, true)) {
            for (prop in bowlProps) assertEquals(KitchenLayout.bowlBox(landscape), KitchenLayout.propSlot(prop, landscape))
            for (prop in ingredientProps) assertEquals(KitchenLayout.ingredientBox(landscape), KitchenLayout.propSlot(prop, landscape))
            assertEquals(KitchenLayout.butterBox(landscape), KitchenLayout.propSlot(Prop.BUTTER, landscape))
            for (prop in panProps) assertEquals(KitchenLayout.panBox(landscape), KitchenLayout.propSlot(prop, landscape))
        }
    }

    /** K5: Timothy's own measured screenshot (1024x768 landscape, the TAP step with the choice
     * strip shown) had the counter top at y~345 and Bear's drawn stool seat at y~372-385, with
     * Bear himself (y~265-370) sitting lower than the counter too -- a net gap of about 4% of the
     * scene's own height between [KitchenLayout.stoolTop] (the stool's own drawn seat, see
     * `paperStool` in VoiceScenes.kt) and [KitchenLayout.counterTop], once [BEAR_SEAT_OVERLAP] is
     * taken out of [BEAR_SEAT_GAP]. These two numbers never change between the tap and talk steps
     * (both are pure functions of `landscape` alone, see [bearsSeatOverlapsTheStoolSeat]'s own
     * doc), so one check per orientation covers every step. */
    @Test
    fun stoolSeatTopIsWithinTwoPercentOfTheCounterTop() {
        for (landscape in listOf(false, true)) {
            val ct = KitchenLayout.counterTop(landscape)
            val stoolTop = KitchenLayout.stoolTop(landscape)
            val diff = kotlin.math.abs(stoolTop - ct)
            assertTrue(
                diff <= 0.02f,
                "stool seat too far from the counter top in ${if (landscape) "landscape" else "portrait"}: stoolTop=$stoolTop ct=$ct diff=$diff",
            )
        }
    }

    /**
     * K6: Timothy's screenshot measured Bear's own body at x~55-115 (centre ~85) and the stool's
     * drawn seat at x~38-90 (centre ~64) -- he read as sitting beside the stool, not on it, since
     * the seat's own canvas (`KitchenBear` in VoiceScenes.kt) used to be pinned to the lane's left
     * edge while Bear's own puppet filled the whole lane. [KitchenLayout.stoolSeatCenterXFraction]
     * must always land at 0.5 (the same centre Bear's own box sits at, for any width -- his lane's
     * width, Bear's own box width, whatever), at every phase and orientation, since both are pure
     * functions of the box width alone: the fix isn't phase- or orientation-specific, so neither
     * is this check. */
    @Test
    fun stoolSeatIsHorizontallyCentredUnderBear() {
        for (boxWidth in listOf(77f, 123f, 200f, 340f)) {
            assertEquals(0.5f, KitchenLayout.stoolSeatCenterXFraction(boxWidth), 0.0001f, "stool not centred at width $boxWidth")
        }
    }

    /** K5: a real screenshot (the POP step, landscape) showed the frying pan's own drawn bottom
     * floating about 15px above the counter top it's meant to sit on, and the portrait "count the
     * berries" TAP step showed the berry pile floating the same way -- the props' own tap-target
     * BOXES ([kitchenPropsAlwaysSitOnTheCounter]) already sat right on the counter, but the
     * hand-drawn paper art inside each box's Canvas (VoiceScenes.kt) stopped short of the box's
     * own bottom edge. [KitchenLayout.symmetricQuadApexY] and the five props' own shared geometry
     * constants are the exact numbers that Canvas drawing uses now, so this confirms each one's
     * own lowest painted pixel lands on its box's bottom edge (fraction 1f, no padding) without
     * needing to render anything. */
    @Test
    fun kitchenPropArtTouchesItsOwnBoxBottomWithNoPadding() {
        val tolerance = 0.0001f
        assertEquals(
            1f,
            KitchenLayout.symmetricQuadApexY(KitchenLayout.BOWL_RIM_Y, KitchenLayout.BOWL_OUTER_CONTROL_Y),
            tolerance,
            "bowl's own lowest drawn point doesn't touch its box's bottom edge",
        )
        assertEquals(1f, KitchenLayout.MILK_ART_BOTTOM, tolerance, "milk carton's own drawn bottom doesn't touch its box's bottom edge")
        assertEquals(
            1f,
            KitchenLayout.BERRIES_LOWEST_CENTER_Y + KitchenLayout.BERRIES_RADIUS_FRACTION,
            tolerance,
            "the lowest berry's own drawn bottom doesn't touch its box's bottom edge",
        )
        assertEquals(
            1f,
            KitchenLayout.PAN_ART_TOP + KitchenLayout.PAN_ART_HEIGHT,
            tolerance,
            "the pan's own drawn bottom doesn't touch its box's bottom edge",
        )
        assertEquals(
            1f,
            KitchenLayout.PANCAKE_ART_TOP + KitchenLayout.PANCAKE_ART_HEIGHT,
            tolerance,
            "the pancake's own drawn bottom doesn't touch its box's bottom edge",
        )
    }
}

/**
 * K7 (Ruby's scene checklist, one shared set of rules for every kitchen beat): props never
 * overlap unless it's a real "in/on" relationship ([ALLOWED_OVERLAP_PAIRS]); whatever's drawn
 * never covers Rory's or Bear's own face ([KitchenLayout.roryFaceBox]/[bearFaceBox]); Rory is the
 * biggest thing on the counter, Bear about half his height ([KitchenLayout.MIN_RORY_TO_BEAR_RATIO]);
 * Bear keeps a real gap above the choice strip; and both of them stay mostly on screen. Pure
 * [KitchenLayout] maths against every real beat in [VoiceStories.kitchen], so this is cheap to run
 * and needs no rendering.
 */
class KitchenSceneRulesTest {
    /** The three co-occurring (prop, still) pairs in [VoiceStories.kitchen] that share a counter
     * station ON PURPOSE (both map to the same [KitchenLayout.propSlot]), since one really sits
     * in or on the other: the berries tumble into the batter, the pancake sits in the pan, and the
     * pan's own steam rises from the same cooking pan as its pancake's bubbles. */
    private val allowedOverlapPairs = setOf(
        setOf(Prop.BERRIES, Prop.BATTER),
        setOf(Prop.PANCAKE, Prop.PAN),
        setOf(Prop.BUBBLE, Prop.STEAM),
    )

    /** A duplicate, platform-free mirror of `kitchenPropSize` in VoiceScenes.kt (the one thing
     * this and that must agree on): the bowl and its batter read bigger in both orientations (K8),
     * the butter plate its own smaller size, the pan a little smaller in landscape to leave them
     * room. */
    private fun kitchenSize(prop: Prop, landscape: Boolean): Float = when (prop) {
        Prop.BUBBLE -> if (landscape) 0.18f else 0.17f
        Prop.BOWL, Prop.BATTER -> if (landscape) 0.2533f else 0.215f
        Prop.BUTTER -> 0.40f
        Prop.PAN -> if (landscape) 0.144f else 0.17f
        Prop.PANCAKE -> if (landscape) 0.15f else 0.17f
        Prop.MILK -> 0.12f
        Prop.PANCAKE -> 0.15f
        Prop.STEAM -> if (landscape) 0.14f else 0.15f
        else -> 0.14f
    }

    private val sizes = listOf(1024f to 768f, 768f to 1024f)

    @Test
    fun noTwoRestingPropsInABeatOverlapUnlessItsARealInOrOnPair() {
        for ((w, h) in sizes) {
            val landscape = w > h
            for (beat in VoiceStories.kitchen.beats) {
                val still = beat.still ?: continue
                val propBox = KitchenLayout.propNaturalBox(beat.prop, landscape, kitchenSize(beat.prop, landscape), w, h)
                val stillBox = KitchenLayout.propNaturalBox(still, landscape, kitchenSize(still, landscape), w, h)
                if (setOf(beat.prop, still) in allowedOverlapPairs) continue
                assertFalse(
                    propBox.overlaps(stillBox),
                    "${beat.prop} overlaps $still at ${w}x$h (beat '${beat.caption}') -- not a real in/on pair",
                )
            }
        }
    }

    @Test
    fun noRestingPropEverCoversRoryOrBearsFace() {
        for ((w, h) in sizes) {
            val landscape = w > h
            val roryFace = KitchenLayout.roryFaceBox(landscape)
            val bearFace = KitchenLayout.bearFaceBox(landscape)
            for (beat in VoiceStories.kitchen.beats) {
                for (prop in listOfNotNull(beat.prop, beat.still)) {
                    val box = KitchenLayout.propNaturalBox(prop, landscape, kitchenSize(prop, landscape), w, h)
                    assertFalse(box.overlaps(roryFace), "$prop covers Rory's face at ${w}x$h (beat '${beat.caption}')")
                    assertFalse(box.overlaps(bearFace), "$prop covers Bear's face at ${w}x$h (beat '${beat.caption}')")
                }
            }
        }
    }

    @Test
    fun roryIsTheBiggestThingBearAboutHalfHisHeight() {
        // K8: checks the real FITTED boxes now (what `KitchenRory`/`KitchenBear` actually draw),
        // not the nominal lane boxes -- a real screenshot showed the two lanes' own nominal
        // heights are a poor stand-in for this once their fitted sizes (narrowed by each lane's
        // own width, see [KitchenLayout.roryFittedBox]/[KitchenLayout.bearFittedBox]) no longer
        // shrink by the same proportion (K8 widened Bear's lane relatively more than Rory's own,
        // see [bearRight]/[roryLeft]'s own K8 notes in KitchenBeat.kt).
        for ((w, h) in sizes) {
            val landscape = w > h
            val rory = KitchenLayout.roryFittedBox(w, h, landscape)
            val bear = KitchenLayout.bearFittedBox(w, h, landscape)
            val roryVisibleHeight = h * KitchenLayout.counterTop(landscape) - rory.t
            val bearHeight = bear.b - bear.t
            assertTrue(
                roryVisibleHeight >= bearHeight * KitchenLayout.MIN_RORY_TO_BEAR_RATIO,
                "Rory isn't at least ${KitchenLayout.MIN_RORY_TO_BEAR_RATIO}x Bear's real drawn height at ${w}x$h: rory=$roryVisibleHeight bear=$bearHeight",
            )
        }
    }

    /** Mirrors VoiceStory.kt's own strip reservation exactly ([KitchenLayout.onStage] for the
     * landscape lift, a plain shrink for portrait -- same two formulas
     * [bearRoryBowlAndPanNeverReachTheChoiceStrip]/[kitchenPropsSitOnTheRealDrawnCounterTopInLandscape]
     * already use), at the two sizes Timothy reported. */
    @Test
    fun bearKeepsAtLeastASixteenPixelGapAboveTheChoiceStrip() {
        val minGapPx = 16f
        for ((w, h) in sizes) {
            val landscape = w > h
            val stripFraction = if (landscape) 0.18f else 0.22f
            val stripMinHeight = 128f
            fun onStage(local: Float) = if (landscape) {
                KitchenLayout.onStage(local, true, h, stripFraction, stripMinHeight)
            } else {
                local * (1f - stripFraction)
            }
            val gapPx = (onStage(1f) - onStage(KitchenLayout.BEAR_FLOOR)) * h
            assertTrue(gapPx >= minGapPx, "gap above the strip under Bear too small at ${w}x$h: ${gapPx}px")
        }
    }

    /** The landscape lift clips away whatever's above the visible scene's own top ([onStage]);
     * portrait never clips, only shrinks. Both of them must still keep at least two thirds of
     * Rory's and Bear's own box on screen. */
    @Test
    fun atLeastTwoThirdsOfRoryAndBearStayOnScreen() {
        for ((w, h) in sizes) {
            val landscape = w > h
            val stripFraction = if (landscape) 0.18f else 0.22f
            val stripMinHeight = 128f
            fun onStage(local: Float) = if (landscape) {
                KitchenLayout.onStage(local, true, h, stripFraction, stripMinHeight)
            } else {
                local * (1f - stripFraction)
            }
            fun visibleFraction(box: KitchenLayout.Rect): Float {
                val fullTop = onStage(box.t)
                val fullBottom = onStage(box.b)
                val visibleTop = maxOf(fullTop, 0f)
                val fullHeight = fullBottom - fullTop
                return ((fullBottom - visibleTop).coerceAtLeast(0f)) / fullHeight
            }
            assertTrue(
                visibleFraction(KitchenLayout.roryBox(landscape)) >= 2f / 3f,
                "Rory not 2/3 on screen at ${w}x$h",
            )
            assertTrue(
                visibleFraction(KitchenLayout.bearBox(landscape)) >= 2f / 3f,
                "Bear not 2/3 on screen at ${w}x$h",
            )
        }
    }
}

/** Rory's Kitchen's own ending (Ruby's K2c): the stack-of-pancakes/bite/sticker-fly sequence
 * lands well inside the designer's "within 3 s of the bite, under ~4 s total, no tap needed"
 * rules, and the carry-on wait after the bye line is the usual 1.5 to 2.5 s shape. */
class KitchenEndingTest {
    @Test
    fun stickerLandsWithinThreeSecondsOfTheBiteStarting() {
        assertTrue(KitchenEnding.stickerLandsAfterBiteStartsMs <= 3000)
    }

    @Test
    fun stackBiteAndFlyFinishWellUnderFourSeconds() {
        assertTrue(KitchenEnding.stackToLandedMs < 4000)
    }

    @Test
    fun leaveDelayIsAroundOneAndAHalfToTwoAndAHalfSeconds() {
        assertTrue(KitchenEnding.LEAVE_DELAY_MS in 1500..2500)
    }

    @Test
    fun timedStagesRunInOrderWithNoNegativeOrZeroSpan() {
        val order = listOf(KitchenEndStage.STACK, KitchenEndStage.BITE, KitchenEndStage.STICKER_FLY, KitchenEndStage.IN_BACKPACK)
        assertEquals(order, KitchenEnding.timedStages.map { it.first })
        for ((_, ms) in KitchenEnding.timedStages) assertTrue(ms > 0)
    }

    @Test
    fun stickerLandsExactlyWhenBiteAndFlyHaveBothElapsed() {
        assertEquals(KitchenEnding.BITE_MS + KitchenEnding.FLY_MS, KitchenEnding.stickerLandsAfterBiteStartsMs)
        assertEquals(KitchenEnding.STACK_MS + KitchenEnding.BITE_MS + KitchenEnding.FLY_MS, KitchenEnding.stackToLandedMs)
    }

    /** K2g: Bear's big lean (ease in, hold the full lean, settle back) must fit inside the bite
     * stage's own span, or the lean gets cut off before it's held or settled. */
    @Test
    fun biteStageLastsLongEnoughForTheFullLean() {
        val leanSpan = KitchenEnding.LEAN_IN_MS + KitchenEnding.LEAN_HOLD_MS + KitchenEnding.LEAN_SETTLE_MS
        assertTrue(KitchenEnding.BITE_MS >= leanSpan)
    }
}
