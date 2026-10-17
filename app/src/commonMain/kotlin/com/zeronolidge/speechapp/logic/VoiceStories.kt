package com.zeronolidge.speechapp.logic

/**
 * Voice-driven stories: his word makes the plot happen (no "magic word" password).
 *
 * The pattern, for every voice story:
 *  1. Setup: a character with one simple want.
 *  2. 6 or 7 beats; in each an obstacle appears and his word fixes it. Rory names the problem,
 *     says the word three times (slower the third time), the listening look shows, and any voice
 *     makes the action happen at once (a soft try: a smaller version of the same action, never a
 *     fail). Rory says the word back and adds one more ("Go! Go, boat!"). The "I said it!"
 *     button does the same action.
 *  3. His target sounds start the words of at least 4 beats (first stories); later stories
 *     put them at the ends of words too ("up", "stop").
 *  4. One prop can be one of his own words (a name for a character, a thing for a thing).
 *  5. Each beat has three versions of its line: one word, two words, a short sentence; the
 *     quiet speech step ([SpeechSteps], [Progress.speechStep]) picks one.
 *  6. A calm goodbye beat, then a sticker. 3 to 4 minutes, at most [MAX_BEATS] speaking beats.
 * Loudness only (no speech recognition).
 *
 * Text can use {p} (the prop: "duck", or his word "Grandma"), {thep} ("the duck" or "Grandma")
 * and {Thep} (the same, starting a sentence).
 */
data class VoiceBeat(
    /** Rory names the problem. */
    val problem: String,
    /** The line to say: one word, two words, a short sentence. */
    val lines: List<String>,
    /** Rory says it back and adds one more. */
    val echo: String,
    /** What his word made happen. */
    val result: String,
    /** The beat's picture, for the word chip and the sticker. */
    val emoji: String,
    /** The calm last beat. */
    val goodbye: Boolean = false,
) {
    /** The word itself (the one-word version). */
    val word: String get() = lines[0]

    /** The line for this speech step: 0 one word, 1 two words, 2 a short sentence. */
    fun line(step: Int): String = lines[step.coerceIn(0, lines.lastIndex)]
}

enum class PropKind { CHARACTER, THING }

/** The story prop his own word can stand in for. */
data class StoryProp(val name: String, val emoji: String, val kind: PropKind, val isName: Boolean = false)

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
    val prop: StoryProp,
    /** The sticker he gets at the end (word, emoji). */
    val sticker: Pair<String, String>,
    val ending: String,
)

object VoiceStories {
    const val MAX_BEATS = 8

    private val duck = StoryProp("duck", "🦆", PropKind.CHARACTER)
    private val berries = StoryProp("berries", "🍒", PropKind.THING)
    private val bird = StoryProp("bird", "🐦", PropKind.CHARACTER)

    val boat = VoiceStory(
        id = "boat", title = "Pip's Boat Trip", emoji = "⛵", land = Land.LAGOON, sounds = listOf("p", "b", "g"),
        setup = "Pip has a little paper boat, and Pip wants to go on a trip down the river! Your words make the boat go.",
        beats = listOf(
            VoiceBeat("The boat is stuck at the dock.", listOf("go", "go boat", "Go, boat, go!"), "Go! Go, boat!", "The boat glides out onto the river!", "⛵"),
            VoiceBeat("Oh! A big bubble is blocking the river.", listOf("pop", "pop bubble", "Pop the bubble!"), "Pop! Pop, bubble!", "Pop! Confetti everywhere!", "🎉"),
            VoiceBeat("The sail is all droopy.", listOf("blow", "blow wind", "Blow, wind, blow!"), "Blow! Blow, wind!", "The sail puffs up, and the boat speeds along!", "🌬️"),
            VoiceBeat("Oh no! {Thep} is stuck on a rock.", listOf("come", "come {p}", "Come here, {p}!"), "Come! Come, {p}!", "{Thep} paddles over to the boat!", "🦆"),
            VoiceBeat("Uh oh, a big wave is coming!", listOf("up", "go up", "Up, up, boat!"), "Up! Up we go!", "The boat rides right over the wave!", "🌊"),
            VoiceBeat("Look, a little island!", listOf("stop", "stop boat", "Stop the boat!"), "Stop! Stop, boat!", "Bump! The boat stops on the soft sand.", "🏝️"),
            VoiceBeat("Everyone is sleepy after the trip.", listOf("bye", "bye {p}", "Bye-bye, {p}!"), "Bye! Bye-bye, {p}!", "{Thep} waves goodbye, and Pip falls fast asleep.", "👋", goodbye = true),
        ),
        prop = duck, sticker = "boat" to "⛵",
        ending = "What a trip! You got a boat sticker. See you next time!",
    )

