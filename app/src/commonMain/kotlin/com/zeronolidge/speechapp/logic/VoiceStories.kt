package com.zeronolidge.speechapp.logic

/**
 * Voice-driven stories: his word makes the plot happen (no "magic word" password). A story is
 * data: a list of beats, each a backdrop, a prop, a move, and the words. The art and the moves
 * are shared, so a new story is mostly words (and maybe a new prop), not new code.
 *
 * The pattern, for every voice story:
 *  1. Setup: a character with one simple want.
 *  2. 6 or 7 beats; in each an obstacle appears and his word fixes it. Rory names the problem,
 *     says the word three times (slower the third time), the listening look shows, and his voice
 *     pushes the prop through its [Move] live (louder = further, holding keeps it moving; see
 *     [VoicePush]). Rory then says the success line ("Go! Go, boat! The boat glides out..."). The
 *     "I said it!" button plays the same move at medium strength.
 *  3. His target sounds start the words of at least 4 beats (first stories); later stories put
 *     them at the ends of words too ("up", "stop"). Each beat names its [VoiceBeat.sound].
 *  4. One prop can be one of his own words ([PropSwap]).
 *  5. Each beat has three versions of its word: one word, two words, a short sentence; the quiet
 *     speech step picks one.
 *  6. A calm goodbye beat, then a sticker. 3 to 4 minutes, at most [VoiceStories.MAX_BEATS] beats.
 * Lines are full fixed sentences (no blanks), so the pre-made voice clips sound natural.
 * Loudness only (no speech recognition).
 */
data class VoiceBeat(
    /** Where it happens: the same backdrop can serve any story. */
    val backdrop: Backdrop,
    /** Rory names the problem. */
    val problem: String,
    /** The word to say: one word, two words, a short sentence. */
    val lines: List<String>,
    /** The speech sound it practises (one of his target sounds), or null. */
    val sound: String?,
    /** What his voice moves, and how. */
    val prop: Prop,
    val move: Move,
    /** Rory says it back, adds one more, and says what happened ("Go! Go, boat! The boat glides out onto the river!"). */
    val success: String,
    /** The picture for the word chip and his sticker. */
    val emoji: String,
    /** How the light changes as the move happens (the sun comes up, night falls). */
    val light: Light = Light.SAME,
    /** The calm last beat. */
    val goodbye: Boolean = false,
    /** A prop that stays still in the scene, for company (Bear in his basket while a cupcake drops). */
    val still: Prop? = null,
    /** The prop shown as one of his own words instead (an emoji), when [PropSwap] applies. */
    val propEmoji: String? = null,
    /**
     * True for a beat that shows a balloon colour-pattern puzzle (see [BalloonGame]) before its
     * usual talk turns: NAME (this beat's [problem]) -> PUZZLE -> TALK_COLOUR -> MODEL_POP ->
     * TALK_POP -> CLOSE -> DONE (see [GameBeatState]). The puzzle itself (colours, pattern,
     * choices) is generated fresh each time it's played, from the quiet [Ladder.GAME_PATTERN]
     * step, not stored here; this beat's own [lines]/[sound] are a "blue"-flavoured fallback
     * (already a recorded clip) for whichever colour the puzzle lands on. At most one per story.
     */
    val game: Boolean = false,
    /** Once the puzzle above is solved, the picked colour (see [BalloonGame]): read only by
     * [Prop.GAP_BALLOON], kept apart from [propEmoji] so it never short-circuits a prop's art. */
    val gapColor: String? = null,
    /** Non-null for a beat in Rory's Kitchen that plays one of [KitchenFlow]'s tap/talk flows
     * before the usual fix (see [KitchenBeatState]), in place of [game]'s balloon-only puzzle. */
    val kitchen: KitchenFlow? = null,
    /** [KitchenFlow.TAP_CHOICE]'s distractor items (the beat's own word is always the answer);
     * empty for every other flow. */
    val kitchenChoices: List<String> = emptyList(),
    /** How many taps or talk turns [kitchen]'s flow needs (see [KitchenFlow.tapGoal] /
     * [KitchenFlow.talkGoal]); unused when [kitchen] is null. */
    val kitchenGoal: Int = 1,
) {
    val word: String get() = lines[0]

    /** The line for this speech step: 0 one word, 1 two words, 2 a short sentence. */
    fun line(step: Int): String = lines[step.coerceIn(0, lines.lastIndex)]
}

