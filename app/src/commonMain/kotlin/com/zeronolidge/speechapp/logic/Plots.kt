package com.zeronolidge.speechapp.logic

/**
 * Hand-written story plots. Each has a beginning, a chain of obstacles (each one hosts a
 * challenge), a twist in the middle where someone new turns up, and a happy ending.
 * Every scene names a [SetPiece], which the app draws behind the characters.
 */

/** A picture drawn behind the characters. Many change once the obstacle is solved. */
enum class SetPiece {
    WINDY_SKY, FORK_PATH, RIVER, BROKEN_BRIDGE, LOCKED_DOOR, TALL_TREE, DARK_CAVE,
    BIG_HILL, PARTY, TREASURE, NIGHT_SKY, PICNIC, GARDEN,
}

/** How the friend feels in a scene. */
enum class Feeling { HAPPY, SAD, WORRIED, CHEER }

data class Scene(
    val text: String,
    val piece: SetPiece,
    /** An emoji that belongs in the picture, e.g. the kite stuck in the tree. */
    val prop: String? = null,
    val feeling: Feeling = Feeling.HAPPY,
)

data class Plot(
    val id: String,
    val goal: String,
    val intro: Scene,
    /** Usually four; the last one is the big finish. */
    val obstacles: List<Scene>,
    /** A short moment, without a challenge, where {v} (a visitor) turns up. */
    val twist: Scene,
    val ending: Scene,
    /** Who turns up in the twist, if the plot needs someone in particular. */
    val visitor: Species? = null,
)

object Plots {
    /** Used when a quest needs more obstacles than a plot has. */
    val extraObstacle = Scene(
        "The trail winds on through tall grass. Something is blocking the way!",
        SetPiece.FORK_PATH,
        feeling = Feeling.WORRIED,
    )

