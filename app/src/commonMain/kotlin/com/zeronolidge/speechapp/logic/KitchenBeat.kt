package com.zeronolidge.speechapp.logic

import kotlin.random.Random

/**
 * The flows a "kitchen" beat ([VoiceBeat.kitchen]) can play in place of its usual single talk
 * turn, in Rory's Kitchen (pancakes for Bear's picnic). Mirrors [GameBeatState]'s "nothing ever
 * fails" shape, generalised so one state machine ([KitchenBeatState]) covers all five. Timothy's
 * iPad notes (K3a): a step that both makes him tap AND then talk reads as "the tiles vanished and
 * a strange listening screen appeared" to a child who can't read the caption explaining why, so
 * every kitchen flow is now either tap-only or talk-only, never both:
 *  - [TAP_CHOICE] ("Tap the bowl!"): a tap among 2 or 3 items, one right (the beat's own word;
 *    [VoiceBeat.kitchenChoices] names the distractors). Wrong taps wiggle, then show the answer,
 *    exactly like [GameBeatState]'s puzzle. No talk turn follows: the fix plays straight from the
 *    tap, same as [COUNT].
 *  - [COUNT] ("Count 3 berries in"): taps with no wrong answer, Rory counting each one aloud
 *    ("one, two, three!"); no talk turn at all, since it's a counting turn, not a speech target.
 *  - [STIR] ("mix!") and [BUBBLES] ("pop!"): no tap phase, but several one-word talk turns in a
 *    row (his stirs, his pops) before the fix plays.
 *  - [FLIP] ("Tap the pancake, then flip!"): one plain tap (no wrong answer). No talk turn, same
 *    as [TAP_CHOICE]/[COUNT].
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

/** How many talk turns this flow needs before the fix plays (0: no talk turn at all; K3a: every
 * tap flow is now 0, so a tap step never slides into a talk/listening turn afterward). */