/**
 * The reusable voice-driven moves. His voice drives each one: how far (loudness and how long he
 * keeps it up) and how big the flourish (his loudest moment). A prop can use any move.
 */
enum class Move {
    /** Up: the sun rises, the balloon lifts, the boat rides over a wave. */
    RISE,
    /** Down: a cupcake drops, berries tumble, the balloon floats down. */
    SINK,
    /** Along: the boat glides, the duck paddles over, the bird moves aside. */
    SLIDE,
    /** Swells, then bursts apart in paper layers: bubbles. */
    POP,
    /** Gets bigger: the balloon puffs up, the sail fills, Owl warms up. */
    GROW,
    /** Wobbles and wakes: Owl wakes up, waving goodbye, leaves rustle. */
    SHAKE,
}

/** The props, each a piece of paper art any move can carry. */
enum class Prop { BOAT, SAIL, BUBBLE, DUCK, SUN, OWL, SLEEPY_OWL, LEAVES, BERRIES, BALLOON, BIRD, CUPCAKE, MOON, GAP_BALLOON }

/** The backdrops: a land's paper scenery plus a little extra (water, an island, a path, the sky). */
enum class Backdrop(val land: Land) {
    RIVER(Land.LAGOON), ISLAND(Land.LAGOON), FOREST(Land.FOREST), PATH(Land.FOREST), MEADOW(Land.MEADOW), SKY(Land.MEADOW),
}

/** How a beat's light changes with the move. */
enum class Light { SAME, DAWN, DUSK }

/** The prop his own word can stand in for: its [name] in the story's lines, and its kind. */
data class PropSwap(val name: String, val prop: Prop, val kind: PropKind)

enum class PropKind { CHARACTER, THING }

data class VoiceStory(
    val id: String,
    val title: String,
    val emoji: String,
    /** The land its map stop sits by. */
    val land: Land,
    /** The speech sounds it practises. */
    val sounds: List<String>,
    val setup: String,
    val beats: List<VoiceBeat>,
    /** Which prop his own word can replace (null: none). */
    val swap: PropSwap?,
    /** The sticker he gets at the end (word, emoji). */
    val sticker: Pair<String, String>,
    val ending: String,
)

object VoiceStories {
    const val MAX_BEATS = 8

    val boat = VoiceStory(
        id = "boat", title = "Pip's Boat Trip", emoji = "⛵", land = Land.LAGOON, sounds = listOf("p", "b", "g"),
        setup = "Pip has a little paper boat, and Pip wants to go on a trip down the river! Your words make the boat go.",
        beats = listOf(
            VoiceBeat(Backdrop.RIVER, "The boat is stuck at the dock.", listOf("go", "go boat", "Go, boat, go!"), "g", Prop.BOAT, Move.SLIDE, "Go! Go, boat! The boat glides out onto the river!", "⛵"),
            VoiceBeat(Backdrop.RIVER, "Oh! A big bubble is blocking the river.", listOf("pop", "pop bubble", "Pop the bubble!"), "p", Prop.BUBBLE, Move.POP, "Pop! Pop, bubble! Pop! Confetti everywhere!", "🎉", still = Prop.BOAT),
            VoiceBeat(Backdrop.RIVER, "The sail is all droopy.", listOf("blow", "blow wind", "Blow, wind, blow!"), "b", Prop.SAIL, Move.GROW, "Blow! Blow, wind! The sail puffs up, and the boat speeds along!", "🌬️"),
            VoiceBeat(Backdrop.RIVER, "Oh no! The duck is stuck on a rock.", listOf("come", "come duck", "Come here, duck!"), null, Prop.DUCK, Move.SLIDE, "Come! Come, duck! The duck paddles over to the boat!", "🦆", still = Prop.BOAT),
            VoiceBeat(Backdrop.RIVER, "Uh oh, a big wave is coming!", listOf("up", "go up", "Up, up, boat!"), "p", Prop.BOAT, Move.RISE, "Up! Up we go! The boat rides right over the wave!", "🌊"),
            VoiceBeat(Backdrop.ISLAND, "Look, a little island!", listOf("stop", "stop boat", "Stop the boat!"), "p", Prop.BOAT, Move.SLIDE, "Stop! Stop, boat! Bump! The boat stops on the soft sand.", "🏝️"),
            VoiceBeat(Backdrop.RIVER, "Everyone is sleepy after the trip.", listOf("bye", "bye duck", "Bye-bye, duck!"), "b", Prop.DUCK, Move.SHAKE, "Bye! Bye-bye, duck! The duck waves goodbye, and Pip falls fast asleep.", "👋", light = Light.DUSK, goodbye = true),
        ),
        swap = PropSwap("duck", Prop.DUCK, PropKind.CHARACTER), sticker = "boat" to "⛵",
        ending = "What a trip! You got a boat sticker. See you next time!",
    )