    val all: List<Plot> = listOf(
        Plot(
            "kite", "🪁",
            intro = Scene(
                "Whoosh! A big gust of wind grabbed {f}'s kite and carried it far, far away! {f} is sad. Let's help find it!",
                SetPiece.WINDY_SKY, "🪁", Feeling.SAD,
            ),
            obstacles = listOf(
                Scene("The kite flew over the hill. But look, the path splits three ways! Which way did it go?", SetPiece.FORK_PATH, feeling = Feeling.WORRIED),
                Scene("A sparkly river is in the way. We need to get across!", SetPiece.RIVER),
                Scene("We can see the kite string past a wobbly bridge, but some boards are missing!", SetPiece.BROKEN_BRIDGE, feeling = Feeling.WORRIED),
                Scene("There it is! The kite is stuck at the very top of a tall tree!", SetPiece.TALL_TREE, "🪁"),
            ),
            twist = Scene("Hoo hoo! A friendly {v} lands on a branch. 'The kite is close! Keep going!'", SetPiece.TALL_TREE),
            ending = Scene("We got the kite down! {f} runs and runs, and the kite flies high in the sky again. Thank you, {hero}!", SetPiece.WINDY_SKY, "🪁", Feeling.CHEER),
            visitor = Species.OWL,
        ),
        Plot(
            "party", "🎂",
            intro = Scene(
                "It's {f}'s birthday! But oh no, the party things are missing. No balloons, no presents, and no cake!",
                SetPiece.PARTY, feeling = Feeling.SAD,
            ),
            obstacles = listOf(
                Scene("First, balloons! They got tangled up in the flower garden.", SetPiece.GARDEN, "🎈"),
                Scene("The presents are in the cupboard, but the door is locked!", SetPiece.LOCKED_DOOR, "🎁", Feeling.WORRIED),
                Scene("We need strawberries for the cake. They grow on the other side of the stream.", SetPiece.RIVER, "🍓"),
                Scene("Now let's make the birthday cake, just right!", SetPiece.PARTY, "🎂"),
            ),
            twist = Scene("Knock knock! A friendly {v} peeks in. 'Can I come to the party too?' Of course you can!", SetPiece.PARTY),
            ending = Scene("Surprise! Balloons, presents and a yummy cake. Happy birthday, {f}! Everyone sings and dances!", SetPiece.PARTY, "🎂", Feeling.CHEER),
        ),
        Plot(
            "treasure", "💰",
            intro = Scene(
                "{f} found an old treasure map! X marks the spot. Let's go find the treasure!",
                SetPiece.TREASURE, "🗺️", Feeling.HAPPY,
            ),
            obstacles = listOf(
                Scene("The map says: follow the path to the big rock. But which path?", SetPiece.FORK_PATH),
                Scene("The map leads into a dark cave. Spooky... but fun!", SetPiece.DARK_CAVE, feeling = Feeling.WORRIED),
                Scene("Now we have to cross the water!", SetPiece.RIVER),
                Scene("Here is the treasure chest! But it has a puzzle lock.", SetPiece.TREASURE),
            ),
            twist = Scene("A {v} pops out from behind a rock. 'The treasure is very close,' it whispers. 'Look for the chest!'", SetPiece.DARK_CAVE),
            ending = Scene("Creak! The chest opens. It's full of shiny gems! {f} shares the treasure with everyone.", SetPiece.TREASURE, feeling = Feeling.CHEER),
        ),
        Plot(
            "teddy", "🧸",
            intro = Scene(
                "{f} can't find Teddy anywhere! Teddy was there at bedtime, and now Teddy is gone!",
                SetPiece.GARDEN, feeling = Feeling.SAD,
            ),
            obstacles = listOf(
                Scene("Let's look in the garden. Look, tiny footprints!", SetPiece.GARDEN),
                Scene("The footprints go all the way up a big hill.", SetPiece.BIG_HILL),
                Scene("The footprints go into a cozy cave.", SetPiece.DARK_CAVE, feeling = Feeling.WORRIED),
                Scene("Look up! Teddy is sitting in a tree, next to a baby bird!", SetPiece.TALL_TREE, "🧸"),
            ),
            twist = Scene("A playful {v} runs by. 'Did you take Teddy?' 'Not me!' it giggles. 'But I saw Teddy go that way!'", SetPiece.BIG_HILL),
            ending = Scene("Teddy was keeping the baby bird company! Now Teddy is home. Big hugs for Teddy!", SetPiece.GARDEN, "🧸", Feeling.CHEER),
        ),
        Plot(
            "star", "⭐",
            intro = Scene(
                "Look! A little star fell out of the sky and landed near {f}'s house. The star wants to go home!",
                SetPiece.NIGHT_SKY, "⭐", Feeling.WORRIED,
            ),
            obstacles = listOf(
                Scene("To get close to the sky, we need to go up high. Let's climb the big hill!", SetPiece.BIG_HILL, "⭐"),
                Scene("A bridge leads to the tallest mountain, but it needs fixing.", SetPiece.BROKEN_BRIDGE, "⭐"),
                Scene("A big tree reaches up to the clouds. Up we go!", SetPiece.TALL_TREE, "⭐"),
                Scene("We're so high! Now let's give the star a big boost back into the sky.", SetPiece.NIGHT_SKY, "⭐"),
            ),
            twist = Scene("A sleepy {v} rubs its eyes. 'Shh, the moon is sleeping,' it whispers. Let's be quiet... and quick!", SetPiece.NIGHT_SKY),
            ending = Scene("Whoosh! The star zooms back up and twinkles. It's saying thank you, {hero}!", SetPiece.NIGHT_SKY, "🌟", Feeling.CHEER),
            visitor = Species.OWL,
        ),
        Plot(
            "key", "🔑",
            intro = Scene(
                "{f}'s clubhouse door is locked, and the golden key is lost! Let's find it.",
                SetPiece.LOCKED_DOOR, feeling = Feeling.SAD,
            ),
            obstacles = listOf(
                Scene("Maybe the key fell in the garden. Let's look!", SetPiece.GARDEN),
                Scene("A fish splashes. It saw something shiny across the river!", SetPiece.RIVER),
                Scene("Something shiny is glowing inside a cave!", SetPiece.DARK_CAVE, "🔑"),
                Scene("We found the key! Now let's open the tricky lock.", SetPiece.LOCKED_DOOR, "🔑"),
            ),
            twist = Scene("A tiny {v} hurries over. 'I saw something gold and shiny go into the cave!'", SetPiece.DARK_CAVE),
            ending = Scene("Click! The door swings open. Welcome to the clubhouse! There are games and snacks inside.", SetPiece.LOCKED_DOOR, feeling = Feeling.CHEER),
            visitor = Species.MOUSE,
        ),
        Plot(
            "picnic", "🧺",
            intro = Scene(
                "What a sunny day! {f} wants to have a picnic, but the basket is empty.",
                SetPiece.PICNIC, feeling = Feeling.WORRIED,
            ),
            obstacles = listOf(
                Scene("Let's pick juicy strawberries in the garden.", SetPiece.GARDEN, "🍓"),
                Scene("Red apples grow on this big tree. Let's get some!", SetPiece.TALL_TREE, "🍎"),
                Scene("We need fresh water. Let's go down to the river.", SetPiece.RIVER),
                Scene("Let's set up the picnic blanket just right.", SetPiece.PICNIC, "🧀"),
            ),
            twist = Scene("Sniff sniff! A hungry {v} smells the food. 'Mmm, is that a picnic?' Let's share!", SetPiece.PICNIC),
            ending = Scene("Picnic time! Everyone shares the yummy food. What a happy day! Thank you, {hero}!", SetPiece.PICNIC, "🧺", Feeling.CHEER),
            visitor = Species.BEAR,
        ),
        Plot(
            "balloon", "🎈",
            intro = Scene(
                "{f}'s red balloon slipped away! Up, up, up it goes!",
                SetPiece.WINDY_SKY, "🎈", Feeling.SAD,
            ),
            obstacles = listOf(
                Scene("Which way did the balloon float? The path splits!", SetPiece.FORK_PATH, "🎈"),
                Scene("The balloon floated over the river!", SetPiece.RIVER, "🎈"),
                Scene("There it is, bobbing above a big hill!", SetPiece.BIG_HILL, "🎈"),
                Scene("The balloon string is caught on a branch!", SetPiece.TALL_TREE, "🎈"),
            ),
            twist = Scene("A little {v} waves. 'Your balloon went that way! Hurry!'", SetPiece.BIG_HILL, "🎈"),
            ending = Scene("Got it! {f} holds the balloon tight and does a happy dance!", SetPiece.WINDY_SKY, "🎈", Feeling.CHEER),
        ),
    )
}

