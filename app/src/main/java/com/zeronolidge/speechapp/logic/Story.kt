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

data class Beat(
    /** The picture for this step of the story; its text is already filled in. */
    val scene: Scene,
    /** Everything said before the challenge asks its own question. */
    val narration: String,
    val challenge: Challenge,
)

data class Quest(
    val land: Land,
    val friend: Friend,
    /** Who turns up in the twist. */
    val visitor: Friend,
    val plot: Plot,
    val intro: Scene,
    val beats: List<Beat>,
    val twist: Scene,
    /** The twist is shown just before this beat. */
    val twistBefore: Int,
    val ending: Scene,
) {
    val goal: String get() = plot.goal
}

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
        val plot = Plots.all.random(rnd)
        val visitor = plot.visitor?.let { sp -> Friends.all.firstOrNull { it.species == sp } }
            ?: Friends.all.filter { it.species != friend.species }.random(rnd)
        val subjects = when {
            mixed || land == Land.CLOUDS -> mixedSubjects(p, rnd)
            land == Land.FOREST -> List(4) { Subject.SPEECH }
            land == Land.FACTORY -> List(3) { Subject.ROBOT }
            // Every land also squeezes in a little speech practice.
            else -> (List(3) { land.subjects.first() } + Subject.SPEECH).shuffled(rnd)
        }
        val scenes = obstaclesFor(plot, subjects.size)
        val used = mutableSetOf<String>()
        val counters = Counters(p)
        fun fillScene(sc: Scene) = sc.copy(text = fill(sc.text, p, friend, land, visitor))
        val beats = subjects.mapIndexed { i, subject ->
            val c = challenge(subject, land, plot.goal, p, counters, used, rnd)
            val scene = fillScene(scenes[i])
            val power = powerLine(scene.piece, p)?.let { fill(it, p, friend, land, visitor) }
            Beat(
                scene = scene,
                narration = listOfNotNull(scene.text, power, fill(hook(c, rnd), p, friend, land, visitor)).joinToString(" "),
                challenge = c,
            )
        }
        return Quest(
            land = land,
            friend = friend,
            visitor = visitor,
            plot = plot,
            intro = fillScene(plot.intro),
            beats = beats,
            twist = fillScene(plot.twist),
            twistBefore = (beats.size - 1).coerceAtLeast(1),
            ending = fillScene(plot.ending),
        )
    }

    /** The plot's obstacles, always keeping its big finish last. */
    fun obstaclesFor(plot: Plot, count: Int): List<Scene> {
        val obs = plot.obstacles
        if (count <= 1) return listOf(obs.last())
        return when {
            count == obs.size -> obs
            count < obs.size -> obs.take(count - 1) + obs.last()
            else -> obs.dropLast(1) + List(count - obs.size) { Plots.extraObstacle } + obs.last()
        }
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
        goalEmoji: String,
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
                Challenge.Robot(robotLevel(n, rnd), goalEmoji)
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

    /** Ties a challenge into the story, after the scene has set things up. */
    private fun hook(c: Challenge, rnd: Random): String = when (c) {
        is Challenge.Say -> listOf(
            "A magic word will help us. Say it loud and clear!",
            "{f} says: let's use the magic word!",
            "Say the magic word, and it will work!",
        ).random(rnd)
        is Challenge.Count -> "We found some ${Things.nameOf(c.round.emoji)} that can help. Let's count them!"
        is Challenge.Math -> if (c.round.plus) {
            "We need ${Things.nameOf(c.round.emoji)}. We have some, and we find more!"
        } else {
            "Uh oh, some of our ${Things.nameOf(c.round.emoji)} rolled away!"
        }
        is Challenge.More -> "There are two piles of ${Things.nameOf(c.round.emoji)}. We'll take the bigger pile!"
        is Challenge.Letter -> "Magic letters will help. Let's solve the letter puzzle!"
        is Challenge.SoundOut -> "There's a word written here. Let's sound it out!"
        is Challenge.WhichWord -> "Three signs point the way. Only one has the right word!"
        is Challenge.Pattern -> "Magic stones light up in a pattern. Finish the pattern to get through!"
        is Challenge.OddOne -> "Something here doesn't belong. Find it!"
        is Challenge.Robot -> "A little robot helper can get us there. Let's give it directions!"
    }

    /** Story powers join in when the scene suits them. */
    private fun powerLine(piece: SetPiece, p: StoryParams): String? {
        val powers = Leveling.powersAt(p.heroLevel)
        return when {
            piece == SetPiece.RIVER && Power.SWIM in powers -> "Good thing {hero} can swim! But {f} can't."
            piece == SetPiece.BROKEN_BRIDGE && Power.JUMP in powers -> "{hero} could jump across, but {f} needs the bridge!"
            (piece == SetPiece.TALL_TREE || piece == SetPiece.BIG_HILL) && Power.CLIMB in powers -> "{hero} is a great climber!"
            piece == SetPiece.DARK_CAVE && Power.LANTERN in powers -> "{hero}'s lantern lights up the cave. Ooh, sparkly!"
            (piece == SetPiece.NIGHT_SKY || piece == SetPiece.WINDY_SKY) && Power.FLY in powers -> "{hero} flies up high to look around!"
            else -> null
        }
    }

    fun fill(text: String, p: StoryParams, f: Friend, land: Land, visitor: Friend? = null) = text
        .replace("{f}", f.name)
        .replace("{hero}", p.heroName)
        .replace("{land}", "the ${land.title}")
        .replace("{v}", (visitor?.species?.label ?: "friend").lowercase())
}
