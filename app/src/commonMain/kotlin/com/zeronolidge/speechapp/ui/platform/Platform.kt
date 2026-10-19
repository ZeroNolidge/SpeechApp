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

/** True when the device asks for less motion (iPad/browser "Reduce motion", Android "Remove animations"). */
@Composable
expect fun rememberReduceMotion(): Boolean

/** Playing offline, "Add to Home Screen" and keeping saved progress safe. */
expect object AppHome {
    /** True for the web version (the Android app is installed and offline already). */
    val isWeb: Boolean
    /** Every file has been saved on this device, so the app works without internet. */
    fun offlineReady(): Boolean
    /** The browser can offer a one-tap install (Chrome/Android). */
    fun canInstall(): Boolean
    fun install()
    /** The browser agreed not to clear this app's saved progress. */
    fun storageProtected(): Boolean
    /** On mobile data or "save data" (as far as the device says; unknown counts as Wi-Fi). */
    fun onMobileData(): Boolean
    /** Saves every voice clip for offline play (a grown-up asked); safe to call again. */
    fun saveVoice()
    /** How far saving the voice has got: saved and total clips (0, 0 before it starts). */
    fun voiceSaved(): Pair<Int, Int>
}
