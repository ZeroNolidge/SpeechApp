package com.zeronolidge.speechapp.audio

import com.zeronolidge.speechapp.logic.VoiceMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Did the child say something?" Listens to microphone loudness for a few seconds (see
 * [VoiceMeter]) and answers true (voice heard), false (quiet), or null (no microphone, or it
 * couldn't be opened). Never answers more than once per [listen], and never hangs.
 */
class VoiceDetector(private val mic: MicLevels, val words: WordListener = WordListener.None) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var session = 0
    private var guard: Job? = null

    val available: Boolean get() = mic.available

    /** For the MIC open/closed log only (K3a): true while a real microphone stream is open. On
     * iOS/iPadOS Safari an open microphone switches the audio session to play-and-record, which
     * ducks or silences any clip/browser-voice playback started while it's open -- so this is
     * also what proves, from the log, that nothing ever plays while the mic is open. */
    private var micOpenNow = false

    /** True while Rory is talking: a turn's microphone waits until he's done (it would hear him). */
    var isSpeaking: () -> Boolean = { false }

    /** The last try's voice: its loudest moment (0 to 1) and how long there was voice (ms). */
    var lastPeak = 0f
        private set
    var lastVoicedMs = 0
        private set

    /** When the last listening turn's microphone opened (taps right after are ignored). */
    var turnMark: kotlin.time.TimeMark? = null
        private set
    private var wordsOn = false

    /**
     * The room's loudness, saved from the last talk turn and reused by the next one, so the room
     * is never measured while (or right after) Rory talks. Only the first turn of a sitting
     * needs [checkRoom].
     */
    var room: Float? = null
        private set

    /** Called with each newly saved room level, so it's remembered across page loads. */
    var onRoom: (Float) -> Unit = {}

    /** A room level remembered from an earlier visit (capped, see [VoiceMeter.SAVED_ROOM_MAX]). */
    fun restoreRoom(saved: Float?) {
        if (room == null && saved != null && saved > 0f) room = minOf(saved, VoiceMeter.SAVED_ROOM_MAX)
    }

    /** What counts as voice right now (see [VoiceMeter.threshold]), for the test-only VOICETURN log. */
    val threshold: Float get() = maxOf(VoiceMeter.MIN_VOICE, (room ?: 0f) * VoiceMeter.ROOM_FACTOR)

    private fun saveRoom(v: Float) {
        room = v
        onRoom(v)
    }

    /**
     * The first talk turn of a sitting: a short quiet check of the room, done in the pause
     * before Rory's prompt, once anything still being said (the story's title) has finished. Calls [then] at once if the room is already
     * known, or without a microphone; never hangs.
     */
    fun checkRoom(then: () -> Unit) {
        if (room != null || !mic.available) {
            then()
            return
        }
        cancel()
        val s = ++session
        val levels = ArrayList<Float>()
        fun done() {
            if (s != session) return
            cancel()
            if (levels.size >= 3) {
                val low = levels.sorted().take(maxOf(1, levels.size * 3 / 4))
                saveRoom(low.sum() / low.size)
            }
            then()
        }
        guard = scope.launch {
            // Only once Rory (or the story title) has finished: the microphone would hear him.
            var waited = 0
            while (isSpeaking() && waited < 8000) {
                delay(100)
                waited += 100
            }
            delay(200)
            if (s != session) return@launch
            micOpenNow = true
            println("MIC open")
            mic.start(
                onLevel = { rms ->
                    if (s == session) {
                        levels += rms
                        if (levels.size * MicLevels.STEP_MS >= VoiceMeter.ROOM_CHECK_MS) done()
                    }
                },
                onFail = { done() },
            )
            delay(VoiceMeter.ROOM_CHECK_MS + 1500L)
            done()
        }
    }

    fun listen(
        windowMs: Int = 4000,
        onLevel: (Float) -> Unit,
        words: List<String>? = null,
        onWords: ((List<Pair<String, Float>>?) -> Unit)? = null,
        onResult: (heard: Boolean?) -> Unit,
    ) =
        listenTry(windowMs, onLevel, onVoice = null, words = words, onWords = onWords, onResult = { t ->
            onResult(when (t) { VoiceTry.HEARD -> true; VoiceTry.NO_MIC -> null; else -> false })
        })

    /**
     * Like [listen], with more detail: [onVoice] fires the moment a clear voice is heard (about
     * 0.15 s after he starts, long before listening ends), so a story can act on his word at
     * once; the result also tells a soft try ([VoiceTry.SOFT]) from silence.
     */
    fun listenTry(
        windowMs: Int = 4000,
        onLevel: (Float) -> Unit,
        onVoice: (() -> Unit)?,
        /** Every reading (each 50 ms): how hard his voice pushes right now, 0 when quiet (see [VoiceMeter.push]). */
        onPush: ((Float) -> Unit)? = null,
        /** Word listening: this turn's word list ([com.zeronolidge.speechapp.logic.WordCheck.grammar]). */
        words: List<String>? = null,
        /** What the recogniser heard (null: nothing, not ready or failed), shortly after [onResult]. */
        onWords: ((List<Pair<String, Float>>?) -> Unit)? = null,
        /** Pop sounds (p b t d g k): one short burst is a clear try; others need a little longer. */
        mode: com.zeronolidge.speechapp.logic.VoicePush.Mode = com.zeronolidge.speechapp.logic.VoicePush.Mode.LOUDNESS,
        onResult: (VoiceTry) -> Unit,
    ) {
        cancel()
        val s = ++session
        val pops = mode == com.zeronolidge.speechapp.logic.VoicePush.Mode.BURSTS
        val meter = VoiceMeter(windowMs, needMs = 50, tailMs = 250, knownRoom = room, pops = pops)
        var voiced = false
        val listenWords = words != null && this.words.ready
        fun done(result: VoiceTry) {
            if (s != session) return
            stopMic()
            if (result != VoiceTry.NO_MIC) meter.roomAfter()?.let { saveRoom(it) }
            lastPeak = meter.peak
            lastVoicedMs = meter.voicedMs
            onResult(result)
            // The words come a moment later (the story has already moved on loudness).
            if (wordsOn) {
                wordsOn = false
                if (result == VoiceTry.NO_MIC) {
                    this.words.cancel()
                    onWords?.invoke(null)
                } else {
                    this.words.finish { onWords?.invoke(it) }
                }
            } else {
                onWords?.invoke(null)
            }
        }
        fun outcome() = when {
            meter.heard -> VoiceTry.HEARD
            meter.soft -> VoiceTry.SOFT
            else -> VoiceTry.QUIET
        }
        guard = scope.launch {
            // Never listen while Rory is still talking: the microphone would hear him.
            var waited = 0
            while (isSpeaking() && waited < 4000) {
                delay(100)
                waited += 100
            }
            if (s != session) return@launch
            turnMark = kotlin.time.TimeSource.Monotonic.markNow()
            if (listenWords) {
                this@VoiceDetector.words.begin(words!!)
                wordsOn = true
            }
            micOpenNow = true
            println("MIC open")
            mic.start(
                onLevel = { rms ->
                    if (s == session) {
                        onLevel(VoiceMeter.display(rms))
                        val finished = meter.feed(rms, MicLevels.STEP_MS, mic.band, mic.periodic)
                        onPush?.invoke(meter.lastPush)
                        if ((meter.heard || meter.soft) && !voiced) {
                            voiced = true
                            onVoice?.invoke()
                        }
                        if (finished) done(outcome())
                    }
                },
                onFail = { done(VoiceTry.NO_MIC) },
            )
            // If the microphone never starts sending (a stuck permission prompt, say), give up kindly.
            // (A long held sound can keep listening past the window, up to HOLD_MAX_MS.)
            delay(windowMs + VoiceMeter.HOLD_MAX_MS + 2000L)
            done(VoiceTry.NO_MIC)
        }
    }

    /**
     * Fully closes the microphone (K3a: no barge-in watch keeps it open during Rory's speech any
     * more, and a turn's stream is never kept open for the next one -- see [release]'s own note
     * on why the web used to keep a stream open between turns, and [MIC open]/[MIC closed] in the
     * log, which should never show a clip playing between them).
     */
    private fun stopMic() {
        session++
        guard?.cancel()
        if (micOpenNow) {
            micOpenNow = false
            println("MIC closed")
        }
        mic.release()
    }

    fun cancel() {
        stopMic()
        if (wordsOn) {
            wordsOn = false
            words.cancel()
        }
    }

    /** Fully closes the microphone (leaving a voice story, or the app going to the background).
     * [cancel] alone already does this now (K3a): every turn opens the mic fresh, only once
     * Rory's own line has finished playing, and fully closes it the moment the turn ends, so a
     * clip or the browser voice is never playing while a real microphone stream is open (the open
     * stream switches iOS/iPadOS Safari's audio session to play-and-record, which ducks or
     * silences any playback started while it's open). */
    fun release() = cancel()
}

/** How a listening turn went: a clear voice, a soft try, silence, or no microphone. */
enum class VoiceTry { HEARD, SOFT, QUIET, NO_MIC }
