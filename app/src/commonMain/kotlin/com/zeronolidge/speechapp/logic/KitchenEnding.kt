package com.zeronolidge.speechapp.logic

/**
 * Rory's Kitchen's own ending (Ruby's design fix, K2c): once Bear's taken his bite, no score or
 * summary screen and no tap needed. The pancake stack appears with steam, Bear takes a bite, his
 * sticker flies into the backpack, then Rory says goodbye and the story carries on by itself
 * (other voice stories keep their usual "tap the big green arrow" ending). [STACK]/[BITE]/
 * [STICKER_FLY]/[IN_BACKPACK] each hold the screen for one of the fixed spans below; [BYE] has no
 * fixed span of its own since it waits for Rory's line to finish speaking (see [LEAVE_DELAY_MS]
 * for what follows it, and [LEAVE] for when the story actually moves on).
 */
enum class KitchenEndStage { STACK, BITE, STICKER_FLY, IN_BACKPACK, BYE, LEAVE }

object KitchenEnding {
    /** The pancake stack and its steam settle onto the counter. */
    const val STACK_MS = 600

    /** Bear leans in; the bite-shaped notch appears in the top pancake. */
    const val BITE_MS = 1000

    /** The sticker peels up and arcs into the backpack. */
    const val FLY_MS = 900

    /** The backpack's little bounce once the sticker lands. */
    const val BOUNCE_MS = 300

    /** How long after Rory's bye line finishes before the story carries on by itself (about 1.5 to
     * 2.5 s, same shape as every other "it just carries on" wait in the app). */
    const val LEAVE_DELAY_MS = 2000

    /** [STACK], [BITE], [STICKER_FLY] and [IN_BACKPACK] in order, each with how long it holds the
     * screen before the next stage starts. [BYE] isn't timed here: it starts right after
     * [IN_BACKPACK]'s bounce and holds until Rory's line finishes. */
    val timedStages: List<Pair<KitchenEndStage, Int>> = listOf(
        KitchenEndStage.STACK to STACK_MS,
        KitchenEndStage.BITE to BITE_MS,
        KitchenEndStage.STICKER_FLY to FLY_MS,
        KitchenEndStage.IN_BACKPACK to BOUNCE_MS,
    )

    /** When the sticker lands in the backpack, measured from the moment [BITE] starts: the
     * designer's rule is "within 3 s of the bite starting". */
    val stickerLandsAfterBiteStartsMs: Int get() = BITE_MS + FLY_MS

    /** Total time from the end of the last beat (when [STACK] starts) to the sticker landing in
     * the backpack: must stay well under the designer's ~4 s ceiling (nothing to tap, no score). */
    val stackToLandedMs: Int get() = STACK_MS + BITE_MS + FLY_MS
}
