package com.zeronolidge.speechapp.logic

/**
 * How his voice moves a voice story's action, live: every reading that counts as voice pushes
 * [progress] on (louder pushes faster; holding the sound keeps it moving; silence stops it), and
 * the loudest push sets how big the flourish is ([strength]). Nothing here can fail:
 *  - a try that reached the end, or any clear voice, completes (the rest at a gentle speed);
 *  - a quiet or soft try nudges it partway, Rory says "Again, together!", and he tries again;
 *  - after [MAX_TRIES] it completes gently anyway, and "I said it!" does the same,
 * so the next beat never depends on a loud voice.
 */
class VoicePush {
    var progress = 0f
        private set
    private var peak = 0f
    var tries = 0
        private set

    /** One reading of [push] (0 to 1, see [VoiceMeter.push]) lasting [ms]. Returns true once it reaches the end. */
    fun feed(push: Float, ms: Int): Boolean {
        if (push > 0f) {
            peak = maxOf(peak, push)
            progress = minOf(1f, progress + ms / 1000f * speed(push))
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
            tries >= MAX_TRIES -> Next.FINISH_GENTLY
            else -> {
                // A quiet try still moves it a little: the action shows his try counted.
                progress = minOf(NUDGE_CAP, progress + if (soft) 0.3f else 0.18f)
                Next.AGAIN_TOGETHER
            }
        }
    }

    /** "I said it!": the same action, at a gentle speed. */
    fun pressed() {
        if (peak == 0f) peak = 0.5f
    }

    enum class Next { FINISH_GENTLY, AGAIN_TOGETHER }

    companion object {
        const val MAX_TRIES = 2
        /** A quiet try never gets it more than this far, so the end is always his voice or the gentle finish. */
        const val NUDGE_CAP = 0.6f

        /** Progress per second at a push: a whisper takes about 2.5 s of sound, a big voice under 0.6 s. */
        fun speed(push: Float): Float = 0.4f + 1.4f * push.coerceIn(0f, 1f)
    }
}