    val owl = VoiceStory(
        id = "owl", title = "Sleepy Owl's Morning", emoji = "🦉", land = Land.FOREST, sounds = listOf("s", "w", "m"),
        setup = "It's morning in the forest, but it's still dark, and Owl is fast asleep. Your words wake up the morning!",
        beats = listOf(
            VoiceBeat(Backdrop.FOREST, "The sky is still dark.", listOf("sun", "sun up", "Wake up, sun!"), "s", Prop.SUN, Move.RISE, "Sun! Hello, sun! The sun peeks over the hill!", "☀️", light = Light.DAWN),
            VoiceBeat(Backdrop.FOREST, "Owl is snoring. Zzz.", listOf("wake", "wake owl", "Wake up, owl!"), "w", Prop.SLEEPY_OWL, Move.SHAKE, "Wake! Wake up, owl! Owl's eyes pop open!", "🦉"),
            VoiceBeat(Backdrop.FOREST, "Brr! Owl is too cold to fly.", listOf("warm", "warm owl", "Get warm, owl!"), "w", Prop.OWL, Move.GROW, "Warm! Warm owl! Sunbeams wrap Owl up, all cozy and warm.", "🌞"),
            VoiceBeat(Backdrop.PATH, "Leaves are blocking the path.", listOf("sss", "sss leaves", "Sss, move, leaves!"), "s", Prop.LEAVES, Move.SLIDE, "Sss! Sss, leaves! Whoosh! The leaves blow aside.", "🍂", still = Prop.OWL),
            VoiceBeat(Backdrop.FOREST, "Owl's tummy rumbles. Owl is hungry!", listOf("more", "more berries", "More berries, please!"), "m", Prop.BERRIES, Move.SINK, "More! More berries! Here come the berries! Yum!", "🍒", still = Prop.OWL),
            VoiceBeat(Backdrop.FOREST, "Owl is ready to fly!", listOf("fly", "fly owl", "Fly up high, owl!"), null, Prop.OWL, Move.RISE, "Fly! Fly, owl! Owl swoops around in a big loop!", "🦉"),
            VoiceBeat(Backdrop.FOREST, "Owl says thank you.", listOf("bye", "bye owl", "Bye, see you, owl!"), null, Prop.OWL, Move.SHAKE, "Bye! Bye-bye, owl! Owl drops a feather for you, and flies home.", "👋", goodbye = true),
        ),
        swap = PropSwap("berries", Prop.BERRIES, PropKind.THING), sticker = "owl" to "🦉",
        ending = "What a morning! You got an owl sticker. See you next time!",
    )

