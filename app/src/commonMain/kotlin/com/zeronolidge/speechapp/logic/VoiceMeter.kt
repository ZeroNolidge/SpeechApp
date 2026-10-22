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
    private val tailMs: Int = 900,
    /**
     * The room's loudness saved from an earlier turn (see [roomAfter]). With it there's no
     * settling: his voice counts from the first reading, and the room is never measured right
     * after Rory talks, when the child may already be answering.
     */
    knownRoom: Float? = null,
    /**
     * Pop words (p b t d g k), each burst of sound judged as a whole:
     *  - voiced (a vowel in it) for [POP_MIN_MS]: his word, counted at once;
     *  - one or two short readings in the voice range (a puff can fall across two, even with a
     *    quiet reading between): a bare "p" puff, counted (now and then a soft cough or a sniff
     *    counts too, which is fine);
     *  - a longer unvoiced burst that holds its loudness: a (loud) whisper, counted;
     *  - a longer unvoiced burst that dies straight away (a cough), or a click outside the voice
     *    range (a clap, a tap on the glass): not counted.
     */
    private val pops: Boolean = false,
) {
    // With a known room: no settling, only a short lead in which nothing counts (the last
    // moment of Rory's voice can still be in the room as his turn starts).
    private val settleMs = if (knownRoom != null) LEAD_MS else settleMs
    private val measuresRoom = knownRoom == null
    private var t = 0
    private var roomSum = knownRoom ?: 0f
    private var roomCount = if (knownRoom != null) 1 else 0
    private val lead = ArrayList<Float>()
    private val recent = ArrayDeque<Float>()
    private var voiceMs = 0
    private var lastVoiceAt = -1
    private var loudest = 0f
    // The current run of sound: when it started, and whether there was quiet before it in this
    // turn (an onset: him starting a sound, not a room that was noisy all along).
    private var runStart = -1
    private var runOnset = false
    private var sawQuiet = measuresRoom
    private var popRunMs = 0
    // The current burst for pop words: its readings, whether it already counted, its
    // energy-weighted voice-range share (over its first two readings), and the readings' loudness.
    private var burstCounted = false
    // A short burst may have one quiet reading in it: a puff split across two readings.
    private var burstGap = false
    private var burstBand = 0f
    private var burstBandWeight = 0f
    private val burst = ArrayList<Float>()
    // Readings that weren't his voice: the room, for [roomAfter].
    private val quiet = ArrayList<Float>()
    // The previous reading, so two readings that are each a bit under the threshold, but whose
    // combined energy clears it, can still start a burst (a puff split so neither half alone
    // reads as loud enough).
    private var prevRms = 0f
    private var prevBand = 1f

    /** How hard the last reading pushed (see [push]), with the pop rules applied. */
    var lastPush = 0f
        private set

    val heard: Boolean get() = voiceMs >= needMs

    /** How long there was voice, and the loudest reading (0 to 1 on the display scale), for this try. */
    val voicedMs: Int get() = voiceMs
    val peak: Float get() = display(loudest)

    /**
     * A soft try: some sound above the room, but not enough to count as a clear voice (a tiny
     * whisper, a breath of a word). Voice stories answer it with a smaller version of the action.
     */
    val soft: Boolean get() = !heard && (voiceMs > 0 || loudest > maxOf(MIN_VOICE * 0.5f, room * 1.4f))

    /** How loud the room was before the child spoke. */
    val room: Float get() = if (roomCount == 0) 0f else roomSum / roomCount

    /** Anything louder than this counts as voice. */
    val threshold: Float get() = maxOf(MIN_VOICE, room * ROOM_FACTOR)

    /**
     * How hard this reading pushes, 0 to 1: nothing below the voice threshold (or while the room
     * is still being measured), louder = more. A voice story moves its action with it, live.
     */
    fun push(rms: Float): Float = if (t <= settleMs || rms <= threshold) 0f else display(rms).coerceAtLeast(0.08f)

    /**
     * Adds a reading covering [stepMs]. [band] is the share of its energy in the voice range and
     * [periodic] how voiced it is (both 0 to 1; 1 when the microphone can't tell). Returns true
     * when listening is finished.
     */
    fun feed(rms: Float, stepMs: Int, band: Float = 1f, periodic: Float = 1f): Boolean {
        t += stepMs
        lastPush = 0f
        if (t <= settleMs) {
            if (measuresRoom) {
                roomSum += rms
                roomCount++
            } else {
                lead += rms
                if (rms <= threshold) sawQuiet = true
                // The room got louder since the saved level (a TV came on): even the quietest
                // moment before he speaks is over the bar, so the bar goes up with it.
                // (Only for room-like levels: a loud start is him answering straight away.)
                if (t + stepMs > settleMs && lead.isNotEmpty() && lead.min() > threshold && lead.min() <= LOUD_ROOM) {
                    setRoom(lead.min())
                    sawQuiet = true
                }
            }
            return false
        }
        loudest = maxOf(loudest, rms)
        val single = rms > threshold
        // Two readings that are each a bit under the threshold, but together carry more energy
        // than one reading at the threshold: a puff split so neither half alone reads as loud
        // enough. Only starts a burst (the voice-range check comes later, judging the burst).
        val combo = pops && !single && burst.isEmpty() &&
            rms >= threshold * 0.5f && prevRms >= threshold * 0.5f &&
            kotlin.math.sqrt(rms * rms + prevRms * prevRms) > threshold
        val over = single || combo
        recent.addLast(rms)
        if (recent.size * stepMs > NOISE_MS) recent.removeFirst()
        if (over) {
            if (runStart < 0) {
                runStart = t - stepMs
                runOnset = sawQuiet
            }
        } else {
            runStart = -1
            sawQuiet = true
            quiet += rms
        }
        // Steady sound that never dips is the room, not him: it raises the bar and never counts
        // as his try, so noise can't finish a story. A sound he starts (quiet before it) is his
        // until it has lasted [HOLD_MAX_MS], so a long "sssss" counts; the room is never updated
        // while he's making a sound. Sound that was there from the start is room after [NOISE_MS].
        val runMs = if (runStart < 0) 0 else t - runStart
        if (over && runMs >= (if (runOnset) HOLD_MAX_MS else NOISE_MS) && recent.min() > threshold) {
            setRoom(recent.min())
            voiceMs = 0
            lastVoiceAt = -1
            loudest = 0f
            runStart = -1
            popRunMs = 0
            burst.clear()
            burstCounted = false
            burstGap = false
            burstBandWeight = 0f
            return t >= windowMs
        }
        if (over) {
            if (pops) {
                if (combo) {
                    // The previous reading wasn't over the threshold on its own: fold it into
                    // this burst now, so the two are judged (and their band averaged) together.
                    burstBand = prevBand
                    burstBandWeight = prevRms
                    burst += prevRms
                }
                if (burst.size < 2) {
                    // The burst's band, weighted by energy across its first two readings: a
                    // puff's faint leading edge shouldn't outweigh its louder second half.
                    burstBand = (burstBand * burstBandWeight + band * rms) / (burstBandWeight + rms)
                    burstBandWeight += rms
                }
                burst += rms
                val voiced = band >= POP_BAND && periodic >= POP_PERIODIC
                if (voiced) popRunMs += stepMs
                if (burstCounted) {
                    voiceMs += stepMs
                    lastVoiceAt = t
                    lastPush = push(rms)
                } else if (popRunMs >= POP_MIN_MS) {
                    // His word: voiced long enough. All of the burst so far counts.
                    burstCounted = true
                    voiceMs += burst.size * stepMs
                    lastVoiceAt = t
                    lastPush = push(burst.max())
                }
            } else {
                voiceMs += stepMs
                lastVoiceAt = t
                lastPush = push(rms)
            }
        } else {
            if (pops && burst.isNotEmpty() && !burstCounted && burst.size <= 2 && !burstGap) {
                // A puff can fall across readings (about 30 ms of sound): one quiet reading
                // doesn't end a short burst yet.
                burstGap = true
            } else if (pops && burst.isNotEmpty()) {
                // The burst has ended: judge it as a whole if it didn't count already.
                if (!burstCounted && Pops.counts(burst, burstBand)) {
                    voiceMs += burst.size * stepMs
                    lastVoiceAt = t
                    lastPush = push(burst.max())
                }
                burst.clear()
                burstCounted = false
                burstGap = false
                burstBandWeight = 0f
            }
            popRunMs = 0
        }
        prevRms = rms
        prevBand = band
        // He's still holding a sound he started: keep listening (up to [HOLD_MAX_MS] past the window).
        val holding = runStart >= 0 && runOnset && t < windowMs + HOLD_MAX_MS
        return (heard && t - lastVoiceAt >= tailMs) || (t >= windowMs && !holding)
    }

    private fun setRoom(v: Float) {
        roomSum = v
        roomCount = 1
    }

    /**
     * The room's loudness to keep for the next turn: the quietest fifth of this turn's readings
     * that weren't his voice (so a long held sound never raises it). It follows the room both
     * ways: down when it's quieter, and up when it's louder (steady noise becomes the room, see
     * [feed]), so the bar keeps up with background noise. With too few quiet readings it keeps
     * the room it had; null when there's nothing to go on.
     */
    fun roomAfter(): Float? {
        if (quiet.size < 4) return if (roomCount > 0) room else null
        val low = quiet.sorted().take(maxOf(1, quiet.size / 5))
        return low.sum() / low.size
    }

    companion object {
        /** How long the room is measured when nothing is saved (before Rory's first prompt). */
        const val ROOM_CHECK_MS = 400

        /** With a known room, the start of a turn where nothing counts yet. */
        const val LEAD_MS = 150

        /** Steady sound this long with no dip, there from the start of the turn, is background noise. */
        const val NOISE_MS = 2000

        /** A sound he starts can be held this long and still be his (Rory's own "sssss" is 2.25 s). */
        const val HOLD_MAX_MS = 5000

        /** A voiced burst counts as his word once it has lasted this long. */
        const val POP_MIN_MS = 70

        /** A pop's share of energy in the voice range, at least. */
        const val POP_BAND = 0.35f

        /** A pop's vowel is voiced at least this much. */
        const val POP_PERIODIC = 0.35f

        /** A longer unvoiced burst is a whisper if the rest of it stays at least this loud (a part of its start); a cough dies away. */
        const val WHISPER_HOLD = 0.5f

        /** A room level saved on another day is used at most this loud (it follows the room after one turn). */
        const val SAVED_ROOM_MAX = 0.02f

        /** Louder than this at the very start of a turn is his voice, not the room. */
        const val LOUD_ROOM = 0.035f

        /** A whisper close to a tablet is above this; a quiet room is below it. Lowered from
         * 0.012 (real iPads): a young child's soft or breathy sounds (p, f, s) were sitting right
         * under the old floor and reading as silence. */
        const val MIN_VOICE = 0.006f

        /** How much louder than the room counts as voice. Lowered from 2.2 along with
         * [MIN_VOICE], so a noisy room still needs a clearly louder voice but a quiet room (most
         * turns) stays generous. */
        const val ROOM_FACTOR = 1.6f

        /** Loudness on a 0 to 1 scale for the listening animation. */
        fun display(rms: Float): Float = (rms / 0.15f).coerceIn(0f, 1f)
    }
}

/** How a finished unvoiced burst on a pop word is judged (see VoiceMeter's pops). */
object Pops {
    /**
     * [levels]: the burst's readings (RMS, 50 ms each); [band]: its first reading's share of
     * energy in the voice range. True for a bare "p" puff or a whisper, false for a cough or a
     * click.
     */
    fun counts(levels: List<Float>, band: Float): Boolean = when {
        levels.isEmpty() -> false
        // One or two short readings: a "p" puff if it's in the voice range (a clap or a tap
        // isn't). A real puff always counts; an odd soft cough or sniff counting too is fine.
        levels.size <= 2 -> band >= VoiceMeter.POP_BAND
        // Longer: a whisper holds its loudness; a cough bursts and dies away.
        else -> levels.drop(1).average() >= VoiceMeter.WHISPER_HOLD * levels.first()
    }
}
