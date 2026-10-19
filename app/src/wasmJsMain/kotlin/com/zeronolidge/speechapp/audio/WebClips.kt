package com.zeronolidge.speechapp.audio

import kotlin.js.js

// The pre-made voice clips in audio/ (see tools/make_voice.py). audio/manifest.json maps each
// line's normalized text to its mp3. Normal speech is decoded and scheduled with Web Audio, so
// pieces play back to back with no gap (silence at the ends of each clip is trimmed); slow speech
// uses an audio element at a lower rate that keeps the voice's pitch.

private fun jsClipsLoad(): Unit = js(
    """(() => {
        if (window.__tlClips) return;
        window.__tlClips = { map: null, cache: new Map(), playing: [], gen: 0 };
        window.__tlClips.loaded = fetch('audio/manifest.json', { cache: 'no-cache' })
            .then(r => r.ok ? r.json() : null)
            .then(m => { if (m && m.clips) window.__tlClips.map = m.clips; })
            .catch(() => {});
    })()"""
)

private fun jsClipsWhenReady(maxMs: Int, then: () -> Unit): Unit = js(
    """(() => {
        let done = false;
        const go = () => { if (!done) { done = true; then(); } };
        const st = window.__tlClips;
        if (st && st.loaded) st.loaded.then(go, go);
        setTimeout(go, maxMs);
    })()"""
)

private fun jsClipsReady(): Boolean = js("!!(window.__tlClips && window.__tlClips.map)")

// A clip that failed to load or turned out silent counts as missing from then on.
private fun jsClipsHas(key: String): Boolean = js("!!(window.__tlClips && window.__tlClips.map && Object.prototype.hasOwnProperty.call(window.__tlClips.map, key) && !(window.__tlClips.bad && window.__tlClips.bad.has(key)))")

private fun jsClipsPlay(keysJoined: String, slow: Boolean, onEnd: (Boolean) -> Unit): Unit = js(
    """(() => {
        const st = window.__tlClips;
        const keys = keysJoined.split('\n').filter(k => k);
        const C = window.AudioContext || window.webkitAudioContext;
        if (!st || !st.map || !keys.length || !C) { onEnd(false); return; }
        const gen = ++st.gen;
        const url = (k) => 'audio/' + st.map[k];
        // '_' (ClipText.PAUSE) is a short breath between two sentences.
        const PAUSE = '_', breath = 0.25, slowRate = 0.72;
        let finished = false;
        const fin = (ok) => { if (finished) return; finished = true; if (gen === st.gen) onEnd(ok); };
        window.__tlAudio = window.__tlAudio || new C();
        const ctx = window.__tlAudio;
        try { ctx.resume(); } catch (e) {}
        // Decoded clips are kept, with the quiet at each end found once (so a clip's trailing
        // silence, up to 2 s on some single words, never delays his turn). A clip that won't load
        // or is silent is rejected and remembered: the caller then uses the browser voice.
        const load = (k) => {
            if (k === PAUSE) return Promise.resolve({ pause: breath });
            if (st.cache.has(k)) return st.cache.get(k);
            const p = fetch(url(k)).then(r => { if (!r.ok) throw new Error('missing'); return r.arrayBuffer(); })
                .then(b => new Promise((res, rej) => ctx.decodeAudioData(b, res, rej))).then(buf => {
                const d = buf.getChannelData(0);
                const th = 0.01;
                let s = 0, e = d.length - 1;
                while (s < e && Math.abs(d[s]) < th) s++;
                while (e > s && Math.abs(d[e]) < th) e--;
                // Under normal speaking loudness (finished clips sit near 0.1) counts as silent
                // too: a whispery clip is harder to hear than the browser voice.
                let peak = 0, sum = 0, n = 0;
                for (let i = s; i <= e; i += 8) { const v = Math.abs(d[i]); peak = Math.max(peak, v); sum += v * v; n++; }
                const rms = Math.sqrt(sum / Math.max(1, n));
                if ((e - s) / buf.sampleRate < 0.12 || peak < 0.05 || rms < 0.03) throw new Error('silent');
                const pad = Math.floor(buf.sampleRate * 0.015);
                return { buf, start: Math.max(0, s - pad) / buf.sampleRate, end: Math.min(d.length, e + pad) / buf.sampleRate };
            });
            st.cache.set(k, p);
            p.catch(() => { st.bad = st.bad || new Set(); st.bad.add(k); });
            return p;
        };
        // Everything is loaded and checked before anything plays, so a bad clip never leaves half
        // a sentence said twice.
        Promise.all(keys.map(load)).then(parts => {
            if (gen !== st.gen) return;
            st.track = null;
            if (slow) {
                // Slow: an audio element per clip, slower at the same pitch, cut to its sound.
                let i = 0;
                const next = () => {
                    if (gen !== st.gen) return;
                    if (i >= parts.length) { fin(true); return; }
                    const c = parts[i], k = keys[i];
                    i++;
                    if (c.pause) { setTimeout(next, c.pause * 1000 / slowRate); return; }
                    const a = new Audio(url(k));
                    a.preservesPitch = true; a.mozPreservesPitch = true; a.webkitPreservesPitch = true;
                    st.playing.push(a);
                    // Rory's mouth follows this clip: its position in the decoded sound.
                    st.track = { gen, el: a, buf: c.buf, key: k };
                    let done = false, timer = 0;
                    const after = () => { if (done) return; done = true; clearTimeout(timer); try { a.pause(); } catch (e) {} next(); };
                    a.onended = after;
                    a.onerror = after;
                    a.onplaying = () => { clearTimeout(timer); timer = setTimeout(after, Math.max(0, c.end - a.currentTime) * 1000 / slowRate + 40); };
                    a.onloadedmetadata = () => {
                        try { a.currentTime = c.start; } catch (e) {}
                        a.playbackRate = slowRate;
                        const p = a.play();
                        if (p && p.catch) p.catch(after);
                    };
                    a.load();
                    // In case the element never gets going.
                    timer = setTimeout(after, (c.end - c.start) * 1000 / slowRate + 3000);
                };
                next();
                return;
            }
            let t = ctx.currentTime + 0.03;
            let last = null;
            const segs = [];
            st.track = { gen, segs };
            parts.forEach((c, i) => {
                if (c.pause) { t += c.pause; return; }
                const src = ctx.createBufferSource();
                src.buffer = c.buf;
                src.connect(ctx.destination);
                const dur = Math.max(0.01, c.end - c.start);
                src.start(t, c.start, dur);
                segs.push({ t0: t, off: c.start, dur, buf: c.buf, key: keys[i] });
                t += dur;
                st.playing.push(src);
                last = src;
            });
            // A little safety net in case 'ended' never comes.
            const ms = (t - ctx.currentTime) * 1000 + 600;
            if (last) last.onended = () => fin(true);
            setTimeout(() => fin(true), ms);
        }).catch(() => fin(false));
    })()"""
)

