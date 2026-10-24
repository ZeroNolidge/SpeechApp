package com.zeronolidge.speechapp.data

/**
 * All of the learning content lives here as plain data, so it is easy to add words
 * or tweak tips without touching any screen code.
 */

data class Word(
    val text: String,
    val emoji: String,
    /** Slow, chunked version spoken by the "turtle" button, e.g. "ba - na - na". */
    val syllables: String = text,
) {
    /** 1 for "pig", 2 for "pizza"; a short phrase counts its words' syllables. */
    val syllableCount: Int get() = text.split(' ').size.coerceAtLeast(syllables.split(" - ").size)
}

data class SpeechSound(
    val id: String,
    /** What the child sees on the tile, e.g. "S" or "Sh". */
    val label: String,
    val emoji: String,
    /** What the app says to model the sound on its own, e.g. "sssss". */
    val modelSound: String,
    /** A short, spoken coaching tip about where the lips and tongue go. */
    val tip: String,
    val words: List<Word>,
    /** Short phrases for when single words get easy. */
    val phrases: List<Word> = emptyList(),
)

object SpeechContent {
    val sounds: List<SpeechSound> = listOf(
        SpeechSound(
            "p", "P", "🐷", "puh, puh, puh",
            "Press your lips together, then pop them open. Puh!",
            listOf(
                Word("pig", "🐷"), Word("pie", "🥧"), Word("pear", "🍐"),
                Word("pizza", "🍕", "pee - tsa"), Word("penguin", "🐧", "pen - gwin"),
                Word("popcorn", "🍿", "pop - corn"),
            ),
            listOf(
                Word("a pink pig", "🐷"), Word("pizza pie", "🍕", "pee - tsa pie"), Word("pop the popcorn", "🍿"),
            ),
        ),
        SpeechSound(
            "b", "B", "🐝", "buh, buh, buh",
            "Lips together, then let your voice pop out. Buh!",
            listOf(
                Word("bee", "🐝"), Word("ball", "⚽"), Word("banana", "🍌", "ba - na - na"),
                Word("bus", "🚌"), Word("bear", "🐻"), Word("boat", "⛵"),
            ),
            listOf(
                Word("a big ball", "⚽"), Word("bye bye bee", "🐝"), Word("a blue bus", "🚌"),
            ),
        ),
        SpeechSound(
            "m", "M", "🐵", "mmmmm",
            "Close your lips and hum. Mmmm, yummy!",
            listOf(
                Word("moon", "🌙"), Word("mouse", "🐭"), Word("milk", "🥛"),
                Word("monkey", "🐵", "mon - key"), Word("mushroom", "🍄", "mush - room"),
                Word("map", "🗺️"),
            ),
            listOf(
                Word("more milk", "🥛"), Word("my mouse", "🐭"), Word("a moon map", "🗺️"),
            ),
        ),
        SpeechSound(
            "t", "T", "🐯", "tuh, tuh, tuh",
            "Tap the tip of your tongue behind your top teeth. Tuh!",
            listOf(
                Word("tiger", "🐯", "ti - ger"), Word("toe", "🦶"), Word("tent", "⛺"),
                Word("taco", "🌮", "ta - co"), Word("turtle", "🐢", "tur - tle"),
                Word("tooth", "🦷"),
            ),
            listOf(
                Word("ten toes", "🦶"), Word("a tiny tent", "⛺"), Word("two tacos", "🌮"),
            ),
        ),
        SpeechSound(
            "d", "D", "🐶", "duh, duh, duh",
            "Tongue tip behind your top teeth, and use your voice. Duh!",
            listOf(
                Word("dog", "🐶"), Word("duck", "🦆"), Word("door", "🚪"),
                Word("dinosaur", "🦕", "di - no - saur"), Word("donut", "🍩", "do - nut"),
                Word("drum", "🥁"),
            ),
            listOf(
                Word("a dog and a duck", "🐶"), Word("open the door", "🚪"), Word("a big drum", "🥁"),
            ),
        ),
        SpeechSound(
            "k", "K", "🐱", "kuh, kuh, kuh",
            "Lift the back of your tongue, like a little cough. Kuh!",
            listOf(
                Word("cat", "🐱"), Word("key", "🔑"), Word("cake", "🎂"),
                Word("car", "🚗"), Word("cow", "🐮"), Word("cookie", "🍪", "coo - kie"),
            ),
            listOf(
                Word("cake for the cat", "🎂"), Word("a car key", "🔑"), Word("cookie please", "🍪"),
            ),
        ),
        SpeechSound(
            "g", "G", "🐐", "guh, guh, guh",
            "Back of your tongue up, and use your voice. Guh!",
            listOf(
                Word("goat", "🐐"), Word("gift", "🎁"), Word("girl", "👧"),
                Word("grapes", "🍇"), Word("guitar", "🎸", "gi - tar"),
                Word("gorilla", "🦍", "go - ril - la"),
            ),
            listOf(
                Word("a goat and a gift", "🐐"), Word("go go go", "🚦"), Word("green grapes", "🍇"),
            ),
        ),
        SpeechSound(
            "f", "F", "🐸", "fffff",
            "Gently bite your bottom lip and blow. Fffff!",
            listOf(
                Word("fish", "🐟"), Word("fox", "🦊"), Word("frog", "🐸"),
                Word("fire", "🔥"), Word("fork", "🍴"), Word("flower", "🌸", "flow - er"),
            ),
            listOf(
                Word("a fish and a frog", "🐟"), Word("four fish", "🐠"), Word("a funny fox", "🦊"),
            ),
        ),
        SpeechSound(
            "s", "S", "🐍", "sssss",
            "Teeth together, smile, and let the air hiss like a snake. Ssss!",
            listOf(
                Word("sun", "☀️"), Word("sock", "🧦"), Word("soap", "🧼"),
                Word("six", "6️⃣"), Word("snake", "🐍"), Word("sandwich", "🥪", "sand - wich"),
            ),
            listOf(
                Word("the sun is up", "☀️"), Word("six socks", "🧦"), Word("a silly snake", "🐍"),
            ),
        ),
        SpeechSound(
            "l", "L", "🦁", "lalala",
            "Put your tongue tip up behind your top teeth. La la la!",
            listOf(
                Word("lion", "🦁", "li - on"), Word("leaf", "🍃"), Word("lemon", "🍋", "lem - on"),
                Word("lock", "🔒"), Word("ladybug", "🐞", "la - dy - bug"),
                Word("lollipop", "🍭", "lol - li - pop"),
            ),
            listOf(
                Word("a lion and a lemon", "🦁"), Word("lots of leaves", "🍃"), Word("a little ladybug", "🐞"),
            ),
        ),
        SpeechSound(
            "r", "R", "🚀", "rrrrr",
            "Pull your tongue back, don't let it touch the top, and growl like a tiger. Rrrr!",
            listOf(
                Word("rabbit", "🐰", "rab - bit"), Word("rain", "🌧️"), Word("rocket", "🚀", "rock - et"),
                Word("ring", "💍"), Word("robot", "🤖", "ro - bot"), Word("rainbow", "🌈", "rain - bow"),
            ),
            listOf(
                Word("a red rocket", "🚀"), Word("rain and rainbow", "🌈"), Word("run rabbit run", "🐰"),
            ),
        ),
        SpeechSound(
            "sh", "Sh", "🤫", "shhhhh",
            "Push your lips out like a fish and say shhh, like a sleeping baby.",
            listOf(
                Word("shoe", "👟"), Word("sheep", "🐑"), Word("ship", "🚢"),
                Word("shark", "🦈"), Word("shell", "🐚"), Word("shirt", "👕"),
            ),
            listOf(
                Word("a shark and a ship", "🦈"), Word("shoes on", "👟"), Word("a shiny shell", "🐚"),
            ),
        ),
        SpeechSound(
            "ch", "Ch", "🚂", "choo choo",
            "Lips out, then a quick sneeze sound, like a train. Choo choo!",
            listOf(
                Word("cheese", "🧀"), Word("chicken", "🐔", "chick - en"), Word("cherry", "🍒", "cher - ry"),
                Word("chocolate", "🍫", "choc - o - late"), Word("chick", "🐤"), Word("chair", "🪑"),
            ),
            listOf(
                Word("cheese please", "🧀"), Word("a chick on a chair", "🐤"), Word("cherry pie", "🍒"),
            ),
        ),
        SpeechSound(
            "th", "Th", "👅", "thhhhh",
            "Put your tongue between your teeth and blow gently. Thhhh!",
            listOf(
                Word("thumb", "👍"), Word("three", "3️⃣"), Word("think", "🤔"),
                Word("thunder", "⛈️", "thun - der"), Word("bath", "🛁"), Word("mouth", "👄"),
            ),
            listOf(
                Word("thumbs up", "👍"), Word("three things", "3️⃣"), Word("take a bath", "🛁"),
            ),
        ),
    )
}

