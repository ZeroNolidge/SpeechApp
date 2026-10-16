package com.zeronolidge.speechapp.logic

/** Where Pip hides, from far away to close by. */
enum class PipSpot(val label: String) { BUSH("bush"), ROCK("rock"), LOG("log") }

/**
 * One speaking turn of "Lost Puppy Pip". [spot] is where Pip is hiding (null for the goodnight
 * turns at the end); [stage] counts the calls at that spot (the last one folds it open).
 */
data class PipTurn(
    val index: Int,
    val spot: PipSpot?,
    val stage: Int,
    /** What he calls out. */
    val call: String,
    /** What Rory says to ask for the call. */
    val prompt: String,
    /** What happens after the call (every try works, heard or not). */
    val reaction: String,
)

/**
 * "Lost Puppy Pip": he calls "Pip!" or "Here, puppy!", and EVERY call makes Pip hop a little
 * closer: out from behind the bush, past the rock, to the log, until Pip jumps out. The spots
 * rustle and fold open as Pip goes by (extra fun on top). Then he says goodnight and Pip falls
 * asleep. 9 calls and a goodnight: 10 turns, 3 to 5 minutes; every try succeeds. His own words
 * and target-sound words come first, as treats that tempt Pip out.
 */
object PipStory {
    /** Calls at each hiding spot: Pip hops on every one, and the last one folds the spot open. */
    const val CALLS_PER_SPOT = 3

    /** How many hops Pip makes in all, one per call (the last one jumps out of the log). */
    val HOPS get() = CALLS_PER_SPOT * PipSpot.entries.size

    fun intro(hero: String) =
        "Oh no! Pip the puppy is lost, and Pip is hiding! $hero, let's call Pip. Every time you call, Pip hops closer."

    fun ending(hero: String) = "Pip is fast asleep, all cozy. Thank you for finding Pip, $hero! Pip got a new tag on the collar!"

    /**
     * The turns. [lures] are his own words, then his target-sound words, most important first:
     * one per hiding spot (the middle call), as the treat or toy that tempts Pip closer.
     */
    fun turns(speechStep: Int, lures: List<String> = emptyList()): List<PipTurn> {
        val calls = when (speechStep) {
            0 -> listOf("Pip!", "Pip!")
            1 -> listOf("Pip!", "Here, puppy!")
            else -> listOf("Here, puppy!", "Come here, Pip!")
        }
        val treats = lures.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val list = mutableListOf<PipTurn>()
        PipSpot.entries.forEachIndexed { s, spot ->
            for (stage in 0 until CALLS_PER_SPOT) {
                val treat = if (stage == 1) treats.getOrNull(s) else null
                val call = treat?.let { "$it!" } ?: calls[(s * CALLS_PER_SPOT + stage) % calls.size]
                val prompt = when {
                    treat != null -> "Pip loves $treat! Say: $call"
                    stage == 0 -> if (s == 0) "Is Pip in the bush? Call: $call" else "Pip is by the ${spot.label}! Call again: $call"
                    stage == 1 -> "Again! Say: $call"
                    else -> "One more time: $call"
                }
                val reaction = when (stage) {
                    0 -> "Hop! Pip hops closer! The ${spot.label} rustles!"
                    1 -> "Hop hop! Pip peeks out and hops closer!" + (treat?.let { " Pip wants $it!" } ?: "")
                    else -> if (spot == PipSpot.LOG) "Hop! The log opens up... It's Pip! Pip jumps out! Woof woof!"
                    else "Hop! The ${spot.label} folds open, and Pip hops closer, to the ${PipSpot.entries[s + 1].label}!"
                }
                list += PipTurn(list.size, spot, stage, call, prompt, reaction)
            }
        }
        val night = if (speechStep == 0) "Night night!" else "Night night, Pip!"
        list += PipTurn(list.size, null, 0, night, "Pip is sleepy. Let's say goodnight, softly: $night", "Pip yawns, curls up and falls asleep.")
        return list
    }
}