    val balloon = VoiceStory(
        id = "balloon", title = "The Balloon Picnic", emoji = "🎈", land = Land.MEADOW, sounds = listOf("b", "p", "m"),
        setup = "Bear wants to have a picnic, up in the sky! Your words make the balloon fly.",
        beats = listOf(
            VoiceBeat(Backdrop.MEADOW, "The balloon is all flat.", listOf("blow", "big balloon", "Blow it big!"), "b", Prop.BALLOON, Move.GROW, "Blow! Big balloon! The balloon puffs up, bigger and bigger!", "🎈"),
            VoiceBeat(Backdrop.MEADOW, "Bear wants a ride.", listOf("up", "go up", "Up we go!"), "p", Prop.BALLOON, Move.RISE, "Up! Up we go! The balloon lifts Bear up into the sky!", "🐻"),
            VoiceBeat(Backdrop.SKY, "Oh! The bird is in the way.", listOf("move", "move bird", "Move, please, bird!"), "m", Prop.BIRD, Move.SLIDE, "Move! Move, bird! The bird moves out of the way!", "🐦", still = Prop.BALLOON),
            VoiceBeat(Backdrop.SKY, "Look! Cupcakes, far down below.", listOf("down", "go down", "Down to the picnic!"), null, Prop.BALLOON, Move.SINK, "Down! Down we go! The balloon floats gently down to the picnic.", "🧁"),
            VoiceBeat(Backdrop.MEADOW, "Bear wants a treat.", listOf("more", "more cake", "More cake, please!"), "m", Prop.CUPCAKE, Move.SINK, "More! More cake! A cupcake drops right into Bear's paw!", "🧁", still = Prop.BALLOON),
            VoiceBeat(Backdrop.MEADOW, "Bear tied some balloons in a row, but one is missing!", listOf("blue", "blue balloon", "Say blue!"), "b", Prop.BALLOON, Move.SLIDE, "The balloon settles into place, and the row is complete!", "🎈", game = true),
            VoiceBeat(Backdrop.MEADOW, "Bubbles are floating past!", listOf("pop", "pop pop", "Pop the bubbles!"), "p", Prop.BUBBLE, Move.POP, "Pop! Pop, pop! Pop, pop, pop! They burst one by one.", "🎉"),
            VoiceBeat(Backdrop.MEADOW, "The sun is going down. Time to rest.", listOf("night", "night Bear", "Good night, Bear!"), null, Prop.MOON, Move.RISE, "Night! Good night, Bear! The moon rises, and Bear gives a big yawn.", "🌙", light = Light.DUSK, goodbye = true, still = Prop.BALLOON),
        ),
        swap = PropSwap("bird", Prop.BIRD, PropKind.CHARACTER), sticker = "balloon" to "🎈",
        ending = "What a picnic! You got a balloon sticker. See you next time!",
    )

    /**
     * Rory's Kitchen: pancakes for Bear's picnic. Five beats reuse a tap/talk flow (see
     * [KitchenFlow], [KitchenBeatState]) instead of the usual single talk turn: tap the bowl
     * (among distractors), stir three times, count three berries in (no talk, a counting turn),
     * pop three bubbles, tap the pancake then say "flip!". The other three beats (milk, pour,
     * his taste of it) are the usual one talk-turn beat. Placed by Market Town (food, not the
     * already-busy Meadow) though the scene itself is Rory's kitchen counter; backdrop/prop/move
     * below are placeholders reusing existing art (see [Backdrop], [Prop], [Move]) until the
     * kitchen counter scene is drawn (art round: step B).
     */
    val kitchen = VoiceStory(
        id = "kitchen", title = "Rory's Kitchen", emoji = "🥞", land = Land.MARKET, sounds = listOf("p", "b", "m"),
        setup = "Bear's hungry! Let's make pancakes!",
        beats = listOf(
            VoiceBeat(
                Backdrop.MEADOW, "We need a bowl!", listOf("bowl", "big bowl", "Get the bowl!"), "b", Prop.CUPCAKE, Move.SLIDE,
                "Bowl! The bowl slides onto the counter!", "🥣", kitchen = KitchenFlow.TAP_CHOICE, kitchenChoices = KitchenGame.distractors,
            ),
            VoiceBeat(Backdrop.MEADOW, "The bowl is empty. Time for milk!", listOf("milk", "pour milk", "Pour in the milk!"), "m", Prop.CUPCAKE, Move.GROW, "Milk! The milk pours into the bowl!", "🥛"),
            VoiceBeat(
                Backdrop.MEADOW, "Now we stir it all up!", listOf("mix", "mix it", "Mix it all up!"), "m", Prop.CUPCAKE, Move.SHAKE,
                "Mix! The batter is all ready!", "🥣", kitchen = KitchenFlow.STIR, kitchenGoal = 3,
            ),
            VoiceBeat(
                Backdrop.MEADOW, "Let's add some berries! Help me count them in.", listOf("three", "three berries", "Three berries, please!"), null, Prop.BERRIES, Move.SINK,
                "Three! The berries tumble into the batter!", "🍓", kitchen = KitchenFlow.COUNT, kitchenGoal = 3,
            ),
            VoiceBeat(Backdrop.MEADOW, "The batter's ready for the pan!", listOf("pour", "pour batter", "Pour in the batter!"), "p", Prop.CUPCAKE, Move.SINK, "Pour! The batter pours into the pan!", "🥞"),
            VoiceBeat(
                Backdrop.MEADOW, "Look! Bubbles are popping on the pancake!", listOf("pop", "pop pop", "Pop it!"), "p", Prop.BUBBLE, Move.POP,
                "Pop! The pancake is bubbling and ready to flip!", "🎉", kitchen = KitchenFlow.BUBBLES, kitchenGoal = 3,
            ),
            VoiceBeat(
                Backdrop.MEADOW, "Tap the pancake, then flip it!", listOf("flip", "flip it", "Flip the pancake!"), null, Prop.CUPCAKE, Move.RISE,
                "Flip! The pancake flips up and lands with a soft plop!", "🥞", kitchen = KitchenFlow.FLIP,
            ),
            VoiceBeat(Backdrop.MEADOW, "Bear takes a big bite!", listOf("mmm", "mmm good", "Mmm, so good!"), "m", Prop.CUPCAKE, Move.SHAKE, "Mmm! Bear says thank you, and gives a happy wave!", "😋", goodbye = true),
        ),
        swap = null, sticker = "pancake" to "🥞",
        ending = "What a treat! You got a pancake sticker. See you next time!",
    )

