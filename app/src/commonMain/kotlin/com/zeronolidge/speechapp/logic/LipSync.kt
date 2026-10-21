package com.zeronolidge.speechapp.logic

/**
 * Rory's mouth, from how loud the clip playing right now is: it opens with the sound, so it
 * moves with every word (and slower on the slow third model, since the sound is slower).
 *
 * Words are found in the sound itself: a new word starts after a short quiet ([GAP_MS]) or after
 * the sound dips well below its last peak and comes back up ("pop pop pop" has no quiet between
 * its words, only dips). A dip right after a word has only just started (within [MIN_WORD_MS]) is
 * its own burst settling into its vowel, or a ripple in a short burst's tail, not a real gap to
 * the next word, so it's kept as the same word; a rise that comes back that soon is ignored
 * (only the dip's end-closure, already applied, stands). The rise out of a dip is judged against
 * the dip's own lowest point as well as the word's peak ([RISE_VALLEY], [RISE_PEAK]): a long,
 * gentle fall (a held vowel easing off) can wander below half the peak without ever truly
 * bottoming out, so comparing only to the peak can leave the next word with nothing to rise
 * above. That also guards against ever splitting a clip into more words than its text has: once
 * the last word has started, no further dip can start another. A word that starts after a real
 * quiet stays provisional until it's clearly not just the last word's release puff (the short,
 * quiet burst of air after a p/b/m, e.g. "pop"'s tail): loud or long enough ([RELEASE_LEVEL],
 * [RELEASE_FRAMES]) locks it in as its own word; a puff that goes quiet again before that is
 * folded back into the word it followed, so it's never counted or closed for on its own. Each
 * confirmed word is matched to the next word of the [step] text. The lips press together on
 * every p, b and m: for [CLOSED_MS] at the start of a word that starts with one, and at the end
 * of a word that ends with one ("pop"), and for the whole of a held "mmm". Plain Kotlin,
 * unit-tested.
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
    // The dip's lowest point so far, for judging the rise back out of it (see [RISE_VALLEY]).
    private var valley = 0f
    // How long the current word has been sounding: a dip this soon after it started is the
    // word's own burst settling, not a real gap to the next word (see [MIN_WORD_MS]).
    private var wordMs = 0

    // A word started after a real quiet stays provisional (undoable) until it's loud or long
    // enough not to be the last word's release puff; these hold the state to undo it into.
    private var provisional = false
    private var burstFrames = 0
    private var burstPeak = 0f
    private var snapWord = -1
    private var snapPeak = 0f
    private var snapDipping = false
    private var snapWordMs = 0
    private var snapClosedMs = 0

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
        wordMs = 0
    }

    private fun endWord() {
        // "pop", "tub", "jam": the lips close as the word ends.
        if (endsClosed(current())) closedMs = maxOf(closedMs, CLOSED_MS)
    }

    // Starts a new word after a real quiet, but keeps enough of the old one to undo it if this
    // turns out to be only the old word's release puff, not a word of its own.
    private fun tryStartWord() {
        snapWord = word
        snapPeak = peak
        snapDipping = dipping
        snapWordMs = wordMs
        snapClosedMs = closedMs
        startWord()
        provisional = true
        burstFrames = 0
        burstPeak = 0f
    }

    private fun revertWord() {
        word = snapWord
        peak = snapPeak
        dipping = snapDipping
        wordMs = snapWordMs
        closedMs = snapClosedMs
        provisional = false
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
            provisional = false
        }
        val l = level ?: 0f
        if (l < OFF) {
            if (inSound) {
                // A short, quiet burst since the last real word started is its release puff
                // ("pop"'s puff of air), not a word of its own: undo it rather than count it.
                if (provisional) revertWord() else endWord()
                inSound = false
            }
            quietMs += dtMs
        } else {
            if (!inSound) {
                // A new word after a quiet (or the same word picking up again after a tiny gap).
                if (quietMs >= GAP_MS || word < 0) tryStartWord()
                inSound = true
            } else {
                wordMs += dtMs
                if (dipping) {
                    if (l >= valley * RISE_VALLEY && l >= peak * RISE_PEAK) {
                        // Back up after a dip: only the next word (with no quiet between, "pop
                        // pop pop") once the current word has sounded for a moment and there's a
                        // word left to move to; a dip and rise this soon after it started is its
                        // own burst settling into its vowel, or a ripple in a short burst's tail,
                        // not a real gap, so stay on it (its end-closure, already applied, stands).
                        if (wordMs >= MIN_WORD_MS && word + 1 < words.size) startWord() else dipping = false
                    } else {
                        valley = minOf(valley, l)
                    }
                } else if (peak > 0f && l < peak * DIP) {
                    dipping = true
                    valley = l
                    endWord()
                }
            }
            if (!dipping) peak = maxOf(peak, l)
            if (provisional) {
                // Loud or long enough not to be a release puff: lock the word in for good.
                burstFrames++
                burstPeak = maxOf(burstPeak, l)
                if (burstPeak >= snapPeak * RELEASE_LEVEL || burstFrames > RELEASE_FRAMES) provisional = false
            }
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
        /** Back above this multiple of the dip's lowest point: the next word starts. */
        const val RISE_VALLEY = 2f
        /** Back above this part of the last peak after a dip: the next word starts. */
        const val RISE_PEAK = 0.5f
        /** A word has to have sounded at least this long before a dip and rise in it can be the
         * next word; sooner, it's the word's own burst settling into its vowel, or a ripple in a
         * short burst's tail. */
        const val MIN_WORD_MS = 80
        /** Above this part of the last word's peak, a word started after a quiet is locked in
         * (not its release puff). */
        const val RELEASE_LEVEL = 0.25f
        /** Sounding longer than this many frames, a word started after a quiet is locked in
         * (not its release puff, which is brief). */
        const val RELEASE_FRAMES = 4
    }
}
