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
}

/**
 * The platform's voice, one utterance at a time. [QueueSpeaker] does the queueing, interrupting
 * and time-outs on top, the same way on every platform.
 */
interface VoiceEngine {
    /** Speaks [text] at [rate] (1.0 is normal). [onEnd] runs once on the main thread when it ends or fails. */
    fun speakOne(text: String, rate: Float, onEnd: () -> Unit)
    /** Stops talking now. A pending [onEnd] may still arrive; callers ignore it. */
    fun cancel()
    fun shutdown()
}

/** Listens for one short phrase. Results always arrive on the main thread. */
interface Listener {
    val available: Boolean

    /**
     * [onResult] gets every guess the recognizer made (best first), or an empty list
     * if it heard nothing it could understand.
     */
    fun listen(hint: String, onLevel: (Float) -> Unit = {}, onResult: (List<String>) -> Unit)

    fun cancel()
}

/**
 * The "parrot": records the child for a few seconds, then plays it straight back,
 * so they can hear how they sound.
 */
interface Parrot {
    fun recordThenPlay(seconds: Int = 3, onPlaying: () -> Unit, onDone: () -> Unit)
    fun stop()
}
