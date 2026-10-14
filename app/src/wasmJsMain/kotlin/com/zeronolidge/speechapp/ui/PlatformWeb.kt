package com.zeronolidge.speechapp.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Browsers ask for the microphone themselves the first time it's used. */
@Composable
actual fun rememberMicPermission(): MicPermission = remember {
    object : MicPermission {
        override fun granted() = true
        override fun request(onResult: (Boolean) -> Unit) = onResult(true)
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // The browser's own back button leaves the page; in-app navigation uses the on-screen arrows.
}
