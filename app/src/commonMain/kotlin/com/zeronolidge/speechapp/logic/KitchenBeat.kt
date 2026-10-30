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
 *  - [CUT] ("Cut!", the butter-cutting step, K6): one plain tap/swipe (no wrong answer, never
 *    listens), same shape as [FLIP] -- only the gesture that satisfies it differs (a drag across
 *    the butter, with a tap on the dotted line as a backup; see [KitchenCut]), not the state
 *    machine. Narrated only ("Cut!"): never a talk turn either side of it.
 *  - [ASK] ("How many berries?", K6): a talk-only flow, like [STIR]/[BUBBLES], but for a plain
 *    yes/no-style question rather than modelling a word: Rory asks once and listens, without
 *    saying the answer first (`VoiceStoryScreen` skips the usual "now you say it... <word>" cue
 *    for this flow). Silence after the usual re-try still counts (never a third ask), but Rory's
 *    fallback line is the beat's own [VoiceBeat.kitchenFallback] (e.g. "One, two... three!"),
 *    never the generic "I heard you!".
 *  - [SWIPE] (Rory's Fruit Salad: "Swipe to slice!"): one plain swipe/tap (no wrong answer, never
 *    listens), the same shape as [FLIP]/[CUT] -- only the gesture's own art differs (a banana on
 *    its plate, not a dotted line over butter).
 *  - [DRAG] (Rory's Fruit Salad: "Drag to the bowl!"): drags ALL of [VoiceBeat.kitchenGoal]'s own
 *    slices into the bowl (4, same as [COUNT]'s own goal-many taps), no wrong answer, never
 *    listens -- each slice is its own touch target, and a tap on any one of them is the same
 *    forgiving backup [COUNT]'s berries already use.
 * How many taps or talk turns a beat's flow needs is [VoiceBeat.kitchenGoal]. This same state
 * machine now also covers Rory's Fruit Salad (bananas and berries, not pancakes), so every doc
 * comment's "kitchen" here means either story, not just the pancake one.
 */
enum class KitchenFlow { TAP_CHOICE, COUNT, STIR, BUBBLES, FLIP, CUT, ASK, SWIPE, DRAG }

/** True when [KitchenFlow]'s tap phase can be tapped wrong (only [KitchenFlow.TAP_CHOICE]: a real
 * choice among distractors; the others are plain taps that always count, so they can never fail). */
fun KitchenFlow.hasWrongTaps(): Boolean = this == KitchenFlow.TAP_CHOICE

/** How many taps this flow needs before moving on (0: no tap phase at all). [DRAG] counts its own
 * goal-many slices (K-fruit: four, one per [KitchenLayout.BANANA_SLICE_X] entry), the same shape
 * as [COUNT]'s own goal-many berries, so dragging/tapping only one slice can never finish the step. */
fun KitchenFlow.tapGoal(goal: Int): Int = when (this) {
    KitchenFlow.TAP_CHOICE, KitchenFlow.FLIP, KitchenFlow.CUT, KitchenFlow.SWIPE -> 1
    KitchenFlow.COUNT, KitchenFlow.DRAG -> goal
    KitchenFlow.STIR, KitchenFlow.BUBBLES, KitchenFlow.ASK -> 0
}

/** How many talk turns this flow needs before the fix plays (0: no talk turn at all; K3a: every
 * tap flow is now 0, so a tap step never slides into a talk/listening turn afterward). */
fun KitchenFlow.talkGoal(goal: Int): Int = when (this) {
    KitchenFlow.COUNT, KitchenFlow.TAP_CHOICE, KitchenFlow.FLIP, KitchenFlow.CUT, KitchenFlow.SWIPE, KitchenFlow.DRAG -> 0
    KitchenFlow.STIR, KitchenFlow.BUBBLES, KitchenFlow.ASK -> goal
}

/** The two flows (K6) whose tap happens on the shared choice strip at the bottom of the screen,
 * same as the balloon puzzle's choices: [KitchenFlow.TAP_CHOICE]'s bowl-among-distractors and
 * [KitchenFlow.FLIP]'s single pancake tile. [KitchenFlow.COUNT] (three berries, tapped right on
 * the counter beside the bowl) and [KitchenFlow.CUT] (a swipe across the butter) never reserve
 * the strip -- Timothy's notes (K6) asked the counting step to lose its choice tiles entirely, and
 * a swipe has nowhere to put a "tile" at all. */
fun KitchenFlow.usesStrip(): Boolean = this == KitchenFlow.TAP_CHOICE || this == KitchenFlow.FLIP

/**
 * The butter-cutting step's own swipe (K6, [KitchenFlow.CUT]): forgiving on purpose, since a young
 * child's drag may wander or undershoot a straight line across the dotted guide -- any drag at
 * least [THRESHOLD_DP] long, in any direction, counts, and a plain tap on the dotted line is the
 * backup (handled the same way `VoiceStoryScreen` handles every other plain tap: it just calls the
 * same "cut" action directly, never measured against this threshold at all).
 */
object KitchenCut {
    /** The swipe's own least distance to count, in dp (scaled to the real device by whatever
     * density the caller measured its drag in). */
    const val THRESHOLD_DP = 60f

    /** [dragDistancePx] is the drag's total straight-line distance so far (from where the finger
     * first went down, not summed step lengths, so wandering back and forth doesn't double-count);
     * [densityScale] converts [THRESHOLD_DP] to the same px the drag was measured in. */
    fun crossed(dragDistancePx: Float, densityScale: Float): Boolean = dragDistancePx >= THRESHOLD_DP * densityScale
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
    /** K8: Timothy's portrait screenshot (768x1024) showed the wall/window taking nearly two
     * thirds of the scene with a cramped counter below it -- lowered from 0.62 so the wall above
     * (window, shelf) and the counter front read as roughly balanced (counter ~45% of the scene,
     * matching his own "counter top ~55% of the scene" ask), and so there's more room below the
     * line for a bigger Rory/Bear/bowl (see [roryHeight]/[bearHeight]/[kitchenPropSize] in
     * VoiceScenes.kt). [KitchenLayoutTest.portraitCounterTopLandsInTheTargetVisibleBand]'s own band
     * moved down with it.
     *
     * K9: a real portrait screenshot (768x1024) measured the counter front at ~45% of the scene's
     * own height, with Rory and Bear both reading small against it -- raised further (counter front
     * now ~1 - 0.75 = 25%, Timothy's own "22-25%" ask) so most of the scene is wall/window above the
     * line, with far more headroom for [roryLeft]/[roryRight]/[bearRight] to fit a bigger Rory and
     * Bear into (their own height fractions, [roryHeight]/[bearHeight], stay put: both are already
     * comfortably WIDTH-bound at this scene size, see [roryFittedBox]/[bearFittedBox], so only a
     * wider lane -- not a taller nominal box -- actually grows what's drawn). */
    const val COUNTER_TOP_PORTRAIT = 0.75f

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

    /** K7: Rory's Kitchen now reserves the choice strip's own band, and frames itself by the
     * real screen orientation, on EVERY step (see VoiceStoryScreen's own `isKitchen`/`landscape`),
     * never only while a tap step's own tiles happen to be up -- so [counterTop] (and everything
     * below that used to be gated on a real "wide" stage independent of a strip lift, `wide`) now
     * applies the same way to every kitchen beat's own talk turns too, not just its taps. The old
     * K6 "wide" flag is gone: it only ever meant "the real stage is landscape-shaped", which
     * [landscape] itself now always means for Rory's Kitchen, so every lane/height function below
     * takes `landscape` directly instead of a second, redundant boolean.
     *
     * Timothy's own K7 ask, now that this no longer only applies mid-tap: both Rory and Bear read
     * noticeably bigger than before (about 1.6x their old landscape height, 1.3x portrait's), with
     * Rory clearly the bigger of the two ([roryHeight]/[bearHeight] are picked so his own visible
     * height is always at least [MIN_RORY_TO_BEAR_RATIO] times Bear's, matching the scene rule
     * "Rory is the biggest thing, Bear about half his height"; see
     * [KitchenLayoutTest.roryIsTheBiggestThingBearAboutHalfHisHeight]). */
    const val MIN_RORY_TO_BEAR_RATIO = 1.6f

    // --- Ruby's K2b: Bear (on his stool), Rory (behind the counter, chef's hat) and the
    // counter's own props, in explicit lanes so none of them ever overlap. Lanes below are
    // fractions of the kitchen scene's own box (0 to 1); [stageBox] rescales one onto the whole
    // stage once a choice strip is reserved below it (see VoiceStory's `gameStrip`).

    /** Bear's lane (his stool), the far end of the counter from the props: stays clear of the
     * floor beside the counter (K2e: the counter itself starts just to his right, see
     * [BackdropExtras] in VoiceScenes.kt, so he reads as sitting *beside* it, not floating over
     * its front). Narrower than before in landscape (K7), giving up some of his own lane's width
     * to Rory's widened one ([roryLeft]) so Rory can read as the bigger of the two without either
     * of them reaching into the other's space. */
    /** K8: real screenshots measured the drawn Bear at roughly a third of this lane's own box
     * (width- and shift-starved, see [BEAR_SHIFT_FRACTION]'s own K8 note) -- widened in landscape
     * (borrowing a little of the gap [roryLeft] also gives up) so his FITTED box
     * ([bearFittedBox]) can actually reach a reasonable fraction of it.
     *
     * K9: portrait widened too now (0.15 to 0.18), borrowing from the same wall space
     * [COUNTER_TOP_PORTRAIT]'s own rise freed up, moving [roryLeft]/[roryRight] along with it (same
     * gap kept either side) -- a real portrait screenshot showed Bear drawn at only ~45px next to a
     * much bigger Rory. */
    private fun bearRight(landscape: Boolean): Float = if (landscape) 0.155f else 0.215f

    /** K4: Timothy's screenshots showed Bear sitting on a low stool far below the counter (his
     * seat about 45% of the scene's own height below the counter top) -- he read as sitting on
     * the floor beside the counter, not at it. [BEAR_SEAT_GAP] puts his *seat* (not his head, see
     * K2e's old [bearBox]) just below the counter top line instead, as if his stool were a tall
     * one that brings him up to counter height, beside its left end.
     *
     * K5: Timothy's own measured screenshot (1024x768 landscape) still showed the stool's drawn
     * seat (and Bear himself) sitting a further 27px below the counter top -- K4's own 0.05 was
     * still too generous once [BEAR_SEAT_OVERLAP] is taken out, landing the net gap around 4% of
     * the scene's own height, not the "within 2%, read as sitting right at counter height" Ruby
     * actually asked for ([KitchenLayoutTest.stoolSeatTopIsWithinTwoPercentOfTheCounterTop]).
     * Lowered so the net gap ([stoolTop]'s own distance from [counterTop]) comes out under 2%: a
     * taller stool (see [stoolTop]) that reads as bringing him right up to the counter, not merely
     * closer to it. */
    private const val BEAR_SEAT_GAP = 0.02f

    /** Bear's own body height (head to seat), as a fraction of the scene's own height: tall enough
     * that his head (and ears) clearly rise above the counter top once his seat sits at
     * [BEAR_SEAT_GAP] below it. K7: Timothy's own ask was both of them bigger, Rory about 1.6x in
     * landscape, 1.3x in portrait -- grown that far, Bear's own height alone would already pass
     * [MIN_RORY_TO_BEAR_RATIO]'s own room (there's only so much height between the counter top
     * and the screen's own edge to share between them), so he stays at his original size here
     * (landscape a touch over it, never under), with [roryHeight] doing almost all of the growing:
     * real screenshots showed Bear reading plenty big already once the counter's own framing
     * stopped jumping between steps; Rory was the one who read too small. */
    /** K8: grown a little in landscape (alongside [bearRight]'s own widened lane) so his FITTED
     * box has more ceiling room to grow into there too -- portrait is unchanged (its lane doesn't
     * widen, see [bearRight]'s own K8 note). */
    private fun bearHeight(landscape: Boolean): Float = if (landscape) 0.34f else 0.30f

    /** The real floor line his stool's legs reach down to (K4): drawn separately from his own body
     * box ([bearBox], which now ends at his seat, not the floor) in `KitchenBear`
     * (VoiceScenes.kt), a tall stool spanning from his seat down to here, so "stool feet on the
     * floor line" and "seat at counter height" can both be true at once. */
    const val BEAR_FLOOR = 1.0f

    /** Rory's lane (with his chef's hat): his own strip of the counter, left of the props.
     * K2d/K2e kept shrinking his height (down to a 9%-tall sliver in landscape) to fit him
     * entirely above the counter top line before the landscape lift clips the scene's own top,
     * which read as a tiny Rory standing ON the counter rather than a normal-sized one behind it.
     * K2f draws him behind the counter instead ([KitchenRory] drawn before [KitchenCounterFront]
     * in VoiceScenes.kt, so the counter's own paint covers his lower body, the way a real counter
     * hides someone's legs): his box can now be one full, un-shrunk size in both orientations
     * ([roryHeight]), reaching down past the counter top line by design
     * ([roryHiddenFraction]) instead of stopping just short of it.
     *
     * K3c: Timothy's screenshots (1024x768 and 768x1024) showed the bowl, milk and berries
     * covering Rory's face -- this lane used to be centred ([0.33, 0.67]) right on top of the
     * bowl station ([0.40, 0.60] at the time), since nothing had ever checked Rory's own visible
     * box against the props themselves (only bear/bowl/pan were in
     * `bearRoryBowlAndPanNeverOverlapEachOther`'s own box list). Moving him into his own strip,
     * left of both stations entirely, is the fix a centred-behind-the-props lane could never give
     * without shrinking him down to fit between the two (K3b's own station widths leave no gap
     * big enough in the middle).
     *
     * K6/K7: his right edge ([roryRight]) barely moves (the bowl station starts just past it, see
     * [bowlLeft]) but his LEFT edge does, widened further in K7 (same gap Bear also gives up, see
     * [bearRight]) than K6's own more modest grow-on-a-wide-stage version: [roryLeft] widens his
     * lane (so he can be fitted bigger, see [roryFittedBox]) by borrowing from the wall between him
     * and Bear, never from the props' own side.
     *
     * K8: Timothy's own portrait screenshot (768x1024) measured his drawn FITTED box at barely a
     * third of [roryBox]'s own nominal height -- his lane there is much narrower, relative to the
     * height he's asked to reach, than landscape's own (portrait's own scene is taller/narrower,
     * so the same width buys proportionally less height, see [roryFittedBox]'s own width-vs-height
     * fit): widened further still in portrait than K7's own version, past where [bowlLeft] used to
     * sit, with every counter station past it ([bowlLeft]/[ingLeft]/[panLeft], all in VoiceScenes.kt
     * too via [kitchenPropSize]) shifted along to keep its own share of the counter.
     *
     * K9: portrait widened again (his own fitted box was still only ~180px wide on a 768px-wide
     * screen) -- [roryLeft] moves along with [bearRight]'s own K9 widening (same 0.02 gap kept),
     * and [roryRight] moves out a further 0.03 past that, so his FITTED width (still comfortably
     * width-bound, see [roryFittedBox]) grows from the old ~24% of the scene's width toward ~33%.
     * [bowlLeft]/[ingLeft]/[panLeft] (and their own right edges) all shift along by the same
     * amount again, borrowing the rest of the room from a narrower [panRight] station (the pan/
     * pancake's own single prop, not three side-by-side things like the bowl/ingredient stations,
     * has the least to lose): every gap between stations stays exactly what it was, so nothing
     * that didn't already overlap can newly start to. */
    private fun roryLeft(landscape: Boolean): Float = if (landscape) 0.165f else 0.22f
    private fun roryRight(landscape: Boolean): Float = if (landscape) 0.44f else 0.535f

    /** Rory's own height, as a fraction of the kitchen scene's own height. K4 grew this from a
     * quarter: with the counter top itself moved down (see
     * [COUNTER_TOP_LANDSCAPE]/[COUNTER_TOP_PORTRAIT]), a bigger Rory reads as standing at a
     * normal-sized counter instead of a tall one looming over a tiny one. K7: bigger again in both
     * orientations now that the strip/lift (and so this whole layout) applies on every step, not
     * only while a tap step's own tiles are up -- landscape grows further than portrait (more
     * spare width to widen his lane into, see [roryLeft]), together with his widened lane so he's
     * fitted bigger however his real box picks between "as tall as the lane allows" and "as wide
     * as the lane allows" (see [roryFittedBox]).
     *
     * K8: portrait lowered a touch (0.60 to 0.58): with [roryLeft]/[roryRight] now much wider
     * there, his fit is comfortably width-bound well short of this height ceiling (see
     * [roryFittedBox]), so this only needs to clear that width-bound height while keeping his own
     * nominal box's top on screen (never negative, see [COUNTER_TOP_PORTRAIT]/[roryHiddenFraction]) --
     * never the thing actually limiting how tall he's drawn. */
    private fun roryHeight(landscape: Boolean): Float = if (landscape) 0.64f else 0.70f

    /** How much of [roryHeight] sits below the counter top line, hidden behind the counter (K2g
     * was 0.45, K4 lowers it further from 0.25): Timothy's ask was chest-up, both arms included --
     * 0.18 clears his whole head, both shoulders and arms and most of his chest, so the counter
     * front hides only his lower chest/belly and legs, the way a waist-high counter should.
     *
     * K8: Timothy's landscape screenshot showed his chef hat right at the very top of the scene,
     * clipped close by VoiceStory's own landscape lift ([onStage]) -- raised in landscape (never
     * his own fitted size, just how much of it the counter hides) so his fitted box's own visible
     * top sits further down, clear of that lift line, with no change to his nominal height/width.
     * Portrait lowered a little the other way: it never lifts/clips (see [onStage]), and showing a
     * bit more of him (closer to the old 0.18) reads better now that he's actually big enough to
     * matter there. */
    private fun roryHiddenFraction(landscape: Boolean): Float = if (landscape) 0.22f else 0.12f

    /** The bowl station (the bowl choice, and the batter while it's being mixed or holding the
     * berries): its own strip of the counter, clear of Rory's lane ([roryRight]) on one side and
     * the ingredient station ([ingLeft]) on the other (K3c: K3b's props band used to sit right
     * under Rory's old centred lane; K6 narrowed it again and gave milk/the berry row their own
     * station, see [ingLeft], since the milk carton used to be drawn centred in this exact same
     * spot as the bowl -- Timothy's screenshot showed it sitting flush on top of the bowl, hiding
     * it completely). */
    /** K8: portrait's own station shifted left along with [roryRight]'s own K8 widening (its
     * share of the counter is unchanged, just moved to make room for Rory's bigger lane); landscape
     * is unchanged here (only [bowlRight] widens there).
     *
     * K9: shifted right again, by the same amount [roryRight]'s own K9 widening grew by (0.07),
     * keeping the same 0.02 gap -- its own width is unchanged (the bowl/batter already read a fair
     * size there; [kitchenPropSize] in VoiceScenes.kt gives the actual prop its own K9 size bump
     * instead, independent of the station it's clamped into). */
    private fun bowlLeft(landscape: Boolean) = if (landscape) 0.46f else 0.55f

    /** K7: widened in landscape so the bigger bowl/batter ([kitchenPropSize] in VoiceScenes.kt,
     * ~1.6x there) still fits inside its own station without reaching into the ingredient
     * station's. K8: widened a little further still in landscape (Timothy's own "bowl still looks
     * small" ask), and portrait keeps the old, un-widened station (its own props only grow a
     * little, see [kitchenPropSize]). K9: portrait shifts right with [bowlLeft], same width. */
    private fun bowlRight(landscape: Boolean) = if (landscape) 0.64f else 0.745f

    /** The ingredient station (K6): milk, on its own, while it pours into the bowl (beat 2) -- or,
     * once it's a different beat, three berries in a row waiting to be tapped into the bowl (see
     * [berrySlot]), never both at the same time, so one station can serve either without the two
     * ever overlapping each other or the bowl. K7: widened in landscape, to match [bowlRight]'s
     * own widening (the butter plate shares this station, and also grows ~1.6x there). K8: both
     * orientations shift right a touch to follow [bowlRight]/[bowlLeft]'s own K8 moves, keeping the
     * same gaps either side. K9: portrait shifts right again with [bowlRight], same width (so
     * [berrySlot]'s own three-way split, and [COUNT_BERRY_SIZE], are unaffected). */
    private fun ingLeft(landscape: Boolean) = if (landscape) 0.655f else 0.755f
    private fun ingRight(landscape: Boolean) = if (landscape) 0.785f else 0.89f

    /** The pan station (the pan itself and everything cooking in it): clear of the ingredient
     * station on one side and the counter's own right edge on the other (K3c). K7: shrinks a
     * little in landscape (the pan itself draws a little smaller there, [kitchenPropSize] in
     * VoiceScenes.kt) to give the bowl and ingredient stations the room they need to grow. K8:
     * both orientations follow [ingLeft]/[ingRight]'s own K8 shift right by the same amount, so
     * every gap stays exactly what it was; portrait's own right edge moves out to the same margin
     * landscape already kept (it had more spare room before Rory's own lane widened into it).
     *
     * K9: portrait's own left edge moves right with [ingRight] (same 0.02 gap), narrowing this
     * station -- the one place K9's own room for a bigger Rory/Bear actually came from: a single
     * pan (one prop at a time, never split three ways like the bowl/ingredient stations) has the
     * least to lose, and its own natural (unclamped) width, even un-shrunk, still lands just inside
     * the screen's own right edge at this station's new size (checked by hand against
     * [kitchenPropSize]'s own PAN fraction in VoiceScenes.kt: 0.16 of a 768px-wide portrait scene
     * is ~123px, comfortably inside the ~15px either side this station's own 0.85-0.98 keeps clear
     * of both [ingRight] and the screen edge). [panRight] itself never moves: it's already the
     * same safe margin from the screen's own right edge landscape independently settled on. */
    private fun panLeft(landscape: Boolean) = if (landscape) 0.80f else 0.765f
    private fun panRight(landscape: Boolean) = if (landscape) 0.98f else 0.99f

    /** How far below the counter-top line the props' own tap-target slots start (K2b) -- and,
     * since K3b, also where every prop's own *bottom edge* actually rests ([propBox]): a thin
     * "counter surface" band right under the line, not spread down toward [BEAR_FLOOR] (K2b's
     * slots are tall enough for a comfortable tap target, but a prop drawn anywhere in that much
     * height read as floating, not standing on the counter). Public so [KitchenLayoutTest] can
     * check against it directly rather than guessing the number. */
    const val PROPS_MARGIN = 0.02f

    /**
     * K5: the "KITCHEN props" log (`propRect` in VoiceStory.kt) measures each prop's layout BOX
     * ([propBox]), which [kitchenPropsAlwaysSitOnTheCounter][KitchenLayoutTest] already checks
     * sits right on the counter -- but real screenshots (the pan during the bubbles beat, the
     * berry pile while counting) showed the hand-drawn paper art INSIDE that box (VoiceScenes.kt's
     * `Item`/`PropArt` canvases) stopping well short of the box's own bottom edge: empty padding
     * baked into each shape's own hardcoded y-fractions, invisible to a box-only check. These are
     * that same drawing math's own numbers, pulled out as named, shared constants (not duplicated
     * literals in VoiceScenes.kt) so the two can never drift apart again, and so a plain unit test
     * can confirm each shape's own lowest painted pixel lands exactly on its box's bottom edge
     * (local fraction 1f), with no gap.
     */
    fun symmetricQuadApexY(edgeY: Float, controlY: Float): Float = (edgeY + controlY) / 2f

    /** [Prop.BOWL]'s outer rim curve (a quadratic Bezier from `(0.1, BOWL_RIM_Y)` to
     * `(0.9, BOWL_RIM_Y)` via `(0.5, BOWL_OUTER_CONTROL_Y)`): its apex ([symmetricQuadApexY]) is
     * the bowl's own lowest drawn point, which must land at 1f. */
    const val BOWL_RIM_Y = 0.56f
    const val BOWL_OUTER_CONTROL_Y = 1.44f

    /** The bowl's inner rim curve (back up from the same two rim points): purely decorative, no
     * bottom-touching requirement of its own. */
    const val BOWL_INNER_CONTROL_Y = 0.72f

    /** The bowl's inner rim highlight oval. */
    const val BOWL_INNER_RIM_TOP = 0.44f
    const val BOWL_INNER_RIM_HEIGHT = 0.24f

    /** [Prop.BUTTER]'s plate oval: top offset and height, as fractions of the box -- their sum
     * must land at 1f so the plate sits flush on the counter surface. */
    const val BUTTER_PLATE_TOP = 0.76f
    const val BUTTER_PLATE_HEIGHT = 0.24f

    /** [Prop.MILK]'s carton: its own drawn bottom edge, as a fraction of its box -- 1f, flush with
     * the box's own bottom, with every other carton measurement (shoulder, gable, label) shifted
     * to match (see the Canvas drawing in VoiceScenes.kt). */
    const val MILK_ART_BOTTOM = 1f

    /** [Prop.BERRIES] (Rory's Kitchen's own blueberries): the lowest berry's own centre Y and
     * radius (as a fraction of the box's smaller side) -- their sum must land at 1f, the lowest
     * berry's own bottom flush with the box's bottom edge. The other two berries keep their
     * original offset from this one (see the Canvas drawing). */
    const val BERRIES_LOWEST_CENTER_Y = 0.83f
    const val BERRIES_RADIUS_FRACTION = 0.17f

    /** [Prop.PAN]'s own oval body (also redrawn, identically, by the pancake-bubbles beat's own
     * `KitchenPancakeBubbles` in VoiceScenes.kt, so the pan it draws always matches this one): top
     * offset and height, as fractions of the box -- their sum must land at 1f. */
    const val PAN_ART_TOP = 0.78f
    const val PAN_ART_HEIGHT = 0.22f

    /** [Prop.PANCAKE]'s own base oval (also shared with `KitchenPancakeBubbles`): top offset and
     * height, as fractions of the box -- their sum must land at 1f. The lighter top-highlight oval
     * keeps its original offset from this one (see the Canvas drawing), no bottom-touching
     * requirement of its own. */
    const val PANCAKE_ART_TOP = 0.6f
    const val PANCAKE_ART_HEIGHT = 0.4f

    /** [Prop.BANANA]'s own plate oval (Rory's Fruit Salad): top offset and height, as fractions
     * of the box -- their sum must land at 1f, same rule as [BUTTER_PLATE_TOP]/[BUTTER_PLATE_HEIGHT]
     * so the plate sits flush on the counter surface instead of floating a couple of percent above
     * it (a real screenshot showed the old 0.80/0.18 pair leaving it visibly short). */
    const val BANANA_PLATE_TOP = 0.78f
    const val BANANA_PLATE_HEIGHT = 0.22f

    /** The four banana slice centres, once sliced (Rory's Fruit Salad), as x fractions of the
     * banana's own box -- shared between the art ([Prop.BANANA] in VoiceScenes.kt) and the drag
     * step's own four touch targets (`FruitDragOverlay`), so each slice is always exactly where he
     * can actually tap or drag it, and Rory can count "one, two, three, four" against something
     * really there. Spread across the banana's own box (now sized and stationed like the bowl, see
     * [bananaBox]), each one's own touch zone still at least 56dp ([FruitDragOverlay]'s own
     * `minZone`) however far apart these four fractions sit. */
    val BANANA_SLICE_X = listOf(0.16f, 0.39f, 0.61f, 0.84f)

    /** A box on screen, as fractions (0 at the left/top edge, 1 at the right/bottom): [overlaps]
     * is false for boxes that merely touch at an edge. */
    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float) {
        fun overlaps(other: Rect): Boolean = l < other.r && r > other.l && t < other.b && b > other.t
    }

    /** Bear's own left edge (K8): real screenshots (1024x768 landscape, 768x1024 portrait) showed
     * him drawn pressed right against the screen's own left edge (well under 16px) -- raised from a
     * near-zero 0.01 so he always keeps a real gap, at every size this story runs at (the smaller
     * of the two test tablet widths, 768, is the binding one: 0.03 * 768 = ~23px). */
    private const val BEAR_LEFT = 0.03f

    /** Bear's own body box (K4: his *seat*, not his floor -- see [BEAR_SEAT_GAP]/[bearHeight]),
     * as a fraction of the kitchen scene's own box: his seat sits just below the counter top line,
     * his head rising [bearHeight] above that, so he reads as sitting right at counter height.
     * His stool's own legs, drawn separately in `KitchenBear` (VoiceScenes.kt), carry on down from
     * this box's own bottom to the real floor ([BEAR_FLOOR]). */
    fun bearBox(landscape: Boolean): Rect {
        val seat = counterTop(landscape) + BEAR_SEAT_GAP
        val height = bearHeight(landscape)
        return Rect(BEAR_LEFT, seat - height, bearRight(landscape), seat)
    }

    /** K4b: real screenshots (1024x768 landscape, 768x1024 portrait) showed Bear's own bottom
     * ([bearBox]'s own `b`) sitting a couple of px above the stool's drawn seat -- a hairline gap
     * that reads as him floating, even though both numbers already shared the same `seatY` anchor
     * in `KitchenBear` (VoiceScenes.kt). [stoolTop] is defined a few px *above* [bearBox]'s own
     * bottom on purpose, never as an independent number, so his seat always rests ON the stool
     * with a small overlap instead of merely touching it (see
     * `KitchenLayoutTest.bearsSeatOverlapsTheStoolSeat`). */
    const val BEAR_SEAT_OVERLAP = 0.01f

    /** Where the stool's own seat (drawn in `KitchenBear`, VoiceScenes.kt) starts: see
     * [BEAR_SEAT_OVERLAP]. */
    fun stoolTop(landscape: Boolean): Float = bearBox(landscape).b - BEAR_SEAT_OVERLAP

    /** Rory's box (with his hat), as a fraction of the kitchen scene's own box: reaches from
     * above the counter top line down past it ([roryHiddenFraction]), since the counter is
     * drawn over his lower body (K2f), not clipped away by this box itself. This is his *nominal*
     * box, at his full own height ([roryHeight]); see [roryFittedBox] for the box actually drawn,
     * which can come out shorter than this in a narrow lane. */
    fun roryBox(landscape: Boolean): Rect {
        val ct = counterTop(landscape)
        val height = roryHeight(landscape)
        val hidden = roryHiddenFraction(landscape)
        return Rect(roryLeft(landscape), ct - height * (1f - hidden), roryRight(landscape), ct + height * hidden)
    }

    /** K4b: Rory's real on-screen box, for a stage of [w] by [h]: a real portrait screenshot
     * (768x1024) showed him standing in front of the counter with his whole body, feet included,
     * visible above it. [roryBox] alone can't be trusted to draw him there: his lane
     * ([roryLeft]/[roryRight]) is much narrower than [CRITTER_ART_RATIO] needs for his nominal
     * height ([roryHeight]) once the scene itself is tall and narrow (portrait), so
     * `Modifier.fillMaxHeight()` plus Critter's own `aspectRatio` fought over the lane's width and
     * measured him shorter, top-anchored at [roryBox]'s own top -- his real feet landed well short
     * of the counter top line instead of reaching down past it. Anchoring from the BOTTOM instead,
     * his hidden fraction ([roryHiddenFraction]) of his own *fitted* height below the counter
     * top line (never the lane's nominal one), keeps him emerging from behind the counter by the
     * same proportion however much the lane narrows him. `KitchenRory` (VoiceScenes.kt) draws
     * exactly this box, not [roryBox], so the two can never drift apart. */
    private const val CRITTER_ART_RATIO = 100f / 140f

    /** K7e: the Critter canvas carries transparent padding either side of Rory's drawn body
     * (measured on a 768x1024 screenshot: body 150px in a 229px-wide canvas, centred). In
     * portrait, where width is the limit, his lane ([roryBox]) now holds his drawn BODY and the
     * canvas may spill its empty padding past it, so he can be ~1.5x taller without anything
     * overlapping what you actually see. Landscape keeps the old whole-canvas fit. */
    fun roryBodyWidthFraction(landscape: Boolean): Float = if (landscape) 1f else 0.68f

    fun roryFittedBox(w: Float, h: Float, landscape: Boolean): Rect {
        val rory = roryBox(landscape)
        val ct = counterTop(landscape)
        val laneWidth = w * (rory.r - rory.l) / roryBodyWidthFraction(landscape)
        val laneHeight = h * (rory.b - rory.t)
        val fittedHeight = minOf(laneHeight, laneWidth / CRITTER_ART_RATIO)
        val fittedWidth = fittedHeight * CRITTER_ART_RATIO
        val centerX = w * (rory.l + rory.r) / 2f
        val bottom = h * ct + fittedHeight * roryHiddenFraction(landscape)
        return Rect(centerX - fittedWidth / 2f, bottom - fittedHeight, centerX + fittedWidth / 2f, bottom)
    }

    /**
     * K8: Bear's real on-screen box, for a stage of [w] by [h] -- the same fit `KitchenBear`
     * (VoiceScenes.kt) actually draws (fitted to a slice of his lane narrower than itself, by
     * [BEAR_SHIFT_FRACTION], and pinned to the lane's own LEFT edge, not centred, so his bite lean
     * keeps a real gap before the counter, see [bearLean]'s own doc), not [bearBox]'s own nominal
     * lane. Real screenshots (1024x768 landscape, 768x1024 portrait) showed the drawn Bear only
     * a third or so of his own lane's box -- [KitchenLayoutTest]'s own `bearOnScreenBox` used to
     * re-derive this exact formula separately for the bite-lean tests; sharing it here means the
     * tests, `KitchenBear` and anything that logs his real box (see VoiceStory.kt's own `KITCHEN`
     * logs) can never quietly drift apart again.
     */
    fun bearFittedBox(w: Float, h: Float, landscape: Boolean): Rect {
        val bear = bearBox(landscape)
        val laneWidth = w * (bear.r - bear.l)
        val laneHeight = h * (bear.b - bear.t)
        val fitWidth = (laneWidth - w * BEAR_SHIFT_FRACTION).coerceAtLeast(laneWidth * 0.5f)
        val fitHeight = minOf(laneHeight, fitWidth / CRITTER_ART_RATIO)
        val fitWidthActual = fitHeight * CRITTER_ART_RATIO
        val laneLeft = w * bear.l
        val laneBottom = h * bear.b
        return Rect(laneLeft, laneBottom - fitHeight, laneLeft + fitWidthActual, laneBottom)
    }

    /** The part of [roryBox] that actually shows: above the counter top line, where the counter
     * can't hide him. The rest is deliberately behind the counter (K2f), so overlap checks
     * against the other kitchen elements (bear, bowl, pan) use this, not the full box: see
     * `bearRoryBowlAndPanNeverOverlapEachOther`. */
    fun roryVisibleBox(landscape: Boolean): Rect {
        val box = roryBox(landscape)
        return Rect(box.l, box.t, box.r, counterTop(landscape))
    }

    /** K7 (Ruby's scene checklist): roughly where Rory's own eyes and mouth sit, inside his
     * visible (above-the-counter) slice -- a prop drawn over this would read as covering his
     * face. A soft approximation (the middle 70% of his visible box's own width, its top 65% of
     * height, since his head sits at the top of a chest-up puppet), not Critter's exact head
     * circle: good enough to catch a prop that's drifted up into his face, not a pixel-exact mask. */
    fun roryFaceBox(landscape: Boolean): Rect {
        val box = roryVisibleBox(landscape)
        val w = box.r - box.l
        val h = box.b - box.t
        return Rect(box.l + w * 0.15f, box.t, box.r - w * 0.15f, box.t + h * 0.65f)
    }

    /** K7 (Ruby's scene checklist): the same soft approximation as [roryFaceBox], for Bear's own
     * body box (head to seat) -- his head sits at the TOP of that box too. */
    fun bearFaceBox(landscape: Boolean): Rect {
        val box = bearBox(landscape)
        val w = box.r - box.l
        val h = box.b - box.t
        return Rect(box.l + w * 0.15f, box.t, box.r - w * 0.15f, box.t + h * 0.55f)
    }

    /** K6: the size (as a fraction of the scene's own smaller side, same convention as
     * `kitchenPropSize` in VoiceScenes.kt) each of the three waiting berries draws at -- small
     * enough that three, each in its own third of [ingredientBox], never touch. */
    const val COUNT_BERRY_SIZE = 0.038f

    /** The "tap the bowl" choice's own slot. */
    fun bowlBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(bowlLeft(landscape), top, bowlRight(landscape), BEAR_FLOOR)
    }

    /** K6: the ingredient station, beside the bowl -- milk on its own (beat 2), sized to the
     * whole station; see [berrySlot] for the three-way split the counting beat uses instead. Never
     * drawn at the same time as the bowl choice's own distractors, so this never needs to dodge
     * anything but the bowl itself and the pan station, both already clear by construction
     * ([ingLeft]/[ingRight]). */
    fun ingredientBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(ingLeft(landscape), top, ingRight(landscape), BEAR_FLOOR)
    }

    /** K6: one third of the ingredient station, for the counting beat's three waiting berries
     * (left to right, [index] 0 to 2) -- beside the bowl, before the pan, each its own tap target,
     * never overlapping the others or the bowl (see
     * [KitchenLayoutTest.coOccurringKitchenPropsNeverOverlapEachOther]). */
    fun berrySlot(index: Int, landscape: Boolean): Rect {
        val whole = ingredientBox(landscape)
        val w = (whole.r - whole.l) / 3f
        val l = whole.l + w * index
        return Rect(l, whole.t, l + w, whole.b)
    }

    /**
     * Where the counting beat's waiting berry [index] (0 to 2) actually draws, for a scene of
     * [sceneWidth] by [sceneHeight]: same maths as [propNaturalBox] (centred in its own slot,
     * [COUNT_BERRY_SIZE] of the scene's smaller side, bottom-anchored on the counter), just
     * centred in [berrySlot] instead of a prop's whole station.
     */
    fun berryBox(index: Int, landscape: Boolean, sceneWidth: Float, sceneHeight: Float): Rect {
        val slot = berrySlot(index, landscape)
        val unit = minOf(sceneWidth, sceneHeight)
        val halfWidth = (unit * COUNT_BERRY_SIZE / 2f) / sceneWidth
        val height = (unit * COUNT_BERRY_SIZE) / sceneHeight
        val centerX = (slot.l + slot.r) / 2f
        val bottom = slot.t
        return Rect(centerX - halfWidth, bottom - height, centerX + halfWidth, bottom)
    }

    /** The pan's own slot (and whatever's on it). */
    /** K7d: the butter's own station. The three butter beats (ask, cut, ask again) show the butter
     * plate ALONE -- no bowl, milk or pan is drawn then (see [VoiceStories.kitchen]) -- so instead of
     * squeezing it into the narrow ingredient slot (portrait butter was ~40px), it gets the whole
     * free counter right of Rory, centred, big enough that its two cut pieces are easy to count.
     * [KitchenSceneRulesTest] still checks it never meets Rory and never co-occurs with another prop. */
    fun butterBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(roryRight(landscape) + 0.04f, top, 0.96f, BEAR_FLOOR)
    }

    fun panBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(panLeft(landscape), top, panRight(landscape), BEAR_FLOOR)
    }

    /** [Prop.BANANA]'s own station (Rory's Fruit Salad, K-fruit): the ingredient AND pan stations
     * combined -- Fruit Salad never shows milk, a pan or a pancake (those are Rory's Kitchen's own
     * props), so both are free for the one banana/slices prop the whole story through. A real
     * screenshot showed the banana squeezed into the narrow ingredient slot alone, well short of
     * [Prop.FRUIT_BOWL]'s own width even once [kitchenPropSize] in VoiceScenes.kt grew it to match
     * the bowl's -- this gives it the room that size actually needs, while staying clear of
     * [bowlBox] (the bowl/[Prop.FRUIT_BOWL] station, drawn alongside it as the beat's own `still`
     * prop) on its left. */
    fun bananaBox(landscape: Boolean): Rect {
        val top = counterTop(landscape) + PROPS_MARGIN
        return Rect(ingLeft(landscape), top, panRight(landscape), BEAR_FLOOR)
    }

    /** Which counter station a kitchen prop uses (K3b; K6 split the old shared bowl/milk/berries
     * station in two, see [bowlLeft]'s own doc): [bowlBox] for the bowl itself and the batter
     * (while it's being mixed, or holding the counted berries), [ingredientBox] for milk on its
     * own, [bananaBox] for Rory's Fruit Salad's own banana/slices, or [panBox] for the pan itself
     * and everything cooking in it (poured batter, the pancake, its bubbles/steam). [Prop.BUTTER]
     * (K6, the butter-cutting mini-sequence) reuses the ingredient station too: by the time it
     * plays, milk's long since poured, so the same spot is free for the butter plate. */
    fun propSlot(prop: Prop, landscape: Boolean): Rect = when (prop) {
        Prop.PAN, Prop.PANCAKE, Prop.BUBBLE, Prop.STEAM -> panBox(landscape)
        Prop.MILK -> ingredientBox(landscape)
        Prop.BANANA -> bananaBox(landscape)
        Prop.BUTTER -> butterBox(landscape)
        // Prop.FRUIT_BOWL (Rory's Fruit Salad's own bowl) falls through to the bowl station below,
        // same as Prop.BOWL/Prop.BATTER.
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

    /**
     * K6: Timothy's screenshot measured Bear's own body at x~55-115 and the stool's drawn seat at
     * x~38-90 -- the stool's own canvas (`KitchenBear` in VoiceScenes.kt, `paperStool`) used to be
     * [STOOL_CANVAS_WIDTH_FRACTION] of Bear's own box width but pinned to its *left* edge
     * (`Modifier.fillMaxWidth(0.6f)`, default `Box` alignment), while Bear's own puppet fills the
     * whole box -- so the seat's own centre sat well left of his, and he read as sitting beside
     * the stool, not on it. The fix centres that canvas in the box instead
     * (`contentAlignment = Alignment.TopCenter`); [stoolSeatCenterXFraction] is the same maths,
     * free of any Compose type, so it's directly testable: the seat oval itself is centred within
     * its own canvas too ([STOOL_SEAT_SIDE_MARGIN] equal on both sides), so a canvas centred in the
     * box always puts the seat's own centre exactly at the box's own centre (0.5), for any width.
     *
     * K8: Timothy's own ask -- a stool seat about as wide as Bear's own box, not a "tall thin"
     * one -- widened the canvas (0.62 to 0.88) and narrowed its own side margin (0.08 to 0.06), so
     * the seat itself now spans about 77% of Bear's box width instead of about 52%.
     * [stoolSeatCenterXFraction] still always lands on 0.5 for any width, so
     * `stoolSeatIsHorizontallyCentredUnderBear` needs no change.
     */
    const val STOOL_CANVAS_WIDTH_FRACTION = 0.88f
    const val STOOL_SEAT_SIDE_MARGIN = 0.06f

    fun stoolSeatCenterXFraction(boxWidth: Float): Float {
        if (boxWidth <= 0f) return 0.5f
        val canvasWidth = boxWidth * STOOL_CANVAS_WIDTH_FRACTION
        val canvasLeft = (boxWidth - canvasWidth) / 2f
        val seatWidth = canvasWidth * (1f - 2f * STOOL_SEAT_SIDE_MARGIN)
        val seatLeft = canvasLeft + canvasWidth * STOOL_SEAT_SIDE_MARGIN
        return (seatLeft + seatWidth / 2f) / boxWidth
    }

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
     * tight, K2i) or the counter's own (tied to [bearRight], never shifts). The runtime clamp on
     * his real measured head bounds (`onHeadBounds`, VoiceScenes.kt) is still the actual
     * guarantee against his head crossing the counter; this is slack so that clamp is never
     * needed at ordinary sizes.
     *
     * K8: real screenshots showed Bear's own drawn puppet only about a third of his lane's own
     * box -- his lane is much narrower than Rory's own to start with (there's only so much counter
     * to split between them), so this same flat, scene-width-based shift that was tuned for K2k's
     * wider lane was eating a disproportionate share of a narrow one. Lowered (0.035 to 0.014,
     * still enough to keep [bearsRestingBoxLeavesAGapBeforeTheCounter]'s own 10px floor at both
     * tablet sizes: 0.014 * 768 = ~10.8px, the tighter (portrait) of the two) so his FITTED box
     * ([bearFittedBox]) can actually reach a sensible fraction of his own lane; the runtime clamp
     * above is still there as the real guarantee against his bite lean crossing the counter. */
    const val BEAR_SHIFT_FRACTION = 0.014f

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
