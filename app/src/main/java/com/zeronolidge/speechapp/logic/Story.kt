package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.data.Things
import com.zeronolidge.speechapp.data.Word
import kotlin.random.Random

/** One learning task inside a story. */
sealed interface Challenge {
    val subject: Subject

    data class Say(val sound: SpeechSound, val word: Word) : Challenge {
        override val subject get() = Subject.SPEECH
    }
    data class Count(val round: CountRound) : Challenge {
        override val subject get() = Subject.NUMBERS
    }
    data class Math(val round: MathRound) : Challenge {
        override val subject get() = Subject.NUMBERS
    }
    data class More(val round: CompareRound) : Challenge {
        override val subject get() = Subject.NUMBERS
    }
    data class Letter(val round: LetterRound) : Challenge {
        override val subject get() = Subject.LETTERS
    }
    data class SoundOut(val round: ReadRound) : Challenge {
        override val subject get() = Subject.READING
    }
    data class WhichWord(val round: ReadRound) : Challenge {
        override val subject get() = Subject.READING
    }
    data class Pattern(val round: PatternRound) : Challenge {
        override val subject get() = Subject.PATTERNS
    }
    data class OddOne(val round: OddOneOutRound) : Challenge {
        override val subject get() = Subject.PATTERNS
    }
    data class Robot(val level: RobotLevel, val goalEmoji: String) : Challenge {
        override val subject get() = Subject.ROBOT
    }
}

/** A cartoon friend who needs help in the story. */
data class Friend(val name: String, val species: Species, val color: Long = species.defaultColor)

object Friends {
    val all = listOf(
        Friend("Benny Bunny", Species.BUNNY),
        Friend("Pip the Penguin", Species.PENGUIN),
        Friend("Freddie Frog", Species.FROG),
        Friend("Millie Mouse", Species.MOUSE),
        Friend("Bruno Bear", Species.BEAR),
        Friend("Olive Owl", Species.OWL),
        Friend("Daisy Dino", Species.DINO, 0xFFFFAB91),
        Friend("Felix Fox", Species.FOX),
        Friend("Kiki Kitty", Species.KITTY, 0xFFB39DDB),
        Friend("Rory Robot", Species.ROBOT, 0xFFFFCC80),
        Friend("Luna Unicorn", Species.UNICORN, 0xFFFFF0F6),
        Friend("Coco Chick", Species.CHICK),
        Friend("Pepper Puppy", Species.PUPPY, 0xFFBCAAA4),
    )
}

/** What the story is about: the friend's problem, and how it ends. */
data class Goal(val emoji: String, val intro: String, val ending: String)

object Goals {
    val all = listOf(
        Goal("🪁", "Oh no! The wind blew {f}'s kite away! Let's go find it!",
            "There's the kite, stuck in a tree! You got it down. {f} is so happy!"),
        Goal("🎂", "{f} is having a birthday party, but the party things are all over {land}! Let's help get ready!",
            "Everything is ready. Happy birthday, {f}! Let's sing and dance!"),
        Goal("💰", "{f} found an old treasure map! Let's follow it and find the treasure!",
            "We found the treasure chest! It's full of shiny gems!"),
        Goal("🧸", "{f} can't find Teddy anywhere! Let's look for Teddy together!",
            "Teddy was hiding behind a rock! Big hugs for Teddy!"),
        Goal("⭐", "A little star fell down from the sky! Let's help {f} take it home!",
            "Whoosh! The star is back in the sky. It twinkles to say thank you!"),
        Goal("🔑", "{f} is locked out of the clubhouse! We need to find the golden key!",
            "Click! The door opens. Welcome to the clubhouse!"),
        Goal("🧺", "{f} wants to have a picnic. Let's go and collect yummy food!",
            "Picnic time! Everyone shares the yummy food. Thank you, {hero}!"),
        Goal("🎈", "{f}'s balloon floated away! Let's follow it!",
            "Got it! The balloon is back, and {f} is giggling with joy!"),
    )
}

data class Beat(
    /** Something that happens on the way, said before the challenge. */
    val travel: String?,
    /** A sentence that sets up the challenge; the challenge then asks its own question. */
    val narration: String,
    val challenge: Challenge,
)

data class Quest(
    val land: Land,
    val friend: Friend,
    val goal: Goal,
    val intro: String,
    val beats: List<Beat>,
    val ending: String,
)