data class LetterInfo(
    val upper: Char,
    val lower: Char,
    /** Spelling that makes text-to-speech say the letter's sound (not its name). */
    val sound: String,
    val word: Word,
)

object LetterContent {
    val letters: List<LetterInfo> = listOf(
        LetterInfo('A', 'a', "ah", Word("apple", "🍎")),
        LetterInfo('B', 'b', "buh", Word("ball", "⚽")),
        LetterInfo('C', 'c', "kuh", Word("cat", "🐱")),
        LetterInfo('D', 'd', "duh", Word("dog", "🐶")),
        LetterInfo('E', 'e', "eh", Word("egg", "🥚")),
        LetterInfo('F', 'f', "fff", Word("fish", "🐟")),
        LetterInfo('G', 'g', "guh", Word("goat", "🐐")),
        LetterInfo('H', 'h', "huh", Word("hat", "🎩")),
        LetterInfo('I', 'i', "ih", Word("igloo", "🧊")),
        LetterInfo('J', 'j', "juh", Word("juice", "🧃")),
        LetterInfo('K', 'k', "kuh", Word("kite", "🪁")),
        LetterInfo('L', 'l', "lll", Word("lion", "🦁")),
        LetterInfo('M', 'm', "mmm", Word("moon", "🌙")),
        LetterInfo('N', 'n', "nnn", Word("nose", "👃")),
        LetterInfo('O', 'o', "aw", Word("octopus", "🐙")),
        LetterInfo('P', 'p', "puh", Word("pig", "🐷")),
        LetterInfo('Q', 'q', "kwuh", Word("queen", "👸")),
        LetterInfo('R', 'r', "rrr", Word("rabbit", "🐰")),
        LetterInfo('S', 's', "sss", Word("sun", "☀️")),
        LetterInfo('T', 't', "tuh", Word("tiger", "🐯")),
        LetterInfo('U', 'u', "uh", Word("umbrella", "☂️")),
        LetterInfo('V', 'v', "vvv", Word("van", "🚐")),
        LetterInfo('W', 'w', "wuh", Word("whale", "🐳")),
        LetterInfo('X', 'x', "ks", Word("fox", "🦊")),
        LetterInfo('Y', 'y', "yuh", Word("yo-yo", "🪀")),
        LetterInfo('Z', 'z', "zzz", Word("zebra", "🦓")),
    )

