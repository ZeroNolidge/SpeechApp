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
    /** A short plain-language cue shown on screen for the whole beat (K2l), at most 4 words, e.g.
     * "Tap the bowl", "Say mix!": he can't read yet, so this is a steady visual anchor beside
     * Rory's spoken line, not a substitute for it. Null for every beat but Rory's Kitchen's own
     * (every other story relies on the word chip instead; see VoiceStoryScreen). */
    val caption: String? = null,
    /** [KitchenFlow.ASK] only (K6): what Rory says instead of the generic "I heard you!" if he's
     * still quiet after the usual re-try -- a plain reveal said OUT LOUD, pointing at what he's
     * asked about, that still counts as the try (never a third ask): "One, two... three!" for "how
     * many berries?", "One!" / "One... two!" for the butter steps. Null for every other flow. */
    val kitchenFallback: String? = null,
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

/** The props, each a piece of paper art any move can carry. [BOWL]/[MILK]/[BATTER]/[PAN]/
 * [PANCAKE]/[STEAM]/[BUTTER] are Rory's Kitchen's own (see [Backdrop.KITCHEN]); the bubbles and
 * berries beats there reuse [BUBBLE] and [BERRIES]. [BUTTER] (K6) is the butter-cutting
 * mini-sequence's own plate and pat, drawn whole or already cut in two (see `PropArt` in
 * VoiceScenes.kt). [BANANA]/[FRUIT_BOWL] are Rory's Fruit Salad's own, sharing the same kitchen
 * counter and bowl shape: [BANANA] is the banana on its plate, whole until swiped then shown as
 * round slices; [FRUIT_BOWL] is the mixing bowl itself, with the slices, [BERRIES] and a stirring
 * fork added one at a time as the story's own steps finish (see `PropArt`). */
enum class Prop {
    BOAT, SAIL, BUBBLE, DUCK, SUN, OWL, SLEEPY_OWL, LEAVES, BERRIES, BALLOON, BIRD, CUPCAKE, MOON, GAP_BALLOON,
    BOWL, MILK, BATTER, PAN, PANCAKE, STEAM, BUTTER, BANANA, FRUIT_BOWL,
}

/** The backdrops: a land's paper scenery plus a little extra (water, an island, a path, the sky,
 * Rory's kitchen counter). */
enum class Backdrop(val land: Land) {
    RIVER(Land.LAGOON), ISLAND(Land.LAGOON), FOREST(Land.FOREST), PATH(Land.FOREST), MEADOW(Land.MEADOW), SKY(Land.MEADOW),
    KITCHEN(Land.MARKET),
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