/**
 * Words the child says out loud to make a scene happen, e.g. "open" opens the door.
 * [words] go from easiest to hardest: one syllable, two syllables, then a short phrase.
 */
data class StoryWord(val piece: SetPiece, val words: List<String>, val emoji: String, val doing: String) {
    fun at(step: Int) = words[step.coerceIn(0, words.lastIndex)]
}

object StoryWords {
    val all = listOf(
        StoryWord(SetPiece.LOCKED_DOOR, listOf("knock", "open", "open the door"), "🚪", "open the door"),
        StoryWord(SetPiece.RIVER, listOf("jump", "jumping", "jump across"), "🐸", "jump across the water"),
        StoryWord(SetPiece.BROKEN_BRIDGE, listOf("fix", "hammer", "fix the bridge"), "🔨", "fix the bridge"),
        StoryWord(SetPiece.TALL_TREE, listOf("up", "climbing", "up we go"), "🌳", "climb up the tree"),
        StoryWord(SetPiece.DARK_CAVE, listOf("glow", "sparkle", "light it up"), "✨", "light up the cave"),
        StoryWord(SetPiece.BIG_HILL, listOf("climb", "up up", "up the hill"), "⛰️", "get up the hill"),
        StoryWord(SetPiece.PARTY, listOf("pop", "party", "happy birthday"), "🎈", "start the party"),
        StoryWord(SetPiece.TREASURE, listOf("dig", "open", "open the box"), "🎁", "open the treasure"),
        StoryWord(SetPiece.WINDY_SKY, listOf("blow", "windy", "blow wind blow"), "💨", "make the wind blow"),
        StoryWord(SetPiece.FORK_PATH, listOf("go", "follow", "follow me"), "👣", "find the way"),
        StoryWord(SetPiece.NIGHT_SKY, listOf("fly", "twinkle", "fly up high"), "🌟", "fly up high"),
        StoryWord(SetPiece.PICNIC, listOf("yum", "yummy", "yum yum yum"), "😋", "start the picnic"),
        StoryWord(SetPiece.GARDEN, listOf("grow", "flowers", "grow grow grow"), "🌱", "make the flowers grow"),
    )

    fun forPiece(piece: SetPiece) = all.first { it.piece == piece }
}
