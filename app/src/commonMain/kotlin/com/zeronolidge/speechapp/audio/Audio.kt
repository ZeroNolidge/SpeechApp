package com.zeronolidge.speechapp.audio

/**
 * Talking, listening and recording, as the game sees them. Android uses its built-in
 * speech engines; the web version uses the browser's Web Speech and MediaRecorder APIs.
 */
interface Speaker {
    /** True while something is being said. */
    val speaking: Boolean

    /** Called when speech didn't start or finish in time, so the app can offer a big replay button. */
    var onTrouble: (() -> Unit)?

    /** 1.0 is normal speed. Grown-ups can change this in settings. */
    var rate: Float

    /**
     * Speaks [text], interrupting anything already being said.
     * [slow] uses about half speed, for modelling a word carefully.
     * [onDone] runs on the main thread when speaking ends (or is interrupted).
     */
    fun say(text: String, slow: Boolean = false, onDone: (() -> Unit)? = null)

    /** Like [say] but waits for anything already queued to finish first. */
    fun then(text: String, slow: Boolean = false, onDone: (() -> Unit)? = null)

    /** Stops talking. Unlike being interrupted by [say], this drops any pending onDone callbacks. */
    fun stop()

    fun shutdown()

    /** What's being said right now, for Rory's mouth (null: not known, e.g. the device's own voice). */
    fun mouth(): MouthNow? = null
}

/** The voice at this moment: its loudness ([level], RMS of the clip's sound) and the text of the piece being played. */
class MouthNow(val level: Float, val text: String)

/**
 * The platform's voice, one utterance at a time. [QueueSpeaker] does the queueing, interrupting
 * and time-outs on top, the same way on every platform.
 */
interface VoiceEngine {
    /**
     * Speaks [text] at [rate] (1.0 is normal). [onEnd] runs once on the main thread when it ends:
     * true if it was spoken, false if it never started (within a couple of seconds) or failed.
     */
    fun speakOne(text: String, rate: Float, onEnd: (spoken: Boolean) -> Unit)
    /** Stops talking now. A pending [onEnd] may still arrive; callers ignore it. */
    fun cancel()
    fun shutdown()
    /** See [Speaker.mouth]. */
    fun mouth(): MouthNow? = null
}

/**
 * The platform's microphone, as a stream of loudness readings. [VoiceDetector] decides on top
 * whether the child spoke, the same way on every platform.
 */
interface MicLevels {
    /** False if this device has no usable microphone at all. */
    val available: Boolean

    /**
     * Starts the microphone and calls [onLevel] with the loudness (RMS, 0 to 1) about every
     * [STEP_MS] milliseconds, on the main thread. Calls [onFail] if the microphone can't be opened
     * (no microphone, or permission denied).
     */
    fun start(onLevel: (Float) -> Unit, onFail: () -> Unit)

    /** Pauses metering between turns. On platforms that keep a stream open for reuse (the web)
     * this does NOT close the microphone; see [release] for that. */
    fun stop()

    /** Fully releases the microphone (leaving a voice story, or the app going to the background).
     * Platforms with nothing to keep open between turns can just treat this as [stop]. */
    fun release() = stop()

    /**
     * What the last reading (just before [start]'s onLevel call) sounded like: the share of its
     * energy in the voice range (about 150 to 2000 Hz), and how voiced it is (0 noise to 1 a
     * clear voice). 1 when this microphone can't tell. Pop words use these so a cough or a tap
     * on the glass doesn't count.
     */
    val band: Float get() = 1f
    val periodic: Float get() = 1f

    companion object {
        const val STEP_MS = 50
    }
}

/**
 * The "parrot": records the child for a few seconds, then plays it straight back,
 * so they can hear how they sound.
 */
interface Parrot {
    fun recordThenPlay(seconds: Int = 3, onPlaying: () -> Unit, onDone: () -> Unit)

    /**
     * The copy-me parrot that waits for him: records, and as soon as he has spoken (and stopped)
     * plays it back. [onLevel] gives his loudness (0 to 1) for the listening look. If there's no
     * voice within [waitMs], nothing is played and [onQuiet] is called (the caller keeps
     * listening). [onDone] after the playback.
     */
    fun listenThenPlay(waitMs: Int, onLevel: (Float) -> Unit, onPlaying: () -> Unit, onQuiet: () -> Unit, onDone: () -> Unit) =
        recordThenPlay(3, onPlaying, onDone)

    fun stop()
}

/** Tiny paper sounds for taps: a soft crinkle when a button is pressed, a pop for other taps. */
interface Sfx {
    fun crinkle()
    fun pop()

    object Silent : Sfx {
        override fun crinkle() {}
        override fun pop() {}
    }
}
