package com.zeronolidge.speechapp.ui.platform

/** The Android app is installed, works offline and keeps its progress in its own storage. */
actual object AppHome {
    actual val isWeb: Boolean = false
    actual fun offlineReady(): Boolean = true
    actual fun canInstall(): Boolean = false
    actual fun install() {}
    actual fun storageProtected(): Boolean = true
    actual fun onMobileData(): Boolean = false
}
