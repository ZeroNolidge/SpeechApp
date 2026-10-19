package com.zeronolidge.speechapp.audio

import kotlin.js.JsAny
import kotlin.js.js

// ---------------------------------------------------------------- talking (Web Speech synthesis)

private fun jsSpeakOne(text: String, rate: Double, onEnd: (Boolean) -> Unit): Unit = js(
    """(() => {
        const s = window.speechSynthesis;
        if (!s) { onEnd(false); return; }
        const u = new SpeechSynthesisUtterance(text);
        u.rate = rate; u.pitch = 1.1; u.lang = 'en-US';
        const vs = s.getVoices();
        const v = vs.find(v => v.lang === 'en-US' && /Google|Samantha|Natural/.test(v.name))
            || vs.find(v => v.lang && v.lang.startsWith('en-US'))
            || vs.find(v => v.lang && v.lang.startsWith('en'));
        if (v) u.voice = v;
        // Keep a reference so the browser doesn't drop the utterance (and its callbacks) early.
        window.__tlUtterances = window.__tlUtterances || [];
        window.__tlUtterances.push(u);
        let done = false, started = false;
        const fin = (ok) => {
            if (done) return;
            done = true;
            window.__tlUtterances = window.__tlUtterances.filter(x => x !== u);
            onEnd(ok);
        };
        u.onstart = () => { started = true; };
        u.onend = () => fin(true);
        u.onerror = (e) => fin(started || (e && e.error === 'interrupted'));
        // If the voice hasn't started within a couple of seconds, it isn't going to: say so now
        // instead of making everyone wait.
        window.__tlLatest = u;
        setTimeout(() => {
            if (started || done) return;
            // Only cancel if nothing newer has started speaking since.
            if (window.__tlLatest === u) { try { s.cancel(); } catch (e) {} }
            fin(false);
        }, 2500);
        // Chrome can leave the synthesizer paused (e.g. after switching tabs); wake it first.
        try { s.resume(); } catch (e) {}
        s.speak(u);
    })()"""
)

private fun jsCancelSpeech(): Unit = js("(() => { if (window.speechSynthesis) window.speechSynthesis.cancel(); })()")

/** The browser's own voice. [QueueSpeaker] handles the queue and time limits on top. */
class WebVoiceEngine : VoiceEngine {
    override fun speakOne(text: String, rate: Float, onEnd: (Boolean) -> Unit) =
        jsSpeakOne(text, rate.toDouble().coerceIn(0.3, 2.0), onEnd)

    override fun cancel() = jsCancelSpeech()
    override fun shutdown() = cancel()
}

// ---------------------------------------------------------------- microphone loudness

private fun jsMicAvailable(): Boolean = js("!!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && (window.AudioContext || window.webkitAudioContext))")