/** Everything the story generator needs to pitch the difficulty right. */
data class StoryParams(
    val heroName: String,
    val heroLevel: Int,
    /** Speech sounds the grown-up wants practised. Empty means all of them. */
    val focusSounds: Set<String> = emptySet(),
    /** How many robot puzzles have been solved so far. */
    val robotSolved: Int = 0,
    val patternLevel: Int = 0,
    val numbersSolved: Int = 0,
    val lettersSolved: Int = 0,
    /** Lands the hero can reach, for mixed adventures. */
    val unlockedLands: List<Land> = listOf(Land.FOREST, Land.MEADOW),
)

object StoryGen {
    private val props = mapOf(
        Land.FOREST to listOf("🍄", "🌰", "🍓", "🐞"),
        Land.MEADOW to listOf("🌸", "🐝", "🦋", "🍎", "🌼", "🥕"),
        Land.LAGOON to listOf("🐚", "🦀", "🐟", "🐠", "🍌"),
        Land.FACTORY to listOf("🔩", "🔋", "🚗", "⚽"),
        Land.PEAKS to listOf("🌰", "⭐", "💎", "🍎"),
        Land.CAVES to listOf("💎", "🕯️", "⭐", "🍄"),
        Land.CLOUDS to listOf("☁️", "⭐", "🎈", "🧁", "🍩"),
    )

    fun make(land: Land, p: StoryParams, rnd: Random, mixed: Boolean = false): Quest {
        val friend = Friends.all.random(rnd)
        val goal = Goals.all.random(rnd)
        val subjects = when {
            mixed || land == Land.CLOUDS -> mixedSubjects(p, rnd)
            land == Land.FOREST -> List(4) { Subject.SPEECH }
            land == Land.FACTORY -> List(3) { Subject.ROBOT }
            // Every land also squeezes in a little speech practice.
            else -> (List(3) { land.subjects.first() } + Subject.SPEECH).shuffled(rnd)
        }
        val used = mutableSetOf<String>()
        val counters = Counters(p)
        val beats = subjects.mapIndexed { i, subject ->
            val c = challenge(subject, land, goal, p, counters, used, rnd)
            Beat(
                travel = if (i == 0) null else fill(travel(land, p, rnd), p, friend, land),
                narration = fill(narrate(c, rnd), p, friend, land),
                challenge = c,
            )
        }
        return Quest(land, friend, goal, fill(goal.intro, p, friend, land), beats, fill(goal.ending, p, friend, land))
    }

    /** A mix of subjects from the lands the hero can reach, always including speech. */
    private fun mixedSubjects(p: StoryParams, rnd: Random): List<Subject> {
        val open = p.unlockedLands.flatMap { it.subjects }.distinct().filter { it != Subject.SPEECH }.shuffled(rnd)
        return (listOf(Subject.SPEECH) + open.take(4)).let { if (it.size < 4) it + Subject.NUMBERS + Subject.SPEECH else it }
            .take(5).shuffled(rnd)
    }

    /** Running tallies so a quest with several robot puzzles gets slightly harder as it goes. */
    private class Counters(p: StoryParams) {
        var robot = p.robotSolved
    }

    private fun challenge(
        subject: Subject,
        land: Land,
        goal: Goal,
        p: StoryParams,
        counters: Counters,
        used: MutableSet<String>,
        rnd: Random,
    ): Challenge {
        val item = (props[land] ?: Things.countables).random(rnd)
        return when (subject) {
            Subject.SPEECH -> {
                val sounds = SpeechContent.sounds.filter { p.focusSounds.isEmpty() || it.id in p.focusSounds }
                    .ifEmpty { SpeechContent.sounds }
                val options = sounds.flatMap { s -> s.words.map { s to it } }.filter { it.second.text !in used }
                val (sound, word) = options.ifEmpty { sounds.flatMap { s -> s.words.map { s to it } } }.random(rnd)
                used += word.text
                Challenge.Say(sound, word)
            }
            Subject.NUMBERS -> {
                val max = if (p.numbersSolved > 15) 10 else 5
                when (rnd.nextInt(if (p.heroLevel >= 2) 4 else 3)) {
                    0 -> Challenge.Count(Numbers.count(if (max > 5) 10 else 6, rnd, item))
                    1 -> Challenge.Math(Numbers.math(true, max, rnd, item))
                    2 -> Challenge.More(Numbers.compare(if (max > 5) 8 else 6, rnd, item))
                    else -> Challenge.Math(Numbers.math(false, max, rnd, item))
                }
            }
            Subject.LETTERS -> {
                val mode = LetterMode.entries.random(rnd)
                var round = Letters.make(mode, rnd, tricky = p.lettersSolved > 15)
                while ("L" + round.target.lower in used) round = Letters.make(mode, rnd, tricky = p.lettersSolved > 15)
                used += "L" + round.target.lower
                Challenge.Letter(round)
            }
            Subject.READING -> {
                val words = ReadingContent.cvcWords.filter { it.text !in used }.ifEmpty { ReadingContent.cvcWords }
                val round = Reading.make(words, rnd)
                used += round.word.text
                if (rnd.nextBoolean()) Challenge.SoundOut(round) else Challenge.WhichWord(round)
            }
            Subject.PATTERNS ->
                if (rnd.nextInt(3) == 0) Challenge.OddOne(OddOneOut.make(if (p.patternLevel > 0) 1 else 0, rnd))
                else Challenge.Pattern(Patterns.make(p.patternLevel, rnd))
            Subject.ROBOT -> {
                val n = counters.robot++
                Challenge.Robot(robotLevel(n, rnd), goal.emoji)
            }
        }
    }