    val all: List<VoiceStory> = listOf(boat, owl, balloon, kitchen)

    fun byId(id: String): VoiceStory? = all.firstOrNull { it.id == id }

    /**
     * The story ready to play: where one fits, one of his own words stands in for the story's swap
     * prop (a name, written with a capital, for a character; any other word for a thing). Its name
     * is swapped into the fixed sentences ("the duck" becomes "Grandma"), and the prop shows his
     * word's picture.
     */
    fun ready(story: VoiceStory, own: List<Pair<String, String>>): VoiceStory {
        val swap = story.swap ?: return story
        val mine = own.map { it.first.trim() to it.second }.filter { it.first.isNotEmpty() }.firstOrNull { (text, _) ->
            val name = text.first().isUpperCase()
            if (swap.kind == PropKind.CHARACTER) name else !name
        } ?: return story
        val (text, emoji) = mine
        val isName = text.first().isUpperCase()
        val word = Regex("\\b${Regex.escape(swap.name)}\\b", RegexOption.IGNORE_CASE)
        val theWord = Regex("\\b([Tt])he ${Regex.escape(swap.name)}\\b")
        fun fill(s: String): String {
            val named = if (isName) theWord.replace(s) { text } else s
            return word.replace(named) { m -> if (m.value.first().isUpperCase() && !isName) text.replaceFirstChar { it.uppercase() } else text }
        }
        return story.copy(
            setup = fill(story.setup),
            beats = story.beats.map { b ->
                b.copy(
                    problem = fill(b.problem), lines = b.lines.map(::fill), success = fill(b.success),
                    emoji = if (b.prop == swap.prop) emoji else b.emoji,
                    propEmoji = if (b.prop == swap.prop) emoji else b.propEmoji,
                )
            },
        )
    }

    /** True when [word] starts with the speech sound [sound] ("p", "sh", ...). */
    fun startsWith(word: String, sound: String): Boolean = word.trim().lowercase().startsWith(sound.lowercase())

    /** True when [word] ends with the speech sound [sound] ("up", "stop"). */
    fun endsWith(word: String, sound: String): Boolean = word.trim().lowercase().trimEnd('!', '.').endsWith(sound.lowercase())

    /** The stories, best first for his target sounds (most beats practising one of them). */
    fun forSounds(focus: Set<String>): List<VoiceStory> =
        if (focus.isEmpty()) all
        else all.sortedByDescending { s -> s.beats.count { it.sound in focus } }
}
