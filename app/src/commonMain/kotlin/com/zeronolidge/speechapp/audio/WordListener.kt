package com.zeronolidge.speechapp.audio

/**
 * On-device word recognition (Vosk in the browser; nothing on Android yet). It listens to the
 * same microphone turn as the loudness check, for a tiny list of words, and answers which one it
 * heard and how sure it is. It never blocks anything: not ready, failed or too slow simply
 * answers null, and the loudness check (which moves the story) carries on as before.
 * No audio and no transcript is ever kept.
 */
interface WordListener {
    /** Loaded and able to listen. */
    val ready: Boolean

    /** For the grown-ups page: OFF (not started), LOADING, READY or UNAVAILABLE. */
    val status: Status

    enum class Status { OFF, LOADING, READY, UNAVAILABLE }

    /** Starts loading the recogniser (once; a no-op after that). Fails silently. */
    fun prepare()

    /** Listen for these words during the next microphone turn ("[unk]" = not sure). */
    fun begin(grammar: List<String>)

    /** The turn is over: [onHeard] gets the words heard with their confidence, or null. Within about a second. */
    fun finish(onHeard: (List<Pair<String, Float>>?) -> Unit)

    /** Stop without an answer. */
    fun cancel()

    object None : WordListener {
        override val ready = false
        override val status = Status.UNAVAILABLE
        override fun prepare() {}
        override fun begin(grammar: List<String>) {}
        override fun finish(onHeard: (List<Pair<String, Float>>?) -> Unit) = onHeard(null)
        override fun cancel() {}
    }
}
