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

    /** The right choice glows throughout the tap phase (including mid-wiggle), from the moment
     * Rory's finished naming the problem -- not only once he's gotten it wrong once (K2l): a
     * 3-year-old shouldn't have to guess wrong first to be shown the answer. */
    val glow: Boolean get() = phase == KitchenPhase.TAP || phase == KitchenPhase.WIGGLE

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
    /** Distractor items for the "tap the bowl" beat's choices: a pan, a spoon (K2l: a coffee cup
     * doesn't belong in a toddler pancake kitchen; a pan is just as familiar and clearly not a bowl). */
    val distractors = listOf("pan", "spoon")

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

    /** K2i moved this up from 0.97: screenshots of the BITE lean ([bearLean]) showed Bear's
     * resting (non-lean) stool already sitting right at the scene's bottom edge, leaving no room
     * below him for the lean's own rotation to swing a corner down without crossing it. 0.90
     * keeps him fully inside the scene at rest and gives the lean enough clearance in every size
     * [bearLean] is tested against. */
    private const val BEAR_BOTTOM = 0.90f

    /** How far Bear's head pokes up past the counter top line (K2e): his head/shoulders should
     * read as level with the counter, not sitting well below it, so his box's top overlaps the
     * counter top by a little rather than stopping at [BEAR_BOTTOM] minus some fixed height. */
    private const val BEAR_HEAD_OVERLAP = 0.04f

    /** Rory's lane (with his chef's hat): centred behind the props. K2d/K2e kept shrinking his
     * height (down to a 9%-tall sliver in landscape) to fit him entirely above the counter top
     * line before the landscape lift clips the scene's own top, which read as a tiny Rory
     * standing ON the counter rather than a normal-sized one behind it. K2f draws him behind the
     * counter instead ([KitchenRory] drawn before [KitchenCounterFront] in VoiceScenes.kt, so the
     * counter's own paint covers his lower body, the way a real counter hides someone's legs):
     * his box can now be one full, un-shrunk size in both orientations ([RORY_HEIGHT]), reaching
     * down past the counter top line by design ([RORY_HIDDEN_FRACTION]) instead of stopping just
     * short of it. */
    private const val RORY_LEFT = 0.33f
    private const val RORY_RIGHT = 0.67f

    /** Rory's own height, as a fraction of the kitchen scene's own height: about a quarter, the
     * same in portrait and landscape now (K2f; no more separate landscape shrink). */
    private const val RORY_HEIGHT = 0.25f

    /** How much of [RORY_HEIGHT] sits below the counter top line, hidden behind the counter
     * (K2g, was 0.45): his puppet's own art leaves empty space above his antenna and his head
     * reaches a good way down past his chin before his shoulders start, so 0.45 hidden only left
     * his hat and eyes above the line (screenshots showed his mouth, chin and shoulders gone).
     * 0.25 clears his whole head (mouth and chin included) and his shoulders/upper chest, so the
     * counter front hides only his belly and legs, the way a waist-high counter should. */
    private const val RORY_HIDDEN_FRACTION = 0.25f

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

    /** Rory's box (with his hat), as a fraction of the kitchen scene's own box: reaches from
     * above the counter top line down past it ([RORY_HIDDEN_FRACTION]), since the counter is
     * drawn over his lower body (K2f), not clipped away by this box itself. */
    fun roryBox(landscape: Boolean): Rect {
        val ct = counterTop(landscape)
        return Rect(RORY_LEFT, ct - RORY_HEIGHT * (1f - RORY_HIDDEN_FRACTION), RORY_RIGHT, ct + RORY_HEIGHT * RORY_HIDDEN_FRACTION)
    }

    /** The part of [roryBox] that actually shows: above the counter top line, where the counter
     * can't hide him. The rest is deliberately behind the counter (K2f), so overlap checks
     * against the other kitchen elements (bear, bowl, pan) use this, not the full box: see
     * `bearRoryBowlAndPanNeverOverlapEachOther`. */
    fun roryVisibleBox(landscape: Boolean): Rect {
        val box = roryBox(landscape)
        return Rect(box.l, box.t, box.r, counterTop(landscape))
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

    /** The lean's own floor (Ruby: it must stay clearly visible even on a tight layout) and
     * ceiling: K2g's original whole-body ask was 20deg, but K2j's lean only tilts his head (see
     * below), so the same visible tilt reads fine a little smaller. [bearLean] picks whatever's
     * smaller than this ceiling, never less than the floor unless even the floor would push his
     * head past the counter (see [headRightEdgeUnits]) -- staying off the counter always wins, a
     * screenshot of K2i's whole-body version showed the opposite priority let him tip past it. */
    const val MIN_LEAN_DEG = 10f
    const val MAX_LEAN_DEG = 18f

    /** K2j's own ask: shift his head a little further forward than the rotation alone, toward
     * the stack, at full lean (local art units, same scale as [headRightEdgeUnits]). */
    const val LEAN_FORWARD_UNITS = 6f

    /** The least share of the scene's own height that must stay clear below Bear at rest. */
    const val BOTTOM_MARGIN_FRACTION = 0.02f

    /** Critter's own head circle (`circle(50f, 63f, 30f, ...)` in Critter.kt) and the neck pivot
     * its bite lean rotates about (`o(50f, 90f)`, non-boxy species): local 100-wide/140-tall art
     * units. Must match Critter.kt's own numbers exactly, or [headRightEdgeUnits] models a
     * different puppet than the one actually drawn -- K2i's bug, in a new place. [HEAD_HALF_WIDTH]
     * sits a little past the head circle's own radius (30): BEAR's round ears (`Ears.ROUND`,
     * centred at x=71, radius 10) poke out to x=81, plus a small safety margin for the baked
     * art's own soft edge. */
    private const val ART_H = 140f
    private const val HEAD_CENTER_X = 50f
    private const val HEAD_CENTER_Y = 63f
    private const val NECK_PIVOT_Y = 90f
    private const val HEAD_HALF_WIDTH = 33f

    /**
     * How far the head circle's own right-most x sits, in Critter's local art units, after
     * rotating the head+arms group by [angleDeg] about the neck pivot and shifting it forward by
     * [forwardUnits] -- the exact transform `KitchenBear` (VoiceScenes.kt) hands to `Critter`'s
     * own `lean`/`leanForward` parameters, via Compose's `rotate`/`translate`, not a separate
     * model of it (K2i rotated and shifted the *whole* Critter about a different pivot than this
     * assumed, which is how a passing unit test still shipped Bear tipping past the counter on
     * screen). [bearLean] below caps against this same function, and the KITCHEN bite log
     * (VoiceStory.kt) prints the real measured head box beside it, so a drift between this
     * prediction and the real drawing would show up as a disagreement, not just a math check
     * passing in isolation.
     */
    fun headRightEdgeUnits(angleDeg: Float, forwardUnits: Float): Float {
        val rad = angleDeg * kotlin.math.PI.toFloat() / 180f
        val pivotToHead = NECK_PIVOT_Y - HEAD_CENTER_Y
        return HEAD_CENTER_X + pivotToHead * kotlin.math.sin(rad) + forwardUnits + HEAD_HALF_WIDTH
    }

    /** Bear's bite lean (K2j), bound by [bearLean]: [angleDeg] the extra clockwise rotation his
     * head+arms (only; his stool-seated lower body never moves, see `KitchenBear` in
     * VoiceScenes.kt) turn by on top of his constant facing tilt, and [forwardUnits] how far that
     * same group shifts forward, both at full lean (`leanAmount` 1f; `KitchenEnding` scales both
     * down for the ease-in/settle). */
    data class BearLean(val angleDeg: Float, val forwardUnits: Float)

    /**
     * How far Bear can lean his head (and arms) toward the stack without [headRightEdgeUnits]
     * passing [counterLeftX], for a Bear box of [bearBox] (his own on-screen box, in scene px;
     * K2j: his lower body/stool never moves during the lean, so unlike K2i's whole-body version
     * there is no bottom-edge case left to check here). Converts [counterLeftX] into the same
     * local art units [headRightEdgeUnits] works in (via the box's own height, since Critter
     * keeps its 100-wide/140-tall aspect ratio whatever size it's drawn at), then finds the
     * largest shared fraction of [MAX_LEAN_DEG]/[LEAN_FORWARD_UNITS] (scaled together) whose
     * predicted head edge still stays a small margin inside that line.
     */
    /** K2k: screenshots taken at full lean (BITE) measured Bear's head ending up about 25px past
     * the counter's own left edge on an iPad-size stage (`1024x768` landscape, `768x1024`
     * portrait) -- a drift between the modelled head circle ([headRightEdgeUnits]) and Critter's
     * real baked/rotated art that [bearLean]'s own tiny (1-unit, a couple of px) safety margin
     * doesn't cover. Bear's own lane right up against the counter at rest (K2i/K2j) leaves no
     * room to add a bigger margin there without the lean itself eating it straight back up (more
     * room just lets [bearLean] lean him further, see K2k's own log); shrinking the width his
     * puppet is fitted to by this much (as a fraction of the stage's own width, so it scales to
     * any size) and pinning him to his lane's own *left* edge (`KitchenBear` in VoiceScenes.kt,
     * `Alignment.BottomStart` in place of `BottomCenter`) instead opens a real gap before the
     * counter that stays a gap however far he leans, without moving his lane's left edge (already
     * tight, K2i) or the counter's own (tied to [BEAR_RIGHT], never shifts). The runtime clamp on
     * his real measured head bounds (`onHeadBounds`, VoiceScenes.kt) is still the actual
     * guarantee against his head crossing the counter; this is slack so that clamp is never
     * needed at ordinary sizes. */
    const val BEAR_SHIFT_FRACTION = 0.035f

    fun bearLean(bearBox: Rect, counterLeftX: Float): BearLean {
        val u = (bearBox.b - bearBox.t) / ART_H
        if (u <= 0f) return BearLean(0f, 0f)
        val roomUnits = (counterLeftX - bearBox.l) / u - 1f // a 1-unit safety margin
        if (headRightEdgeUnits(MAX_LEAN_DEG, LEAN_FORWARD_UNITS) <= roomUnits) return BearLean(MAX_LEAN_DEG, LEAN_FORWARD_UNITS)
        if (headRightEdgeUnits(0f, 0f) >= roomUnits) return BearLean(0f, 0f)
        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (headRightEdgeUnits(MAX_LEAN_DEG * mid, LEAN_FORWARD_UNITS * mid) <= roomUnits) lo = mid else hi = mid
        }
        return BearLean(MAX_LEAN_DEG * lo, LEAN_FORWARD_UNITS * lo)
    }
}
