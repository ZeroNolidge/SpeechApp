package com.zeronolidge.speechapp.logic

/** How an answer tile looks. Only [RIGHT] is green, and only the correct answer can be [RIGHT]. */
enum class TileLook { PLAIN, RIGHT, GLOW, GREY }

/**
 * The one rule every quiz uses to colour its answer tiles, so a wrong answer can never turn
 * green:
 *  - solved by tapping the correct answer: the correct one is green (RIGHT);
 *  - 2nd miss (hint time): the correct one glows, the rest stay plain;
 *  - answer shown after the 3rd miss: the correct one glows, the rest are grey;
 *  - a tile the magic wand removed is grey.
 */
fun tileLook(isAnswer: Boolean, misses: Int, solvedByTap: Boolean, revealed: Boolean, wanded: Boolean): TileLook = when {
    isAnswer && solvedByTap && !revealed -> TileLook.RIGHT
    isAnswer && (revealed || misses == 2) -> TileLook.GLOW
    !isAnswer && (revealed || wanded) -> TileLook.GREY
    else -> TileLook.PLAIN
}

/** What a tap on an answer tile did. */
enum class TapResult { RIGHT, TRY_AGAIN, HINT, SHOWN, IGNORED }

/**
 * One quiz round's state, and the one rule for taps (used by every quiz screen): a tap is
 * checked against the correct answer first; only the correct answer is ever RIGHT. Wrong taps go
 * 1st TRY_AGAIN, 2nd HINT (the correct one glows), 3rd SHOWN (the correct one is shown and the
 * round ends). After the round ends, taps are IGNORED.
 */
data class QuizState(val misses: Int = 0, val solved: Boolean = false, val revealed: Boolean = false) {
    fun tap(isAnswer: Boolean): Pair<QuizState, TapResult> = when {
        solved -> this to TapResult.IGNORED
        isAnswer -> copy(solved = true) to TapResult.RIGHT
        misses == 0 -> copy(misses = 1) to TapResult.TRY_AGAIN
        misses == 1 -> copy(misses = 2) to TapResult.HINT
        else -> copy(misses = misses + 1, solved = true, revealed = true) to TapResult.SHOWN
    }

    fun look(isAnswer: Boolean, wanded: Boolean = false): TileLook =
        tileLook(isAnswer, misses, solvedByTap = solved && !revealed, revealed = revealed, wanded = wanded)
}
