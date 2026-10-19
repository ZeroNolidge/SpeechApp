package com.zeronolidge.speechapp.logic

/**
 * Rory's mouth, from how loud the clip playing right now is: it opens with the sound, so it
 * moves with every word (and slower on the slow third model, since the sound is slower).
 *
 * Words are found in the sound itself: a new word starts after a short quiet ([GAP_MS]) or after
 * the sound dips well below its last peak and comes back ("pop pop pop" has no quiet between its
 * words, only dips), and each is matched to the next word of the [step] text. The lips press
 * together on every p, b and m: for [CLOSED_MS] at the start of a word that starts with one, and
 * at the end of a word that ends with one ("pop"), and for the whole of a held "mmm". Plain
 * Kotlin, unit-tested.
 */
class LipSync {
    private var text: String? = null
    private var words: List<String> = emptyList()
    private var word = -1
    private var quietMs = GAP_MS
    private var closedMs = 0
    private var open = 0f
    private var inSound = false
    private var peak = 0f
    private var dipping = false

    /** True while the lips are pressed together (a p, b or m, or a held "mmm"). */
    var closed = false
        private set

    // Past the last word, the words come round again: "Pop! Pop! Pop!" plays as three clips of
    // the same text "pop", so the text never changes between them, and each one still closes.
    private fun current(): String? = if (words.isEmpty() || word < 0) null else words[word % words.size]

    private fun hum(w: String?) = w != null && w.all { it == 'm' }

    private fun endsClosed(w: String?) = w != null && w.isNotEmpty() && w.last() in "pbm"

    private fun startWord() {
        word++
        val w = current()
        closedMs = if (w != null && w.first() in "pbm") CLOSED_MS else 0
        peak = 0f
        dipping = false
    }

    private fun endWord() {
        // "pop", "tub", "jam": the lips close as the word ends.
        if (endsClosed(current())) closedMs = maxOf(closedMs, CLOSED_MS)
    }

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
            inSound = false
            dipping = false
        }
        val l = level ?: 0f
        if (l < OFF) {
            if (inSound) {
                endWord()
                inSound = false
            }
            quietMs += dtMs
        } else {
            if (!inSound) {
                // A new word after a quiet (or the same word picking up again after a tiny gap).
                if (quietMs >= GAP_MS || word < 0) startWord()
                inSound = true
            } else if (dipping && l > peak * RISE) {
                // Back up after a dip: the next word, with no quiet between ("pop pop pop").
                startWord()
            } else if (!dipping && peak > 0f && l < peak * DIP) {
                dipping = true
                endWord()
            }
            if (!dipping) peak = maxOf(peak, l)
            quietMs = 0
        }
        // A held "mmm": the lips stay together the whole time the sound lasts.
        val humming = inSound && hum(current())
        closed = level != null && (closedMs > 0 || humming)
        val target = if (closed) 0f else ((l - OFF) / (FULL - OFF)).coerceIn(0f, 1f)
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
        /** p, b and m close the lips this long (long enough to see). */
        const val CLOSED_MS = 100
        /** Below this part of the word's peak, the word is ending (a dip between words). */
        const val DIP = 0.45f
        /** Back above this part of the last peak after a dip: the next word starts. */
        const val RISE = 0.7f
    }
}