private fun jsMicStart(onLevel: (Double, Double, Double) -> Unit, onFail: () -> Unit): JsAny = js(
    """(() => {
        const st = { stopped: false, stream: null, timer: null, src: null };
        try {
            navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true } }).then(stream => {
                if (st.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
                st.stream = stream;
                const C = window.AudioContext || window.webkitAudioContext;
                window.__tlAudio = window.__tlAudio || new C();
                const ctx = window.__tlAudio;
                try { ctx.resume(); } catch (e) {}
                const src = ctx.createMediaStreamSource(stream);
                const an = ctx.createAnalyser();
                // About 43 ms of sound per reading (most of each 50 ms step).
                an.fftSize = 2048;
                an.smoothingTimeConstant = 0;
                src.connect(an);
                st.src = src;
                // Word listening (WebWords.kt) hears the same stream, when a turn asked for it.
                try { if (window.__tlWords && window.__tlWords.attach) window.__tlWords.attach(ctx, src); } catch (e) {}
                const buf = new Float32Array(an.fftSize);
                const spec = new Float32Array(an.frequencyBinCount);
                const hz = ctx.sampleRate / an.fftSize;
                const k = Math.max(1, Math.round(ctx.sampleRate / 16000));
                const n = Math.floor(buf.length / k);
                const d = new Float32Array(n);
                st.timer = setInterval(() => {
                    an.getFloatTimeDomainData(buf);
                    let sum = 0;
                    for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i];
                    const rms = Math.sqrt(sum / buf.length);
                    // Share of the energy in the voice range (150 to 2000 Hz) of 50 to 8000 Hz.
                    an.getFloatFrequencyData(spec);
                    let inV = 0, all = 0;
                    for (let b = 1; b < spec.length; b++) {
                        const f = b * hz;
                        if (f < 50 || f > 8000) continue;
                        const pw = Math.pow(10, spec[b] / 10);
                        all += pw;
                        if (f >= 150 && f <= 2000) inV += pw;
                    }
                    const band = all > 0 ? inV / all : 0;
                    // How voiced: the best normalised self-match at a child's or grown-up's pitch
                    // (80 to 600 Hz), on the sound brought down to about 16 kHz.
                    for (let i = 0; i < n; i++) { let a = 0; for (let j = 0; j < k; j++) a += buf[i * k + j]; d[i] = a / k; }
                    const sr = ctx.sampleRate / k;
                    const lo = Math.floor(sr / 600), hi = Math.min(Math.floor(sr / 80), n - 64);
                    let best = 0;
                    for (let lag = lo; lag <= hi; lag++) {
                        let xy = 0, xx = 0, yy = 0;
                        for (let i = 0; i + lag < n; i++) { const x = d[i], y = d[i + lag]; xy += x * y; xx += x * x; yy += y * y; }
                        if (xx > 0 && yy > 0) { const r = xy / Math.sqrt(xx * yy); if (r > best) best = r; }
                    }
                    onLevel(rms, band, best);
                }, 50);
            }).catch(() => { if (!st.stopped) onFail(); });
        } catch (e) { onFail(); }
        return st;
    })()"""
)

private fun jsMicStop(st: JsAny): Unit = js(
    """(() => {
        st.stopped = true;
        try { if (window.__tlWords && window.__tlWords.detach) window.__tlWords.detach(); } catch (e) {}
        try { if (st.timer) clearInterval(st.timer); } catch (e) {}
        try { if (st.src) st.src.disconnect(); } catch (e) {}
        try { if (st.stream) st.stream.getTracks().forEach(t => t.stop()); } catch (e) {}
    })()"""
)

/** Microphone loudness in the browser (getUserMedia and an analyser). Works on iPhone and iPad too. */
class WebMicLevels : MicLevels {
    private var current: JsAny? = null
    override val available: Boolean get() = jsMicAvailable()
    override var band = 1f
        private set
    override var periodic = 1f
        private set

    override fun start(onLevel: (Float) -> Unit, onFail: () -> Unit) {
        stop()
        current = jsMicStart({ rms, b, p ->
            band = b.toFloat()
            periodic = p.toFloat()
            onLevel(rms.toFloat())
        }, onFail)
    }

    override fun stop() {
        current?.let { jsMicStop(it) }
        current = null
    }
}

// ---------------------------------------------------------------- the parrot (MediaRecorder)

private fun jsRecordThenPlay(seconds: Int, onPlaying: () -> Unit, onDone: () -> Unit): JsAny = js(
    """(() => {
        const st = { stopped: false, rec: null, audio: null, stream: null };
        if (!navigator.mediaDevices || !window.MediaRecorder) { onDone(); return st; }
        navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
            if (st.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
            st.stream = stream;
            const chunks = [];
            const rec = new MediaRecorder(stream);
            st.rec = rec;
            rec.ondataavailable = e => chunks.push(e.data);
            rec.onstop = () => {
                stream.getTracks().forEach(t => t.stop());
                if (st.stopped) return;
                const url = URL.createObjectURL(new Blob(chunks, { type: rec.mimeType }));
                const a = new Audio(url);
                st.audio = a;
                a.onended = () => onDone();
                a.onerror = () => onDone();
                a.play().then(() => onPlaying()).catch(() => onDone());
            };
            rec.start();
            setTimeout(() => { if (rec.state !== 'inactive') rec.stop(); }, seconds * 1000);
        }).catch(() => onDone());
        return st;
    })()"""
)

