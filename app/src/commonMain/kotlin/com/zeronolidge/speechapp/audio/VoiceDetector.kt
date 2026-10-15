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
class VoiceDetector(private val mic: MicLevels) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var session = 0
    private var guard: Job? = null

    val available: Boolean get() = mic.available

    fun listen(windowMs: Int = 4000, onLevel: (Float) -> Unit, onResult: (heard: Boolean?) -> Unit) {
        cancel()
        val s = ++session
        val meter = VoiceMeter(windowMs)
        fun done(result: Boolean?) {
            if (s != session) return
            cancel()
            onResult(result)
        }
        // If the microphone never starts sending (a stuck permission prompt, say), give up kindly.
        guard = scope.launch {
            delay(windowMs + 3000L)
            done(null)
        }
        mic.start(
            onLevel = { rms ->
                if (s == session) {
                    onLevel(VoiceMeter.display(rms))
                    if (meter.feed(rms, MicLevels.STEP_MS)) done(meter.heard)
                }
            },
            onFail = { done(null) },
        )
    }

    fun cancel() {
        session++
        guard?.cancel()
        mic.stop()
    }
}
