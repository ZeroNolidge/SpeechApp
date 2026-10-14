package com.zeronolidge.speechapp.logic

/**
 * The adventure side of the app: heroes, paper pieces for the buddy, powers, lands and the trail.
 * Plain Kotlin so it can be unit tested; drawing lives in ui/art.
 */

enum class Ears { NONE, ROUND, BIG_ROUND, LONG, POINTY, FLOPPY }

enum class Mouth { SMILE, SNOUT, BEAK }

/** A kind of cartoon creature. Heroes, friends and pets are all built from these. */
enum class Species(
    val label: String,
    val ears: Ears,
    val mouth: Mouth,
    val defaultColor: Long,
    val bellyColor: Long,
    val horn: Boolean = false,
    val spikes: Boolean = false,
    val antenna: Boolean = false,
    val whiskers: Boolean = false,
    val frogEyes: Boolean = false,
    val hair: Boolean = false,
    val tail: Boolean = false,
    val boxy: Boolean = false,
) {
    KID("Kid", Ears.NONE, Mouth.SMILE, 0xFFFFCC99, 0xFFFFCC99, hair = true),
    PUPPY("Puppy", Ears.FLOPPY, Mouth.SNOUT, 0xFFD7A26B, 0xFFFFE6C7, tail = true),
    KITTY("Kitty", Ears.POINTY, Mouth.SMILE, 0xFFFFB74D, 0xFFFFF3E0, whiskers = true, tail = true),
    DINO("Dino", Ears.NONE, Mouth.SMILE, 0xFF7BD389, 0xFFE8F8C8, spikes = true, tail = true),
    UNICORN("Unicorn", Ears.POINTY, Mouth.SMILE, 0xFFF8E1F4, 0xFFFFFFFF, horn = true, tail = true),
    ROBOT("Robot", Ears.NONE, Mouth.SMILE, 0xFF90CAF9, 0xFFE3F2FD, antenna = true, boxy = true),
    BUNNY("Bunny", Ears.LONG, Mouth.SMILE, 0xFFE8E8EE, 0xFFFFE4EC, whiskers = true, tail = true),
    BEAR("Bear", Ears.ROUND, Mouth.SNOUT, 0xFFA1724E, 0xFFE6C9A8),
    MOUSE("Mouse", Ears.BIG_ROUND, Mouth.SMILE, 0xFFBDBDBD, 0xFFFFE4EC, whiskers = true, tail = true),
    FOX("Fox", Ears.POINTY, Mouth.SNOUT, 0xFFFF8A3D, 0xFFFFF8F0, tail = true),
    FROG("Frog", Ears.NONE, Mouth.SMILE, 0xFF66CC66, 0xFFDDF5C2, frogEyes = true),
    OWL("Owl", Ears.POINTY, Mouth.BEAK, 0xFFA98467, 0xFFF1E3C8),
    PENGUIN("Penguin", Ears.NONE, Mouth.BEAK, 0xFF37474F, 0xFFFFFFFF),
    CHICK("Chick", Ears.NONE, Mouth.BEAK, 0xFFFFE066, 0xFFFFF5BF),
}

/** Heroes the child can be. */
val heroSpecies = listOf(
    Species.KID, Species.PUPPY, Species.KITTY, Species.DINO, Species.UNICORN, Species.ROBOT, Species.BUNNY,
)

enum class Slot(val label: String, val emoji: String) {
    HAT("Hats", "🎩"), CAPE("Capes", "🦸"), WINGS("Wings", "🦋"), GLASSES("Glasses", "👓"), COLOR("Colors", "🎨"), PET("Pets", "🐾"),
}

/**
 * A paper piece the buddy can wear (or a pet that follows them). Pieces are earned for showing
 * up and trying, in the order of [Catalog.awardOrder], and are never taken away.
 */
data class Cosmetic(
    val id: String,
    val slot: Slot,
    val name: String,
    /** Main colour for hats, capes, wings, glasses and body colours. */
    val color: Long = 0,
    /** For pets: what kind of mini creature follows the hero. */
    val pet: Species? = null,
)

