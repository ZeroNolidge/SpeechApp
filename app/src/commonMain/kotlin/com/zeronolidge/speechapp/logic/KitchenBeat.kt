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

/**
 * Rory's Kitchen's wooden counter (drawn by `BackdropExtras(Backdrop.KITCHEN)` in VoiceScenes.kt):
 * how far down the wall/window goes before the counter starts, as a fraction of the scene's own
 * height. Ruby (design fix K2a) found the counter clipped down behind the choice strip on a
 * landscape phone: VoiceStory's landscape lift (it moves the scene up, clear of the strip, and
 * clips off whatever scrolls above the top, normally just sky) ate into the counter because the
 * portrait wall was still half the scene. Landscape draws a shorter wall / taller counter so most
 * of what's left above the strip is counter, not wall; portrait is unchanged, the counter was
 * already big there.
 */
object KitchenLayout {
    const val COUNTER_TOP_PORTRAIT = 0.5f

    /** K2a had this at 0.3 (a short wall, mostly counter); K2d moved it down to 0.42, because the
     * landscape lift (VoiceStory's `lift`, clipping the scene's own top clear of the choice strip)
     * was eating into that short wall almost entirely, leaving Rory a sliver a few px tall above
     * the counter. K2e moved it down again to 0.46: K2d's own wall band still wasn't quite clear
     * of the lift on a short landscape phone (its `gameStrip` floor, `BalloonStripMinHeight`, is a
     * much bigger share of a short screen's height), and separately Rory's own Critter had been
     * drawn with `fillMaxWidth()`, which ignores [roryHeight] entirely and renders him several
     * times taller than his lane (fixed in `KitchenCharacters`, now `fillMaxHeight()`), so neither
     * fix alone was enough. 0.46 still clears [MIN_VISIBLE_FRACTION] with room to spare and stays
     * below [COUNTER_TOP_PORTRAIT] (the wall must stay shorter than portrait's). */
    const val COUNTER_TOP_LANDSCAPE = 0.46f

    /** The least share of the whole screen's height the counter must keep on screen in landscape
     * (Ruby's ask, K2a): a hard floor the VoiceStory landscape lift is tuned to clear. */
    const val MIN_VISIBLE_FRACTION = 0.35f

    fun counterTop(landscape: Boolean): Float = if (landscape) COUNTER_TOP_LANDSCAPE else COUNTER_TOP_PORTRAIT

    // --- Ruby's K2b: Bear (on his stool), Rory (behind the counter, chef's hat) and the
    // counter's own props, in explicit lanes so none of them ever overlap. Lanes below are
    // fractions of the kitchen scene's own box (0 to 1); [stageBox] rescales one onto the whole
    // stage once a choice strip is reserved below it (see VoiceStory's `gameStrip`).

    /** Bear's lane (his stool), the far end of the counter from the props: stays inside a fifth
     * of the scene's width, on the floor (K2e: the counter itself starts just to his right, see
     * [BackdropExtras] in VoiceScenes.kt, so he reads as sitting *beside* it, not floating over
     * its front). */
    private const val BEAR_RIGHT = 0.20f
    private const val BEAR_BOTTOM = 0.97f

    /** How far Bear's head pokes up past the counter top line (K2e): his head/shoulders should
     * read as level with the counter, not sitting well below it, so his box's top overlaps the
     * counter top by a little rather than stopping at [BEAR_BOTTOM] minus some fixed height. */
    private const val BEAR_HEAD_OVERLAP = 0.04f

    /** Rory's lane (with his chef's hat): centred behind the props, his head and shoulders a
     * band just above the counter top line, entirely clear of it (his own [Critter] is sized to
     * this band exactly, `fillMaxHeight()`, so nothing of him needs hiding behind the counter).
     * A fixed height, not the whole wall: landscape's lift (K2a) scrolls most of the wall
     * off-screen, so a band hugging the counter line stays visible there too, instead of a tall
     * Rory mostly cropped away. Landscape gets a shorter band than portrait (K2d, shrunk again at
     * K2e): even with [COUNTER_TOP_LANDSCAPE] moved down, the lift still clips off roughly its
     * own share of the scene's top (more of it on a short landscape phone, where the choice
     * strip's own floor height is a bigger share of the screen), so a shorter Rory sits entirely
     * below that clip line instead of a taller one straddling it (only its bottom sliver
     * surviving, or none of it at all). */
    private const val RORY_LEFT = 0.33f
    private const val RORY_RIGHT = 0.67f
    private const val RORY_HEIGHT_PORTRAIT = 0.20f
    private const val RORY_HEIGHT_LANDSCAPE = 0.09f
    private const val RORY_MARGIN = 0.015f

    private fun roryHeight(landscape: Boolean): Float = if (landscape) RORY_HEIGHT_LANDSCAPE else RORY_HEIGHT_PORTRAIT

    /** Where the counter's props (the bowl choice, the pan and what's on it, the berries) are
     * kept: the rest of the counter's width, clear of Bear's lane, always at or below the
     * counter top line. */
    private const val PROPS_LEFT = 0.40f
    private const val PROPS_RIGHT = 0.88f
    private const val PROP_SLOT_WIDTH = 0.2f
    private const val PROPS_MARGIN = 0.02f

    /** A box on screen, as fractions (0 at the left/top edge, 1 at the right/bottom): [overlaps]
     * is false for boxes that merely touch at an edge. */
    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float) {
        fun overlaps(other: Rect): Boolean = l < other.r && r > other.l && t < other.b && b > other.t
    }

    /** Bear's box (his stool), as a fraction of the kitchen scene's own box: from the floor up to
     * just past the counter top line (K2e), so his head reads level with the counter rather than
     * stopping well short of it. */
    fun bearBox(landscape: Boolean): Rect = Rect(0.01f, counterTop(landscape) - BEAR_HEAD_OVERLAP, BEAR_RIGHT, BEAR_BOTTOM)

    /** Rory's box (with his hat), as a fraction of the kitchen scene's own box. */
    fun roryBox(landscape: Boolean): Rect {
        val bottom = counterTop(landscape) - RORY_MARGIN
        return Rect(RORY_LEFT, bottom - roryHeight(landscape), RORY_RIGHT, bottom)
    }

    /** The "tap the bowl" choice's own slot, at the near end of the props band. */
    fun bowlBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(PROPS_LEFT, top, PROPS_LEFT + PROP_SLOT_WIDTH, BEAR_BOTTOM)
    }

    /** The pan's own slot (and whatever's on it), at the far end of the props band. */
    fun panBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(PROPS_RIGHT - PROP_SLOT_WIDTH, top, PROPS_RIGHT, BEAR_BOTTOM)
    }

    /** Rescales a box from the kitchen scene's own fractions onto the whole stage's, given how
     * much of the stage's height the scene itself keeps once a choice strip is reserved below it
     * ([sceneFraction]; 1f when no strip is reserved this beat). */
    fun stageBox(box: Rect, sceneFraction: Float): Rect = Rect(box.l, box.t * sceneFraction, box.r, box.b * sceneFraction)

    /** The choice strip's own band, reserved at the bottom of the stage while its tap tiles show. */
    fun stripBox(stripFraction: Float): Rect = Rect(0f, 1f - stripFraction, 1f, 1f)
}