    val owl = VoiceStory(
        id = "owl", title = "Sleepy Owl's Morning", emoji = "🦉", land = Land.FOREST, sounds = listOf("s", "w", "m"),
        setup = "It's morning in the forest, but it's still dark, and Owl is fast asleep. Your words wake up the morning!",
        beats = listOf(
            VoiceBeat("The sky is still dark.", listOf("sun", "sun up", "Wake up, sun!"), "Sun! Hello, sun!", "The sun peeks over the hill!", "☀️"),
            VoiceBeat("Owl is snoring. Zzz.", listOf("wake", "wake owl", "Wake up, owl!"), "Wake! Wake up, owl!", "Owl's eyes pop open!", "🦉"),
            VoiceBeat("Brr! Owl is too cold to fly.", listOf("warm", "warm owl", "Get warm, owl!"), "Warm! Warm owl!", "Sunbeams wrap Owl up, all cozy and warm.", "🌞"),
            VoiceBeat("Leaves are blocking the path.", listOf("sss", "sss leaves", "Sss, move, leaves!"), "Sss! Sss, leaves!", "Whoosh! The leaves blow aside.", "🍂"),
            VoiceBeat("Owl's tummy rumbles. Owl is hungry!", listOf("more", "more {p}", "More {p}, please!"), "More! More {p}!", "Here come the {p}! Yum!", "🍒"),
            VoiceBeat("Owl is ready to fly!", listOf("fly", "fly owl", "Fly up high, owl!"), "Fly! Fly, owl!", "Owl swoops around in a big loop!", "🦉"),
            VoiceBeat("Owl says thank you.", listOf("bye", "bye owl", "Bye, see you, owl!"), "Bye! Bye-bye, owl!", "Owl drops a feather for you, and flies home.", "👋", goodbye = true),
        ),
        prop = berries, sticker = "owl" to "🦉",
        ending = "What a morning! You got an owl sticker. See you next time!",
    )

    val balloon = VoiceStory(
        id = "balloon", title = "The Balloon Picnic", emoji = "🎈", land = Land.MEADOW, sounds = listOf("b", "p", "m"),
        setup = "Bear wants to have a picnic, up in the sky! Your words make the balloon fly.",
        beats = listOf(
            VoiceBeat("The balloon is all flat.", listOf("blow", "big balloon", "Blow it big!"), "Blow! Big balloon!", "The balloon puffs up, bigger and bigger!", "🎈"),
            VoiceBeat("Bear wants a ride.", listOf("up", "go up", "Up we go!"), "Up! Up we go!", "The balloon lifts Bear up into the sky!", "🐻"),
            VoiceBeat("Oh! {Thep} is in the way.", listOf("move", "move {p}", "Move, please, {p}!"), "Move! Move, {p}!", "{Thep} moves out of the way!", "🐦"),
            VoiceBeat("Look! Cupcakes, far down below.", listOf("down", "go down", "Down to the picnic!"), "Down! Down we go!", "The balloon floats gently down to the picnic.", "🧁"),
            VoiceBeat("Bear wants a treat.", listOf("more", "more cake", "More cake, please!"), "More! More cake!", "A cupcake drops right into Bear's paw!", "🧁"),
            VoiceBeat("Bubbles are floating past!", listOf("pop", "pop pop", "Pop the bubbles!"), "Pop! Pop, pop!", "Pop, pop, pop! They burst one by one.", "🎉"),
            VoiceBeat("The sun is going down. Time to rest.", listOf("night", "night Bear", "Good night, Bear!"), "Night! Good night, Bear!", "The moon rises, and Bear gives a big yawn.", "🌙", goodbye = true),
        ),
        prop = bird, sticker = "balloon" to "🎈",
        ending = "What a picnic! You got a balloon sticker. See you next time!",
    )

    val all: List<VoiceStory> = listOf(boat, owl, balloon)

    fun byId(id: String): VoiceStory? = all.firstOrNull { it.id == id }

    /**
     * The story ready to play: its prop is one of his own words where one fits (a name, written
     * with a capital, for a character; any other word for a thing), and every {p} is filled in.
     */
    fun ready(story: VoiceStory, own: List<Pair<String, String>>): VoiceStory {
        val mine = own.map { it.first.trim() to it.second }.filter { it.first.isNotEmpty() }.firstOrNull { (text, _) ->
            val name = text.first().isUpperCase()
            if (story.prop.kind == PropKind.CHARACTER) name else !name
        }
        val prop = mine?.let { StoryProp(it.first, it.second, story.prop.kind, isName = it.first.first().isUpperCase()) } ?: story.prop
        fun fill(s: String): String {
            val the = if (prop.isName) prop.name else "the ${prop.name}"
            return s.replace("{Thep}", the.replaceFirstChar { it.uppercase() }).replace("{thep}", the).replace("{p}", prop.name)
        }
        return story.copy(
            prop = prop,
            setup = fill(story.setup),
            beats = story.beats.map { b ->
                b.copy(
                    problem = fill(b.problem), lines = b.lines.map(::fill), echo = fill(b.echo), result = fill(b.result),
                    emoji = if (b.emoji == story.prop.emoji) prop.emoji else b.emoji,
                )
            },
        )
    }

    /** True when [word] starts with the speech sound [sound] ("p", "sh", ...). */
    fun startsWith(word: String, sound: String): Boolean = word.trim().lowercase().startsWith(sound.lowercase())

    /** True when [word] ends with the speech sound [sound] ("up", "stop"). */
    fun endsWith(word: String, sound: String): Boolean = word.trim().lowercase().trimEnd('!', '.').endsWith(sound.lowercase())

    /** The stories, best first for his target sounds (most beats starting with one of them). */
    fun forSounds(focus: Set<String>): List<VoiceStory> =
        if (focus.isEmpty()) all
        else all.sortedByDescending { s -> s.beats.count { b -> focus.any { startsWith(b.word, it) } } }
}
