package com.zeronolidge.speechapp.ui.platform

import kotlin.js.js

private fun jsOffline(): Boolean = js("(() => !!window.__tlOffline)()")
private fun jsCanInstall(): Boolean = js("(() => !!window.__tlInstall)()")
private fun jsInstall(): Unit = js("(() => { try { const e = window.__tlInstall; if (e) { e.prompt(); window.__tlInstall = null; } } catch (x) {} })()")
private fun jsPersisted(): Boolean = js("(() => !!window.__tlPersisted)()")

actual object AppHome {
    actual val isWeb: Boolean = true
    actual fun offlineReady(): Boolean = jsOffline()
    actual fun canInstall(): Boolean = jsCanInstall()
    actual fun install() = jsInstall()
    actual fun storageProtected(): Boolean = jsPersisted()
}
