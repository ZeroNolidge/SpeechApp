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
    actual fun onMobileData(): Boolean = jsMobileData()
    actual fun saveVoice() = jsSaveVoice()
    actual fun voiceSaved(): Pair<Int, Int> = jsVoiceSaved().split("/").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
}

// Every clip in audio/manifest.json into the long-lived cache (the service worker serves them
// from there), a few at a time; already saved ones are skipped.
private fun jsSaveVoice(): Unit = js("""(() => {
    if (window.__tlVoice && window.__tlVoice.running) return;
    const st = window.__tlVoice = { running: true, done: 0, total: 0 };
    if (!window.caches) { st.running = false; return; }
    fetch('audio/manifest.json', { cache: 'no-cache' }).then(r => r.json()).then(m => {
        const files = Array.from(new Set(Object.values(m.clips || {}))).map(f => 'audio/' + f);
        st.total = files.length;
        return caches.open('talkandlearn-static').then(c => {
            let i = 0;
            const next = () => {
                if (i >= files.length) return Promise.resolve();
                const url = new URL(files[i++], document.baseURI).href;
                return c.match(url).then(hit => hit || c.add(url)).catch(() => {}).then(() => { st.done++; return next(); });
            };
            return Promise.all([next(), next(), next(), next()]);
        });
    }).catch(() => {}).then(() => { st.running = false; });
})()""")

private fun jsVoiceSaved(): String = js("(() => { const s = window.__tlVoice; return s ? (s.done + '/' + s.total) : '0/0'; })()")

// Chrome on Android says the connection type; iPad Safari says nothing (counted as Wi-Fi: most iPads are).
private fun jsMobileData(): Boolean = js("(() => { const c = navigator.connection; return !!(c && (c.saveData || c.type === 'cellular')); })()")
