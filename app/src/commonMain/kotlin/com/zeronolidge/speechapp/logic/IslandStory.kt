package com.zeronolidge.speechapp.logic

/**
 * The island story layer. Every place keeps its games; Rory says one short line before a place's
 * story and one after (each well under ten seconds, no cutscenes). The first time a place is
 * finished it switches on the map from broken (sleepy, ~60% colour, one small sad detail) to
 * fixed (full colour, one small looping motion), Rory earns that place's costume, and one of the
 * eight party flags fills. All eight: a ~5 s Cloud Castle party, then play goes on. Replays are
 * always allowed, in any order. No reading, no failing, and nothing the child has to remember
 * from one session to the next (the map itself shows what is fixed).
 *
 * What "finishing a place" means: a place is finished the first time any story chapter set
 * there reaches its happy ending -- the next stop on the trail, a finished stop's ⭐ replay, or a
 * story opened from the place itself all count (every land has [Trail.STOPS_PER_LAND] trail
 * stops, and the first one he completes there fixes it). A chapter can't be failed (every
 * question ends with the answer shown), so every finished chapter counts. The area's own
 * practice games and the voice stories don't fix places: they stay free play. The "before" line
 * plays as a chapter at a not-yet-fixed place opens, the first time in each session (so he hears
 * it again on another day, nothing to remember); the "after" line plays back on the map, with
 * the fix moment.
 */
object IslandStory {
    data class Place(
        val land: Land,
        /** Rory's one line before the place's story (Aida's; recorded). */
        val before: String,
        /** The costume Rory earns there (drawn in Critter.kt; never in the buddy's closet catalog). */
        val costume: Cosmetic,
        /** The party flag's colour on the bunting (the place's own colour; never green). */
        val flag: Long,
    ) {
        /** Rory's one line after the first finish (Aida's; recorded). */
        val after: String get() = afterLines.getValue(land)
    }

    val places: List<Place> = listOf(
        Place(Land.FOREST, "The forest animals lost their voices!",
            Cosmetic("costume_leaf", Slot.HAT, "Leaf crown", 0xFF7CB342), 0xFFB9814A), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.MEADOW, "The balloons blew away, so let's count them back!",
            Cosmetic("costume_balloon", Slot.HAT, "Balloon hat", 0xFFFF7043), 0xFFFFB300), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.LAGOON, "The little letters lost their big letters in the waves!",
            Cosmetic("costume_sailor", Slot.HAT, "Sailor hat", 0xFFFFFFFF), 0xFF29B6F6), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.MARKET, "The shops can't open until someone pays!",
            Cosmetic("costume_apron", Slot.CAPE, "Shopkeeper apron", 0xFFE57373), 0xFFFF8A65), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.PEAKS, "The mountain path is jumbled!",
            Cosmetic("costume_helmet", Slot.HAT, "Climber helmet", 0xFFFFA726), 0xFFAB7BD6), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.FACTORY, "Rory's robot friends are stuck!",
            Cosmetic("costume_goggles", Slot.GLASSES, "Robot goggles", 0xFFFFB300), 0xFF4F8FD6), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.CAVES, "The cave went dark, and words light the lanterns!",
            Cosmetic("costume_lantern", Slot.HAT, "Lantern hat", 0xFF8D6E63), 0xFF7E6CD9), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
        Place(Land.CLOUDS, "The castle is getting ready for the island party!",
            Cosmetic("costume_crown", Slot.HAT, "Crown", 0xFFFFC107), 0xFFF06292), // NO_CLIP_YET (name: shown on the review sheet only, never spoken)
    )

    /**
     * The after lines (Aida's, recorded): each opens with "You did it!" or "You fixed it!", taking
     * turns along the trail so it never sounds the same twice in a row.
     */
    private val afterLines: Map<Land, String> = mapOf(
        Land.FOREST to "You did it! The birds are singing again!",
        Land.MEADOW to "You fixed it! All the balloons are back!",
        Land.LAGOON to "You did it! Here comes Pip's boat!",
        Land.MARKET to "You fixed it! The shops are open!",
        Land.PEAKS to "You did it! The path goes all the way up!",
        Land.FACTORY to "You fixed it! Beep boop! The gears are spinning!",
        Land.CAVES to "You did it! The cave is glowing!",
        Land.CLOUDS to "You fixed it! Up go the flags!",
    )

    fun place(land: Land): Place = places.first { it.land == land }

    /** The party starts on the first fix that fills the last of the eight flags. */
    fun partyNow(before: Set<Land>, after: Set<Land>) = before.size < places.size && after.size == places.size

    /** How long the fix moment's colour sweep takes. */
    const val SWEEP_MS = 1500

    /** The whole fix moment: the sweep, the costume popping on, and a moment to see it. */
    const val FIX_MS = 2600

    /** How long the Cloud Castle party plays, before the map is back to normal. */
    const val PARTY_MS = 5000

    /** Rory's one link line at the end of a cooking stop, tying it to the party (Aida's; recorded). */
    val linkLines: Map<String, String> = mapOf(
        "fruitsalad" to "Let's bring the fruit salad to the party!",
        "birthdaycake" to "Let's bring the cake to the party!",
    )
}

/** Test hooks only (review and deep links): "#test=island&fixed=N", "island-fix:LAND", "island-party". */
object IslandTest {
    /** Places shown fixed (every place open). */
    var fixed: Set<Land>? = null
    /** A fix moment to play as the map opens. */
    var fix: Land? = null
}
