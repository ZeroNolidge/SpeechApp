package com.zeronolidge.speechapp.ui.platform

import androidx.compose.runtime.Composable

/** Whether the app may use the microphone. */
enum class MicStatus { ON, BLOCKED, NOT_ASKED }

/**
 * Asking for the microphone works differently on Android and in a browser. The app only asks
 * from the start screen or the grown-ups page (so a grown-up taps Allow), never mid-story.
 */
interface MicPermission {
    /** Read inside composition; it updates when the answer changes. */
    val status: MicStatus
    fun granted(): Boolean = status == MicStatus.ON
    fun request(onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberMicPermission(): MicPermission

/** The system back button/gesture (Android only; browsers have their own back button). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Copies text (a backup code) to the clipboard. Returns false if it couldn't. */
@Composable
expect fun rememberCopyText(): (String) -> Boolean
