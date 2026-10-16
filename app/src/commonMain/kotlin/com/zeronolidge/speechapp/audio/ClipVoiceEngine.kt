package com.zeronolidge.speechapp.audio

import com.zeronolidge.speechapp.logic.ClipText

/**
 * Plays pre-made voice clips (made once with tools/make_voice.py). Each key is a line's
 * normalized text ([ClipText.normalize]).
 */
interface ClipPlayer {
    /** True once the list of clips has loaded (false if there are none, e.g. before they're made). */
    val ready: Boolean
    fun has(key: String): Boolean
    /** Plays [keys] back to back with no gap; [slow] plays them slower without changing the voice's pitch. */
    fun play(keys: List<String>, slow: Boolean, onEnd: (ok: Boolean) -> Unit)
    fun stop()
}

/**
 * Rory's voice: every line is played from the pre-made clips where they exist, piece by piece
 * with no gap (a name or a number in the middle is its own clip), and anything without a clip
 * (his name, the grown-up's own words) is said by the device's voice. [QueueSpeaker] does the
 * queue and time limits on top, as before.
 */
class ClipVoiceEngine(private val clips: ClipPlayer, private val voice: VoiceEngine) : VoiceEngine {
    private var token = 0

    override fun speakOne(text: String, rate: Float, onEnd: (spoken: Boolean) -> Unit) {
        val parts = if (clips.ready) ClipText.plan(text) { clips.has(it) } else emptyList()
        // No clips for this line at all: the device voice says it whole, so it sounds natural.
        if (parts.none { it is ClipText.Part.Clips }) return voice.speakOne(text, rate, onEnd)
        val my = ++token
        val slow = rate < 0.65f
        var anySpoken = false
        fun run(i: Int) {
            if (my != token) return
            if (i == parts.size) return onEnd(anySpoken)
            when (val part = parts[i]) {
                is ClipText.Part.Clips -> clips.play(part.keys, slow) { ok -> anySpoken = anySpoken || ok; run(i + 1) }
                is ClipText.Part.Speak -> voice.speakOne(part.text, rate) { ok -> anySpoken = anySpoken || ok; run(i + 1) }
            }
        }
        run(0)
    }

    override fun cancel() {
        token++
        clips.stop()
        voice.cancel()
    }

    override fun shutdown() {
        cancel()
        voice.shutdown()
    }
}
