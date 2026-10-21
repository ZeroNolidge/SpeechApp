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
            assertTrue(s.beats.size in 6..VoiceStories.MAX_BEATS, "${s.id}: ${s.beats.size} beats")
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
        val soft = meter(room + List(10) { 0.009f } + List(70) { 0.004f })
        assertTrue(!soft.heard && soft.soft)
        val quiet = meter(room + List(80) { 0.004f })
        assertTrue(!quiet.heard && !quiet.soft)
        val loud = meter(room + List(10) { 0.05f } + List(20) { 0.004f })
        assertTrue(loud.heard && !loud.soft)
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
        assertTrue(fresh.threshold > 0.1f)
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
        assertTrue(pops(Triple(0.009f, 0.5f, 0.1f), Triple(0.009f, 0.5f, 0.1f)).heard)
        // Steady room-level readings, each well under the near-threshold band, never count.
        assertTrue(!pops(Triple(0.006f, 0.5f, 0.1f), Triple(0.006f, 0.5f, 0.1f), Triple(0.006f, 0.5f, 0.1f)).heard)
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
        val allowed = setOf("flip", "three")
        for (b in VoiceStories.kitchen.beats) {
            val first = b.word.first().lowercaseChar()
            assertTrue(first in "pbm" || b.word in allowed, "unexpected kitchen target word '${b.word}'")
        }
    }

    @Test
    fun theFiveSpecialBeatsCarryTheRightFlowAndGoal() {
        val byWord = VoiceStories.kitchen.beats.associateBy { it.word }
        assertEquals(KitchenFlow.TAP_CHOICE, byWord.getValue("bowl").kitchen)
        assertEquals(listOf("cup", "spoon"), byWord.getValue("bowl").kitchenChoices)
        assertEquals(KitchenFlow.STIR, byWord.getValue("mix").kitchen)
        assertEquals(3, byWord.getValue("mix").kitchenGoal)
        assertEquals(KitchenFlow.COUNT, byWord.getValue("three").kitchen)
        assertEquals(3, byWord.getValue("three").kitchenGoal)
        assertEquals(KitchenFlow.BUBBLES, byWord.getValue("pop").kitchen)
        assertEquals(3, byWord.getValue("pop").kitchenGoal)
        assertEquals(KitchenFlow.FLIP, byWord.getValue("flip").kitchen)
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
}

/** The kitchen flows' pure state machine (see [KitchenBeatState]): nothing ever fails, and every
 * path reaches [KitchenPhase.DONE]. */
class KitchenBeatStateTest {
    @Test
    fun tapChoiceRightFirstTimeSkipsStraightToTalk() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true)
        assertEquals(KitchenPhase.TALK, s.phase)
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
        assertEquals(KitchenPhase.TALK, s.phase)
        assertFalse(s.rightFirstTry)
    }

    @Test
    fun tapChoiceTwoWrongTapsShowTheAnswerThenTalk() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(false)
        assertEquals(KitchenPhase.SHOW_ANSWER, s.phase)
        assertEquals(KitchenPhase.TALK, s.shown().phase)
    }

    @Test
    fun tapChoiceOneTalkTryThenClose() {
        val s = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true).talked()
        assertEquals(KitchenPhase.CLOSE, s.phase)
        assertEquals(KitchenPhase.DONE, s.closed().phase)
    }

    @Test
    fun flipIsOneTapWithNoWrongAnswerThenOneTalkTurn() {
        var s = KitchenBeatState(KitchenFlow.FLIP).named()
        assertEquals(KitchenPhase.TAP, s.phase)
        // A stray "wrong" tap is impossible for this flow: it's simply ignored, never fails.
        assertEquals(s, s.tap(false))
        s = s.tap(true)
        assertEquals(KitchenPhase.TALK, s.phase)
        s = s.talked()
        assertEquals(KitchenPhase.CLOSE, s.phase)
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
        val talking = KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true)
        assertEquals(talking, talking.wiggled())
        assertEquals(talking, talking.shown())
        assertEquals(talking, talking.closed())
        val start = KitchenBeatState(KitchenFlow.STIR, goal = 3)
        assertEquals(start, start.talked())
        assertEquals(start, start.tap(true))
    }

    @Test
    fun everyFlowAndEveryPathReachesDone() {
        val paths = listOf(
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(true).talked(),
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(true).talked(),
            KitchenBeatState(KitchenFlow.TAP_CHOICE).named().tap(false).wiggled().tap(false).shown().talked(),
            KitchenBeatState(KitchenFlow.FLIP).named().tap(true).talked(),
            KitchenBeatState(KitchenFlow.STIR, goal = 3).named().talked().talked().talked(),
            KitchenBeatState(KitchenFlow.BUBBLES, goal = 3).named().talked().talked().talked(),
            KitchenBeatState(KitchenFlow.COUNT, goal = 3).named().tap(true).tap(true).tap(true),
        )
        for (p in paths) {
            assertEquals(KitchenPhase.CLOSE, p.phase)
            assertEquals(KitchenPhase.DONE, p.closed().phase)
        }
    }
}

/** Rory's Kitchen's counter (K2a): landscape must draw a shorter wall / taller counter than
 * portrait, and keep at least [KitchenLayout.MIN_VISIBLE_FRACTION] of the scene as counter. */
class KitchenLayoutTest {
    @Test
    fun portraitCounterIsUnchanged() {
        assertEquals(0.5f, KitchenLayout.counterTop(landscape = false))
    }

    @Test
    fun landscapeCounterMeetsRubysFloor() {
        val counterFraction = 1f - KitchenLayout.counterTop(landscape = true)
        assertTrue(counterFraction >= KitchenLayout.MIN_VISIBLE_FRACTION)
    }

    @Test
    fun landscapeWallIsShorterThanPortrait() {
        assertTrue(KitchenLayout.counterTop(landscape = true) < KitchenLayout.counterTop(landscape = false))
    }

    /** Ruby's K2b: Bear (on his stool), Rory (behind the counter, chef's hat) and the counter's
     * own props (the bowl choice, the pan) never share any area, in either aspect ratio. */
    @Test
    fun bearRoryBowlAndPanNeverOverlapEachOther() {
        for (landscape in listOf(false, true)) {
            val boxes = listOf(
                "bear" to KitchenLayout.bearBox(landscape),
                "rory" to KitchenLayout.roryBox(landscape),
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
}
