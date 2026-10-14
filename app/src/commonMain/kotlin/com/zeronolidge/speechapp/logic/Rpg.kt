package com.zeronolidge.speechapp.logic

/**
 * The role-playing side of the app: heroes, cosmetics, powers, levels and lands.
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
    HAT("Hats", "🎩"), CAPE("Capes", "🦸"), GLASSES("Glasses", "👓"), COLOR("Colors", "🎨"), PET("Pets", "🐾"),
}

/**
 * Something to wear (or a pet). Items show up in the shop at [level].
 * A [price] of 0 means it is a free gift at that level; otherwise it costs that many gems.
 */
data class Cosmetic(
    val id: String,
    val slot: Slot,
    val name: String,
    val level: Int,
    val price: Int,
    /** Main colour for capes, glasses and body colours. */
    val color: Long = 0,
    /** For pets: what kind of mini creature follows the hero. */
    val pet: Species? = null,
)

object Catalog {
    val items: List<Cosmetic> = listOf(
        // Hats
        Cosmetic("hat_cap", Slot.HAT, "Red cap", 2, 0, 0xFFE53935),
        Cosmetic("hat_party", Slot.HAT, "Party hat", 1, 8, 0xFFAB47BC),
        Cosmetic("hat_flower", Slot.HAT, "Flower crown", 3, 12, 0xFFFF80AB),
        Cosmetic("hat_pirate", Slot.HAT, "Pirate hat", 4, 15, 0xFF263238),
        Cosmetic("hat_wizard", Slot.HAT, "Wizard hat", 5, 20, 0xFF3949AB),
        Cosmetic("hat_crown", Slot.HAT, "Golden crown", 6, 0, 0xFFFFC107),
        Cosmetic("hat_top", Slot.HAT, "Top hat", 7, 25, 0xFF212121),
        // Capes
        Cosmetic("cape_red", Slot.CAPE, "Hero cape", 4, 0, 0xFFE53935),
        Cosmetic("cape_blue", Slot.CAPE, "Ocean cape", 2, 10, 0xFF1E88E5),
        Cosmetic("cape_green", Slot.CAPE, "Leaf cape", 3, 12, 0xFF43A047),
        Cosmetic("cape_purple", Slot.CAPE, "Star cape", 6, 20, 0xFF8E24AA),
        Cosmetic("cape_rainbow", Slot.CAPE, "Rainbow cape", 8, 30, 0xFFFF4081),
        // Glasses
        Cosmetic("glasses_round", Slot.GLASSES, "Smart glasses", 1, 6, 0xFF5D4037),
        Cosmetic("glasses_sun", Slot.GLASSES, "Cool shades", 3, 10, 0xFF212121),
        Cosmetic("glasses_star", Slot.GLASSES, "Star glasses", 5, 15, 0xFFFFC107),
        Cosmetic("glasses_heart", Slot.GLASSES, "Heart glasses", 7, 18, 0xFFE91E63),
        // Colours (free from the start, or cheap)
        Cosmetic("color_default", Slot.COLOR, "Classic", 1, 0, 0),
        Cosmetic("color_sky", Slot.COLOR, "Sky blue", 1, 0, 0xFF81D4FA),
        Cosmetic("color_pink", Slot.COLOR, "Bubblegum", 1, 0, 0xFFF8BBD0),
        Cosmetic("color_lime", Slot.COLOR, "Lime", 1, 0, 0xFFC5E1A5),
        Cosmetic("color_sun", Slot.COLOR, "Sunshine", 2, 4, 0xFFFFE082),
        Cosmetic("color_grape", Slot.COLOR, "Grape", 3, 5, 0xFFCE93D8),
        Cosmetic("color_gold", Slot.COLOR, "Gold", 6, 15, 0xFFFFD54F),
        // Pets
        Cosmetic("pet_chick", Slot.PET, "Chick", 2, 0, pet = Species.CHICK),
        Cosmetic("pet_puppy", Slot.PET, "Puppy", 3, 15, pet = Species.PUPPY),
        Cosmetic("pet_kitty", Slot.PET, "Kitty", 4, 15, pet = Species.KITTY),
        Cosmetic("pet_dino", Slot.PET, "Baby dino", 5, 20, pet = Species.DINO),
        Cosmetic("pet_unicorn", Slot.PET, "Baby unicorn", 7, 25, pet = Species.UNICORN),
        Cosmetic("pet_robot", Slot.PET, "Robo pal", 8, 25, pet = Species.ROBOT),
    )

    fun byId(id: String?): Cosmetic? = items.firstOrNull { it.id == id }
    fun inSlot(slot: Slot) = items.filter { it.slot == slot }
    fun freeAt(level: Int) = items.filter { it.level == level && it.price == 0 }
    fun freeUpTo(level: Int) = items.filter { it.level <= level && it.price == 0 }
}

