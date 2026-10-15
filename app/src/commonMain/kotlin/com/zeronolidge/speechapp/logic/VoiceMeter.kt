package com.zeronolidge.speechapp.logic

/**
 * Decides whether the child said something, from microphone loudness alone (no speech
 * recognition, which is unreliable with young voices and on iPhones and iPads).
 *
 * Feed it one loudness reading (RMS, 0 to 1) per step. The first [settleMs] measure the room;
 * after that, anything clearly louder than the room counts as voice. [heard] becomes true after
 * [needMs] of voice. Listening ends early a moment after the child stops talking, or after
 * [windowMs] at the latest.
 */
class VoiceMeter(
    private val windowMs: Int = 4000,
    private val settleMs: Int = 300,
    private val needMs: Int = 250,
    private val tailMs: Int = 700,
) {
    private var t = 0
    private var roomSum = 0f
    private var roomCount = 0
    private var voiceMs = 0
    private var lastVoiceAt = -1

    val heard: Boolean get() = voiceMs >= needMs

    /** How loud the room was before the child spoke. */
    val room: Float get() = if (roomCount == 0) 0f else roomSum / roomCount

    /** Anything louder than this counts as voice. */
    val threshold: Float get() = maxOf(MIN_VOICE, room * 3f)

    /** Adds a reading covering [stepMs]. Returns true when listening is finished. */
    fun feed(rms: Float, stepMs: Int): Boolean {
        t += stepMs
        if (t <= settleMs) {
            roomSum += rms
            roomCount++
        } else if (rms > threshold) {
            voiceMs += stepMs
            lastVoiceAt = t
        }
        return (heard && t - lastVoiceAt >= tailMs) || t >= windowMs
    }

    companion object {
        /** Quiet speech a step away from a tablet is well above this; a quiet room is well below. */
        const val MIN_VOICE = 0.02f

        /** Loudness on a 0 to 1 scale for the listening animation. */
        fun display(rms: Float): Float = (rms / 0.15f).coerceIn(0f, 1f)
    }
}