    /** Arrow puzzles first, then the Cubetto-style turn-and-go puzzles, then a random mix. */
    fun robotLevel(solved: Int, rnd: Random): RobotLevel {
        val easy = RobotLevels.easy
        val turn = RobotLevels.robot
        return when {
            solved < easy.size -> easy[solved]
            solved < easy.size + turn.size -> turn[solved - easy.size]
            else -> RobotLevels.random(relative = rnd.nextBoolean(), rnd = rnd)
        }
    }

    private fun narrate(c: Challenge, rnd: Random): String = when (c) {
        is Challenge.Say -> listOf(
            "A magic door! It only opens when you say the magic word.",
            "{f} wants to learn a new word. Can you teach it?",
            "Look what we found! Let's say what it is.",
            "A talking tree says: say my word, and I'll let you pass!",
        ).random(rnd)
        is Challenge.Count -> listOf(
            "{f} found lots of ${Things.nameOf(c.round.emoji)}!",
            "Look at all these ${Things.nameOf(c.round.emoji)} on the path!",
            "We need ${Things.nameOf(c.round.emoji)} for the adventure. Let's count them.",
        ).random(rnd)
        is Challenge.Math -> if (c.round.plus) {
            "{f} is collecting ${Things.nameOf(c.round.emoji)}. Here come some more!"
        } else {
            "Uh oh! Some of {f}'s ${Things.nameOf(c.round.emoji)} rolled away!"
        }
        is Challenge.More -> "{f} has two baskets of ${Things.nameOf(c.round.emoji)}. We should take the bigger one!"
        is Challenge.Letter -> listOf(
            "The bridge is missing a stepping stone, and each stone has a letter!",
            "A sleepy crab is guarding the path. It wants to play a letter game!",
            "There's a message in a bottle! It has letters on it.",
        ).random(rnd)
        is Challenge.SoundOut -> "There's a sign on the wall! Let's sound out the word."
        is Challenge.WhichWord -> "Three signs point different ways. Only one has the right word!"
        is Challenge.Pattern -> listOf(
            "Magic beads light up the way, but one is missing!",
            "The stepping stones go in a pattern. Which stone comes next?",
        ).random(rnd)
        is Challenge.OddOne -> "One of these things got mixed up and doesn't belong!"
        is Challenge.Robot -> listOf(
            "A friendly robot cart can carry us! Tell it how to drive.",
            "The robot wants to help. Make a plan to get to the prize!",
        ).random(rnd)
    }

    private fun travel(land: Land, p: StoryParams, rnd: Random): String {
        val powers = Leveling.powersAt(p.heroLevel).filter { !it.helper }
        val lines = mutableListOf(
            "Let's keep going! {hero} skips down the path.",
            "Tip toe, tip toe... what's next?",
            "Onward, brave {hero}!",
            "{f} follows {hero} along the trail.",
        )
        if (Power.JUMP in powers) lines += "{hero} uses Super Jump to leap over a big log!"
        if (Power.SWIM in powers && land in listOf(Land.LAGOON, Land.FOREST, Land.MEADOW)) lines += "{hero} swims across a sparkly stream. Splash!"
        if (Power.CLIMB in powers && land == Land.PEAKS) lines += "{hero} climbs up a rocky hill. Almost at the top!"
        if (Power.LANTERN in powers && land == Land.CAVES) lines += "{hero}'s lantern lights up a dark tunnel. Ooh, sparkly!"
        if (Power.FLY in powers) lines += "{hero} flies over the treetops. Wheee!"
        return lines.random(rnd)
    }

    fun fill(text: String, p: StoryParams, f: Friend, land: Land) = text
        .replace("{f}", f.name)
        .replace("{hero}", p.heroName)
        .replace("{land}", "the ${land.title}")
}