    /** Rory's Kitchen alone (K6): it was already the odd one out, breaking the uniform "3 to 4
     * minutes" pattern with tap/talk flows and its own ending, so it gets its own, higher ceiling
     * for the "how many?" asks and the butter-cutting mini-sequence rather than forcing every
     * other voice story's [MAX_BEATS] up to make room for one story's extra steps. */
    const val KITCHEN_MAX_BEATS = 13

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
     * Rory's Kitchen: pancakes for Bear's picnic. Several beats reuse a tap/talk flow (see
     * [KitchenFlow], [KitchenBeatState]) instead of the usual single talk turn, and every one is
     * now either tap-only or talk-only, never both (K3a: Timothy's iPad notes -- a tap step that
     * then also asked him to talk read as the tiles vanishing into an unexplained listening
     * screen): tap the bowl (among distractors), tap three berries in, tap the pancake to flip it
     * (one tap, then straight to the fix, no talk turn) and the butter-cutting step's own swipe
     * (K6, [KitchenFlow.CUT]) -- stir three times and pop three bubbles (no tap at all, several
     * talk tries in a row) -- and three plain "how many?" asks (K6, [KitchenFlow.ASK]: he answers
     * without Rory saying the number first, see [KitchenFlow]'s own doc). The other three beats
     * (milk, pour, his taste of it) are the usual one talk-turn beat. Placed by Market Town (food,
     * not the already-busy Meadow) though the scene itself is Rory's kitchen counter
     * ([Backdrop.KITCHEN]). Every beat carries its own [VoiceBeat.caption] (K2l): he can't read
     * yet, so it's a steady on-screen cue ("Tap the bowl", "Say mix!") beside Rory's spoken line,
     * not instead of it -- every step's instruction, and on a talk step the cue to speak, is
     * always said out loud too (K3a), never left to the caption alone.
     */
    val kitchen = VoiceStory(
        id = "kitchen", title = "Rory's Kitchen", emoji = "🥞", land = Land.MARKET, sounds = listOf("p", "b", "m"),
        setup = "Bear's hungry! Let's make pancakes!",
        beats = listOf(
            VoiceBeat(
                Backdrop.KITCHEN, "We need a bowl! Tap the bowl!", listOf("bowl", "big bowl", "Get the bowl!"), "b", Prop.BOWL, Move.SLIDE,
                "Bowl! The bowl slides onto the counter!", "🥣", kitchen = KitchenFlow.TAP_CHOICE, kitchenChoices = KitchenGame.distractors,
                caption = "Tap the bowl",
            ),
            VoiceBeat(Backdrop.KITCHEN, "The bowl is empty. Time for milk!", listOf("milk", "pour milk", "Pour in the milk!"), "m", Prop.MILK, Move.SINK, "Milk! The milk pours into the bowl!", "🥛", still = Prop.BOWL, caption = "Say milk!"),
            VoiceBeat(
                Backdrop.KITCHEN, "Now we stir it all up!", listOf("mix", "mix it", "Mix it all up!"), "m", Prop.BATTER, Move.SHAKE,
                "Mix! The batter is all ready!", "🥣", kitchen = KitchenFlow.STIR, kitchenGoal = 1,
                caption = "Say mix!",
            ),
            // K6: Timothy's notes split the old single "count the berries" tap-then-count beat in
            // two -- tap them in (no talk turn, same as before), then a SEPARATE talk-only beat
            // asks how many there were, so a step is always tap OR talk, never both. The berries
            // are drawn right on the counter beside the bowl, not as choice tiles.
            VoiceBeat(
                Backdrop.KITCHEN, "Let's add some berries! Tap them in!", listOf("three", "three berries", "Three berries, please!"), null, Prop.BERRIES, Move.SINK,
                "Three! The berries tumble into the batter!", "🫐", kitchen = KitchenFlow.COUNT, kitchenGoal = 3, still = Prop.BATTER,
                caption = "Tap the berries",
            ),
            // The scene stays exactly as the tap step left it (the batter-filled bowl, berries
            // already in): only one item renders (Prop.BATTER, no separate still), or the generic
            // single-prop berries pile would draw right on top of it, the same overlap bug as the
            // old milk-on-the-bowl one (K6).
            VoiceBeat(
                Backdrop.KITCHEN, "How many berries did we put in?", listOf("three", "three berries", "Three berries, all counted!"), null, Prop.BATTER, Move.SHAKE,
                "Three! That's three juicy berries in the batter!", "🫐", kitchen = KitchenFlow.ASK,
                caption = "How many berries?", kitchenFallback = "One, two... three!",
            ),
            VoiceBeat(Backdrop.KITCHEN, "The batter's ready for the pan!", listOf("pour", "pour batter", "Pour in the batter!"), "p", Prop.BATTER, Move.SINK, "Pour! The batter pours into the pan!", "🥞", still = Prop.PAN, caption = "Say pour!"),
            VoiceBeat(
                Backdrop.KITCHEN, "Look! Bubbles are popping on the pancake!", listOf("pop", "pop pop", "Pop it!"), "p", Prop.BUBBLE, Move.POP,
                "Pop! The pancake is bubbling and ready to flip!", "🎉", kitchen = KitchenFlow.BUBBLES, kitchenGoal = 1, still = Prop.STEAM,
                caption = "Pop the bubbles",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "Tap the pancake, then flip it!", listOf("flip", "flip it", "Flip the pancake!"), null, Prop.PANCAKE, Move.RISE,
                "Flip! The pancake flips up and lands with a soft plop!", "🥞", kitchen = KitchenFlow.FLIP, still = Prop.PAN,
                caption = "Tap to flip",
            ),
            // K6: the butter-cutting mini-sequence, right before it goes on the pancakes -- ask,
            // swipe to cut (narrated only, never a talk turn), ask again.
            VoiceBeat(
                Backdrop.KITCHEN, "We need some butter, but it's too big! How many pieces?", listOf("one", "one butter", "One pat of butter, please!"), null, Prop.BUTTER, Move.SLIDE,
                "One! Just one pat of butter!", "🧈", kitchen = KitchenFlow.ASK, caption = "How many pieces?", kitchenFallback = "One!",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "Let's cut the butter! Cut!", listOf("cut", "cut it", "Cut it in two!"), null, Prop.BUTTER, Move.SHAKE,
                "Cut! The butter splits into two pieces!", "🧈", kitchen = KitchenFlow.CUT, caption = "Cut!",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "Now how many?", listOf("two", "two pieces", "Two pieces of butter now!"), null, Prop.BUTTER, Move.SLIDE,
                "Two! Two pieces, ready to melt!", "🧈", kitchen = KitchenFlow.ASK, caption = "Now how many?", kitchenFallback = "One... two!",
            ),
            VoiceBeat(Backdrop.KITCHEN, "Bear takes a big bite!", listOf("mmm", "mmm good", "Mmm, so good!"), "m", Prop.PANCAKE, Move.SHAKE, "Mmm! Bear says thank you, and gives a happy wave!", "😋", goodbye = true, caption = "Say mmm"),
        ),
        swap = null, sticker = "pancake" to "🥞",
        ending = "What a treat! You got a pancake sticker. See you next time!",
    )

    /** Rory's Kitchen's own "your turn" cue (K3d), said once after he's modelled the word: a
     * per-word line, naming the word twice ("Milk! Now you say it... milk!"), in place of the old
     * generic "Say it out loud!" every other kitchen talk turn used -- it reads the same for every
     * word, so a child who can't read the caption had only the picture to tell him which word was
     * his turn to say. Covers the kitchen's five plain talk beats (milk, mix, pour, pop, mmm;
     * bowl/three/flip are tap-only and never reach a talk turn at all) and, since both stories
     * share the same `Backdrop.KITCHEN` talk-turn code path, Rory's Fruit Salad's three too (four,
     * fill, fork). Falls back to the old line for any word that isn't one of these (kept only so a
     * future kitchen-style word never goes silent waiting on a new clip). */
    fun kitchenYourTurnLine(word: String): String = when (word) {
        "milk" -> "Milk! Now you say it... milk!"
        "mix" -> "Mix! Now you say it... mix!"
        "pour" -> "Pour! Now you say it... pour!"
        "pop" -> "Pop! Now you say it... pop!"
        "mmm" -> "Mmm! Now you say it... mmm!"
        "four" -> "Four! Now you say it... four!"
        "fill" -> "Fill! Now you say it... fill!"
        "fork" -> "Fork! Now you say it... fork!"
        else -> "Say it out loud!"
    }

    /**
     * Rory's Fruit Salad: a sibling of [kitchen], sharing its counter, bowl, Rory and Bear, and
     * the same single-try talk rule (every talk beat here carries no [KitchenFlow] of its own,
     * same as kitchen's own milk/pour/mmm: one model, one listen, then the fix plays -- heard or
     * not). Five steps, each tap-only or talk-only, never both (the same K3a rule [kitchen]
     * follows):
     *  1. [SWIPE] a banana on its plate to slice it (a tap also works).
     *  2. A talk turn: "Four!" -- the slices are already there to count, Rory counting them aloud
     *     in the fix line.
     *  3. [DRAG] the slices into the bowl (a tap on them also works).
     *  4. A talk turn: "Fill!" -- the berries pour in.
     *  5. A talk turn: "Fork!" -- Rory stirs with a big fork, then he and Bear cheer "Fruit
     *     salad!", with a bonus "Yum!" line bundled into the same fix (never its own voice check),
     *     then the usual kitchen-style ending (sticker into the backpack, no tap needed).
     * Placed by Puzzle Peaks (clear of Market Town's own Rory's Kitchen stop) even though the
     * scene itself is the same kitchen counter ([Backdrop.KITCHEN]'s own land is independent of
     * where a story's map stop sits, same as [kitchen]'s own choice of Market over the busier
     * Meadow).
     */
    val fruitSalad = VoiceStory(
        // K-fruit: 🥗 (green salad) showed as a box glyph in the top-bar title on a real screenshot
        // -- 🍌 is already used (and confirmed drawing correctly) by the recipe card right beside
        // it, so the title reuses it rather than risk another emoji missing from the same build.
        id = "fruitsalad", title = "Rory's Fruit Salad", emoji = "🍌", land = Land.PEAKS, sounds = listOf("f", "s"),
        setup = "Bruno's hungry for something fresh! Let's make a fruit salad!",
        beats = listOf(
            VoiceBeat(
                Backdrop.KITCHEN, "Let's slice the banana! Swipe across it to cut it up!", listOf("slice", "slice it", "Slice the banana!"), "s", Prop.BANANA, Move.SHAKE,
                "Slice! The banana splits into slices!", "🍌", kitchen = KitchenFlow.SWIPE, still = Prop.FRUIT_BOWL, caption = "Swipe to slice!",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "How many banana slices are there? Count with me and say four!", listOf("four", "four slices", "Four banana slices!"), "f", Prop.BANANA, Move.SHAKE,
                "Four! Good talking! One, two, three, four banana slices!", "🍌", still = Prop.FRUIT_BOWL, caption = "Say four!",
            ),
            // K-fruit9: drag-first (a small nudge toward the bowl sends a slice hopping in at
            // once; a tap is only the fallback once nothing's been dragged for a moment), so the
            // step's own word is "drag", not "tap" -- its success line (like every beat's) must
            // start with that word.
            VoiceBeat(
                Backdrop.KITCHEN, "Drag the banana slices into the bowl!", listOf("drag", "drag it", "Drag it in!"), null, Prop.BANANA, Move.SHAKE,
                "Drag! Hop, hop, hop, hop! All the banana slices are in the bowl!", "🍌", kitchen = KitchenFlow.DRAG, kitchenGoal = 4, still = Prop.FRUIT_BOWL, caption = "Drag the bananas!",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "Let's fill the bowl with berries! Say fill!", listOf("fill", "fill it", "Fill the bowl!"), "f", Prop.FRUIT_BOWL, Move.SHAKE,
                "Fill! Good talking! The berries pour into the bowl!", "🫐", caption = "Say fill!",
            ),
            VoiceBeat(
                Backdrop.KITCHEN, "Time to stir it all up! Say fork!", listOf("fork", "big fork", "Get the fork!"), "f", Prop.FRUIT_BOWL, Move.SHAKE,
                "Fork! Good talking! Let's stir it all up!", "🥗", goodbye = true, caption = "Say fork!",
            ),
        ),
        swap = null, sticker = "fruit salad" to "🥗",
        ending = "What a treat! You got a fruit salad sticker. See you next time!",
    )

    val all: List<VoiceStory> = listOf(boat, owl, balloon, kitchen, fruitSalad)

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

    /** True when [word] begins with the "f" sound: a plain "f" ("fish") or the silent-h spelling
     * "ph" ("phone"). Rory's one-off "f" mouth pose (tucked lips, two big rounded teeth over the
     * lower lip, see `Critter`'s own `mouthPose`) shows for about a second while he models a word
     * like this. */
    fun isFWord(word: String): Boolean = startsWith(word, "f") || startsWith(word, "ph")

    /** The stories, best first for his target sounds (most beats practising one of them). */
    fun forSounds(focus: Set<String>): List<VoiceStory> =
        if (focus.isEmpty()) all
        else all.sortedByDescending { s -> s.beats.count { it.sound in focus } }
}