fun KitchenFlow.talkGoal(goal: Int): Int = when (this) {
    KitchenFlow.COUNT, KitchenFlow.TAP_CHOICE, KitchenFlow.FLIP -> 0
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
    /** K4: Timothy's iPad screenshots (1024x768 landscape) measured the counter's real drawn top
     * at only ~28% down the VISIBLE scene (after VoiceStory's landscape lift clips the scene's own
     * top clear of the choice strip, see [onStage]) -- the counter filled nearly three quarters of
     * what's on screen, leaving Rory a sliver above it. [COUNTER_TOP_PORTRAIT]/[COUNTER_TOP_LANDSCAPE]
     * are local fractions of the scene's own (unclipped) box, not of what's actually visible;
     * portrait never clips (see [onStage]), so its local fraction IS the visible one, but
     * landscape's isn't -- [onStage] maps a landscape local fraction back to where it really lands
     * once the lift and the clip are both accounted for. Both are picked so the counter's real
     * drawn top lands at roughly 60-65% of the visible scene's own height (so the counter band
     * itself is only the lower ~35-40%), matching Timothy's ask that Rory show from the chest up
     * and Bear's stool read as sitting at counter height, not far below it. */
    const val COUNTER_TOP_PORTRAIT = 0.62f

    /** K4: landscape's local fraction ends up BIGGER than portrait's now (the opposite of K2a's
     * old "shorter wall" rule), because the lift clips away local fractions below its own clip
     * line (`onStage`'s own `strip/stageHeight`) before anything is visible at all -- to land the
     * counter's real on-screen position at the same ~60-65%-down-the-visible-scene spot portrait
     * reaches directly, landscape's local line has to sit further down the full (partly-clipped)
     * box to begin with. See `KitchenLayoutTest.landscapeCounterTopLandsInTheSameVisibleBandAsPortrait`
     * for the real, [onStage]-based check this was tuned against, at the sizes Timothy reported. */
    const val COUNTER_TOP_LANDSCAPE = 0.70f

    /** The least share of the whole screen's height the counter must keep on screen in landscape
     * (Ruby's ask, K2a): a hard floor the VoiceStory landscape lift is tuned to clear. K4 lowered
     * this along with [COUNTER_TOP_LANDSCAPE]'s own rise: the counter is deliberately a smaller
     * local share of the scene now (so more of it is open wall, room for Rory), but still clears
     * this floor with margin. */
    const val MIN_VISIBLE_FRACTION = 0.26f

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

    /** K4: Timothy's screenshots showed Bear sitting on a low stool far below the counter (his
     * seat about 45% of the scene's own height below the counter top) -- he read as sitting on
     * the floor beside the counter, not at it. [BEAR_SEAT_GAP] puts his *seat* (not his head, see
     * K2e's old [bearBox]) just below the counter top line instead, as if his stool were a tall
     * one that brings him up to counter height, beside its left end. */
    private const val BEAR_SEAT_GAP = 0.05f

    /** Bear's own body height (head to seat), as a fraction of the scene's own height: tall enough
     * that his head (and ears) clearly rise above the counter top once his seat sits at
     * [BEAR_SEAT_GAP] below it. */
    private const val BEAR_HEIGHT = 0.30f

    /** The real floor line his stool's legs reach down to (K4): drawn separately from his own body
     * box ([bearBox], which now ends at his seat, not the floor) in `KitchenBear`
     * (VoiceScenes.kt), a tall stool spanning from his seat down to here, so "stool feet on the
     * floor line" and "seat at counter height" can both be true at once. */
    const val BEAR_FLOOR = 0.92f

    /** Rory's lane (with his chef's hat): his own strip of the counter, left of the props.
     * K2d/K2e kept shrinking his height (down to a 9%-tall sliver in landscape) to fit him
     * entirely above the counter top line before the landscape lift clips the scene's own top,
     * which read as a tiny Rory standing ON the counter rather than a normal-sized one behind it.
     * K2f draws him behind the counter instead ([KitchenRory] drawn before [KitchenCounterFront]
     * in VoiceScenes.kt, so the counter's own paint covers his lower body, the way a real counter
     * hides someone's legs): his box can now be one full, un-shrunk size in both orientations
     * ([RORY_HEIGHT]), reaching down past the counter top line by design
     * ([RORY_HIDDEN_FRACTION]) instead of stopping just short of it.
     *
     * K3c: Timothy's screenshots (1024x768 and 768x1024) showed the bowl, milk and berries
     * covering Rory's face -- this lane used to be centred ([0.33, 0.67]) right on top of the
     * bowl station ([0.40, 0.60] at the time), since nothing had ever checked Rory's own visible
     * box against the props themselves (only bear/bowl/pan were in
     * `bearRoryBowlAndPanNeverOverlapEachOther`'s own box list). Moving him into his own strip,
     * left of both stations entirely, is the fix a centred-behind-the-props lane could never give
     * without shrinking him down to fit between the two (K3b's own station widths leave no gap
     * big enough in the middle). */
    private const val RORY_LEFT = 0.30f
    private const val RORY_RIGHT = 0.45f

    /** Rory's own height, as a fraction of the kitchen scene's own height: the same in portrait
     * and landscape (K2f; no more separate landscape shrink). K4 grew this from a quarter: with
     * the counter top itself moved down (see [COUNTER_TOP_LANDSCAPE]/[COUNTER_TOP_PORTRAIT]), a
     * bigger Rory reads as standing at a normal-sized counter instead of a tall one looming over a
     * tiny one. */
    private const val RORY_HEIGHT = 0.34f

    /** How much of [RORY_HEIGHT] sits below the counter top line, hidden behind the counter (K2g
     * was 0.45, K4 lowers it further from 0.25): Timothy's ask was chest-up, both arms included --
     * 0.18 clears his whole head, both shoulders and arms and most of his chest, so the counter
     * front hides only his lower chest/belly and legs, the way a waist-high counter should. */
    private const val RORY_HIDDEN_FRACTION = 0.18f

    /** The bowl station (the bowl choice, milk, batter while it's being mixed, the berries
     * falling in): its own strip of the counter, clear of Rory's lane ([RORY_RIGHT]) on one side
     * and the pan station ([PAN_LEFT]) on the other (K3c: K3b's props band used to sit right
     * under Rory's old centred lane). */
    private const val BOWL_LEFT = 0.52f
    private const val BOWL_RIGHT = 0.70f

    /** The pan station (the pan itself and everything cooking in it): clear of the bowl station
     * on one side and the counter's own right edge on the other (K3c). */
    private const val PAN_LEFT = 0.74f
    private const val PAN_RIGHT = 0.94f

    /** How far below the counter-top line the props' own tap-target slots start (K2b) -- and,
     * since K3b, also where every prop's own *bottom edge* actually rests ([propBox]): a thin
     * "counter surface" band right under the line, not spread down toward [BEAR_FLOOR] (K2b's
     * slots are tall enough for a comfortable tap target, but a prop drawn anywhere in that much
     * height read as floating, not standing on the counter). Public so [KitchenLayoutTest] can
     * check against it directly rather than guessing the number. */
    const val PROPS_MARGIN = 0.02f

    /** A box on screen, as fractions (0 at the left/top edge, 1 at the right/bottom): [overlaps]
     * is false for boxes that merely touch at an edge. */
    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float) {
        fun overlaps(other: Rect): Boolean = l < other.r && r > other.l && t < other.b && b > other.t
    }

    /** Bear's own body box (K4: his *seat*, not his floor -- see [BEAR_SEAT_GAP]/[BEAR_HEIGHT]),
     * as a fraction of the kitchen scene's own box: his seat sits just below the counter top line,
     * his head rising [BEAR_HEIGHT] above that, so he reads as sitting right at counter height.
     * His stool's own legs, drawn separately in `KitchenBear` (VoiceScenes.kt), carry on down from
     * this box's own bottom to the real floor ([BEAR_FLOOR]). */
    fun bearBox(landscape: Boolean): Rect {
        val seat = counterTop(landscape) + BEAR_SEAT_GAP
        return Rect(0.01f, seat - BEAR_HEIGHT, BEAR_RIGHT, seat)
    }

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

    /** The "tap the bowl" choice's own slot. */
    fun bowlBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(BOWL_LEFT, top, BOWL_RIGHT, BEAR_FLOOR)
    }

    /** The pan's own slot (and whatever's on it). */
    fun panBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(PAN_LEFT, top, PAN_RIGHT, BEAR_FLOOR)
    }

    /** Which of the two counter stations a kitchen prop uses (K3b): [bowlBox] for everything that
     * happens at the bowl (the bowl itself, milk, batter while it's being mixed, the berries
     * falling in) or [panBox] for the pan itself and everything cooking in it (poured batter, the
     * pancake, its bubbles/steam). */
    fun propSlot(prop: Prop, landscape: Boolean): Rect = when (prop) {
        Prop.PAN, Prop.PANCAKE, Prop.BUBBLE, Prop.STEAM -> panBox(landscape)
        else -> bowlBox(landscape)
    }

    /**
     * Where [prop] actually renders, centred in its own station ([propSlot]) and bottom-anchored
     * on the counter's own surface ([PROPS_MARGIN] below the counter-top line), with no clamp:
     * the exact box `Item` draws in VoiceScenes.kt (its size there is `kitchenUnit * sizeFraction`,
     * `kitchenUnit = min(sceneWidth, sceneHeight)`, centred at this box's own centre). [propBox] is
     * this, clamped into the station; [KitchenLayoutTest]'s own overlap check
     * (`kitchenPropsNeverOverlapRoryOrTheOtherStation`) uses this one instead, since the real
     * render ignores the clamp entirely -- a size picked without checking against this would
     * still look fine in [propBox]'s own (clamped) numbers while actually overlapping on screen.
     *
     * K3e: `kitchenUnit` dropped the generic open-air sizing's `* 1.4` height boost (see
     * `VoiceScenes.kt`'s own `unit`, used for props that float free in a scene with no counter to
     * sit on): in a wide landscape stage that boost lets the *width* win the `min`, so a prop's
     * on-screen height ends up `width/height` times bigger than its own size fraction -- on an
     * iPad-size screen enough to push the berries almost to the very top of the scene, over the
     * wall and the caption, well clear of the counter they're meant to be standing on. Plain
     * `min(sceneWidth, sceneHeight)` keeps a kitchen prop's own height a direct, un-amplified
     * fraction of the scene's height in every orientation (portrait already had `sceneWidth` win
     * this `min` either way, so it draws exactly as before there).
     */
    fun propNaturalBox(prop: Prop, landscape: Boolean, sizeFraction: Float, sceneWidth: Float, sceneHeight: Float): Rect {
        val slot = propSlot(prop, landscape)
        val unit = minOf(sceneWidth, sceneHeight)
        val halfWidth = (unit * sizeFraction / 2f) / sceneWidth
        val height = (unit * sizeFraction) / sceneHeight
        val centerX = (slot.l + slot.r) / 2f
        val bottom = slot.t
        return Rect(centerX - halfWidth, bottom - height, centerX + halfWidth, bottom)
    }

    /**
     * Where [prop] actually sits on the counter (K3b), pure maths so it's directly testable
     * ([KitchenLayoutTest.kitchenPropsAlwaysSitOnTheCounter]) and loggable once per beat ("KITCHEN
     * props", VoiceStory.kt): centred in its own station ([propSlot]), its bottom on the counter's
     * own surface ([PROPS_MARGIN] below the counter-top line) instead of drifting down toward
     * Bear's level the way the full tap-target slot would allow. A real iPad showed the bug this
     * fixes: the generic move-based position (`pose` in VoiceScenes.kt, shared with the open-air
     * boat/owl/balloon scenes, whose fractions were never built with a counter in mind) put the
     * bowl up over the wall, near Bear, instead of on the counter.
     *
     * [sizeFraction] is the prop's own size (`propSize` in VoiceScenes.kt). The result's
     * horizontal span is [propNaturalBox], clamped inside [prop]'s own station ([propSlot]), not
     * the props band as a whole (K3c: the bowl and pan stations are each other's neighbour now,
     * not two ends of one shared band, so a prop can never drift out of its own station into the
     * other's -- or, before K3c moved Rory out of the middle, into Rory's lane).
     */
    fun propBox(prop: Prop, landscape: Boolean, sizeFraction: Float, sceneWidth: Float, sceneHeight: Float): Rect {
        val slot = propSlot(prop, landscape)
        val natural = propNaturalBox(prop, landscape, sizeFraction, sceneWidth, sceneHeight)
        return Rect(natural.l.coerceAtLeast(slot.l), natural.t, natural.r.coerceAtMost(slot.r), natural.b)
    }

    /** Rescales a box from the kitchen scene's own fractions onto the whole stage's, given how
     * much of the stage's height the scene itself keeps once a choice strip is reserved below it
     * ([sceneFraction]; 1f when no strip is reserved this beat). Models a scene that *shrinks* to
     * fit above the strip -- right for portrait (and for every non-Kitchen beat with a strip, the
     * balloon puzzle included), but NOT for Rory's Kitchen in landscape, which keeps the scene at
     * full size and lifts it clear of the strip instead ([onStage] below models that one). */
    fun stageBox(box: Rect, sceneFraction: Float): Rect = Rect(box.l, box.t * sceneFraction, box.r, box.b * sceneFraction)

    /**
     * Where a scene-local fraction ([local], the same 0-to-1 space every box in this file is
     * measured in) actually lands on the WHOLE STAGE's own height, once the choice strip is
     * reserved below it (K3d): mirrors VoiceStory.kt's own landscape lift exactly (one shared
     * source, not two separately-guessed formulas), so a test -- or the "KITCHEN layout
     * counterTop=" / "KITCHEN props ..." logs themselves -- can check the real drawn position
     * without re-deriving it. Landscape never shrinks the scene: it stays the stage's own full
     * height and is shifted up clear of the strip (by the strip's own height; Rory's Kitchen adds
     * no extra lift gap, see [COUNTER_TOP_LANDSCAPE]'s own comment), so a scene-local fraction's
     * on-stage position is simply itself minus the strip's own share of the stage. Portrait (and
     * landscape with no strip at all) never lifts: the scene just shrinks into [1 - stripFraction]
     * of the stage, same as [stageBox]. Because this is one linear conversion applied the same way
     * to any local fraction, the counter's own top and every kitchen prop's own bottom
     * ([propBox]) stay exactly [PROPS_MARGIN] apart on stage, in either orientation -- never
     * independently guessed positions that could drift apart (the bug this fixes: props drawn as
     * if the scene had shrunk like the balloon puzzle's, while the counter they're meant to sit on
     * had actually lifted, left them floating above it).
     */
    fun onStage(local: Float, landscape: Boolean, stageHeight: Float, stripFraction: Float, stripMinHeight: Float): Float {
        if (!landscape) return local * (1f - stripFraction)
        if (stageHeight <= 0f) return local
        val strip = (stageHeight * stripFraction).coerceAtLeast(stripMinHeight)
        return local - strip / stageHeight
    }

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
