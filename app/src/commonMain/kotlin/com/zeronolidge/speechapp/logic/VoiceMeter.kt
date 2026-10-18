package com.zeronolidge.speechapp.logic

/**
 * Decides whether the child said something, from microphone loudness alone (no speech
 * recognition, which is unreliable with young voices and on iPhones and iPads).
 *
 * Feed it one loudness reading (RMS, 0 to 1) per step. The first [settleMs] measure the room;
 * after that, anything louder than the room counts as voice. It is deliberately generous: a
 * whisper or part of a word counts as a try. [heard] becomes true after [needMs] of voice. Listening ends early a moment after the child stops talking, or after
 * [windowMs] at the latest.
 */
class VoiceMeter(
    private val windowMs: Int = 4000,
    settleMs: Int = 300,
    private val needMs: Int = 150,
    private val tailMs: Int = 700,
    /**
     * The room's loudness saved from an earlier turn (see [roomAfter]). With it there's no
     * settling: his voice counts from the first reading, and the room is never measured right
     * after Rory talks, when the child may already be answering.
     */
    knownRoom: Float? = null,
) {
    // With a known room: no settling, only a short lead in which nothing counts (the last
    // moment of Rory's voice can still be in the room as his turn starts).
    private val settleMs = if (knownRoom != null) LEAD_MS else settleMs
    private val measuresRoom = knownRoom == null
    private var t = 0
    private var roomSum = knownRoom ?: 0f
    private var roomCount = if (knownRoom != null) 1 else 0
    private val quiet = ArrayList<Float>()
    private var voiceMs = 0
    private var lastVoiceAt = -1
    private var loudest = 0f

    val heard: Boolean get() = voiceMs >= needMs

    /**
     * A soft try: some sound above the room, but not enough to count as a clear voice (a tiny
     * whisper, a breath of a word). Voice stories answer it with a smaller version of the action.
     */
    val soft: Boolean get() = !heard && (voiceMs > 0 || loudest > maxOf(MIN_VOICE * 0.5f, room * 1.4f))

    /** How loud the room was before the child spoke. */
    val room: Float get() = if (roomCount == 0) 0f else roomSum / roomCount

    /** Anything louder than this counts as voice. */
    val threshold: Float get() = maxOf(MIN_VOICE, room * 2.2f)

    /**
     * How hard this reading pushes, 0 to 1: nothing below the voice threshold (or while the room
     * is still being measured), louder = more. A voice story moves its action with it, live.
     */
    fun push(rms: Float): Float = if (t <= settleMs || rms <= threshold) 0f else display(rms).coerceAtLeast(0.08f)

    /** Adds a reading covering [stepMs]. Returns true when listening is finished. */
    fun feed(rms: Float, stepMs: Int): Boolean {
        t += stepMs
        if (t <= settleMs) {
            if (measuresRoom) {
                roomSum += rms
                roomCount++
            }
        } else {
            loudest = maxOf(loudest, rms)
        }
        if (t > settleMs && rms > threshold) {
            voiceMs += stepMs
            lastVoiceAt = t
        } else if (t > settleMs) {
            quiet += rms
        }
        return (heard && t - lastVoiceAt >= tailMs) || t >= windowMs
    }

    /**
     * The room's loudness to keep for the next turn: the quieter half of this turn's readings
     * that weren't voice (so his quiet voice never raises the bar), never more than 1.5 times
     * the room it started from. Null when there weren't enough quiet readings.
     */
    fun roomAfter(): Float? {
        if (quiet.size < 4) return if (roomCount > 0) room else null
        val low = quiet.sorted().take(quiet.size / 2)
        val est = low.sum() / low.size
        return if (roomCount > 0) minOf(est, maxOf(room, 0.001f) * 1.5f) else est
    }

    companion object {
        /** How long the room is measured when nothing is saved (before Rory's first prompt). */
        const val ROOM_CHECK_MS = 400

        /** With a known room, the start of a turn where nothing counts yet. */
        const val LEAD_MS = 150

        /** A whisper close to a tablet is above this; a quiet room is below it. */
        const val MIN_VOICE = 0.012f

        /** Loudness on a 0 to 1 scale for the listening animation. */
        fun display(rms: Float): Float = (rms / 0.15f).coerceIn(0f, 1f)
    }
}
