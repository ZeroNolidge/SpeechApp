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
