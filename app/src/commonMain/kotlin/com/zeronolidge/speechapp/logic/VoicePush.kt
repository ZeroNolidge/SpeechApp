package com.zeronolidge.speechapp.logic

/**
 * How his voice moves a voice story's action, live: every reading that counts as voice pushes
 * [progress] on (louder pushes faster; holding the sound keeps it moving; silence stops it), and
 * the loudest push sets how big the flourish is ([strength]). Nothing here can fail:
 *  - a try that reached the end, or any clear voice, completes (the rest at a gentle speed);
 *  - a quiet or soft try nudges it partway, Rory says "Again, together!", and he tries again;
 *  - after [MAX_TRIES] quiet tries it waits, listening, for any sound or "I said it!" (the card
 *    never moves on by itself), and any sound then completes it gently,
 * so the next beat never depends on a loud voice. How a reading pushes depends on [Mode].
 */
class VoicePush(val mode: Mode = Mode.LOUDNESS) {
    var progress = 0f
        private set
    private var peak = 0f
    var tries = 0
        private set
    /** Pop sounds counted so far ([Mode.BURSTS]). */
    var bursts = 0
        private set
    private var quietMs = BURST_GAP_MS
    /** True after [MAX_TRIES] quiet tries: listening carries on until his voice or "I said it!". */
    var waiting = false
        private set

    /**
     * How his voice moves it:
     *  - [BURSTS] for pop sounds (p, b, t, d, g, k): each separate burst is one push, so
     *    "pop! pop!" pushes twice; holding a pop sound doesn't keep it going.
     *  - [HOLD] for long sounds (s, m, f, sh): how long he holds the sound, loudness matters little.
     *  - [LOUDNESS] for everything else: louder is faster, holding keeps it moving.
     */
    enum class Mode { BURSTS, HOLD, LOUDNESS }

    /** One reading of [push] (0 to 1, see [VoiceMeter.push]) lasting [ms]. Returns true once it reaches the end. */
    fun feed(push: Float, ms: Int): Boolean {
        if (push > 0f) {
            peak = maxOf(peak, push)
            when (mode) {
                Mode.BURSTS -> if (quietMs >= BURST_GAP_MS) {
                    bursts++
                    progress = minOf(1f, progress + burstStep(push))
                }
                Mode.HOLD -> progress = minOf(1f, progress + ms / 1000f * holdSpeed(push))
                Mode.LOUDNESS -> progress = minOf(1f, progress + ms / 1000f * speed(push))
            }
            quietMs = 0
        } else {
            quietMs += ms
        }
        return progress >= 1f
    }

    /** How big the flourish is: about half for a whisper, full for a big voice ("I said it!" in between). */
    val strength: Float get() = (0.45f + 0.55f * peak).coerceIn(0.45f, 1f)

    /** What to do after a listening turn: [heard] a clear voice, [soft] some sound. */
    fun afterTry(heard: Boolean, soft: Boolean): Next {
        tries++
        return when {
            progress >= 1f || heard -> Next.FINISH_GENTLY
            // While waiting, any sound at all counts.
            waiting && soft -> Next.FINISH_GENTLY
            waiting -> Next.WAIT
            tries >= MAX_TRIES -> {
                waiting = true
                Next.WAIT
            }
            else -> {
                // A quiet try still moves it a little: the action shows his try counted.
                progress = minOf(NUDGE_CAP, progress + if (soft) 0.3f else 0.18f)
                Next.AGAIN_TOGETHER
            }
        }
    }

    /** "I said it!": the same action, at a gentle speed. */
    fun pressed() {
        // Never weaker than medium, even after a faint try.
        peak = maxOf(peak, PRESSED)
    }

    /**
     * FINISH_GENTLY: play the rest of the action. AGAIN_TOGETHER: Rory models again, one more try.
     * WAIT: keep listening quietly (no more prompts) until his voice or "I said it!".
     */
    enum class Next { FINISH_GENTLY, AGAIN_TOGETHER, WAIT }

    companion object {
        const val MAX_TRIES = 2
        /** A quiet try never gets it more than this far, so the end is always his voice or the gentle finish. */
        const val NUDGE_CAP = 0.6f

        /** Progress per second at a push: a whisper takes about 2.5 s of sound, a big voice under 0.6 s. */
        fun speed(push: Float): Float = 0.4f + 1.4f * push.coerceIn(0f, 1f)

        /** Held sounds: about 1.6 s of "sssss" gets there, a little faster when louder. */
        fun holdSpeed(push: Float): Float = 0.55f + 0.35f * push.coerceIn(0f, 1f)

        /** One pop: about a third of the way (three pops get there), a bit more when louder. */
        fun burstStep(push: Float): Float = 0.34f + 0.1f * push.coerceIn(0f, 1f)

        /** Quiet needed between two pops so one word that dips mid-way isn't counted twice. */
        const val BURST_GAP_MS = 100

        /** "I said it!" plays at least this strong (medium). */
        const val PRESSED = 0.5f

        /** The same for a word or line with no named sound: its first consonant's. */
        fun modeForLine(line: String, sound: String?): Mode = modeFor(sound ?: Swaps.firstSound(line.trim().lowercase()))

        /** Which way his voice moves the action for a word practising [sound]. */
        fun modeFor(sound: String?): Mode = when (sound?.lowercase()) {
            "p", "b", "t", "d", "g", "k" -> Mode.BURSTS
            "s", "m", "f", "sh" -> Mode.HOLD
            else -> Mode.LOUDNESS
        }
    }
}
