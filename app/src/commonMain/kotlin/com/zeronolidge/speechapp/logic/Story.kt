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

    /**
     * Say a word out loud. [sound] is the speech sound it practises (null for story words and the
     * grown-up's own words). For a story word, [doing] is what saying it makes happen.
     */
    data class Say(val sound: SpeechSound?, val word: Word, val kind: SayKind = SayKind.PRACTICE, val doing: String? = null) : Challenge {
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
    data class Coins(val round: CoinRound) : Challenge {
        override val subject get() = Subject.COINS
    }
}

/** Where a speaking word comes from. */
enum class SayKind {
    /** A word from the speech sounds, matched to how hard words are right now. */
    PRACTICE,
    /** A word that makes the scene happen, like "open" for a door. */
    STORY,
    /** A word practised on an earlier day, brought back. */
    REVIEW,
    /** One of the grown-up's own words, like a family name. */
    OWN,
}

/** A cartoon friend who needs help in the story. */
data class Friend(val name: String, val species: Species, val color: Long = species.defaultColor) {
    /** What the child calls out, e.g. "Penny" for Penny Penguin. */
    val callName: String get() = name.substringBefore(' ')
}

object Friends {
    val all = listOf(
        Friend("Benny Bunny", Species.BUNNY),
        Friend("Penny Penguin", Species.PENGUIN),
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
    /** How many chapters have been finished; later chapters are a little harder. */
    val chapter: Int = 0,
    /** Speech sounds the grown-up wants practised. Empty means all of them. */
    val focusSounds: Set<String> = emptySet(),
    /** How many robot puzzles have been solved so far. */
    val robotSolved: Int = 0,
    val patternLevel: Int = 0,
    val numbersSolved: Int = 0,
    val lettersSolved: Int = 0,
    /** 0 = one-syllable words, 1 = two syllables, 2 = short phrases. */
    val speechStep: Int = 0,
    /** Today's day number, for bringing words back on later days. */
    val today: Int = 0,
    val words: WordBook = WordBook(),
    /** The grown-up's own words (text to emoji). */
    val customWords: List<Pair<String, String>> = emptyList(),
    /** Lands the trail has reached, for mixed adventures. */
    val reachedLands: List<Land> = listOf(Land.FOREST, Land.MEADOW),
)

object StoryGen {
    private val props = mapOf(
        Land.FOREST to listOf("🍄", "🌰", "🍓", "🐞"),
        Land.MEADOW to listOf("🌸", "🐝", "🦋", "🍎", "🌼", "🥕"),
        Land.LAGOON to listOf("🐚", "🦀", "🐟", "🐠", "🍌"),
        Land.MARKET to Coins.goods,
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
            land == Land.FACTORY -> List(3) { Subject.ROBOT } + Subject.SPEECH
            // Every land also squeezes in a little speech practice.
            else -> (List(3) { land.subjects.first() } + Subject.SPEECH).shuffled(rnd)
        }
        val scenes = obstaclesFor(plot, subjects.size)
        val used = mutableSetOf<String>()
        val counters = Counters(p)
        val sayKinds = sayKinds(p, if (land == Land.FOREST) 4 else 1)
        fun fillScene(sc: Scene) = sc.copy(text = fill(sc.text, p, friend, land, visitor))
        val beats = subjects.mapIndexed { i, subject ->
            val kind = if (subject == Subject.SPEECH) sayKinds.removeFirstOrNull() ?: SayKind.PRACTICE else SayKind.PRACTICE
            val c = challenge(subject, land, plot.goal, p, counters, used, rnd, scenes[i].piece, kind)
            val scene = fillScene(scenes[i])
            val power = powerLine(scene.piece, p)?.let { fill(it, p, friend, land, visitor) }
            Beat(
                scene = scene,
                narration = listOfNotNull(scene.text, power, fill(hook(c, land, rnd), p, friend, land, visitor)).joinToString(" "),
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

    /**
     * Which kinds of speaking words this chapter uses, in order. Story words come first because
     * they make the story happen; words from earlier days and the grown-up's own words come back
     * regularly. [count] is how many speaking moments the chapter has.
     */
    fun sayKinds(p: StoryParams, count: Int): ArrayDeque<SayKind> {
        // With the grown-up's own words or target sounds set, most speaking turns use them:
        // OWN for his own words, PRACTICE for words with his target sounds.
        if (p.customWords.isNotEmpty() || p.focusSounds.isNotEmpty()) {
            val own = if (p.customWords.isNotEmpty()) SayKind.OWN else SayKind.PRACTICE
            val review = p.words.dueForReview(p.today, 1).isNotEmpty()
            val all = buildList {
                add(SayKind.STORY)
                add(own)
                add(SayKind.PRACTICE)
                add(if (review) SayKind.REVIEW else own)
                add(own)
                add(SayKind.PRACTICE)
            }
            return ArrayDeque(if (count == 1) listOf(listOf(own, SayKind.PRACTICE, own, SayKind.STORY)[p.chapter % 4]) else all.take(count))
        }
        val all = buildList {
            add(SayKind.STORY)
            if (p.words.dueForReview(p.today, 1).isNotEmpty()) add(SayKind.REVIEW)
            if (p.customWords.isNotEmpty()) add(SayKind.OWN)
            add(SayKind.PRACTICE)
            add(SayKind.PRACTICE)
            add(SayKind.PRACTICE)
        }
        // With one speaking moment, take turns between the kinds from chapter to chapter.
        return ArrayDeque(if (count == 1) listOf(all[p.chapter % (all.size - 2)]) else all.take(count))
    }

    /** A practice word for the current difficulty step, avoiding words already used. */
    fun practiceWord(p: StoryParams, used: Set<String>, rnd: Random): Pair<SpeechSound, Word> {
        val sounds = SpeechContent.sounds.filter { p.focusSounds.isEmpty() || it.id in p.focusSounds }
            .ifEmpty { SpeechContent.sounds }
        val all = sounds.flatMap { s -> s.words.map { s to it } }
        val atStep = when (p.speechStep) {
            0 -> all.filter { it.second.syllableCount == 1 }
            1 -> all.filter { it.second.syllableCount == 2 }
            else -> sounds.flatMap { s -> s.phrases.map { s to it } }
        }
        val fresh = { list: List<Pair<SpeechSound, Word>> -> list.filter { it.second.text !in used } }
        return fresh(atStep).ifEmpty { fresh(all) }.ifEmpty { all }.random(rnd)
    }

    /** A mix of subjects from the lands the hero can reach, always including speech. */
    private fun mixedSubjects(p: StoryParams, rnd: Random): List<Subject> {
        val open = p.reachedLands.filter { it != Land.CLOUDS }.flatMap { it.subjects }.distinct().filter { it != Subject.SPEECH }.shuffled(rnd)
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
        piece: SetPiece = SetPiece.FORK_PATH,
        sayKind: SayKind = SayKind.PRACTICE,
    ): Challenge {
        val item = (props[land] ?: Things.countables).random(rnd)
        return when (subject) {
            Subject.SPEECH -> {
                val review = p.words.dueForReview(p.today, 3).firstOrNull { it.text !in used }
                val own = p.customWords.filter { it.first !in used }.randomOrNull(rnd)
                val c = when {
                    sayKind == SayKind.STORY -> {
                        val sw = StoryWords.forPiece(piece)
                        Challenge.Say(null, Word(sw.at(p.speechStep), sw.emoji), SayKind.STORY, sw.doing)
                    }
                    sayKind == SayKind.REVIEW && review != null -> {
                        val sound = SpeechContent.sounds.firstOrNull { s -> (s.words + s.phrases).any { it.text == review.text } }
                        val word = sound?.let { s -> (s.words + s.phrases).first { it.text == review.text } } ?: Word(review.text, review.emoji)
                        Challenge.Say(sound, word, SayKind.REVIEW)
                    }
                    sayKind == SayKind.OWN && own != null -> Challenge.Say(null, Word(own.first, own.second), SayKind.OWN)
                    else -> {
                        val (sound, word) = practiceWord(p, used, rnd)
                        Challenge.Say(sound, word, SayKind.PRACTICE)
                    }
                }
                used += c.word.text
                c
            }
            Subject.NUMBERS -> {
                val max = if (p.numbersSolved > 15) 10 else 5
                when (rnd.nextInt(if (p.chapter >= 4) 4 else 3)) {
                    0 -> Challenge.Count(Numbers.count(if (max > 5) 10 else 6, rnd, item))
                    1 -> Challenge.Math(Numbers.math(true, max, rnd, item))
                    2 -> Challenge.More(Numbers.compare(if (max > 5) 8 else 6, rnd, item))
                    else -> Challenge.Math(Numbers.math(false, max, rnd, item))
                }
            }
            Subject.LETTERS -> {
                val mode = LetterMode.entries.random(rnd)
                // Letter puzzles mostly use the letters of his target sounds and own words.
                val mine = Letters.targetsFor(p.focusSounds, p.customWords.map { it.first })
                    .filter { "L" + it.lower !in used }
                val target = Letters.pickTarget(mine, rnd)
                var round = Letters.make(mode, rnd, tricky = p.lettersSolved > 15, target = target)
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
            Subject.COINS -> Challenge.Coins(Coins.make(p.chapter, rnd, if (land == Land.MARKET) item else Coins.goods.random(rnd)))
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
    private fun hook(c: Challenge, land: Land, rnd: Random): String = when (c) {
        is Challenge.Say -> when (c.kind) {
            SayKind.STORY -> "We can ${c.doing} with a magic word!"
            SayKind.REVIEW -> "Remember this word? Saying it will help us!"
            SayKind.OWN -> "Here's a special word, just for {hero}!"
            SayKind.PRACTICE -> listOf(
                "A magic word will help us. Say it loud and clear!",
                "{f} says: let's use the magic word!",
                "Say the magic word, and it will work!",
            ).random(rnd)
        }
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
        is Challenge.Pattern ->
            if (land == Land.PEAKS) "A rope bridge crosses the valley. Its planks go in a pattern, but one is missing! Let's fix it."
            else "Magic stones light up in a pattern. Finish the pattern to get through!"
        is Challenge.OddOne -> "Something here doesn't belong. Find it!"
        is Challenge.Robot -> "A little robot helper can get us there. Let's give it directions!"
        is Challenge.Coins -> when (c.round.mode) {
            CoinMode.PAY -> "Look, a market stall! We can get ${Things.nameOf(c.round.item)} to help. Let's pay with coins!"
            CoinMode.PURSE -> "The stall keeper needs the right coins. Which purse has just enough?"
        }
    }

    /** Story powers join in when the scene suits them. */
    private fun powerLine(piece: SetPiece, p: StoryParams): String? {
        val powers = Trail.powers(p.chapter)
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