    fun of(c: Char): LetterInfo = letters.first { it.lower == c.lowercaseChar() }
}

/** Short, regular words a child can sound out letter by letter. */
object ReadingContent {
    val cvcWords: List<Word> = listOf(
        Word("cat", "🐱"), Word("dog", "🐶"), Word("sun", "☀️"), Word("bus", "🚌"),
        Word("pig", "🐷"), Word("hat", "🎩"), Word("bed", "🛏️"), Word("cup", "☕"),
        Word("fox", "🦊"), Word("hen", "🐔"), Word("map", "🗺️"), Word("net", "🥅"),
        Word("pen", "🖊️"), Word("box", "📦"), Word("bug", "🐛"), Word("van", "🚐"),
        Word("web", "🕸️"), Word("bat", "🦇"), Word("ten", "🔟"), Word("six", "6️⃣"),
        Word("red", "🔴"), Word("bag", "👜"), Word("jet", "✈️"), Word("hot", "🔥"),
        Word("nut", "🥜"), Word("pot", "🍲"), Word("leg", "🦵"), Word("bun", "🍞"),
    )
}

/** Friendly emoji used for counting, adding and patterns. */
object Things {
    val countables = listOf("🍎", "⭐", "🐟", "🚗", "🎈", "🐞", "🍪", "🌸", "🐥", "⚽")
    val countableNames = mapOf(
        "🥕" to "carrots", "🌰" to "acorns", "🐚" to "shells", "💎" to "gems", "🧁" to "cupcakes",
        "🍌" to "bananas", "🦋" to "butterflies", "🐝" to "bees", "🍄" to "mushrooms", "🔩" to "bolts",
        "🔋" to "batteries", "🦀" to "crabs", "🍓" to "strawberries", "🕯️" to "candles",
        "🌼" to "daisies", "☁️" to "clouds", "🍩" to "donuts", "🐠" to "fish", "🎁" to "presents",
        "🍎" to "apples", "⭐" to "stars", "🐟" to "fish", "🚗" to "cars", "🎈" to "balloons",
        "🐞" to "ladybugs", "🍪" to "cookies", "🌸" to "flowers", "🐥" to "chicks", "⚽" to "balls",
        "🍞" to "bread rolls", "🧀" to "cheeses", "🥚" to "eggs",
    )
    val shapes = listOf("🔴", "🔵", "🟡", "🟢", "🟣", "🟠")
    val shapeNames = mapOf(
        "🔴" to "red", "🔵" to "blue", "🟡" to "yellow", "🟢" to "green", "🟣" to "purple", "🟠" to "orange",
    )
    val animals = listOf("🐶", "🐱", "🐰", "🐸", "🐵", "🐷", "🐮", "🦁")
    val foods = listOf("🍎", "🍌", "🍇", "🍓", "🍕", "🍪", "🥕", "🧀")
    val vehicles = listOf("🚗", "🚌", "🚀", "🚂", "✈️", "🚲", "🚁", "⛵")
    val categories = listOf(animals, foods, vehicles)

    fun nameOf(emoji: String): String =
        countableNames[emoji] ?: shapeNames[emoji] ?: "things"
}

object Praise {
    val great = listOf(
        "Great job!", "You did it!", "Awesome!", "Good talking!", "Way to go!",
        "Fantastic!", "Hooray!", "I love your talking!", "Wonderful!",
    )
    val tryAgain = listOf(
        "Good try! Let's try again.", "Almost! Try one more time.",
        "Nice try! You can do it.", "Oops! Let's try again.",
    )
    val speechTry = listOf(
        "Good talking! Let's say it again together.",
        "Nice try! Listen once more and say it with me.",
        "I love your trying! One more time.",
    )
}
