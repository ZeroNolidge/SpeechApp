package com.zeronolidge.speechapp.logic

/**
 * The island story layer (Phase 1). Every place keeps its games; Rory says one short line before
 * a place's round and one after (each well under ten seconds, no cutscenes). The first time a
 * place is finished it switches on the map from broken (sleepy, ~60% colour, one small sad
 * detail) to fixed (full colour, one small looping motion), Rory earns that place's costume, and
 * one of the eight party flags fills. All eight: a ~5 s Cloud Castle party, then play goes on.
 * Replays are always allowed, in any order. No reading, no failing, and nothing the child has to
 * remember from one session to the next (the map itself shows what is fixed).
 */
object IslandStory {
    data class Place(
        val land: Land,
        /** Rory's one line before the place's round. */
        val before: String,
        /** Rory's one line after the first finish (draft: not yet approved or recorded). */
        val after: String,
        /** The costume Rory earns there (drawn in Critter.kt; never in the buddy's closet catalog). */
        val costume: Cosmetic,
        /** The party flag's colour on the bunting (the place's own colour; never green). */
        val flag: Long,
    )

    val places: List<Place> = listOf(
        Place(Land.FOREST, "The forest animals lost their voices!", "Listen! The birds are singing again!",
            Cosmetic("costume_leaf", Slot.HAT, "Leaf crown", 0xFF7CB342), 0xFFB9814A),
        Place(Land.MEADOW, "The balloons blew away, so let's count them back!", "All the balloons are back!",
            Cosmetic("costume_balloon", Slot.HAT, "Balloon hat", 0xFFFF7043), 0xFFFFB300),
        Place(Land.LAGOON, "The little letters lost their big letters in the waves!", "Here comes Pip's boat!",
            Cosmetic("costume_sailor", Slot.HAT, "Sailor hat", 0xFFFFFFFF), 0xFF29B6F6),
        Place(Land.MARKET, "The shops can't open until someone pays!", "The shops are open!",
            Cosmetic("costume_apron", Slot.CAPE, "Shopkeeper apron", 0xFFE57373), 0xFFFF8A65),
        Place(Land.PEAKS, "The mountain path is jumbled!", "The path goes all the way up!",
            Cosmetic("costume_helmet", Slot.HAT, "Climber helmet", 0xFFFFA726), 0xFFAB7BD6),
        Place(Land.FACTORY, "Rory's robot friends are stuck!", "Beep boop! The gears are spinning!",
            Cosmetic("costume_goggles", Slot.GLASSES, "Robot goggles", 0xFFFFB300), 0xFF4F8FD6),
        Place(Land.CAVES, "The cave went dark, and words light the lanterns!", "The cave is glowing!",
            Cosmetic("costume_lantern", Slot.HAT, "Lantern hat", 0xFF8D6E63), 0xFF7E6CD9),
        Place(Land.CLOUDS, "The castle is getting ready for the island party!", "Up go the flags!",
            Cosmetic("costume_crown", Slot.HAT, "Crown", 0xFFFFC107), 0xFFF06292),
    )

    fun place(land: Land): Place = places.first { it.land == land }

    /** The party starts on the first fix that fills the last of the eight flags. */
    fun partyNow(before: Set<Land>, after: Set<Land>) = before.size < places.size && after.size == places.size

    /** How long the Cloud Castle party plays, before the map is back to normal. */
    const val PARTY_MS = 5000L

    /** Rory's one link line in a story or cooking stop, tying it to the party (draft text). */
    val linkLines: Map<String, String> = mapOf(
        "fruit" to "Let's bring the fruit salad to the party!",
        "cake" to "Let's bring the cake to the party!",
    )
}

/** Test-only (review stills): "#test=island&fixed=N" shows the first N trail places fixed, every place open. */
object IslandTest {
    var fixed: Set<Land>? = null
}
