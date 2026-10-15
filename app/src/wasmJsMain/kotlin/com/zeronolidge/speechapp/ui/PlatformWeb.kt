package com.zeronolidge.speechapp.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.js.js

private fun jsQueryMic(onState: (String) -> Unit): Unit = js(
    """(() => {
        try {
            if (!navigator.permissions || !navigator.permissions.query) { onState('unknown'); return; }
            navigator.permissions.query({ name: 'microphone' }).then((p) => {
                onState(p.state);
                p.onchange = () => onState(p.state);
            }).catch(() => onState('unknown'));
        } catch (e) { onState('unknown'); }
    })()"""
)

private fun jsAskMic(onResult: (Boolean) -> Unit): Unit = js(
    """(() => {
        try {
            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) { onResult(false); return; }
            navigator.mediaDevices.getUserMedia({ audio: true })
                .then((s) => { s.getTracks().forEach((t) => t.stop()); onResult(true); })
                .catch(() => onResult(false));
        } catch (e) { onResult(false); }
    })()"""
)

/**
 * The browser's microphone permission, kept up to date with the Permissions API (where the
 * browser has it; Safari may not, so a "yes" from asking is remembered for this visit).
 */
object WebMic : MicPermission {
    private var state by mutableStateOf(MicStatus.NOT_ASKED)
    private var allowedThisVisit = false

    init {
        jsQueryMic { s ->
            state = when (s) {
                "granted" -> MicStatus.ON
                "denied" -> MicStatus.BLOCKED
                else -> if (allowedThisVisit) MicStatus.ON else MicStatus.NOT_ASKED
            }
        }
    }

    override val status: MicStatus get() = state

    override fun request(onResult: (Boolean) -> Unit) {
        jsAskMic { ok ->
            allowedThisVisit = allowedThisVisit || ok
            state = if (ok) MicStatus.ON else MicStatus.BLOCKED
            onResult(ok)
        }
    }
}

@Composable
actual fun rememberMicPermission(): MicPermission = WebMic

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // The browser's own back button leaves the page; in-app navigation uses the on-screen arrows.
}

private fun jsCopy(text: String): Boolean = js("(() => { try { if (navigator.clipboard) { navigator.clipboard.writeText(text); return true; } } catch (e) {} return false; })()")

@Composable
actual fun rememberCopyText(): (String) -> Boolean = remember { { text: String -> jsCopy(text) } }

private fun jsReduceMotion(): Boolean = js("(() => { try { return window.matchMedia('(prefers-reduced-motion: reduce)').matches; } catch (e) { return false; } })()")

@Composable
actual fun rememberReduceMotion(): Boolean = remember { jsReduceMotion() }