// How loud the clip playing right now is at this moment (RMS over 30 ms of the decoded sound), and
// its text, for Rory's mouth: "level<tab>key", or "" when no clip is playing.
private fun jsClipsMouth(): String = js(
    """(() => {
        const st = window.__tlClips;
        const tr = st && st.track;
        if (!tr || tr.gen !== st.gen) return '';
        let buf = null, pos = 0, key = '';
        if (tr.el) {
            if (tr.el.paused || tr.el.ended) return '';
            buf = tr.buf; pos = tr.el.currentTime; key = tr.key;
        } else {
            const now = window.__tlAudio ? window.__tlAudio.currentTime : 0;
            const s = tr.segs.find(s => now >= s.t0 && now < s.t0 + s.dur);
            if (!s) return '';
            buf = s.buf; pos = s.off + (now - s.t0); key = s.key;
        }
        const d = buf.getChannelData(0), sr = buf.sampleRate;
        const half = Math.floor(sr * 0.015), c = Math.floor(pos * sr);
        let sum = 0, n = 0;
        for (let i = Math.max(0, c - half); i < Math.min(d.length, c + half); i += 2) { sum += d[i] * d[i]; n++; }
        return Math.sqrt(sum / Math.max(1, n)).toFixed(4) + '\t' + key;
    })()"""
)

private fun jsClipsStop(): Unit = js(
    """(() => {
        const st = window.__tlClips;
        if (!st) return;
        st.gen++;
        st.playing.forEach(p => { try { if (p.stop) p.stop(); else { p.pause(); p.src = ''; } } catch (e) {} });
        st.playing = [];
        st.track = null;
    })()"""
)

/** The web player for the pre-made clips. Starts loading the clip list straight away. */
class WebClips : ClipPlayer {
    init { jsClipsLoad() }

    override val ready: Boolean get() = jsClipsReady()
    override fun whenReady(maxMs: Int, then: () -> Unit) = jsClipsWhenReady(maxMs, then)
    override fun has(key: String): Boolean = jsClipsHas(key)
    override fun play(keys: List<String>, slow: Boolean, onEnd: (ok: Boolean) -> Unit) = jsClipsPlay(keys.joinToString("\n"), slow, onEnd)
    override fun stop() = jsClipsStop()
    override fun mouth(): MouthNow? = jsClipsMouth().takeIf { it.isNotEmpty() }?.let { s ->
        val tab = s.indexOf('\t')
        s.substring(0, tab).toFloatOrNull()?.let { MouthNow(it, s.substring(tab + 1)) }
    }
}