object Catalog {
    val items: List<Cosmetic> = listOf(
        // Hats
        Cosmetic("hat_cap", Slot.HAT, "Red cap", 0xFFE53935),
        Cosmetic("hat_party", Slot.HAT, "Party hat", 0xFFAB47BC),
        Cosmetic("hat_flower", Slot.HAT, "Flower crown", 0xFFFF80AB),
        Cosmetic("hat_pirate", Slot.HAT, "Pirate hat", 0xFF263238),
        Cosmetic("hat_wizard", Slot.HAT, "Wizard hat", 0xFF3949AB),
        Cosmetic("hat_crown", Slot.HAT, "Golden crown", 0xFFFFC107),
        Cosmetic("hat_top", Slot.HAT, "Top hat", 0xFF212121),
        // Capes
        Cosmetic("cape_red", Slot.CAPE, "Hero cape", 0xFFE53935),
        Cosmetic("cape_blue", Slot.CAPE, "Ocean cape", 0xFF1E88E5),
        Cosmetic("cape_green", Slot.CAPE, "Leaf cape", 0xFF43A047),
        Cosmetic("cape_purple", Slot.CAPE, "Star cape", 0xFF8E24AA),
        Cosmetic("cape_rainbow", Slot.CAPE, "Rainbow cape", 0xFFFF4081),
        // Wings
        Cosmetic("wings_fairy", Slot.WINGS, "Fairy wings", 0xFFF8BBD0),
        Cosmetic("wings_bird", Slot.WINGS, "Bird wings", 0xFFFFFFFF),
        Cosmetic("wings_butterfly", Slot.WINGS, "Butterfly wings", 0xFFFFB74D),
        Cosmetic("wings_gold", Slot.WINGS, "Golden wings", 0xFFFFD54F),
        // Glasses
        Cosmetic("glasses_round", Slot.GLASSES, "Smart glasses", 0xFF5D4037),
        Cosmetic("glasses_sun", Slot.GLASSES, "Cool shades", 0xFF212121),
        Cosmetic("glasses_star", Slot.GLASSES, "Star glasses", 0xFFFFC107),
        Cosmetic("glasses_heart", Slot.GLASSES, "Heart glasses", 0xFFE91E63),
        // Colours
        Cosmetic("color_default", Slot.COLOR, "Classic", 0),
        Cosmetic("color_sky", Slot.COLOR, "Sky blue", 0xFF81D4FA),
        Cosmetic("color_pink", Slot.COLOR, "Bubblegum", 0xFFF8BBD0),
        Cosmetic("color_lime", Slot.COLOR, "Lime", 0xFFC5E1A5),
        Cosmetic("color_sun", Slot.COLOR, "Sunshine", 0xFFFFE082),
        Cosmetic("color_grape", Slot.COLOR, "Grape", 0xFFCE93D8),
        Cosmetic("color_gold", Slot.COLOR, "Gold", 0xFFFFD54F),
        // Pets
        Cosmetic("pet_chick", Slot.PET, "Chick", pet = Species.CHICK),
        Cosmetic("pet_puppy", Slot.PET, "Puppy", pet = Species.PUPPY),
        Cosmetic("pet_kitty", Slot.PET, "Kitty", pet = Species.KITTY),
        Cosmetic("pet_dino", Slot.PET, "Baby dino", pet = Species.DINO),
        Cosmetic("pet_unicorn", Slot.PET, "Baby unicorn", pet = Species.UNICORN),
        Cosmetic("pet_robot", Slot.PET, "Robo pal", pet = Species.ROBOT),
    )

    /** The buddy has these from the start. */
    val starters = setOf("color_default")

    /** The order pieces are given out: the big, exciting ones (hat, wings, cape) come first. */
    val awardOrder: List<String> = listOf(
        "hat_party", "wings_fairy", "cape_red", "pet_chick", "glasses_star", "hat_crown",
        "color_sky", "wings_bird", "cape_blue", "hat_pirate", "pet_puppy", "glasses_sun",
        "wings_butterfly", "cape_green", "hat_flower", "color_pink", "pet_kitty", "glasses_round",
        "hat_wizard", "cape_purple", "color_lime", "pet_dino", "wings_gold", "hat_cap",
        "glasses_heart", "color_sun", "cape_rainbow", "pet_unicorn", "hat_top", "color_grape",
        "pet_robot", "color_gold",
    )

    fun byId(id: String?): Cosmetic? = items.firstOrNull { it.id == id }
    fun inSlot(slot: Slot) = items.filter { it.slot == slot }

    /** The next piece to give, or null once the buddy has everything. */
    fun nextPiece(owned: Set<String>): Cosmetic? = awardOrder.firstOrNull { it !in owned && it !in starters }?.let { byId(it) }
}

/**
 * Helper powers make challenges easier and are always there. Story powers are learned when the
 * trail first reaches their land, and then show up in the stories.
 */
enum class Power(val emoji: String, val label: String, val helper: Boolean, val describe: String) {
    OWL("🦉", "Owl friend", true, "Owl says the question again, nice and slow."),
    WAND("🪄", "Magic wand", true, "The magic wand makes one wrong answer disappear."),
    MAP("🗺️", "Treasure map", true, "The map shows the robot's next move."),
    SWIM("🏊", "Swim", false, "Now you can swim!"),
    CLIMB("🧗", "Climb", false, "Now you can climb up high!"),
    JUMP("🦘", "Super jump", false, "Now you can jump super high!"),
    LANTERN("🏮", "Lantern", false, "Your lantern lights up dark places!"),
    FLY("🦸", "Fly", false, "You can fly!"),
}

/** The kinds of learning challenges a story can contain. */
enum class Subject { SPEECH, LETTERS, READING, NUMBERS, COINS, PATTERNS, ROBOT }

