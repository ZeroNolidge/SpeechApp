package com.zeronolidge.speechapp.audio

import com.zeronolidge.speechapp.logic.ClipText

/**
 * Plays pre-made voice clips (made once with tools/make_voice.py). Each key is a line's
 * normalized text ([ClipText.normalize]).
 */
interface ClipPlayer {
    /** True once the list of clips has loaded (false if there are none, e.g. before they're made). */
    val ready: Boolean
    /** Calls [then] once the clip list has loaded, or after [maxMs] if it hasn't (or there is none). */
    fun whenReady(maxMs: Int, then: () -> Unit)
    fun has(key: String): Boolean
    /** Plays [keys] back to back with no gap; [slow] plays them slower without changing the voice's pitch. */
    fun play(keys: List<String>, slow: Boolean, onEnd: (ok: Boolean) -> Unit)
    fun stop()
    /** The clip playing right now: how loud it is at this moment and its text (null when none). */
    fun mouth(): MouthNow? = null
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
        // Just after start the clip list may still be loading: wait a moment rather than switch voices.
        if (!clips.ready) {
            val my = ++token
            return clips.whenReady(2000) { if (my == token) speakNow(text, rate, onEnd) }
        }
        speakNow(text, rate, onEnd)
    }

    private fun speakNow(text: String, rate: Float, onEnd: (spoken: Boolean) -> Unit) {
        val parts = if (clips.ready) ClipText.plan(text) { clips.has(it) } else emptyList()
        // K3a: every spoken line, logged as it starts, with whether it's fully covered by
        // pre-made clips (ok=false: no clip at all, or part of it falls back to the browser
        // voice) -- proves, alongside VoiceDetector's MIC open/closed log, that no clip or
        // browser-voice line ever starts while the microphone is open.
        println("CLIP play text=$text ok=${parts.isNotEmpty() && parts.none { it is ClipText.Part.Speak }}")
        // Words with no clip are said by the device voice (a different voice): log them, so the
        // smoke test can list any line that still mixes voices.
        if (clips.ready) parts.filterIsInstance<ClipText.Part.Speak>().forEach { println("VOICEGAP [${it.text}] in: $text") }
        // No clips for this line at all: the device voice says it whole, so it sounds natural.
        if (parts.none { it is ClipText.Part.Clips }) return voice.speakOne(text, rate, onEnd)
        val my = ++token
        val slow = rate < 0.65f
        var anySpoken = false
        fun run(i: Int) {
            if (my != token) return
            if (i == parts.size) return onEnd(anySpoken)
            when (val part = parts[i]) {
                is ClipText.Part.Clips -> clips.play(part.keys, slow) { ok ->
                    if (ok) {
                        anySpoken = true
                        run(i + 1)
                    } else if (my == token) {
                        // A clip was missing, wouldn't load or was silent: the device voice says
                        // this part instead of leaving a gap.
                        val words = part.keys.filter { it != ClipText.PAUSE }.joinToString(" ")
                        voice.speakOne(words, rate) { spoken -> anySpoken = anySpoken || spoken; run(i + 1) }
                    }
                }
                is ClipText.Part.Speak -> voice.speakOne(part.text, rate) { ok -> anySpoken = anySpoken || ok; run(i + 1) }
            }
        }
        run(0)
    }

    override fun mouth(): MouthNow? = clips.mouth() ?: voice.mouth()

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
