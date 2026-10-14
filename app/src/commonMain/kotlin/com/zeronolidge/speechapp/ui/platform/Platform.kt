package com.zeronolidge.speechapp.ui.platform

import androidx.compose.runtime.Composable

/** Asking for the microphone works differently on Android and in a browser. */
interface MicPermission {
    fun granted(): Boolean
    fun request(onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberMicPermission(): MicPermission

/** The system back button/gesture (Android only; browsers have their own back button). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