private fun jsListenThenPlay(waitMs: Int, onLevel: (Double) -> Unit, onPlaying: () -> Unit, onQuiet: () -> Unit, onDone: () -> Unit): JsAny = js(
    """(() => {
        const st = { stopped: false, rec: null, audio: null, stream: null, timer: null };
        if (!navigator.mediaDevices || !window.MediaRecorder) { onDone(); return st; }
        navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true } }).then(stream => {
            if (st.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
            st.stream = stream;
            const C = window.AudioContext || window.webkitAudioContext;
            window.__tlAudio = window.__tlAudio || new C();
            const ctx = window.__tlAudio;
            try { ctx.resume(); } catch (e) {}
            const src = ctx.createMediaStreamSource(stream);
            const an = ctx.createAnalyser();
            an.fftSize = 1024;
            src.connect(an);
            const buf = new Float32Array(an.fftSize);
            const chunks = [];
            const rec = new MediaRecorder(stream);
            st.rec = rec;
            let heard = false, voiceMs = 0, quietMs = 0, t = 0, room = [], quiet = false;
            rec.ondataavailable = e => chunks.push(e.data);
            rec.onstop = () => {
                try { src.disconnect(); } catch (e) {}
                stream.getTracks().forEach(tr => tr.stop());
                if (st.stopped) return;
                if (quiet) { onQuiet(); return; }
                const url = URL.createObjectURL(new Blob(chunks, { type: rec.mimeType }));
                const a = new Audio(url);
                st.audio = a;
                a.onended = () => onDone();
                a.onerror = () => onDone();
                a.play().then(() => onPlaying()).catch(() => onDone());
            };
            rec.start();
            st.timer = setInterval(() => {
                an.getFloatTimeDomainData(buf);
                let sum = 0;
                for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i];
                const rms = Math.sqrt(sum / buf.length);
                onLevel(Math.min(1, rms / 0.15));
                t += 50;
                if (t <= 300) { room.push(rms); return; }
                const bar = Math.max(0.012, 2.2 * (room.reduce((a, b) => a + b, 0) / Math.max(1, room.length)));
                if (rms > bar) { voiceMs += 50; quietMs = 0; if (voiceMs >= 100) heard = true; } else { quietMs += 50; }
                // Stop 0.7 s after he stops talking, or after 6 s of talking; no voice in time: quiet.
                if ((heard && quietMs >= 700) || voiceMs >= 6000 || (!heard && t >= waitMs)) {
                    clearInterval(st.timer);
                    quiet = !heard;
                    if (rec.state !== 'inactive') rec.stop();
                }
            }, 50);
        }).catch(() => onDone());
        return st;
    })()"""
)

private fun jsStopParrot(st: JsAny): Unit = js(
    """(() => {
        st.stopped = true;
        try { if (st.timer) clearInterval(st.timer); } catch (e) {}
        try { if (st.rec && st.rec.state !== 'inactive') st.rec.stop(); } catch (e) {}
        try { if (st.audio) st.audio.pause(); } catch (e) {}
        try { if (st.stream) st.stream.getTracks().forEach(t => t.stop()); } catch (e) {}
    })()"""
)

class WebParrot : Parrot {
    private var current: JsAny? = null

    override fun recordThenPlay(seconds: Int, onPlaying: () -> Unit, onDone: () -> Unit) {
        stop()
        current = jsRecordThenPlay(seconds, onPlaying) {
            current = null
            onDone()
        }
    }

    override fun listenThenPlay(waitMs: Int, onLevel: (Float) -> Unit, onPlaying: () -> Unit, onQuiet: () -> Unit, onDone: () -> Unit) {
        stop()
        current = jsListenThenPlay(waitMs, { onLevel(it.toFloat()) }, onPlaying, {
            current = null
            onQuiet()
        }) {
            current = null
            onDone()
        }
    }

    override fun stop() {
        current?.let { jsStopParrot(it) }
        current = null
    }
}
