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
        fetch('audio/manifest.json', { cache: 'no-cache' })
            .then(r => r.ok ? r.json() : null)
            .then(m => { if (m && m.clips) window.__tlClips.map = m.clips; })
            .catch(() => {});
    })()"""
)

private fun jsClipsReady(): Boolean = js("!!(window.__tlClips && window.__tlClips.map)")

private fun jsClipsHas(key: String): Boolean = js("!!(window.__tlClips && window.__tlClips.map && Object.prototype.hasOwnProperty.call(window.__tlClips.map, key))")

private fun jsClipsPlay(keysJoined: String, slow: Boolean, onEnd: (Boolean) -> Unit): Unit = js(
    """(() => {
        const st = window.__tlClips;
        const keys = keysJoined.split('\n').filter(k => k);
        if (!st || !st.map || !keys.length) { onEnd(false); return; }
        const gen = ++st.gen;
        const url = (k) => 'audio/' + st.map[k];
        let finished = false;
        const fin = (ok) => { if (finished) return; finished = true; if (gen === st.gen) onEnd(ok); };
        if (slow) {
            // Slow: one after another, slower but at the same pitch.
            let i = 0;
            const next = () => {
                if (gen !== st.gen) return;
                if (i >= keys.length) { fin(true); return; }
                const a = new Audio(url(keys[i++]));
                a.preservesPitch = true; a.mozPreservesPitch = true; a.webkitPreservesPitch = true;
                a.playbackRate = 0.72;
                st.playing.push(a);
                a.onended = next;
                a.onerror = next;
                const p = a.play();
                if (p && p.catch) p.catch(() => next());
            };
            next();
            return;
        }
        const C = window.AudioContext || window.webkitAudioContext;
        if (!C) { fin(false); return; }
        window.__tlAudio = window.__tlAudio || new C();
        const ctx = window.__tlAudio;
        try { ctx.resume(); } catch (e) {}
        // Decoded clips are kept, with the quiet at each end found once.
        const load = (k) => {
            if (st.cache.has(k)) return st.cache.get(k);
            const p = fetch(url(k)).then(r => r.arrayBuffer()).then(b => new Promise((res, rej) => ctx.decodeAudioData(b, res, rej))).then(buf => {
                const d = buf.getChannelData(0);
                const th = 0.01;
                let s = 0, e = d.length - 1;
                while (s < e && Math.abs(d[s]) < th) s++;
                while (e > s && Math.abs(d[e]) < th) e--;
                const pad = Math.floor(buf.sampleRate * 0.015);
                return { buf, start: Math.max(0, s - pad) / buf.sampleRate, end: Math.min(d.length, e + pad) / buf.sampleRate };
            });
            st.cache.set(k, p);
            p.catch(() => st.cache.delete(k));
            return p;
        };
        Promise.all(keys.map(load)).then(parts => {
            if (gen !== st.gen) return;
            let t = ctx.currentTime + 0.03;
            let last = null;
            parts.forEach(c => {
                const src = ctx.createBufferSource();
                src.buffer = c.buf;
                src.connect(ctx.destination);
                const dur = Math.max(0.01, c.end - c.start);
                src.start(t, c.start, dur);
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

private fun jsClipsStop(): Unit = js(
    """(() => {
        const st = window.__tlClips;
        if (!st) return;
        st.gen++;
        st.playing.forEach(p => { try { if (p.stop) p.stop(); else { p.pause(); p.src = ''; } } catch (e) {} });
        st.playing = [];
    })()"""
)

/** The web player for the pre-made clips. Starts loading the clip list straight away. */
class WebClips : ClipPlayer {
    init { jsClipsLoad() }

    override val ready: Boolean get() = jsClipsReady()
    override fun has(key: String): Boolean = jsClipsHas(key)
    override fun play(keys: List<String>, slow: Boolean, onEnd: (ok: Boolean) -> Unit) = jsClipsPlay(keys.joinToString("\n"), slow, onEnd)
    override fun stop() = jsClipsStop()
}