enum class Land(
    val title: String,
    val emoji: String,
    val subjects: List<Subject>,
    /** The story power the buddy learns when the trail first gets here. */
    val power: Power?,
    val sky: Long,
    val ground: Long,
    val decor: List<String>,
    /** Where the land sits on the world map, from 0 to 1. */
    val mapX: Float,
    val mapY: Float,
) {
    FOREST("Talking Forest", "🌳", listOf(Subject.SPEECH), null, 0xFFB3E5FC, 0xFF9CCC65, listOf("🌳", "🌲", "🍄", "🌼"), 0.12f, 0.5f),
    MEADOW("Number Meadow", "🌻", listOf(Subject.NUMBERS), null, 0xFFFFF9C4, 0xFFAED581, listOf("🌻", "🌷", "🦋", "🐝"), 0.28f, 0.74f),
    LAGOON("Letter Lagoon", "🏝️", listOf(Subject.LETTERS), Power.SWIM, 0xFFB2EBF2, 0xFFFFE0B2, listOf("🌴", "🐚", "🦀", "⛵"), 0.5f, 0.8f),
    MARKET("Market Town", "🏪", listOf(Subject.COINS), null, 0xFFFFE0B2, 0xFFD7CCC8, listOf("🍎", "🥕", "🧺", "🍞"), 0.67f, 0.72f),
    PEAKS("Puzzle Peaks", "⛰️", listOf(Subject.PATTERNS), Power.CLIMB, 0xFFE1BEE7, 0xFFBCAAA4, listOf("⛰️", "🏔️", "🌲", "🦅"), 0.76f, 0.42f),
    FACTORY("Robot Factory", "🏭", listOf(Subject.ROBOT), Power.JUMP, 0xFFCFD8DC, 0xFFB0BEC5, listOf("⚙️", "🔩", "🔋", "💡"), 0.5f, 0.48f),
    CAVES("Word Caves", "🕯️", listOf(Subject.READING), Power.LANTERN, 0xFF5C6BC0, 0xFF8D6E63, listOf("💎", "🕯️", "🦇", "🍄"), 0.33f, 0.24f),
    CLOUDS("Cloud Castle", "🏰", Subject.entries.toList(), Power.FLY, 0xFFE3F2FD, 0xFFFFFFFF, listOf("☁️", "🌈", "⭐", "🏰"), 0.88f, 0.16f);

    /** How to point at this land for a child who can't read: by its picture, not its name. */
    val hint: String get() = when (this) {
        FOREST -> "the big trees"
        MEADOW -> "the sunflower"
        LAGOON -> "the palm tree beach"
        MARKET -> "the market stalls"
        PEAKS -> "the mountains"
        FACTORY -> "the factory"
        CAVES -> "the cave"
        CLOUDS -> "the castle in the clouds"
    }
}

/**
 * The paper trail across the map. Every land has [STOPS_PER_LAND] stops, and each stop is one
 * short chapter. Finishing a chapter unfolds the next stop. After the last stop the trail starts
 * a new lap from the beginning, so there is always a next chapter and nothing ever goes backwards.
 */
object Trail {
    const val STOPS_PER_LAND = 2
    val lands = listOf(Land.FOREST, Land.MEADOW, Land.LAGOON, Land.MARKET, Land.PEAKS, Land.FACTORY, Land.CAVES, Land.CLOUDS)
    val size = lands.size * STOPS_PER_LAND

    /** Which stop on the map chapter [chapter] (counting from 0) is played at. */
    fun stopOf(chapter: Int) = chapter.coerceAtLeast(0) % size

    fun landOf(chapter: Int) = landAt(stopOf(chapter))
    fun landAt(stop: Int) = lands[stop / STOPS_PER_LAND]

    /** True for the first stop in a land, where a new place (and maybe a new skill) begins. */
    fun opensLand(chapter: Int) = stopOf(chapter) % STOPS_PER_LAND == 0

    /** Lands the trail has reached when [chapter] is the next chapter to play. */
    fun reached(chapter: Int): List<Land> =
        if (chapter >= size) lands else lands.take(stopOf(chapter) / STOPS_PER_LAND + 1)

    /** Story powers learned so far. */
    fun powers(chapter: Int): List<Power> = reached(chapter).mapNotNull { it.power }

    /**
     * Where stop [stop] sits on the map (0 to 1). The first stop in a land sits just in front of
     * its landmark; the second sits halfway along the path to the next land.
     */
    fun position(stop: Int): Pair<Float, Float> {
        val i = lands.indexOf(landAt(stop))
        fun front(l: Land) = l.mapX to l.mapY + 0.09f
        val first = front(lands[i])
        if (stop % STOPS_PER_LAND == 0) return first
        val next = lands.getOrNull(i + 1) ?: return lands[i].mapX + 0.04f to lands[i].mapY + 0.26f
        val n = front(next)
        return (first.first + n.first) / 2 to (first.second + n.second) / 2
    }
}
