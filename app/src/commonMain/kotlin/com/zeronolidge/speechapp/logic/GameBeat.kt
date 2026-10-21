package com.zeronolidge.speechapp.logic

/**
 * The phases of a game beat (see [VoiceBeat.game]), in order: Rory names the puzzle, it waits for
 * a tap (no timers), a wrong tap wiggles and comes back with the right choice glowing, a second
 * wrong tap shows the answer, then the usual talk turn plays out, the fix (and sticker) close the
 * beat, and the story carries on. Nothing ever fails: every path reaches [DONE].
 */
enum class GameBeatPhase { NAME, PUZZLE, WIGGLE, SHOW_ANSWER, TALK, CLOSE, DONE }

/**
 * A game beat's state (pure, no timers; see [GameBeatPhase]):
 *  - NAME -> PUZZLE: [named].
 *  - PUZZLE, right tap: straight to TALK. Wrong tap, 1st time: WIGGLE; wrong tap, 2nd time:
 *    SHOW_ANSWER. See [tap].
 *  - WIGGLE -> PUZZLE, with the right choice glowing ([glow]): [wiggled].
 *  - SHOW_ANSWER -> TALK: [shown].
 *  - TALK -> CLOSE: [talked].
 *  - CLOSE -> DONE: [closed].
 * A tap outside the PUZZLE phase is ignored, so it can never fail or get stuck.
 */
data class GameBeatState(val phase: GameBeatPhase = GameBeatPhase.NAME, val misses: Int = 0) {
    /** Rory's one short line (the beat's [VoiceBeat.problem]) has finished: the puzzle is shown. */
    fun named(): GameBeatState = if (phase == GameBeatPhase.NAME) copy(phase = GameBeatPhase.PUZZLE) else this

    /** A tap on one of the choice tiles; [isAnswer] says whether it was the right one. */
    fun tap(isAnswer: Boolean): GameBeatState = when {
        phase != GameBeatPhase.PUZZLE -> this
        isAnswer -> copy(phase = GameBeatPhase.TALK)
        misses == 0 -> copy(phase = GameBeatPhase.WIGGLE, misses = 1)
        else -> copy(phase = GameBeatPhase.SHOW_ANSWER, misses = misses + 1)
    }

    /** The right choice glows from the first wrong tap on (used for the puzzle tiles' look). */
    val glow: Boolean get() = misses >= 1

    /** The wiggle has played: back to the puzzle, with the right choice now glowing. */
    fun wiggled(): GameBeatState = if (phase == GameBeatPhase.WIGGLE) copy(phase = GameBeatPhase.PUZZLE) else this

    /** Rory has shown (and said) the answer: on to the usual talk turn. */
    fun shown(): GameBeatState = if (phase == GameBeatPhase.SHOW_ANSWER) copy(phase = GameBeatPhase.TALK) else this

    /** The talk turn is done (voice heard, or "I said it!"): the fix plays out. */
    fun talked(): GameBeatState = if (phase == GameBeatPhase.TALK) copy(phase = GameBeatPhase.CLOSE) else this

    /** The close (fix + sticker) has played: the beat, and the story, carry on. */
    fun closed(): GameBeatState = if (phase == GameBeatPhase.CLOSE) copy(phase = GameBeatPhase.DONE) else this

    /** Right first time, with no wrong taps at all: what the quiet ladder (see [Ladder.GAME_PATTERN]) scores as right. */
    val rightFirstTry: Boolean get() = misses == 0
}
