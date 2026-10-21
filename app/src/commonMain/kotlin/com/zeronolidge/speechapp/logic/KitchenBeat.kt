package com.zeronolidge.speechapp.logic

import kotlin.random.Random

/**
 * The flows a "kitchen" beat ([VoiceBeat.kitchen]) can play before (or instead of) its usual
 * single talk turn, in Rory's Kitchen (pancakes for Bear's picnic). Mirrors [GameBeatState]'s
 * "nothing ever fails" shape, generalised so one state machine ([KitchenBeatState]) covers all
 * five:
 *  - [TAP_CHOICE] ("Tap the bowl!"): a tap among 2 or 3 items, one right (the beat's own word;
 *    [VoiceBeat.kitchenChoices] names the distractors). Wrong taps wiggle, then show the answer,
 *    exactly like [GameBeatState]'s puzzle. One talk turn follows.
 *  - [COUNT] ("Count 3 berries in"): taps with no wrong answer, Rory counting each one aloud
 *    ("one, two, three!"); no talk turn at all, since it's a counting turn, not a speech target.
 *  - [STIR] ("mix!") and [BUBBLES] ("pop!"): no tap phase, but several one-word talk turns in a
 *    row (his stirs, his pops) before the fix plays.
 *  - [FLIP] ("Tap the pancake, then flip!"): one plain tap (no wrong answer), then one talk turn.
 * How many taps or talk turns a beat's flow needs is [VoiceBeat.kitchenGoal].
 */
enum class KitchenFlow { TAP_CHOICE, COUNT, STIR, BUBBLES, FLIP }

/** True when [KitchenFlow]'s tap phase can be tapped wrong (only [KitchenFlow.TAP_CHOICE]: a real
 * choice among distractors; the others are plain taps that always count, so they can never fail). */
fun KitchenFlow.hasWrongTaps(): Boolean = this == KitchenFlow.TAP_CHOICE

/** How many taps this flow needs before moving on (0: no tap phase at all). */
fun KitchenFlow.tapGoal(goal: Int): Int = when (this) {
    KitchenFlow.TAP_CHOICE, KitchenFlow.FLIP -> 1
    KitchenFlow.COUNT -> goal
    KitchenFlow.STIR, KitchenFlow.BUBBLES -> 0
}

/** How many talk turns this flow needs before the fix plays (0: no talk turn at all, only [COUNT]). */
fun KitchenFlow.talkGoal(goal: Int): Int = when (this) {
    KitchenFlow.COUNT -> 0
    KitchenFlow.TAP_CHOICE, KitchenFlow.FLIP -> 1
    KitchenFlow.STIR, KitchenFlow.BUBBLES -> goal
}

enum class KitchenPhase { NAME, TAP, WIGGLE, SHOW_ANSWER, TALK, CLOSE, DONE }

/**
 * A kitchen beat's state (pure, no timers; see [KitchenPhase]):
 *  - NAME -> TAP (a flow with a tap phase) or TALK (no tap phase but a talk turn) or straight to
 *    CLOSE (neither, [KitchenFlow.COUNT] once its taps are in): [named].
 *  - TAP, right tap ([KitchenFlow.hasWrongTaps] false: every tap is "right"): one more counted;
 *    once the flow's tap goal is reached, on to TALK or CLOSE. Wrong tap (only possible when
 *    [KitchenFlow.hasWrongTaps]), 1st time: WIGGLE; 2nd time: SHOW_ANSWER. See [tap].
 *  - WIGGLE -> TAP, with the right choice glowing ([glow]): [wiggled].
 *  - SHOW_ANSWER -> TALK or CLOSE, same as a right tap: [shown].
 *  - TALK, one try done (his voice, or "I said it!"): one more counted; once the flow's talk goal
 *    is reached, on to CLOSE, otherwise stays at TALK for the next try (mix again, pop again): [talked].
 *  - CLOSE -> DONE: [closed].
 * A [tap] outside TAP, or a [talked] outside TALK, is ignored, so it can never fail or get stuck.
 */
data class KitchenBeatState(
    val flow: KitchenFlow,
    val goal: Int = 1,
    val phase: KitchenPhase = KitchenPhase.NAME,
    val misses: Int = 0,
    val tapsDone: Int = 0,
    val talksDone: Int = 0,
) {
    private val tapGoal get() = flow.tapGoal(goal)
    private val talkGoal get() = flow.talkGoal(goal)
    private fun afterTaps() = if (talkGoal > 0) KitchenPhase.TALK else KitchenPhase.CLOSE

    /** Rory's one short line (the beat's [VoiceBeat.problem]) has finished. */
    fun named(): KitchenBeatState =
        if (phase != KitchenPhase.NAME) this else copy(phase = if (tapGoal > 0) KitchenPhase.TAP else afterTaps())

    /** A tap, in the TAP phase; [isAnswer] says whether it was the right one (always true for a
     * flow with [KitchenFlow.hasWrongTaps] false: there is no wrong tap to make). */
    fun tap(isAnswer: Boolean): KitchenBeatState = when {
        phase != KitchenPhase.TAP -> this
        isAnswer -> {
            val done = tapsDone + 1
            if (done >= tapGoal) copy(tapsDone = done, phase = afterTaps()) else copy(tapsDone = done)
        }
        !flow.hasWrongTaps() -> this
        misses == 0 -> copy(phase = KitchenPhase.WIGGLE, misses = 1)
        else -> copy(phase = KitchenPhase.SHOW_ANSWER, misses = misses + 1)
    }

    /** The right choice glows from the first wrong tap on. */
    val glow: Boolean get() = misses >= 1

    /** The wiggle has played: back to tapping, with the right choice now glowing. */
    fun wiggled(): KitchenBeatState = if (phase == KitchenPhase.WIGGLE) copy(phase = KitchenPhase.TAP) else this

    /** Rory has shown (and said) the answer: on, same as a right tap. */
    fun shown(): KitchenBeatState = if (phase == KitchenPhase.SHOW_ANSWER) copy(tapsDone = tapGoal, phase = afterTaps()) else this

    /** One talk try is done (his voice, or "I said it!"): one more counted, or on to CLOSE. */
    fun talked(): KitchenBeatState = if (phase != KitchenPhase.TALK) this else {
        val done = talksDone + 1
        if (done >= talkGoal) copy(talksDone = done, phase = KitchenPhase.CLOSE) else copy(talksDone = done)
    }

    /** The close (fix + sticker) has played: the beat, and the story, carry on. */
    fun closed(): KitchenBeatState = if (phase == KitchenPhase.CLOSE) copy(phase = KitchenPhase.DONE) else this

    /** Right first time, with no wrong taps at all. */
    val rightFirstTry: Boolean get() = misses == 0
}

/**
 * The "tap the bowl" round ([KitchenFlow.TAP_CHOICE]): the beat's own word among 2 or 3 items on
 * the counter, shuffled so the answer isn't always in the same spot (mirrors [BalloonGame.make]).
 */
object KitchenGame {
    /** Distractor items for the "tap the bowl" beat's choices: a cup, a spoon. */
    val distractors = listOf("cup", "spoon")

    fun tapRound(answer: String, count: Int, rnd: Random): Choice<String> = choiceOf(answer, distractors, count, rnd)
}
