package com.zeronolidge.speechapp.logic

/**
 * Rory's mouth, from how loud the clip playing right now is: it opens with the sound, so it
 * moves with every word (and slower on the slow third model, since the sound is slower).
 *
 * Each new burst of sound after a short quiet ([GAP_MS]) is taken as the start of the next word
 * of the [step] text. A word starting with p, b or m shows closed lips for its first
 * [CLOSED_MS], as the lips really do. Plain Kotlin, unit-tested.
 */
class LipSync {
    private var text: String? = null
    private var words: List<String> = emptyList()
    private var word = -1
    private var quietMs = GAP_MS
    private var closedMs = 0
    private var open = 0f

    /** True while the lips are pressed together (the start of a p, b or m word). */
    var closed = false
        private set

    /**
     * One frame: the clip's loudness [level] (RMS, null when nothing is playing) and its [text],
     * [dtMs] since the last frame. Returns how open the mouth is, 0 to 1.
     */
    fun step(level: Float?, text: String?, dtMs: Int): Float {
        if (text != this.text) {
            this.text = text
            words = text?.lowercase()?.split(Regex("[^a-z']+"))?.filter { it.isNotEmpty() } ?: emptyList()
            word = -1
            quietMs = GAP_MS
            closedMs = 0
        }
        val l = level ?: 0f
        if (l < OFF) {
            quietMs += dtMs
        } else {
            if (quietMs >= GAP_MS) {
                // A new word starts.
                word++
                val w = words.getOrNull(word)
                closedMs = if (w != null && w.first() in "pbm") CLOSED_MS else 0
            }
            quietMs = 0
        }
        closed = closedMs > 0 && level != null
        val target = when {
            closed -> 0f
            else -> ((l - OFF) / (FULL - OFF)).coerceIn(0f, 1f)
        }
        if (closedMs > 0) closedMs -= dtMs
        // Opens quickly, closes a little more gently, like a real mouth.
        open += (target - open) * if (target > open) 0.6f else 0.35f
        if (closed) open = 0f
        return open
    }

    companion object {
        /** Quieter than this (RMS) the mouth is closed. Finished clips speak at about 0.1. */
        const val OFF = 0.015f
        /** This loud (RMS) the mouth is wide open. */
        const val FULL = 0.16f
        /** Quiet this long before a sound means a new word. */
        const val GAP_MS = 60
        /** p, b and m words start with the lips together this long. */
        const val CLOSED_MS = 80
    }
}