/** Powers come with levels. Story powers open lands; helper powers make challenges easier. */
enum class Power(val emoji: String, val label: String, val level: Int, val helper: Boolean, val describe: String) {
    OWL("🦉", "Owl friend", 1, true, "Owl says the question again, nice and slow."),
    SWIM("🏊", "Swim", 2, false, "Now you can swim to Letter Lagoon!"),
    JUMP("🦘", "Super jump", 3, false, "Now you can jump over the fence into the Robot Factory!"),
    WAND("🪄", "Magic wand", 3, true, "The magic wand makes one wrong answer disappear."),
    CLIMB("🧗", "Climb", 4, false, "Now you can climb Puzzle Peaks!"),
    LANTERN("🏮", "Lantern", 5, false, "Your lantern lights up the Word Caves!"),
    MAP("🗺️", "Treasure map", 5, true, "The map shows the robot's next move."),
    FLY("🦸", "Fly", 6, false, "You can fly! Let's visit the Cloud Castle!"),
}

/** The kinds of learning challenges a story can contain. */
enum class Subject { SPEECH, LETTERS, READING, NUMBERS, PATTERNS, ROBOT }

enum class Land(
    val title: String,
    val emoji: String,
    val subjects: List<Subject>,
    val needs: Power?,
    val sky: Long,
    val ground: Long,
    val decor: List<String>,
    /** Where the land sits on the world map, from 0 to 1. */
    val mapX: Float,
    val mapY: Float,
) {
    FOREST("Talking Forest", "🌳", listOf(Subject.SPEECH), null, 0xFFB3E5FC, 0xFF9CCC65, listOf("🌳", "🌲", "🍄", "🌼"), 0.13f, 0.55f),
    MEADOW("Number Meadow", "🌻", listOf(Subject.NUMBERS), null, 0xFFFFF9C4, 0xFFAED581, listOf("🌻", "🌷", "🦋", "🐝"), 0.36f, 0.25f),
    LAGOON("Letter Lagoon", "🏝️", listOf(Subject.LETTERS), Power.SWIM, 0xFFB2EBF2, 0xFFFFE0B2, listOf("🌴", "🐚", "🦀", "⛵"), 0.3f, 0.82f),
    FACTORY("Robot Factory", "🏭", listOf(Subject.ROBOT), Power.JUMP, 0xFFCFD8DC, 0xFFB0BEC5, listOf("⚙️", "🔩", "🔋", "💡"), 0.52f, 0.52f),
    PEAKS("Puzzle Peaks", "⛰️", listOf(Subject.PATTERNS), Power.CLIMB, 0xFFE1BEE7, 0xFFBCAAA4, listOf("⛰️", "🏔️", "🌲", "🦅"), 0.66f, 0.3f),
    CAVES("Word Caves", "🕯️", listOf(Subject.READING), Power.LANTERN, 0xFF5C6BC0, 0xFF8D6E63, listOf("💎", "🕯️", "🦇", "🪨"), 0.74f, 0.7f),
    CLOUDS("Cloud Castle", "🏰", Subject.entries.toList(), Power.FLY, 0xFFE3F2FD, 0xFFFFFFFF, listOf("☁️", "🌈", "⭐", "🏰"), 0.9f, 0.17f);

    fun unlocked(level: Int, openAll: Boolean = false) = openAll || needs == null || level >= needs.level

    /** How to point at this land for a child who can't read: by its picture, not its name. */
    val hint: String get() = when (this) {
        FOREST -> "the big trees"
        MEADOW -> "the sunflower"
        LAGOON -> "the palm tree island"
        FACTORY -> "the factory"
        PEAKS -> "the mountain"
        CAVES -> "the candle cave"
        CLOUDS -> "the castle in the clouds"
    }

    companion object {
        /**
         * Which land the map should point at: the first open land not visited yet,
         * otherwise the next open land after the last one played.
         */
        fun suggest(open: List<Land>, visited: Set<Land>, last: Land?): Land {
            if (open.isEmpty()) return FOREST
            open.firstOrNull { it !in visited }?.let { return it }
            val i = open.indexOf(last)
            return open[(i + 1) % open.size]
        }
    }
}

object Leveling {
    const val XP_PER_CHALLENGE = 10
    const val XP_FIRST_TRY_BONUS = 5
    const val XP_PER_QUEST = 20

    /** Total XP needed to *reach* [level]. Level 2 comes after about one quest. */
    fun xpFor(level: Int): Int = if (level <= 1) 0 else (2..level).sumOf { 25 + 25 * (it - 1) }

    fun levelFor(xp: Int): Int {
        var l = 1
        while (xp >= xpFor(l + 1)) l++
        return l
    }

    /** How far through the current level, from 0 to 1, for the XP bar. */
    fun progress(xp: Int): Float {
        val l = levelFor(xp)
        val start = xpFor(l)
        return (xp - start).toFloat() / (xpFor(l + 1) - start)
    }

    fun powersAt(level: Int) = Power.entries.filter { it.level <= level }
    fun newPowers(level: Int) = Power.entries.filter { it.level == level }
}
